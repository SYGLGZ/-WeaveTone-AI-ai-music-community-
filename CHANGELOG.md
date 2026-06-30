# Changelog

版本遵循 [Semantic Versioning](https://semver.org/)。

## [1.0.0-demo] - 2026-06-29

### Added

- Jetpack Compose Android 客户端与 Ktor REST API。
- MiniMax、Replicate、Fake 三种音乐生成 Provider 及持久化任务状态机。
- Docker Compose 一键启动后端与 PostgreSQL，包含健康检查和持久化卷。
- 19 个 Android/后端自动化用例与 Compose CI 烟雾测试。

### Security

- 增加私有歌单、播放历史、作品删除与 AI 任务的资源所有权校验。
- 音频上传限制为 50 MB 白名单格式，并使用 UUID 服务端文件名。
- API Key 仅从后端环境变量读取，Release APK 默认禁用明文网络。

### Fixed

- 使用事务、行锁和联合主键修复点赞计数漂移、重复关系与 AI 重复发布。
- 删除作品或歌单时同步清理关系数据，避免外键失败和孤儿记录。

[1.0.0-demo]: https://github.com/SYGLGZ/ai-music-community/releases/tag/v1.0.0-demo
