package server;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import db.DatabaseManager;
import dsa.*;
import model.Doctor;
import model.Patient;

import java.io.IOException;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class HospitalController implements HttpHandler {
    private final DatabaseManager db;
    private final CustomHashMap<String, Patient> patientMap;
    private final CustomHashMap<String, Doctor> doctorMap;
    private final EmergencyMinHeap triageMinHeap;
    private final DoctorWaitingQueue availableDoctorsQueue;
    private final TreatmentLinkedList activeTreatmentList;
    private final TreatmentLinkedList dischargedHistoryList;
    private long arrivalSequence = 100L;

    public HospitalController() {
        this.db = new DatabaseManager();
        this.patientMap = new CustomHashMap<>(128);
        this.doctorMap = new CustomHashMap<>(64);
        this.triageMinHeap = new EmergencyMinHeap();
        this.availableDoctorsQueue = new DoctorWaitingQueue();
        this.activeTreatmentList = new TreatmentLinkedList();
        this.dischargedHistoryList = new TreatmentLinkedList();
        initializeFromDatabase();
    }

    private synchronized void initializeFromDatabase() {
        List<Doctor> doctors = db.loadAllDoctors();
        for (Doctor d : doctors) {
            doctorMap.put(d.getDoctorId(), d);
            if ("AVAILABLE".equalsIgnoreCase(d.getStatus())) {
                availableDoctorsQueue.enqueue(d);
            }
        }

        List<Patient> patients = db.loadAllPatients();
        for (Patient p : patients) {
            patientMap.put(p.getPatientId(), p);
            if (p.getArrivalOrder() >= arrivalSequence) {
                arrivalSequence = p.getArrivalOrder() + 1;
            }
            if ("WAITING".equalsIgnoreCase(p.getStatus())) {
                triageMinHeap.insert(p);
            } else if ("IN_TREATMENT".equalsIgnoreCase(p.getStatus())) {
                activeTreatmentList.addLast(p);
            } else if ("DISCHARGED".equalsIgnoreCase(p.getStatus())) {
                dischargedHistoryList.addLast(p);
            }
        }
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        URI uri = exchange.getRequestURI();
        String path = uri.getPath();
        String method = exchange.getRequestMethod();

        if (path.startsWith("/api/")) {
            handleApi(exchange, path, method, uri.getQuery());
        } else {
            serveStaticFile(exchange, path);
        }
    }

    private void handleApi(HttpExchange exchange, String path, String method, String rawQuery) throws IOException {
        Map<String, String> params = parseQueryParams(rawQuery);
        try {
            if (path.equals("/api/state") && method.equals("GET")) {
                sendJson(exchange, 200, buildDashboardJson());

            } else if (path.equals("/api/patient/register") && method.equals("POST")) {
                Map<String, String> body = parseFormBody(exchange);
                String name = body.getOrDefault("name", "").trim();
                int age = Integer.parseInt(body.getOrDefault("age", "30").trim());
                String symptoms = body.getOrDefault("symptoms", "").trim();
                int level = Integer.parseInt(body.getOrDefault("emergencyLevel", "3").trim());

                if (name.isEmpty() || symptoms.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"Patient name and symptoms are required.\"}");
                    return;
                }

                String customId = body.getOrDefault("patientId", "").trim().toUpperCase();
                if (customId.isEmpty()) {
                    customId = "PT-" + (1001 + patientMap.size());
                    while (patientMap.containsKey(customId)) {
                        customId = "PT-" + (1000 + new Random().nextInt(8999));
                    }
                } else if (patientMap.containsKey(customId)) {
                    sendJson(exchange, 400, "{\"error\":\"Patient ID already exists.\"}");
                    return;
                }

                String timeNow = LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a"));
                Patient patient = new Patient(
                    customId, name, age, symptoms, level,
                    arrivalSequence++, timeNow, "WAITING", "", ""
                );

                patientMap.put(customId, patient);
                triageMinHeap.insert(patient);
                db.syncState(patientMap.values(), doctorMap.values());
                sendJson(exchange, 200, "{\"status\":\"registered\",\"patientId\":\"" + Patient.escapeJson(customId) + "\"}");

            } else if (path.equals("/api/patient/priority") && method.equals("POST")) {
                Map<String, String> body = parseFormBody(exchange);
                String patientId = body.getOrDefault("patientId", "").trim().toUpperCase();
                int newLevel = Integer.parseInt(body.getOrDefault("emergencyLevel", "1").trim());

                Patient p = patientMap.get(patientId);
                if (p == null) {
                    sendJson(exchange, 404, "{\"error\":\"Patient not found.\"}");
                    return;
                }
                if (!"WAITING".equalsIgnoreCase(p.getStatus())) {
                    sendJson(exchange, 400, "{\"error\":\"Priority can only be updated while patient is waiting.\"}");
                    return;
                }

                triageMinHeap.updatePriority(patientId, newLevel);
                db.syncState(patientMap.values(), doctorMap.values());
                sendJson(exchange, 200, "{\"status\":\"priority_updated\"}");

            } else if (path.equals("/api/triage/assign-next") && method.equals("POST")) {
                if (triageMinHeap.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"No patients currently waiting in triage.\"}");
                    return;
                }
                if (availableDoctorsQueue.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"All on-duty doctors are currently busy. Discharge a patient first.\"}");
                    return;
                }

                Patient nextPatient = triageMinHeap.extractMin();
                Doctor nextDoctor = availableDoctorsQueue.dequeue();

                nextPatient.setStatus("IN_TREATMENT");
                nextPatient.setAssignedDoctorId(nextDoctor.getDoctorId());
                nextPatient.setAssignedDoctorName(nextDoctor.getName());

                nextDoctor.setStatus("BUSY");
                nextDoctor.setCurrentPatientId(nextPatient.getPatientId());

                activeTreatmentList.addLast(nextPatient);
                db.syncState(patientMap.values(), doctorMap.values());

                String resp = String.format(
                    "{\"status\":\"assigned\",\"patientName\":\"%s\",\"doctorName\":\"%s\"}",
                    Patient.escapeJson(nextPatient.getName()),
                    Patient.escapeJson(nextDoctor.getName())
                );
                sendJson(exchange, 200, resp);

            } else if (path.equals("/api/patient/discharge") && method.equals("POST")) {
                Map<String, String> body = parseFormBody(exchange);
                String patientId = body.getOrDefault("patientId", "").trim().toUpperCase();

                Patient p = patientMap.get(patientId);
                if (p == null || !"IN_TREATMENT".equalsIgnoreCase(p.getStatus())) {
                    sendJson(exchange, 400, "{\"error\":\"Patient is not currently in active treatment.\"}");
                    return;
                }

                activeTreatmentList.removeByPatientId(patientId);
                p.setStatus("DISCHARGED");
                dischargedHistoryList.addLast(p);

                String docId = p.getAssignedDoctorId();
                if (docId != null && !docId.isEmpty()) {
                    Doctor doc = doctorMap.get(docId);
                    if (doc != null) {
                        doc.setStatus("AVAILABLE");
                        doc.setCurrentPatientId("");
                        availableDoctorsQueue.enqueue(doc);
                    }
                }

                db.syncState(patientMap.values(), doctorMap.values());
                sendJson(exchange, 200, "{\"status\":\"discharged\"}");

            } else if (path.equals("/api/doctor/add") && method.equals("POST")) {
                Map<String, String> body = parseFormBody(exchange);
                String name = body.getOrDefault("name", "").trim();
                String specialty = body.getOrDefault("specialty", "").trim();
                if (name.isEmpty() || specialty.isEmpty()) {
                    sendJson(exchange, 400, "{\"error\":\"Doctor name and specialty are required.\"}");
                    return;
                }
                String docId = "DOC-" + (101 + doctorMap.size());
                while (doctorMap.containsKey(docId)) {
                    docId = "DOC-" + (100 + new Random().nextInt(899));
                }
                Doctor doc = new Doctor(docId, name, specialty, "AVAILABLE", "");
                doctorMap.put(docId, doc);
                availableDoctorsQueue.enqueue(doc);
                db.syncState(patientMap.values(), doctorMap.values());
                sendJson(exchange, 200, "{\"status\":\"doctor_added\"}");

            } else if (path.equals("/api/patient/lookup") && method.equals("GET")) {
                String q = params.getOrDefault("q", "").trim();
                List<Patient> ordered = triageMinHeap.getOrderedWaitingList();
                Map<String, Integer> posMap = new HashMap<>();
                for (int i = 0; i < ordered.size(); i++) {
                    posMap.put(ordered.get(i).getPatientId(), i + 1);
                }

                List<Patient> matches = new ArrayList<>();
                Patient exact = patientMap.get(q.toUpperCase());
                if (exact != null) {
                    matches.add(exact);
                } else {
                    String lower = q.toLowerCase();
                    for (Patient p : patientMap.values()) {
                        if (p.getName().toLowerCase().contains(lower) || p.getPatientId().toLowerCase().contains(lower)) {
                            matches.add(p);
                        }
                    }
                }

                int availDocs = Math.max(1, availableDoctorsQueue.size());
                StringBuilder sb = new StringBuilder("[");
                for (int i = 0; i < matches.size(); i++) {
                    if (i > 0) sb.append(",");
                    Patient p = matches.get(i);
                    int pos = posMap.getOrDefault(p.getPatientId(), 0);
                    int waitMins = (pos == 0) ? 0 : ((pos - 1) / availDocs + 1) * 12;
                    sb.append(p.toJson(pos, waitMins));
                }
                sb.append("]");
                sendJson(exchange, 200, sb.toString());

            } else {
                sendJson(exchange, 404, "{\"error\":\"Endpoint not found\"}");
            }
        } catch (Exception e) {
            sendJson(exchange, 400, "{\"error\":\"" + Patient.escapeJson(e.getMessage()) + "\"}");
        }
    }

    private String buildDashboardJson() {
        List<Patient> waitingOrdered = triageMinHeap.getOrderedWaitingList();
        List<Patient> inTreatment = activeTreatmentList.toList();
        List<Patient> discharged = dischargedHistoryList.toList();
        List<Doctor> allDoctors = doctorMap.values();
        allDoctors.sort(Comparator.comparing(Doctor::getDoctorId));

        int availableDocCount = availableDoctorsQueue.size();
        int divisor = Math.max(1, availableDocCount);

        int criticalWaiting = 0;
        for (Patient p : waitingOrdered) {
            if (p.getEmergencyLevel() == 1) criticalWaiting++;
        }

        StringBuilder sb = new StringBuilder("{");
        sb.append("\"dbStatus\":\"").append(db.isMysqlConnected() ? "MySQL Connected" : "Local Storage Active").append("\",");
        sb.append("\"totalPatients\":").append(patientMap.size()).append(",");
        sb.append("\"waitingCount\":").append(waitingOrdered.size()).append(",");
        sb.append("\"criticalCount\":").append(criticalWaiting).append(",");
        sb.append("\"inTreatmentCount\":").append(inTreatment.size()).append(",");
        sb.append("\"dischargedCount\":").append(discharged.size()).append(",");
        sb.append("\"availableDoctorsCount\":").append(availableDocCount).append(",");
        sb.append("\"totalDoctorsCount\":").append(allDoctors.size()).append(",");

        // Waiting list with calculated queue position & estimated wait time (12 mins avg consult per doctor slot)
        sb.append("\"waitingList\":[");
        for (int i = 0; i < waitingOrdered.size(); i++) {
            if (i > 0) sb.append(",");
            int pos = i + 1;
            int estWait = ((i / divisor) + 1) * 12;
            sb.append(waitingOrdered.get(i).toJson(pos, estWait));
        }
        sb.append("],");

        // In treatment
        sb.append("\"inTreatmentList\":[");
        for (int i = 0; i < inTreatment.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(inTreatment.get(i).toJson(0, 0));
        }
        sb.append("],");

        // Discharged
        sb.append("\"dischargedList\":[");
        for (int i = 0; i < discharged.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(discharged.get(i).toJson(0, 0));
        }
        sb.append("],");

        // Doctors
        sb.append("\"doctors\":[");
        for (int i = 0; i < allDoctors.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append(allDoctors.get(i).toJson());
        }
        sb.append("]");

        sb.append("}");
        return sb.toString();
    }

    private void serveStaticFile(HttpExchange exchange, String path) throws IOException {
        if (path.equals("/")) path = "/index.html";
        Path filePath = Paths.get("web" + path).normalize();
        if (!filePath.startsWith(Paths.get("web")) || !Files.exists(filePath)) {
            byte[] notFound = "404 Not Found".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(404, notFound.length);
            exchange.getResponseBody().write(notFound);
            exchange.close();
            return;
        }
        String contentType = "text/plain";
        if (path.endsWith(".html")) contentType = "text/html; charset=UTF-8";
        else if (path.endsWith(".css")) contentType = "text/css; charset=UTF-8";
        else if (path.endsWith(".js")) contentType = "application/javascript; charset=UTF-8";

        byte[] bytes = Files.readAllBytes(filePath);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(200, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private void sendJson(HttpExchange exchange, int status, String json) throws IOException {
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private Map<String, String> parseFormBody(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseQueryParams(body);
    }
}
