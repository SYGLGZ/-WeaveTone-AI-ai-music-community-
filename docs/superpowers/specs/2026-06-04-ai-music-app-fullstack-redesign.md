# AI音乐App 全栈重构设计文档

> **定位:** 以本地播放器为基础、用户上传/分享AI创作为核心的社区型音乐APP（简化版 SoundCloud）
> **架构:** 前端 Kotlin/Compose + 后端 Kotlin/Ktor + PostgreSQL
> **交付:** GitHub 开源 + 服务器部署上线

---

## 一、项目背景与定位

当前项目是一个可编译但没有完整功能的 AI 音乐 Demo。核心问题：
- 播放功能仅基础可用，无播放队列、无浏览体系
- 无后端支持，无法实现社区发现功能
- AI 生成框架完整但无真实 API 对接

### 目标用户画像
- 独立音乐创作者
- AI 音乐爱好者
- 寻找新鲜独立音乐的听众

### 成功标准
- APP 能像正常音乐播放器一样使用（播放队列、浏览、搜索、播放列表）
- 用户可以注册、上传音乐、发现他人作品
- AI 音乐生成功能可用并可直接发布到社区

---

## 二、技术栈

| 层 | 技术 | 说明 |
|---|------|------|
| Android UI | Jetpack Compose + Material3 | 主题统一、底部导航 |
| DI | Hilt | 现有不变 |
| 本地数据库 | Room | 现有，补充迁移 |
| 播放引擎 | Media3 ExoPlayer + MediaSession | 现有，增强队列播放 |
| 图片加载 | Coil | 现有但未使用 |
| 后端框架 | Kotlin + Ktor 3.x | 轻量异步 |
| 数据库 | PostgreSQL + Exposed ORM | 类型安全SQL |
| 认证 | JWT + bcrypt | 无状态认证 |
| 文件存储 | 本地磁盘 + Nginx 静态代理 | 简单可靠 |
| 部署 | 阿里云轻量服务器 + Docker | 低成本 |

---

## 三、Android 前端架构

### 3.1 导航架构（新增底部导航）

```
┌─────────────────────┐
│     Bottom Nav       │
│ 发现 │ 音乐库 │ 创作 │ 我的  │
└─────────────────────┘
     │       │       │      │
     ▼       ▼       ▼      ▼
  发现页    本地音乐   AI生成  个人中心
```

### 3.2 底层导航结构

```
AIMusicNavHost (根)
├── MainScaffold (带 BottomBar)
│   ├── HomeScreen (发现/推荐)
│   ├── LibraryScreen (本地音乐)
│   │   ├── 歌曲列表 (Tab: 歌曲 / 专辑 / 艺术家)
│   │   ├── 专辑详情
│   │   └── 艺术家详情
│   ├── CreateScreen (创作中心)
│   │   ├── AIGenerateScreen (AI生成)
│   │   └── UploadScreen (上传)
│   └── ProfileScreen (个人中心)
│       ├── 本地播放列表
│       ├── 收藏歌曲
│       └── 设置
├── PlayerScreen (全屏播放器，独立路由)
└── PlaylistDetailScreen
```

### 3.3 ViewModel 重构

| ViewModel | 职责 |
|-----------|------|
| `MusicViewModel` | 现有，拆分缩小职责 |
| `PlayerViewModel` | 新增 — 纯播放控制：队列、进度、播放模式 |
| `LibraryViewModel` | 新增 — 本地音乐浏览、搜索、分组 |
| `AuthViewModel` | 新增 — 注册/登录/Token管理 |
| `DiscoveryViewModel` | 新增 — 发现页数据、搜索远端 |
| `GenerateViewModel` | 新增 — AI生成流程 |

### 3.4 播放队列设计

```
MediaController 管理 MediaSession
  → ExoPlayer 内部维护播放队列
  → PlayerViewModel 暴露:
    - playQueue: List<Song>       当前队列
    - currentIndex: Int           当前位置
    - playMode: PlayMode          顺序/单曲循环/随机
```

---

## 四、后端架构 (Ktor)

### 4.1 项目结构

```
backend/
├── src/main/kotlin/
│   ├── Application.kt         入口
│   ├── config/                配置
│   │   ├── Database.kt        PostgreSQL 连接
│   │   ├── Security.kt        JWT 配置
│   │   └── Storage.kt         文件存储配置
│   ├── model/                 数据模型
│   │   ├── User.kt
│   │   ├── Track.kt
│   │   ├── Playlist.kt
│   │   └── ...
│   ├── route/                 API 路由
│   │   ├── AuthRoutes.kt      /api/auth/*
│   │   ├── TrackRoutes.kt     /api/music/*
│   │   ├── PlaylistRoutes.kt  /api/playlist/*
│   │   ├── SocialRoutes.kt    /api/social/*
│   │   └── SearchRoutes.kt    /api/search/*
│   ├── service/               业务逻辑
│   └── middleware/            中间件
│       ├── JWTMiddleware.kt    JWT 验证
│       └── RateLimit.kt       限流
└── build.gradle.kts
```

### 4.2 API 端点总览

```
POST   /api/auth/register                  注册
POST   /api/auth/login                     登录
GET    /api/auth/refresh                   刷新 Token

GET    /api/music/discover?page=&tag=     发现页（热门/最新）
GET    /api/music/search?q=&type=         搜索音乐/用户/歌单
POST   /api/music/upload                  上传音乐（multipart）
GET    /api/music/{id}                    音乐详情（含评论数/点赞数）
DELETE /api/music/{id}                    删除（仅自己的）
POST   /api/music/{id}/like               点赞/取消点赞
POST   /api/music/{id}/view               增加播放量

POST   /api/music/{id}/comment            评论
GET    /api/music/{id}/comments           获取评论列表

POST   /api/playlist                      创建歌单
GET    /api/playlist/{id}                 歌单详情
POST   /api/playlist/{id}/add             加入歌曲
POST   /api/playlist/{id}/remove          移除歌曲
GET    /api/playlist/public               歌单广场

GET    /api/user/{id}/profile             个人主页
POST   /api/user/{id}/follow              关注/取消
GET    /api/user/{id}/followers           粉丝列表
GET    /api/user/{id}/tracks              用户作品列表
```

### 4.3 数据库表

```sql
-- 用户
CREATE TABLE users (
    id          SERIAL PRIMARY KEY,
    username    VARCHAR(50) UNIQUE NOT NULL,
    email       VARCHAR(255) UNIQUE NOT NULL,
    password    VARCHAR(255) NOT NULL,       -- bcrypt hash
    avatar_url  VARCHAR(500),
    bio         TEXT,
    created_at  TIMESTAMP DEFAULT NOW()
);

-- 音乐
CREATE TABLE tracks (
    id              SERIAL PRIMARY KEY,
    user_id         INT REFERENCES users(id),
    title           VARCHAR(200) NOT NULL,
    artist          VARCHAR(200),
    genre           VARCHAR(50),
    bpm             INT,
    duration_sec    INT,
    file_url        VARCHAR(500) NOT NULL,   -- 音频文件路径
    cover_url       VARCHAR(500),            -- 封面图
    description     TEXT,
    tags            TEXT[],                  -- 标签数组
    play_count      INT DEFAULT 0,
    like_count      INT DEFAULT 0,
    comment_count   INT DEFAULT 0,
    is_ai_generated BOOLEAN DEFAULT FALSE,
    ai_prompt       TEXT,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 歌单
CREATE TABLE playlists (
    id          SERIAL PRIMARY KEY,
    user_id     INT REFERENCES users(id),
    name        VARCHAR(100) NOT NULL,
    cover_url   VARCHAR(500),
    is_public   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMP DEFAULT NOW()
);

CREATE TABLE playlist_tracks (
    playlist_id INT REFERENCES playlists(id) ON DELETE CASCADE,
    track_id    INT REFERENCES tracks(id) ON DELETE CASCADE,
    position    INT DEFAULT 0,
    PRIMARY KEY (playlist_id, track_id)
);

-- 社交
CREATE TABLE likes (
    user_id    INT REFERENCES users(id),
    track_id   INT REFERENCES tracks(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (user_id, track_id)
);

CREATE TABLE comments (
    id         SERIAL PRIMARY KEY,
    user_id    INT REFERENCES users(id),
    track_id   INT REFERENCES tracks(id) ON DELETE CASCADE,
    content    TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE follows (
    follower_id  INT REFERENCES users(id),
    followed_id  INT REFERENCES users(id),
    created_at   TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (follower_id, followed_id)
);

-- 索引
CREATE INDEX idx_tracks_user_id ON tracks(user_id);
CREATE INDEX idx_tracks_created_at ON tracks(created_at DESC);
CREATE INDEX idx_tracks_play_count ON tracks(play_count DESC);
CREATE INDEX idx_tracks_genre ON tracks(genre);
CREATE INDEX idx_comments_track_id ON comments(track_id);
CREATE INDEX idx_likes_track_id ON likes(track_id);
```

---

## 五、分阶段实施计划

### Phase 1: 完整本地播放器（纯前端改造）
**目标:** 让 APP 像一个真正可用的本地音乐播放器

| 任务 | 文件改动 | 说明 |
|------|---------|------|
| 1.1 底部导航 | 新建 BottomNavBar.kt + 重构 AIMusicNavHost | 四个 Tab：发现/音乐库/创作/我的 |
| 1.2 播放队列 | PlayerViewModel 新建 + 改造 PlayerScreen | 支持队列管理、随机/循环 |
| 1.3 专辑封面加载 | 改造 SongListItem、PlayerScreen 使用 Coil | 从 MediaStore 加载专辑封面 |
| 1.4 分组浏览 | 新建 LibraryScreen 替代 HomeScreen | 歌曲/专辑/艺术家 Tab 切换 |
| 1.5 本地搜索 | 新增 SearchBar 到 LibraryScreen | 按标题/艺术家过滤 |
| 1.6 播放列表 CRUD | 改造 PlaylistScreen | 创建/删除/添加歌曲/移除歌曲 |
| 1.7 收藏功能 | UI 层 + Dao 查询 | 歌曲详情页点击收藏 |
| 1.8 通知栏完善 | 改造 Service | 正确显示 Metadata |
| 1.9 模块拆分 | 拆分 MusicViewModel | → PlayerViewModel + LibraryViewModel |

### Phase 2: 后端 + 社区（全栈）
**目标:** 用户注册/登录、上传音乐、发现社区内容

| 任务 | 说明 |
|------|------|
| 2.1 搭建 Ktor 项目 | 初始化 backend/ 目录 |
| 2.2 数据库建表 | PostgreSQL + Exposed |
| 2.3 注册/登录 API | JWT 签发/验证 |
| 2.4 用户 Token 管理 | APP 端 AuthViewModel + 本地 Token 存储 |
| 2.5 上传 API | multipart + 文件存储 |
| 2.6 发现页 API | 热门/最新/标签筛选 |
| 2.7 搜索 API | 全文搜索 |
| 2.8 社交 API | 点赞/评论/关注 |
| 2.9 歌单 API | 公开/收藏/同步 |
| 2.10 前端联网 | Retrofit 对接所有 API |

### Phase 3: AI 生成（差异化）
**目标:** AI 音乐生成 + 一键发布社区

| 任务 | 说明 |
|------|------|
| 3.1 对接真实 AI API | Suno/Udio 或其他 |
| 3.2 生成历史管理 | 本地存储 + 同步 |
| 3.3 一键发布 | AI作品 → 提交到 tracks 表 |
| 3.4 AI 作品展示 | 发现页 AI 专区 |

### Phase 4: 部署上线
**目标:** 服务器部署、域名、GitHub CI/CD

| 任务 | 说明 |
|------|------|
| 4.1 服务器采购 | 阿里云轻量 2C2G |
| 4.2 Docker 部署 | 后端容器化 |
| 4.3 Nginx 配置 | 静态文件代理 |
| 4.4 域名 | 购买 + ICP 备案 |
| 4.5 CI/CD | GitHub Action 自动构建 APK + 部署后端 |
| 4.6 APK 分发 | Release 页面供下载 |

---

## 六、错误处理策略

### 前端
- 网络请求统一拦截器：401 → 自动刷新 Token / 跳转登录
- Room 数据库：开启 WAL 模式，处理并发
- 播放器捕获 ConnectionError → 显示"网络播放受限"提示

### 后端
- 统一错误响应格式：`{ code, message, details? }`
- 400: 参数校验失败
- 401: Token 过期/无效
- 403: 无权操作
- 404: 资源不存在
- 429: 请求太频繁
- 500: 服务器内部错误

---

## 七、测试策略

| 层 | 工具 | 范围 |
|----|------|------|
| Unit | JUnit5 + MockK | ViewModel、Repository 业务逻辑 |
| UI | Compose UI Test | 各 Screen 渲染、交互 |
| API | Ktor Test | 端点请求/响应 |
| E2E | Postman/手动 | 完整用户流程 |

---

## 八、上线与简历建议

### 服务器成本
- 阿里云轻量 2C2G 3M: ¥34/月
- 域名 .com: ¥60/年
- 纯个人项目量级下，每月 ¥40 足够支撑 1000+ 日活

### 简历呈现方式
- GitHub 仓库 README: 架构图 + 截图 + 部署链接 + 技术栈标签
- 面试重点：**全栈思维** — 为什么用 Ktor 而不用 Spring？为什么 Media3 而不是原生 MediaPlayer？缓存策略怎么做的？
