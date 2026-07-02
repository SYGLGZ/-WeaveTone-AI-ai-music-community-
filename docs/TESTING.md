# 自动化测试策略

当前共有 19 个单元/路由测试：后端 14 个，Android JVM 5 个。后端使用 Ktor Test Host 与独立 H2 内存数据库，不依赖第三方 AI 额度。

## 最近验证结果

2026-07-02 使用 JDK 17 与 Gradle 8.13 对当前代码完成本地验收：

- 后端：14 tests，0 failures，0 errors。
- Android JVM：5 tests，0 failures，0 errors。
- Android Debug APK：构建成功，23,618,720 bytes。
- APK SHA-256：`0E2FE14B66F1D600C1BEA55E312FC83717A1E8A4CD32199593D1C02FE2F81AA2`。
- GitHub Actions：Android、backend、docker-smoke 三个 Job 全部通过。

完整环境和运行证据见 [VERIFICATION.md](VERIFICATION.md)。

| 风险 | 自动化证据 |
| --- | --- |
| JWT 被篡改 | 拒绝修改 payload 的 Token |
| 私有歌单泄露 | 匿名用户和非所有者读取均返回 403 |
| 跨用户删改 | 非所有者无法删除作品、歌单或移除歌单曲目 |
| 重复关系 | 重复添加歌单曲目返回 409，联合主键数据库兜底 |
| 关注关系异常 | 关注不存在用户返回 404，关注/取关后断言关系表数量 |
| 冗余计数漂移 | 点赞/取消后同时断言关系数与 `likeCount` |
| AI 重复发布 | 首次返回 201，再次返回 200，Track 始终只有一条 |
| 删除孤儿数据 | 删除歌单后关联记录同步清零 |
| Provider 不可演示 | Fake Provider 生成 RIFF/WAV 并验证有效文件头 |
| 部署文档漂移 | CI 构建并拉起 Compose，访问 `/ready` 探活 |

本地执行：

```powershell
cd backend
.\gradlew.bat test
cd ..
.\gradlew.bat :app:testDebugUnitTest :app:assembleDebug
```

容器烟雾测试：

```powershell
docker compose up --build -d --wait
Invoke-RestMethod http://127.0.0.1:8080/ready
docker compose down
```
