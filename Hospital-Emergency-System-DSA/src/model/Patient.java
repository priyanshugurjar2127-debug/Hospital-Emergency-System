package model;

public class Patient {
    private String patientId;
    private String name;
    private int age;
    private String symptoms;
    private int emergencyLevel; // 1 = Critical, 2 = Severe, 3 = Moderate, 4 = Mild
    private long arrivalOrder;
    private String registeredAt;
    private String status; // WAITING, IN_TREATMENT, DISCHARGED
    private String assignedDoctorId;
    private String assignedDoctorName;

    public Patient(String patientId, String name, int age, String symptoms, int emergencyLevel,
                   long arrivalOrder, String registeredAt, String status,
                   String assignedDoctorId, String assignedDoctorName) {
        this.patientId = patientId;
        this.name = name;
        this.age = age;
        this.symptoms = symptoms;
        this.emergencyLevel = emergencyLevel;
        this.arrivalOrder = arrivalOrder;
        this.registeredAt = registeredAt;
        this.status = status;
        this.assignedDoctorId = assignedDoctorId;
        this.assignedDoctorName = assignedDoctorName;
    }

    public String getPatientId() { return patientId; }
    public void setPatientId(String patientId) { this.patientId = patientId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    public String getSymptoms() { return symptoms; }
    public void setSymptoms(String symptoms) { this.symptoms = symptoms; }

    public int getEmergencyLevel() { return emergencyLevel; }
    public void setEmergencyLevel(int emergencyLevel) { this.emergencyLevel = emergencyLevel; }

    public long getArrivalOrder() { return arrivalOrder; }
    public void setArrivalOrder(long arrivalOrder) { this.arrivalOrder = arrivalOrder; }

    public String getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(String registeredAt) { this.registeredAt = registeredAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAssignedDoctorId() { return assignedDoctorId; }
    public void setAssignedDoctorId(String assignedDoctorId) { this.assignedDoctorId = assignedDoctorId; }

    public String getAssignedDoctorName() { return assignedDoctorName; }
    public void setAssignedDoctorName(String assignedDoctorName) { this.assignedDoctorName = assignedDoctorName; }

    public String getPriorityLabel() {
        switch (emergencyLevel) {
            case 1: return "Critical (Level 1)";
            case 2: return "Severe (Level 2)";
            case 3: return "Moderate (Level 3)";
            default: return "Mild (Level 4)";
        }
    }

    public String toJson(int queuePosition, int estimatedWaitMins) {
        String docId = (assignedDoctorId == null) ? "" : assignedDoctorId;
        String docName = (assignedDoctorName == null) ? "" : assignedDoctorName;
        return String.format(
            "{\"patientId\":\"%s\",\"name\":\"%s\",\"age\":%d,\"symptoms\":\"%s\",\"emergencyLevel\":%d,\"priorityLabel\":\"%s\",\"registeredAt\":\"%s\",\"status\":\"%s\",\"assignedDoctorId\":\"%s\",\"assignedDoctorName\":\"%s\",\"queuePosition\":%d,\"estimatedWaitMins\":%d}",
            escapeJson(patientId),
            escapeJson(name),
            age,
            escapeJson(symptoms),
            emergencyLevel,
            escapeJson(getPriorityLabel()),
            escapeJson(registeredAt),
            escapeJson(status),
            escapeJson(docId),
            escapeJson(docName),
            queuePosition,
            estimatedWaitMins
        );
    }

    public static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }
}
