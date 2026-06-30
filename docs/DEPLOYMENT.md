# Docker 部署指南

## 快速启动

要求 Docker Engine 24+ 与 Docker Compose v2：

```powershell
Copy-Item .env.docker.example .env
docker compose up --build -d --wait
Invoke-RestMethod http://127.0.0.1:8080/ready
```

Compose 会启动 `postgres` 与 `backend`，数据库就绪后再启动 API。`postgres_data` 保存数据库，`audio_data` 保存上传和生成音频。默认 `AI_PROVIDER=fake`，可离线演示完整业务。

## 生产配置

部署前至少修改：

- `POSTGRES_PASSWORD`：高强度随机密码。
- `JWT_SECRET`：至少 32 位随机字符串，禁止沿用示例值。
- `BACKEND_PORT`：宿主机监听端口，默认 8080。
- `AI_PROVIDER` 及所选供应商密钥：仅在需要真实生成服务时设置。

`.env` 已被 Git 忽略。不要把密钥写入 Compose、APK、日志或 Git 历史。

## 运维命令

```powershell
docker compose ps
docker compose logs -f backend
docker compose restart backend
docker compose down
```

`docker compose down` 保留数据卷；只有确认要清空数据时才执行 `docker compose down -v`。升级前应备份 PostgreSQL 卷和音频卷。

## 健康检查

- `/health`：进程存活检查，不访问数据库。
- `/ready`：执行 `SELECT 1`，数据库不可用时返回 503。

镜像使用多阶段构建，运行阶段为 JRE 17，并以 UID 10001 的非 root 用户运行。GitHub Actions 会对每次提交执行 Compose 构建、启动和就绪探测。
