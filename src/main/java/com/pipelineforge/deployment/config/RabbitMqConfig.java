package com.pipelineforge.deployment.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {
	public static final String DEPLOYMENT_EXCHANGE = "pipelineforge.deployment.exchange";
	public static final String DEPLOYMENT_QUEUE = "pipelineforge.deployment.queue";
	public static final String DEPLOYMENT_ROUTING_KEY = "pipelineforge.deployment";
	public static final String DEPLOYMENT_DLQ = "pipelineforge.deployment.dlq";
	public static final String DEPLOYMENT_DLX = "pipelineforge.deployment.dlx";
	public static final String DEPLOYMENT_DLK = "pipelineforge.deployment.dlq";

	@Bean
	public DirectExchange deploymentExchange() {
		return new DirectExchange(DEPLOYMENT_EXCHANGE);
	}

	@Bean
	public DirectExchange deploymentDeadLetterExchange() {
		return new DirectExchange(DEPLOYMENT_DLX);
	}

	@Bean
	public Queue deploymentQueue() {
		return QueueBuilder.durable(DEPLOYMENT_QUEUE)
				.deadLetterExchange(DEPLOYMENT_DLX)
				.deadLetterRoutingKey(DEPLOYMENT_DLK)
				.build();
	}

	@Bean
	public Queue deploymentDeadLetterQueue() {
		return QueueBuilder.durable(DEPLOYMENT_DLQ).build();
	}

	@Bean
	public Binding deploymentBinding(Queue deploymentQueue, DirectExchange deploymentExchange) {
		return BindingBuilder.bind(deploymentQueue).to(deploymentExchange).with(DEPLOYMENT_ROUTING_KEY);
	}

	@Bean
	public Binding deploymentDeadLetterBinding(Queue deploymentDeadLetterQueue, DirectExchange deploymentDeadLetterExchange) {
		return BindingBuilder.bind(deploymentDeadLetterQueue).to(deploymentDeadLetterExchange).with(DEPLOYMENT_DLK);
	}

	@Bean
	public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
		return new Jackson2JsonMessageConverter();
	}

	@Bean
	public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, Jackson2JsonMessageConverter converter) {
		RabbitTemplate template = new RabbitTemplate(connectionFactory);
		template.setMessageConverter(converter);
		return template;
	}

	@Bean
	public RabbitListenerContainerFactory<?> rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
			Jackson2JsonMessageConverter converter) {
		SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
		factory.setConnectionFactory(connectionFactory);
		factory.setMessageConverter(converter);
		factory.setAdviceChain(RetryInterceptorBuilder.stateless()
				.maxAttempts(3)
				.recoverer(new RejectAndDontRequeueRecoverer())
				.build());
		return factory;
	}
}
