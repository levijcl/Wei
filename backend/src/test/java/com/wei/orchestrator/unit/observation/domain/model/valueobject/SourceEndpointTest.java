package com.wei.orchestrator.unit.observation.domain.model.valueobject;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import org.junit.jupiter.api.Test;

class SourceEndpointTest {

    @Test
    void shouldCreateSourceEndpointWithValidParameters() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "testuser", "testpass");

        assertNotNull(sourceEndpoint);
        assertEquals("jdbc:oracle:thin:@localhost:1521:XE", sourceEndpoint.getJdbcUrl());
        assertEquals("testuser", sourceEndpoint.getUsername());
        assertEquals("testpass", sourceEndpoint.getPassword());
    }

    @Test
    void shouldThrowExceptionWhenJdbcUrlIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new SourceEndpoint(null, "user", "pass");
                        });

        assertTrue(exception.getMessage().contains("JDBC URL cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenJdbcUrlIsEmpty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new SourceEndpoint("", "user", "pass");
                        });

        assertTrue(exception.getMessage().contains("JDBC URL cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", null, "pass");
                        });

        assertTrue(exception.getMessage().contains("Username cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenUsernameIsEmpty() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "", "pass");
                        });

        assertTrue(exception.getMessage().contains("Username cannot be null or empty"));
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsNull() {
        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> {
                            new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", null);
                        });

        assertTrue(exception.getMessage().contains("Password cannot be null"));
    }

    @Test
    void shouldBeEqualWhenJdbcUrlAndUsernameAreSame() {
        SourceEndpoint endpoint1 =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass1");
        SourceEndpoint endpoint2 =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass2");

        assertEquals(endpoint1, endpoint2);
        assertEquals(endpoint1.hashCode(), endpoint2.hashCode());
    }

    @Test
    void shouldNotBeEqualWhenJdbcUrlIsDifferent() {
        SourceEndpoint endpoint1 =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        SourceEndpoint endpoint2 =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1522:XE", "user", "pass");

        assertNotEquals(endpoint1, endpoint2);
    }

    @Test
    void shouldNotBeEqualWhenUsernameIsDifferent() {
        SourceEndpoint endpoint1 =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user1", "pass");
        SourceEndpoint endpoint2 =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user2", "pass");

        assertNotEquals(endpoint1, endpoint2);
    }
}
