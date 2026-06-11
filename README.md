# Table-backend

课表图片识别项目，前端使用原生 `HTML/CSS/JS`，后端使用 `Spring Boot + LangChain4j` 调用 OpenAI 兼容多模态模型。

## Quick Start

1. 准备环境

- `JDK 21`
- 可访问目标 AI 服务的 OpenAI 兼容接口
- 一个有效的 `AI_API_KEY`

2. 配置 env file

项目运行时从环境变量读取配置。推荐在 IDEA 的 EnvFile 插件里加载项目根目录的 `.env`。

仓库中提供可提交的模板文件：

- [`.env.example`](/D:/javaee/Table/demo1/.env.example)

推荐变量如下：

```env
AI_API_KEY=your-api-key-here
AI_BASE_URL=https://token-plan-cn.xiaomimimo.com/v1
AI_MODEL=mimo-v2.5
AI_TEMPERATURE=0.0
AI_TIMEOUT_SECONDS=180
AI_MAX_RETRIES=1
AI_MAX_IMAGE_SIZE_MB=5
```

3. 启动项目

```powershell
.\mvnw.cmd spring-boot:run
```

4. 访问页面

```text
http://localhost:8080/
```

5. 检查运行配置

```text
http://localhost:8080/api/ai/schedule/config
```

正常应看到：

- `model = mimo-v2.5`
- `baseUrl = https://token-plan-cn.xiaomimimo.com/v1`
- `apiKeyConfigured = true`

## Docs

完整开发文档见：

- [课表开发文档v2.md](/D:/javaee/Table/demo1/课表开发文档v2.md)
