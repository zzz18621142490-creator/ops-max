# AI Operations Assistant

一个基于 Java 21 和 Spring Boot 的本地 AIOps 日志分析工具。它支持调用 OpenAI 兼容模型分析日志；模型不可用时会自动回退到本地规则引擎。

## 当前能力

- 分析粘贴的日志文本或上传的日志文件
- 输出严重等级、故障摘要、可能原因、影响范围和处置建议
- 对 API Key、Bearer Token、密码等敏感内容进行脱敏
- 将分析历史保存到本地 H2 数据库
- 记录模型调用状态、耗时和错误摘要，不在审计表中保存原始日志
- 提供日志分析、分析历史和模型审计三个中文界面

## 环境要求

- JDK 21
- Maven 3.9+
- 可选：支持 OpenAI Responses API 的模型服务及 API Key

验证环境：Spring Boot 3.5.4、Java 21.0.12、Maven 3.9.16。

## 配置模型

在 PowerShell 中运行：

```powershell
.\scripts\configure-ai.ps1 -BaseUrl "https://api.deepseek.com/"
```

脚本会以隐藏输入方式读取 API Key，并把以下配置保存到当前 Windows 用户的环境变量中：

- `AI_MODEL_ENABLED`
- `AI_MODEL_BASE_URL`
- `AI_MODEL_API_KEY`
- `AI_MODEL_NAME`
- `AI_MODEL_REASONING_EFFORT`
- `AI_MODEL_STORE_RESPONSES`

配置后需要重新打开终端。不要把 API Key 写入源码、README 或提交到 Git。

不配置模型也可以启动。默认情况下 `AI_MODEL_ENABLED=false`，分析请求会使用规则引擎。

## 本地启动

```powershell
mvn spring-boot:run
```

启动完成后访问：

- 应用界面：http://127.0.0.1:8080/
- 健康检查：http://127.0.0.1:8080/api/health
- H2 控制台：http://127.0.0.1:8080/h2-console

本地数据库文件保存在 `data/`，该目录不会纳入 Git。

## 运行测试

```powershell
mvn test
```

当前测试覆盖应用启动、分析接口、历史记录、文件上传、日志脱敏和模型响应解析。

## API

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `GET` | `/api/health` | 健康检查 |
| `POST` | `/api/analyze-log` | 分析 JSON 格式的日志文本 |
| `POST` | `/api/analyze-log-file` | 上传并分析日志文件 |
| `GET` | `/api/analysis-history` | 查询分析历史 |
| `GET` | `/api/analysis-history/{id}` | 查询单条分析记录 |
| `GET` | `/api/model-call-audits` | 查询模型调用审计 |

文本分析请求示例：

```json
{
  "serviceName": "payment-api",
  "environment": "staging",
  "logText": "ERROR inventory-service request timed out"
}
```

## 配置项

主要配置位于 `src/main/resources/application.properties`。生产部署时至少应通过环境变量覆盖模型配置、数据库连接和上传大小限制。

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `AI_MODEL_ENABLED` | `false` | 是否启用模型分析 |
| `AI_MODEL_BASE_URL` | `https://api.deepseek.com/` | 模型服务地址 |
| `AI_MODEL_NAME` | `deepseek-v4-flash` | 模型名称 |
| `AI_MODEL_REASONING_EFFORT` | `medium` | 推理强度 |
| `AI_MODEL_STORE_RESPONSES` | `false` | 是否允许模型服务保存响应 |
| `AI_MODEL_CONNECT_TIMEOUT` | `5s` | 连接超时 |
| `AI_MODEL_READ_TIMEOUT` | `120s` | 响应超时 |
| `AI_MODEL_MAX_INPUT_CHARS` | `20000` | 发送给模型的最大字符数 |
| `AI_LOG_UPLOAD_MAX_BYTES` | `5242880` | 日志上传大小上限 |

## 当前边界

这是可用于本地演示和内部验证的 MVP。正式部署前还需要补充身份认证与授权、分页、数据库迁移、生产数据库、监控告警、容器化和端到端测试。
