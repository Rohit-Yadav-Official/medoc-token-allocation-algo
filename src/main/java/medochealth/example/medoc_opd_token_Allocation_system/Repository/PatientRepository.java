package medochealth.example.medoc_opd_token_Allocation_system.Repository;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, String> {
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    Optional<Patient> findByEmail(String email);
}

