# 暖月 · 交接文档

> 写给接手的开发者（人或 AI）。机密信息（服务器私钥、签名密码、她的邀请码）**不在本仓库**，在 `deploy/private/HANDOFF_SECRETS.md`，由项目负责人单独提供。

## 1. 这是什么

一个给女朋友用的安卓经期记录 App「暖月」，外加一个极小的加密同步服务。

- **App**：Kotlin + Jetpack Compose，本地 Room 数据库，离线可用。1.9.0 的界面是暖白底 `#FFF9F8`、白卡片、墨色文字 `#4A3540`。经期日是浅玫瑰块 `#F39BAE` 加墨色数字，易孕是浅杏 `#FFF1DC`，排卵是杏底上的橙点，预测是 `#EB8399` 虚线。吉祥物仍是「小月」，每屏最多一次。底栏是贴底的白条，选中项是一颗高出顶边的浅玫瑰圆。
- **服务器**：域名 <https://leafy.fxxkopenai.com>（Cloudflare 代理 HTTPS，源站仍是 `129.151.21.101` 的 nginx :80）+ FastAPI（127.0.0.1:8090）+ SQLite。只存端到端加密的密文，同时托管下载页和 APK。443 被 xray 占用，源站不要加证书。
- **当前版本**：1.10.1（versionCode 15，已部署，验证见文末）。发布走 `deploy/deploy.sh`，它按 APK 重写 `version.json`。
- **版本线**：1.5.0 域名 HTTPS，网络失败才回退源站 IP。1.6.0 首页一屏放下月历、开始/结束和悄悄话。1.7.0 圆角方格月历，应用内下载更新。1.7.1 方格按内容高度排，余白留给选中日。1.8.0 换成浅灰白日历和浮起的底栏圆泡，同步、加密、Room、更新协议都没动。1.9.0 按 v0.5 色板改回暖色，底栏改成随选中项隆起的白条，更新下载和安装流程没动。

用户的核心诉求（按优先级）：
1. 聚焦经期数据本身（开始 / 结束、预测、统计），**不要**做心情、症状、留言这类附加记录——已在 1.3.0 删掉。
2. 温暖、可爱、偏少女的风格，参考柠檬小月；但文案要具体有用，不要空洞的「AI 味」套话，不用 emoji。
3. 临近经期要准时提醒；交互动画要明显、有质感。

## 2. 目录

```
android/                 安卓工程（Gradle 8.11.1, AGP 8.7.3, Kotlin 2.0.21, compileSdk 35, minSdk 26）
  app/src/main/java/com/nuanyue/app/
    NuanYueApp.kt        Application：手动 DI（settings / repository / sync / phrases）
    MainActivity.kt
    data/                Room 实体、DAO、Repository、DataStore 设置、Phrases（短语库）
    domain/CycleEngine.kt  预测核心（纯 Kotlin，有单测）
    domain/Copy.kt         所有固定文案（问候、通知）
    notify/Reminders.kt    每日闹钟 → 判断今天该提醒什么 → 通知；开机/改时间后重排
    sync/                  Crypto（PBKDF2 + AES-GCM、邀请码）、SyncClient（HTTP）、SyncManager（合并/上传）
    ui/                    Compose 界面：home / stats / settings / onboarding / components / theme
    widget/PeriodWidget.kt 桌面小组件（RemoteViews）
  app/src/main/assets/phrases.txt   1011 条短语（首启导入 Room）
  app/src/test/…/CycleEngineTest.kt、FeedbackTest.kt、SyncClientFallbackTest.kt、HomeActionsTest.kt 单测；HomeScreenshotTest.kt 用 Paparazzi 画首页（月历 / 整年 / 周期环）
  app/schemas/            Room schema 导出（v1、v2）
art/                     设计源文件（SVG），改图改这里
  mascot/                小月的分层 SVG（身体、腮红、三种眼睛、两种嘴、热水袋、Zzz）
  stickers/              13 个手账贴纸
  phrases/               短语原稿，按类别分文件
  tools/svg2vector.py    SVG → Android VectorDrawable
  tools/preview.py       渲染形象和贴纸总览图（需要 cairosvg）
server/                  FastAPI 同步服务 app.py + 测试 test_app.py + 下载页 web/
deploy/                  一键部署脚本、nginx / systemd / 备份 cron、邀请码导入脚本
  private/               （gitignore）机密：私钥、她的数据、交接机密文档
scripts/adb_tap.sh       模拟器自动化：按界面文字点击（uiautomator dump）
dist/                    （gitignore）各版本签名 APK
```

## 3. 关键设计

### 3.1 预测（`domain/CycleEngine.kt`）
- 周期 = 最近 6 次合理周期（15–90 天）的加权平均，越近权重越大；≥3 次时剔除偏离中位数 ×0.6~1.6 之外的异常值（比如漏记导致的 88 天）。
- 经期长度 = 最近 6 次已结束经期的加权平均。
- 排卵日 = 下次经期前 14 天；易孕期 = 排卵日 −5 ~ +4。
- 推迟：今天超过预测开始日且不在经期中 → `Phase.LATE`，日历把预测从明天开始画。
- 统计页的「平均周期 38 天」是全部周期的算术平均（和柠檬小月一致），与预测用的加权值（她目前是 34 天）不同，统计页有说明卡片。

### 3.2 数据与同步
- 本地：Room `periods`（id, startDay, endDay?, updatedAt, deleted 墓碑）、`day_records`（1.3.0 起界面不再使用，但保留以兼容同步）、`phrases`。
- 同步：一个「空间」对应一份数据。客户端用 PBKDF2-HMAC-SHA256(120k, 512 bit) 从 (账号, 密码) 派生：前 32 字节 = AES 密钥，后 32 字节 = 鉴权令牌。服务器只存 sha256(令牌) 和 AES-GCM 密文。
- **邀请码**：`NY-XXXX-XXXX-XXXX`（字母表去掉易混字符）。账号 = `inv-` + 小写码，密码 = 大写码。她选「我是本人」（可写），男朋友选「我是伴侣」（只读拉取，提醒文案改用第三人称）。
- 合并：按记录 id 取 `updatedAt` 较新者，墓碑参与合并；伴侣模式直接镜像。备份里带一个 `profile`（昵称），本机为空时采用。
- 地址：`BuildConfig.SERVER_URL` = `https://leafy.fxxkopenai.com`，同步、检查更新都走它。请求如果在网络层失败（连不上、超时、TLS 失败，还没有 HTTP 状态码），同一请求再试一次 `BuildConfig.SERVER_FALLBACK_URL` = `http://129.151.21.101`。HTTP 4xx/5xx 不回退，避免把服务器的拒绝当成线路问题。明文只对这个 IP 放开（`res/xml/network_security_config.xml`）。加密、备份格式、Room、API 都没改，旧版本覆盖安装即可。
- `deploy/seed_invite.py`：用 CSV 生成邀请码数据并上传，算法与 App 完全一致；同一个码重复运行是幂等的（id 由码 + 日期确定性生成）。默认打到域名；域名不通时加 `--server http://129.151.21.101`。

### 3.3 提醒
- `AlarmManager` 每天在设定时间（默认 9:00）触发一次 `ReminderReceiver`，用 `Reminders.due()` 判断：经期前 N 天、当天、推迟 1/3/7/14 天、超平均经期未结束、易孕期开始 / 排卵日（默认关）。
- 精确闹钟：声明了 `USE_EXACT_ALARM`（非应用商店分发，可用）；`BootReceiver` 在开机、更新、改时间后重排。
- 已在模拟器上用改系统时间的方式验证过闹钟会自己触发并重排次日。

### 3.4 界面与动画（1.3.0）
- 首页三种视图：月历（左右滑 / 点标题弹出年月选择器快速跳转）、整年（12 个迷你月，点月份跳入）、周期环（上次 / 本次 / 下次）。1.6.0 起这三页的主体都不靠滚动。1.9.0 的日期色块是 36dp 圆角方块（紧凑屏 32dp，圆角 12dp），行高 48dp（紧凑 40dp）。经期浅玫瑰底加墨色字，易孕浅杏底，排卵在数字下有一个 5dp 橙点，预测是 1.5dp 虚线，今天是墨色外环、间隙 2dp。图例只列出这个月出现的状态。
- 月历下面是一块暖灰圆角面板：日期和阶段一行，主按钮是整宽的「月经来了 / 月经结束了」，次操作是玫瑰色文字。悄悄话在按钮下面，点一下换一句。
- 状态栏和导航栏的空隙由 `Scaffold` 的 `padding` 统一留出，页面内容不再画进系统栏。底栏自己的高度包含凸起的圆，所以圆不会挡住日历。
- 底栏三个标签是「首页 / 统计 / 我」。白条贴底，只有顶部 1dp 发丝线。没选中是浅色线框图标加 `#876E7C` 字。选中图标在 56dp 浅玫瑰圆里，圆有 6dp 白边，高出白条大约 16dp，白条顶边绕着圆平滑隆起。切换时只有这颗圆用弹簧移到下一个标签，其它标签不跳。有轻触反馈。系统「动画时长」为 0 时 `LocalMotion` 为 false，动画停在终态。
- 手动点「检查更新」时，结果写在这一行右侧：检查中是小进度圈且不能连点，已是最新显示几秒后恢复，失败可以再点。有新版本仍走原来的更新卡片。日志 tag 是 `NyUpdate`。

### 3.5 应用内更新（1.7.0 起，协议到 1.8.0 没变）
1.6.0 及更早的客户端仍然只读 `url`，检查到新版本就打开下载页。1.7.0 起在应用里下载 APK，1.8.0 沿用同一套。

流程：`WorkManager` 每 12 小时、在有网络时查一次 `GET /api/version`。发现比本机大的 versionCode，每个 versionCode 只发一次通知（更新的号会再通知）。应用内下载 APK，有 `sha256` 就校验，没有就只认 ZIP 头 `PK`。域名在网络层失败时，下载再试一次 `http://129.151.21.101/download/nuanyue.apk`；HTTP 4xx/5xx 不换地址。

`GET /api/version` 照旧把 `version.json` 原样返回，没有改接口。客户端认这些字段：

| 字段 | 必填 | 作用 |
|---|---|---|
| `versionCode` | 是 | 整数。大于本机才算新版本 |
| `versionName` | 是 | 展示用，例如 `1.9.0` |
| `url` | 是 | 下载页。只有应用内下载失败时，卡片上的「打开下载页」才会用它。旧客户端仍然直接打开它 |
| `notes` | 否 | 通知正文和更新卡片上的说明，缺省空字符串 |
| `apkUrl` | 否 | 安装包地址。缺省或空字符串时用 `https://leafy.fxxkopenai.com/download/nuanyue.apk` |
| `sha256` | 否 | APK 的 SHA-256，小写十六进制。有就校验，没有就只检查 ZIP 头 `PK` |
| `size` | 否 | 下载页可以继续带，客户端忽略 |

发布时写成：

```json
{"versionCode": 11, "versionName": "1.9.0", "url": "https://leafy.fxxkopenai.com/", "apkUrl": "https://leafy.fxxkopenai.com/download/nuanyue.apk", "sha256": "<apk 的 sha256>", "notes": "", "size": "3MB"}
```

`size` 由 `deploy/deploy.sh` 用 `du -m` 填，客户端不读。`sha256` 是小写十六进制。

域名在网络层失败时，下载会再试一次 `http://129.151.21.101/download/nuanyue.apk`（HTTP 4xx/5xx 不换地址）。这个 IP 本来就允许明文，不用再加路径。下载可断点续传；校验通过的文件不会再下；用户取消后，下次进程起来不会自动接着下，要点按钮。通知渠道 `app_updates`，每个 versionCode 只通知一次。WorkManager 每 12 小时在有网络时查一次。Android 13+ 的通知权限不在第一次打开时弹，引导和更新卡片里再要。安装走 FileProvider 和系统安装器；没开「安装未知应用」时带去设置，回来继续。
- 短暂提示（同步完成、复制、更新、日历点未来日期等）一律走 `AppViewModel.toast` → `WarmToast`。不要改回 Material `Snackbar`：主题里 `bodyMedium` 写死了深色字，Snackbar 又用默认的近黑 `inverseSurface`，1.3.0 在真机上是黑底深字。颜色对比度锁在 `FeedbackTest`。
- 短语：1011 条，分 P(经期)/F(卵泡期)/O(易孕)/L(经前&推迟)/A(日常)/B(伴侣) 六类；每次回到前台换一句，点卡片换一句；70% 取当前阶段类别、30% 取日常类。改短语：编辑 `art/phrases/*.txt` 后重新生成 `assets/phrases.txt`（见第 5 节），App 发现条数变化会自动重新导入。

## 4. 服务器

| 项 | 说明 |
|---|---|
| 系统 | Ubuntu 24.04 aarch64（Oracle Cloud） |
| 不要动 | `x-ui`、`xray` 占用 443 / 2096 / 4396，是用户的代理服务 |
| 域名 | `leafy.fxxkopenai.com`，Cloudflare 代理（橙云）终结 HTTPS，回源这台机器的 nginx `:80`。源站没有证书 |
| 入口 | nginx `:80`，配置 `/etc/nginx/sites-available/nuanyue`。`server_name leafy.fxxkopenai.com 129.151.21.101 _`，并保持 `default_server`，IP 直连仍给旧版 App 用 |
| 服务 | systemd `nuanyue.service`，uvicorn 监听 127.0.0.1:8090，用户 `nuanyue`，venv `/opt/nuanyue/venv` |
| 代码 | `/opt/nuanyue/app/app.py` |
| 静态 | `/opt/nuanyue/web/`（下载页、`download/nuanyue.apk`、`version.json`） |
| 数据 | `/var/lib/nuanyue/nuanyue.db`，每天 03:17 备份到 `/var/lib/nuanyue/backups/`，保留 14 天 |
| 防火墙 | iptables 已放行 80；Oracle 安全组也已放行（外网可访问） |
| API | `GET /api/health`、`GET /api/version`、`GET/PUT /api/backup/{space}`（头 `X-Auth`）；每 IP 120 次/分钟，错误密码每 IP 10 分钟 20 次 |

HTTPS 在 Cloudflare 上终结，回源仍是明文 `:80`。不要在这台机器上占 443，也不要给 nginx 加证书。App 1.5.0 优先走 `https://leafy.fxxkopenai.com`；域名在网络层失败时，同一请求回退一次到 `http://129.151.21.101`。`version.json` 的字段见 3.5。1.6.0 及更早打开 `url` 下载页；1.7.0 优先按 `apkUrl`（没有就用域名上的 `/download/nuanyue.apk`）在应用内下载，校验 `sha256`，网络失败才改走源站 IP 的同一个路径。安全性仍然靠端到端加密：回退走的明文通道上，服务器看到的也只是密文。

## 5. 常用操作

```bash
# 构建 + 单测
cd android && ./gradlew testDebugUnitTest assembleRelease

# 发布新版本：先改 android/app/build.gradle.kts 的 versionCode / versionName，然后
./deploy/deploy.sh deploy/private/server_id_rsa android/app/build/outputs/apk/release/app-release.apk
# 脚本会上传服务端代码、下载页、APK，写 version.json，重启服务；App 内「检查更新」据此提示

# 服务端测试
server/.venv/bin/python -m pytest -q server/test_app.py

# 导入 / 更新她的历史数据（幂等）。域名不通时加 --server http://129.151.21.101
server/.venv/bin/python deploy/seed_invite.py deploy/private/her_periods.csv --code <她的邀请码> --nickname 宝贝

# 改形象或贴纸：编辑 art/ 下的 SVG，然后
python3 art/tools/svg2vector.py art/mascot android/app/src/main/res/drawable mascot_
python3 art/tools/svg2vector.py art/stickers android/app/src/main/res/drawable st_
DYLD_FALLBACK_LIBRARY_PATH=/opt/homebrew/lib server/.venv/bin/python art/tools/preview.py /tmp/sheet.png   # 预览

# 改短语：编辑 art/phrases/*.txt 后重新生成资源
python3 - <<'PY'
out=[]
for c in 'PFOLAB':
    out += [f"{c}|{l.strip()}" for l in open(f'art/phrases/{c}.txt', encoding='utf-8') if l.strip()]
open('android/app/src/main/assets/phrases.txt','w',encoding='utf-8').write('\n'.join(out)+'\n')
print(len(out))
PY

# 模拟器自动化
~/Library/Android/sdk/emulator/emulator -avd nuanyue -no-window -no-audio &
adb install -r android/app/build/outputs/apk/debug/app-debug.apk
scripts/adb_tap.sh 我有邀请码        # 按界面文字点击
adb exec-out screencap -p > /tmp/s.png
```

测试邀请数据时请用一次性的码（例如 `--code NY-2222-3333-4444`），测完到服务器上删掉对应的 space 行，**不要用她的真实邀请码以「本人」身份做写入测试**（会把测试改动同步到她的数据里；以「伴侣」身份只读拉取是安全的）。

## 6. 已验证 / 未验证

已验证（模拟器 Pixel 7 / Android 15 + 真实服务器）：
- 单测 7/7、服务端测试 2/2 通过；release（R8 混淆）包能正确解密 Python 生成的邀请数据。
- 1.0.0 → 1.1.0 → 1.2.0 → 1.3.0 逐级覆盖安装数据保留，含 Room v1→v2 迁移。
- 闹钟提醒通过改系统时间触发并自动重排。
- 伴侣模式只读、错误密码提示、二次确认、整年视图、年月跳转、短语翻牌。

未验证 / 已知缺口：
- 桌面小组件没在真机上看过效果。
- 国产 ROM（小米 / 华为 / OPPO / vivo）后台限制下的提醒准时性；App 内有「提醒不准时？」引导。
- 她的柠檬小月截图只录入了能看到的 30 条（2023-10 起），更早的记录没有；柠檬小月显示最长经期 10 天，可能就在没截到的部分。
- 暗色模式未适配（固定浅色）。
- 域名在大陆网络上的实际成功率，以及回退到 IP 要多久。回退规则有单测（网络失败重试一次且请求不变，HTTP 错误不重试），没有在她的手机上遇到过 Cloudflare 抽风。

## 7. 可以继续做的
- 小组件已提供「方形 2×2」「横向 4×2」两个独立入口；后续仍需在小米 HyperOS 真机核验桌面网格及后台刷新。
- 经期开始 / 结束也可以从通知里一键确认（通知按钮 + 同样的二次确认）。
- 统计页加「每个月几号来」的散点图，方便看规律。

---

## 2026-10-20 晚交接（Claude，1.9.2，未部署）

### 已完成（已提交到 main，release 包已能构建）
基于 origin/main 的 1.9.1 改动，版本号升到 **1.9.2（versionCode 13）**：
1. **日历方格改正方形**：`ui/home/HomeScreen.kt` 的 `MonthGrid`，`DateChip` 不再传 `cellWidth/cellHeight`，尺寸取 `minOf(maxWidth, maxHeight, 48.dp)`。
2. **选中边框颜色自适应**：`ui/components/Components.kt` 的 `DateChip`，选中环按当天类型取色：经期 = PinkDeep 55%，易孕 / 排卵 = Orange 60%，其他 = Pink 55%（原来固定蓝色 Sky）。
3. **降低饱和度**：`ui/theme/Feedback.kt` `ACCENT` `#F2708A` → `#E88A9C`（即 Palette.Pink）；`Theme.kt` `Orange` `#F7A928` → `#F0B46A`，`PinkLine` → `#F0B8C3`。
4. **按钮放到一行，不再顶起来**：`DayDock` 里主按钮和次按钮并排（各占一半，高 46dp）；未来日期不再多出一行提示文字，按钮变灰，点击用 toast 提示原因。
5. **小贴士显示不下**：`PhraseLine` 固定两行（`minLines = 2, maxLines = 2`，13sp），卡片高度不再变化。
6. 首页本身已是 `OneScreen` 一屏布局（没有 verticalScroll），这次没改结构。

**以上都没在模拟器上验证过**（按用户要求不做测试；且本机 SDK 被删，已重装但没有模拟器镜像）。接手后请先在 1440×3200 / 560dpi 上看一眼首页。

### 未完成
1. **部署失败**：`deploy/deploy.sh` 连 `129.151.21.101:22` 时，TCP 端口是通的，但在认证前就被服务器断开（`Connection closed by ... port 22`）。用 `deploy/private/server_id_rsa` 和本机 `~/.ssh/oracle_*` 三把钥匙都一样。最可能是 **fail2ban 封了本机出口 IP**，或者 sshd 配置 / 密钥被改过。处理办法：从 Oracle 控制台的 Cloud Shell 或串口控制台登录，执行 `sudo fail2ban-client status sshd`，再 `sudo fail2ban-client set sshd unbanip <IP>`，或者核对 `~/.ssh/authorized_keys`。恢复后执行：
   ```bash
   ./deploy/deploy.sh deploy/private/server_id_rsa android/app/build/outputs/apk/release/app-release.apk
   ```
   本地已构建好的包：`dist/nuanyue-v1.9.2.apk`（签名证书 SHA-256 `ce98b7f6…bca`，和线上一致，能覆盖安装）。线上目前仍是 1.9.1（`https://leafy.fxxkopenai.com/api/version`）。注意 1.9.x 的 `version.json` 带 `apkUrl` / `sha256` 字段，以当前 `deploy.sh` 的写法为准。
2. **桌面小组件重新设计（小米 15 Pro 用户要求）**，还没动。现状在 `widget/PeriodWidget.kt` + `res/layout/widget_period.xml`（RemoteViews）。需求：
   - 经期中：大字「经期第 N 天」，副行「预计还有 X 天结束 · 下次 M月D日」。
   - 非经期：大字「还有 N 天」，副行「下次月经 M月D日 · 当前阶段（安全期/易孕期/排卵日）」；推迟时显示「已推迟 N 天」。
   - 信息要全：再加一条周期进度条（周期第几天 / 平均周期）和小月头像。
   - 建议做 2×2 和 4×2 两种尺寸（`res/xml/period_widget_info.xml` + `targetCellWidth/Height`，Android 12+ 可以用 `RemoteViews(Map<SizeF, RemoteViews>)` 响应式布局）；数据来源沿用 `CycleEngine.snapshot`；背景色降饱和：经期 `#E88A9C`，易孕 `#F0B46A`，其他白底。
   - 刷新：现有 `PeriodWidget.updateAll()` 在数据变化和每日闹钟时调用，够用。

### 环境注意
- **Android SDK**：原来的 `~/Library/Android/sdk` 被删了，这次只重装了 cmdline-tools、platform-tools、platforms;android-35、build-tools;35.0.0，**没有 emulator 和系统镜像**。用户提到的模拟器 `nuanyue` 和快照 `ref160` / `pre190` 如果要用，需要先确认 AVD 在哪（`~/.android/avd`）、再装 `emulator` 和 `system-images;android-35;google_apis;arm64-v8a`。
- `adb` 路径：`~/Library/Android/sdk/platform-tools/adb`（已重装）。
- 测试机配置（用户提供）：Pixel 7 原生 Android 15，用 adb 改成 1440×3200、密度 560（约 411dp 宽），手势导航，字号 1.0；测试数据：昵称 QA190、8 条经期、周期 29 天、经期 5 天、提醒 21:30。

### 接手续做方案（2026-10-20，用户已确认）

- 小组件保留现有 `PeriodWidget` 入口，用系统 RemoteViews 提供 2×2 紧凑布局及拉宽后的 4×2 布局。Android 12+ 按尺寸自动选布局，旧系统在尺寸变化时重绘。
- 数据展示独立为纯 Kotlin `WidgetState`，只消费 `CycleEngine.snapshot`，不改预测、同步或数据库。经期显示第几天、预计剩余天数和下次日期；非经期显示倒计时及阶段；推迟显示推迟天数和原预测日期，避免把今天误写成下一次日期。
- 两种尺寸均显示周期进度及小月头像。经期用低饱和粉色、易孕用浅橙、其余白底，文字保持深色；空数据提示开始记录，不显示虚假预测。点击打开应用。
- 实施顺序：先用合成日期覆盖经期、预计当天、推迟、无数据和进度边界；再实现展示模型、两种布局、尺寸响应；最后运行单测、release 构建与签名检查，并验证模拟器首页及桌面小组件。
- 部署单独核实 SSH 断连原因。仅发布暖月文件和服务，保留数据库、邀请码及 x-ui/xray；发布后核对健康检查、版本、APK 摘要和签名。连接未恢复前不把发布记为完成。


### 2026-10-20 Codex 接手续做记录

- 已从已登录的 Oracle Cloud Shell 使用原有私钥登录服务器，并运行 `deploy/deploy.sh` 发布 1.9.2。公网版本接口返回 versionCode 13，APK SHA-256 与本机签名包一致；`nuanyue`、`nginx`、`x-ui` 均为 active。旧包及旧版本文件保存在服务器 `/opt/nuanyue/releases/pre-1.9.2/`。
- 本机 SSH 仍在认证前断开，Cloud Shell 同密钥可登录。实际查询 fail2ban 的 sshd jail 当前封禁数为 0，因此没有执行解封、修改 IAM、关闭安全防护或改动代理服务；原交接中的 fail2ban 推断未被证实。
- 小组件版本为 1.10.0（versionCode 14）。`WidgetState` 整理已有 CycleEngine 的结果：经期按包含今天的剩余天数展示，推迟展示原预测日期，无数据或只有未来记录时不编造当前周期。当前阶段沿用现有引擎，不增加医学判断。
- RemoteViews 提供 2×2 紧凑布局及宽度达到 300dp 后的 4×2 布局，两种尺寸都有下次日期、周期进度和已有小月头像；Android 12+ 交给桌面按尺寸选择，旧版通过尺寸回调重绘。保留既有数据变更、每日提醒及系统定期刷新入口。最小验证尺寸为 140×160dp 和 300×160dp，字号 1.0；更小尺寸和小米 HyperOS 的实际桌面行为仍需真机核验。
- 验证：`android/gradlew -p android testDebugUnitTest assembleRelease assembleDebug --offline --console=plain` 成功；JUnit 77 项、失败/错误/跳过均为 0。新增 7 项展示逻辑测试和 5 项布局测试（每项覆盖两种尺寸），用真实 RemoteViews 膨胀并检查文字宽高，修复了窄布局标题及宽布局日期裁切。首页同时按 1440×3200／560dpi 和小屏渲染。服务端 `server/.venv/bin/python -m pytest -q server/test_app.py`：2 passed（依赖库一项弃用提示）。
- 签名包：`dist/nuanyue-v1.10.0.apk`，签名证书 SHA-256 与 1.9.2 一致；APK SHA-256 为 `cc05585189fbba66a20e44d02d6070dba2eab675753389623d754bab901e213b`。`dist/widget-1.10.0-2x2.png`、`dist/widget-1.10.0-4x2.png` 是布局渲染结果，不代表 HyperOS 真机截图。


### 最终发布与模拟器验证（2026-10-20）

- **线上已更新到 1.10.0 / versionCode 14**。通过 Oracle Cloud Shell 执行现有 `deploy/deploy.sh`；公网 `/api/version` 返回 14 / 1.10.0，公网实际下载 APK、服务器 APK 和本机包的 SHA-256 三者相同，均为上文 `cc055851…e213b`；公网 `/api/health` 返回 `ok: true`，`nuanyue`、`nginx`、`x-ui` 均为 active。服务器 `/opt/nuanyue/releases/pre-1.10.0/` 保留 1.9.2 安装包和版本文件，`pre-1.9.2/` 保留此前版本；没有迁移或导入业务数据。
- **Cloud Shell 的临时私钥文件已删除并核验不存在**，本机 `deploy/private/server_id_rsa` 及签名文件仍在。机密文档和签名密钥没有上传。发布核验截图：`dist/oracle-1.10.0-verification.png`。本机直连 SSH 的断连仍未定位，后续发布可使用已验证的 Cloud Shell 路径，按需临时上传原有私钥并在完成后删除；不要根据旧推断直接停用 fail2ban。
- **模拟器运行环境已恢复**：`~/Library/Android/sdk`，emulator 37.2.12、API 35 Google APIs ARM64 镜像 revision 9；下载包先按 Google 仓库摘要校验再安装，完成后删除安装压缩包。adb 为 `~/Library/Android/sdk/platform-tools/adb`。
- 从 `nuanyue` 的 `pre190` 快照以 `-read-only -no-snapshot-save` 启动，Android 15。快照恢复后的物理配置为 1080×2400 / 420dpi，本轮用 `adb -s emulator-5560 shell wm size 1440x3200` 和 `wm density 560` 设置目标测试尺寸；字号 1.0、手势导航已确认。QA190 首页、1.10.0 签名覆盖安装、桌面添加 2×2、拖动到 4×2 自动切换布局、点击返回应用均已实测通过，未写入经期记录。
- 实测截图：`dist/emulator-home-1.10.0.png`、`dist/emulator-widget-2x2-1.10.0.png`、`dist/emulator-widget-4x2-1.10.0.png`。这些是 Pixel 7 原生 Android 15 按指定屏幕尺寸模拟的结果；小米 HyperOS 真机的桌面网格、后台刷新仍未验证。
- 验证结束已关闭模拟器，没有保存新的测试快照；`ref160` 和 `pre190` 均保留。加载 `pre190` 会切换 QCOW2 活动快照，并更新 `snapshot.pb` 和活动盘的修改时间；本轮比较的 12 个原始文件大小均未变化，但不能声称所有文件逐字节不变。下次启动若需保留本轮覆盖安装或桌面摆放，需要另行决定是否保存状态；本次没有保存这些测试操作。
- 代码保存在本地 `codex/nuanyue-widget` 分支，功能提交 `7d6e4ee`；本轮未推送 GitHub。


### 2026-10-21 小组件尺寸修复方案

用户反馈 HyperOS 添加后出现窄长卡片，无法压成正方形。原配置把最小高度限制在 160dp，且只提供一个入口，紧凑内容撑满宿主；昨天的 140×160dp 回归没有覆盖正方形。

本轮保留已有 `PeriodWidget` 组件，改为「方形 2×2」，新增「横向 4×2」入口。方形使用独立背景卡片，Android 12+ 按宿主实际可用宽高取较短边，确保背景等宽等高，外框留透明区域；两种入口均降低最小高度并重新排布文字，测试 110×110dp、140×140dp、140×240dp 以及横向紧凑高度。数据预测和同步不改，两个入口都接入现有刷新。升级后旧卡可继续显示；若桌面缓存旧约束，需移除旧卡再从对应新入口添加。桌面占几格最终由宿主决定，不把 Pixel 尺寸验证写成 HyperOS 真机验收。

实施顺序：先复现小正方形裁切、长宿主中的方卡尺寸及入口约束；再修复布局和 provider；随后全量回归、签名构建、模拟器双入口与缩放验收；通过后发布补丁版本并推送当前分支。服务器继续优先使用昨日验证可用的 Oracle Cloud Shell 路线，本机认证前断连仍不得归因于未经证实的 fail2ban。


### 2026-10-21 修复结果与 SSH 根因

- **1.10.1 / versionCode 15 已从本机发布**，公网 `/api/version`、`/api/health` 及实际下载 APK 已核验。APK SHA-256：`f78f3343d6e0971d5f9678e6819377bf8971ddc81479ee4bc93273bc9065ba9c`，签名证书与此前版本一致。1.10.0 的 APK 与版本文件备份在服务器 `/opt/nuanyue/releases/pre-1.10.1/`。
- **尺寸修复**：保留 `PeriodWidget` 的组件名并更名为「方形 2×2」，新增 `WidePeriodWidget`「横向 4×2」。方形背景与宿主外框分离，Android 12+ 优先按桌面提供的实际尺寸取宽高较短边，外框过长时不再把粉色背景撑长；没有实际尺寸列表时按横竖屏范围回退。Android 8–11 使用固定 110dp 方形背景。两种入口均重排文字，最小高度由 160dp 降至 110dp，宽版独立默认为 4×2，并接入原有数据变更、每日提醒和周期刷新。
- **使用方式**：升级后先移除旧卡片，再从系统添加小组件里选择「暖月 · 方形 2×2」或「暖月 · 横向 4×2」。旧组件的内容可继续更新，但部分桌面会缓存旧的最小高度，重新添加才会读取新约束。粉色卡片的正方形比例由 App 保证；桌面选框的占格和精确像素尺寸仍归桌面管理，不能把 2×2 网格直接等同于正方形像素外框。
- **验证**：`android/gradlew -p android testDebugUnitTest assembleRelease --offline --console=plain` 成功，81 项测试通过。补充 110×110dp 正方形、140×240dp 长宿主仍保持方形、280×110dp 横向和最小高度回归。原生 Android 15 模拟器在 1440×3200、560dpi 下显示两个添加入口，添加后方形背景实测 602×602px，宽版 4×2 可直接添加；HyperOS 真机仍需用户验收。布局效果与实测图在 `dist/widget-square-110-1.10.1.png`、`dist/emulator-widget-square-1.10.1.png`、`dist/emulator-widget-wide-1.10.1.png`。
- **SSH 的当前根因已定位到本机 TUN 路径**：本机到 `129.151.21.101` 的路由走 `utun1024`、网关 `198.18.0.1`；Clash TUN 与 auto-route 均开启。普通连接在收到 SSH 横幅前被断开；同机同钥使用 `ssh -B en0` 后，收到 OpenSSH 横幅并正常认证登录。Cloud Shell 同钥也可登录。服务器 SSH、暖月、nginx、x-ui 均为 active，iptables 放行 22，fail2ban 当前封禁数为 0。由此排除当前密钥失效、SSH 服务宕掉及 fail2ban 当前封禁；具体 TUN 代理规则或转发环节没有继续改动。昨日现象与此一致，但未保留昨日 TUN 规则与转发日志，不把今日证据写成昨日链路的完整还原。
- **本机部署命令**：`SSH_BIND_INTERFACE=en0 ./deploy/deploy.sh deploy/private/server_id_rsa dist/nuanyue-v1.10.1.apk`。脚本用参数数组给 SSH 和 SCP 同时传入 `BindInterface`，未设置该变量时保持原默认行为。`en0` 为当前物理有线网卡，换网络或机器应先确认接口；没有关闭全局代理、改系统路由或改服务器防护。脚本已通过 `bash -n deploy/deploy.sh` 并实际发布成功。
- Cloud Shell 本轮只用于只读诊断，临时私钥已删除并确认不存在。模拟器使用 `-read-only -no-snapshot-load -no-snapshot-save` 冷启动验证，不加载或保存新快照，不写入经期记录。原 `ref160` 与 `pre190` 保留。
