# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 9`，可演示“交易 -> 买手履约 -> 动态提价 -> 仓配清关 -> 售后仲裁 -> 分账结算/对账 -> 会员增长复购”主链路：
  - Sprint 1：下单/支付/任务发布/接单/凭证审核/时间线。
  - Sprint 2：商品与库存中台（SPU/SKU、上架审核、缺货预警）。
  - Sprint 3：购物车、优惠券、税费估算、支付失败补偿。
  - Sprint 4：买手入驻审核、任务分层、接单准入、信誉分沉淀。
  - Sprint 5：72h 超时自动提价重派与终止规则。
  - Sprint 6：交仓、入仓质检、合规清关、物流回传、签收指标。
  - Sprint 7：缺货工单、用户替代/部分退款决策、真伪争议、后台仲裁与风控指标。
  - Sprint 8：签收后自动分账、买手结算申请、后台放款确认、对账报表与结算指标。
  - Sprint 9：会员分层、运营活动触达、复购推荐、增长指标归因。

## 新增能力（Sprint 9）
- 用户端 H5（TypeScript + TSX）：
  - 新增“会员与复购增长中心”：会员等级、活动券包、复购/同类推荐商品，并可一键加购。
  - 新增接口对接：
    - `GET /api/v1/growth/membership`
    - `GET /api/v1/growth/coupons`
    - `GET /api/v1/growth/recommendations`
- 管理后台（TypeScript + TSX）：
  - 新增“运营增长活动台”：活动创建、活动列表、按用户批量触达。
  - 新增接口对接：
    - `POST /api/v1/admin/growth/campaigns`
    - `GET /api/v1/admin/growth/campaigns`
    - `POST /api/v1/admin/growth/campaigns/{campaignId}/publish`
- 数据平台（TypeScript + TSX）：
  - 新增增长指标：`membership_profile_total`、`growth_campaign_active_total`、`growth_touch_sent_total`、`growth_coupon_used_order_total`、`repurchase_user_30d_total`、`repurchase_rate_30d`。
- 后端（Java 8 + Spring MVC）：
  - 新增 `GrowthController`、`GrowthService`、`GrowthRepository`、增长活动 DTO/领域模型。
  - 扩展 `OrderService/OrderRepository`，支持订单优惠券与活动归因（`applied_coupon_code`、`applied_campaign_id`）。
  - 扩展 `MetricsService` 与 `AdminController` 输出增长指标和活动管理能力。
- MySQL：
  - 新增 `db/mysql/V9__sprint9_growth_repurchase.sql`，落地会员画像、增长活动、活动触达表，并扩展订单增长归因字段与索引。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 增长用户端入口：[GrowthController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/GrowthController.java)
- 后台入口：[AdminController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/AdminController.java)
- 增长服务入口：[GrowthService.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/service/GrowthService.java)
- MySQL 配置：[application.yml](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/resources/application.yml)

## 运行与测试状态（2026-03-15）
前端（可执行）：
- `cd frontend && npm run test`：通过（1 文件 / 4 用例）
- `cd frontend && npm run typecheck`：通过
- `cd frontend && npm run build`：通过

后端（当前环境阻塞）：
- `mvn -f backend/pom.xml test`：失败，`mvn: command not found`
- `java -version`：失败，未安装 Java Runtime

## 风险与下一步
- 风险：当前机器缺少 JDK8/Maven，后端自动化测试无法本地执行。
- 风险：Sprint 9 触达为平台内记录，尚未接外部短信/Push 通道。
- 风险：Sprint 9 推荐策略为规则型 V1，召回与转化仍需后续优化。
- 下一步建议：推进 `Roadmap Sprint 10`（稳定性优化、A/B 实验与成本时效优化）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 9`。
- 合规边界：仅实现合规清关路径下的增长与复购能力，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
