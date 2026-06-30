# 简历项目表述

## 项目名称

**织音 WeaveTone｜AI 音乐创作与社区平台**

## 一句话介绍

基于 Jetpack Compose 与 Ktor 构建的 Android 全栈音乐社区，支持自然语言生成音乐、任务恢复、试听发布、点赞评论、收藏歌单和本地音乐播放。

## 简历版（推荐）

- 使用 **Kotlin + Jetpack Compose + Ktor** 独立完成 Android 全栈 AI 音乐社区，覆盖登录、AI 生成、试听发布、点赞评论、歌单与后台播放等完整闭环。
- 设计 `MusicGenerationProvider` 策略接口，统一适配 **MiniMax、Replicate 与离线 Fake Provider**；用五态持久化任务模型支持轮询、失败记录和应用重进恢复，模型密钥仅保留在服务端。
- 基于 JWT 实现资源级鉴权，修复私有歌单泄露、跨用户删改与播放历史越权；利用联合主键、行锁和事务保证点赞计数、关系清理及 AI 发布幂等一致。
- 使用 **Docker Compose** 编排 Ktor、PostgreSQL、健康检查和持久化卷，实现一条命令部署；GitHub Actions 自动执行 Android/后端测试并拉起完整容器栈探活。
- 建立 **19 个自动化用例**，覆盖 Provider 音频输出、JWT 防篡改、歌单越权、点赞/关注一致性、AI 重复发布和 Android Repository/URL 逻辑，形成可复现测试矩阵。

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
