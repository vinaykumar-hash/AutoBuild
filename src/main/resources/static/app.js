/**
 * AutoBuild Single Page Application JavaScript Controller
 */

// UI View Management
const VIEWS = ['auth', 'dashboard', 'repositories', 'pipelines', 'logs'];
let currentView = 'auth';

// Token and Session Management
const tokenKey = 'pf_token';
const nameKey = 'pf_name';
const roleKey = 'pf_role';

// Polling and Execution logs state
let logsPollInterval = null;
let currentPollingRunId = null;
let autoscrollEnabled = true;

// UI Elements mapping
const elements = {
	loadingOverlay: document.getElementById('loading-overlay'),
	sidebar: document.getElementById('sidebar'),
	mainContent: document.getElementById('main-content'),
	logoutBtn: document.getElementById('logout-btn'),
	userDisplayName: document.getElementById('user-display-name'),
	
	// Auth Elements
	toRegister: document.getElementById('to-register'),
	toLogin: document.getElementById('to-login'),
	loginForm: document.getElementById('login-form'),
	registerForm: document.getElementById('register-form'),
	loginContainer: document.getElementById('auth-login-container'),
	registerContainer: document.getElementById('auth-register-container'),
	loginError: document.getElementById('login-error'),
	registerError: document.getElementById('register-error'),

	// Dashboard Elements
	statRepos: document.getElementById('stat-repos-count'),
	statPipelines: document.getElementById('stat-pipelines-count'),
	statSuccess: document.getElementById('stat-success-count'),
	statFailed: document.getElementById('stat-failed-count'),
	dashTriggerShortcut: document.getElementById('dash-trigger-shortcut'),
	refreshDeploysBtn: document.getElementById('refresh-deployments-btn'),
	deploymentsListTable: document.getElementById('dashboard-deployments-list'),
	quickTriggerForm: document.getElementById('quick-trigger-form'),
	quickPipelineSelect: document.getElementById('quick-pipeline-select'),
	actuatorHealth: document.getElementById('actuator-health-badge'),

	// Repositories Elements
	openRepoModal: document.getElementById('open-register-repo-modal'),
	closeRepoCard: document.getElementById('close-register-repo-btn'),
	registerRepoCard: document.getElementById('register-repo-card'),
	registerRepoForm: document.getElementById('register-repo-form'),
	cancelRegisterRepo: document.getElementById('cancel-register-repo'),
	repoError: document.getElementById('repo-error'),
	reposGrid: document.getElementById('repositories-list-grid'),

	// Pipelines Elements
	openPipelineModal: document.getElementById('open-create-pipeline-modal'),
	closePipelineCard: document.getElementById('close-create-pipeline-btn'),
	createPipelineCard: document.getElementById('create-pipeline-card'),
	createPipelineForm: document.getElementById('create-pipeline-form'),
	cancelCreatePipeline: document.getElementById('cancel-create-pipeline'),
	pipelineError: document.getElementById('pipeline-error'),
	pipelinesGrid: document.getElementById('pipelines-list-grid'),
	pipelineRepoSelect: document.getElementById('pipeline-repo-select'),

	// Logs / Console Elements
	backToDash: document.getElementById('back-to-dashboard-btn'),
	executionList: document.getElementById('execution-runs-list-ul'),
	consoleRunId: document.getElementById('console-run-id'),
	consolePipelineName: document.getElementById('console-pipeline-name'),
	consoleMetadata: document.getElementById('console-metadata-panel'),
	consoleStagesProgress: document.getElementById('console-stages-progress'),
	consoleTerminal: document.getElementById('console-terminal-body'),
	autoscrollToggle: document.getElementById('terminal-autoscroll-toggle'),
	consoleRollback: document.getElementById('console-rollback-btn')
};

// --- BASE API REQUEST HELPER ---
async function apiCall(endpoint, method = 'GET', body = null) {
	const token = localStorage.getItem(tokenKey);
	const headers = {
		'Content-Type': 'application/json'
	};
	if (token) {
		headers['Authorization'] = `Bearer ${token}`;
	}

	const options = {
		method,
		headers
	};
	if (body) {
		options.body = JSON.stringify(body);
	}

	try {
		const response = await fetch(endpoint, options);
		
		// Unauthenticated - clear token and send to Login
		if (response.status === 401) {
			clearSession();
			showView('auth');
			throw new Error('Session expired. Please log in.');
		}

		if (response.status === 204) {
			return null;
		}

		const data = await response.json();

		if (!response.ok) {
			// Structured spring validations handle
			const message = data.message || 'An unexpected error occurred.';
			const error = new Error(message);
			error.validationErrors = data.validationErrors || {};
			error.status = response.status;
			throw error;
		}
		
		return data;
	} catch (err) {
		console.error(`API Error on ${endpoint}:`, err);
		throw err;
	}
}

// --- SESSION HELPERS ---
function setSession(token, name, role) {
	localStorage.setItem(tokenKey, token);
	localStorage.setItem(nameKey, name);
	localStorage.setItem(roleKey, role);
}

function clearSession() {
	localStorage.removeItem(tokenKey);
	localStorage.removeItem(nameKey);
	localStorage.removeItem(roleKey);
	stopLogsPolling();
}

function getSession() {
	return {
		token: localStorage.getItem(tokenKey),
		name: localStorage.getItem(nameKey),
		role: localStorage.getItem(roleKey)
	};
}

function updateUserInfoDisplay() {
	const session = getSession();
	if (session.name) {
		elements.userDisplayName.textContent = session.name;
	}
}

// --- ROUTER VIEW CHANGER ---
function showView(viewName) {
	if (!VIEWS.includes(viewName)) return;
	
	const session = getSession();
	// Guard against viewing pages if unauthenticated
	if (viewName !== 'auth' && !session.token) {
		viewName = 'auth';
	}
	// Guard against showing auth if authenticated
	if (viewName === 'auth' && session.token) {
		viewName = 'dashboard';
	}

	currentView = viewName;

	// Toggle view sections
	VIEWS.forEach(view => {
		const panel = document.getElementById(`view-${view}`);
		if (view === viewName) {
			panel.classList.remove('hidden');
		} else {
			panel.classList.add('hidden');
		}
	});

	// Toggle Sidebar / Layout Class
	if (viewName === 'auth') {
		elements.sidebar.classList.add('hidden');
		elements.mainContent.classList.add('full-width');
	} else {
		elements.sidebar.classList.remove('hidden');
		elements.mainContent.classList.remove('full-width');
		updateUserInfoDisplay();
		
		// Update active sidebar nav button
		document.querySelectorAll('.menu-item').forEach(item => {
			if (item.getAttribute('data-view') === viewName) {
				item.classList.add('active');
			} else {
				item.classList.remove('active');
			}
		});

		// Trigger view load events
		onViewLoaded(viewName);
	}
}

// --- LOAD ACTIONS PER VIEW ---
function onViewLoaded(viewName) {
	switch(viewName) {
		case 'dashboard':
			loadDashboardMetrics();
			loadRecentDeployments();
			loadQuickTriggerPipelines();
			checkActuatorHealth();
			break;
		case 'repositories':
			loadRepositoriesList();
			break;
		case 'pipelines':
			loadPipelinesList();
			loadRepositoriesDropdown();
			break;
		case 'logs':
			loadExecutionsHistory();
			break;
	}
}

// Show/hide loader
function showLoading(show) {
	if (show) elements.loadingOverlay.classList.remove('hidden');
	else elements.loadingOverlay.classList.add('hidden');
}

// --- 1. AUTH LOGIC ---
elements.toRegister.addEventListener('click', (e) => {
	e.preventDefault();
	elements.loginContainer.classList.add('hidden');
	elements.registerContainer.classList.remove('hidden');
});

elements.toLogin.addEventListener('click', (e) => {
	e.preventDefault();
	elements.registerContainer.classList.add('hidden');
	elements.loginContainer.classList.remove('hidden');
});

elements.loginForm.addEventListener('submit', async (e) => {
	e.preventDefault();
	elements.loginError.classList.add('hidden');
	showLoading(true);

	const email = document.getElementById('login-email').value;
	const password = document.getElementById('login-password').value;

	try {
		const res = await apiCall('/api/v1/auth/login', 'POST', { email, password });
		setSession(res.token, res.user.name, res.user.role);
		elements.loginForm.reset();
		showView('dashboard');
	} catch (err) {
		elements.loginError.textContent = err.message || 'Login failed. Please check credentials.';
		elements.loginError.classList.remove('hidden');
	} finally {
		showLoading(false);
	}
});

elements.registerForm.addEventListener('submit', async (e) => {
	e.preventDefault();
	elements.registerError.classList.add('hidden');
	showLoading(true);

	const name = document.getElementById('register-name').value;
	const email = document.getElementById('register-email').value;
	const password = document.getElementById('register-password').value;
	const role = document.getElementById('register-role').value;

	try {
		const res = await apiCall('/api/v1/auth/register', 'POST', { name, email, password, role });
		setSession(res.token, res.user.name, res.user.role);
		elements.registerForm.reset();
		showView('dashboard');
	} catch (err) {
		elements.registerError.textContent = err.message || 'Registration failed.';
		elements.registerError.classList.remove('hidden');
	} finally {
		showLoading(false);
	}
});

elements.logoutBtn.addEventListener('click', () => {
	clearSession();
	showView('auth');
});


// --- 2. DASHBOARD LOGIC ---
async function loadDashboardMetrics() {
	try {
		const [reposPage, pipelinesPage, metrics] = await Promise.all([
			apiCall('/api/v1/repositories?size=1'),
			apiCall('/api/v1/pipelines?size=1'),
			apiCall('/api/v1/deployments/metrics')
		]);
		
		elements.statRepos.textContent = reposPage.totalElements || 0;
		elements.statPipelines.textContent = pipelinesPage.totalElements || 0;
		elements.statSuccess.textContent = metrics.successfulDeployments || 0;
		elements.statFailed.textContent = metrics.failedDeployments || 0;
	} catch (err) {
		console.error("Failed to load dashboard statistics:", err);
	}
}

async function loadRecentDeployments() {
	try {
		const page = await apiCall('/api/v1/deployments?size=8');
		const deployments = page.content || [];
		
		if (deployments.length === 0) {
			elements.deploymentsListTable.innerHTML = `<tr><td colspan="5" class="table-empty">No deployments found. Trigger a pipeline to start.</td></tr>`;
			return;
		}

		// Fetch Pipeline details in parallel to draw pipeline names
		const pipelineIds = [...new Set(deployments.map(d => d.pipelineId))];
		const pipelineMap = {};
		await Promise.all(pipelineIds.map(async id => {
			try {
				const pipe = await apiCall(`/api/v1/pipelines/${id}`);
				pipelineMap[id] = pipe.pipelineName;
			} catch {
				pipelineMap[id] = 'Unknown Pipeline';
			}
		}));

		elements.deploymentsListTable.innerHTML = deployments.map(d => {
			const startedTime = d.startedAt ? new Date(d.startedAt).toLocaleString() : 'N/A';
			let statusClass = 'status-running';
			let statusIcon = 'fa-spinner fa-spin';
			
			if (d.status === 'SUCCESS') {
				statusClass = 'status-success';
				statusIcon = 'fa-circle-check';
			} else if (d.status === 'FAILED') {
				statusClass = 'status-failed';
				statusIcon = 'fa-circle-xmark';
			}

			return `
				<tr>
					<td><strong>${pipelineMap[d.pipelineId] || 'Pipeline'}</strong></td>
					<td><span class="stage-badge stage-build">${d.currentStage || 'QUEUE'}</span></td>
					<td>
						<span class="badge ${statusClass}">
							<i class="fa-solid ${statusIcon}"></i> ${d.status}
						</span>
					</td>
					<td>${startedTime}</td>
					<td>
						<button class="btn btn-secondary btn-sm" onclick="viewDeploymentLogs('${d.id}')">
							<i class="fa-solid fa-terminal"></i> Console Logs
						</button>
					</td>
				</tr>
			`;
		}).join('');
	} catch (err) {
		console.error("Failed to load recent deployments:", err);
	}
}

async function loadQuickTriggerPipelines() {
	try {
		const page = await apiCall('/api/v1/pipelines?size=100');
		const pipelines = page.content || [];
		
		elements.quickPipelineSelect.innerHTML = '<option value="" disabled selected>Choose a pipeline...</option>' + 
			pipelines.map(p => `<option value="${p.id}">${p.pipelineName}</option>`).join('');
	} catch (err) {
		console.error("Failed to load pipelines for quick trigger:", err);
	}
}

elements.quickTriggerForm.addEventListener('submit', async (e) => {
	e.preventDefault();
	const pipelineId = elements.quickPipelineSelect.value;
	if (!pipelineId) return;

	showLoading(true);
	try {
		const res = await apiCall(`/api/v1/pipelines/${pipelineId}/trigger`, 'POST');
		elements.quickTriggerForm.reset();
		loadRecentDeployments();
		
		// If trigger returns a deployment running, route directly to the execution logs console
		if (res.lastExecutionStartedAt || res.status) {
			// Find latest deployment run for this pipeline
			setTimeout(async () => {
				const page = await apiCall(`/api/v1/deployments?pipelineId=${pipelineId}&size=1`);
				if (page.content && page.content.length > 0) {
					viewDeploymentLogs(page.content[0].id);
				} else {
					showView('logs');
				}
			}, 600);
		}
	} catch (err) {
		alert(`Trigger failed: ${err.message}. (Note: Only ADMIN / RELEASE_MANAGER roles can trigger pipelines).`);
	} finally {
		showLoading(false);
	}
});

async function checkActuatorHealth() {
	try {
		const res = await apiCall('/actuator/health');
		if (res.status === 'UP') {
			elements.actuatorHealth.innerHTML = `<i class="fa-solid fa-circle-check"></i> System Healthy`;
			elements.actuatorHealth.className = 'health-badge status-success';
		} else {
			elements.actuatorHealth.innerHTML = `<i class="fa-solid fa-circle-xmark"></i> System Critical`;
			elements.actuatorHealth.className = 'health-badge status-failed';
		}
	} catch {
		elements.actuatorHealth.innerHTML = `<i class="fa-solid fa-triangle-exclamation"></i> Server Offline`;
		elements.actuatorHealth.className = 'health-badge status-failed';
	}
}

elements.refreshDeploysBtn.addEventListener('click', () => {
	loadRecentDeployments();
	loadDashboardMetrics();
});

elements.dashTriggerShortcut.addEventListener('click', () => {
	const select = elements.quickPipelineSelect;
	select.focus();
});


// --- 3. REPOSITORIES LOGIC ---
elements.openRepoModal.addEventListener('click', () => {
	elements.registerRepoCard.classList.remove('hidden');
});

elements.closeRepoCard.addEventListener('click', () => {
	elements.registerRepoCard.classList.add('hidden');
});

elements.cancelRegisterRepo.addEventListener('click', () => {
	elements.registerRepoCard.classList.add('hidden');
	elements.registerRepoForm.reset();
});

async function loadRepositoriesList() {
	showLoading(true);
	try {
		const page = await apiCall('/api/v1/repositories?size=100');
		const repos = page.content || [];
		
		if (repos.length === 0) {
			elements.reposGrid.innerHTML = `
				<div class="table-empty glass-panel" style="grid-column: 1/-1; width: 100%;">
					<i class="fa-solid fa-code-fork" style="font-size: 3rem; color: var(--text-muted); margin-bottom: 16px; display: block;"></i>
					<p>No repositories registered yet. Connect a GitHub repository to get started.</p>
				</div>
			`;
			return;
		}

		elements.reposGrid.innerHTML = repos.map(r => `
			<div class="repo-card glass-panel">
				<div class="repo-card-top">
					<h3>${r.repoName}</h3>
					<div class="repo-github-link">
						<i class="fa-brands fa-github"></i> ${r.githubUrl.replace('.git', '')}
					</div>
					<span class="repo-branch-tag">
						<i class="fa-solid fa-code-branch"></i> ${r.branch}
					</span>
				</div>
				<div class="repo-card-bottom">
					<span class="form-help">Created by: ${r.createdBy || 'Unknown'}</span>
					<button class="btn btn-danger btn-sm" onclick="deleteRepository('${r.id}')">
						<i class="fa-solid fa-trash-can"></i> Delete
					</button>
				</div>
			</div>
		`).join('');
	} catch (err) {
		console.error("Failed to load repositories:", err);
	} finally {
		showLoading(false);
	}
}

elements.registerRepoForm.addEventListener('submit', async (e) => {
	e.preventDefault();
	elements.repoError.classList.add('hidden');
	showLoading(true);

	const repoName = document.getElementById('repo-name').value;
	const githubUrl = document.getElementById('repo-url').value;
	const branch = document.getElementById('repo-branch').value;
	const accessToken = document.getElementById('repo-token').value;

	try {
		await apiCall('/api/v1/repositories', 'POST', { repoName, githubUrl, branch, accessToken });
		elements.registerRepoForm.reset();
		elements.registerRepoCard.classList.add('hidden');
		loadRepositoriesList();
	} catch (err) {
		elements.repoError.textContent = err.message || 'Failed to register repository.';
		elements.repoError.classList.remove('hidden');
	} finally {
		showLoading(false);
	}
});

window.deleteRepository = async function(id) {
	if (!confirm("Are you sure you want to delete this repository? All linked pipelines will be orphaned or deleted.")) return;
	
	showLoading(true);
	try {
		await apiCall(`/api/v1/repositories/${id}`, 'DELETE');
		loadRepositoriesList();
	} catch (err) {
		alert(`Delete failed: ${err.message}. (Note: Only ADMIN / RELEASE_MANAGER roles can delete repositories).`);
	} finally {
		showLoading(false);
	}
};


// --- 4. PIPELINES LOGIC ---
elements.openPipelineModal.addEventListener('click', () => {
	elements.createPipelineCard.classList.remove('hidden');
});

elements.closePipelineCard.addEventListener('click', () => {
	elements.createPipelineCard.classList.add('hidden');
});

elements.cancelCreatePipeline.addEventListener('click', () => {
	elements.createPipelineCard.classList.add('hidden');
	elements.createPipelineForm.reset();
});

async function loadRepositoriesDropdown() {
	try {
		const page = await apiCall('/api/v1/repositories?size=100');
		const repos = page.content || [];
		
		elements.pipelineRepoSelect.innerHTML = '<option value="" disabled selected>Choose a repository...</option>' +
			repos.map(r => `<option value="${r.id}">${r.repoName}</option>`).join('');
	} catch (err) {
		console.error("Failed to load repositories dropdown:", err);
	}
}

async function loadPipelinesList() {
	showLoading(true);
	try {
		const [pipelinesPage, reposPage] = await Promise.all([
			apiCall('/api/v1/pipelines?size=100'),
			apiCall('/api/v1/repositories?size=100')
		]);
		const pipelines = pipelinesPage.content || [];
		const repos = reposPage.content || [];
		
		const repoMap = {};
		repos.forEach(r => repoMap[r.id] = r.repoName);

		if (pipelines.length === 0) {
			elements.pipelinesGrid.innerHTML = `
				<div class="table-empty glass-panel" style="grid-column: 1/-1; width: 100%;">
					<i class="fa-solid fa-diagram-project" style="font-size: 3rem; color: var(--text-muted); margin-bottom: 16px; display: block;"></i>
					<p>No pipelines created yet. Define a build execution order to deploy your repository.</p>
				</div>
			`;
			return;
		}

		elements.pipelinesGrid.innerHTML = pipelines.map(p => {
			// Sort stages by execution order
			const stages = (p.stages || []).sort((a,b) => a.executionOrder - b.executionOrder);
			
			const stagesChainHtml = stages.map(s => {
				let cls = 'stage-build';
				if (s.stageName === 'TEST') cls = 'stage-test';
				if (s.stageName === 'DEPLOY') cls = 'stage-deploy';
				return `<span class="stage-badge ${cls}">${s.stageName}</span>`;
			}).join('<i class="fa-solid fa-chevron-right stage-arrow"></i>');

			return `
				<div class="pipeline-card glass-panel">
					<div class="pipeline-card-top">
						<h3>${p.pipelineName}</h3>
						<div class="pipeline-linked-repo">
							<i class="fa-solid fa-code-fork"></i> Repo: ${repoMap[p.repositoryId] || 'Unknown Repository'}
						</div>
						<div class="pipeline-stages-chain">
							${stagesChainHtml || '<span class="form-help">No stages configured</span>'}
						</div>
					</div>
					<div class="repo-card-bottom" style="margin-top: 10px;">
						<span class="form-help">Created by: ${p.createdBy || 'Unknown'}</span>
						<button class="btn btn-primary btn-sm" onclick="triggerPipelineDirect('${p.id}')">
							<i class="fa-solid fa-play"></i> Trigger Run
						</button>
					</div>
				</div>
			`;
		}).join('');
	} catch (err) {
		console.error("Failed to load pipelines:", err);
	} finally {
		showLoading(false);
	}
}

elements.createPipelineForm.addEventListener('submit', async (e) => {
	e.preventDefault();
	elements.pipelineError.classList.add('hidden');
	showLoading(true);

	const pipelineName = document.getElementById('pipeline-name').value;
	const repositoryId = elements.pipelineRepoSelect.value;
	
	// Construct stages array based on enabled checkboxes
	const stages = [];
	
	// BUILD stage (always enabled in the HTML form design)
	stages.push({
		stageName: "BUILD",
		executionOrder: parseInt(document.getElementById('stage-order-build').value)
	});

	// TEST stage
	if (document.getElementById('stage-enable-test').checked) {
		stages.push({
			stageName: "TEST",
			executionOrder: parseInt(document.getElementById('stage-order-test').value)
		});
	}

	// DEPLOY stage
	if (document.getElementById('stage-enable-deploy').checked) {
		stages.push({
			stageName: "DEPLOY",
			executionOrder: parseInt(document.getElementById('stage-order-deploy').value)
		});
	}

	try {
		await apiCall('/api/v1/pipelines', 'POST', { pipelineName, repositoryId, stages });
		elements.createPipelineForm.reset();
		elements.createPipelineCard.classList.add('hidden');
		loadPipelinesList();
	} catch (err) {
		elements.pipelineError.textContent = err.message || 'Failed to create pipeline.';
		elements.pipelineError.classList.remove('hidden');
	} finally {
		showLoading(false);
	}
});

window.triggerPipelineDirect = async function(id) {
	showLoading(true);
	try {
		await apiCall(`/api/v1/pipelines/${id}/trigger`, 'POST');
		// Small timeout to allow the background trigger worker to record the running deployment
		setTimeout(async () => {
			const page = await apiCall(`/api/v1/deployments?pipelineId=${id}&size=1`);
			if (page.content && page.content.length > 0) {
				viewDeploymentLogs(page.content[0].id);
			} else {
				showView('logs');
			}
		}, 600);
	} catch (err) {
		alert(`Trigger failed: ${err.message}. (Note: Only ADMIN / RELEASE_MANAGER roles can trigger pipelines).`);
	} finally {
		showLoading(false);
	}
};


// --- 5. LOGS & EXECUTIONS LOGIC ---
async function loadExecutionsHistory() {
	try {
		const page = await apiCall('/api/v1/deployments?size=50');
		const runs = page.content || [];
		
		if (runs.length === 0) {
			elements.executionList.innerHTML = `<li class="run-item-empty">No deployment runs recorded.</li>`;
			return;
		}

		// Fetch Pipeline details to show names
		const pipelineIds = [...new Set(runs.map(r => r.pipelineId))];
		const pipelineMap = {};
		await Promise.all(pipelineIds.map(async id => {
			try {
				const pipe = await apiCall(`/api/v1/pipelines/${id}`);
				pipelineMap[id] = pipe.pipelineName;
			} catch {
				pipelineMap[id] = 'Unknown Pipeline';
			}
		}));

		elements.executionList.innerHTML = runs.map(r => {
			const date = r.startedAt ? new Date(r.startedAt).toLocaleTimeString() : 'N/A';
			let icon = 'fa-spinner fa-spin text-info';
			if (r.status === 'SUCCESS') icon = 'fa-circle-check text-success';
			else if (r.status === 'FAILED') icon = 'fa-circle-xmark text-danger';

			const activeClass = (currentPollingRunId === r.id) ? 'active' : '';

			return `
				<li class="run-item ${activeClass}" onclick="selectDeploymentRun('${r.id}')" id="run-item-${r.id}">
					<div class="run-item-top">
						<span class="run-item-name">${pipelineMap[r.pipelineId] || 'Pipeline Run'}</span>
						<i class="fa-solid ${icon}"></i>
					</div>
					<div class="run-item-top">
						<span class="run-item-date">v${r.versionNumber} • ${date}</span>
						<span class="stage-badge stage-build" style="font-size: 0.65rem; padding: 1px 4px;">${r.status}</span>
					</div>
				</li>
			`;
		}).join('');
	} catch (err) {
		console.error("Failed to load executions history list:", err);
	}
}

window.viewDeploymentLogs = function(deploymentId) {
	currentPollingRunId = deploymentId;
	showView('logs');
	selectDeploymentRun(deploymentId);
};

window.selectDeploymentRun = function(id) {
	stopLogsPolling();
	currentPollingRunId = id;
	
	// Update active item styling in list
	document.querySelectorAll('.run-item').forEach(el => el.classList.remove('active'));
	const activeLi = document.getElementById(`run-item-${id}`);
	if (activeLi) activeLi.classList.add('active');

	elements.consoleRunId.textContent = `RUN ID: ${id}`;
	elements.consoleStagesProgress.classList.remove('hidden');
	elements.consoleTerminal.innerHTML = `<div class="terminal-line line-system">> Loading logs connection for run ${id}...</div>`;

	// Start Logs & State Polling loop
	pollLogsAndState();
	logsPollInterval = setInterval(pollLogsAndState, 1500);
};

function stopLogsPolling() {
	if (logsPollInterval) {
		clearInterval(logsPollInterval);
		logsPollInterval = null;
	}
}

async function pollLogsAndState() {
	if (!currentPollingRunId) {
		stopLogsPolling();
		return;
	}

	const runId = currentPollingRunId;
	
	try {
		// Fetch Deployment metadata and Logs in parallel
		const [deployment, logsPage] = await Promise.all([
			apiCall(`/api/v1/deployments/${runId}`),
			apiCall(`/api/v1/deployments/${runId}/logs?size=1000`)
		]);

		// Stop polling if another run was selected while this promise was fetching
		if (currentPollingRunId !== runId) return;

		// 1. Draw metadata details
		const pipe = await apiCall(`/api/v1/pipelines/${deployment.pipelineId}`);
		elements.consolePipelineName.textContent = pipe.pipelineName;
		
		const duration = deployment.durationMs ? `${(deployment.durationMs / 1000).toFixed(1)}s` : 'Running...';
		elements.consoleMetadata.innerHTML = `
			<div class="console-metadata-item"><i class="fa-solid fa-clock"></i> Duration: ${duration}</div>
			<div class="console-metadata-item"><i class="fa-solid fa-code-commit"></i> Commit: ${deployment.commitHash || 'webhook-push'}</div>
			<div class="console-metadata-item"><i class="fa-solid fa-arrow-up-1-9"></i> Version: v${deployment.versionNumber}</div>
			<div class="console-metadata-item"><i class="fa-solid fa-circle"></i> Status: ${deployment.status}</div>
		`;

		// Draw rollback button if deployment succeeded and is stable, but not a running deploy
		if (deployment.status === 'SUCCESS' && deployment.stable) {
			elements.consoleRollback.classList.remove('hidden');
			elements.consoleRollback.onclick = () => triggerRollback(pipe.id, runId);
		} else {
			elements.consoleRollback.classList.add('hidden');
		}

		// 2. Draw stages progress bar
		const sortedStages = (pipe.stages || []).sort((a,b) => a.executionOrder - b.executionOrder);
		elements.consoleStagesProgress.innerHTML = sortedStages.map(s => {
			let statusClass = ''; // empty, running, success, failed
			let iconHtml = s.executionOrder;
			
			if (s.status === 'RUNNING') {
				statusClass = 'running';
				iconHtml = '<i class="fa-solid fa-spinner fa-spin"></i>';
			} else if (s.status === 'SUCCESS') {
				statusClass = 'success';
				iconHtml = '<i class="fa-solid fa-check"></i>';
			} else if (s.status === 'FAILED') {
				statusClass = 'failed';
				iconHtml = '<i class="fa-solid fa-xmark"></i>';
			}

			return `
				<div class="stage-step ${statusClass}">
					<div class="stage-step-icon">${iconHtml}</div>
					<div class="stage-step-name">${s.stageName}</div>
				</div>
			`;
		}).join('');

		// 3. Draw logs console lines
		const logLines = logsPage.content || [];
		if (logLines.length === 0) {
			elements.consoleTerminal.innerHTML = `<div class="terminal-line line-system">> Waiting for process output to stream...</div>`;
		} else {
			elements.consoleTerminal.innerHTML = logLines.map(l => {
				let lineClass = 'line-info';
				if (l.logLevel === 'WARN') lineClass = 'line-warn';
				else if (l.logLevel === 'ERROR') lineClass = 'line-error';
				
				const timeStr = new Date(l.timestamp).toLocaleTimeString();
				return `<div class="terminal-line ${lineClass}">[${timeStr}] ${escapeHtml(l.message)}</div>`;
			}).join('');
		}

		// Auto-scroll logic
		if (autoscrollEnabled) {
			elements.consoleTerminal.scrollTop = elements.consoleTerminal.scrollHeight;
		}

		// Stop polling loop if compilation/execution is complete
		if (deployment.status === 'SUCCESS' || deployment.status === 'FAILED') {
			stopLogsPolling();

			// Re-fetch pipeline to get final stage statuses after cache eviction
			try {
				const finalPipe = await apiCall(`/api/v1/pipelines/${deployment.pipelineId}`);
				const finalStages = (finalPipe.stages || []).sort((a,b) => a.executionOrder - b.executionOrder);
				elements.consoleStagesProgress.innerHTML = finalStages.map(s => {
					let statusClass = '';
					let iconHtml = s.executionOrder;
					if (deployment.status === 'SUCCESS') {
						statusClass = 'success';
						iconHtml = '<i class="fa-solid fa-check"></i>';
					} else if (s.status === 'SUCCESS') {
						statusClass = 'success';
						iconHtml = '<i class="fa-solid fa-check"></i>';
					} else if (s.status === 'FAILED') {
						statusClass = 'failed';
						iconHtml = '<i class="fa-solid fa-xmark"></i>';
					}
					return `
						<div class="stage-step ${statusClass}">
							<div class="stage-step-icon">${iconHtml}</div>
							<div class="stage-step-name">${s.stageName}</div>
						</div>
					`;
				}).join('');
			} catch (finalErr) {
				console.error("Failed to re-fetch final pipeline state:", finalErr);
			}

			loadExecutionsHistory(); // Refresh run items list to show final status icon
		}

	} catch (err) {
		console.error("Error occurred during console logs polling:", err);
		elements.consoleTerminal.innerHTML += `<div class="terminal-line line-error">[SYSTEM ERROR] Failed to connect: ${err.message}</div>`;
		stopLogsPolling();
	}
}

async function triggerRollback(pipelineId, targetDeploymentId) {
	const reason = prompt("Please enter the reason for this rollback (optional):", "Rolling back to stable execution version");
	if (reason === null) return; // user cancelled

	showLoading(true);
	try {
		await apiCall('/api/v1/deployments/rollbacks', 'POST', {
			pipelineId,
			targetDeploymentId,
			reason
		});
		alert("Rollback trigger submitted successfully!");
		loadExecutionsHistory();
	} catch (err) {
		alert(`Rollback failed: ${err.message}. (Note: Only ADMIN / RELEASE_MANAGER roles can trigger rollbacks).`);
	} finally {
		showLoading(false);
	}
}

// Autoscroll toggle handler
elements.autoscrollToggle.addEventListener('click', () => {
	autoscrollEnabled = !autoscrollEnabled;
	if (autoscrollEnabled) {
		elements.autoscrollToggle.classList.add('active');
		elements.consoleTerminal.scrollTop = elements.consoleTerminal.scrollHeight;
	} else {
		elements.autoscrollToggle.classList.remove('active');
	}
});

// HTML escaping helper
function escapeHtml(text) {
	const map = {
		'&': '&amp;',
		'<': '&lt;',
		'>': '&gt;',
		'"': '&quot;',
		"'": '&#039;'
	};
	return text.replace(/[&<>"']/g, function(m) { return map[m]; });
}

elements.backToDash.addEventListener('click', () => {
	stopLogsPolling();
	showView('dashboard');
});


// --- INITIALIZATION ---
document.addEventListener('DOMContentLoaded', () => {
	// Sidebar navigation event routing
	document.querySelectorAll('.menu-item').forEach(item => {
		item.addEventListener('click', (e) => {
			e.preventDefault();
			const view = item.getAttribute('data-view');
			showView(view);
		});
	});

	// Check authentication state
	const session = getSession();
	if (session.token) {
		showView('dashboard');
	} else {
		showView('auth');
	}
});
