# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 1` 的最小交易闭环切片（下单 -> 支付 -> 任务发布 -> 接单 -> 凭证上传 -> 后台审核 -> 用户时间线查询）。

## 新增能力
- H5 用户端（TypeScript + TSX）：创建订单、支付订单、查询订单详情与时间线。
- 买手端（TypeScript + TSX）：任务列表、接单、提交采购凭证。
- 管理后台（TypeScript + TSX）：待审核凭证列表、审核通过/驳回。
- 数据平台（TypeScript + TSX）：漏斗指标 `payment_success/task_accepted/proof_submitted`。
- 后端（Java 8 + Spring MVC）：订单、任务、凭证、审核、漏斗 API。
- MySQL：`db/mysql/V1__sprint1_mvp.sql` 提供订单/任务/凭证/时间线表与索引。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- MySQL 配置：[application.yml](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/resources/application.yml)

## 运行与测试状态（2026-03-14）
前端（可执行）：
- `cd frontend && npm install`：通过
- `cd frontend && npm run test`：通过（1 文件/2 用例）
- `cd frontend && npm run typecheck`：通过
- `cd frontend && npm run build`：通过

后端（当前环境阻塞）：
- `mvn -f backend/pom.xml test`：失败，`mvn: command not found`
- `java -version`：失败，未安装 Java Runtime

## 风险与下一步
- 风险：当前执行环境缺少 JDK8/Maven，后端自动化测试未能在本地完成。
- 下一步：补齐 Java8 + Maven 或在 CI 容器执行后端测试，并继续推进 Roadmap Sprint 2（商品与库存中台）。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 1`。
- 合规边界：仅实现合规清关路径的状态占位与可追踪链路，未实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（DDL 与索引脚本在 `db/mysql`）。
