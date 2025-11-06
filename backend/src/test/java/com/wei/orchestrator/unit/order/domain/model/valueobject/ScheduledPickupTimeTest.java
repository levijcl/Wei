package com.wei.orchestrator.unit.order.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import com.wei.orchestrator.order.domain.model.valueobject.ScheduledPickupTime;
import java.time.Duration;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class ScheduledPickupTimeTest {

    @Test
    void shouldCreateScheduledPickupTimeWithValidDateTime() {
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);

        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);

        assertNotNull(scheduledPickupTime);
        assertEquals(pickupTime, scheduledPickupTime.getPickupTime());
    }

    @Test
    void shouldThrowExceptionWhenPickupTimeIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new ScheduledPickupTime(null);
                        });

        assertTrue(exception.getMessage().contains("Pickup time cannot be null"));
    }

    @Test
    void shouldReturnTrueWhenPickupTimeIsInFuture() {
        LocalDateTime currentTime = LocalDateTime.of(2025, 11, 6, 10, 0);
        LocalDateTime futureTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(futureTime);

        boolean result = scheduledPickupTime.isInFuture(currentTime);

        assertTrue(result);
    }

    @Test
    void shouldReturnFalseWhenPickupTimeIsInPast() {
        LocalDateTime currentTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        LocalDateTime pastTime = LocalDateTime.of(2025, 11, 6, 10, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pastTime);

        boolean result = scheduledPickupTime.isInFuture(currentTime);

        assertFalse(result);
    }

    @Test
    void shouldReturnFalseWhenPickupTimeEqualsCurrentTime() {
        LocalDateTime currentTime = LocalDateTime.of(2025, 11, 6, 12, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(currentTime);

        boolean result = scheduledPickupTime.isInFuture(currentTime);

        assertFalse(result);
    }

    @Test
    void shouldCalculateFulfillmentStartTimeCorrectly() {
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = new FulfillmentLeadTime(Duration.ofHours(2));

        LocalDateTime fulfillmentStartTime =
                scheduledPickupTime.calculateFulfillmentStartTime(leadTime);

        assertEquals(LocalDateTime.of(2025, 11, 6, 12, 0), fulfillmentStartTime);
    }

    @Test
    void shouldCalculateFulfillmentStartTimeWithMinutes() {
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 30);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);
        FulfillmentLeadTime leadTime = new FulfillmentLeadTime(Duration.ofMinutes(90));

        LocalDateTime fulfillmentStartTime =
                scheduledPickupTime.calculateFulfillmentStartTime(leadTime);

        assertEquals(LocalDateTime.of(2025, 11, 6, 13, 0), fulfillmentStartTime);
    }

    @Test
    void shouldThrowExceptionWhenLeadTimeIsNull() {
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            scheduledPickupTime.calculateFulfillmentStartTime(null);
                        });

        assertTrue(exception.getMessage().contains("Lead time cannot be null"));
    }

    @Test
    void shouldBeEqualWhenPickupTimesAreSame() {
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime1 = new ScheduledPickupTime(pickupTime);
        ScheduledPickupTime scheduledPickupTime2 = new ScheduledPickupTime(pickupTime);

        assertEquals(scheduledPickupTime1, scheduledPickupTime2);
        assertEquals(scheduledPickupTime1.hashCode(), scheduledPickupTime2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenPickupTimesAreDifferent() {
        LocalDateTime pickupTime1 = LocalDateTime.of(2025, 11, 6, 14, 0);
        LocalDateTime pickupTime2 = LocalDateTime.of(2025, 11, 6, 15, 0);
        ScheduledPickupTime scheduledPickupTime1 = new ScheduledPickupTime(pickupTime1);
        ScheduledPickupTime scheduledPickupTime2 = new ScheduledPickupTime(pickupTime2);

        assertNotEquals(scheduledPickupTime1, scheduledPickupTime2);
    }

    @Test
    void shouldReturnCorrectStringRepresentation() {
        LocalDateTime pickupTime = LocalDateTime.of(2025, 11, 6, 14, 0);
        ScheduledPickupTime scheduledPickupTime = new ScheduledPickupTime(pickupTime);

        String result = scheduledPickupTime.toString();

        assertTrue(result.contains("ScheduledPickupTime"));
        assertTrue(result.contains("2025-11-06T14:00"));
    }
}
