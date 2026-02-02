package medochealth.example.medoc_opd_token_Allocation_system.Service;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Doctor;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.TokenStatus;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.DoctorRepository;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class TokenReallocationService {

    private final TokenRepository tokenRepository;
    private final DoctorRepository doctorRepository;
    private final SlotCapacityService slotCapacityService;
    private final SlotQueueService slotQueueService;

    public TokenReallocationService(
            TokenRepository tokenRepository,
            DoctorRepository doctorRepository,
            SlotCapacityService slotCapacityService,
            SlotQueueService slotQueueService) {
        this.tokenRepository = tokenRepository;
        this.doctorRepository = doctorRepository;
        this.slotCapacityService = slotCapacityService;
        this.slotQueueService = slotQueueService;
    }

    /**
     * Find alternative slot when requested slot is full
     */
    public Optional<String> findAlternativeSlot(String doctorId, LocalDate visitDate, String requestedSlot) {
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            return Optional.empty();
        }

        Doctor doctor = doctorOpt.get();
        Set<String> availableSlots = doctor.getSlots();
        
        // Parse requested slot time
        LocalTime requestedTime = parseSlotTime(requestedSlot);
        if (requestedTime == null) {
            return Optional.empty();
        }

        // Find slots with available capacity, sorted by time proximity
        return availableSlots.stream()
            .filter(slot -> !slot.equals(requestedSlot))
            .filter(slot -> slotCapacityService.hasAvailableCapacity(doctorId, slot, visitDate))
            .min(Comparator.comparing(slot -> {
                LocalTime slotTime = parseSlotTime(slot);
                if (slotTime == null) return Long.MAX_VALUE;
                return Math.abs(java.time.Duration.between(requestedTime, slotTime).toMinutes());
            }));
    }

    /**
     * Reallocate tokens from waiting queue when capacity becomes available
     */
    @Transactional
    public void processWaitingQueue(String doctorId, String slot, LocalDate visitDate) {
        String slotId = getSlotId(doctorId, slot, visitDate);
        
        while (slotCapacityService.hasAvailableCapacity(doctorId, slot, visitDate)) {
            String tokenId = slotQueueService.pollNextWaiting(slotId);
            if (tokenId == null) {
                break; // No more waiting tokens
            }

            Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
            if (tokenOpt.isEmpty()) {
                continue;
            }

            Token token = tokenOpt.get();
            if (token.getStatus() != TokenStatus.WAITING) {
                continue;
            }

            // Allocate the token
            token.setStatus(TokenStatus.ALLOCATED);
            token.setSlot(slot);
            
            // Assign token number
            Integer maxTokenNumber = tokenRepository.findMaxTokenNumber(doctorId, visitDate, slot);
            token.setTokenNumber((maxTokenNumber == null ? 0 : maxTokenNumber) + 1);
            
            tokenRepository.save(token);
            slotQueueService.addToAllocatedQueue(slotId, token);
        }
    }

    /**
     * Handle cancellation - reallocate waiting tokens if possible
     */
    @Transactional
    public boolean cancelToken(String tokenId, String reason) {
        Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        Token token = tokenOpt.get();
        if (token.getStatus() == TokenStatus.COMPLETED || token.getStatus() == TokenStatus.CANCELLED) {
            return false;
        }

        // Update token status
        token.setStatus(TokenStatus.CANCELLED);
        token.setCancellationReason(reason);
        tokenRepository.save(token);

        // Remove from allocated queue
        String slotId = getSlotId(token.getDoctorId(), token.getSlot(), token.getVisitDate());
        slotQueueService.removeFromAllocatedQueue(slotId, tokenId);

        // Process waiting queue to fill the vacancy
        processWaitingQueue(token.getDoctorId(), token.getSlot(), token.getVisitDate());

        return true;
    }

    /**
     * Handle no-show - mark as expired and reallocate
     */
    @Transactional
    public boolean markNoShow(String tokenId) {
        Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        Token token = tokenOpt.get();
        if (token.getStatus() == TokenStatus.COMPLETED || token.getStatus() == TokenStatus.EXPIRED) {
            return false;
        }

        // Update token status
        token.setStatus(TokenStatus.EXPIRED);
        token.setExpiredAt(java.time.LocalDateTime.now());
        tokenRepository.save(token);

        // Remove from allocated queue
        String slotId = getSlotId(token.getDoctorId(), token.getSlot(), token.getVisitDate());
        slotQueueService.removeFromAllocatedQueue(slotId, tokenId);

        // Process waiting queue
        processWaitingQueue(token.getDoctorId(), token.getSlot(), token.getVisitDate());

        return true;
    }

    /**
     * Handle emergency insertion - insert at appropriate position
     */
    @Transactional
    public boolean insertEmergencyToken(String tokenId) {
        Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        Token token = tokenOpt.get();
        if (!token.isEmergency()) {
            return false;
        }

        // Check if slot has emergency capacity
        if (!slotCapacityService.hasEmergencyCapacity(
            token.getDoctorId(), token.getSlot(), token.getVisitDate())) {
            return false;
        }

        // Emergency tokens get highest priority (0) and are inserted at the front
        token.setStatus(TokenStatus.ALLOCATED);
        token.setPriority(0);
        
        // Get all allocated tokens for this slot and shift numbers
        List<Token> allocatedTokens = tokenRepository.findByDoctorIdAndVisitDateAndSlotAndStatus(
            token.getDoctorId(), token.getVisitDate(), token.getSlot(), TokenStatus.ALLOCATED
        );
        
        // Emergency gets token number 1, shift others
        token.setTokenNumber(1);
        allocatedTokens.forEach(t -> {
            if (t.getTokenNumber() != null) {
                t.setTokenNumber(t.getTokenNumber() + 1);
                tokenRepository.save(t);
            }
        });
        
        tokenRepository.save(token);
        slotQueueService.addToAllocatedQueue(
            getSlotId(token.getDoctorId(), token.getSlot(), token.getVisitDate()), 
            token
        );

        return true;
    }

    /**
     * Handle delay - adjust subsequent tokens
     */
    @Transactional
    public void handleDelay(String doctorId, String slot, LocalDate visitDate, int delayMinutes) {
        // In a real system, this would notify patients and adjust schedules
        // For now, we just log the delay
        // Could trigger notifications to subsequent patients
    }

    private LocalTime parseSlotTime(String slot) {
        try {
            String[] parts = slot.split("-");
            if (parts.length != 2) {
                return null;
            }
            int hour = Integer.parseInt(parts[0]);
            return LocalTime.of(hour, 0);
        } catch (Exception e) {
            return null;
        }
    }

    private String getSlotId(String doctorId, String slot, LocalDate visitDate) {
        return String.format("%s:%s:%s", doctorId, slot, visitDate);
    }
}

