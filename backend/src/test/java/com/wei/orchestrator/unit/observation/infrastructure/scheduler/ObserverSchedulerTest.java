package com.wei.orchestrator.unit.observation.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.application.OrderObserverApplicationService;
import com.wei.orchestrator.observation.infrastructure.scheduler.ObserverScheduler;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.support.locks.LockRegistry;

@ExtendWith(MockitoExtension.class)
class ObserverSchedulerTest {

    @Mock private LockRegistry lockRegistry;

    @Mock private OrderObserverApplicationService orderObserverService;

    @Mock private Lock lock;

    @InjectMocks private ObserverScheduler observerScheduler;

    @Nested
    class pollAllObserverTypes {

        @Test
        void shouldAcquireLockAndPollSuccessfully() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lock).tryLock(1, TimeUnit.SECONDS);
            verify(orderObserverService).pollAllActiveObservers();
            verify(lock).unlock();
        }

        @Test
        void shouldSkipPollingWhenLockNotAcquired() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(false);

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lock).tryLock(1, TimeUnit.SECONDS);
            verify(orderObserverService, never()).pollAllActiveObservers();
            verify(lock, never()).unlock();
        }

        @Test
        void shouldReleaseLockEvenWhenPollingFails() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new RuntimeException("Polling failed"))
                    .when(orderObserverService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lock).tryLock(1, TimeUnit.SECONDS);
            verify(orderObserverService).pollAllActiveObservers();
            verify(lock).unlock();
        }

        @Test
        void shouldHandleInterruptedExceptionDuringLockAcquisition() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lock).tryLock(1, TimeUnit.SECONDS);
            verify(orderObserverService, never()).pollAllActiveObservers();
            verify(lock, never()).unlock();
        }

        @Test
        void shouldHandleExceptionDuringLockRelease() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new RuntimeException("Unlock failed")).when(lock).unlock();

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lock).tryLock(1, TimeUnit.SECONDS);
            verify(orderObserverService).pollAllActiveObservers();
            verify(lock).unlock();
        }

        @Test
        void shouldUseTryLockWithCorrectTimeout() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lock).tryLock(1, TimeUnit.SECONDS);
        }

        @Test
        void shouldHandleNullPointerExceptionDuringPolling() throws InterruptedException {
            when(lockRegistry.obtain("order-observer-poll")).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new NullPointerException("Unexpected null"))
                    .when(orderObserverService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(lock).unlock();
        }

        @Test
        void shouldReleaseMultipleLocksIndependently() throws InterruptedException {
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lock, atLeastOnce()).unlock();
        }
    }
}
