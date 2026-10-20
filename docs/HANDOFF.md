# 暖月 · 交接文档

> 写给接手的开发者（人或 AI）。机密信息（服务器私钥、签名密码、她的邀请码）**不在本仓库**，在 `deploy/private/HANDOFF_SECRETS.md`，由项目负责人单独提供。

## 1. 这是什么

一个给女朋友用的安卓经期记录 App「暖月」，外加一个极小的加密同步服务。

- **App**：Kotlin + Jetpack Compose，本地 Room 数据库，离线可用。界面风格参考「柠檬小月」：暖粉纸底、圆角卡片、粉色经期、橙色易孕期、心形排卵日、吉祥物「小月」（会犯困的小月亮）、手账贴纸。
- **服务器**：域名 <https://leafy.fxxkopenai.com>（Cloudflare 代理 HTTPS，源站仍是 `129.151.21.101` 的 nginx :80）+ FastAPI（127.0.0.1:8090）+ SQLite。只存端到端加密的密文，同时托管下载页和 APK。443 被 xray 占用，源站不要加证书。
- **当前版本**：1.6.0（versionCode 7）。线上如果还是 1.5.0，要等 `deploy/deploy.sh` 跑过才会换成这一版。

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
- 首页三种视图：月历（左右滑 / 点标题弹出年月选择器快速跳转）、整年（12 个迷你月，点月份跳入）、周期环（上次 / 本次 / 下次）。1.6.0 起这三页的主体都不靠滚动：月历格子按剩下的高度分（只画这个月实际占的 5 或 6 行），整年和周期环缩进同一块区域。抬头收成一行问候加状态，小月缩小。
- 点某一天，底下显示这天的日子。经期中主按钮是「月经结束了」，另一个动作收成一行文字；没在经期时主按钮是「月经来了」。点下去仍先弹出确认，比以前矮一截。悄悄话是一行，点一下换一句，不用往下滚。
- 状态栏和导航栏的空隙由 `Scaffold` 的 `padding` 统一留出，页面内容不再画进系统栏。字体变大时先压缩月历，开始/结束和悄悄话留在屏幕上。
- 动画：按压弹簧缩放、激活时墨水扩散填充 + 图标旋转成对勾 + 爱心粒子爆开、分段控件滑动高亮、底栏滑动指示条 + 图标弹跳、页面方向感切换、状态数字滚动、短语卡片 3D 翻牌、小月呼吸 / 眨眼 / 点击挤压回弹、贴纸漂浮。都在 `ui/components/Motion.kt`、`Mascot.kt`、`Effects.kt`。
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

HTTPS 在 Cloudflare 上终结，回源仍是明文 `:80`。不要在这台机器上占 443，也不要给 nginx 加证书。App 1.5.0 优先走 `https://leafy.fxxkopenai.com`；域名在网络层失败时，同一请求回退一次到 `http://129.151.21.101`。`version.json` 里的 `url` 是 `https://leafy.fxxkopenai.com/`，检查更新打开的是下载页。安全性仍然靠端到端加密：回退走的明文通道上，服务器看到的也只是密文。

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
- 小组件做成和首页一样的小月风格，或加一个 2×2 的「距离下次」小组件。
- 经期开始 / 结束也可以从通知里一键确认（通知按钮 + 同样的二次确认）。
- 统计页加「每个月几号来」的散点图，方便看规律。
