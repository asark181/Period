# 暖月 · 经期记录 App

给她用的安卓经期记录应用。风格参考柠檬小月：暖粉纸底、圆角卡片、粉色经期、橙色易孕期、心形排卵日，吉祥物是会犯困的小月亮「小月」，配一套手账贴纸。下载页：https://leafy.fxxkopenai.com/

## 功能
- **首页**：月历 / 整年 / 周期环三种视图；点年月标题可快速跳转；底部只有「月经开始 / 月经结束」，都带二次确认
- **预测**：最近 6 次周期加权平均，剔除漏记造成的异常周期；排卵日与易孕期
- **提醒**：经期前 N 天、当天、推迟、该结束了、易孕期 / 排卵日，开机自动恢复
- **统计**：平均 / 最长 / 最短周期与经期，「经期图表 / 经期数据」两个标签页
- **悄悄话**：内置 1011 条短语，按经期阶段挑选，每次打开自动换一句，点一下也能换
- **桌面小组件**：独立选择「方形 2×2」或「横向 4×2」；显示经期第几天、下次倒计时或推迟天数，并带预计日期、阶段、周期进度和小月头像。点卡片打开应用
- **邀请码同步**：她选「我是本人」，伴侣选「我是伴侣」只读查看并收到提醒；数据端到端加密，服务器只存密文
- 动画：弹簧按压、墨水填充、爱心粒子、滑动指示条、3D 翻牌、小月呼吸眨眼

## 文档
- 交接与运维：[docs/HANDOFF.md](docs/HANDOFF.md)
- 机密（私钥、签名密码、邀请码）：`deploy/private/HANDOFF_SECRETS.md`，不在仓库中

## 构建与部署
```bash
(cd android && ./gradlew testDebugUnitTest assembleRelease)
./deploy/deploy.sh deploy/private/server_id_rsa android/app/build/outputs/apk/release/app-release.apk
```
预测仅供参考，不能用于避孕。


本机使用 Clash TUN 时，如果普通 SSH 在认证前断开，可先对照测试 `ssh -B en0`（`en0` 是当前有线物理网卡，换网卡后需重新确认）。本机发布可使用：

```bash
SSH_BIND_INTERFACE=en0 ./deploy/deploy.sh deploy/private/server_id_rsa dist/nuanyue-v1.10.1.apk
```

没有设置 `SSH_BIND_INTERFACE` 时，脚本保持原来的默认网络路径。
