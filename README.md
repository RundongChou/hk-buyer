# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 8`，可演示“交易 -> 买手履约 -> 动态提价 -> 仓配清关 -> 售后仲裁 -> 分账结算/对账”主链路：
  - Sprint 1：下单/支付/任务发布/接单/凭证审核/时间线。
  - Sprint 2：商品与库存中台（SPU/SKU、上架审核、缺货预警）。
  - Sprint 3：购物车、优惠券、税费估算、支付失败补偿。
  - Sprint 4：买手入驻审核、任务分层、接单准入、信誉分沉淀。
  - Sprint 5：72h 超时自动提价重派与终止规则。
  - Sprint 6：交仓、入仓质检、合规清关、物流回传、签收指标。
  - Sprint 7：缺货工单、用户替代/部分退款决策、真伪争议、后台仲裁与风控指标。
  - Sprint 8：签收后自动分账、买手结算申请、后台放款确认、对账报表与结算指标。

## 新增能力（Sprint 8）
- 买手端（TypeScript + TSX）：
  - 新增结算中心：查询买手台账并提交放款申请。
  - 新增接口对接：
    - `GET /api/v1/buyer/settlements`
    - `POST /api/v1/buyer/settlements/{ledgerId}/request-payout`
- 管理后台（TypeScript + TSX）：
  - 新增财务结算与对账台：待放款台账查询、放款确认、对账提交、报表查看。
  - 新增接口对接：
    - `GET /api/v1/admin/settlements/pending-payout`
    - `POST /api/v1/admin/settlements/{ledgerId}/complete-payout`
    - `POST /api/v1/admin/settlements/{ledgerId}/reconcile`
    - `GET /api/v1/admin/settlements/reconciliation/report`
- 数据平台（TypeScript + TSX）：
  - 新增结算指标：`settlement_ledger_total`、`settlement_pending_total`、`settlement_payout_requested_total`、`settlement_settled_total`、`settlement_completion_rate`、`settlement_reconciliation_accuracy_rate`。
- 后端（Java 8 + Spring MVC）：
  - 新增 `SettlementService`、`SettlementRepository`、`SettlementLedger`。
  - 在签收节点自动生成分账台账，形成“签收 -> 分账”自动化链路。
  - 扩展 `BuyerController`/`AdminController`/`MetricsService` 输出结算与对账接口。
- MySQL：
  - 新增 `db/mysql/V8__sprint8_settlement_finance.sql`，落地 `settlement_ledger` 表与索引，并扩展 `buyer_profile.settlement_account`。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 订单与售后入口：[OrderController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/OrderController.java)
- 买手任务入口：[BuyerTaskController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/BuyerTaskController.java)
- 管理后台入口：[AdminController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/AdminController.java)
- 结算服务入口：[SettlementService.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/service/SettlementService.java)
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
- 风险：Sprint 8 仅实现平台内结算状态流转，尚未接入真实出款/支付清算回单。
- 风险：清关与物流仍为后台人工回传，未接关务/物流实时回调。
- 下一步建议：推进 `Roadmap Sprint 9`（运营增长与复购触达）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 8`。
- 合规边界：仅实现合规清关与售后风控流程，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
