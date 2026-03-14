# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 2`，可演示链路：
  - Sprint 1：下单 -> 支付 -> 任务发布 -> 接单 -> 凭证上传 -> 后台审核 -> 时间线查询。
  - Sprint 2：运营建品 -> SKU 批量导入 -> 上架审核 -> H5 检索可售 SKU -> 下单库存校验 -> 缺货预警。

## 新增能力
- H5 用户端（TypeScript + TSX）：
  - SKU 检索（仅上架商品）、库存状态展示、选中 SKU 回填下单。
  - 下单链路兼容旧参数，同时优先使用商品中心价格策略。
- 管理后台（TypeScript + TSX）：
  - SPU 创建、SKU 批量导入、SKU 上架审核、库存调整、缺货预警。
  - 保留 Sprint 1 凭证审核能力。
- 数据平台（TypeScript + TSX）：
  - 新增 `sku_total/sku_published/sku_out_of_stock` 指标卡。
- 后端（Java 8 + Spring MVC）：
  - 新增商品与库存中台 API（建品、审核、库存、检索、预警、指标）。
  - 下单接口新增“上架状态 + 库存可售 + 系统定价”校验，防止误售。
- MySQL：
  - 新增 `db/mysql/V2__sprint2_catalog_inventory.sql`，包含 `sku_spu/sku_item/sku_price_policy/stock_snapshot/sku_publish_audit_log` 与索引。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 商品与库存后端入口：[CatalogAdminController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/CatalogAdminController.java)
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
- 风险：当前 SKU 检索仍为基础关键词匹配，后续大规模数据需进一步优化检索和分页策略。
- 下一步建议：推进 `Roadmap Sprint 3`（购物车、优惠券、税费估算、支付失败补偿）并补齐后端 CI 测试容器。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 2`。
- 合规边界：仅实现合规清关路径相关字段与状态，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
