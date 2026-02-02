package medochealth.example.medoc_opd_token_Allocation_system.Model;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "tokens",
        indexes = {
                @Index(name = "idx_token_doctor_date", columnList = "doctorId,visitDate"),
                @Index(name = "idx_token_status", columnList = "status")
        }
)
public class Token {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String tokenId;

    @Column(nullable = false)
    private String patientId;

    @Column(nullable = false)
    private String doctorId;

    // Example: 09-10
    @Column(nullable = false)
    private String slot;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BookingType bookingType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TokenStatus status;

    // 0 (Emergency) → 4 (Online)
    @Column(nullable = false)
    private int priority;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // When token was actually served
    private LocalDateTime completedAt;

    // For NO_SHOW / EXPIRED
    private LocalDateTime expiredAt;
    
    // Token number within the slot (e.g., 1, 2, 3...)
    private Integer tokenNumber;
    
    // Emergency flag
    private boolean emergency;
    
    // Original slot if reallocated
    private String originalSlot;
    
    // Cancellation reason
    private String cancellationReason;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.status = TokenStatus.WAITING;
    }
    public enum TokenStatus {
        WAITING,         // Waiting for allocation
        ALLOCATED,        // Allocated to a slot
        IN_PROGRESS,      // Currently being served
        COMPLETED,        // Consultation completed
        CANCELLED,        // Cancelled by patient/system
        EXPIRED,          // Expired (no-show)
        REALLOCATED       // Reallocated to different slot
    }

    public enum BookingType {
        ONLINE,           // Online booking
        WALK_IN,          // Walk-in at OPD desk
        PAID_PRIORITY,    // Paid priority patients
        FOLLOW_UP         // Follow-up patients
    }



    // getters & setters


    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public BookingType getBookingType() {
        return bookingType;
    }

    public void setBookingType(BookingType bookingType) {
        this.bookingType = bookingType;
    }

    public TokenStatus getStatus() {
        return status;
    }

    public void setStatus(TokenStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }

    public Integer getTokenNumber() {
        return tokenNumber;
    }

    public void setTokenNumber(Integer tokenNumber) {
        this.tokenNumber = tokenNumber;
    }

    public boolean isEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
    }

    public String getOriginalSlot() {
        return originalSlot;
    }

    public void setOriginalSlot(String originalSlot) {
        this.originalSlot = originalSlot;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }
}
