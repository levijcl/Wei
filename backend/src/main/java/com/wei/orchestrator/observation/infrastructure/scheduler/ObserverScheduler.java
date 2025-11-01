package com.wei.orchestrator.observation.infrastructure.scheduler;

import com.wei.orchestrator.observation.application.OrderObserverApplicationService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ObserverScheduler {

    private static final Logger logger = LoggerFactory.getLogger(ObserverScheduler.class);

    private final LockRegistry lockRegistry;
    private final OrderObserverApplicationService orderObserverService;

    public ObserverScheduler(
            LockRegistry lockRegistry, OrderObserverApplicationService orderObserverService) {
        this.lockRegistry = lockRegistry;
        this.orderObserverService = orderObserverService;
    }

    @Scheduled(fixedDelayString = "${scheduler.observer.fixed-delay:30000}")
    public void pollAllObserverTypes() {
        logger.info("Starting scheduled polling cycle for all observer types");

        pollObserverType("order-observer-poll", orderObserverService::pollAllActiveObservers);

        logger.info("Polling cycle completed for all observer types");
    }

    private void pollObserverType(String lockKey, Runnable pollingAction) {
        Lock lock = lockRegistry.obtain(lockKey);
        boolean lockAcquired = false;

        try {
            lockAcquired = lock.tryLock(1, TimeUnit.SECONDS);

            if (lockAcquired) {
                logger.info("Lock acquired for: {}", lockKey);
                pollingAction.run();
                logger.info("Completed polling for: {}", lockKey);
            } else {
                logger.debug("Lock not acquired for: {} (another node is polling)", lockKey);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warn("Interrupted while waiting for lock: {}", lockKey, e);
        } catch (Exception e) {
            logger.error("Error during polling for: {}", lockKey, e);
        } finally {
            if (lockAcquired) {
                try {
                    lock.unlock();
                    logger.debug("Lock released for: {}", lockKey);
                } catch (Exception e) {
                    logger.error("Error releasing lock for: {}", lockKey, e);
                }
            }
        }
    }
}
