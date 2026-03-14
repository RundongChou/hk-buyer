# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 7`，可演示“交易 -> 买手履约 -> 动态提价 -> 仓配清关 -> 售后仲裁”主链路：
  - Sprint 1：下单/支付/任务发布/接单/凭证审核/时间线。
  - Sprint 2：商品与库存中台（SPU/SKU、上架审核、缺货预警）。
  - Sprint 3：购物车、优惠券、税费估算、支付失败补偿。
  - Sprint 4：买手入驻审核、任务分层、接单准入、信誉分沉淀。
  - Sprint 5：72h 超时自动提价重派与终止规则。
  - Sprint 6：交仓、入仓质检、合规清关、物流回传、签收指标。
  - Sprint 7：缺货工单、用户替代/部分退款决策、真伪争议、后台仲裁与风控指标。

## 新增能力（Sprint 7）
- 买手端（TypeScript + TSX）：
  - 新增缺货上报：`POST /api/v1/buyer/tasks/{taskId}/stockout-report`。
  - 支持填写缺货原因、替代建议、建议部分退款金额。
- 用户端 H5（TypeScript + TSX）：
  - 新增售后中心：发起真伪争议、查看售后工单、提交缺货方案决策。
  - 新增接口对接：
    - `POST /api/v1/orders/{orderId}/after-sale/authenticity`
    - `GET /api/v1/orders/{orderId}/after-sale/cases`
    - `POST /api/v1/orders/{orderId}/after-sale/cases/{caseId}/decision`
- 管理后台（TypeScript + TSX）：
  - 新增售后与风控仲裁台：待仲裁工单查询与仲裁提交。
  - 新增接口对接：
    - `GET /api/v1/admin/after-sale/cases/pending`
    - `POST /api/v1/admin/after-sale/cases/{caseId}/arbitrate`
- 数据平台（TypeScript + TSX）：
  - 新增售后风控指标：`after_sale_open_cases_total`、`after_sale_pending_arbitration_total`、`after_sale_resolved_total`、`counterfeit_complaint_rate`、`order_cancel_rate`。
- 后端（Java 8 + Spring MVC）：
  - 新增 `AfterSaleService`、`AfterSaleRepository`、`AfterSaleCase`。
  - 新增订单状态 `AFTER_SALE_PROCESSING`，实现售后工单流程与时间线留痕。
  - 扩展 `MetricsService` 输出 `after-sale-risk` 指标。
- MySQL：
  - 新增 `db/mysql/V7__sprint7_after_sale_risk.sql`，落地 `after_sale_case` 表与索引。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 订单与售后入口：[OrderController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/OrderController.java)
- 买手任务入口：[BuyerTaskController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/BuyerTaskController.java)
- 管理后台入口：[AdminController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/AdminController.java)
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
- 风险：Sprint 7 已实现工单与仲裁留痕，但未接入真实退款资金落账与财务对账。
- 风险：清关与物流仍为后台人工回传，未接关务/物流实时回调。
- 下一步建议：推进 `Roadmap Sprint 8`（分账结算与财务对账闭环）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 7`。
- 合规边界：仅实现合规清关与售后风控流程，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
