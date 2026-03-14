# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 6`，可演示“交易 -> 买手准入 -> 分层接单 -> 超时自动提价重派/终止 -> 交仓 -> 入仓质检 -> 合规清关 -> 国内物流 -> 签收”闭环：
  - Sprint 1：下单 -> 支付 -> 任务发布 -> 接单 -> 凭证上传 -> 后台审核 -> 时间线查询。
  - Sprint 2：运营建品 -> SKU 批量导入 -> 上架审核 -> H5 检索可售 SKU -> 下单库存校验 -> 缺货预警。
  - Sprint 3：购物车加购 -> 优惠券与税费估算 -> 购物车下单 -> 支付失败 -> 补偿支付 -> 任务发布。
  - Sprint 4：买手入驻申请 -> 后台审核 -> 买手档案激活 -> 分层任务可见 -> 接单准入校验 -> 凭证审核后信誉分更新。
  - Sprint 5：72h 超时任务扫描 -> 自动提价 -> 24h 频控 -> 自动重派 -> 达上限自动终止 -> 指标回传。
  - Sprint 6：买手交仓登记 -> 后台入仓质检 -> 清关资料提交/审核 -> 国内物流轨迹回传 -> 履约详情查询与签收指标。

## 新增能力
- 买手端（TypeScript + TSX）：
  - 买手入驻申请（身份后缀、服务区域、擅长品类、结算账户）。
  - 买手档案查询（审核状态、等级、信用分、奖惩积分）。
  - 任务大厅按 `buyerId` 分层过滤，显示任务等级/最低等级/区域/品类/SLA/提价与重派次数。
  - 新增交仓登记（`/api/v1/buyer/tasks/{taskId}/handover`），支持凭证审核通过后提交仓库编码。
- 管理后台（TypeScript + TSX）：
  - 新增买手入驻待审核列表与审核操作（APPROVE/REJECT）。
  - 新增 Sprint 5 控制台：超时候选任务查询、自动提价重派执行与结果明细。
  - 新增 Sprint 6 控制台：入仓扫描/质检、清关提交与审核、物流状态回传、履约详情查询。
- 数据平台（TypeScript + TSX）：
  - 新增买手履约指标：`buyer_pending_applications`、`buyer_approved_total`、`task_timeout_unaccepted_72h`、等级分布。
  - 新增动态提价指标：`timeout_candidates_total`、`auto_markup_applied_total`、`task_redispatch_total`、`task_timeout_terminated_total`、`repriced_task_accept_rate`。
  - 新增仓配清关指标：`warehouse_inbound_completed_total`、`customs_success_rate`、`shipment_signed_total`、`signed_within_7_15_days_rate`。
- 后端（Java 8 + Spring MVC）：
  - 新增买手入驻接口（`/api/v1/buyer/onboarding/applications`）与买手档案接口（`/api/v1/buyer/profile/{buyerId}`）。
  - 新增后台买手审核接口（`/api/v1/admin/buyer/onboarding/*`）与买手履约指标接口（`/api/v1/admin/metrics/buyer-fulfillment`）。
  - 新增超时任务自动提价引擎与管理接口：`/api/v1/admin/tasks/timeout-candidates`、`/api/v1/admin/tasks/timeout-reprice/run`、`/api/v1/admin/metrics/dynamic-pricing`。
  - 任务服务支持 24h 频控、最多 3 次自动提价、20% 封顶、重派计数与终止原因留痕。
  - 新增仓配清关履约接口：`/api/v1/admin/fulfillment/*`、`/api/v1/orders/{orderId}/fulfillment`、`/api/v1/admin/metrics/fulfillment`。
  - 订单状态新增 `CUSTOMS_CLEARANCE`，用于表达合规清关阶段。
- MySQL：
  - 新增 `db/mysql/V4__sprint4_buyer_fulfillment.sql`，包含 `buyer_onboarding_application`、`buyer_profile`，并扩展 `procurement_task` 分层字段与索引。
  - 新增 `db/mysql/V5__sprint5_dynamic_markup_engine.sql`，扩展 `procurement_task` 的提价执行字段与超时扫描索引。
  - 新增 `db/mysql/V6__sprint6_warehouse_customs_logistics.sql`，落地 `warehouse_inbound`、`customs_clearance_record`、`shipment_tracking` 三张履约中段表与索引。

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

## 运行与测试状态（2026-03-15）
前端（可执行）：
- `cd frontend && npm run test`：通过（1 文件 / 3 用例）
- `cd frontend && npm run typecheck`：通过
- `cd frontend && npm run build`：通过

后端（当前环境阻塞）：
- `mvn -f backend/pom.xml test`：失败，`mvn: command not found`
- `java -version`：失败，未安装 Java Runtime

## 风险与下一步
- 风险：本地缺少 JDK8/Maven，后端自动化测试无法在当前机器完成。
- 风险：Sprint 6 的清关与物流状态当前由后台人工回传，尚未接入关务/物流系统实时回调。
- 下一步建议：推进 `Roadmap Sprint 7`（售后与风控闭环：缺货替代、退款、真伪争议与仲裁）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 6`。
- 合规边界：仅实现合规清关路径相关字段与状态，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
