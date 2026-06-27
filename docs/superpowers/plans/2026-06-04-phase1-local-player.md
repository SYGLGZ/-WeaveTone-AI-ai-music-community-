# Phase 1: 完整本地播放器 — 实施计划

> **Goal:** 将 APP 从 Demo 重构为可用的本地音乐播放器
> **Architecture:** Jetpack Compose + Hilt + Room + Media3，拆分 ViewModel，新增底部导航和播放队列
> **Tech Stack:** Kotlin, Jetpack Compose, Material3, Media3, Coil, Room

---

### Task 1.1: 底部导航栏 + 导航重构

**Files:**
- Create: `app/src/main/java/com/example/myfirstapp/ui/navigation/BottomNavBar.kt`
- Create: `app/src/main/java/com/example/myfirstapp/ui/screen/LibraryScreen.kt`
- Create: `app/src/main/java/com/example/myfirstapp/ui/screen/ProfileScreen.kt`
- Create: `app/src/main/java/com/example/myfirstapp/ui/screen/CreateScreen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/ui/navigation/AIMusicNavHost.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/MainActivity.kt`

### Task 1.2: 播放队列 + PlayerViewModel + 通知栏完善

**Files:**
- Create: `app/src/main/java/com/example/myfirstapp/ui/viewmodel/PlayerViewModel.kt`
- Create: `app/src/main/java/com/example/myfirstapp/ui/viewmodel/LibraryViewModel.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/ui/viewmodel/MusicViewModel.kt` (缩减)
- Modify: `app/src/main/java/com/example/myfirstapp/ui/screen/PlayerScreen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/ui/screen/HomeScreen.kt` → 精简为 LibraryScreen

### Task 1.3: 专辑封面 (Coil) + 播放列表 CRUD + 收藏

**Files:**
- Modify: `app/src/main/java/com/example/myfirstapp/ui/screen/HomeScreen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/ui/screen/PlaylistScreen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/ui/screen/PlayerScreen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/service/MusicPlaybackService.kt`

### Task 1.4: 本地搜索

**Files:**
- Modify: `app/src/main/java/com/example/myfirstapp/ui/screen/LibraryScreen.kt`
- Modify: `app/src/main/java/com/example/myfirstapp/data/local/SongDao.kt`

---

执行方式：Inline — 按 Task 顺序批量实现，每个任务完成后编译验证。
