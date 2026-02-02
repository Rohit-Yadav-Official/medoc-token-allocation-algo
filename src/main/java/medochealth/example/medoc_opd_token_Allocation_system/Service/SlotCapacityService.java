package medochealth.example.medoc_opd_token_Allocation_system.Service;

import medochealth.example.medoc_opd_token_Allocation_system.DTO.SlotCapacityDTO;
import medochealth.example.medoc_opd_token_Allocation_system.Model.Token.TokenStatus;
import medochealth.example.medoc_opd_token_Allocation_system.Repository.TokenRepository;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class SlotCapacityService {

    private final TokenRepository tokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    
    // Default capacity per slot (can be configured per doctor/slot)
    private static final int DEFAULT_SLOT_CAPACITY = 10;
    private static final int EMERGENCY_BUFFER = 2; // Reserve 2 slots for emergencies

    public SlotCapacityService(TokenRepository tokenRepository, RedisTemplate<String, Object> redisTemplate) {
        this.tokenRepository = tokenRepository;
        this.redisTemplate = redisTemplate;
    }


    public int getSlotCapacity(String doctorId, String slot, LocalDate visitDate) {

        try {
            String key = getCapacityKey(doctorId, slot, visitDate);
            Object capacity = redisTemplate.opsForValue().get(key);

            if (capacity != null) {
                if (capacity instanceof String) {
                    return Integer.parseInt((String) capacity);
                } else if (capacity instanceof Integer) {
                    return (Integer) capacity;
                }
            }

            // Default capacity
            int defaultCapacity = DEFAULT_SLOT_CAPACITY;
            setSlotCapacity(doctorId, slot, visitDate, defaultCapacity);
            return defaultCapacity;
        }
        catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    public void setSlotCapacity(String doctorId, String slot, LocalDate visitDate, int capacity) {
        String key = getCapacityKey(doctorId, slot, visitDate);
        redisTemplate.opsForValue().set(key, String.valueOf(capacity), 1, TimeUnit.DAYS);
    }


    public int getCurrentAllocation(String doctorId, String slot, LocalDate visitDate) {
        List<TokenStatus> allocatedStatuses = Arrays.asList(
            TokenStatus.ALLOCATED, 
            TokenStatus.IN_PROGRESS,
            TokenStatus.WAITING
        );
        
        return (int) tokenRepository.countByDoctorIdAndVisitDateAndSlotAndStatusIn(
            doctorId, visitDate, slot, allocatedStatuses
        );
    }

    public boolean hasAvailableCapacity(String doctorId, String slot, LocalDate visitDate) {
        int capacity = getSlotCapacity(doctorId, slot, visitDate);
        int allocated = getCurrentAllocation(doctorId, slot, visitDate);
        
        // Reserve emergency buffer
        int available = capacity - allocated - EMERGENCY_BUFFER;
        return available > 0;
    }


    public boolean hasEmergencyCapacity(String doctorId, String slot, LocalDate visitDate) {
        int capacity = getSlotCapacity(doctorId, slot, visitDate);
        int allocated = getCurrentAllocation(doctorId, slot, visitDate);
        return allocated < capacity;
    }


    public SlotCapacityDTO getSlotCapacityInfo(String doctorId, String slot, LocalDate visitDate) {
        SlotCapacityDTO dto = new SlotCapacityDTO();
        dto.setDoctorId(doctorId);
        dto.setSlot(slot);
        dto.setVisitDate(visitDate);
        
        int capacity = getSlotCapacity(doctorId, slot, visitDate);
        int allocated = getCurrentAllocation(doctorId, slot, visitDate);
        
        dto.setMaxCapacity(capacity);
        dto.setCurrentAllocated(allocated);
        dto.setAvailableSlots(Math.max(0, capacity - allocated - EMERGENCY_BUFFER));
        
        return dto;
    }

    private String getCapacityKey(String doctorId, String slot, LocalDate visitDate) {
        return String.format("capacity:%s:%s:%s", doctorId, slot, visitDate);
    }
}

