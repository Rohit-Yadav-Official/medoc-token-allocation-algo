package medochealth.example.medoc_opd_token_Allocation_system.DTO;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.BookingType;
import java.time.LocalDate;

public class TokenRequestDTO {
    private String patientId;
    private String doctorId;
    private String slot;  // e.g., "09-10"
    private LocalDate visitDate;
    private BookingType bookingType;
    private boolean emergency;

    // Getters and Setters
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

    public boolean isEmergency() {
        return emergency;
    }

    public void setEmergency(boolean emergency) {
        this.emergency = emergency;
    }
}

