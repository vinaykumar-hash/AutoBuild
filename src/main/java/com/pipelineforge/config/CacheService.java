package com.pipelineforge.config;

import java.util.UUID;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Service
public class CacheService {
	private final CacheManager cacheManager;

	public CacheService(CacheManager cacheManager) {
		this.cacheManager = cacheManager;
	}

	public void evictPipelineStatus(UUID pipelineId) {
		evict(CacheNames.PIPELINE_STATUS, pipelineId);
	}

	public void evictDeploymentSummary(UUID deploymentId) {
		evict(CacheNames.DEPLOYMENT_SUMMARY, deploymentId);
	}

	public void evictRepository(UUID repositoryId) {
		evict(CacheNames.REPOSITORY, repositoryId);
	}

	public void evictAll(String cacheName) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache != null) {
			cache.clear();
		}
	}

	private void evict(String cacheName, Object key) {
		Cache cache = cacheManager.getCache(cacheName);
		if (cache != null) {
			cache.evict(key);
		}
	}
}
