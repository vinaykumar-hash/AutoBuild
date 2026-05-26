package com.pipelineforge.deployment.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pipelineforge.config.CacheService;
import com.pipelineforge.deployment.config.DeploymentRunnerProperties;
import com.pipelineforge.deployment.config.DeploymentSimulationProperties;
import com.pipelineforge.deployment.dto.DeploymentJobEvent;
import com.pipelineforge.deployment.entity.DeploymentLogLevel;
import com.pipelineforge.deployment.entity.DeploymentRollbackPoint;
import com.pipelineforge.deployment.entity.DeploymentRollbackStatus;
import com.pipelineforge.deployment.entity.DeploymentJobExecution;
import com.pipelineforge.deployment.entity.DeploymentJobStatus;
import com.pipelineforge.deployment.repository.DeploymentJobExecutionRepository;
import com.pipelineforge.deployment.repository.DeploymentRollbackPointRepository;
import com.pipelineforge.deployment.repository.DeploymentRepository;
import com.pipelineforge.deployment.service.DeploymentLogService;
import com.pipelineforge.deployment.service.DeploymentService;
import com.pipelineforge.pipeline.entity.Pipeline;
import com.pipelineforge.pipeline.entity.PipelineStage;
import com.pipelineforge.pipeline.entity.PipelineStageStatus;
import com.pipelineforge.pipeline.entity.PipelineStatus;
import com.pipelineforge.pipeline.repository.PipelineRepository;
import com.pipelineforge.repositorymgmt.entity.CodeRepository;
import com.pipelineforge.repositorymgmt.repository.CodeRepositoryRepository;
import com.pipelineforge.security.crypto.EncryptionService;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
public class DeploymentExecutionEngine {
	private static final Logger logger = LoggerFactory.getLogger(DeploymentExecutionEngine.class);

	private static final String SHARED_DEPLOYMENTS_DIR = "/shared/deployments";

	private final DeploymentService deploymentService;
	private final DeploymentLogService deploymentLogService;
	private final DeploymentRollbackPointRepository rollbackPointRepository;
	private final DeploymentJobExecutionRepository executionRepository;
	private final DeploymentRepository deploymentRepository;
	private final PipelineRepository pipelineRepository;
	private final DeploymentSimulationProperties simulationProperties;
	private final DeploymentRunnerProperties runnerProperties;
	private final CacheService cacheService;
	private final TransactionTemplate transactionTemplate;
	private final CodeRepositoryRepository codeRepositoryRepository;
	private final EncryptionService encryptionService;

	public DeploymentExecutionEngine(
			DeploymentService deploymentService,
			DeploymentLogService deploymentLogService,
			DeploymentRollbackPointRepository rollbackPointRepository,
			DeploymentJobExecutionRepository executionRepository,
			DeploymentRepository deploymentRepository,
			PipelineRepository pipelineRepository,
			DeploymentSimulationProperties simulationProperties,
			DeploymentRunnerProperties runnerProperties,
			CacheService cacheService,
			PlatformTransactionManager transactionManager,
			CodeRepositoryRepository codeRepositoryRepository,
			EncryptionService encryptionService
	) {
		this.deploymentService = deploymentService;
		this.deploymentLogService = deploymentLogService;
		this.rollbackPointRepository = rollbackPointRepository;
		this.executionRepository = executionRepository;
		this.deploymentRepository = deploymentRepository;
		this.pipelineRepository = pipelineRepository;
		this.simulationProperties = simulationProperties;
		this.runnerProperties = runnerProperties;
		this.cacheService = cacheService;
		this.transactionTemplate = new TransactionTemplate(transactionManager);
		this.codeRepositoryRepository = codeRepositoryRepository;
		this.encryptionService = encryptionService;
	}

	private void savePipelineAndEvictCache(Pipeline pipeline) {
		pipelineRepository.save(pipeline);
		final UUID pipelineId = pipeline.getId();
		if (TransactionSynchronizationManager.isSynchronizationActive()) {
			TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
				@Override
				public void afterCommit() {
					cacheService.evictPipelineStatus(pipelineId);
				}
			});
		} else {
			cacheService.evictPipelineStatus(pipelineId);
		}
	}

	private record PipelineStageInfo(UUID id, String name) {}

	@Async
	public void executeAsync(DeploymentJobEvent event) {
		final UUID finalDeploymentId = transactionTemplate.execute(status -> {
			DeploymentJobExecution execution = executionRepository.findByMessageId(event.messageId())
					.orElseThrow(() -> new IllegalStateException("Deployment execution not found"));
			UUID deploymentId = execution.getDeploymentId();
			if (deploymentId == null) {
				deploymentId = deploymentService.startDeployment(event);
				execution.setDeploymentId(deploymentId);
			}
			execution.setStatus(DeploymentJobStatus.RUNNING);
			Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
					.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
			pipeline.setStatus(PipelineStatus.RUNNING);
			pipeline.setLastExecutionStartedAt(Instant.now());
			pipelineRepository.save(pipeline);
			executionRepository.save(execution);
			return deploymentId;
		});

		String branch = transactionTemplate.execute(status -> {
			Pipeline p = pipelineRepository.findById(event.pipelineId()).orElse(null);
			if (p != null) {
				CodeRepository r = codeRepositoryRepository.findById(p.getRepositoryId()).orElse(null);
				if (r != null) {
					return r.getBranch();
				}
			}
			return "main";
		});

		UUID repoId = transactionTemplate.execute(status -> {
			Pipeline p = pipelineRepository.findById(event.pipelineId()).orElse(null);
			return p != null ? p.getRepositoryId() : null;
		});

		Map<String, String> stagesConfig = repoId != null ? getPipelineConfig(repoId, branch, finalDeploymentId) : Collections.emptyMap();
		File workingDir = null;

		if (!stagesConfig.isEmpty()) {
			workingDir = downloadAndExtractRepo(repoId, branch, finalDeploymentId);
		}

		try {
			List<PipelineStageInfo> stages = transactionTemplate.execute(status -> {
				Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
						.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
				return pipeline.getStages().stream()
						.sorted(Comparator.comparingInt(PipelineStage::getExecutionOrder))
						.map(s -> new PipelineStageInfo(s.getId(), s.getStageName().name()))
						.toList();
			});

			if (stages != null) {
				for (PipelineStageInfo stageInfo : stages) {
					transactionTemplate.executeWithoutResult(status -> {
						Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
								.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
						PipelineStage stage = pipeline.getStages().stream()
								.filter(s -> s.getId().equals(stageInfo.id()))
								.findFirst()
								.orElseThrow(() -> new IllegalStateException("Stage not found"));
						stage.setStatus(PipelineStageStatus.RUNNING);
						stage.setStartedAt(Instant.now());
						savePipelineAndEvictCache(pipeline);

						deploymentRepository.findById(finalDeploymentId).ifPresent(deployment -> {
							deployment.setCurrentStage(stageInfo.name());
							deploymentRepository.save(deployment);
						});

						deploymentLogService.append(finalDeploymentId, "Stage started: " + stageInfo.name(), DeploymentLogLevel.INFO);
						rollbackPointRepository.save(DeploymentRollbackPoint.builder()
								.deploymentId(finalDeploymentId)
								.stageName(stageInfo.name())
								.status(DeploymentRollbackStatus.PREPARED)
								.createdAt(Instant.now())
								.metadata("Prepared rollback point for stage")
								.build());
					});

					String command = stagesConfig.get(stageInfo.name().toLowerCase());
					boolean failed = false;

					if (command != null && workingDir != null) {
						try {
							executeCommand(command, workingDir, finalDeploymentId, stageInfo.name());
						} catch (Exception ex) {
							failed = true;
							transactionTemplate.executeWithoutResult(status -> {
								Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
										.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
								PipelineStage stage = pipeline.getStages().stream()
										.filter(s -> s.getId().equals(stageInfo.id()))
										.findFirst()
										.orElseThrow(() -> new IllegalStateException("Stage not found"));
								stage.setStatus(PipelineStageStatus.FAILED);
								stage.setFinishedAt(Instant.now());
								savePipelineAndEvictCache(pipeline);
								deploymentLogService.append(finalDeploymentId, "Stage failed with error: " + ex.getMessage(), DeploymentLogLevel.ERROR);
							});
							throw new IllegalStateException("Command execution failed for stage " + stageInfo.name() + ": " + ex.getMessage());
						}
					} else {
						// Fallback to simulation mode
						failed = shouldFail(stageInfo.name());
						if (failed) {
							transactionTemplate.executeWithoutResult(status -> {
								Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
										.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
								PipelineStage stage = pipeline.getStages().stream()
										.filter(s -> s.getId().equals(stageInfo.id()))
										.findFirst()
										.orElseThrow(() -> new IllegalStateException("Stage not found"));
								stage.setStatus(PipelineStageStatus.FAILED);
								stage.setFinishedAt(Instant.now());
								savePipelineAndEvictCache(pipeline);
								deploymentLogService.append(finalDeploymentId, "Stage failed: " + stageInfo.name(), DeploymentLogLevel.ERROR);
							});
							throw new IllegalStateException("Simulated failure at stage " + stageInfo.name());
						}
					}

					transactionTemplate.executeWithoutResult(status -> {
						Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
								.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
						PipelineStage stage = pipeline.getStages().stream()
								.filter(s -> s.getId().equals(stageInfo.id()))
								.findFirst()
								.orElseThrow(() -> new IllegalStateException("Stage not found"));
						stage.setStatus(PipelineStageStatus.SUCCESS);
						stage.setFinishedAt(Instant.now());
						savePipelineAndEvictCache(pipeline);
						deploymentLogService.append(finalDeploymentId, "Stage completed: " + stageInfo.name(), DeploymentLogLevel.INFO);
					});
				}
			}

			transactionTemplate.executeWithoutResult(status -> {
				Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
						.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
				pipeline.setStatus(PipelineStatus.SUCCESS);
				pipeline.setLastExecutionFinishedAt(Instant.now());
				savePipelineAndEvictCache(pipeline);

				deploymentService.markSuccess(finalDeploymentId);

				DeploymentJobExecution execution = executionRepository.findByMessageId(event.messageId())
						.orElseThrow(() -> new IllegalStateException("Deployment execution not found"));
				execution.setStatus(DeploymentJobStatus.SUCCESS);
				execution.setFinishedAt(Instant.now());
				executionRepository.save(execution);

				deploymentRepository.findById(finalDeploymentId).ifPresent(deployment -> {
					deployment.setCurrentStage(null);
					deploymentRepository.save(deployment);
				});

				cacheService.evictDeploymentSummary(finalDeploymentId);
				logger.info("Deployment succeeded pipelineId={} deploymentId={}", pipeline.getId(), finalDeploymentId);
			});

		} catch (Exception ex) {
			transactionTemplate.executeWithoutResult(status -> {
				Pipeline pipeline = pipelineRepository.findById(event.pipelineId())
						.orElseThrow(() -> new IllegalStateException("Pipeline not found"));
				pipeline.setStatus(PipelineStatus.FAILED);
				pipeline.setLastExecutionFinishedAt(Instant.now());
				savePipelineAndEvictCache(pipeline);

				rollbackPointRepository.findByDeploymentIdOrderByCreatedAtDesc(finalDeploymentId)
						.forEach(point -> {
							point.setStatus(DeploymentRollbackStatus.ROLLED_BACK);
							rollbackPointRepository.save(point);
						});

				deploymentService.markFailed(finalDeploymentId, ex.getMessage());

				DeploymentJobExecution execution = executionRepository.findByMessageId(event.messageId())
						.orElseThrow(() -> new IllegalStateException("Deployment execution not found"));
				execution.setStatus(DeploymentJobStatus.FAILED);
				execution.setFinishedAt(Instant.now());
				executionRepository.save(execution);

				deploymentRepository.findById(finalDeploymentId).ifPresent(deployment -> {
					deployment.setCurrentStage(null);
					deploymentRepository.save(deployment);
				});

				cacheService.evictDeploymentSummary(finalDeploymentId);
				logger.error("Deployment failed pipelineId={} deploymentId={} error={}", pipeline.getId(), finalDeploymentId, ex.getMessage());
			});

			// On failure, clean up the shared deployment directory
			File deployDir = new File(SHARED_DEPLOYMENTS_DIR + "/" + finalDeploymentId);
			if (deployDir.exists()) {
				deleteDirectory(deployDir);
			}
		}
	}

	private boolean shouldFail(String stageName) {
		String configuredStage = simulationProperties.failStage();
		if (configuredStage != null && !configuredStage.isBlank() && configuredStage.equalsIgnoreCase(stageName)) {
			return true;
		}
		double failureRate = simulationProperties.failureRate();
		return failureRate > 0 && ThreadLocalRandom.current().nextDouble() < failureRate;
	}

	private void executeCommand(String command, File workingDir, UUID deploymentId, String stageName) throws Exception {
		String containerName = runnerProperties.containerName();
		String containerPath = workingDir.getAbsolutePath();
		boolean isDeployStage = "DEPLOY".equalsIgnoreCase(stageName);

		if (isDeployStage) {
			// Kill any previously running Node.js process in the runner
			deploymentLogService.append(deploymentId, "Stopping any previously running application...", DeploymentLogLevel.INFO);
			try {
				ProcessBuilder killPb = new ProcessBuilder(
					"docker", "exec", containerName, "sh", "-c", "pkill -f 'node' || true"
				);
				killPb.redirectErrorStream(true);
				Process killProcess = killPb.start();
				killProcess.waitFor();
			} catch (Exception e) {
				logger.warn("Failed to kill previous processes: {}", e.getMessage());
			}

			// Run the deploy command in detached mode (nohup + background)
			String deployLogFile = containerPath + "/deploy.log";
			String wrappedCommand = String.format(
				"cd %s && nohup %s > %s 2>&1 & echo $!",
				containerPath, command, deployLogFile
			);
			deploymentLogService.append(deploymentId, "Executing deploy command: " + command, DeploymentLogLevel.INFO);

			ProcessBuilder pb = new ProcessBuilder(
				"docker", "exec", containerName, "sh", "-c", wrappedCommand
			);
			pb.redirectErrorStream(true);
			Process process = pb.start();

			String pid = null;
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					pid = line.trim();
					logger.info("[Deploy PID] {}", pid);
				}
			}
			process.waitFor();

			deploymentLogService.append(deploymentId, "Application started with PID: " + pid, DeploymentLogLevel.INFO);

			// Wait for the application to start up
			int waitSeconds = runnerProperties.startupWaitSeconds();
			deploymentLogService.append(deploymentId, "Waiting " + waitSeconds + "s for application to start...", DeploymentLogLevel.INFO);
			Thread.sleep(waitSeconds * 1000L);

			// Verify the process is still alive
			ProcessBuilder checkPb = new ProcessBuilder(
				"docker", "exec", containerName, "sh", "-c", "pgrep -f 'node' > /dev/null 2>&1"
			);
			checkPb.redirectErrorStream(true);
			Process checkProcess = checkPb.start();
			int checkExit = checkProcess.waitFor();

			if (checkExit != 0) {
				// Process died, read the deploy log for error details
				ProcessBuilder logPb = new ProcessBuilder(
					"docker", "exec", containerName, "sh", "-c", "cat " + deployLogFile + " 2>/dev/null || echo 'No deploy log found'"
				);
				logPb.redirectErrorStream(true);
				Process logProcess = logPb.start();
				StringBuilder errorLog = new StringBuilder();
				try (BufferedReader reader = new BufferedReader(new InputStreamReader(logProcess.getInputStream(), StandardCharsets.UTF_8))) {
					String line;
					while ((line = reader.readLine()) != null) {
						errorLog.append(line).append("\n");
						deploymentLogService.append(deploymentId, line, DeploymentLogLevel.ERROR);
					}
				}
				logProcess.waitFor();
				throw new IllegalStateException("Deploy command failed. Application process exited. Check logs for details.");
			}

			// Health check
			deploymentLogService.append(deploymentId, "Running health check...", DeploymentLogLevel.INFO);
			ProcessBuilder healthPb = new ProcessBuilder(
				"docker", "exec", containerName, "sh", "-c",
				"curl -sf " + runnerProperties.healthCheckUrl() + " > /dev/null 2>&1"
			);
			healthPb.redirectErrorStream(true);
			Process healthProcess = healthPb.start();
			int healthExit = healthProcess.waitFor();

			if (healthExit == 0) {
				deploymentLogService.append(deploymentId, "Health check passed! Application is live at " + runnerProperties.healthCheckUrl(), DeploymentLogLevel.INFO);
			} else {
				deploymentLogService.append(deploymentId, "Health check did not pass, but process is running. Application may still be starting.", DeploymentLogLevel.WARN);
			}

		} else {
			// BUILD / TEST stages: run blocking via docker exec
			String dockerCommand = String.format("cd %s && %s", containerPath, command);
			deploymentLogService.append(deploymentId, "Executing command: " + command, DeploymentLogLevel.INFO);

			ProcessBuilder pb = new ProcessBuilder(
				"docker", "exec", containerName, "sh", "-c", dockerCommand
			);
			pb.redirectErrorStream(true);

			Process process = pb.start();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					logger.info("[Process Log] {}", line);
					deploymentLogService.append(deploymentId, line, DeploymentLogLevel.INFO);
				}
			}

			int exitCode = process.waitFor();
			if (exitCode != 0) {
				throw new IllegalStateException("Command failed with exit code: " + exitCode);
			}
		}
	}

	private Map<String, String> getPipelineConfig(UUID repositoryId, String branch, UUID deploymentId) {
		try {
			CodeRepository repository = codeRepositoryRepository.findById(repositoryId)
					.orElseThrow(() -> new IllegalStateException("Repository not found"));
			
			String token = repository.getEncryptedAccessToken();
			String githubUrl = repository.getGithubUrl();
			
			String url = githubUrl;
			if (url.endsWith(".git")) {
				url = url.substring(0, url.length() - 4);
			}
			String[] parts = url.replace("https://github.com/", "").split("/");
			if (parts.length < 2) {
				return Collections.emptyMap();
			}
			String owner = parts[0];
			String repo = parts[1];
			
			String apiEndpoint = String.format("https://api.github.com/repos/%s/%s/contents/pipelineforge.yml?ref=%s", owner, repo, branch);
			HttpURLConnection conn = (HttpURLConnection) new URI(apiEndpoint).toURL().openConnection();
			conn.setRequestMethod("GET");
			if (token != null && !token.isBlank() && !"dummy".equalsIgnoreCase(token)) {
				conn.setRequestProperty("Authorization", "Bearer " + token);
			}
			conn.setRequestProperty("Accept", "application/vnd.github+json");
			conn.setRequestProperty("User-Agent", "PipelineForge");
			
			if (conn.getResponseCode() != 200) {
				logger.warn("No pipelineforge.yml found. Using simulation mode. HTTP {}", conn.getResponseCode());
				deploymentLogService.append(deploymentId, "No pipelineforge.yml found in repository. Defaulting to simulation mode.", DeploymentLogLevel.INFO);
				return Collections.emptyMap();
			}
			
			StringBuilder response = new StringBuilder();
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
				String line;
				while ((line = reader.readLine()) != null) {
					response.append(line);
				}
			}
			
			ObjectMapper mapper = new ObjectMapper();
			Map<?, ?> json = mapper.readValue(response.toString(), Map.class);
			String base64Content = ((String) json.get("content")).replaceAll("\\s", "");
			byte[] bytes = Base64.getDecoder().decode(base64Content);
			String yamlContent = new String(bytes, StandardCharsets.UTF_8);
			
			org.yaml.snakeyaml.Yaml yamlParser = new org.yaml.snakeyaml.Yaml();
			Map<?, ?> root = yamlParser.load(yamlContent);
			if (root != null && root.get("stages") instanceof Map) {
				Map<String, String> stagesMap = new java.util.HashMap<>();
				Map<?, ?> rawStages = (Map<?, ?>) root.get("stages");
				for (Map.Entry<?, ?> entry : rawStages.entrySet()) {
					if (entry.getKey() != null && entry.getValue() != null) {
						stagesMap.put(entry.getKey().toString().toLowerCase(), entry.getValue().toString());
					}
				}
				return stagesMap;
			}
		} catch (Exception e) {
			logger.error("Error loading pipelineforge.yml from repository", e);
			deploymentLogService.append(deploymentId, "Warning: Failed to fetch pipelineforge.yml: " + e.getMessage() + ". Defaulting to simulation mode.", DeploymentLogLevel.WARN);
		}
		return Collections.emptyMap();
	}

	private File downloadAndExtractRepo(UUID repositoryId, String branch, UUID deploymentId) {
		try {
			CodeRepository repository = codeRepositoryRepository.findById(repositoryId)
					.orElseThrow(() -> new IllegalStateException("Repository not found"));
			
			String token = repository.getEncryptedAccessToken();
			String githubUrl = repository.getGithubUrl();
			
			String urlStr = githubUrl;
			if (urlStr.endsWith(".git")) {
				urlStr = urlStr.substring(0, urlStr.length() - 4);
			}
			String[] parts = urlStr.replace("https://github.com/", "").split("/");
			if (parts.length < 2) {
				return null;
			}
			String owner = parts[0];
			String repo = parts[1];
			
			String zipUrlStr = String.format("https://api.github.com/repos/%s/%s/zipball/%s", owner, repo, branch);
			deploymentLogService.append(deploymentId, "Downloading repository source archive...", DeploymentLogLevel.INFO);
			
			URL url = new URI(zipUrlStr).toURL();
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			if (token != null && !token.isBlank() && !"dummy".equalsIgnoreCase(token)) {
				conn.setRequestProperty("Authorization", "Bearer " + token);
			}
			conn.setRequestProperty("Accept", "application/vnd.github+json");
			conn.setRequestProperty("User-Agent", "PipelineForge");
			
			int status = conn.getResponseCode();
			if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM || status == 307 || status == 308) {
				String newUrl = conn.getHeaderField("Location");
				conn = (HttpURLConnection) new URI(newUrl).toURL().openConnection();
				conn.setRequestMethod("GET");
				conn.setRequestProperty("User-Agent", "PipelineForge");
			}
			
			if (conn.getResponseCode() != 200) {
				throw new IllegalStateException("Failed to download repository zipball: HTTP " + conn.getResponseCode());
			}
			
			File destDir = new File(SHARED_DEPLOYMENTS_DIR + "/" + deploymentId);
			if (!destDir.exists()) {
				destDir.mkdirs();
			}
			
			try (ZipInputStream zipIn = new ZipInputStream(new BufferedInputStream(conn.getInputStream()))) {
				ZipEntry entry = zipIn.getNextEntry();
				while (entry != null) {
					File filePath = new File(destDir, entry.getName());
					if (!entry.isDirectory()) {
						filePath.getParentFile().mkdirs();
						try (FileOutputStream fos = new FileOutputStream(filePath)) {
							byte[] bytesIn = new byte[4096];
							int read;
							while ((read = zipIn.read(bytesIn)) != -1) {
								fos.write(bytesIn, 0, read);
							}
						}
					} else {
						filePath.mkdirs();
					}
					zipIn.closeEntry();
					entry = zipIn.getNextEntry();
				}
			}
			
			File[] files = destDir.listFiles();
			if (files != null && files.length > 0) {
				for (File f : files) {
					if (f.isDirectory()) {
						deploymentLogService.append(deploymentId, "Repository source extracted successfully.", DeploymentLogLevel.INFO);
						return f;
					}
				}
			}
			return destDir;
		} catch (Exception e) {
			logger.error("Failed to download or extract repository archive", e);
			deploymentLogService.append(deploymentId, "Error: Failed to download/extract repository archive: " + e.getMessage(), DeploymentLogLevel.ERROR);
		}
		return null;
	}

	private void deleteDirectory(File dir) {
		File[] allContents = dir.listFiles();
		if (allContents != null) {
			for (File file : allContents) {
				deleteDirectory(file);
			}
		}
		dir.delete();
	}
}
