package medochealth.example.medoc_opd_token_Allocation_system.DTO;

import java.time.LocalDate;

public class SlotCapacityDTO {
    private String doctorId;
    private String slot;
    private LocalDate visitDate;
    private int maxCapacity;
    private int currentAllocated;
    private int availableSlots;

    // Getters and Setters
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

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public int getCurrentAllocated() {
        return currentAllocated;
    }

    public void setCurrentAllocated(int currentAllocated) {
        this.currentAllocated = currentAllocated;
    }

    public int getAvailableSlots() {
        return availableSlots;
    }

    public void setAvailableSlots(int availableSlots) {
        this.availableSlots = availableSlots;
    }
}

