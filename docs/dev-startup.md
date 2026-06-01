# 开发环境启动指南

## 环境要求

| 依赖 | 版本 |
|------|------|
| JDK | 17+ |
| Maven | 系统安装 |
| Node.js | 18+ |
| npm | 随 Node.js |

## 启动步骤

### 1. 启动后端 `:8080`

```bash
cd c-mulagent-server
mvn spring-boot:run
```

- Spring Boot 3.3.0 + WebFlux（响应式）
- SQLite 数据库，自动在 `data/cmulagent.db` 创建

### 2. 启动前端 `:3000`

```bash
cd c-mulagent-ui
npm install              # 首次运行
npm run dev              # 后续启动
```

- Vite 5 + React 18 + TypeScript 5.4
- `/api` 代理到 `http://localhost:8080`
- `/ws` 代理到 `ws://localhost:8080`

### 3. 访问

打开 `http://localhost:3000`

## LLM API Key 配置

Agent 的 `apiKey`、`baseUrl`、`model` 通过前端 UI（创建/编辑 Agent 时）配置，存储在 SQLite 中。支持 Anthropic / OpenAI 兼容 / Ollama 三种后端。