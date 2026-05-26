package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class DeploymentJobConsumer {
	private static final Logger logger = LoggerFactory.getLogger(DeploymentJobConsumer.class);

	private final DeploymentJobService deploymentJobService;

	public DeploymentJobConsumer(DeploymentJobService deploymentJobService) {
		this.deploymentJobService = deploymentJobService;
	}

	@RabbitListener(queues = "pipelineforge.deployment.queue", containerFactory = "rabbitListenerContainerFactory")
	public void consume(DeploymentJobEvent event) {
		logger.info("Consuming deployment event messageId={} pipelineId={}", event.messageId(), event.pipelineId());
		deploymentJobService.process(event);
	}
}
