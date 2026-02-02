# Token Allocation Algorithm Documentation

## Overview

The OPD Token Allocation System implements a sophisticated algorithm that handles dynamic token allocation, reallocation, and prioritization across multiple booking sources while enforcing hard capacity limits per slot.

---

## Core Algorithm: Token Allocation

### Step 1: Validation
1. **Doctor Validation**: Verify doctor exists and is active
2. **Patient Validation**: Verify patient exists
3. **Slot Validation**: Verify slot is available for the doctor
4. **Duplicate Check**: Ensure patient doesn't already have an active token for the date

### Step 2: Priority Calculation

Priority is calculated based on booking type and emergency status:

```java
Priority = {
    Emergency: 0,
    Paid Priority: 1,
    Follow-up: 2,
    Walk-in: 3,
    Online: 4
}
```

**Priority Score Formula:**
```
Score = Priority × 1,000,000,000 + CreatedAt (Unix timestamp)
```

This ensures:
- Higher priority tokens always come first
- Within same priority, FIFO ordering (earliest created first)

### Step 3: Capacity Check

**For Regular Tokens:**
```
Available Capacity = Max Capacity - Current Allocated - Emergency Buffer (2)
```

**For Emergency Tokens:**
```
Available Capacity = Max Capacity - Current Allocated
```
(Emergency tokens bypass the buffer)

### Step 4: Allocation Decision

**If capacity available:**
1. Create token with status `ALLOCATED`
2. Assign token number (sequential within slot)
3. Add to allocated queue (Redis sorted set)
4. Return success response

**If capacity not available (non-emergency):**
1. Try to find alternative slot (same doctor, different time)
2. If alternative found: Allocate to alternative slot
3. If no alternative: Add to waiting queue (status `WAITING`)

**If capacity not available (emergency):**
1. Check if emergency buffer can be used
2. If yes: Allocate with priority 0
3. If no: Try alternative slot or return error

---

## Dynamic Reallocation Algorithm

### Scenario 1: Cancellation

**Process:**
1. Mark token as `CANCELLED`
2. Remove from allocated queue
3. Trigger waiting queue processing:
   - Poll highest priority waiting token
   - Check if slot has capacity
   - If yes: Allocate and assign token number
   - Repeat until queue empty or capacity full

**Edge Cases:**
- Multiple cancellations: Process sequentially with locks
- Waiting token priority: Highest priority first, then FIFO

### Scenario 2: No-Show

**Process:**
1. Mark token as `EXPIRED`
2. Set `expiredAt` timestamp
3. Remove from allocated queue
4. Trigger waiting queue processing (same as cancellation)

**Grace Period:**
- 10-minute grace period before marking as no-show
- Timer stored in Redis with TTL

### Scenario 3: Emergency Insertion

**Process:**
1. Validate emergency flag
2. Check emergency capacity (bypasses buffer)
3. If capacity available:
   - Set priority to 0 (highest)
   - Assign token number 1
   - Shift all existing token numbers by +1
   - Insert at front of queue

**Priority Handling:**
- Emergency tokens always get priority 0
- Token numbers are shifted to accommodate emergency

### Scenario 4: Alternative Slot Finding

**Algorithm:**
1. Get all slots for the doctor
2. Filter out requested slot
3. Filter slots with available capacity
4. Sort by time proximity to requested slot
5. Return closest available slot

**Time Proximity Calculation:**
```
Distance = |RequestedSlotTime - AlternativeSlotTime| (in minutes)
```

---

## Capacity Management

### Hard Limits Enforcement

**Per-Slot Capacity:**
- Default: 10 tokens per slot
- Configurable per doctor-slot-date
- Stored in Redis with 1-day TTL

**Capacity States:**
- **Available**: `Current < Max - Buffer`
- **Full (Regular)**: `Current >= Max - Buffer`
- **Full (Emergency)**: `Current >= Max`

### Emergency Buffer

- **Purpose**: Reserve slots for emergency cases
- **Size**: 2 slots per slot-time
- **Behavior**: 
  - Regular tokens cannot use buffer
  - Emergency tokens can use buffer
  - Buffer released if no emergency by end of slot

---

## Queue Management (Redis)

### Waiting Queue
- **Structure**: Redis Sorted Set (ZSet)
- **Key**: `slot:{doctorId}:{slot}:{date}:waiting`
- **Score**: Priority-based score
- **Operations**:
  - `ZADD`: Add token to queue
  - `ZPOPMIN`: Get and remove highest priority token

### Allocated Queue
- **Structure**: Redis Sorted Set (ZSet)
- **Key**: `slot:{doctorId}:{slot}:{date}:allocated`
- **Score**: Priority-based score
- **Operations**:
  - `ZADD`: Add allocated token
  - `ZREM`: Remove token (on cancellation/no-show)
  - `ZREVRANGE`: Get tokens in priority order

### Slot Lock
- **Purpose**: Prevent race conditions during allocation
- **Key**: `slot:{doctorId}:{slot}:{date}:lock`
- **TTL**: 5 seconds
- **Usage**: Acquire before allocation, release after

---

## Prioritization Logic

### Primary Priority: Booking Type + Emergency

1. **Emergency** (Priority 0)
   - Highest priority
   - Can bypass capacity buffer
   - Inserted at front of queue

2. **Paid Priority** (Priority 1)
   - Second highest
   - Patients who paid for priority service

3. **Follow-up** (Priority 2)
   - Third priority
   - Returning patients

4. **Walk-in** (Priority 3)
   - Fourth priority
   - Patients arriving at OPD desk

5. **Online** (Priority 4)
   - Lowest priority
   - Pre-booked online appointments

### Secondary Priority: Time (FIFO)

Within the same priority level, tokens are ordered by creation time:
- Earlier created = Higher priority
- Ensures fairness within priority groups

---

## Edge Cases Handling

### 1. Slot Full - Alternative Slot Available
**Handling:**
- Automatically allocate to nearest alternative slot
- Update token slot field
- Notify user in response message

### 2. Slot Full - No Alternative Available
**Handling:**
- Add to waiting queue
- Status: `WAITING`
- Process automatically when capacity available

### 3. Multiple Cancellations Simultaneously
**Handling:**
- Use Redis locks to prevent race conditions
- Process cancellations sequentially
- Each cancellation triggers waiting queue processing

### 4. Emergency When Slot Full
**Handling:**
- Check emergency buffer
- If buffer available: Allocate with priority 0
- If buffer full: Try alternative slot
- If no alternative: Return error (rare case)

### 5. Patient Already Has Token
**Handling:**
- Check for active tokens (WAITING, ALLOCATED, IN_PROGRESS)
- Reject new allocation if active token exists
- Return error message



### 6. Invalid Slot for Doctor
**Handling:**
- Validate slot exists in doctor's available slots
- Reject if invalid
- Return error message



### 7. Token Number Assignment
**Handling:**
- Sequential numbering within slot
- Query max token number for slot
- Assign next number
- Emergency tokens get number 1, shift others

### 8. Concurrent Allocations
**Handling:**
- Redis distributed locks
- 5-second lock timeout
- Retry mechanism for lock acquisition

---

## Failure Handling

### Database Failures
- **Transaction Management**: All allocation operations are transactional
- **Rollback**: On failure, all changes are rolled back
- **Error Response**: Return appropriate error message to client

### Redis Failures
- **Graceful Degradation**: System can operate without Redis (slower)
- **Queue Persistence**: Waiting queue stored in database as fallback
- **Retry Logic**: Automatic retry for transient failures

### Capacity Overflow
- **Prevention**: Hard limits enforced at allocation time
- **Handling**: Queue overflow tokens in waiting queue
- **Recovery**: Automatic processing when capacity available

### Invalid Requests
- **Validation**: Comprehensive input validation
- **Error Messages**: Clear, descriptive error messages
- **Status Codes**: Appropriate HTTP status codes (400, 404, 500)

### Race Conditions
- **Locks**: Redis distributed locks for critical sections
- **Atomic Operations**: Use Redis atomic operations where possible
- **Idempotency**: Operations are idempotent where applicable

---

## Performance Considerations

### Optimization Strategies

1. **Redis Caching**
   - Slot capacities cached in Redis
   - Queue operations use Redis sorted sets
   - Reduces database load

2. **Database Indexing**
   - Indexes on (doctorId, visitDate, slot, status)
   - Fast queries for token lookups
   - Efficient capacity counting

3. **Batch Processing**
   - Waiting queue processed in batches
   - Reduces lock contention

4. **Lazy Evaluation**
   - Alternative slot finding only when needed
   - Capacity checks cached

### Scalability

- **Horizontal Scaling**: Stateless design allows multiple instances
- **Redis Cluster**: Can use Redis cluster for high availability
- **Database Sharding**: Can shard by doctor or date for large scale

---

## Monitoring and Observability

### Key Metrics

1. **Allocation Rate**: Tokens allocated per minute
2. **Queue Length**: Waiting queue size per slot
3. **Capacity Utilization**: Percentage of capacity used
4. **Cancellation Rate**: Percentage of tokens cancelled
5. **No-Show Rate**: Percentage of no-shows
6. **Reallocation Rate**: Tokens reallocated per day

### Logging

- All allocation operations logged
- Cancellations and no-shows logged
- Emergency insertions logged
- Capacity overflow events logged

---

## Future Enhancements

1. **Predictive Capacity**: ML-based capacity prediction
2. **Dynamic Pricing**: Adjust capacity based on demand
3. **Patient Notifications**: SMS/Email notifications for reallocations
4. **Analytics Dashboard**: Real-time dashboard for OPD operations
5. **Multi-Hospital Support**: Support for multiple hospitals
6. **Appointment Reminders**: Automated reminder system

