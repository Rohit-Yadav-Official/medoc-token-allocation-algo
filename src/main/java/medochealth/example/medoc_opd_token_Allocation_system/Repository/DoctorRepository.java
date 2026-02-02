package medochealth.example.medoc_opd_token_Allocation_system.Repository;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, String> {

   // List<Doctor> findByActiveTrue();
}

