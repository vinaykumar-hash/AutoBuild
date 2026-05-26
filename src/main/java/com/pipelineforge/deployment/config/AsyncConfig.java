package com.pipelineforge.deployment.config;

import com.pipelineforge.security.tracing.MdcTaskDecorator;
import java.util.concurrent.Executor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {
	@Bean(name = "taskExecutor")
	public Executor taskExecutor() {
		SimpleAsyncTaskExecutor executor = new SimpleAsyncTaskExecutor("pipelineforge-async-");
		executor.setTaskDecorator(new MdcTaskDecorator());
		executor.setVirtualThreads(true);
		return executor;
	}
}
