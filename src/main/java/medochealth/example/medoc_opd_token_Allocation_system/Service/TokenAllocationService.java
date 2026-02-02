package medochealth.example.medoc_opd_token_Allocation_system.Service;

import medochealth.example.medoc_opd_token_Allocation_system.DTO.TokenRequestDTO;
import medochealth.example.medoc_opd_token_Allocation_system.DTO.TokenResponseDTO;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Doctor;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Patient;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.BookingType;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.TokenStatus;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.DoctorRepository;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.PatientRepository;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.TokenRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class TokenAllocationService {

    private final TokenRepository tokenRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SlotCapacityService slotCapacityService;
    private final SlotQueueService slotQueueService;
    private final TokenReallocationService tokenReallocationService;

    public TokenAllocationService(
            TokenRepository tokenRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            SlotCapacityService slotCapacityService,
            SlotQueueService slotQueueService,
            TokenReallocationService tokenReallocationService) {
        this.tokenRepository = tokenRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.slotCapacityService = slotCapacityService;
        this.slotQueueService = slotQueueService;
        this.tokenReallocationService = tokenReallocationService;
    }

    /**
     * Core algorithm for token allocation with prioritization
     * Priority order: Emergency (0) > Paid Priority (1) > Follow-up (2) > Walk-in (3) > Online (4)
     */
    @Transactional
    public TokenResponseDTO allocateToken(TokenRequestDTO request) {

        try{
        // Validate doctor exists
        Optional<Doctor> doctorOpt = doctorRepository.findById(request.getDoctorId());
        if (doctorOpt.isEmpty() || !doctorOpt.get().isActive()) {
            return createErrorResponse("Doctor not found or inactive");
        }

        // Validate patient exists
        Optional<Patient> patientOpt = patientRepository.findById(request.getPatientId());
        if (patientOpt.isEmpty()) {
            return createErrorResponse("Patient not found");
        }

        Doctor doctor = doctorOpt.get();
        // Patient validation - patient exists (checked above)

        // Validate slot exists for doctor
        if (!doctor.getSlots().contains(request.getSlot())) {
            return createErrorResponse("Slot not available for this doctor");
        }

        // Check if patient already has a token for this date
        List<Token> existingTokens = tokenRepository.findByPatientIdAndVisitDateAndStatusIn(
            request.getPatientId(),
            request.getVisitDate(),
            List.of(TokenStatus.WAITING, TokenStatus.ALLOCATED, TokenStatus.IN_PROGRESS)
        );
        
        if (!existingTokens.isEmpty()) {
            return createErrorResponse("Patient already has an active token for this date");
        }

        // Calculate priority based on booking type and emergency status
        int priority = calculatePriority(request.getBookingType(), request.isEmergency());

        // Check capacity
        boolean hasCapacity;
        if (request.isEmergency()) {
            hasCapacity = slotCapacityService.hasEmergencyCapacity(
                request.getDoctorId(), request.getSlot(), request.getVisitDate()
            );
        } else {
            hasCapacity = slotCapacityService.hasAvailableCapacity(
                request.getDoctorId(), request.getSlot(), request.getVisitDate()
            );
        }

        String allocatedSlot = request.getSlot();
        TokenResponseDTO response = new TokenResponseDTO();

        // If no capacity in requested slot, try to reallocate
        if (!hasCapacity && !request.isEmergency()) {
            Optional<String> alternativeSlot = tokenReallocationService.findAlternativeSlot(
                request.getDoctorId(), request.getVisitDate(), request.getSlot()
            );
            
            if (alternativeSlot.isPresent()) {
                allocatedSlot = alternativeSlot.get();
                response.setMessage("Requested slot full. Allocated to alternative slot: " + allocatedSlot);
            } else {
                // Add to waiting queue
                Token token = createToken(request, priority, allocatedSlot);
                token.setStatus(TokenStatus.WAITING);
                token = tokenRepository.save(token);
                slotQueueService.addToWaitingQueue(getSlotId(request.getDoctorId(), allocatedSlot, request.getVisitDate()), token);
                
                response = convertToDTO(token);
                response.setMessage("Slot full. Token added to waiting queue.");
                return response;
            }
        }

        // Create and allocate token
        Token token = createToken(request, priority, allocatedSlot);

        
        // Assign token number
        Integer maxTokenNumber = tokenRepository.findMaxTokenNumber(
            request.getDoctorId(), request.getVisitDate(), allocatedSlot
        );
        token.setTokenNumber((maxTokenNumber == null ? 0 : maxTokenNumber) + 1);
        token.setStatus(TokenStatus.ALLOCATED);
        token = tokenRepository.save(token);
        slotQueueService.addToAllocatedQueue(
            getSlotId(request.getDoctorId(), allocatedSlot, request.getVisitDate()), 
            token
        );

        response = convertToDTO(token);
        if (response.getMessage() == null) {
            response.setMessage("Token allocated successfully");
            response.setStatus(TokenStatus.ALLOCATED);
        }
        
        return response;
        }
        catch (Exception e) {
            return createErrorResponse(e.getMessage());
        }
    }

    /**
     * Calculate priority: Lower number = Higher priority
     * Emergency: 0
     * Paid Priority: 1
     * Follow-up: 2
     * Walk-in: 3
     * Online: 4
     */
    private int calculatePriority(BookingType bookingType, boolean emergency) {
        if (emergency) {
            return 0;
        }
        
        return switch (bookingType) {
            case PAID_PRIORITY -> 1;
            case FOLLOW_UP -> 2;
            case WALK_IN -> 3;
            case ONLINE -> 4;
        };
    }

    private Token createToken(TokenRequestDTO request, int priority, String slot) {
        Token token = new Token();
        token.setPatientId(request.getPatientId());
        token.setDoctorId(request.getDoctorId());
        token.setSlot(slot);
        token.setVisitDate(request.getVisitDate());
        token.setBookingType(request.getBookingType());
        token.setPriority(priority);
        token.setEmergency(request.isEmergency());
        return token;
    }

    private TokenResponseDTO convertToDTO(Token token) {
        TokenResponseDTO dto = new TokenResponseDTO();
        dto.setTokenId(token.getTokenId());
        dto.setPatientId(token.getPatientId());
        dto.setDoctorId(token.getDoctorId());
        dto.setSlot(token.getSlot());
        dto.setVisitDate(token.getVisitDate());
        dto.setBookingType(token.getBookingType());
        dto.setStatus(token.getStatus());
        dto.setPriority(token.getPriority());
        dto.setTokenNumber(token.getTokenNumber());
        dto.setEmergency(token.isEmergency());
        dto.setCreatedAt(token.getCreatedAt());
        return dto;
    }

    private TokenResponseDTO createErrorResponse(String message) {
        TokenResponseDTO response = new TokenResponseDTO();
        response.setMessage(message);
        return response;
    }

    private String getSlotId(String doctorId, String slot, LocalDate visitDate) {
        return String.format("%s:%s:%s", doctorId, slot, visitDate);
    }
}

