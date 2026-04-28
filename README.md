# AI Auto Code

基于 AI 大模型的智能代码生成平台，支持通过自然语言描述自动生成并部署 Web 应用。

## 功能特性

- **智能代码生成**：输入自然语言需求，AI 自动选择生成类型并生成完整的前端代码
- **多种生成类型**：
  - `HTML`：生成单文件 HTML 应用
  - `MULTI_FILE`：生成多文件（HTML/CSS/JS）项目
  - `VUE_PROJECT`：生成完整的 Vue 3 项目（通过 AI Tool Call + LangGraph4j 工作流）
- **流式输出**：通过 SSE（Server-Sent Events）实时推送生成过程
- **一键部署**：将生成的代码部署到服务器，自动生成访问链接及封面截图
- **图片支持**：支持上传图片（存储至阿里云 OSS），AI 可参考图片修改页面
- **对话历史**：记录每个应用的完整对话上下文，支持多轮修改
- **AI 记忆压缩**：对话消息超过阈值时自动触发摘要压缩（`SummarizingChatMemory`）
- **安全护轨**：
  - 输入护轨（`PromptSafetyInputGuardrail`）：拦截提示词注入攻击
  - 输出护轨（`RetryOutputGuardrail`）：检测空响应、代码块不闭合、凭据泄露等异常，自动重试

## 技术栈

| 层次 | 技术 |
|------|------|
| 后端框架 | Spring Boot 3.5.4 / Java 21 |
| AI 框架 | LangChain4j 1.1.0 |
| AI 工作流 | LangGraph4j 1.6.0-rc2 |
| 大模型 | DeepSeek Chat / DeepSeek Reasoner / GLM-4.6v |
| 数据库 | MySQL 8 + MyBatis-Flex 1.11.0 |
| 缓存 | Redis（对话记忆持久化）+ Caffeine（本地缓存）|
| 对象存储 | 阿里云 OSS |
| 图片生成 | 阿里云 DashScope（通义万象）|
| 截图 | Selenium 4 + WebDriverManager |
| 安全 | Spring Security |
| API 文档 | Knife4j（OpenAPI 3）|

## 快速开始

### 环境要求

- JDK 21+
- Maven 3.8+
- MySQL 8.0+
- Redis 6.0+
- Node.js 18+（Vue 项目构建所需）
- Chrome 浏览器（自动截图所需）

### 1. 初始化数据库

```sql
-- 执行 sql/create_table.sql
source sql/create_table.sql
```

### 2. 配置文件

在 `src/main/resources/application-local.yml` 中填写以下配置：

```yaml
langchain4j:
  open-ai:
    chat-model:
      base-url: https://api.deepseek.com
      api-key: <YOUR_DEEPSEEK_API_KEY>
      model-name: deepseek-chat
    streaming-chat-model:
      base-url: https://open.bigmodel.cn/api/paas/v4/
      api-key: <YOUR_GLM_API_KEY>
      model-name: glm-4.6v
    reasoning-streaming-chat-model:
      base-url: https://api.deepseek.com
      api-key: <YOUR_DEEPSEEK_API_KEY>
      model-name: deepseek-reasoner
    routing-chat-model:
      base-url: https://api.deepseek.com
      api-key: <YOUR_DEEPSEEK_API_KEY>
      model-name: deepseek-chat
    summarization-chat-model:
      base-url: https://api.deepseek.com
      api-key: <YOUR_DEEPSEEK_API_KEY>
      model-name: deepseek-chat

oss:
  client:
    endpoint: <OSS_ENDPOINT>
    accessKeyId: <ACCESS_KEY_ID>
    accessKeySecret: <ACCESS_KEY_SECRET>
    bucketName: <BUCKET_NAME>

pexels:
  api-key: <PEXELS_API_KEY>

dashscope:
  api-key: <DASHSCOPE_API_KEY>
```

`application.yaml` 中数据库和 Redis 默认配置：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_auto_code
    username: root
    password: <YOUR_MYSQL_PASSWORD>
  data:
    redis:
      host: localhost
      port: 6379
```

### 3. 启动服务

```bash
./mvnw spring-boot:run
```

服务启动后访问：
- 接口文档：http://localhost:8123/api/doc.html

## 项目结构

```
src/main/java/org/lim/aiautocode/
├── ai/
│   ├── enums/          # 代码生成类型枚举（HTML / MULTI_FILE / VUE_PROJECT）
│   ├── guardrail/      # AI 护轨（输入安全检测 + 输出质量检测）
│   ├── memory/         # 自定义对话记忆（SummarizingChatMemory）
│   ├── model/          # AI 响应模型（HtmlCodeResult / MultiFileCodeResult 等）
│   ├── services/       # LangChain4j AI Service 接口
│   └── tools/          # AI Tool Call 工具（文件读写、目录读取、删除）
├── config/             # Spring 配置（模型、安全、OSS、Redis 等）
├── controller/         # HTTP 接口（AppController / UserController 等）
├── core/
│   ├── AiCodeGeneratorFacade.java  # 代码生成统一门面
│   ├── builder/        # Vue 项目构建器
│   ├── handler/        # 流式处理执行器
│   ├── parse/          # 代码解析器
│   └── saver/          # 代码文件保存器
├── langgraph4j/        # LangGraph4j 工作流
│   └── node/           # 工作流节点（图片收集、提示词增强、路由、代码生成、质量检查、项目构建）
├── manager/            # OSS 文件管理
├── model/              # 业务模型（entity / dto / vo）
└── service/            # 业务服务层
```

## 核心工作流（Vue 项目）

```
用户输入
  ↓
ImageCollectorNode    → 并发搜集所需图片资源
  ↓
PromptEnhancerNode    → 拼接提示词（图片 + 需求）
  ↓
RouterNode            → 智能路由选择生成策略
  ↓
CodeGeneratorNode     → DeepSeek Reasoner 生成代码（Tool Call 写文件）
  ↓
CodeQualityCheckNode  → 检查生成代码质量
  ↓
ProjectBuilderNode    → npm install && npm run build
```

## API 概览

| 方法 | 路径 | 描述 |
|------|------|------|
| GET  | `/api/app/chat/gen/code` | 流式生成代码（SSE）|
| POST | `/api/app/add` | 创建应用 |
| POST | `/api/app/deploy` | 部署应用 |
| GET  | `/api/app/download/{appId}` | 下载应用代码（ZIP）|
| POST | `/api/app/upload/image` | 上传图片至 OSS |
| GET  | `/api/app/get/vo` | 获取应用详情 |
| POST | `/api/app/my/list/page/vo` | 分页获取我的应用 |
| POST | `/api/app/good/list/page/vo` | 分页获取精选应用 |

## 注意事项

- `application-local.yml` 包含密钥信息，已在 `.gitignore` 中忽略，**切勿提交到版本库**
- Vue 项目部署需要服务器安装 Node.js，构建产物默认输出到 `dist/` 目录
- 自动截图依赖 Chrome 浏览器，WebDriverManager 会自动下载对应 ChromeDriver