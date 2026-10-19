# 暖月 · 经期记录 App

温暖明媚的安卓经期记录应用，界面参考柠檬小月。下载页：http://129.151.21.101/

## 功能
- **首页**：周期环形图、倒计时 / 经期第 N 天 / 推迟天数，一键「大姨妈来了 / 走了」，七日条，今日记录，阶段小贴士，首页小纸条
- **日历**：月视图翻页，经期 / 预测经期 / 易孕期 / 排卵日着色，任意过去日期补记开始、结束
- **记录**：流量、痛经、8 种心情、15 种症状、爱爱、体温、体重、小日记
- **分析**：平均周期 / 经期、规律度、周期与经期趋势柱状图、经期历史条、症状 / 心情频次、体温体重曲线
- **提醒**：经期前 N 天和当天、推迟（1/3/7/14 天）、结束确认、易孕期 / 排卵日、每日记录；精确闹钟，重启后自动恢复
- **云同步与伴侣守护**：同一账号可以「本人记录」，也可以「伴侣只读查看」，伴侣也会收到提醒；数据在手机端 AES-256-GCM 加密，服务器只存密文
- 桌面小组件（经期倒计时）、JSON 导出、检查更新

## 预测规则
- 周期取近 6 次的加权平均，越近权重越高，并剔除漏记造成的异常周期
- 排卵日 = 下次经期前 14 天；易孕期 = 排卵日前 5 天到后 4 天
- 预测仅供参考，不能作为避孕依据

## 目录
- `android/`：Kotlin + Jetpack Compose + Room + DataStore + WorkManager / AlarmManager
- `server/`：FastAPI 同步服务（`app.py`）与下载页（`web/`）
- `deploy/`：nginx、systemd、每日备份 cron、一键部署脚本

## 构建与部署
```bash
cd android && ./gradlew testDebugUnitTest assembleRelease
./deploy/deploy.sh <ssh-key> android/app/build/outputs/apk/release/app-release.apk
```
发布新版本时，先在 `android/app/build.gradle.kts` 里递增 `versionCode` / `versionName`，App 内的「检查更新」会提示升级。

签名文件在 `android/keystore/`（已加入 gitignore），**请务必单独备份**；丢失后新版本将无法覆盖安装。
