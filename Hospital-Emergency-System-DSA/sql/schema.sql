CREATE DATABASE IF NOT EXISTS hospital_emergency_db;
USE hospital_emergency_db;

CREATE TABLE IF NOT EXISTS doctors (
    doctor_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    specialty VARCHAR(80) NOT NULL,
    status ENUM('AVAILABLE', 'BUSY') NOT NULL DEFAULT 'AVAILABLE',
    current_patient_id VARCHAR(20) DEFAULT NULL
);

CREATE TABLE IF NOT EXISTS patients (
    patient_id VARCHAR(20) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    age INT NOT NULL,
    symptoms VARCHAR(255) NOT NULL,
    emergency_level INT NOT NULL,
    arrival_order BIGINT NOT NULL,
    registered_at VARCHAR(40) NOT NULL,
    status ENUM('WAITING', 'IN_TREATMENT', 'DISCHARGED') NOT NULL DEFAULT 'WAITING',
    assigned_doctor_id VARCHAR(20) DEFAULT NULL,
    assigned_doctor_name VARCHAR(100) DEFAULT NULL
);

INSERT IGNORE INTO doctors (doctor_id, name, specialty, status, current_patient_id) VALUES
('DOC-101', 'Dr. Aarav Sharma', 'Emergency Medicine', 'AVAILABLE', NULL),
('DOC-102', 'Dr. Priya Nair', 'Pulmonology & Critical Care', 'AVAILABLE', NULL),
('DOC-103', 'Dr. Rohan Verma', 'Internal Medicine', 'AVAILABLE', NULL),
('DOC-104', 'Dr. Meera Joshi', 'Trauma & ICU', 'AVAILABLE', NULL);

INSERT IGNORE INTO patients (patient_id, name, age, symptoms, emergency_level, arrival_order, registered_at, status, assigned_doctor_id, assigned_doctor_name) VALUES
('PT-1001', 'Rajesh Kumar', 62, 'Severe hypoxia, SpO2 84%, high fever', 1, 1, '09:10 AM', 'WAITING', NULL, NULL),
('PT-1002', 'Sunita Devi', 48, 'Persistent chest tightness & cough', 2, 2, '09:18 AM', 'WAITING', NULL, NULL),
('PT-1003', 'Ankit Singh', 29, 'Moderate fever and body ache', 3, 3, '09:25 AM', 'WAITING', NULL, NULL),
('PT-1004', 'Kavita Sharma', 71, 'Acute respiratory distress, tachycardia', 1, 4, '09:32 AM', 'WAITING', NULL, NULL),
('PT-1005', 'Mohit Agarwal', 34, 'Mild sore throat and fatigue', 4, 5, '09:40 AM', 'WAITING', NULL, NULL);
