package com.suresh.sms.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;

/**
 * Circuit breaker + retry instances for the two external calls in this app
 * that are worth protecting against a flaky dependency (Drop G4): RabbitMQ
 * publishing ({@code EventPublisher}) and object-storage upload ({@code
 * StorageService#upload}). Nothing else gets this treatment - see each
 * bean's own comment below for why these two specifically, and {@code
 * CacheConfig} for how Redis is handled instead (no retry/circuit breaker
 * at all; a cache read/write that fails just quietly misses, there is
 * nothing worth retrying).
 *
 * <p>Built through the {@link CircuitBreakerRegistry}/{@link RetryRegistry}
 * beans {@code resilience4j-spring-boot3} auto-configures, not the plain
 * {@code CircuitBreaker.of(...)}/{@code Retry.of(...)} factory methods:
 * registering each instance in its registry is what makes it visible to
 * that starter's own Micrometer bindings, so {@code eventPublisher} and
 * {@code objectStorageUpload} show up on {@code GET /actuator/prometheus}
 * (state, call counts, ...) the same way every other metric in this app
 * already does, without any extra wiring here.
 *
 * <p>Each pair is still a plain {@code @Bean}, not configured to be built
 * purely by name from {@code application.properties}: {@code
 * EventPublisher}/{@code StorageService} {@code @Autowired}/{@code
 * @Qualifier} these exact beans, so a typo in a property key fails loudly
 * at compile time instead of silently handing out resilience4j's defaults
 * under a name nothing actually requested.
 */
@Configuration
public class ResilienceConfig {

    // RABBITMQ PUBLISH (EventPublisher)
    //
    // Publishing a domain event is already a best-effort, swallowed side
    // effect (see EventPublisher's own Javadoc) - retry/circuit breaker
    // don't change that contract, they make the best effort smarter: a
    // couple of quick retries ride out a blip instead of dropping the
    // event on the very first failure, and once the broker is clearly
    // down, the circuit opens so every subsequent publish fails in
    // microseconds instead of each one waiting out its own doomed
    // connection attempt.

    @Bean
    public Retry eventPublisherRetry(RetryRegistry registry) {

        return registry.retry("eventPublisher", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                // Retrying a call the breaker itself refused would just
                // burn through the retry budget without ever reaching
                // RabbitMQ - there is nothing to "ride out" yet.
                .ignoreExceptions(CallNotPermittedException.class)
                .build());
    }

    @Bean
    public CircuitBreaker eventPublisherCircuitBreaker(CircuitBreakerRegistry registry) {

        return registry.circuitBreaker("eventPublisher", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build());
    }

    // OBJECT STORAGE UPLOAD (StorageService#upload)
    //
    // Unlike the publish above, a failed upload is NOT swallowed - see
    // StorageService's own Javadoc, object storage is the whole point of a
    // syllabus/report-export request. Retry/circuit breaker here exist to
    // make a request MORE likely to succeed (ride out a transient network
    // blip) and a doomed one fail FASTER (once the breaker is open, a
    // request still ends in the same 502, just in microseconds instead of
    // however long three real attempts against an unreachable server would
    // take).

    @Bean
    public Retry objectStorageUploadRetry(RetryRegistry registry) {

        return registry.retry("objectStorageUpload", RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(300))
                .ignoreExceptions(CallNotPermittedException.class)
                .build());
    }

    @Bean
    public CircuitBreaker objectStorageUploadCircuitBreaker(CircuitBreakerRegistry registry) {

        return registry.circuitBreaker("objectStorageUpload", CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .minimumNumberOfCalls(5)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build());
    }
}
