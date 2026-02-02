package medochealth.example.medoc_opd_token_Allocation_system.Controller;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Doctor;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.DoctorRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        try{
        List<Doctor> doctors = doctorRepository.findAll();
        return ResponseEntity.ok(doctors);}
        catch (DataIntegrityViolationException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).build();

        }

    }

    // Get doctor by ID
    @GetMapping("/{doctorId}")
    public ResponseEntity<Doctor> getDoctor(@PathVariable String doctorId) {
        try{
            Optional<Doctor> doctor = doctorRepository.findById(doctorId);
        return doctor
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
        }
        catch (DataIntegrityViolationException e){
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    // Create doctor
    @PostMapping
    public ResponseEntity<Doctor> createDoctor(@RequestBody Doctor doc) {
        try {
            Doctor savedDoctor = doctorRepository.save(doc);
            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(savedDoctor);

        } catch (DataIntegrityViolationException e) {
            // DB constraint issues (null, unique, FK, etc.)
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();

        } catch (IllegalArgumentException e) {
            // Logical inconsistency
            return ResponseEntity
                    .status(HttpStatus.BAD_REQUEST)
                    .build();

        } catch (Exception e) {
            // Fallback (never expose internal error)
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
