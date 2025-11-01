package com.wei.orchestrator.integration.observation.infrastructure.scheduler;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.application.OrderObserverApplicationService;
import com.wei.orchestrator.observation.infrastructure.scheduler.ObserverScheduler;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class ObserverSchedulerIntegrationTest {

    @Autowired private ObserverScheduler observerScheduler;

    @Autowired private LockRegistry lockRegistry;

    @Autowired private DataSource dataSource;

    @MockitoBean private OrderObserverApplicationService orderObserverApplicationService;

    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate = new JdbcTemplate(dataSource);
        clearLockTable();
        reset(orderObserverApplicationService);
    }

    private void clearLockTable() {
        jdbcTemplate.execute("DELETE FROM int_lock");
    }

    private int countLocksInDatabase() {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM int_lock", Integer.class);
        return count != null ? count : 0;
    }

    @Test
    void shouldExecuteScheduledPollingWithRealLocks() {
        observerScheduler.pollAllObserverTypes();

        verify(orderObserverApplicationService, times(1)).pollAllActiveObservers();
    }

    @Test
    void shouldReleaseLockAfterPollingCompletes() {
        observerScheduler.pollAllObserverTypes();

        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldReleaseLockEvenWhenPollingThrowsException() {
        doThrow(new RuntimeException("Simulated polling failure"))
                .when(orderObserverApplicationService)
                .pollAllActiveObservers();

        observerScheduler.pollAllObserverTypes();

        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldAllowMultipleSequentialPollingCycles() {
        observerScheduler.pollAllObserverTypes();
        observerScheduler.pollAllObserverTypes();
        observerScheduler.pollAllObserverTypes();

        verify(orderObserverApplicationService, times(3)).pollAllActiveObservers();
        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldAllowSecondPollingAfterFirstCompletes() throws InterruptedException {
        AtomicInteger executionCount = new AtomicInteger(0);
        CountDownLatch firstDone = new CountDownLatch(1);
        CountDownLatch secondDone = new CountDownLatch(1);

        doAnswer(
                        invocation -> {
                            executionCount.incrementAndGet();
                            return null;
                        })
                .when(orderObserverApplicationService)
                .pollAllActiveObservers();

        Thread thread1 =
                new Thread(
                        () -> {
                            observerScheduler.pollAllObserverTypes();
                            firstDone.countDown();
                        });

        Thread thread2 =
                new Thread(
                        () -> {
                            try {
                                firstDone.await();
                                Thread.sleep(50);
                                observerScheduler.pollAllObserverTypes();
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            } finally {
                                secondDone.countDown();
                            }
                        });

        thread1.start();
        thread2.start();

        assertTrue(secondDone.await(5, TimeUnit.SECONDS));

        assertEquals(2, executionCount.get());
        verify(orderObserverApplicationService, times(2)).pollAllActiveObservers();
        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldCleanUpAllLocksAfterPollingCycles() throws InterruptedException {
        doAnswer(
                        invocation -> {
                            Thread.sleep(50);
                            return null;
                        })
                .when(orderObserverApplicationService)
                .pollAllActiveObservers();

        observerScheduler.pollAllObserverTypes();
        Thread.sleep(100);

        assertEquals(0, countLocksInDatabase());
    }

    @Test
    void shouldObtainLockFromRegistry() throws InterruptedException {
        var lock = lockRegistry.obtain("test-lock");
        assertNotNull(lock);

        boolean acquired = lock.tryLock(1, TimeUnit.SECONDS);
        assertTrue(acquired);

        lock.unlock();
    }

    @Test
    void shouldReturnSameLockInstanceForSameKey() {
        var lock1 = lockRegistry.obtain("same-key");
        var lock2 = lockRegistry.obtain("same-key");

        assertSame(lock1, lock2);
    }

    @Test
    void shouldAllowDifferentLockKeysConcurrently() throws InterruptedException {
        var lock1 = lockRegistry.obtain("key1");
        var lock2 = lockRegistry.obtain("key2");

        boolean acquired1 = lock1.tryLock(1, TimeUnit.SECONDS);
        boolean acquired2 = lock2.tryLock(1, TimeUnit.SECONDS);

        assertTrue(acquired1);
        assertTrue(acquired2);

        lock1.unlock();
        lock2.unlock();
    }
}
