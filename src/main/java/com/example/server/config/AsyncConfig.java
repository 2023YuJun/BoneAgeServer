package com.example.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
@EnableAsync
public class AsyncConfig implements AsyncConfigurer {
    // 请求级别线程池（每个请求独立线程）
    @Override
    public Executor getAsyncExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Request-");
        executor.initialize();
        return executor;
    }

    // DICOM操作线程池
    @Bean(name = "dicomTaskExecutor")
    public ScheduledExecutorService dicomTaskExecutor() {
        return Executors.newScheduledThreadPool(20);
    }

    // 图像处理线程池
    @Bean(name = "imageProcessingExecutor")
    public Executor imageProcessingExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(32); // 核心线程数
        executor.setMaxPoolSize(64); // 最大线程数
        executor.setQueueCapacity(200); // 队列容量
        executor.setThreadNamePrefix("ImageProcessor-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "controllerTaskExecutor")
    public Executor controllerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);  // 核心线程数
        executor.setMaxPoolSize(50);   // 最大线程数
        executor.setQueueCapacity(200); // 队列容量
        executor.setThreadNamePrefix("Controller-Async-");
        executor.initialize();
        return executor;
    }
}