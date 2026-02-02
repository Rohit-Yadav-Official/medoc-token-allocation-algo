package medochealth.example.medoc_opd_token_Allocation_system.Service;

import java.time.Duration;
import java.time.ZoneOffset;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import medochealth.example.medoc_opd_token_Allocation_system.Model.Token;
@Service
public class SlotQueueService {

    private final RedisTemplate<String, Object> redisTemplate;

    public SlotQueueService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }



    private String waitingKey(String slotId) {
        return "slot:" + slotId + ":waiting";
    }

    private String allocatedKey(String slotId) {
        return "slot:" + slotId + ":allocated";
    }

    private String lockKey(String slotId) {
        return "slot:" + slotId + ":lock";
    }



    public void addToWaitingQueue(String slotId, Token token) {
        double score = calculateScore(token);

        redisTemplate.opsForZSet()
                .add(waitingKey(slotId), token.getTokenId(), score);
    }

    public String pollNextWaiting(String slotId) {
        String key = waitingKey(slotId);
        // Get the minimum score element (highest priority)
        var result = redisTemplate.opsForZSet().range(key, 0, 0);
        if (result != null && !result.isEmpty()) {
            String tokenId = result.iterator().next().toString();
            // Remove it from the set
            redisTemplate.opsForZSet().remove(key, tokenId);
            return tokenId;
        }
        return null;
    }



    public void addToAllocatedQueue(String slotId, Token token) {
        double score = calculateScore(token);

        redisTemplate.opsForZSet()
                .add(allocatedKey(slotId), token.getTokenId(), score);
    }

    public String getWeakestAllocated(String slotId) {
        var result = redisTemplate.opsForZSet().reverseRange(allocatedKey(slotId), 0, 0);
        if (result != null && !result.isEmpty()) {
            return (String) result.iterator().next();
        }
        return null;
    }

    public void removeFromAllocated(String slotId, String tokenId) {
        redisTemplate.opsForZSet()
                .remove(allocatedKey(slotId), tokenId);
    }

    public void removeFromAllocatedQueue(String slotId, String tokenId) {
        removeFromAllocated(slotId, tokenId);
    }



    public void startNoShowTimer(String tokenId) {
        redisTemplate.opsForValue()
                .set("token:" + tokenId + ":grace", "1", Duration.ofMinutes(10));
    }

    public void clearNoShowTimer(String tokenId) {
        redisTemplate.delete("token:" + tokenId + ":grace");
    }



    public boolean acquireSlotLock(String slotId) {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(lockKey(slotId), "LOCKED", Duration.ofSeconds(5));

        return Boolean.TRUE.equals(locked);
    }

    public void releaseSlotLock(String slotId) {
        redisTemplate.delete(lockKey(slotId));
    }



    private double calculateScore(Token token) {
        return token.getPriority() * 1_000_000_000L
                + token.getCreatedAt().toEpochSecond(ZoneOffset.UTC);
    }
}
