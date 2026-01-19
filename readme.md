# ✅ 小游戏流水线流程 — v1.0（已验证可运行）

**Version:** `pipeline-v1.0`
**Status:** ✅ 已完整跑通（doctor → validate → build → APK 导出 → registry → commit）
**最后验证时间:** 2026-01-18

------

## 一、适用范围

- Windows + PowerShell
- Android Studio / Gradle 构建环境
- 多项目 Monorepo 结构：

```
repo-root/
  docs/
  registry/
  games/
    <game_id>/
  tools/
  kb/
  artifacts/
```

------

## 二、权威规范文件（Single Source of Truth）

必须存在于 repo root：

1. `docs/GAME_GENERATION_STANDARD.md`
   → 游戏结构 / 启动 Activity / 命名规范 / 禁止中文等
2. `docs/ENVIRONMENT_BASELINE.md`
   → JDK / Gradle / AGP / SDK 版本基线
3. `registry/produced_games.json`
   → 已生成游戏登记表（防止玩法重复）

------

## 三、流水线阶段总览

```
ENV CHECK  ->  PROJECT CHECK  ->  BUILD APK  ->  EXPORT ARTIFACT
   |               |                |                |
 doctor.ps1    validate.ps1     build_apk.ps1     artifacts/apk/
```

------

## 四、标准执行顺序（从 0 开始）

### ✅ Step 1：环境校验（必须先通过）

```
powershell -ExecutionPolicy Bypass -File tools/env/doctor.ps1
```

通过条件：

```
Doctor check PASSED.
Fails: 0, Warnings: 0
```

若失败 → 必须先修环境，不得继续。

------

### ✅ Step 2：生成新游戏项目

要求：

- 必须生成在：`games/<new_game_id>/`
- 必须包含：
  - `settings.gradle(.kts)`
  - `app/` module
  - gradle wrapper

并且：

- AGP / Gradle / SDK 必须匹配 ENVIRONMENT_BASELINE

------

### ✅ Step 3：项目规范校验（Validator）

```
powershell -ExecutionPolicy Bypass -File tools/validate.ps1 -Project games/<new_game_id>
```

校验内容包括：

- 路径必须是 `repo/games/<id>/`
- 启动 Activity 必须符合标准
- manifest label/icon 必须规范
- 源码 / 资源 / 配置中无非 ASCII（无中文）
- icon 资源存在
- compileSdk / minSdk / targetSdk = 基线值

通过条件：

```
Validator PASSED.
Fails: 0
```

失败 → 修项目，不得继续。

------

### ✅ Step 4：构建最终 APK 产物（交付物）

```
powershell -ExecutionPolicy Bypass -File tools/build_apk.ps1 -Project games/<new_game_id> -Variant debug
```

该脚本会自动：

1. 再跑一次 doctor（若存在）
2. 再跑一次 validator（若存在）
3. 使用项目自身 `gradlew.bat`
4. 执行：`clean assembleDebug`
5. 验证 APK 是否存在
6. 导出到统一目录：

```
artifacts/apk/<game_id>/<game_id>-debug-YYYYMMDD-HHMM.apk
```

并输出：

```
FINAL_APK=artifacts/apk/<game_id>/xxx.apk
```

这就是**最终安装包交付物** ✅

------

### ✅ Step 5：登记 registry（必须）

在 `registry/produced_games.json` 追加：

```
{
  "id": "<new_game_id>",
  "name": "...",
  "tags": ["..."],
  "core_loop": "...",
  "created_at": "YYYY-MM-DD"
}
```

要求：

- core_loop 不能与已有条目重复

------

### ✅ Step 6：Git 提交（必须）

```
git add
git commit -m "Add <new_game_id>"
```

不允许出现：

- 未登记 registry 就提交
- 构建失败仍提交

------

## 五、出错处理（KB 强制流程）

当出现以下任一情况：

- doctor 失败
- validator 失败
- gradle build 失败

必须按顺序：

### 1. 先搜 KB

```
rg -n "<关键报错>" kb/problems
```

### 2. 找到 → 按 Fix 操作 + 更新 Prevention

### 3. 找不到 → 新建 KB 条目

```
powershell -ExecutionPolicy Bypass -File tools/kb/new_kb_entry.ps1 -Slug "<short_slug>"
```

并填写：

- Symptom
- Error Log
- Root Cause
- Fix
- Prevention

目的：**同类问题不允许第二次踩坑**。

------

## 六、当前流程能力边界（明确不做的事）

pipeline-v1.0 明确不包含：

- ❌ 模板工程生成器
- ❌ 自动 keystore / release 签名
- ❌ CI 集成
- ❌ 自动玩法查重算法（仅 registry 人工描述）

这些属于 v2+ 版本内容。

------

## ✅ 结论

- 环境基线 ✔
- 项目规范校验 ✔
- 最终 APK 交付 ✔
- 问题沉淀机制 ✔

已经是**真正工程级可持续流水线**了，不是玩具脚本。

我们现在正式定义为：

> 🎯 **Android Mini-Game Pipeline — v1.0 (Baseline Stable)**

后续任何改动都可以：

- v1.1：性能/日志优化
- v1.2：模板生成
- v2.0：全自动游戏工厂

------

如果你后面想继续推进，我建议下一步优先级是：

👉 **v1.1：build_apk 支持 latest.apk + registry 记录产物路径**
👉 **v2.0：模板化生成器 + 一键出多款游戏**