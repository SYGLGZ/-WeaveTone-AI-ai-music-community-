# GitHub 发布清单

## 已完成

- [x] README 包含项目定位、截图、技术栈、架构与运行步骤
- [x] Android 后端地址不再写死个人局域网 IP
- [x] Debug 允许本地 HTTP，Release 默认拒绝明文流量
- [x] API Key 仅由后端环境变量读取
- [x] H2 + Fake Provider 可离线演示完整生成闭环
- [x] Android Debug 构建和单元测试通过
- [x] 后端健康检查测试通过
- [x] GitHub Actions 同时验证 Android 与后端
- [x] Docker Compose 编排 PostgreSQL、后端、健康检查与持久化卷
- [x] CI 拉起完整 Compose 栈并验证 `/ready`
- [x] 关键权限与一致性路径具备自动化回归测试
- [x] API、架构、部署、测试和 CHANGELOG 文档齐全
- [x] Tag 触发 Release 工作流，产出 APK、后端分发包与 SHA-256 校验文件
- [x] 2026-07-02 本地验证 19/19 测试、Debug APK、Docker 构建与重启持久化
- [x] GitHub CI 的 Android、backend、docker-smoke 三个 Job 全绿
- [x] GitHub 已识别 CI 与 Release 两个 Workflow 为 active

## 发布前由仓库所有者确认

- [x] 使用 MIT 开源许可证
- [ ] 将仓库重命名为品牌名称，例如 `weavetone`
- [ ] 在 GitHub 开启 Private Vulnerability Reporting
- [ ] 上传 60–90 秒演示视频或 GIF，并放到 README 顶部
- [ ] 推送 `v1.0.0-demo` Tag，由 Release 工作流生成 GitHub Release
- [x] 使用常见 MiniMax/Replicate/OpenAI/AWS Token 特征扫描当前文件与 Git 历史，未发现真实密钥
- [ ] 把 `docs/RESUME.md` 中的项目描述加入简历并补充可量化结果
