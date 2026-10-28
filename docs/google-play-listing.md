# Google Play 商店资料草稿（暂不使用）

当前仅通过 GitHub Releases 分发，暂不提交 Google Play。本文件仅保留商店文案草稿。

## 基本信息

- 应用名称：流光日历
- 包名：`com.example.liuguangcalendar`
- 版本：1.0.3（versionCode 13）
- 类别：效率（Productivity）
- 是否含广告：否
- 是否含应用内购买：否
- 是否需要登录：否
- 目标用户：普通用户，不是专门面向儿童设计
- 联系方式：发布者需要在 Play Console 填写自己的可用联系邮箱

## 商店文案

### 简短说明

农历节假日月历与本地待办提醒，轻盈高效的中文日历

### 完整说明

流光日历是一款为 Android 手机设计的中文日历与待办提醒应用，把公历、农历、节日、法定节假日和当天任务放在同一个清晰的月视图中。

主要功能：

- 周一至周日的月历视图，支持左右滑动切换月份。
- 显示公历日期、农历日期、节日名称、休息日和调休上班日。
- 提供“今天”入口，快速回到当前日期。
- 当天待办集中显示在日历下方，支持完成、编辑、删除和延后 10 分钟。
- 支持一次性、每天、工作日、每周和自定义星期重复。
- 支持工作、生活、家庭等分类颜色。
- 支持关键词搜索已保存的待办事项。
- 提前 10 分钟提醒一次，到点再次提醒。
- 通知中可直接选择“已完成”或“延后 10 分钟”。
- 待办数据保存在本机，不需要注册账号，也不会上传待办内容。
- 节假日数据优先联网更新，并在本地缓存；没有网络时仍可使用内置节日和年度兜底数据。

流光日历适合需要同时查看中国农历、节假日和个人安排的用户。界面采用轻盈的半透明控件和响应式布局，适配不同尺寸的 Android 手机。

## 数据安全表单建议

- 是否收集数据：否
- 是否与第三方共享数据：否
- 是否使用账号：否
- 是否包含广告：否
- 是否允许用户请求删除数据：是，用户可以在应用内删除待办，或卸载/清除应用数据
- 数据是否加密传输：节假日请求使用 HTTPS；待办数据不离开设备
- 隐私政策：`https://github.com/lxlatsdkd/liuguang-calendar/blob/main/docs/privacy-policy.md`；也可将 `docs/privacy-policy.html` 部署到 GitHub Pages 后使用 Pages 地址

## 内容分级问卷建议

按实际问卷选择：无暴力、无色情、无赌博、无用户生成公开内容、无付费功能、无广告。目标受众选择普通用户，不要将应用声明为儿童专属应用。

## 应用访问

选择“不需要特殊访问权限”。应用无需账号登录，也没有必须提供给审核员的测试账号。

## 素材

- 应用图标：`docs/store-assets/liuguang-calendar-icon-512.png`
- 主界面截图：`outputs/calendar-v6-screen.png`
- 紧凑布局截图：`outputs/calendar-v6-screen-compact.png`
- 月份滑动截图：`outputs/calendar-v6-swipe.png`
- 搜索截图：`outputs/calendar-v6-search.png`
- 添加待办截图：`outputs/calendar-v6-add-sheet.png`
- 日期选择截图：`outputs/calendar-v6-date-calendar.png`
- 日期滚轮截图：`outputs/calendar-v6-date-wheel.png`
- 时间滚轮截图：`outputs/calendar-v6-time-wheel.png`
- 商店横幅：`docs/store-assets/google-play-feature-graphic.png`

## 上传文件

- Google Play App Bundle：`outputs/liuguang-calendar-v1.0.3-release.aab`
- 直接安装测试包：`outputs/liuguang-calendar-v1.0.3-release.apk`
