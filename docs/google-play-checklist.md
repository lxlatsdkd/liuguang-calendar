# Google Play 上架清单

## 本地已完成

- [x] 正式签名 Release APK
- [x] 正式签名 Release AAB
- [x] `targetSdk 36`
- [x] `versionCode 13`、`versionName 1.0.3`
- [x] 21 项单元测试通过
- [x] 应用图标和商店截图
- [x] 隐私政策页面草稿
- [x] 商店文案和数据安全填表建议

## 需要账号持有人完成

- [ ] 注册或登录 Google Play Console 开发者账号
- [ ] 完成开发者身份验证和开发者注册费支付
- [ ] 确认永久包名：`com.example.liuguangcalendar`
- [ ] 创建应用并选择默认语言、应用类别和联系邮箱
- [ ] 填写商店资料、内容分级、目标受众和数据安全表单
- [ ] 在 Play Console 设置中填写隐私政策 URL：`https://github.com/lxlatsdkd/liuguang-calendar/blob/main/docs/privacy-policy.md`
- [ ] 上传 `outputs/liuguang-calendar-v1.0.3-release.aab` 到内部测试
- [ ] 在 Pixel 10 Pro XL 上安装内部测试版本并验证提醒、通知和待办持久化
- [ ] 根据 Play Console 对个人开发者账号的测试要求完成测试后，再申请公开发布

## 重要事项

包名一旦首次发布，后续不能更换。后续更新必须继续使用同一个签名密钥。不要把 `keystore.properties`、`.jks` 文件或密码提交到 GitHub。
