package com.wei.orchestrator.unit.observation.infrastructure.scheduler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.wei.orchestrator.observation.application.InventoryObserverApplicationService;
import com.wei.orchestrator.observation.application.OrderObserverApplicationService;
import com.wei.orchestrator.observation.application.WesObserverApplicationService;
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

    @Mock private WesObserverApplicationService wesObserverApplicationService;

    @Mock private InventoryObserverApplicationService inventoryObserverApplicationService;

    @Mock private Lock lock;

    @InjectMocks private ObserverScheduler observerScheduler;

    @Nested
    class pollAllObserverTypes {

        @Test
        void shouldPollBothOrderAndWesObservers() throws InterruptedException {
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lockRegistry).obtain("wes-observer-poll");
            verify(orderObserverService).pollAllActiveObservers();
            verify(wesObserverApplicationService).pollAllActiveObservers();
            verify(lock, times(3)).unlock();
        }

        @Test
        void shouldAcquireLocksForBothObserverTypes() throws InterruptedException {
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lockRegistry).obtain("order-observer-poll");
            verify(lockRegistry).obtain("wes-observer-poll");
            verify(lock, times(3)).tryLock(1, TimeUnit.SECONDS);
        }

        @Test
        void shouldSkipOrderObserverWhenLockNotAcquired() throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(false);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService, never()).pollAllActiveObservers();
            verify(wesObserverApplicationService).pollAllActiveObservers();
            verify(orderLock, never()).unlock();
            verify(wesLock).unlock();
        }

        @Test
        void shouldSkipWesObserverWhenLockNotAcquired() throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(false);

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService).pollAllActiveObservers();
            verify(wesObserverApplicationService, never()).pollAllActiveObservers();
            verify(orderLock).unlock();
            verify(wesLock, never()).unlock();
        }

        @Test
        void shouldReleaseLockEvenWhenOrderObserverPollingFails() throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new RuntimeException("Polling failed"))
                    .when(orderObserverService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(orderLock).unlock();
            verify(wesObserverApplicationService).pollAllActiveObservers();
            verify(wesLock).unlock();
        }

        @Test
        void shouldReleaseLockEvenWhenWesObserverPollingFails() throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new RuntimeException("Polling failed"))
                    .when(wesObserverApplicationService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService).pollAllActiveObservers();
            verify(orderLock).unlock();
            verify(wesLock).unlock();
        }

        @Test
        void shouldContinueWesPollingWhenOrderObserverFails() throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new RuntimeException("Order observer failed"))
                    .when(orderObserverService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(wesObserverApplicationService).pollAllActiveObservers();
        }

        @Test
        void shouldHandleInterruptedExceptionDuringOrderObserverLockAcquisition()
                throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenThrow(new InterruptedException());
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService, never()).pollAllActiveObservers();
            verify(wesObserverApplicationService).pollAllActiveObservers();
            verify(orderLock, never()).unlock();
            verify(wesLock).unlock();
        }

        @Test
        void shouldHandleInterruptedExceptionDuringWesObserverLockAcquisition()
                throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenThrow(new InterruptedException());

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService).pollAllActiveObservers();
            verify(wesObserverApplicationService, never()).pollAllActiveObservers();
            verify(orderLock).unlock();
            verify(wesLock, never()).unlock();
        }

        @Test
        void shouldHandleExceptionDuringLockRelease() throws InterruptedException {
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new RuntimeException("Unlock failed")).when(lock).unlock();

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService).pollAllActiveObservers();
            verify(wesObserverApplicationService).pollAllActiveObservers();
            verify(lock, times(3)).unlock();
        }

        @Test
        void shouldUseTryLockWithCorrectTimeout() throws InterruptedException {
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(lock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lock, times(3)).tryLock(1, TimeUnit.SECONDS);
        }

        @Test
        void shouldHandleNullPointerExceptionDuringOrderObserverPolling()
                throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new NullPointerException("Unexpected null"))
                    .when(orderObserverService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(orderLock).unlock();
            verify(wesObserverApplicationService).pollAllActiveObservers();
            verify(wesLock).unlock();
        }

        @Test
        void shouldHandleNullPointerExceptionDuringWesObserverPolling()
                throws InterruptedException {
            Lock orderLock = mock(Lock.class);
            Lock wesLock = mock(Lock.class);

            when(lockRegistry.obtain("order-observer-poll")).thenReturn(orderLock);
            when(lockRegistry.obtain("wes-observer-poll")).thenReturn(wesLock);
            when(orderLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            when(wesLock.tryLock(1, TimeUnit.SECONDS)).thenReturn(true);
            doThrow(new NullPointerException("Unexpected null"))
                    .when(wesObserverApplicationService)
                    .pollAllActiveObservers();

            observerScheduler.pollAllObserverTypes();

            verify(orderObserverService).pollAllActiveObservers();
            verify(orderLock).unlock();
            verify(wesLock).unlock();
        }

        @Test
        void shouldReleaseAllLocksIndependently() throws InterruptedException {
            when(lockRegistry.obtain(anyString())).thenReturn(lock);
            when(lock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);

            observerScheduler.pollAllObserverTypes();

            verify(lock, times(3)).unlock();
        }
    }
}
