package com.wei.orchestrator.integration.observation.infrastructure.repository;

import static org.junit.jupiter.api.Assertions.*;

import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.PollingInterval;
import com.wei.orchestrator.observation.domain.model.valueobject.SourceEndpoint;
import com.wei.orchestrator.observation.infrastructure.mapper.OrderObserverMapper;
import com.wei.orchestrator.observation.infrastructure.repository.OrderObserverRepositoryImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@DataJpaTest
@Import({OrderObserverRepositoryImpl.class, OrderObserverMapper.class})
class OrderObserverRepositoryIntegrationTest {

    @Autowired private OrderObserverRepositoryImpl orderObserverRepository;

    @Test
    void shouldSaveAndFindOrderObserverById() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver observer =
                new OrderObserver("observer-repo-1", sourceEndpoint, pollingInterval);

        OrderObserver savedObserver = orderObserverRepository.save(observer);

        assertNotNull(savedObserver);
        assertEquals("observer-repo-1", savedObserver.getObserverId());
        assertTrue(savedObserver.isActive());
        assertEquals(60, savedObserver.getPollingInterval().getSeconds());
    }

    @Test
    void shouldFindOrderObserverByIdAfterSaving() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "testuser", "testpass");
        PollingInterval pollingInterval = new PollingInterval(120);

        OrderObserver observer =
                new OrderObserver("observer-repo-2", sourceEndpoint, pollingInterval);
        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver = orderObserverRepository.findById("observer-repo-2");

        assertTrue(foundObserver.isPresent());
        assertEquals("observer-repo-2", foundObserver.get().getObserverId());
        assertEquals(
                "jdbc:oracle:thin:@localhost:1521:XE",
                foundObserver.get().getSourceEndpoint().getJdbcUrl());
        assertEquals("testuser", foundObserver.get().getSourceEndpoint().getUsername());
    }

    @Test
    void shouldReturnEmptyWhenObserverNotFound() {
        Optional<OrderObserver> foundObserver = orderObserverRepository.findById("NON-EXISTENT");

        assertFalse(foundObserver.isPresent());
    }

    @Test
    void shouldSaveObserverWithLastPolledTimestamp() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver observer =
                new OrderObserver("observer-repo-3", sourceEndpoint, pollingInterval);
        LocalDateTime now = LocalDateTime.now();
        observer.setLastPolledTimestamp(now);

        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver = orderObserverRepository.findById("observer-repo-3");
        assertTrue(foundObserver.isPresent());
        assertNotNull(foundObserver.get().getLastPolledTimestamp());
    }

    @Test
    void shouldUpdateExistingObserver() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver observer =
                new OrderObserver("observer-repo-4", sourceEndpoint, pollingInterval);
        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver = orderObserverRepository.findById("observer-repo-4");
        assertTrue(foundObserver.isPresent());

        OrderObserver existingObserver = foundObserver.get();
        LocalDateTime now = LocalDateTime.now();
        existingObserver.setLastPolledTimestamp(now);

        orderObserverRepository.save(existingObserver);

        Optional<OrderObserver> updatedObserver =
                orderObserverRepository.findById("observer-repo-4");
        assertTrue(updatedObserver.isPresent());
        assertNotNull(updatedObserver.get().getLastPolledTimestamp());
    }

    @Test
    void shouldDeleteObserverById() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver observer =
                new OrderObserver("observer-repo-5", sourceEndpoint, pollingInterval);
        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver = orderObserverRepository.findById("observer-repo-5");
        assertTrue(foundObserver.isPresent());

        orderObserverRepository.deleteById("observer-repo-5");

        Optional<OrderObserver> deletedObserver =
                orderObserverRepository.findById("observer-repo-5");
        assertFalse(deletedObserver.isPresent());
    }

    @Test
    void shouldSaveActiveAndInactiveObservers() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver activeObserver =
                new OrderObserver("observer-active", sourceEndpoint, pollingInterval);
        OrderObserver inactiveObserver =
                new OrderObserver("observer-inactive", sourceEndpoint, pollingInterval);
        inactiveObserver.deactivate();

        orderObserverRepository.save(activeObserver);
        orderObserverRepository.save(inactiveObserver);

        Optional<OrderObserver> foundActive = orderObserverRepository.findById("observer-active");
        Optional<OrderObserver> foundInactive =
                orderObserverRepository.findById("observer-inactive");

        assertTrue(foundActive.isPresent());
        assertTrue(foundInactive.isPresent());
        assertTrue(foundActive.get().isActive());
        assertFalse(foundInactive.get().isActive());
    }

    @Test
    void shouldFindAllActiveObservers() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver observer1 =
                new OrderObserver("observer-all-1", sourceEndpoint, pollingInterval);
        OrderObserver observer2 =
                new OrderObserver("observer-all-2", sourceEndpoint, pollingInterval);
        OrderObserver observer3 =
                new OrderObserver("observer-all-3", sourceEndpoint, pollingInterval);
        observer3.deactivate();

        orderObserverRepository.save(observer1);
        orderObserverRepository.save(observer2);
        orderObserverRepository.save(observer3);

        List<OrderObserver> activeObservers = orderObserverRepository.findAllActive();

        assertTrue(activeObservers.size() >= 2);
        assertTrue(activeObservers.stream().allMatch(OrderObserver::isActive));
    }

    @Test
    void shouldToggleObserverActiveStatus() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(60);

        OrderObserver observer =
                new OrderObserver("observer-toggle", sourceEndpoint, pollingInterval);
        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver = orderObserverRepository.findById("observer-toggle");
        assertTrue(foundObserver.isPresent());
        assertTrue(foundObserver.get().isActive());

        OrderObserver deactivatedObserver = foundObserver.get();
        deactivatedObserver.deactivate();
        orderObserverRepository.save(deactivatedObserver);

        Optional<OrderObserver> reloadedObserver =
                orderObserverRepository.findById("observer-toggle");
        assertTrue(reloadedObserver.isPresent());
        assertFalse(reloadedObserver.get().isActive());

        OrderObserver reactivatedObserver = reloadedObserver.get();
        reactivatedObserver.activate();
        orderObserverRepository.save(reactivatedObserver);

        Optional<OrderObserver> finalObserver = orderObserverRepository.findById("observer-toggle");
        assertTrue(finalObserver.isPresent());
        assertTrue(finalObserver.get().isActive());
    }

    @Test
    void shouldPersistSourceEndpointDetails() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint(
                        "jdbc:oracle:thin:@db.example.com:1521:PROD", "produser", "prodpass");
        PollingInterval pollingInterval = new PollingInterval(90);

        OrderObserver observer =
                new OrderObserver("observer-endpoint", sourceEndpoint, pollingInterval);
        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver =
                orderObserverRepository.findById("observer-endpoint");
        assertTrue(foundObserver.isPresent());

        SourceEndpoint retrievedEndpoint = foundObserver.get().getSourceEndpoint();
        assertEquals("jdbc:oracle:thin:@db.example.com:1521:PROD", retrievedEndpoint.getJdbcUrl());
        assertEquals("produser", retrievedEndpoint.getUsername());
        assertEquals("prodpass", retrievedEndpoint.getPassword());
    }

    @Test
    void shouldPersistPollingIntervalDetails() {
        SourceEndpoint sourceEndpoint =
                new SourceEndpoint("jdbc:oracle:thin:@localhost:1521:XE", "user", "pass");
        PollingInterval pollingInterval = new PollingInterval(300);

        OrderObserver observer =
                new OrderObserver("observer-interval", sourceEndpoint, pollingInterval);
        orderObserverRepository.save(observer);

        Optional<OrderObserver> foundObserver =
                orderObserverRepository.findById("observer-interval");
        assertTrue(foundObserver.isPresent());

        PollingInterval retrievedInterval = foundObserver.get().getPollingInterval();
        assertEquals(300, retrievedInterval.getSeconds());
        assertEquals(300000L, retrievedInterval.getMilliseconds());
    }
}
