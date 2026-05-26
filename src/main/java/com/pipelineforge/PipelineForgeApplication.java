package com.pipelineforge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class PipelineForgeApplication {
	public static void main(String[] args) {
		SpringApplication.run(PipelineForgeApplication.class, args);
	}
}
