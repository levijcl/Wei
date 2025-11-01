# Observer Distributed Locking Design

## 1. Problem Statement: Active-Active (AA) Deployment Conflict

### Current Architecture
The system has multiple observer types (Order, Inventory, WES) that periodically poll external systems:
- `OrderObserverScheduler` calls `OrderObserverApplicationService.pollAllActiveObservers()`
- `InventoryObserverScheduler` calls `InventoryObserverApplicationService.pollAllActiveObservers()`
- `WesObserverScheduler` calls `WesObserverApplicationService.pollAllActiveObservers()`

All use Spring `@Scheduled` annotation for periodic execution.

### AA Deployment Scenario
When deployed in Active-Active mode with 2+ nodes:
- Node 1 and Node 2 both run all observer schedulers
- Both schedulers trigger at the same time (e.g., every 30 seconds)
- Both nodes execute polling for all observer types simultaneously

### Detailed Problem Analysis

**Timeline Example (OrderObserver):**
```
Time: 10:00:00 - Scheduled trigger fires on both nodes

Node 1:
10:00:00.000 - OrderObserverService.pollAllActiveObservers() called
10:00:00.010 - findAllActive() → [observer-1, observer-2]
10:00:00.050 - Load observer-1 (lastPolledTimestamp = 09:59:30)
10:00:00.100 - pollOrderSource() fetches orders since 09:59:30 from external DB
10:00:00.150 - External DB returns: [order-123, order-124, order-125]
10:00:00.200 - Create 3 NewOrderObservedEvent
10:00:00.250 - Update lastPolledTimestamp = 10:00:00.250
10:00:00.300 - Save observer-1 to DB
10:00:00.350 - Publish 3 events

Node 2 (parallel execution):
10:00:00.005 - OrderObserverService.pollAllActiveObservers() called
10:00:00.015 - findAllActive() → [observer-1, observer-2]
10:00:00.060 - Load observer-1 (lastPolledTimestamp = 09:59:30) ← SAME!
10:00:00.110 - pollOrderSource() fetches orders since 09:59:30 from external DB
10:00:00.160 - External DB returns: [order-123, order-124, order-125] ← DUPLICATES!
10:00:00.210 - Create 3 NewOrderObservedEvent
10:00:00.260 - Update lastPolledTimestamp = 10:00:00.260
10:00:00.310 - Save observer-1 to DB ← May overwrite Node 1's update
10:00:00.360 - Publish 3 events ← DUPLICATE EVENTS!
```

**Note**: This problem occurs for ALL observer types (Order, Inventory, WES). Each observer type has its own application service but faces the same AA conflict.

### Identified Problems

#### 1. Duplicate Event Publishing
- Both nodes publish `NewOrderObservedEvent` for the same orders
- Downstream event handlers (e.g., Order aggregate creation) process duplicates
- Results in duplicate Order aggregates in the system
- **Impact**: Data integrity violation, duplicate business processing

#### 2. Race Condition on State Update
- Both nodes update `lastPolledTimestamp` nearly simultaneously
- Second update may overwrite the first
- Inconsistent state between nodes
- **Impact**: Potential data loss or inconsistent polling behavior

#### 3. Resource Waste
- Both nodes query external Oracle database simultaneously
- Double the network traffic, database load on external system
- Double the processing within orchestrator nodes
- **Impact**: Performance degradation, increased infrastructure cost

#### 4. External System Impact
- External Oracle database receives duplicate queries
- May trigger rate limiting or throttling
- Increased load on source system
- **Impact**: Potential service degradation on external system

#### 5. Non-Deterministic Behavior
- Which node's lastPolledTimestamp "wins" is unpredictable
- May lead to orders being skipped or re-processed on next poll
- Debugging becomes difficult
- **Impact**: Unpredictable system behavior, operational complexity

### Why Traditional Solutions Don't Work

#### Application-Level Flags
- Each node has separate JVM memory
- Cannot share state without external coordination
- Does not solve the problem

#### Database Optimistic Locking (@Version)
- Can detect conflicts but doesn't prevent duplicate polling
- Both nodes still fetch from external DB before conflict detection
- Only prevents conflicting saves, not duplicate processing
- Does not solve the core problem

#### Single Node Deployment
- Eliminates redundancy
- Single point of failure
- Does not meet AA deployment requirement
- Not acceptable solution

## 2. Proposed Solution: Spring Integration Distributed Locks

### Overview
Use Spring Integration's Distributed Lock Registry with JDBC backend to ensure only ONE node executes polling for each observer type at any given time.

**Key Design Decision**: Use **per-observer-type locks** (3 separate lock keys) instead of a single global lock, enabling load distribution across nodes.

Reference: https://docs.spring.io/spring-integration/reference/distributed-locks.html

### How It Works

#### Lock Acquisition Flow (Generic Observer Scheduler)
```
Time: 10:00:00 - @Scheduled trigger on both nodes
Both nodes attempt to acquire locks for all 3 observer types

Node A:                                      Node B:
  |                                            |
  | @Scheduled trigger                         | @Scheduled trigger
  |                                            |
  ├─ tryLock("order-observer-poll")           ├─ tryLock("order-observer-poll")
  │    INSERT INTO INT_LOCK                   │    INSERT INTO INT_LOCK
  │    SUCCESS (lock acquired)                │    FAIL (PK violation)
  │                                           │
  ├─ Execute orderObserverService             ├─ Skip (Node A has lock)
  │    .pollAllActiveObservers()              │
  │                                           │
  ├─ unlock("order-observer-poll")            │
  │    DELETE FROM INT_LOCK                   │
  │                                           │
  ├─ tryLock("inventory-observer-poll")       ├─ tryLock("inventory-observer-poll")
  │    INSERT INTO INT_LOCK                   │    INSERT INTO INT_LOCK
  │    FAIL (PK violation)                    │    SUCCESS (lock acquired)
  │                                           │
  ├─ Skip (Node B has lock)                   ├─ Execute inventoryObserverService
  │                                           │    .pollAllActiveObservers()
  │                                           │
  │                                           ├─ unlock("inventory-observer-poll")
  │                                           │    DELETE FROM INT_LOCK
  │                                           │
  ├─ tryLock("wes-observer-poll")             ├─ tryLock("wes-observer-poll")
  │    INSERT INTO INT_LOCK                   │    INSERT INTO INT_LOCK
  │    SUCCESS (lock acquired)                │    FAIL (PK violation)
  │                                           │
  ├─ Execute wesObserverService               ├─ Skip (Node A has lock)
  │    .pollAllActiveObservers()              │
  │                                           │
  └─ unlock("wes-observer-poll")              │
       DELETE FROM INT_LOCK                   │

Result:
- Node A polled: Order + WES observers
- Node B polled: Inventory observer
- Both nodes actively working!
```

#### Database-Level Coordination
- Lock state stored in database table `INT_LOCK`
- Primary key constraint ensures only one node holds lock
- Database transaction guarantees atomicity
- Both nodes see the same lock state immediately

#### Automatic Expiration
- Lock has TTL (time-to-live)
- If node crashes while holding lock, TTL expires
- Other nodes can acquire after expiration
- Prevents permanent deadlock

### Architecture Components

#### 1. Lock Registry Bean
```
JdbcLockRegistry
  ├── Uses existing DataSource
  ├── Creates INT_LOCK table
  ├── Manages lock lifecycle
  └── Provides Lock objects
```

#### 2. Scheduler with Lock
```
OrderObserverScheduler
  ├── Injected with LockRegistry
  ├── @Scheduled method
  ├── Acquires lock before polling
  ├── Executes pollAllActiveObservers()
  └── Releases lock after completion
```

#### 3. Database Table
```sql
CREATE TABLE INT_LOCK (
  LOCK_KEY CHAR(36) PRIMARY KEY,
  REGION VARCHAR(100),
  CLIENT_ID CHAR(36),
  CREATED_DATE TIMESTAMP NOT NULL
);
```

### Benefits

#### ✅ Eliminates Duplicate Processing
- Only one node executes at a time
- No duplicate events published
- Consistent system behavior

#### ✅ No Race Conditions
- Single node updates state
- Consistent lastPolledTimestamp
- Predictable behavior

#### ✅ Resource Efficiency
- Only one node queries external DB
- Reduced load on external system
- Cost savings

#### ✅ Maintains High Availability
- If lock-holding node crashes, TTL expires
- Other nodes can acquire lock
- System continues operating
- No single point of failure

#### ✅ Simple Implementation
- Spring Integration provides abstraction
- Minimal code changes
- Leverages existing database
- No additional infrastructure (Redis, Zookeeper)

### Load Distribution Across Nodes

#### Problem: Single Lock = Single Worker
If we use one global lock (e.g., "observer-poll"), Node A will likely always acquire it first:
```
Every cycle:
- Node A acquires "observer-poll" lock → does ALL work
- Node B tries to acquire → fails → sits idle
- Node B becomes wasted resource
```

#### Solution: Per-Observer Lock Keys
Use **separate lock keys for each observer type**:

```
Lock Keys:
- "order-observer-poll"      → for OrderObserver polling
- "inventory-observer-poll"  → for InventoryObserver polling
- "wes-observer-poll"        → for WesObserver polling
```

#### Work Distribution Example
```
Time: 10:00:00 - All schedulers trigger on both nodes

Node A attempts:
├─ tryLock("order-observer-poll")     → SUCCESS (acquired)
├─ tryLock("inventory-observer-poll") → FAIL (Node B got it first)
└─ tryLock("wes-observer-poll")       → SUCCESS (acquired)

Node B attempts:
├─ tryLock("order-observer-poll")     → FAIL (Node A got it first)
├─ tryLock("inventory-observer-poll") → SUCCESS (acquired)
└─ tryLock("wes-observer-poll")       → FAIL (Node A got it first)

Result:
- Node A: Polls Order + WES observers
- Node B: Polls Inventory observer
- Both nodes actively working!
```

#### Distribution Characteristics

**Non-Deterministic but Balanced:**
- Which node acquires which lock varies by cycle
- Race conditions are random (network latency, CPU scheduling, etc.)
- Over time, work naturally balances across nodes

**Example Distribution Over 5 Cycles:**
```
Cycle 1: Node A [Order, WES]       Node B [Inventory]
Cycle 2: Node A [Inventory]        Node B [Order, WES]
Cycle 3: Node A [Order, Inventory] Node B [WES]
Cycle 4: Node A [WES]              Node B [Order, Inventory]
Cycle 5: Node A [Order]            Node B [Inventory, WES]

Average: ~50/50 distribution
```

**Scalability:**
- 3 observers + 2 nodes = good distribution
- Future: 10 observers + 3 nodes = even better distribution
- Adding more observers improves load balancing
- Adding more nodes improves redundancy

**Failure Handling:**
```
Normal: Node A [Order, WES], Node B [Inventory]
Node A crashes: Node B acquires all locks → [Order, Inventory, WES]
Node A recovers: Work redistributes → balanced again
```

**Benefits:**
- ✅ No single node monopolizes work
- ✅ True Active-Active deployment
- ✅ Better resource utilization
- ✅ Natural load balancing
- ✅ Graceful degradation on node failure

## 3. Implementation Design

### 3.1 Database Schema

**Table: INT_LOCK**
```sql
CREATE TABLE INT_LOCK (
  LOCK_KEY CHAR(36) NOT NULL,
  REGION VARCHAR(100) NOT NULL,
  CLIENT_ID CHAR(36) NOT NULL,
  CREATED_DATE TIMESTAMP NOT NULL,
  PRIMARY KEY (LOCK_KEY, REGION)
);

CREATE INDEX IDX_INT_LOCK_CREATED ON INT_LOCK(CREATED_DATE);
```

**Fields:**
- `LOCK_KEY`: Identifier for the lock (e.g., "order-observer-poll")
- `REGION`: Allows multiple lock groups (default: "DEFAULT")
- `CLIENT_ID`: UUID of the node holding the lock
- `CREATED_DATE`: When lock was acquired (used for TTL)

### 3.2 Configuration

**application.yml**
```yaml
scheduler:
  observer:
    fixed-delay: 30000  # 30 seconds between polls
    lock:
      ttl: 60000  # 60 seconds lock TTL

  # Specific configuration per observer type (if needed)
  order-observer:
    enabled: true
  inventory-observer:
    enabled: true
  wes-observer:
    enabled: true
```

**Configuration Class**
```java
@Configuration
public class DistributedLockConfiguration {

    @Bean
    public DefaultLockRepository lockRepository(DataSource dataSource) {
        return new DefaultLockRepository(dataSource);
    }

    @Bean
    public JdbcLockRegistry lockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }
}
```

### 3.3 Scheduler Implementation

**ObserverScheduler.java** - Generic scheduler supporting multiple observer types (Order, Inventory, WES):

```java
@Component
public class ObserverScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ObserverScheduler.class);

    private final LockRegistry lockRegistry;
    private final OrderObserverApplicationService orderObserverService;
    private final InventoryObserverApplicationService inventoryObserverService;
    private final WesObserverApplicationService wesObserverService;

    public ObserverScheduler(
            LockRegistry lockRegistry,
            OrderObserverApplicationService orderObserverService,
            InventoryObserverApplicationService inventoryObserverService,
            WesObserverApplicationService wesObserverService) {
        this.lockRegistry = lockRegistry;
        this.orderObserverService = orderObserverService;
        this.inventoryObserverService = inventoryObserverService;
        this.wesObserverService = wesObserverService;
    }

    @Scheduled(fixedDelayString = "${scheduler.observer.fixed-delay:30000}")
    public void pollAllObserverTypes() {
        logger.info("Starting scheduled polling cycle for all observer types");

        // Poll each observer type with its own lock key
        pollObserverType("order-observer-poll", () -> orderObserverService.pollAllActiveObservers());
        pollObserverType("inventory-observer-poll", () -> inventoryObserverService.pollAllActiveObservers());
        pollObserverType("wes-observer-poll", () -> wesObserverService.pollAllActiveObservers());

        logger.info("Polling cycle completed for all observer types");
    }

    private void pollObserverType(String lockKey, Runnable pollingAction) {
        Lock lock = lockRegistry.obtain(lockKey);
        boolean lockAcquired = false;

        try {
            lockAcquired = lock.tryLock(1, TimeUnit.SECONDS);

            if (lockAcquired) {
                logger.info("Lock acquired for: {}", lockKey);
                pollingAction.run();
                logger.info("Completed polling for: {}", lockKey);
            } else {
                logger.debug("Lock not acquired for: {} (another node is polling)", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for lock: {}", lockKey, e);
        } catch (Exception e) {
            logger.error("Error during polling for: {}", lockKey, e);
        } finally {
            if (lockAcquired) {
                try {
                    lock.unlock();
                    logger.debug("Lock released for: {}", lockKey);
                } catch (Exception e) {
                    logger.error("Error releasing lock for: {}", lockKey, e);
                }
            }
        }
    }
}
```

### 3.4 Lock Behavior

#### Success Path (Node acquires lock)
1. `tryLock()` attempts to acquire with 1-second timeout
2. If successful, execute polling logic
3. After completion, release lock in `finally` block
4. Log success

#### Skip Path (Node cannot acquire lock)
1. `tryLock()` fails (another node holds lock)
2. Log debug message and return
3. Wait for next scheduled trigger
4. No error thrown

#### Error Path (Exception during polling)
1. Exception caught in catch block
2. Lock still released in `finally` block
3. Error logged
4. System continues, next schedule will retry

#### Crash Recovery
1. If node crashes while holding lock, lock remains in DB
2. TTL mechanism: Spring Integration checks CREATED_DATE
3. If `(now - CREATED_DATE) > TTL`, lock is considered expired
4. Other nodes can acquire expired lock
5. Old lock record is deleted, new one inserted

### 3.5 Testing Strategy

#### Unit Tests
**ObserverSchedulerTest.java**
- Mock LockRegistry, all observer application services
- Test lock acquisition success path for each observer type
- Test lock acquisition failure path (other node holds lock)
- Test mixed scenario: acquires order lock, fails inventory lock, acquires wes lock
- Test exception handling with lock release
- Test interrupted exception handling
- Verify correct observer service called for each lock

#### Integration Tests
**ObserverSchedulerIntegrationTest.java**
- Use real database with INT_LOCK table
- Use @MockBean for observer services (focus on locking behavior)
- Test single node acquires all 3 locks successfully
- Test second node skips all when first node holds locks
- Test distributed execution: Node A gets 2 locks, Node B gets 1 lock
- Test lock release after completion for each observer type
- Test TTL expiration after simulated crash

#### Load Tests
**Scenario: 2 Nodes + 3 Observer Types**
- Simulate 2 nodes polling simultaneously
- Verify only one node polls each observer type at a time
- Verify no duplicate events published
- Measure lock distribution: expect ~50/50 across nodes over time
- Measure resource usage reduction: 50% fewer external DB queries
- Test failure scenario: Node crashes, other node takes over all locks

## 4. Migration Plan

### Phase 1: Preparation
1. Add Spring Integration JDBC dependency to `pom.xml`
   ```xml
   <dependency>
       <groupId>org.springframework.integration</groupId>
       <artifactId>spring-integration-jdbc</artifactId>
   </dependency>
   ```
2. Create Flyway migration for INT_LOCK table: `V{version}__create_int_lock_table.sql`
3. Create `DistributedLockConfiguration` class with `LockRegistry` bean
4. Add configuration properties to `application.yml`
5. No scheduler changes yet - existing schedulers continue running

### Phase 2: Implementation
1. Create `ObserverScheduler` class (Generic Observer Scheduler)
2. Inject all 3 observer application services
3. Implement `pollAllObserverTypes()` with per-type locking
4. Add `@EnableScheduling` to configuration
5. Write unit tests for `ObserverScheduler`
6. Write integration tests

### Phase 3: Testing
1. Deploy to staging with 2 nodes
2. Monitor logs for lock acquisition patterns
   - Expect: Both nodes acquiring different locks
   - Expect: "Lock not acquired" debug messages (normal)
3. Verify no duplicate events in event logs
4. Run load tests with 2-3 nodes
5. Test failure scenarios: kill one node, verify other takes over

### Phase 4: Gradual Rollout
1. Deploy to production with feature flag `scheduler.observer.enabled=false`
2. Enable for 1 node first: `scheduler.observer.enabled=true` (canary)
3. Monitor for 24 hours: check metrics, logs, errors
4. Enable for all nodes
5. Monitor for 1 week

### Phase 5: Cleanup (Optional)
1. Remove old individual schedulers once confident
2. Archive old scheduler code
3. Update documentation

## 5. Monitoring & Observability

### Metrics to Track

**Per-Observer-Type Metrics:**
- `scheduler.observer.lock_acquired_total{type="order"}` (counter)
- `scheduler.observer.lock_acquired_total{type="inventory"}` (counter)
- `scheduler.observer.lock_acquired_total{type="wes"}` (counter)
- `scheduler.observer.lock_failed_total{type="order"}` (counter)
- `scheduler.observer.lock_failed_total{type="inventory"}` (counter)
- `scheduler.observer.lock_failed_total{type="wes"}` (counter)
- `scheduler.observer.polling_duration_seconds{type="order"}` (histogram)
- `scheduler.observer.polling_duration_seconds{type="inventory"}` (histogram)
- `scheduler.observer.polling_duration_seconds{type="wes"}` (histogram)

**Node Distribution Metrics:**
- `scheduler.observer.lock_acquired_by_node{node="node-a", type="order"}` (counter)
- `scheduler.observer.lock_acquired_by_node{node="node-b", type="order"}` (counter)

**Cycle Metrics:**
- `scheduler.observer.cycle_duration_seconds` (histogram) - Total time for one polling cycle
- `scheduler.observer.cycle_locked_count` (gauge) - Number of observer types locked by this node
- `scheduler.observer.cycle_skipped_count` (gauge) - Number of observer types skipped

### Log Messages

**INFO level:**
```
[Node A] Starting scheduled polling cycle for all observer types
[Node A] Lock acquired for: order-observer-poll
[Node A] Completed polling for: order-observer-poll
[Node B] Lock acquired for: inventory-observer-poll
[Node B] Completed polling for: inventory-observer-poll
[Node A] Lock acquired for: wes-observer-poll
[Node A] Completed polling for: wes-observer-poll
[Node A] Polling cycle completed for all observer types
[Node B] Polling cycle completed for all observer types
```

**DEBUG level:**
```
[Node B] Lock not acquired for: order-observer-poll (another node is polling)
[Node A] Lock not acquired for: inventory-observer-poll (another node is polling)
[Node B] Lock not acquired for: wes-observer-poll (another node is polling)
[Node A] Lock released for: order-observer-poll
```

**WARN level:**
```
[Node A] Interrupted while waiting for lock: order-observer-poll
```

**ERROR level:**
```
[Node A] Error during polling for: inventory-observer-poll
[Node B] Error releasing lock for: wes-observer-poll
```

### Dashboards

**Load Distribution Dashboard:**
- Graph: Lock acquisitions per node over time (stacked area chart)
  - Expected: Roughly 50/50 for 2 nodes, 33/33/33 for 3 nodes
- Graph: Lock acquisitions per observer type per node (heatmap)
  - Shows which node is handling which observer type
- Table: Lock acquisition counts by node and type (last 1 hour)

**Observer Health Dashboard:**
- Graph: Polling success rate per observer type
- Graph: Polling duration per observer type (p50, p95, p99)
- Graph: Lock contention rate (failed locks / total attempts)
- Alert: If any observer type has 0 successful polls for 5+ minutes

**AA Verification Dashboard:**
- Graph: Active nodes polling (distinct node IDs acquiring locks)
- Graph: Total lock attempts vs acquisitions across all nodes
- Metric: Load balance score (0-100, where 100 = perfect balance)
- Alert: If only one node is ever acquiring locks (AA degraded)

### Alerts

**🔴 Critical (Page immediately):**
- All nodes failing to acquire any locks for 5+ minutes
  - Indicates: Database issue, INT_LOCK table problem, or all nodes down
- Zero polling activity for any observer type for 10+ minutes
  - Indicates: All nodes crashed or scheduler not running
- Lock hold time exceeds TTL (60+ seconds) for 3+ consecutive cycles
  - Indicates: Stuck process, potential deadlock

**🟡 Warning (Notify on-call):**
- Lock acquisition failure rate > 80% for single observer type for 30+ minutes
  - Indicates: High contention, possible configuration issue
- Only one node acquiring locks for 30+ minutes (in 2+ node deployment)
  - Indicates: AA degraded, one node may be down
- Polling duration increasing trend (20%+ increase over 1 hour)
  - Indicates: Performance degradation, external system slowness

**⚪ Info (Log only, no alert):**
- Occasional lock acquisition failures (expected in AA)
- Lock release errors (will auto-expire via TTL)
- Lock distribution skewed short-term (< 5 minutes)

## 6. Alternative Approaches Considered

### Alternative 1: ShedLock
- Third-party library specifically for distributed scheduling
- Similar JDBC-based locking
- **Pros**: Purpose-built for this use case, @SchedulerLock annotation
- **Cons**: Additional dependency, less flexible than Spring Integration

### Alternative 2: Redis-based Lock (Redisson)
- Use Redis for distributed locking
- **Pros**: Fast, commonly used
- **Cons**: Additional infrastructure, Redis operational complexity, not needed if DB exists

### Alternative 3: Leader Election (Kubernetes)
- Use Kubernetes leader election
- **Pros**: Platform-native solution
- **Cons**: Couples to Kubernetes, not portable, complex setup

### Alternative 4: Database Advisory Locks (PostgreSQL)
- Use database-native locking
- **Pros**: Very efficient, no additional table
- **Cons**: Database-specific, may not work with Oracle/MySQL, less portable

### Decision: Spring Integration JDBC
**Rationale:**
- Already using Spring Framework
- No additional infrastructure
- Portable across databases
- Production-tested
- Simple implementation
- Works with existing DataSource

## 7. Risks & Mitigations

### Risk 1: Lock Table Grows Unbounded
- **Mitigation**: Regular cleanup job for old lock records
- **Mitigation**: TTL mechanism auto-expires old locks

### Risk 2: Database Becomes Bottleneck
- **Mitigation**: Lock table is very small, minimal DB load
- **Mitigation**: Indexed properly for quick lookups

### Risk 3: Node Crashes While Holding Lock
- **Mitigation**: TTL expires lock after 60 seconds
- **Mitigation**: Next poll cycle will acquire and continue

### Risk 4: Clock Skew Between Nodes
- **Mitigation**: Use database timestamp (CURRENT_TIMESTAMP)
- **Mitigation**: TTL is generous (60s for 30s polling)

## 8. Success Criteria

### Functional
- ✅ Only one node executes polling per observer type at a time
- ✅ No duplicate events published (NewOrderObservedEvent, InventoryObservedEvent, WesTaskPolledEvent)
- ✅ System continues operating if one node fails (other node acquires all locks)
- ✅ Lock released properly after each observer type execution
- ✅ All 3 observer types can be polled simultaneously by different nodes

### Performance
- ✅ Lock acquisition latency < 100ms per lock
- ✅ No significant impact on polling execution time (< 5% overhead)
- ✅ ~50% reduction in external DB query load (averaged across all observer types)
- ✅ Work distribution: Each node handles 33-66% of observer types per cycle (2 nodes)

### Operational
- ✅ Clear log messages showing which node acquired which lock
- ✅ Metrics available per observer type and per node
- ✅ No manual intervention required for lock recovery (TTL handles it)
- ✅ Easy to add new observer types (just add to Generic Observer Scheduler)
- ✅ Ability to monitor load distribution across nodes in real-time

### Load Distribution (2 Nodes)
Over 100 polling cycles:
- ✅ Order observer: ~50 locks to Node A, ~50 locks to Node B
- ✅ Inventory observer: ~50 locks to Node A, ~50 locks to Node B
- ✅ WES observer: ~50 locks to Node A, ~50 locks to Node B
- ✅ Each node gets at least 1 lock per cycle on average
- ✅ No single node consistently monopolizes all locks
