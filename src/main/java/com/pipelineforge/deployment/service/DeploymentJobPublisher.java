package com.pipelineforge.deployment.service;

import com.pipelineforge.deployment.config.RabbitMqConfig;
import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
public class DeploymentJobPublisher {
	private static final Logger logger = LoggerFactory.getLogger(DeploymentJobPublisher.class);

	private final RabbitTemplate rabbitTemplate;

	public DeploymentJobPublisher(RabbitTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void publish(DeploymentJobEvent event) {
		logger.info("Publishing deployment event messageId={} pipelineId={}", event.messageId(), event.pipelineId());
		rabbitTemplate.convertAndSend(RabbitMqConfig.DEPLOYMENT_EXCHANGE, RabbitMqConfig.DEPLOYMENT_ROUTING_KEY, event);
	}
}
