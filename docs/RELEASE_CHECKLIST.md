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

## 发布前由仓库所有者确认

- [ ] 选择开源许可证（个人作品集通常可选 MIT）
- [ ] 将仓库重命名为清晰名称，例如 `ai-music-community`
- [ ] 在 GitHub 开启 Private Vulnerability Reporting
- [ ] 上传 60–90 秒演示视频或 GIF，并放到 README 顶部
- [ ] 创建 `v1.0.0-demo` Release，附 Debug APK 和演示账号说明
- [ ] 确认 Git 历史中从未出现真实 API Key；如出现过则先吊销密钥
- [ ] 把 `docs/RESUME.md` 中的项目描述加入简历并补充可量化结果
