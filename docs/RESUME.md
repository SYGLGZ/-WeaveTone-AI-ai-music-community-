# 简历项目表述

## 项目名称

**AI Music｜AI 音乐创作与社区平台**

## 一句话介绍

基于 Jetpack Compose 与 Ktor 构建的 Android 全栈音乐社区，支持自然语言生成音乐、任务恢复、试听发布、点赞评论、收藏歌单和本地音乐播放。

## 简历版（推荐）

- 使用 **Kotlin + Jetpack Compose + Ktor** 独立完成 Android 全栈 AI 音乐社区，覆盖登录、生成、试听、发布、点赞评论、收藏与歌单等核心闭环。
- 设计 `MusicGenerationProvider` 策略接口，统一适配 **MiniMax Music、Replicate MusicGen 与离线 Fake Provider**，将供应商切换从业务路由中解耦。
- 设计持久化 AI 任务模型与 `PENDING/RUNNING/SUCCEEDED/FAILED/PUBLISHED` 状态机，实现轮询进度、失败可观测、应用重进恢复及生成音频服务端落盘。
- 将第三方 API Key 收口至 Ktor 环境变量，Android 仅持有 JWT 并访问自有后端；结合 H2 演示模式，使项目无需外部账号即可本地运行。
- 基于 **Media3 + MediaSessionService** 实现后台播放，使用 **Room/MediaStore** 管理本地歌曲，并通过 Retrofit、Hilt、Coroutines 构建分层数据流。

## 技术关键词

`Kotlin` `Jetpack Compose` `Ktor` `Exposed` `PostgreSQL` `H2` `JWT` `Hilt` `Retrofit` `Room` `Media3` `Coroutines` `Provider Pattern`

## 面试时重点讲

1. 为什么不让 Android 直接调用模型 API：密钥安全、任务持久化、供应商替换和音频保存都需要服务端边界。
2. 为什么做任务状态机：真实生成耗时不确定，移动端会旋转、退后台和断网，一次同步请求无法保证体验。
3. Fake Provider 的价值：它不仅是占位，还让开发、CI、答辩和招聘者体验不依赖付费额度。
4. 当前最需要继续演进的地方：对象存储/CDN、Redis 限流、数据库迁移工具、通知与推荐系统。

## 避免夸大的表述

- 不写“自研音乐生成大模型”；项目完成的是生成服务工程化与产品闭环。
- 不写“微服务/高并发”；当前是适合课设和作品集的单体 Ktor 服务。
- 不写“已上架”；除非后续确实完成签名、隐私政策和应用商店审核。
