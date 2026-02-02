package medochealth.example.medoc_opd_token_Allocation_system.Controller;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Doctor;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Patient;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.PatientRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/patients")
public class PatientController {

    private final PatientRepository patientRepository;

    public PatientController(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @GetMapping("/{patientId}")
    public ResponseEntity<Patient> getPatient(@PathVariable String patientId) {
        try {
            Optional<Patient> patient = patientRepository.findById(patientId);
            return patient.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        }
        catch (Exception e) {
            return ResponseEntity.notFound().build();

        }
    }

    @GetMapping()
    public ResponseEntity<List<Patient>> getallPatient() {
        try {
            List<Patient> patients = patientRepository.findAll();
            return ResponseEntity.ok(patients);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }

    @PostMapping
    public ResponseEntity<Patient> createPatient(@RequestBody Patient patient) {
        try {
            Patient saved = patientRepository.save(patient);
            return ResponseEntity.ok(saved);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}

