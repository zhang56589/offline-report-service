package com.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;

@Configuration
public class ThreadPoolConfig {

    @Value("${task.pool.core-size}")
    private int coreSize;
    @Value("${task.pool.max-size}")
    private int maxSize;
    @Value("${task.pool.queue-capacity}")
    private int queueCapacity;

    @Bean("reportExportPool")
    public Executor reportExportPool(){
        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
