# WeaveTone v1.0.0-demo

这是面向作品集展示的首个可复现版本，重点验证 AI 音乐业务闭环、权限边界、数据一致性和容器化交付。

## 体验路径

1. 下载 `weavetone-v1.0.0-demo-debug.apk`，或从源码构建 Android Debug APK。
2. 克隆仓库，复制 `.env.docker.example` 为 `.env`。
3. 执行 `docker compose up --build -d --wait`。
4. 确认 `http://127.0.0.1:8080/ready` 返回 `UP`，再启动 Android 客户端。

默认启用 Fake Provider，无需模型账号即可演示注册、生成、试听、发布、点赞评论与歌单流程。

## 本版工程能力

- Docker Compose 编排 Ktor、PostgreSQL、健康检查与持久化卷。
- JWT 身份校验与 Track、Playlist、AI Job、播放历史的资源级权限控制。
- 使用联合主键、行锁和数据库事务保证关系唯一、计数一致与发布幂等。
- 19 个 Android/后端自动化用例，CI 额外拉起完整容器栈执行就绪探测。
- Tag 驱动 Release：自动测试、构建 APK 与后端分发包，并附 SHA-256 校验文件。

## Assets

- `weavetone-v1.0.0-demo-debug.apk`：Android 演示包，不用于应用商店发布。
- `weavetone-backend-v1.0.0-demo.zip`：Ktor 后端 JVM 分发包。
- `SHA256SUMS.txt`：发布资产完整性校验。

已知边界与生产化建议见 README 的“当前边界”和 [部署指南](DEPLOYMENT.md)。完整变更见 [CHANGELOG](../CHANGELOG.md)。
