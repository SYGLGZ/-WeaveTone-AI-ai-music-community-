# REST API 概览

默认服务地址：`http://localhost:8080`。需要登录的接口使用：

```http
Authorization: Bearer <jwt>
```

## 系统

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/` | 服务名称、版本与运行状态 |
| GET | `/health` | 轻量健康检查 |

## 认证

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/auth/register` | 注册并签发 JWT |
| POST | `/api/v1/auth/login` | 登录并签发 JWT |
| POST | `/api/v1/auth/verify` | 校验当前 Token |

## AI 生成

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/ai/generations` | 创建生成任务 |
| GET | `/api/v1/ai/generations/mine` | 当前用户的任务历史 |
| GET | `/api/v1/ai/generations/{id}` | 查询本人任务状态 |
| GET | `/api/v1/ai/generations/{id}/audio` | 流式读取已生成音频 |
| POST | `/api/v1/ai/generations/{id}/publish` | 发布为社区曲目 |

创建任务示例：

```json
{
  "prompt": "8bit 芯片音乐风格的 Boss 战，紧张而轻快",
  "genre": "电子",
  "bpm": 120,
  "durationSec": 30
}
```

## 曲目与社区

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/api/v1/music/discover` | 最新社区作品 |
| GET | `/api/v1/music/hot` | 热门作品 |
| GET | `/api/v1/music/search?q=` | 搜索曲目与用户 |
| POST | `/api/v1/music/upload/file` | Multipart 上传本地音频 |
| GET | `/api/v1/music/{id}` | 曲目详情 |
| GET | `/api/v1/music/{id}/stream` | 音频流 |
| POST | `/api/v1/music/{id}/like` | 切换点赞 |
| GET | `/api/v1/music/liked/mine` | 我的收藏 |
| GET | `/api/v1/music/{id}/comments` | 评论列表 |
| POST | `/api/v1/music/{id}/comment` | 发布评论 |

## 歌单

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/api/v1/playlist` | 创建歌单 |
| GET | `/api/v1/playlist/mine` | 我的歌单 |
| GET | `/api/v1/playlist/{id}` | 歌单详情 |
| GET | `/api/v1/playlist/{id}/tracks` | 歌单曲目 |
| POST | `/api/v1/playlist/{id}/add` | 添加曲目 |
| POST | `/api/v1/playlist/{id}/remove` | 移除曲目 |
| DELETE | `/api/v1/playlist/{id}` | 删除本人歌单 |

错误响应统一使用：

```json
{ "error": "human-readable message" }
```
