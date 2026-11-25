package model;

public class Doctor {
    private String doctorId;
    private String name;
    private String specialty;
    private String status; // AVAILABLE or BUSY
    private String currentPatientId;

    public Doctor(String doctorId, String name, String specialty, String status, String currentPatientId) {
        this.doctorId = doctorId;
        this.name = name;
        this.specialty = specialty;
        this.status = status;
        this.currentPatientId = currentPatientId;
    }

    public String getDoctorId() { return doctorId; }
    public void setDoctorId(String doctorId) { this.doctorId = doctorId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSpecialty() { return specialty; }
    public void setSpecialty(String specialty) { this.specialty = specialty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getCurrentPatientId() { return currentPatientId; }
    public void setCurrentPatientId(String currentPatientId) { this.currentPatientId = currentPatientId; }

    public String toJson() {
        String pt = (currentPatientId == null || currentPatientId.isEmpty()) ? "" : currentPatientId;
        return String.format(
            "{\"doctorId\":\"%s\",\"name\":\"%s\",\"specialty\":\"%s\",\"status\":\"%s\",\"currentPatientId\":\"%s\"}",
            Patient.escapeJson(doctorId),
            Patient.escapeJson(name),
            Patient.escapeJson(specialty),
            Patient.escapeJson(status),
            Patient.escapeJson(pt)
        );
    }
}
