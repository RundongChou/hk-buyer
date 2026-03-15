# hk-buyer

香港买手跨境代购平台（前后端分离）单仓工程。

## 当前完成度
- 当前已完成到 `Roadmap Sprint 10`，形成“交易 -> 买手履约 -> 动态提价 -> 仓配清关 -> 售后仲裁 -> 分账结算/对账 -> 会员增长复购 -> 稳态运营优化”主链路。
- Sprint 10 增量能力已打通“实验配置 -> 分流派单 -> 指标观测 -> 护栏告警 -> 策略建议”闭环。

## 新增能力（Sprint 10）
- 管理后台（TypeScript + TSX）：
  - 新增“稳态运营与优化台”，支持实验创建/激活、分流记录查询、护栏告警评估与 OPEN 告警查看。
  - 新增接口：
    - `POST /api/v1/admin/ops/experiments`
    - `POST /api/v1/admin/ops/experiments/{experimentId}/activate`
    - `GET /api/v1/admin/ops/experiments/active`
    - `GET /api/v1/admin/ops/experiments/{experimentId}/assignments`
    - `POST /api/v1/admin/ops/alerts/evaluate`
    - `GET /api/v1/admin/ops/alerts/open`
- 数据平台（TypeScript + TSX）：
  - 新增稳态优化指标：实验规模、Control/Treatment 7-15天履约率、分组平台服务费率、OPEN 告警数、策略建议。
  - 新增接口：`GET /api/v1/admin/metrics/ops-optimization`。
- 后端（Java 8 + Spring MVC）：
  - 新增 `OptimizationService`、`OptimizationRepository` 与实验/告警 DTO、领域模型。
  - 扩展 `OrderService`：支付后发布任务接入 A/B 分流策略（加价与 SLA 参数可追溯）。
  - 扩展 `AdminController` 与 `MetricsService` 输出 Sprint 10 管理与指标能力。
- MySQL：
  - 新增 `db/mysql/V10__sprint10_ops_optimization.sql`，落地 `ops_experiment`、`ops_experiment_assignment`、`ops_alert_event`。

## 关键入口
- 用户端入口：[h5-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/h5-main.tsx)
- 买手端入口：[buyer-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/buyer-main.tsx)
- 管理后台入口：[admin-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/admin-main.tsx)
- 数据平台入口：[data-main.tsx](/Users/yinbin/PycharmProjects/hk-buyer/frontend/src/data-main.tsx)
- Spring MVC 入口：[HkBuyerApplication.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/HkBuyerApplication.java)
- 后台入口：[AdminController.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/api/AdminController.java)
- 稳态优化服务入口：[OptimizationService.java](/Users/yinbin/PycharmProjects/hk-buyer/backend/src/main/java/com/hkbuyer/service/OptimizationService.java)
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
- 风险：Sprint 10 告警为平台内事件，尚未接外部通知通道与自动工单联动。
- 风险：A/B 策略建议仍为规则型，需更多样本验证稳定性。
- 下一步建议：进入 Roadmap 后续运营迭代阶段，补齐后端 CI 环境并按实验样本滚动调参。

## Roadmap 对齐状态
- 当前做到：`Roadmap Sprint 10`。
- 合规边界：仅实现合规清关路径下的运营优化能力，不实现任何绕关/走私能力。

## 技术栈符合性
- 前端：新增页面和交互均为 `TypeScript + TSX`。
- 后端：`Java 8 + Spring MVC`（Spring Boot MVC）。
- 数据库：`MySQL`（迁移脚本在 `db/mysql`）。
