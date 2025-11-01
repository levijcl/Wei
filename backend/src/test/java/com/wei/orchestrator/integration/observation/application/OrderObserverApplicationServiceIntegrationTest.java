package com.wei.orchestrator.integration.observation.application;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.wei.orchestrator.observation.application.OrderObserverApplicationService;
import com.wei.orchestrator.observation.application.command.CreateOrderObserverCommand;
import com.wei.orchestrator.observation.application.command.PollOrderSourceCommand;
import com.wei.orchestrator.observation.domain.model.OrderObserver;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservationResult;
import com.wei.orchestrator.observation.domain.model.valueobject.ObservedOrderItem;
import com.wei.orchestrator.observation.domain.port.OrderSourcePort;
import com.wei.orchestrator.observation.domain.repository.OrderObserverRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class OrderObserverApplicationServiceIntegrationTest {

    @Autowired private OrderObserverApplicationService orderObserverApplicationService;

    @Autowired private OrderObserverRepository orderObserverRepository;

    @MockitoBean private OrderSourcePort orderSourcePort;

    @Nested
    class createOrderObserver {

        @Test
        void shouldCreateOrderObserverAndPersistToDatabase() {
            CreateOrderObserverCommand command =
                    new CreateOrderObserverCommand(
                            "observer-integration-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "testuser",
                            "testpass",
                            60);

            String observerId = orderObserverApplicationService.createOrderObserver(command);

            assertNotNull(observerId);
            assertEquals("observer-integration-1", observerId);

            Optional<OrderObserver> foundObserver = orderObserverRepository.findById(observerId);
            assertTrue(foundObserver.isPresent());
            assertEquals("observer-integration-1", foundObserver.get().getObserverId());
            assertTrue(foundObserver.get().isActive());
            assertEquals(60, foundObserver.get().getPollingInterval().getSeconds());
        }

        @Test
        void shouldPersistSourceEndpointCorrectly() {
            CreateOrderObserverCommand command =
                    new CreateOrderObserverCommand(
                            "observer-integration-2",
                            "jdbc:oracle:thin:@db.example.com:1521:PROD",
                            "produser",
                            "prodpass",
                            120);

            orderObserverApplicationService.createOrderObserver(command);

            Optional<OrderObserver> persistedObserver =
                    orderObserverRepository.findById("observer-integration-2");
            assertTrue(persistedObserver.isPresent());

            OrderObserver observer = persistedObserver.get();
            assertEquals(
                    "jdbc:oracle:thin:@db.example.com:1521:PROD",
                    observer.getSourceEndpoint().getJdbcUrl());
            assertEquals("produser", observer.getSourceEndpoint().getUsername());
            assertEquals("prodpass", observer.getSourceEndpoint().getPassword());
        }

        @Test
        void shouldPersistPollingIntervalCorrectly() {
            CreateOrderObserverCommand command =
                    new CreateOrderObserverCommand(
                            "observer-integration-3",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user",
                            "pass",
                            300);

            orderObserverApplicationService.createOrderObserver(command);

            Optional<OrderObserver> persistedObserver =
                    orderObserverRepository.findById("observer-integration-3");
            assertTrue(persistedObserver.isPresent());
            assertEquals(300, persistedObserver.get().getPollingInterval().getSeconds());
            assertEquals(300000L, persistedObserver.get().getPollingInterval().getMilliseconds());
        }

        @Test
        void shouldCreateMultipleObserversIndependently() {
            CreateOrderObserverCommand command1 =
                    new CreateOrderObserverCommand(
                            "observer-multi-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user1",
                            "pass1",
                            60);

            CreateOrderObserverCommand command2 =
                    new CreateOrderObserverCommand(
                            "observer-multi-2",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user2",
                            "pass2",
                            90);

            String observer1 = orderObserverApplicationService.createOrderObserver(command1);
            String observer2 = orderObserverApplicationService.createOrderObserver(command2);

            assertNotNull(observer1);
            assertNotNull(observer2);
            assertNotEquals(observer1, observer2);

            Optional<OrderObserver> persistedObserver1 =
                    orderObserverRepository.findById("observer-multi-1");
            Optional<OrderObserver> persistedObserver2 =
                    orderObserverRepository.findById("observer-multi-2");

            assertTrue(persistedObserver1.isPresent());
            assertTrue(persistedObserver2.isPresent());
        }
    }

    @Nested
    class pollOrderSource {

        @Test
        void shouldPollAndUpdateLastPolledTimestamp() {
            CreateOrderObserverCommand createCommand =
                    new CreateOrderObserverCommand(
                            "observer-poll-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user",
                            "pass",
                            60);
            orderObserverApplicationService.createOrderObserver(createCommand);

            List<ObservationResult> mockResults = createMockObservationResults(2);
            when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);

            PollOrderSourceCommand pollCommand = new PollOrderSourceCommand("observer-poll-1");
            orderObserverApplicationService.pollOrderSource(pollCommand);

            Optional<OrderObserver> updatedObserver =
                    orderObserverRepository.findById("observer-poll-1");
            assertTrue(updatedObserver.isPresent());
            assertNotNull(updatedObserver.get().getLastPolledTimestamp());
        }

        @Test
        void shouldHandleMultiplePolls() {
            CreateOrderObserverCommand createCommand =
                    new CreateOrderObserverCommand(
                            "observer-poll-2",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user",
                            "pass",
                            10);
            orderObserverApplicationService.createOrderObserver(createCommand);

            List<ObservationResult> mockResults1 = createMockObservationResults(1);
            when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults1);

            PollOrderSourceCommand pollCommand = new PollOrderSourceCommand("observer-poll-2");
            orderObserverApplicationService.pollOrderSource(pollCommand);

            Optional<OrderObserver> afterFirstPoll =
                    orderObserverRepository.findById("observer-poll-2");
            assertTrue(afterFirstPoll.isPresent());
            LocalDateTime firstPollTime = afterFirstPoll.get().getLastPolledTimestamp();
            assertNotNull(firstPollTime);

            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            List<ObservationResult> mockResults2 = createMockObservationResults(2);
            when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults2);

            orderObserverApplicationService.pollOrderSource(pollCommand);

            Optional<OrderObserver> afterSecondPoll =
                    orderObserverRepository.findById("observer-poll-2");
            assertTrue(afterSecondPoll.isPresent());
            LocalDateTime secondPollTime = afterSecondPoll.get().getLastPolledTimestamp();
            assertNotNull(secondPollTime);
            assertTrue(
                    secondPollTime.isAfter(firstPollTime) || secondPollTime.isEqual(firstPollTime));
        }
    }

    @Nested
    class activateObserver {

        @Test
        void shouldActivateInactiveObserver() {
            CreateOrderObserverCommand createCommand =
                    new CreateOrderObserverCommand(
                            "observer-activate-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user",
                            "pass",
                            60);
            orderObserverApplicationService.createOrderObserver(createCommand);

            orderObserverApplicationService.deactivateObserver("observer-activate-1");

            Optional<OrderObserver> deactivatedObserver =
                    orderObserverRepository.findById("observer-activate-1");
            assertTrue(deactivatedObserver.isPresent());
            assertFalse(deactivatedObserver.get().isActive());

            orderObserverApplicationService.activateObserver("observer-activate-1");

            Optional<OrderObserver> activatedObserver =
                    orderObserverRepository.findById("observer-activate-1");
            assertTrue(activatedObserver.isPresent());
            assertTrue(activatedObserver.get().isActive());
        }
    }

    @Nested
    class deactivateObserver {

        @Test
        void shouldDeactivateActiveObserver() {
            CreateOrderObserverCommand createCommand =
                    new CreateOrderObserverCommand(
                            "observer-deactivate-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user",
                            "pass",
                            60);
            orderObserverApplicationService.createOrderObserver(createCommand);

            Optional<OrderObserver> activeObserver =
                    orderObserverRepository.findById("observer-deactivate-1");
            assertTrue(activeObserver.isPresent());
            assertTrue(activeObserver.get().isActive());

            orderObserverApplicationService.deactivateObserver("observer-deactivate-1");

            Optional<OrderObserver> deactivatedObserver =
                    orderObserverRepository.findById("observer-deactivate-1");
            assertTrue(deactivatedObserver.isPresent());
            assertFalse(deactivatedObserver.get().isActive());
        }
    }

    @Nested
    class pollAllActiveObservers {

        @Test
        void shouldPollOnlyActiveObservers() {
            CreateOrderObserverCommand command1 =
                    new CreateOrderObserverCommand(
                            "observer-all-1",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user1",
                            "pass1",
                            60);
            CreateOrderObserverCommand command2 =
                    new CreateOrderObserverCommand(
                            "observer-all-2",
                            "jdbc:oracle:thin:@localhost:1521:XE",
                            "user2",
                            "pass2",
                            60);

            orderObserverApplicationService.createOrderObserver(command1);
            orderObserverApplicationService.createOrderObserver(command2);

            orderObserverApplicationService.deactivateObserver("observer-all-2");

            List<ObservationResult> mockResults = createMockObservationResults(1);
            when(orderSourcePort.fetchNewOrders(any(), any())).thenReturn(mockResults);

            orderObserverApplicationService.pollAllActiveObservers();

            Optional<OrderObserver> observer1 = orderObserverRepository.findById("observer-all-1");
            Optional<OrderObserver> observer2 = orderObserverRepository.findById("observer-all-2");

            assertTrue(observer1.isPresent());
            assertTrue(observer2.isPresent());
            assertNotNull(observer1.get().getLastPolledTimestamp());
            assertNull(observer2.get().getLastPolledTimestamp());
        }
    }

    private List<ObservationResult> createMockObservationResults(int count) {
        List<ObservationResult> results = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            List<ObservedOrderItem> items = new ArrayList<>();
            items.add(
                    new ObservedOrderItem(
                            "SKU-" + i, "Product " + i, 1, BigDecimal.valueOf(100.0)));

            ObservationResult result =
                    new ObservationResult(
                            "order-" + i,
                            "Customer " + i,
                            "customer" + i + "@example.com",
                            "Address " + i,
                            "TYPE_A",
                            "WH001",
                            "NEW",
                            items,
                            LocalDateTime.now());
            results.add(result);
        }
        return results;
    }
}
