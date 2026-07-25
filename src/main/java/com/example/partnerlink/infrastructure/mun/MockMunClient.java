package com.example.partnerlink.infrastructure.mun;

import com.example.partnerlink.domain.ApplicationStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * In-process MUN mock. Configurable delay/result for demos and tests.
 * Uses an injected executor (not {@code @Async} on this bean) so tests can
 * inject the concrete type and read {@link #getCallCount()} without hitting
 * a JDK interface proxy.
 */
@Component
@Primary
public class MockMunClient implements MunClient {

    private static final Logger log = LoggerFactory.getLogger(MockMunClient.class);

    private final long delayMs;
    private final ApplicationStatus defaultResult;
    private final Executor executor;
    private final AtomicInteger callCount = new AtomicInteger();

    public MockMunClient(
            @Value("${partnerlink.mun.delay-ms:200}") long delayMs,
            @Value("${partnerlink.mun.default-result:APPROVED}") String defaultResult,
            @Qualifier("taskExecutor") Executor executor) {
        this.delayMs = delayMs;
        this.defaultResult = ApplicationStatus.valueOf(defaultResult);
        this.executor = executor;
    }

    @Override
    public CompletableFuture<ApplicationStatus> screenAsync(String applicationId) {
        int n = callCount.incrementAndGet();
        log.info("MUN screen started applicationId={} call#={}", applicationId, n);
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("MUN screening interrupted", e);
            }
            log.info("MUN screen finished applicationId={} result={}", applicationId, defaultResult);
            return defaultResult;
        }, executor);
    }

    public int getCallCount() {
        return callCount.get();
    }

    public void resetCallCount() {
        callCount.set(0);
    }
}
