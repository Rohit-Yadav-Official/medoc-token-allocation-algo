package medochealth.example.medoc_opd_token_Allocation_system.Model;

import jakarta.persistence.*;
import java.util.Set;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @Column(name = "doctor_id")
    private String id;   // e.g. D1, D102

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String speciality;

    // Example: 09-10, 13-14, 18-19
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "doctor_slots",
            joinColumns = @JoinColumn(name = "doctor_id")
    )
    @Column(name = "slot_time")
    private Set<String> slots;

    // MON, WED, FRI
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "doctor_days",
            joinColumns = @JoinColumn(name = "doctor_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "working_day")
    private Set<WorkingDay> workingDays;

    @Column(nullable = false)
    private String phoneNumber;

    @Column(unique = true)
    private String email;

    private String designation;

    @Column(nullable = false)
    private boolean active;

    @PrePersist
    protected void onCreate() {
        this.active = true;
    }
    public enum WorkingDay {
        MON,

        WED,
        FRI


    }


    // getters & setters


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

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public Set<String> getSlots() {
        return slots;
    }

    public void setSlots(Set<String> slots) {
        this.slots = slots;
    }

    public Set<WorkingDay> getWorkingDays() {
        return workingDays;
    }

    public void setWorkingDays(Set<WorkingDay> workingDays) {
        this.workingDays = workingDays;
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

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
