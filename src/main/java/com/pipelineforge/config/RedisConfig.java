package com.pipelineforge.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipelineforge.pipeline.dto.PipelineResponse;
import com.pipelineforge.deployment.dto.DeploymentResponse;
import com.pipelineforge.repositorymgmt.dto.RepositoryResponse;
import java.util.HashMap;
import java.util.Map;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
@EnableCaching
public class RedisConfig {
	@Bean
	public RedisCacheManager cacheManager(
			RedisConnectionFactory connectionFactory,
			CacheTtlProperties ttlProperties,
			ObjectMapper objectMapper
	) {
		ObjectMapper cacheObjectMapper = objectMapper.copy();
		cacheObjectMapper.activateDefaultTyping(
				cacheObjectMapper.getPolymorphicTypeValidator(),
				ObjectMapper.DefaultTyping.NON_FINAL,
				com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
		);

		RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new GenericJackson2JsonRedisSerializer(cacheObjectMapper)))
				.disableCachingNullValues();

		Map<String, RedisCacheConfiguration> configs = new HashMap<>();
		
		configs.put(CacheNames.PIPELINE_STATUS, defaultConfig
				.entryTtl(ttlProperties.pipelineStatus())
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, PipelineResponse.class))));
						
		configs.put(CacheNames.DEPLOYMENT_SUMMARY, defaultConfig
				.entryTtl(ttlProperties.deploymentSummary())
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, DeploymentResponse.class))));
						
		configs.put(CacheNames.REPOSITORY, defaultConfig
				.entryTtl(ttlProperties.repository())
				.serializeValuesWith(RedisSerializationContext.SerializationPair
						.fromSerializer(new Jackson2JsonRedisSerializer<>(objectMapper, RepositoryResponse.class))));

		return RedisCacheManager.builder(connectionFactory)
				.cacheDefaults(defaultConfig)
				.withInitialCacheConfigurations(configs)
				.transactionAware()
				.build();
	}
}
