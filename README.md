# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 3`，可演示交易闭环：
  - Sprint 1：下单 -> 支付 -> 任务发布 -> 接单 -> 凭证上传 -> 后台审核 -> 时间线查询。
  - Sprint 2：运营建品 -> SKU 批量导入 -> 上架审核 -> H5 检索可售 SKU -> 下单库存校验 -> 缺货预警。
  - Sprint 3：购物车加购 -> 优惠券与税费估算 -> 购物车下单 -> 支付失败 -> 补偿支付 -> 任务发布。

## 新增能力
- H5 用户端（TypeScript + TSX）：
  - 购物车管理（加购、刷新、移除）。
  - 结算报价（商品金额、优惠券抵扣、税费估算、应付金额）。
  - 支付场景模拟（成功/失败）与补偿支付重试。
- 数据平台（TypeScript + TSX）：
  - 新增 `payment_failed` 与 `payment_compensated` 指标卡。
- 后端（Java 8 + Spring MVC）：
  - 新增购物车与结算接口（`/api/v1/cart`、`/api/v1/checkout`）。
  - 支付接口支持失败场景与补偿支付（`/pay-compensate`）。
  - 交易服务支持优惠券校验与税费估算。
- MySQL：
  - 新增 `db/mysql/V3__sprint3_trade_payment_stability.sql`，包含 `user_cart_item`、`coupon_template`、`payment_compensation` 与索引。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 交易与支付入口：[OrderController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/OrderController.java)
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
- 风险：税费估算当前为统一估算税率模型，尚未接入细分税则策略。
- 下一步建议：推进 `Roadmap Sprint 4`（买手入驻审核、任务分层、接单 SLA、信誉分）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 3`。
- 合规边界：仅实现合规清关路径相关字段与状态，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
