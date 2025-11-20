# AuditRecord System Implementation Design

## 1. Problem Statement: Event Auditing & Traceability

### Current Situation
The Wei Orchestrator system processes multiple domain events across different bounded contexts (Order, WES, Inventory, Observation). Currently:
- Domain events are published and processed but not systematically recorded
- No centralized audit trail for compliance and debugging
- Difficult to trace event causality (e.g., what triggered OrderCreatedEvent?)
- No correlation between related events across contexts
- Limited ability to reconstruct system state at a point in time

### Business Requirements
From backend/README.md (lines 1880-1892, 1303-1307), the AuditRecord system must:

1. **Complete Event Traceability**: Record ALL domain events across all contexts
2. **Event Causality Tracking**: Track what triggered each event (trigger source)
3. **Cross-Context Correlation**: Support correlation IDs for tracing flows across contexts
4. **Non-Intrusive Design**: Should not impact existing business logic
5. **Async Processing**: Should not slow down main business flows
6. **Queryable Audit Trail**: Enable searching by aggregate, event type, time range

### Key Challenge: Trigger Source Detection

The most critical challenge is determining **what triggered each event**:

```
Example Event Chain:
OrderObserver (polling)
  → NewOrderObservedEvent (trigger: Scheduled polling)
  → NewOrderObservedEventHandler
  → CreateOrderCommand
  → Order.createOrder()
  → OrderCreatedEvent (trigger: ???)  ← How to know it was triggered by NewOrderObservedEvent?
```

**Trigger Source Types:**
- **Event Name**: "NewOrderObservedEvent" - when an event handler creates a new event
- **"Manual"**: Direct API call or user action
- **"Scheduled"**: Cron job or scheduled task
- **"System"**: System initialization or startup

### Success Criteria
- ✅ Every domain event is recorded with complete metadata
- ✅ Trigger source is accurately captured for all events
- ✅ Correlation IDs enable cross-context tracing
- ✅ Zero impact on business transaction performance
- ✅ Audit failures do not affect business operations
- ✅ Searchable audit trail for compliance and debugging

---

## 2. AuditRecord Data Model

### 2.1 Domain Model

**AuditRecord Aggregate**
```java
public class AuditRecord {
    private final UUID recordId;
    private final String aggregateType;      // "Order", "PickingTask", "InventoryTransaction"
    private final String aggregateId;        // "ORD-001", "PICK-123"
    private final String eventName;          // "OrderCreatedEvent", "PickingTaskCompletedEvent"
    private final LocalDateTime eventTimestamp;
    private final EventMetadata eventMetadata;
    private final String payload;            // JSON serialized event
    private final LocalDateTime createdAt;   // When audit record was created
}
```

**EventMetadata Value Object**
```java
public class EventMetadata {
    private final String context;           // "Order Context", "WES Context"
    private final UUID correlationId;       // For cross-context tracing
    private final String triggerSource;     // "NewOrderObservedEvent", "Manual", "Scheduled"
    private final String triggerBy;         // Optional: user ID or system name
}
```

### 2.2 Database Schema

**Table: audit_records**
```sql
CREATE TABLE audit_records (
  record_id VARCHAR2(36) PRIMARY KEY,
  aggregate_type VARCHAR2(100) NOT NULL,
  aggregate_id VARCHAR2(255) NOT NULL,
  event_name VARCHAR2(255) NOT NULL,
  event_timestamp TIMESTAMP NOT NULL,
  event_metadata CLOB CHECK (event_metadata IS JSON),
  payload CLOB CHECK (payload IS JSON),
  created_at TIMESTAMP NOT NULL,
  CONSTRAINT idx_audit_aggregate INDEX (aggregate_type, aggregate_id),
  CONSTRAINT idx_audit_event_time INDEX (event_timestamp),
  CONSTRAINT idx_audit_created INDEX (created_at)
);
```

**Indexes Rationale:**
- `idx_audit_aggregate`: Query all events for specific aggregate (e.g., all events for Order ORD-001)
- `idx_audit_event_time`: Time-range queries for compliance reports
- `idx_audit_created`: Audit log cleanup jobs

### 2.3 JSON Structure Examples

**EventMetadata JSON:**
```json
{
  "context": "Order Context",
  "correlationId": "550e8400-e29b-41d4-a716-446655440000",
  "triggerSource": "NewOrderObservedEvent",
  "triggerBy": "system:order-observer-1"
}
```

**Payload JSON (OrderCreatedEvent example):**
```json
{
  "orderId": "ORD-001",
  "customerId": "CUST-123",
  "items": [...],
  "status": "PENDING",
  "occurredAt": "2025-11-16T10:00:00"
}
```

---

## 3. Trigger Source Tracking Design (Hybrid Approach)

### 3.1 Design Decision: Approach 1 + Approach 2 Hybrid

We combine two approaches for maximum flexibility and accuracy:

**Approach 1: Explicit TriggerContext (Command-based)**
- Commands carry explicit TriggerContext
- Event handlers pass trigger source to commands
- Domain models include trigger context in events

**Approach 2: Automatic EventContext (ThreadLocal)**
- EventContextHolder stores current processing event
- EventContextInterceptor automatically sets context
- Provides fallback when commands don't carry context

**Priority:** Explicit > Automatic > Default
1. If command has TriggerContext → use it (highest priority)
2. Else if ThreadLocal has EventContext → use it (automatic)
3. Else → "Manual" (default)

### 3.2 Component Design

#### TriggerContext Value Object
```java
public class TriggerContext {
    private final String triggerSource;     // Event name, "Manual", "Scheduled"
    private final UUID correlationId;       // For cross-context tracing
    private final LocalDateTime timestamp;  // When trigger occurred

    public static TriggerContext fromEvent(Object event, UUID correlationId) {
        return new TriggerContext(
            event.getClass().getSimpleName(),
            correlationId,
            LocalDateTime.now()
        );
    }

    public static TriggerContext manual() {
        return new TriggerContext("Manual", UUID.randomUUID(), LocalDateTime.now());
    }

    public static TriggerContext scheduled(String schedulerName) {
        return new TriggerContext("Scheduled:" + schedulerName, UUID.randomUUID(), LocalDateTime.now());
    }
}
```

#### EventContextHolder (ThreadLocal)
```java
@Component
public class EventContextHolder {
    private static final ThreadLocal<EventContext> context = new ThreadLocal<>();

    public static void setCurrentEvent(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();
        UUID correlationId = extractCorrelationId(event);
        context.set(new EventContext(eventName, correlationId));
    }

    public static TriggerContext getCurrentContext() {
        EventContext ctx = context.get();
        if (ctx != null) {
            return new TriggerContext(ctx.getEventName(), ctx.getCorrelationId(), LocalDateTime.now());
        }
        return TriggerContext.manual();  // Default fallback
    }

    public static void clear() {
        context.remove();
    }

    private static UUID extractCorrelationId(DomainEvent event) {
        // Try to extract correlationId via reflection
        try {
            Method method = event.getClass().getMethod("getCorrelationId");
            return (UUID) method.invoke(event);
        } catch (Exception e) {
            return UUID.randomUUID();  // Generate new if not present
        }
    }
}
```

#### EventContextInterceptor
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class EventContextInterceptor {
    private static final Logger logger = LoggerFactory.getLogger(EventContextInterceptor.class);

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void onEventProcessing(DomainEvent event) {
        EventContextHolder.setCurrentEvent(event);
        logger.debug("EventContext set for: {}", event.getClass().getSimpleName());
    }

    @EventListener
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMPLETION)
    public void clearEventContext(DomainEvent event) {
        EventContextHolder.clear();
        logger.debug("EventContext cleared for: {}", event.getClass().getSimpleName());
    }
}
```

### 3.3 Integration Patterns

#### Pattern 1: Event Handler with Explicit TriggerContext (Recommended)
```java
@Component
public class NewOrderObservedEventHandler {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void handleNewOrderObserved(NewOrderObservedEvent event) {
        // Create explicit TriggerContext from the event
        TriggerContext triggerContext = TriggerContext.fromEvent(event, event.getCorrelationId());

        // Pass trigger context to command
        CreateOrderCommand command = new CreateOrderCommand(
            event.getOrderId(),
            event.getCustomerId(),
            event.getItems(),
            triggerContext  // Explicit trigger source
        );

        orderApplicationService.createOrder(command);
    }
}
```

#### Pattern 2: Domain Model Using TriggerContext
```java
public class Order {
    private final List<Object> domainEvents = new ArrayList<>();
    private TriggerContext triggerContext;  // Store trigger context

    public void createOrder(TriggerContext triggerContext) {
        this.status = OrderStatus.PENDING;
        this.triggerContext = triggerContext;

        // Include trigger context in event
        this.domainEvents.add(new OrderCreatedEvent(
            this.orderId,
            this.customerId,
            triggerContext  // Pass through
        ));
    }
}
```

#### Pattern 3: Automatic Fallback (No Code Changes)
```java
// Existing code without changes works via ThreadLocal
@Component
public class SomeExistingEventHandler {

    @TransactionalEventListener
    public void handleSomeEvent(SomeEvent event) {
        // EventContextInterceptor already set context in ThreadLocal
        // AuditLogSubscriber will read from ThreadLocal automatically

        someService.doSomething();  // No changes needed
    }
}
```

### 3.4 Trigger Source Flow Examples

#### Example 1: Order Creation from Observer
```
1. OrderObserverScheduler executes (Scheduled)
   ├─ Sets TriggerContext.scheduled("OrderObserver")
   └─ Publishes: NewOrderObservedEvent(correlationId=UUID-A)

2. EventContextInterceptor catches NewOrderObservedEvent
   └─ Sets ThreadLocal: EventContext("NewOrderObservedEvent", UUID-A)

3. NewOrderObservedEventHandler processes event
   ├─ Creates: TriggerContext.fromEvent(event, UUID-A)
   └─ Creates: CreateOrderCommand(triggerContext)

4. Order.createOrder() receives TriggerContext
   └─ Publishes: OrderCreatedEvent(triggerContext)

5. AuditLogSubscriber records OrderCreatedEvent
   └─ EventMetadata: {
        "triggerSource": "NewOrderObservedEvent",  ← From TriggerContext
        "correlationId": "UUID-A"
      }
```

#### Example 2: Manual API Call
```
1. REST Controller receives request
   ├─ No ThreadLocal context (new thread)
   └─ Creates: TriggerContext.manual()

2. Service creates: CreateOrderCommand(TriggerContext.manual())

3. Order.createOrder() receives TriggerContext
   └─ Publishes: OrderCreatedEvent(triggerContext)

4. AuditLogSubscriber records OrderCreatedEvent
   └─ EventMetadata: {
        "triggerSource": "Manual",  ← From TriggerContext
        "correlationId": "UUID-new"
      }
```

#### Example 3: Event Chain (Automatic Propagation)
```
1. OrderCreatedEvent published (correlationId=UUID-A)

2. EventContextInterceptor sets ThreadLocal
   └─ EventContext("OrderCreatedEvent", UUID-A)

3. OrderCreatedEventHandler processes (no explicit TriggerContext)
   └─ Publishes: OrderScheduledEvent (no explicit context)

4. AuditLogSubscriber records OrderScheduledEvent
   ├─ Reads from EventContextHolder.getCurrentContext()
   └─ EventMetadata: {
        "triggerSource": "OrderCreatedEvent",  ← From ThreadLocal
        "correlationId": "UUID-A"  ← Propagated!
      }
```

---

## 4. Event Chains & Trigger Source Mapping

### 4.1 Chain 1: Order Observation & Creation

```
┌────────────────────────────────────────────────────────────────────┐
│ OrderObserverScheduler.pollAllActiveObservers()                    │
│ TriggerSource: "Scheduled:OrderObserver"                           │
└──────────────────────────┬─────────────────────────────────────────┘
                           │
                           ▼
                  NewOrderObservedEvent
                  correlationId: UUID-A
                  triggerSource: "Scheduled:OrderObserver"
                           │
                           ▼
         ┌─────────────────────────────────────┐
         │ NewOrderObservedEventHandler        │
         │ Sets: TriggerContext.fromEvent()    │
         └──────────────────┬──────────────────┘
                           │
                           ▼
                  CreateOrderCommand
                  triggerContext: {
                    source: "NewOrderObservedEvent",
                    correlationId: UUID-A
                  }
                           │
                           ▼
                  Order.createOrder()
                           │
                           ▼
                  OrderCreatedEvent / OrderScheduledEvent
                  triggerSource: "NewOrderObservedEvent"
                  correlationId: UUID-A
```

**Audit Records Created:**
1. **NewOrderObservedEvent**
   - aggregateType: "ObservedOrder"
   - eventMetadata.triggerSource: "Scheduled:OrderObserver"
   - eventMetadata.correlationId: UUID-A

2. **OrderCreatedEvent**
   - aggregateType: "Order"
   - eventMetadata.triggerSource: "NewOrderObservedEvent"
   - eventMetadata.correlationId: UUID-A

### 4.2 Chain 2: Scheduled Fulfillment

```
FulfillmentScheduler.checkScheduledOrders()
  └─ TriggerSource: "Scheduled:FulfillmentScheduler"
     └─ OrderReadyForFulfillmentEvent
        ├─ triggerSource: "Scheduled:FulfillmentScheduler"
        └─ correlationId: UUID-B
           └─ OrderReadyForFulfillmentEventHandler
              └─ ReserveInventoryCommand
                 └─ triggerContext: {
                      source: "OrderReadyForFulfillmentEvent",
                      correlationId: UUID-B
                    }
                    └─ InventoryReservedEvent / ReservationFailedEvent
                       ├─ triggerSource: "OrderReadyForFulfillmentEvent"
                       └─ correlationId: UUID-B
```

### 4.3 Chain 3: WES Task Creation (Dual Origin)

**Origin A: Orchestrator-Submitted**
```
OrderReservedEvent (correlationId: UUID-C)
  └─ OrderReservedEventHandler
     └─ CreatePickingTaskForOrderCommand
        └─ triggerContext: {
             source: "OrderReservedEvent",
             correlationId: UUID-C
           }
           └─ PickingTaskSubmittedEvent
              ├─ triggerSource: "OrderReservedEvent"
              └─ correlationId: UUID-C
```

**Origin B: WES-Direct**
```
WesObserver.pollWesTaskStatus() (Scheduled)
  └─ WesTaskDiscoveredEvent
     ├─ triggerSource: "Scheduled:WesObserver"
     └─ correlationId: UUID-D (new)
        └─ CreatePickingTaskFromWesCommand
           └─ PickingTaskCreatedEvent
              ├─ triggerSource: "WesTaskDiscoveredEvent"
              └─ correlationId: UUID-D
```

### 4.4 Chain 4: Inventory Discrepancy Detection

```
InventoryObserver.pollInventorySnapshot() (Scheduled)
  └─ InventorySnapshotObservedEvent
     ├─ triggerSource: "Scheduled:InventoryObserver"
     └─ correlationId: UUID-E
        └─ DetectDiscrepancyCommand
           └─ InventoryDiscrepancyDetectedEvent
              ├─ triggerSource: "InventorySnapshotObservedEvent"
              └─ correlationId: UUID-E
                 └─ ApplyAdjustmentCommand
                    └─ InventoryAdjustedEvent
                       ├─ triggerSource: "InventoryDiscrepancyDetectedEvent"
                       └─ correlationId: UUID-E
```

### 4.5 Chain 5: Picking Task Lifecycle

```
PickingTask.markCompleted()
  └─ PickingTaskCompletedEvent
     ├─ triggerSource: "Manual" (WES callback)
     └─ correlationId: UUID-F
        └─ PickingTaskCompletedEventHandler
           └─ ConsumeReservationCommand
              └─ triggerContext: {
                   source: "PickingTaskCompletedEvent",
                   correlationId: UUID-F
                 }
                 └─ ReservationConsumedEvent
                    ├─ triggerSource: "PickingTaskCompletedEvent"
                    └─ correlationId: UUID-F
```

### 4.6 Correlation ID Propagation Rules

**Rule 1: Observer Events Create New Correlation ID**
- NewOrderObservedEvent → new UUID
- WesTaskDiscoveredEvent → new UUID
- InventorySnapshotObservedEvent → new UUID

**Rule 2: Event Chains Preserve Correlation ID**
- All events in a chain share the same correlationId
- Enables full trace from trigger to completion

**Rule 3: Manual Actions Create New Correlation ID**
- API calls → new UUID
- User actions → new UUID with user context

**Rule 4: Cross-Context Boundaries Preserve Correlation ID**
- Order Context → WES Context: same correlationId
- Enables cross-context tracing

---

## 5. Implementation Components

### 5.1 DomainEvent Marker Interface

```java
package com.wei.orchestrator.shared.domain.event;

/**
 * Marker interface for all domain events in the system.
 * Events implementing this interface will be automatically recorded by AuditLogSubscriber.
 */
public interface DomainEvent {

    /**
     * Returns the correlation ID for cross-event tracing.
     * Default implementation generates a new UUID.
     * Events should override this to propagate existing correlation IDs.
     */
    default UUID getCorrelationId() {
        return UUID.randomUUID();
    }

    /**
     * Returns the timestamp when this event occurred.
     * Default implementation returns current time.
     */
    default LocalDateTime getOccurredAt() {
        return LocalDateTime.now();
    }

    /**
     * Returns the trigger context if explicitly provided.
     * Default implementation returns null (will fallback to ThreadLocal).
     */
    default TriggerContext getTriggerContext() {
        return null;
    }
}
```

### 5.2 AuditService - Metadata Extraction

```java
package com.wei.orchestrator.audit.domain.service;

@Service
public class AuditService {
    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);
    private final ObjectMapper objectMapper;

    public AuditRecord createAuditRecord(DomainEvent event) {
        try {
            String eventName = event.getClass().getSimpleName();
            String context = extractContext(event);
            String aggregateType = extractAggregateType(event);
            String aggregateId = extractAggregateId(event);
            TriggerContext triggerContext = resolveTriggerContext(event);

            EventMetadata metadata = new EventMetadata(
                context,
                event.getCorrelationId(),
                triggerContext.getTriggerSource(),
                triggerContext.getTriggerBy()
            );

            String payload = serializePayload(event);

            return new AuditRecord(
                UUID.randomUUID(),
                aggregateType,
                aggregateId,
                eventName,
                event.getOccurredAt(),
                metadata,
                payload,
                LocalDateTime.now()
            );

        } catch (Exception e) {
            logger.error("Failed to create audit record for event: {}", event, e);
            throw new AuditRecordCreationException("Failed to create audit record", e);
        }
    }

    private String extractContext(DomainEvent event) {
        String packageName = event.getClass().getPackageName();

        if (packageName.contains(".order.")) return "Order Context";
        if (packageName.contains(".wes.")) return "WES Context";
        if (packageName.contains(".inventory.")) return "Inventory Context";
        if (packageName.contains(".observation.")) return "Observation Context";

        return "Unknown Context";
    }

    private String extractAggregateType(DomainEvent event) {
        String eventName = event.getClass().getSimpleName();

        // Pattern: OrderCreatedEvent → Order
        // Pattern: PickingTaskCompletedEvent → PickingTask
        // Pattern: InventoryReservedEvent → Inventory

        String[] patterns = {
            "Order", "PickingTask", "Inventory", "Reservation",
            "ObservedOrder", "WesTask", "Discrepancy", "Adjustment"
        };

        for (String pattern : patterns) {
            if (eventName.contains(pattern)) {
                return pattern;
            }
        }

        return "Unknown";
    }

    private String extractAggregateId(DomainEvent event) {
        // Try common patterns: orderId, pickingTaskId, reservationId, etc.
        try {
            Method[] methods = event.getClass().getMethods();
            for (Method method : methods) {
                String methodName = method.getName();
                if (methodName.matches("get[A-Z]\\w+Id") && method.getParameterCount() == 0) {
                    Object id = method.invoke(event);
                    if (id != null) {
                        return id.toString();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract aggregate ID for event: {}", event.getClass().getSimpleName(), e);
        }

        return "UNKNOWN";
    }

    private TriggerContext resolveTriggerContext(DomainEvent event) {
        // Priority 1: Explicit TriggerContext from event
        TriggerContext explicit = event.getTriggerContext();
        if (explicit != null) {
            logger.debug("Using explicit TriggerContext for {}", event.getClass().getSimpleName());
            return explicit;
        }

        // Priority 2: ThreadLocal EventContext
        TriggerContext fromThreadLocal = EventContextHolder.getCurrentContext();
        logger.debug("Using ThreadLocal TriggerContext for {}: {}",
            event.getClass().getSimpleName(), fromThreadLocal.getTriggerSource());
        return fromThreadLocal;
    }

    private String serializePayload(DomainEvent event) throws JsonProcessingException {
        return objectMapper.writeValueAsString(event);
    }
}
```

### 5.3 AuditLogSubscriber - Global Event Listener

```java
package com.wei.orchestrator.audit.application.eventhandler;

@Component
public class AuditLogSubscriber {
    private static final Logger logger = LoggerFactory.getLogger(AuditLogSubscriber.class);

    private final AuditService auditService;
    private final AuditRecordRepository auditRecordRepository;

    public AuditLogSubscriber(
            AuditService auditService,
            AuditRecordRepository auditRecordRepository) {
        this.auditService = auditService;
        this.auditRecordRepository = auditRecordRepository;
    }

    /**
     * Listens to ALL DomainEvent implementations across all contexts.
     * Uses @Async for non-blocking processing.
     * Uses AFTER_COMMIT to only record events after successful business transaction.
     */
    @Async("auditExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onDomainEvent(DomainEvent event) {
        try {
            logger.debug("Recording audit for event: {}", event.getClass().getSimpleName());

            AuditRecord auditRecord = auditService.createAuditRecord(event);
            auditRecordRepository.save(auditRecord);

            logger.info("Audit record created: recordId={}, event={}, aggregate={}/{}",
                auditRecord.getRecordId(),
                auditRecord.getEventName(),
                auditRecord.getAggregateType(),
                auditRecord.getAggregateId());

        } catch (Exception e) {
            // IMPORTANT: Do not throw exception to avoid affecting business transaction
            logger.error("Failed to record audit for event: {}", event.getClass().getSimpleName(), e);
            // Could publish AuditFailureEvent here for monitoring
        }
    }
}
```

### 5.4 Async Configuration

```java
package com.wei.orchestrator.shared.infrastructure.config;

@Configuration
@EnableAsync
public class AsyncConfiguration implements AsyncConfigurer {

    @Bean(name = "auditExecutor")
    public Executor auditExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("audit-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (throwable, method, params) -> {
            logger.error("Async execution failed: method={}, params={}",
                method.getName(), Arrays.toString(params), throwable);
        };
    }
}
```

### 5.5 Repository Layer

**Domain Repository Interface:**
```java
package com.wei.orchestrator.audit.domain.repository;

public interface AuditRecordRepository {
    void save(AuditRecord auditRecord);
    Optional<AuditRecord> findById(UUID recordId);
    List<AuditRecord> findByAggregateTypeAndId(String aggregateType, String aggregateId);
    List<AuditRecord> findByEventTimestampBetween(LocalDateTime start, LocalDateTime end);
    List<AuditRecord> findByCorrelationId(UUID correlationId);
}
```

**JPA Entity:**
```java
package com.wei.orchestrator.audit.infrastructure.persistence;

@Entity
@Table(name = "audit_records")
public class AuditRecordEntity {
    @Id
    @Column(name = "record_id", length = 36)
    private String recordId;

    @Column(name = "aggregate_type", nullable = false, length = 100)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 255)
    private String aggregateId;

    @Column(name = "event_name", nullable = false, length = 255)
    private String eventName;

    @Column(name = "event_timestamp", nullable = false)
    private LocalDateTime eventTimestamp;

    @Lob
    @Column(name = "event_metadata", nullable = false)
    private String eventMetadata;  // JSON string

    @Lob
    @Column(name = "payload", nullable = false)
    private String payload;  // JSON string

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    // Getters, setters, converters to/from domain model
}
```

**Repository Implementation:**
```java
package com.wei.orchestrator.audit.infrastructure.repository;

@Repository
public class AuditRecordRepositoryImpl implements AuditRecordRepository {
    private final AuditRecordJpaRepository jpaRepository;
    private final AuditRecordMapper mapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(AuditRecord auditRecord) {
        AuditRecordEntity entity = mapper.toEntity(auditRecord, objectMapper);
        jpaRepository.save(entity);
    }

    @Override
    public List<AuditRecord> findByCorrelationId(UUID correlationId) {
        // Query by JSON field - Oracle syntax
        String sql = "SELECT * FROM audit_records " +
                     "WHERE JSON_VALUE(event_metadata, '$.correlationId') = ?";
        // Implementation using native query or custom JSON function
    }

    // Other repository methods...
}
```

---

## 6. Integration Strategy: Non-Intrusive Approach

### 6.1 Minimal Changes Required

**Step 1: Update Existing Events (One-Line Change)**
```java
// Before:
public class OrderCreatedEvent {
    private final String orderId;
    private final LocalDateTime occurredAt;
}

// After:
public class OrderCreatedEvent implements DomainEvent {  // ← Only this line added
    private final String orderId;
    private final LocalDateTime occurredAt;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    // Add correlationId and triggerContext fields + getters
}
```

**Step 2: Update Event Handlers (Optional but Recommended)**
```java
// Minimal change: Just implement DomainEvent, no other changes
// Audit works via ThreadLocal automatically

// Recommended: Add explicit TriggerContext
@TransactionalEventListener
public void handleEvent(SomeEvent event) {
    TriggerContext context = TriggerContext.fromEvent(event, event.getCorrelationId());
    Command command = new Command(..., context);  // ← Add trigger context
    service.execute(command);
}
```

**Step 3: Update Commands (Optional)**
```java
// Optional: Add TriggerContext field to commands
public class CreateOrderCommand {
    private final String orderId;
    private final TriggerContext triggerContext;  // ← Optional field
}
```

**Step 4: Update Domain Models (Optional)**
```java
// Optional: Accept and use TriggerContext
public class Order {
    public void createOrder(TriggerContext triggerContext) {
        this.domainEvents.add(new OrderCreatedEvent(
            this.orderId,
            triggerContext  // ← Pass through
        ));
    }
}
```

### 6.2 Gradual Migration Path

**Phase 1: Basic Audit (No Code Changes)**
- Deploy AuditLogSubscriber
- Deploy EventContextInterceptor
- All events implementing DomainEvent are automatically audited
- Trigger source: Detected via ThreadLocal (automatic)
- Works immediately but less accurate

**Phase 2: Event Updates (Minimal Changes)**
- Add `implements DomainEvent` to all events
- Add correlationId field
- Add getTriggerContext() method
- Trigger source: Still via ThreadLocal but with correlation

**Phase 3: Handler Updates (Recommended)**
- Update event handlers to create explicit TriggerContext
- Pass TriggerContext to commands
- Trigger source: Accurate event names

**Phase 4: Full Integration (Complete)**
- Update domain models to accept TriggerContext
- Update commands to carry TriggerContext
- Trigger source: Fully accurate with correlation propagation

### 6.3 Migration Example: Order Context

**Current State:**
```java
// OrderCreatedEvent.java - Current
public class OrderCreatedEvent {
    private final String orderId;
}

// NewOrderObservedEventHandler.java - Current
@TransactionalEventListener
public void handle(NewOrderObservedEvent event) {
    CreateOrderCommand command = new CreateOrderCommand(event.getOrderId());
    orderService.createOrder(command);
}
```

**After Phase 2 (Minimal):**
```java
// OrderCreatedEvent.java - After Phase 2
public class OrderCreatedEvent implements DomainEvent {
    private final String orderId;
    private final UUID correlationId;
    private final TriggerContext triggerContext;

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    @Override
    public TriggerContext getTriggerContext() {
        return triggerContext;
    }
}

// Handler - No changes yet (ThreadLocal works)
```

**After Phase 3 (Recommended):**
```java
// NewOrderObservedEventHandler.java - After Phase 3
@TransactionalEventListener
public void handle(NewOrderObservedEvent event) {
    TriggerContext context = TriggerContext.fromEvent(event, event.getCorrelationId());
    CreateOrderCommand command = new CreateOrderCommand(
        event.getOrderId(),
        context  // ← Add trigger context
    );
    orderService.createOrder(command);
}
```

---

## 7. Testing Strategy

### 7.1 Unit Tests

**AuditServiceTest.java**
- Test metadata extraction for each context (Order, WES, Inventory)
- Test aggregate type detection from event names
- Test aggregate ID extraction via reflection
- Test trigger context resolution (explicit > ThreadLocal > Manual)
- Test payload serialization
- Test error handling for malformed events

**EventContextHolderTest.java**
- Test ThreadLocal set/get/clear operations
- Test correlation ID extraction
- Test fallback to manual when no context
- Test thread isolation (context doesn't leak across threads)

**AuditLogSubscriberTest.java**
- Test event recording for different event types
- Test async processing
- Test graceful failure (exception doesn't propagate)
- Test transaction propagation (REQUIRES_NEW)

### 7.2 Integration Tests

**AuditRecordIntegrationTest.java**
```java
@SpringBootTest
@Transactional
public class AuditRecordIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private AuditRecordRepository auditRecordRepository;

    @Test
    public void shouldRecordOrderCreatedEvent() {
        // Given
        UUID correlationId = UUID.randomUUID();
        TriggerContext context = TriggerContext.manual();
        OrderCreatedEvent event = new OrderCreatedEvent("ORD-001", correlationId, context);

        // When
        eventPublisher.publishEvent(event);

        // Wait for async processing
        await().atMost(2, SECONDS).until(() ->
            !auditRecordRepository.findByAggregateId("ORD-001").isEmpty()
        );

        // Then
        List<AuditRecord> records = auditRecordRepository.findByAggregateId("ORD-001");
        assertThat(records).hasSize(1);

        AuditRecord record = records.get(0);
        assertThat(record.getAggregateType()).isEqualTo("Order");
        assertThat(record.getEventName()).isEqualTo("OrderCreatedEvent");
        assertThat(record.getEventMetadata().getTriggerSource()).isEqualTo("Manual");
        assertThat(record.getEventMetadata().getCorrelationId()).isEqualTo(correlationId);
    }

    @Test
    public void shouldPropagateCorrelationIdThroughEventChain() {
        // Given
        UUID correlationId = UUID.randomUUID();

        // When - Publish first event
        NewOrderObservedEvent observedEvent = new NewOrderObservedEvent("ORD-002", correlationId);
        eventPublisher.publishEvent(observedEvent);

        // ... rest of test verifying all events in chain have same correlationId
    }
}
```

**EventContextPropagationTest.java**
- Test ThreadLocal context setting via EventContextInterceptor
- Test context propagation through event handler
- Test context cleanup after transaction
- Test multiple events in same transaction

### 7.3 End-to-End Event Chain Tests

**OrderCreationChainTest.java**
```java
@SpringBootTest
public class OrderCreationChainTest {

    @Test
    public void shouldAuditCompleteOrderCreationChain() {
        // Given
        UUID correlationId = UUID.randomUUID();

        // When - Simulate observer finding new order
        NewOrderObservedEvent observedEvent = new NewOrderObservedEvent("ORD-003", correlationId);
        eventPublisher.publishEvent(observedEvent);

        // Wait for chain to complete
        await().atMost(5, SECONDS).until(() ->
            auditRecordRepository.findByCorrelationId(correlationId).size() >= 2
        );

        // Then - Verify audit trail
        List<AuditRecord> auditTrail = auditRecordRepository.findByCorrelationId(correlationId);

        assertThat(auditTrail).hasSize(2);
        assertThat(auditTrail).extracting("eventName").containsExactlyInAnyOrder(
            "NewOrderObservedEvent",
            "OrderCreatedEvent"
        );

        // Verify trigger source chain
        AuditRecord observedRecord = findByEventName(auditTrail, "NewOrderObservedEvent");
        assertThat(observedRecord.getEventMetadata().getTriggerSource()).isEqualTo("Scheduled:OrderObserver");

        AuditRecord createdRecord = findByEventName(auditTrail, "OrderCreatedEvent");
        assertThat(createdRecord.getEventMetadata().getTriggerSource()).isEqualTo("NewOrderObservedEvent");

        // Verify correlation ID propagation
        assertThat(auditTrail).extracting(r -> r.getEventMetadata().getCorrelationId())
            .containsOnly(correlationId);
    }
}
```

### 7.4 Performance Tests

**AuditPerformanceTest.java**
- Test audit recording doesn't slow down business transactions
- Measure overhead: Business transaction time should increase < 5%
- Test async queue doesn't overflow under load
- Test database write throughput (target: 1000+ audit records/second)

---

## 8. Migration Plan

### Phase 1: Infrastructure Setup (Week 1)
**Goal:** Deploy audit infrastructure without affecting existing code

**Tasks:**
1. Create audit package structure (`audit/domain`, `audit/application`, `audit/infrastructure`)
2. Create database migration: `V{version}__create_audit_records_table.sql`
3. Create `DomainEvent` marker interface
4. Create `TriggerContext`, `EventContext`, `EventMetadata` value objects
5. Create `EventContextHolder` utility class
6. Create `AsyncConfiguration` with audit executor
7. Deploy to staging (no events implementing DomainEvent yet → no audit records)

**Testing:**
- Verify database table created
- Verify async executor starts
- Verify no errors in logs

**Success Criteria:**
- ✅ audit_records table exists
- ✅ Application starts successfully
- ✅ No impact on existing functionality

---

### Phase 2: Audit Components Implementation (Week 2)
**Goal:** Implement and test audit recording components

**Tasks:**
1. Implement `AuditRecord` domain model
2. Implement `AuditService` with metadata extraction
3. Implement `AuditRecordEntity` and repository
4. Implement `AuditLogSubscriber`
5. Implement `EventContextInterceptor`
6. Write comprehensive unit tests
7. Write integration tests
8. Test with one dummy event implementing DomainEvent

**Testing:**
- Unit test coverage > 80%
- Integration tests pass
- Verify dummy event is audited correctly
- Verify async processing works
- Verify no exceptions

**Success Criteria:**
- ✅ All tests pass
- ✅ Dummy event audited successfully
- ✅ Async processing works
- ✅ No performance degradation

---

### Phase 3: Event Updates - Observation Context (Week 3)
**Goal:** Update all Observation Context events to implement DomainEvent

**Events to Update:**
- `NewOrderObservedEvent`
- `WesTaskDiscoveredEvent`
- `InventorySnapshotObservedEvent`

**Tasks:**
1. Add `implements DomainEvent` to each event
2. Add `correlationId` field (UUID)
3. Add `triggerContext` field (TriggerContext)
4. Add getters for `getCorrelationId()`, `getTriggerContext()`
5. Update constructors to accept these fields
6. Update observer schedulers to create TriggerContext.scheduled()
7. Test each observer end-to-end

**Testing:**
- Run observer schedulers
- Verify audit records created
- Verify triggerSource = "Scheduled:ObserverName"
- Verify correlation IDs generated

**Success Criteria:**
- ✅ All Observation events audited
- ✅ Trigger source shows "Scheduled:..."
- ✅ No duplicate events (existing distributed locking prevents this)

---

### Phase 4: Event Updates - Order Context (Week 4)
**Goal:** Update all Order Context events and handlers

**Events to Update:**
- `OrderCreatedEvent`
- `OrderScheduledEvent`
- `OrderReadyForFulfillmentEvent`
- `OrderReservedEvent`
- `OrderCancelledEvent`

**Tasks:**
1. Update events to implement DomainEvent
2. Update event handlers to create and pass TriggerContext
3. Update commands to accept TriggerContext (optional but recommended)
4. Update Order aggregate to use TriggerContext
5. Test order creation flow end-to-end

**Testing:**
- Create order via observer
- Verify complete audit trail
- Verify trigger source chain: Scheduled → NewOrderObservedEvent → OrderCreatedEvent
- Verify correlation ID propagation

**Success Criteria:**
- ✅ Order context events audited
- ✅ Trigger source chain accurate
- ✅ Correlation ID propagated through chain

---

### Phase 5: Event Updates - WES Context (Week 5)
**Goal:** Update all WES Context events and handlers

**Events to Update:**
- `PickingTaskCreatedEvent`
- `PickingTaskSubmittedEvent`
- `PickingTaskCompletedEvent`
- `PickingTaskCancelledEvent`

**Tasks:**
1. Update events to implement DomainEvent
2. Update event handlers with TriggerContext
3. Update PickingTask aggregate
4. Test picking task flows (both origins: orchestrator-submitted and WES-direct)

**Testing:**
- Test orchestrator-submitted flow
- Test WES-direct flow
- Verify different correlation IDs for different origins
- Verify trigger source accuracy

**Success Criteria:**
- ✅ WES context events audited
- ✅ Both picking task origins tracked correctly
- ✅ Correlation IDs distinguish different flows

---

### Phase 6: Event Updates - Inventory Context (Week 6)
**Goal:** Update all Inventory Context events and handlers

**Events to Update:**
- `InventoryReservedEvent`
- `ReservationFailedEvent`
- `ReservationConsumedEvent`
- `ReservationReleasedEvent`
- `InventoryDiscrepancyDetectedEvent`
- `InventoryAdjustedEvent`

**Tasks:**
1. Update events to implement DomainEvent
2. Update event handlers with TriggerContext
3. Update InventoryTransaction aggregate
4. Test reservation lifecycle
5. Test discrepancy detection flow

**Testing:**
- Test reserve → consume flow
- Test reserve → release flow
- Test discrepancy detection → adjustment flow
- Verify correlation ID propagation across contexts (Order → Inventory)

**Success Criteria:**
- ✅ Inventory context events audited
- ✅ Cross-context correlation working (Order correlationId → Inventory events)
- ✅ Reservation lifecycle fully audited

---

### Phase 7: Production Deployment (Week 7)
**Goal:** Deploy to production with monitoring

**Tasks:**
1. Deploy to production
2. Enable audit logging for all contexts
3. Monitor metrics (audit record creation rate, async queue size, errors)
4. Monitor database growth
5. Set up alerts for audit failures
6. Document runbook for troubleshooting

**Monitoring:**
- Audit records created per minute
- Async executor queue size
- Async executor rejected count
- Audit failures (exceptions in AuditLogSubscriber)
- Database table size growth rate

**Success Criteria:**
- ✅ All events being audited
- ✅ No performance degradation (< 5% overhead)
- ✅ No audit failures
- ✅ Audit trail queryable and accurate

---

### Phase 8: Audit Query API (Week 8+)
**Goal:** Provide API for querying audit trail

**Tasks:**
1. Create REST API for audit queries
   - GET /api/audit/aggregates/{type}/{id} - Get all events for an aggregate
   - GET /api/audit/correlations/{correlationId} - Trace event chain
   - GET /api/audit/events?from={timestamp}&to={timestamp} - Time range query
2. Implement pagination
3. Add security/authorization
4. Create admin dashboard UI (optional)

**Success Criteria:**
- ✅ Audit trail queryable via API
- ✅ Cross-context tracing works
- ✅ Performance acceptable (< 1s for typical queries)

---

## 9. Monitoring & Observability

### 9.1 Metrics

**Audit Recording Metrics:**
- `audit.records.created.total` (counter) - Total audit records created
- `audit.records.created.by_context{context="Order"}` (counter) - Per-context audit records
- `audit.records.failed.total` (counter) - Failed audit record attempts
- `audit.processing.duration.seconds` (histogram) - Time to create and save audit record
- `audit.async.queue.size` (gauge) - Current async queue size
- `audit.async.rejected.total` (counter) - Rejected async tasks

**Trigger Source Distribution:**
- `audit.trigger_source{source="NewOrderObservedEvent"}` (counter) - Count by trigger source
- `audit.trigger_source{source="Manual"}` (counter)
- `audit.trigger_source{source="Scheduled:OrderObserver"}` (counter)

**Correlation Metrics:**
- `audit.correlation.chain_length` (histogram) - Number of events per correlation ID
- `audit.correlation.unique.total` (counter) - Unique correlation IDs created

### 9.2 Dashboards

**Audit Overview Dashboard:**
```
┌─────────────────────────────────────────┐
│ Audit Records Created (Last 24h)        │
│ ▓▓▓▓▓▓▓▓▓▓▓▓▓ 145,234                  │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Audit Records by Context                │
│ Order:     65,432 ████████████          │
│ WES:       42,156 ████████              │
│ Inventory: 28,901 ██████                │
│ Observation: 8,745 ██                   │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Trigger Source Distribution             │
│ NewOrderObservedEvent: 30%              │
│ Manual: 25%                             │
│ Scheduled: 20%                          │
│ Other events: 25%                       │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Async Queue Status                      │
│ Queue size: 12 / 100                    │
│ Rejected: 0                             │
└─────────────────────────────────────────┘
```

**Event Chain Dashboard:**
```
┌─────────────────────────────────────────┐
│ Top Event Chains (by frequency)         │
│ 1. NewOrderObserved → OrderCreated      │
│ 2. OrderReserved → PickingTaskSubmitted │
│ 3. PickingTaskCompleted → ReservationConsumed │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ Average Chain Length by Origin          │
│ NewOrderObservedEvent: 4.2 events       │
│ WesTaskDiscoveredEvent: 2.1 events      │
│ Manual: 1.5 events                      │
└─────────────────────────────────────────┘
```

### 9.3 Alerts

**🔴 Critical (Page immediately):**
- Audit failure rate > 5% for 5+ minutes
  - **Impact:** Losing audit trail, compliance risk
  - **Action:** Check AuditLogSubscriber logs, check database connectivity

- Async queue rejected count > 10 in 1 minute
  - **Impact:** Audit records being dropped
  - **Action:** Increase async executor pool size, check database performance

- Zero audit records for 10+ minutes during business hours
  - **Impact:** Audit system down
  - **Action:** Check AuditLogSubscriber, check database, check event publishing

**🟡 Warning (Notify on-call):**
- Async queue size > 80% capacity for 5+ minutes
  - **Impact:** Approaching capacity, may start rejecting
  - **Action:** Monitor, consider increasing pool size

- Audit processing time p95 > 1 second for 10+ minutes
  - **Impact:** Slow audit recording, queue buildup
  - **Action:** Check database performance, check serialization performance

- Database table size growth > 10GB/day (unexpected)
  - **Impact:** Storage concerns
  - **Action:** Verify event volume is expected, consider archival strategy

**⚪ Info (Log only):**
- Occasional audit failures (< 1%)
- Temporary queue buildup (< 5 minutes)
- Expected database growth

### 9.4 Log Messages

**INFO level:**
```
[AuditLogSubscriber] Audit record created: recordId=123e4567-e89b-12d3-a456-426614174000, event=OrderCreatedEvent, aggregate=Order/ORD-001
[AuditService] Recording audit for OrderCreatedEvent with trigger source: NewOrderObservedEvent
```

**DEBUG level:**
```
[EventContextInterceptor] EventContext set for: NewOrderObservedEvent
[AuditService] Using explicit TriggerContext for OrderCreatedEvent
[AuditService] Extracted aggregateType=Order, aggregateId=ORD-001 for OrderCreatedEvent
[EventContextInterceptor] EventContext cleared for: NewOrderObservedEvent
```

**WARN level:**
```
[AuditService] Failed to extract aggregate ID for SomeEvent, using UNKNOWN
[AuditLogSubscriber] Async queue approaching capacity: 85/100
```

**ERROR level:**
```
[AuditLogSubscriber] Failed to record audit for event: OrderCreatedEvent
[AuditService] Failed to serialize payload for event: InvalidEvent
[AuditRecordRepository] Database error saving audit record: Connection timeout
```

---

## 10. Database Management

### 10.1 Table Growth Estimation

**Assumptions:**
- Average event payload: 2KB
- Events per day: 100,000 (estimated based on order volume)
- Retention period: 1 year

**Growth Rate:**
```
Daily: 100,000 events × 2KB = 200MB/day
Monthly: 200MB × 30 = 6GB/month
Yearly: 6GB × 12 = 72GB/year
```

### 10.2 Archival Strategy

**Option 1: Time-Based Archival**
```sql
-- Archive records older than 90 days to archive table
INSERT INTO audit_records_archive
SELECT * FROM audit_records
WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90' DAY;

DELETE FROM audit_records
WHERE created_at < CURRENT_TIMESTAMP - INTERVAL '90' DAY;
```

**Option 2: Partitioning**
```sql
-- Partition by month
ALTER TABLE audit_records
PARTITION BY RANGE (created_at) (
  PARTITION p202511 VALUES LESS THAN (TO_DATE('2025-12-01', 'YYYY-MM-DD')),
  PARTITION p202512 VALUES LESS THAN (TO_DATE('2026-01-01', 'YYYY-MM-DD')),
  ...
);

-- Drop old partitions
ALTER TABLE audit_records DROP PARTITION p202501;
```

**Option 3: Export to Data Lake**
```java
@Scheduled(cron = "0 0 2 * * *")  // Daily at 2 AM
public void exportToDataLake() {
    LocalDateTime cutoff = LocalDateTime.now().minusDays(30);
    List<AuditRecord> oldRecords = auditRecordRepository.findOlderThan(cutoff);

    // Export to S3/Data Lake
    dataLakeExporter.export(oldRecords);

    // Delete from active table
    auditRecordRepository.deleteOlderThan(cutoff);
}
```

### 10.3 Query Optimization

**Index Strategy:**
```sql
-- Primary queries
CREATE INDEX idx_audit_aggregate ON audit_records(aggregate_type, aggregate_id);
CREATE INDEX idx_audit_event_time ON audit_records(event_timestamp);
CREATE INDEX idx_audit_created ON audit_records(created_at);

-- JSON index for correlation ID (Oracle)
CREATE INDEX idx_audit_correlation ON audit_records(
  JSON_VALUE(event_metadata, '$.correlationId')
);

-- Composite index for common query pattern
CREATE INDEX idx_audit_type_time ON audit_records(aggregate_type, event_timestamp);
```

**Query Performance Targets:**
- Single aggregate query: < 100ms
- Correlation trace query: < 500ms
- Time range query (1 day): < 1s
- Full-text search: < 2s

---

## 11. Security & Compliance

### 11.1 Data Privacy

**PII Handling:**
- Do NOT include sensitive customer data (names, addresses, payment info) in event payloads
- If PII must be audited, encrypt payload column
- Consider separate audit table for PII events with stricter access control

**Sensitive Field Masking:**
```java
public class AuditService {

    private String serializePayload(DomainEvent event) throws JsonProcessingException {
        // Clone event and mask sensitive fields
        Object sanitizedEvent = maskSensitiveFields(event);
        return objectMapper.writeValueAsString(sanitizedEvent);
    }

    private Object maskSensitiveFields(DomainEvent event) {
        // Mask fields like: customerEmail, phoneNumber, creditCardNumber
        // Implementation using reflection or custom serializers
    }
}
```

### 11.2 Access Control

**Read Access:**
- Audit records should only be accessible to authorized users (admins, compliance officers)
- Implement role-based access control for audit query API
- Log all audit record queries (audit the audit!)

**Write Access:**
- Only AuditLogSubscriber can write audit records
- Database user for application should have INSERT-only permission on audit_records table
- No UPDATE or DELETE permissions (immutability)

### 11.3 Compliance Requirements

**Audit Trail Integrity:**
- Audit records are immutable (no updates or deletes except archival)
- Consider cryptographic signing of audit records for tamper-evidence
- Maintain chain of custody for archived records

**Retention Policies:**
- Comply with regulatory requirements (e.g., SOX, GDPR, HIPAA)
- Typical retention: 7 years for financial records
- Implement automated retention enforcement

---

## 12. Troubleshooting Guide

### 12.1 Common Issues

**Issue: Audit records not being created**

**Symptoms:**
- No records in audit_records table
- No logs from AuditLogSubscriber

**Diagnosis:**
```bash
# Check if events implement DomainEvent
grep -r "implements DomainEvent" src/

# Check if AuditLogSubscriber is registered
# Look for bean registration logs

# Check async executor status
# Look for "auditExecutor" in logs

# Check database connectivity
# Query: SELECT COUNT(*) FROM audit_records;
```

**Solutions:**
- Verify events implement DomainEvent interface
- Verify @TransactionalEventListener is enabled
- Verify async executor is configured
- Check database permissions

---

**Issue: Trigger source always shows "Manual"**

**Symptoms:**
- EventMetadata.triggerSource is always "Manual"
- Expected event names not appearing

**Diagnosis:**
```bash
# Check if EventContextInterceptor is running
# Look for "EventContext set for:" in DEBUG logs

# Check if TriggerContext is being passed
# Add debug logging in event handlers
```

**Solutions:**
- Verify EventContextInterceptor is registered
- Verify event handlers are creating TriggerContext
- Verify commands are accepting and passing TriggerContext
- Check ThreadLocal is not being cleared prematurely

---

**Issue: Correlation IDs not propagating**

**Symptoms:**
- Different correlation IDs for events in same chain
- Cannot trace event flow

**Diagnosis:**
```bash
# Query audit records for a known order
SELECT event_name,
       JSON_VALUE(event_metadata, '$.correlationId') as correlation_id
FROM audit_records
WHERE aggregate_id = 'ORD-001'
ORDER BY event_timestamp;

# Should show same correlation ID for all events
```

**Solutions:**
- Verify events have getCorrelationId() method
- Verify correlation ID is being passed to new events
- Verify EventContextHolder.extractCorrelationId() is working
- Check that event constructors accept and store correlationId

---

**Issue: Async queue rejecting tasks**

**Symptoms:**
- `audit.async.rejected.total` metric increasing
- Logs: "Task rejected from auditExecutor"

**Diagnosis:**
```bash
# Check current queue size
# Metric: audit.async.queue.size

# Check thread pool utilization
# Look for "auditExecutor" threads in thread dump
```

**Solutions:**
- Increase async executor pool size
- Increase queue capacity
- Check database write performance (may be bottleneck)
- Consider batching audit writes

---

## 13. Future Enhancements

### 13.1 Real-Time Event Streaming

**Kafka Integration:**
```java
@Component
public class AuditEventPublisher {

    @KafkaListener(topics = "audit-events")
    public void publishToKafka(AuditRecord auditRecord) {
        kafkaTemplate.send("audit-events", auditRecord);
    }
}
```

**Use Cases:**
- Real-time dashboards
- Event-driven analytics
- External system notifications

### 13.2 ElasticSearch Integration

**Search Optimization:**
```java
@Component
public class AuditSearchIndexer {

    @Async
    @TransactionalEventListener
    public void indexAuditRecord(AuditRecord auditRecord) {
        elasticsearchTemplate.index(auditRecord);
    }
}
```

**Benefits:**
- Full-text search on payloads
- Complex aggregation queries
- Better performance for large date ranges

### 13.3 Event Replay Capability

**Replay API:**
```java
@Service
public class EventReplayService {

    public void replayEventsForAggregate(String aggregateType, String aggregateId) {
        List<AuditRecord> events = auditRecordRepository.findByAggregateTypeAndId(
            aggregateType, aggregateId
        );

        events.stream()
            .sorted(Comparator.comparing(AuditRecord::getEventTimestamp))
            .forEach(record -> {
                DomainEvent event = deserializeEvent(record.getPayload());
                eventPublisher.publishEvent(event);
            });
    }
}
```

**Use Cases:**
- Rebuild read models
- Migrate to new schema
- Debug issues by replaying event sequences

### 13.4 Anomaly Detection

**ML-Based Monitoring:**
- Detect unusual event patterns
- Identify potential security breaches
- Alert on anomalous event chains

---

## 14. Success Criteria

### 14.1 Functional Requirements

- ✅ Every domain event across all contexts (Order, WES, Inventory, Observation) is recorded
- ✅ Trigger source is accurately captured for 95%+ of events
- ✅ Correlation IDs enable complete event chain tracing across contexts
- ✅ Audit records are immutable (no updates/deletes except archival)
- ✅ Audit failures do not cause business transaction failures
- ✅ Audit query API provides < 1s response time for typical queries

### 14.2 Performance Requirements

- ✅ Audit recording overhead < 5% on business transaction latency
- ✅ Async processing handles 1000+ events/second
- ✅ Audit record creation latency p95 < 500ms
- ✅ Database write throughput > 1000 records/second
- ✅ Query performance: single aggregate < 100ms, correlation trace < 500ms

### 14.3 Operational Requirements

- ✅ Audit failure rate < 1% under normal conditions
- ✅ Clear monitoring dashboards show audit health
- ✅ Alerts trigger on critical issues (failures, queue overflow)
- ✅ Runbook available for troubleshooting common issues
- ✅ Automated archival/cleanup for old records
- ✅ Role-based access control for audit queries

### 14.4 Integration Requirements

- ✅ Non-intrusive: Existing business logic requires minimal changes
- ✅ All existing events implement DomainEvent (one-line change)
- ✅ Event handlers optionally use explicit TriggerContext (recommended but not required)
- ✅ ThreadLocal fallback works for code not yet updated
- ✅ Gradual migration possible (context by context)

### 14.5 Compliance Requirements

- ✅ Complete audit trail for all business events
- ✅ Immutable audit records (tamper-evident)
- ✅ Retention policy enforcement (configurable)
- ✅ Access control and audit of audit queries
- ✅ PII handling complies with privacy regulations
- ✅ Export capability for regulatory reporting

---

## 15. References

### 15.1 Related Documentation
- backend/README.md (lines 1880-1892): AuditRecord specification
- backend/README.md (lines 1303-1307): Audit Logging requirements
- backend/spec/order-observer-distributed-locking.md: Observer implementation patterns

### 15.2 Technology References
- Spring Framework Events: https://docs.spring.io/spring-framework/reference/core/beans/context-introduction.html#context-functionality-events
- Spring @Async: https://docs.spring.io/spring-framework/reference/integration/scheduling.html#scheduling-annotation-support-async
- Oracle JSON Support: https://docs.oracle.com/en/database/oracle/oracle-database/21/adjsn/
- Jackson ObjectMapper: https://github.com/FasterXML/jackson-databind

### 15.3 Design Patterns
- Domain Events (DDD): Event-Driven Architecture
- Event Sourcing: Complete event history
- ThreadLocal Pattern: Context propagation
- Async Listener Pattern: Non-blocking processing
- Repository Pattern: Data access abstraction

---

**Document Version:** 1.0
**Last Updated:** 2025-11-16
**Author:** Wei Orchestrator Team
**Status:** Design Approved
