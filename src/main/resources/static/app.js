const state = {currentView: "analyzer", inputMode: "text", history: [], audits: []};

const elements = {
    form: document.querySelector("#analysis-form"),
    serviceName: document.querySelector("#service-name"),
    environment: document.querySelector("#environment"),
    logText: document.querySelector("#log-text"),
    logFile: document.querySelector("#log-file"),
    textInputArea: document.querySelector("#text-input-area"),
    fileInputArea: document.querySelector("#file-input-area"),
    dropZone: document.querySelector("#drop-zone"),
    fileName: document.querySelector("#file-name"),
    characterCount: document.querySelector("#character-count"),
    analyzeButton: document.querySelector("#analyze-button"),
    resultEmpty: document.querySelector("#result-empty"),
    resultLoading: document.querySelector("#result-loading"),
    resultContent: document.querySelector("#result-content"),
    resultMeta: document.querySelector("#result-meta"),
    severityBadge: document.querySelector("#severity-badge"),
    historyBody: document.querySelector("#history-body"),
    historyEmpty: document.querySelector("#history-empty"),
    historyCount: document.querySelector("#history-count"),
    auditBody: document.querySelector("#audit-body"),
    auditEmpty: document.querySelector("#audit-empty"),
    auditCount: document.querySelector("#audit-count"),
    refreshButton: document.querySelector("#refresh-button"),
    pageTitle: document.querySelector("#page-title"),
    toast: document.querySelector("#toast")
};

document.addEventListener("DOMContentLoaded", function () {
    initializeIcons();
    bindNavigation();
    bindInputMode();
    bindForm();
    bindFileDrop();
    bindActions();
    checkHealth();
});

function initializeIcons() {
    if (window.lucide) window.lucide.createIcons({attrs: {"stroke-width": 1.8}});
}

function bindNavigation() {
    document.querySelectorAll(".nav-item").forEach(function (button) {
        button.addEventListener("click", function () { showView(button.dataset.view); });
    });
}

function showView(view) {
    state.currentView = view;
    document.querySelectorAll(".nav-item").forEach(function (button) {
        button.classList.toggle("active", button.dataset.view === view);
    });
    document.querySelectorAll(".view").forEach(function (section) {
        section.classList.toggle("active", section.id === "view-" + view);
    });
    elements.pageTitle.textContent = {analyzer: "日志分析", history: "分析历史", audits: "模型审计"}[view];
    if (view === "history") loadHistory();
    if (view === "audits") loadAudits();
}

function bindInputMode() {
    document.querySelectorAll("[data-input-mode]").forEach(function (button) {
        button.addEventListener("click", function () {
            state.inputMode = button.dataset.inputMode;
            document.querySelectorAll("[data-input-mode]").forEach(function (item) {
                item.classList.toggle("active", item === button);
            });
            elements.textInputArea.hidden = state.inputMode !== "text";
            elements.fileInputArea.hidden = state.inputMode !== "file";
        });
    });
}

function bindForm() {
    elements.logText.addEventListener("input", function () {
        elements.characterCount.textContent = elements.logText.value.length.toLocaleString() + " 字符";
    });
    elements.form.addEventListener("submit", function (event) {
        event.preventDefault();
        analyze();
    });
}

function bindActions() {
    document.querySelector("#clear-log").addEventListener("click", function () {
        elements.logText.value = "";
        elements.characterCount.textContent = "0 字符";
        elements.logText.focus();
    });
    elements.refreshButton.addEventListener("click", async function () {
        elements.refreshButton.classList.add("spinning");
        try {
            if (state.currentView === "history") await loadHistory();
            else if (state.currentView === "audits") await loadAudits();
            else await checkHealth();
        } finally {
            window.setTimeout(function () { elements.refreshButton.classList.remove("spinning"); }, 250);
        }
    });
}

function bindFileDrop() {
    elements.logFile.addEventListener("change", updateSelectedFile);
    ["dragenter", "dragover"].forEach(function (name) {
        elements.dropZone.addEventListener(name, function (event) {
            event.preventDefault();
            elements.dropZone.classList.add("dragover");
        });
    });
    ["dragleave", "drop"].forEach(function (name) {
        elements.dropZone.addEventListener(name, function (event) {
            event.preventDefault();
            elements.dropZone.classList.remove("dragover");
        });
    });
    elements.dropZone.addEventListener("drop", function (event) {
        if (event.dataTransfer.files.length) {
            elements.logFile.files = event.dataTransfer.files;
            updateSelectedFile();
        }
    });
}

function updateSelectedFile() {
    const file = elements.logFile.files[0];
    elements.fileName.textContent = file ? file.name + " · " + formatBytes(file.size) : "选择日志文件";
}

async function analyze() {
    const serviceName = elements.serviceName.value.trim();
    const environment = elements.environment.value;
    let request;

    if (state.inputMode === "text") {
        const logText = elements.logText.value;
        if (!logText.trim()) return showToast("请输入需要分析的日志");
        request = fetch("/api/analyze-log", {
            method: "POST",
            headers: {"Content-Type": "application/json"},
            body: JSON.stringify({serviceName: serviceName, environment: environment, logText: logText})
        });
    } else {
        const file = elements.logFile.files[0];
        if (!file) return showToast("请选择日志文件");
        const data = new FormData();
        data.append("file", file);
        data.append("serviceName", serviceName);
        data.append("environment", environment);
        request = fetch("/api/analyze-log-file", {method: "POST", body: data});
    }

    setLoading(true);
    try {
        renderResult(await readResponse(await request));
    } catch (error) {
        showToast(error.message || "分析失败");
        showEmptyResult();
    } finally {
        setLoading(false);
    }
}

function setLoading(loading) {
    elements.analyzeButton.disabled = loading;
    elements.analyzeButton.querySelector("span").textContent = loading ? "分析中" : "开始分析";
    if (loading) {
        elements.resultEmpty.hidden = true;
        elements.resultContent.hidden = true;
        elements.resultLoading.hidden = false;
        elements.resultMeta.textContent = "模型调用中";
        setSeverity("neutral", "分析中");
    }
}

function showEmptyResult() {
    elements.resultLoading.hidden = true;
    elements.resultContent.hidden = true;
    elements.resultEmpty.hidden = false;
    elements.resultMeta.textContent = "等待提交";
    setSeverity("neutral", "待分析");
}

function renderResult(record) {
    const result = record.result;
    elements.resultLoading.hidden = true;
    elements.resultEmpty.hidden = true;
    elements.resultContent.hidden = false;
    elements.resultMeta.textContent = sourceLabel(result.analysisSource) + " · " + (result.modelName || "规则引擎") + " · " + formatDate(record.createdAt);
    setSeverity(result.severity, result.severity);
    document.querySelector("#result-summary").textContent = result.summary;
    document.querySelector("#confidence-value").textContent = Math.round(result.confidence * 100) + "%";
    document.querySelector("#impact-scope").textContent = impactLabel(result.impactScope);
    document.querySelector("#signal-count").textContent = result.errorCount + " / " + result.warningCount;
    renderList("#issues-list", result.detectedIssues);
    renderList("#causes-list", result.possibleRootCauses);
    renderList("#actions-list", result.recommendedActions);
    renderList("#steps-list", result.investigationSteps);
}

function renderList(selector, items) {
    document.querySelector(selector).innerHTML = (items || []).map(function (item) {
        return "<li>" + escapeHtml(item) + "</li>";
    }).join("");
}

function setSeverity(severity, label) {
    elements.severityBadge.className = "severity-badge " + severity;
    elements.severityBadge.textContent = label;
}

async function loadHistory() {
    try {
        state.history = await fetchJson("/api/analysis-history");
        elements.historyCount.textContent = state.history.length + " 条记录";
        elements.historyEmpty.hidden = state.history.length !== 0;
        elements.historyBody.innerHTML = state.history.map(historyRow).join("");
        elements.historyBody.querySelectorAll("[data-history-id]").forEach(function (button) {
            button.addEventListener("click", function () {
                const record = state.history.find(function (item) { return item.id === button.dataset.historyId; });
                if (record) {
                    renderResult(record);
                    showView("analyzer");
                }
            });
        });
        initializeIcons();
    } catch (error) {
        showToast(error.message || "历史记录加载失败");
    }
}

function historyRow(record) {
    return "<tr>" +
        "<td class=\"muted-cell\">" + escapeHtml(formatDate(record.createdAt)) + "</td>" +
        "<td>" + escapeHtml(record.serviceName) + "</td>" +
        "<td>" + escapeHtml(environmentLabel(record.environment)) + "</td>" +
        "<td><span class=\"severity-badge " + escapeHtml(record.result.severity) + "\">" + escapeHtml(record.result.severity) + "</span></td>" +
        "<td>" + escapeHtml(sourceLabel(record.result.analysisSource)) + "</td>" +
        "<td class=\"summary-cell\" title=\"" + escapeHtml(record.result.summary) + "\">" + escapeHtml(record.result.summary) + "</td>" +
        "<td><button class=\"row-action\" type=\"button\" data-history-id=\"" + escapeHtml(record.id) + "\" title=\"查看分析\" aria-label=\"查看分析\"><i data-lucide=\"arrow-up-right\"></i></button></td>" +
        "</tr>";
}

async function loadAudits() {
    try {
        state.audits = await fetchJson("/api/model-call-audits");
        elements.auditCount.textContent = state.audits.length + " 条记录";
        elements.auditEmpty.hidden = state.audits.length !== 0;
        elements.auditBody.innerHTML = state.audits.map(auditRow).join("");
    } catch (error) {
        showToast(error.message || "模型审计加载失败");
    }
}

function auditRow(audit) {
    const statusClass = audit.status === "SUCCESS" ? "success" : "failed";
    const error = audit.errorSummary || "—";
    return "<tr>" +
        "<td class=\"muted-cell\">" + escapeHtml(formatDate(audit.createdAt)) + "</td>" +
        "<td>" + escapeHtml(audit.serviceName) + "</td>" +
        "<td>" + escapeHtml(environmentLabel(audit.environment)) + "</td>" +
        "<td>" + escapeHtml(audit.modelName) + "</td>" +
        "<td><span class=\"status-badge " + statusClass + "\">" + escapeHtml(audit.status) + "</span></td>" +
        "<td>" + Number(audit.durationMs).toLocaleString() + " ms</td>" +
        "<td class=\"summary-cell muted-cell\" title=\"" + escapeHtml(error) + "\">" + escapeHtml(error) + "</td>" +
        "</tr>";
}

async function checkHealth() {
    const dot = document.querySelector("#sidebar-status-dot");
    const label = document.querySelector("#sidebar-status-text");
    try {
        const health = await fetchJson("/api/health");
        dot.className = "status-dot online";
        label.textContent = health.status === "UP" ? "服务正常" : health.status;
    } catch (error) {
        dot.className = "status-dot offline";
        label.textContent = "服务离线";
    }
}

async function fetchJson(url, options) {
    return readResponse(await fetch(url, options));
}

async function readResponse(response) {
    const contentType = response.headers.get("content-type") || "";
    const body = contentType.includes("application/json") ? await response.json() : await response.text();
    if (!response.ok) {
        const message = typeof body === "string" ? body : body.detail || body.message || body.error || "请求失败 (" + response.status + ")";
        throw new Error(message);
    }
    return body;
}

function sourceLabel(source) { return source === "llm" ? "AI 模型" : "规则引擎"; }
function impactLabel(scope) {
    return {cluster: "集群", dependency: "依赖服务", "single-service": "单服务", unknown: "待确认"}[scope] || scope || "待确认";
}
function environmentLabel(environment) {
    return {production: "生产", prod: "生产", staging: "预发布", test: "测试", development: "开发", dev: "开发", unknown: "未知"}[environment] || environment || "未知";
}
function formatDate(value) {
    if (!value) return "—";
    return new Intl.DateTimeFormat("zh-CN", {
        month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit", second: "2-digit", hour12: false
    }).format(new Date(value));
}
function formatBytes(bytes) {
    if (bytes < 1024) return bytes + " B";
    if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + " KB";
    return (bytes / 1024 / 1024).toFixed(1) + " MB";
}
function escapeHtml(value) {
    return String(value == null ? "" : value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll("\"", "&quot;")
        .replaceAll("'", "&#039;");
}

let toastTimer;
function showToast(message) {
    window.clearTimeout(toastTimer);
    elements.toast.textContent = message;
    elements.toast.classList.add("show");
    toastTimer = window.setTimeout(function () { elements.toast.classList.remove("show"); }, 4200);
}
