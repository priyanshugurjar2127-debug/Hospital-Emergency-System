package db;

import model.Doctor;
import model.Patient;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseManager {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/hospital_emergency_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "root";

    private Connection connection;
    private boolean mysqlConnected = false;
    private static final String LOCAL_PATIENTS_FILE = "data/patients.tsv";
    private static final String LOCAL_DOCTORS_FILE = "data/doctors.tsv";

    public DatabaseManager() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            mysqlConnected = true;
            System.out.println("[DB] Connected to MySQL hospital_emergency_db.");
        } catch (Exception e) {
            mysqlConnected = false;
            System.out.println("[DB] Running in Local Storage Mode (data/patients.tsv & data/doctors.tsv).");
            initLocalFallbackData();
        }
    }

    public boolean isMysqlConnected() {
        return mysqlConnected;
    }

    private void initLocalFallbackData() {
        try {
            Files.createDirectories(Paths.get("data"));
            File docFile = new File(LOCAL_DOCTORS_FILE);
            if (!docFile.exists()) {
                List<Doctor> docs = new ArrayList<>();
                docs.add(new Doctor("DOC-101", "Dr. Aarav Sharma", "Emergency Medicine", "AVAILABLE", ""));
                docs.add(new Doctor("DOC-102", "Dr. Priya Nair", "Pulmonology & Critical Care", "AVAILABLE", ""));
                docs.add(new Doctor("DOC-103", "Dr. Rohan Verma", "Internal Medicine", "AVAILABLE", ""));
                docs.add(new Doctor("DOC-104", "Dr. Meera Joshi", "Trauma & ICU", "AVAILABLE", ""));
                saveAllDoctorsLocal(docs);
            }

            File patFile = new File(LOCAL_PATIENTS_FILE);
            if (!patFile.exists()) {
                List<Patient> pats = new ArrayList<>();
                pats.add(new Patient("PT-1001", "Rajesh Kumar", 62, "Severe hypoxia, SpO2 84%, high fever", 1, 1L, "09:10 AM", "WAITING", "", ""));
                pats.add(new Patient("PT-1002", "Sunita Devi", 48, "Persistent chest tightness & cough", 2, 2L, "09:18 AM", "WAITING", "", ""));
                pats.add(new Patient("PT-1003", "Ankit Singh", 29, "Moderate fever and body ache", 3, 3L, "09:25 AM", "WAITING", "", ""));
                pats.add(new Patient("PT-1004", "Kavita Sharma", 71, "Acute respiratory distress, tachycardia", 1, 4L, "09:32 AM", "WAITING", "", ""));
                pats.add(new Patient("PT-1005", "Mohit Agarwal", 34, "Mild sore throat and fatigue", 4, 5L, "09:40 AM", "WAITING", "", ""));
                saveAllPatientsLocal(pats);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public List<Doctor> loadAllDoctors() {
        List<Doctor> list = new ArrayList<>();
        if (mysqlConnected) {
            String sql = "SELECT doctor_id, name, specialty, status, current_patient_id FROM doctors";
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Doctor(
                        rs.getString("doctor_id"),
                        rs.getString("name"),
                        rs.getString("specialty"),
                        rs.getString("status"),
                        rs.getString("current_patient_id")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(LOCAL_DOCTORS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] p = line.split("\t", -1);
                    if (p.length >= 5) {
                        list.add(new Doctor(p[0], p[1], p[2], p[3], p[4]));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public List<Patient> loadAllPatients() {
        List<Patient> list = new ArrayList<>();
        if (mysqlConnected) {
            String sql = "SELECT patient_id, name, age, symptoms, emergency_level, arrival_order, registered_at, status, assigned_doctor_id, assigned_doctor_name FROM patients";
            try (Statement st = connection.createStatement(); ResultSet rs = st.executeQuery(sql)) {
                while (rs.next()) {
                    list.add(new Patient(
                        rs.getString("patient_id"),
                        rs.getString("name"),
                        rs.getInt("age"),
                        rs.getString("symptoms"),
                        rs.getInt("emergency_level"),
                        rs.getLong("arrival_order"),
                        rs.getString("registered_at"),
                        rs.getString("status"),
                        rs.getString("assigned_doctor_id"),
                        rs.getString("assigned_doctor_name")
                    ));
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            try (BufferedReader br = new BufferedReader(new FileReader(LOCAL_PATIENTS_FILE))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    String[] p = line.split("\t", -1);
                    if (p.length >= 10) {
                        list.add(new Patient(
                            p[0], p[1], Integer.parseInt(p[2]), p[3],
                            Integer.parseInt(p[4]), Long.parseLong(p[5]),
                            p[6], p[7], p[8], p[9]
                        ));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return list;
    }

    public void syncState(List<Patient> allPatients, List<Doctor> allDoctors) {
        if (mysqlConnected) {
            try {
                String patSql = "INSERT INTO patients (patient_id, name, age, symptoms, emergency_level, arrival_order, registered_at, status, assigned_doctor_id, assigned_doctor_name) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                                "name=VALUES(name), age=VALUES(age), symptoms=VALUES(symptoms), emergency_level=VALUES(emergency_level), " +
                                "status=VALUES(status), assigned_doctor_id=VALUES(assigned_doctor_id), assigned_doctor_name=VALUES(assigned_doctor_name)";
                try (PreparedStatement ps = connection.prepareStatement(patSql)) {
                    for (Patient p : allPatients) {
                        ps.setString(1, p.getPatientId());
                        ps.setString(2, p.getName());
                        ps.setInt(3, p.getAge());
                        ps.setString(4, p.getSymptoms());
                        ps.setInt(5, p.getEmergencyLevel());
                        ps.setLong(6, p.getArrivalOrder());
                        ps.setString(7, p.getRegisteredAt());
                        ps.setString(8, p.getStatus());
                        ps.setString(9, p.getAssignedDoctorId());
                        ps.setString(10, p.getAssignedDoctorName());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }

                String docSql = "INSERT INTO doctors (doctor_id, name, specialty, status, current_patient_id) " +
                                "VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE " +
                                "name=VALUES(name), specialty=VALUES(specialty), status=VALUES(status), current_patient_id=VALUES(current_patient_id)";
                try (PreparedStatement ps = connection.prepareStatement(docSql)) {
                    for (Doctor d : allDoctors) {
                        ps.setString(1, d.getDoctorId());
                        ps.setString(2, d.getName());
                        ps.setString(3, d.getSpecialty());
                        ps.setString(4, d.getStatus());
                        ps.setString(5, d.getCurrentPatientId());
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            saveAllPatientsLocal(allPatients);
            saveAllDoctorsLocal(allDoctors);
        }
    }

    private void saveAllPatientsLocal(List<Patient> patients) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOCAL_PATIENTS_FILE))) {
            for (Patient p : patients) {
                pw.printf("%s\t%s\t%d\t%s\t%d\t%d\t%s\t%s\t%s\t%s%n",
                    clean(p.getPatientId()),
                    clean(p.getName()),
                    p.getAge(),
                    clean(p.getSymptoms()),
                    p.getEmergencyLevel(),
                    p.getArrivalOrder(),
                    clean(p.getRegisteredAt()),
                    clean(p.getStatus()),
                    clean(p.getAssignedDoctorId()),
                    clean(p.getAssignedDoctorName())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveAllDoctorsLocal(List<Doctor> doctors) {
        try (PrintWriter pw = new PrintWriter(new FileWriter(LOCAL_DOCTORS_FILE))) {
            for (Doctor d : doctors) {
                pw.printf("%s\t%s\t%s\t%s\t%s%n",
                    clean(d.getDoctorId()),
                    clean(d.getName()),
                    clean(d.getSpecialty()),
                    clean(d.getStatus()),
                    clean(d.getCurrentPatientId())
                );
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String clean(String val) {
        return (val == null) ? "" : val.replace("\t", " ").replace("\n", " ").replace("\r", " ");
    }
}
