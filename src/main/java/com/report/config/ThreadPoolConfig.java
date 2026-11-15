package com.report.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
public class ThreadPoolConfig {

    @Value("${task.pool.core-size}")
    private int coreSize;
    @Value("${task.pool.max-size}")
    private int maxSize;
    @Value("${task.pool.queue-capacity}")
    private int queueCapacity;

    private final AtomicInteger threadSeq = new AtomicInteger(1);

    /**
     * 导出线程池。
     * 注意：CallerRunsPolicy 在队列满且线程达到上限时，会由提交任务的 Tomcat 请求线程直接执行导出，
     * 高并发下可能耗尽 Web 线程导致其他接口排队；此处按既定参数保留，后续可评估改为 AbortPolicy + 拒绝告警。
     */
    @Bean("reportExportPool")
    public Executor reportExportPool(){
        return new ThreadPoolExecutor(
                coreSize,
                maxSize,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                new ExportThreadFactory(threadSeq),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 为导出线程池生成可读线程名，便于 jstack / 日志排查。
     */
    private static class ExportThreadFactory implements ThreadFactory {

        private final AtomicInteger seq;

        ExportThreadFactory(AtomicInteger seq) {
            this.seq = seq;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "report-export-pool-" + seq.getAndIncrement());
            thread.setDaemon(false);
            return thread;
        }
    }
}
