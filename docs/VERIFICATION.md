# 工程验收报告

验收日期：2026-07-02

验收对象：`main` 分支，工程验收提交 `a88bdc6`。

## 结论

面向简历项目设定的四项硬指标均已获得可复现证据：Docker 一键部署、19 项关键业务测试、后端权限与一致性、GitHub Release 级交付。

## 1. Docker 一键部署

验证环境：Docker Desktop 4.80.0、Docker Engine 29.6.1、Docker Compose 5.1.4、WSL 2。

验证命令：

```powershell
$env:BACKEND_PORT="18080" # 本机 8080 位于 Windows 保留端口段
docker compose up --build --detach --wait
docker compose ps
Invoke-RestMethod http://127.0.0.1:18080/ready
```

结果：

- 多阶段后端镜像构建成功，运行阶段使用 JRE 17 与非 root UID 10001。
- Docker 构建上下文由约 505 MB 优化为 3.22 KB。
- BuildKit 持久缓存 Gradle 依赖，弱网重试后冷构建成功；缓存重建约 10 秒。
- `postgres` 与 `backend` 均为 `healthy`。
- `/ready` 返回 `status=UP`。
- 创建验收用户后执行 `docker compose restart`，再次登录成功，证明 PostgreSQL 数据卷生效。

## 2. 关键业务测试

使用完整 JDK 17 与 Gradle 8.13 本地执行：

| 测试层 | 数量 | Failures | Errors | 结果 |
| --- | ---: | ---: | ---: | --- |
| Ktor/后端 | 14 | 0 | 0 | 通过 |
| Android JVM | 5 | 0 | 0 | 通过 |
| 合计 | 19 | 0 | 0 | 通过 |

Debug APK 构建成功：

- 文件：`app/build/outputs/apk/debug/app-debug.apk`
- 大小：23,618,720 bytes
- SHA-256：`0E2FE14B66F1D600C1BEA55E312FC83717A1E8A4CD32199593D1C02FE2F81AA2`

## 3. 权限与一致性

自动化回归覆盖：

- JWT payload 篡改拒绝。
- 私有歌单对匿名用户和非所有者返回 403。
- 非所有者不能删除作品、歌单或移除歌单曲目。
- 播放历史仅本人可读。
- 重复歌单关系返回 409，联合主键数据库兜底。
- 点赞/取消后关系表数量与 `likeCount` 同步。
- 关注不存在用户返回 404；关注/取关保持关系一致。
- AI 生成任务重复发布只创建一个 Track。
- 删除歌单/作品同步清理关联记录。

实现使用资源所有权校验、联合主键、数据库行锁与事务，而非仅依赖客户端隐藏按钮。

## 4. GitHub Release 级交付

GitHub Actions Run：<https://github.com/SYGLGZ/-WeaveTone-AI-ai-music-community-/actions/runs/28560192490>

该 Run 的三个 Job 均成功：

- `android`：success
- `backend`：success
- `docker-smoke`：success

GitHub API 已确认以下 Workflow 为 `active`：

- `.github/workflows/ci.yml`
- `.github/workflows/release.yml`

本地执行 `distZip` 成功生成 `backend/build/distributions/ai-music-backend.zip`。Release Workflow 会在仓库所有者推送 `v1.0.0-demo` Tag 后重新测试、构建 APK/后端包、验证 Docker 镜像、生成 SHA-256 并创建 GitHub Release。

## 保留边界

- 当前是单体 Ktor 服务，不宣称微服务或高并发集群。
- Fake Provider 用于离线演示和测试，不宣称自研音乐生成模型。
- 本地音频卷适合演示，生产环境仍应迁移到对象存储/CDN。
- Release Tag、演示视频和 GitHub Private Vulnerability Reporting 需要仓库所有者在公开发布时确认。
