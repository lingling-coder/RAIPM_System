package com.institute.achievement.reminder.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Dedicated async thread pool for email sending (D-11, T-4-05).
 * <p>
 * Provides a bounded thread pool specifically for async email delivery
 * so that email backlog does not impact other async operations in the system.
 * <p>
 * Sizing rationale (I/O-bound network calls to SMTP):
 * <ul>
 *   <li>Core pool: 4 — enough for concurrent SMTP connections</li>
 *   <li>Max pool: 8 — allows burst under load</li>
 *   <li>Queue: 200 — smooths out spikes without overwhelming SMTP server</li>
 *   <li>CallerRunsPolicy — graceful degradation: caller thread sends email
 *       directly when queue is full, preventing silent message loss</li>
 * </ul>
 * <p>
 * Per T-4-05: Bounded pool prevents OOM from uncontrolled thread creation.
 * Per D-11: Async email sending via dedicated @Async("emailTaskExecutor").
 */
@Slf4j
@Configuration
@EnableAsync
public class EmailAsyncConfig {

    /**
     * Email task executor bean referenced by @Async("emailTaskExecutor").
     *
     * @return configured ThreadPoolTaskExecutor for async email sending
     */
    @Bean("emailTaskExecutor")
    public Executor emailTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("EmailAsync-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();

        log.info("Email async thread pool initialized: core=4, max=8, queue=200, CallerRunsPolicy");
        return executor;
    }
}
