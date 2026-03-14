# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 4`，可演示“交易 -> 买手准入 -> 分层接单 -> 信誉沉淀”闭环：
  - Sprint 1：下单 -> 支付 -> 任务发布 -> 接单 -> 凭证上传 -> 后台审核 -> 时间线查询。
  - Sprint 2：运营建品 -> SKU 批量导入 -> 上架审核 -> H5 检索可售 SKU -> 下单库存校验 -> 缺货预警。
  - Sprint 3：购物车加购 -> 优惠券与税费估算 -> 购物车下单 -> 支付失败 -> 补偿支付 -> 任务发布。
  - Sprint 4：买手入驻申请 -> 后台审核 -> 买手档案激活 -> 分层任务可见 -> 接单准入校验 -> 凭证审核后信誉分更新。

## 新增能力
- 买手端（TypeScript + TSX）：
  - 买手入驻申请（身份后缀、服务区域、擅长品类、结算账户）。
  - 买手档案查询（审核状态、等级、信用分、奖惩积分）。
  - 任务大厅按 `buyerId` 分层过滤，显示任务等级/最低等级/区域/品类/SLA。
- 管理后台（TypeScript + TSX）：
  - 新增买手入驻待审核列表与审核操作（APPROVE/REJECT）。
- 数据平台（TypeScript + TSX）：
  - 新增买手履约指标：`buyer_pending_applications`、`buyer_approved_total`、`task_timeout_unaccepted_72h`、等级分布。
- 后端（Java 8 + Spring MVC）：
  - 新增买手入驻接口（`/api/v1/buyer/onboarding/applications`）与买手档案接口（`/api/v1/buyer/profile/{buyerId}`）。
  - 新增后台买手审核接口（`/api/v1/admin/buyer/onboarding/*`）与买手履约指标接口（`/api/v1/admin/metrics/buyer-fulfillment`）。
  - 任务服务新增分层派发与接单准入校验；凭证审核后自动更新买手信誉分、等级和奖惩积分。
- MySQL：
  - 新增 `db/mysql/V4__sprint4_buyer_fulfillment.sql`，包含 `buyer_onboarding_application`、`buyer_profile`，并扩展 `procurement_task` 分层字段与索引。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 交易与支付入口：[OrderController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/OrderController.java)
- 买手入驻入口：[BuyerController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/BuyerController.java)
- 购物车入口：[CartController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/CartController.java)
- 结算入口：[CheckoutController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/CheckoutController.java)
- MySQL 配置：[application.yml](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/resources/application.yml)

## 运行与测试状态（2026-03-14）
前端（可执行）：
- `cd frontend && npm run test`：通过（1 文件 / 2 用例）
- `cd frontend && npm run typecheck`：通过
- `cd frontend && npm run build`：通过

后端（当前环境阻塞）：
- `mvn -f backend/pom.xml test`：失败，`mvn: command not found`
- `java -version`：失败，未安装 Java Runtime

## 风险与下一步
- 风险：本地缺少 JDK8/Maven，后端自动化测试无法在当前机器完成。
- 风险：任务分层当前按订单金额和首个 SKU 品类生成，后续可升级为多 SKU 加权分层策略。
- 下一步建议：推进 `Roadmap Sprint 5`（72h 自动提价执行器、24h 频控、重派策略）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 4`。
- 合规边界：仅实现合规清关路径相关字段与状态，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
