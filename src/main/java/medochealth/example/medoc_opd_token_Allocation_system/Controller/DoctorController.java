package medochealth.example.medoc_opd_token_Allocation_system.Controller;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Doctor;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.DoctorRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/doctors")
public class DoctorController {

    private final DoctorRepository doctorRepository;

    public DoctorController(DoctorRepository doctorRepository) {
        this.doctorRepository = doctorRepository;
    }

    // Get all active doctors
    @GetMapping
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        List<Doctor> doctors = doctorRepository.findAll();
        return ResponseEntity.ok(doctors);
    }

    // Get doctor by ID
    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable String doctorId) {
        Optional<Doctor> doctor = doctorRepository.findById(doctorId);
        return doctor
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Create doctor
    @PostMapping
    public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doc) {
        Doctor savedDoctor = doctorRepository.save(doc);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedDoctor);
    }
}
