package com.wei.orchestrator.unit.observation.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import org.junit.jupiter.api.Test;

class PollingIntervalTest {

    @Test
    void shouldCreatePollingIntervalWithValidSeconds() {
        PollingInterval pollingInterval = new PollingInterval(60);

        assertNotNull(pollingInterval);
        assertEquals(60, pollingInterval.getSeconds());
        assertEquals(60000L, pollingInterval.getMilliseconds());
    }

    @Test
    void shouldThrowExceptionWhenSecondsIsZero() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new PollingInterval(0);
                        });

        assertTrue(exception.getMessage().contains("Polling interval must be positive"));
    }

    @Test
    void shouldThrowExceptionWhenSecondsIsNegative() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new PollingInterval(-10);
                        });

        assertTrue(exception.getMessage().contains("Polling interval must be positive"));
    }

    @Test
    void shouldThrowExceptionWhenSecondsIsLessThanMinimum() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new PollingInterval(5);
                        });

        assertTrue(exception.getMessage().contains("Polling interval must be at least 10 seconds"));
    }

    @Test
    void shouldAcceptMinimumAllowedValue() {
        PollingInterval pollingInterval = new PollingInterval(10);

        assertEquals(10, pollingInterval.getSeconds());
        assertEquals(10000L, pollingInterval.getMilliseconds());
    }

    @Test
    void shouldConvertSecondsToMilliseconds() {
        PollingInterval pollingInterval = new PollingInterval(120);

        assertEquals(120000L, pollingInterval.getMilliseconds());
    }

    @Test
    void shouldBeEqualWhenSecondsAreSame() {
        PollingInterval interval1 = new PollingInterval(60);
        PollingInterval interval2 = new PollingInterval(60);

        assertEquals(interval1, interval2);
        assertEquals(interval1.hashCode(), interval2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenSecondsAreDifferent() {
        PollingInterval interval1 = new PollingInterval(60);
        PollingInterval interval2 = new PollingInterval(120);

        assertNotEquals(interval1, interval2);
    }
}
