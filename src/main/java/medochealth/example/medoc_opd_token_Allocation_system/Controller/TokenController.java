package medochealth.example.medoc_opd_token_Allocation_system.Controller;

import medochealth.example.medoc_opd_token_Allocation_system.DTO.SlotCapacityDTO;
import medochealth.example.medoc_opd_token_Allocation_system.DTO.TokenRequestDTO;
import medochealth.example.medoc_opd_token_Allocation_system.DTO.TokenResponseDTO;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.TokenStatus;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.TokenRepository;
import medochealth.example.medoc_opd_token_Allocation_system.Service.SlotCapacityService;
import medochealth.example.medoc_opd_token_Allocation_system.Service.TokenAllocationService;
import medochealth.example.medoc_opd_token_Allocation_system.Service.TokenReallocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/tokens")
public class TokenController {

    private final TokenAllocationService tokenAllocationService;
    private final TokenReallocationService tokenReallocationService;
    private final SlotCapacityService slotCapacityService;
    private final TokenRepository tokenRepository;

    public TokenController(
            TokenAllocationService tokenAllocationService,
            TokenReallocationService tokenReallocationService,
            SlotCapacityService slotCapacityService,
            TokenRepository tokenRepository) {
        this.tokenAllocationService = tokenAllocationService;
        this.tokenReallocationService = tokenReallocationService;
        this.slotCapacityService = slotCapacityService;
        this.tokenRepository = tokenRepository;
    }


    @PostMapping("/allocate")
    public ResponseEntity<TokenResponseDTO> allocateToken(@RequestBody TokenRequestDTO request) {

       try{ TokenResponseDTO response = tokenAllocationService.allocateToken(request);
        
        if (response.getMessage() != null && response.getMessage().contains("not found") || 
            response.getMessage().contains("inactive") || 
            response.getMessage().contains("not available")) {
            return ResponseEntity.badRequest().body(response);
        }
        
        return ResponseEntity.ok(response);
       }
       catch (Exception e) {
           e.printStackTrace();
           return ResponseEntity.badRequest().build();
       }

    }


    @GetMapping("/{tokenId}")
    public ResponseEntity<TokenResponseDTO> getToken(@PathVariable String tokenId) {
        try {
            Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
            if (tokenOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Token token = tokenOpt.get();
            TokenResponseDTO dto = convertToDTO(token);
            return ResponseEntity.ok(dto);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/doctor/{doctorId}/date/{date}/slot/{slot}")
    public ResponseEntity<List<Token>> getTokensBySlot(
            @PathVariable String doctorId,
            @PathVariable LocalDate date,
            @PathVariable String slot) {
        try {


            List<TokenStatus> statuses = List.of(
                    TokenStatus.ALLOCATED,
                    TokenStatus.COMPLETED,
                    TokenStatus.WAITING
            );

            List<Token> tokens = tokenRepository
                    .findByDoctorIdAndVisitDateAndSlotAndStatusIn(
                            doctorId, date, slot, statuses
                    );
            return ResponseEntity.ok(tokens);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @GetMapping("/slot-capacity/{doctorId}/{slot}/{date}")
    public ResponseEntity<SlotCapacityDTO> getSlotCapacity(
            @PathVariable String doctorId,
            @PathVariable String slot,
            @PathVariable LocalDate date) {
        try {
            SlotCapacityDTO capacity = slotCapacityService.getSlotCapacityInfo(doctorId, slot, date);
            return ResponseEntity.ok(capacity);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/{tokenId}/cancel")
    public ResponseEntity<Map<String, String>> cancelToken(
            @PathVariable String tokenId,
            @RequestParam(required = false, defaultValue = "Patient cancellation") String reason) {

        try {
            boolean success = tokenReallocationService.cancelToken(tokenId, reason);

            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("message", "Token cancelled successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Token not found or cannot be cancelled");
                return ResponseEntity.badRequest().body(response);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/{tokenId}/no-show")
    public ResponseEntity<Map<String, String>> markNoShow(@PathVariable String tokenId) {
        try {
            boolean success = tokenReallocationService.markNoShow(tokenId);

            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("message", "Token marked as no-show");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Token not found or cannot be marked as no-show");
                return ResponseEntity.badRequest().body(response);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/{tokenId}/emergency")
    public ResponseEntity<Map<String, String>> insertEmergencyToken(@PathVariable String tokenId) {
        try {
            boolean success = tokenReallocationService.insertEmergencyToken(tokenId);

            Map<String, String> response = new HashMap<>();
            if (success) {
                response.put("message", "Emergency token inserted successfully");
                return ResponseEntity.ok(response);
            } else {
                response.put("message", "Emergency token insertion failed");
                return ResponseEntity.badRequest().body(response);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/{tokenId}/complete")
    public ResponseEntity<Map<String, String>> completeToken(@PathVariable String tokenId) {
        try {
            Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
            if (tokenOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Token not found");
                return ResponseEntity.badRequest().body(response);
            }

            Token token = tokenOpt.get();
            token.setStatus(TokenStatus.COMPLETED);
            token.setCompletedAt(java.time.LocalDateTime.now());
            tokenRepository.save(token);

            // Process waiting queue
            tokenReallocationService.processWaitingQueue(
                    token.getDoctorId(), token.getSlot(), token.getVisitDate()
            );

            Map<String, String> response = new HashMap<>();
            response.put("message", "Token marked as completed");
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();
        }
    }


    @PutMapping("/{tokenId}/start")
    public ResponseEntity<Map<String, String>> startToken(@PathVariable String tokenId) {

        try {
            Optional<Token> tokenOpt = tokenRepository.findByTokenId(tokenId);
            if (tokenOpt.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("message", "Token not found");
                return ResponseEntity.badRequest().body(response);
            }

            Token token = tokenOpt.get();
            token.setStatus(TokenStatus.IN_PROGRESS);
            tokenRepository.save(token);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Token marked as in-progress");
            return ResponseEntity.ok(response);
        }
        catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().build();

        }
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
}

