package com.wei.orchestrator.unit.order.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.order.domain.model.valueobject.FulfillmentLeadTime;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class FulfillmentLeadTimeTest {

    @Test
    void shouldCreateFulfillmentLeadTimeWithValidDuration() {
        Duration duration = Duration.ofHours(2);

        FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(duration);

        assertNotNull(fulfillmentLeadTime);
        assertEquals(duration, fulfillmentLeadTime.getDuration());
        assertEquals(120, fulfillmentLeadTime.getMinutes());
        assertEquals(2, fulfillmentLeadTime.getHours());
    }

    @Test
    void shouldThrowExceptionWhenDurationIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new FulfillmentLeadTime(null);
                        });

        assertTrue(exception.getMessage().contains("Duration cannot be null"));
    }

    @Test
    void shouldThrowExceptionWhenDurationIsNegative() {
        Duration negativeDuration = Duration.ofHours(-1);

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new FulfillmentLeadTime(negativeDuration);
                        });

        assertTrue(exception.getMessage().contains("Duration cannot be negative"));
    }

    @Test
    void shouldAllowZeroDuration() {
        Duration zeroDuration = Duration.ZERO;

        FulfillmentLeadTime fulfillmentLeadTime = new FulfillmentLeadTime(zeroDuration);

        assertNotNull(fulfillmentLeadTime);
        assertEquals(0, fulfillmentLeadTime.getMinutes());
        assertEquals(0, fulfillmentLeadTime.getHours());
    }

    @Test
    void shouldCreateDefaultLeadTime() {
        FulfillmentLeadTime defaultLeadTime = FulfillmentLeadTime.defaultLeadTime();

        assertNotNull(defaultLeadTime);
        assertEquals(120, defaultLeadTime.getMinutes());
        assertEquals(2, defaultLeadTime.getHours());
    }

    @Test
    void shouldCreateLeadTimeFromHours() {
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(3);

        assertEquals(180, leadTime.getMinutes());
        assertEquals(3, leadTime.getHours());
    }

    @Test
    void shouldCreateLeadTimeFromMinutes() {
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofMinutes(90);

        assertEquals(90, leadTime.getMinutes());
        assertEquals(1, leadTime.getHours());
    }

    @Test
    void shouldThrowExceptionWhenOfHoursIsNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            FulfillmentLeadTime.ofHours(-1);
                        });

        assertTrue(exception.getMessage().contains("Duration cannot be negative"));
    }

    @Test
    void shouldThrowExceptionWhenOfMinutesIsNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            FulfillmentLeadTime.ofMinutes(-30);
                        });

        assertTrue(exception.getMessage().contains("Duration cannot be negative"));
    }

    @Test
    void shouldConvertMinutesCorrectlyForNonWholeHours() {
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofMinutes(150);

        assertEquals(150, leadTime.getMinutes());
        assertEquals(2, leadTime.getHours());
    }

    @Test
    void shouldBeEqualWhenDurationsAreSame() {
        FulfillmentLeadTime leadTime1 = FulfillmentLeadTime.ofHours(2);
        FulfillmentLeadTime leadTime2 = new FulfillmentLeadTime(Duration.ofMinutes(120));

        assertEquals(leadTime1, leadTime2);
        assertEquals(leadTime1.hashCode(), leadTime2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenDurationsAreDifferent() {
        FulfillmentLeadTime leadTime1 = FulfillmentLeadTime.ofHours(2);
        FulfillmentLeadTime leadTime2 = FulfillmentLeadTime.ofHours(3);

        assertNotEquals(leadTime1, leadTime2);
    }

    @Test
    void shouldReturnCorrectStringRepresentation() {
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(2);

        String result = leadTime.toString();

        assertTrue(result.contains("FulfillmentLeadTime"));
        assertTrue(result.contains("duration="));
    }

    @Test
    void shouldHandleLargeDurations() {
        FulfillmentLeadTime leadTime = FulfillmentLeadTime.ofHours(24);

        assertEquals(24, leadTime.getHours());
        assertEquals(1440, leadTime.getMinutes());
    }
}
