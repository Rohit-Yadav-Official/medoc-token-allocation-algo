package medochealth.example.medoc_opd_token_Allocation_system.Model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Emergency / close relative contact
    private String emergencyContactName;
    private String emergencyContactNumber;
    private String emergencyContactRelation;

    // Default / primary doctor (can change per visit)
    private String primaryDoctorId;

    // Lightweight summary only (full records in EMR service)
    @Column(length = 1000)
    private String medicalHistorySummary;

    // Token tracking
    private String lastTokenId;
    private String nextTokenId;

    @Enumerated(EnumType.STRING)
    private PatientStatus status;

    // Optional but VERY useful
    private int noShowCount;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = PatientStatus.ACTIVE;
        this.noShowCount = 0;
    }
    public enum PatientStatus {
        ACTIVE,
        BLOCKED,
        DECEASED
    }

    // getters & setters omitted for brevity


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getEmergencyContactName() {
        return emergencyContactName;
    }

    public void setEmergencyContactName(String emergencyContactName) {
        this.emergencyContactName = emergencyContactName;
    }

    public String getEmergencyContactNumber() {
        return emergencyContactNumber;
    }

    public void setEmergencyContactNumber(String emergencyContactNumber) {
        this.emergencyContactNumber = emergencyContactNumber;
    }

    public String getEmergencyContactRelation() {
        return emergencyContactRelation;
    }

    public void setEmergencyContactRelation(String emergencyContactRelation) {
        this.emergencyContactRelation = emergencyContactRelation;
    }

    public String getPrimaryDoctorId() {
        return primaryDoctorId;
    }

    public void setPrimaryDoctorId(String primaryDoctorId) {
        this.primaryDoctorId = primaryDoctorId;
    }

    public String getMedicalHistorySummary() {
        return medicalHistorySummary;
    }

    public void setMedicalHistorySummary(String medicalHistorySummary) {
        this.medicalHistorySummary = medicalHistorySummary;
    }

    public String getLastTokenId() {
        return lastTokenId;
    }

    public void setLastTokenId(String lastTokenId) {
        this.lastTokenId = lastTokenId;
    }

    public String getNextTokenId() {
        return nextTokenId;
    }

    public void setNextTokenId(String nextTokenId) {
        this.nextTokenId = nextTokenId;
    }

    public PatientStatus getStatus() {
        return status;
    }

    public void setStatus(PatientStatus status) {
        this.status = status;
    }

    public int getNoShowCount() {
        return noShowCount;
    }

    public void setNoShowCount(int noShowCount) {
        this.noShowCount = noShowCount;
    }
}
