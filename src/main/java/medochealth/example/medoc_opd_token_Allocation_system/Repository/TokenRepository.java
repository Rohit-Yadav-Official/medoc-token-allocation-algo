package medochealth.example.medoc_opd_token_Allocation_system.Repository;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Token;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TokenRepository extends JpaRepository<Token, String> {
    Optional<Token> findByTokenId(String tokenId);
    
    List<Token> findByDoctorIdAndVisitDateAndStatus(String doctorId, LocalDate visitDate, TokenStatus status);

    List<Token> findByDoctorIdAndVisitDateAndSlotAndStatusIn(
            String doctorId,
            LocalDate visitDate,
            String slot,
            List<TokenStatus> status
    );
    List<Token> findByDoctorIdAndVisitDateAndSlotAndStatus( String doctorId, LocalDate visitDate, String slot, TokenStatus status );

    @Query("SELECT COUNT(t) FROM Token t WHERE t.doctorId = :doctorId AND t.visitDate = :visitDate " +
           "AND t.slot = :slot AND t.status IN :statuses")
    long countByDoctorIdAndVisitDateAndSlotAndStatusIn(
        @Param("doctorId") String doctorId,
        @Param("visitDate") LocalDate visitDate,
        @Param("slot") String slot,
        @Param("statuses") List<TokenStatus> statuses
    );
    
    @Query("SELECT MAX(t.tokenNumber) FROM Token t WHERE t.doctorId = :doctorId AND t.visitDate = :visitDate " +
           "AND t.slot = :slot")
    Integer findMaxTokenNumber(@Param("doctorId") String doctorId, 
                               @Param("visitDate") LocalDate visitDate, 
                               @Param("slot") String slot);
    
    List<Token> findByPatientIdAndVisitDateAndStatusIn(String patientId, LocalDate visitDate, List<TokenStatus> statuses);
}

