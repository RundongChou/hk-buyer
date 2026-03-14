import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { apiRequest } from './api';
import './styles.css';

interface ProofItem {
  proofId: number;
  taskId: number;
  buyerId: number;
  storeName: string;
  receiptUrl: string;
  batchNo: string;
  expiryDate: string;
  auditStatus: string;
}

interface CatalogSku {
  skuId: number;
  spuId: number;
  skuName: string;
  specValue: string;
  publishStatus: string;
  finalPrice: string;
  availableQty: number;
  stockStatus: string;
  saleable: boolean;
}

interface BuyerOnboardingItem {
  applicationId: number;
  buyerId: number;
  realName: string;
  idCardSuffix: string;
  serviceRegion: string;
  specialtyCategory: string;
  settlementAccount: string;
  applicationStatus: string;
  createdAt: string;
}

interface TimeoutCandidateItem {
  taskId: number;
  orderId: number;
  taskStatus: string;
  acceptDeadline: string;
  suggestedMarkup: string;
  markupAppliedCount: number;
  redispatchCount: number;
  nextMarkupEligibleAt: string | null;
  terminalReason: string | null;
  canAutoReprice: boolean;
  frequencyLimited: boolean;
  alreadyAtCapOrLimit: boolean;
}

interface TimeoutRepriceResult {
  runAt: string;
  batchSize: number;
  repricedCount: number;
  skippedFrequencyCount: number;
  terminatedCount: number;
  concurrencySkippedCount: number;
  details: Array<Record<string, unknown>>;
}

interface FulfillmentDetail {
  orderId: number;
  orderStatus: string;
  warehouse: Record<string, unknown> | null;
  customs: Record<string, unknown> | null;
  shipment: Record<string, unknown> | null;
  timeline: Array<Record<string, unknown>>;
}

function AdminApp(): JSX.Element {
  const [proofs, setProofs] = useState<ProofItem[]>([]);
  const [buyerApplications, setBuyerApplications] = useState<BuyerOnboardingItem[]>([]);
  const [proofId, setProofId] = useState('');
  const [adminId, setAdminId] = useState('70001');
  const [decision, setDecision] = useState('APPROVE');
  const [comment, setComment] = useState('符合采购凭证要求');
  const [buyerApplicationId, setBuyerApplicationId] = useState('');
  const [buyerDecision, setBuyerDecision] = useState('APPROVE');
  const [buyerComment, setBuyerComment] = useState('资质通过，允许接单');

  const [spuName, setSpuName] = useState('港版维C精华');
  const [brandName, setBrandName] = useState('HK Beauty Lab');
  const [categoryName, setCategoryName] = useState('药妆护肤');
  const [spuId, setSpuId] = useState('');

  const [importSkuName, setImportSkuName] = useState('港版维C精华 30ml');
  const [importSpec, setImportSpec] = useState('30ml');
  const [basePrice, setBasePrice] = useState('168.00');
  const [serviceFeeRate, setServiceFeeRate] = useState('0.1200');
  const [availableQty, setAvailableQty] = useState('20');
  const [alertThreshold, setAlertThreshold] = useState('5');

  const [pendingSkus, setPendingSkus] = useState<CatalogSku[]>([]);
  const [skuAuditId, setSkuAuditId] = useState('');
  const [skuDecision, setSkuDecision] = useState('APPROVE');
  const [skuComment, setSkuComment] = useState('商品资料完整，允许上架');

  const [stockSkuId, setStockSkuId] = useState('');
  const [stockQty, setStockQty] = useState('15');
  const [stockAlert, setStockAlert] = useState('5');
  const [stockReason, setStockReason] = useState('manual_adjust');
  const [outOfStockAlerts, setOutOfStockAlerts] = useState<CatalogSku[]>([]);
  const [timeoutCandidates, setTimeoutCandidates] = useState<TimeoutCandidateItem[]>([]);
  const [timeoutRepriceResult, setTimeoutRepriceResult] = useState<TimeoutRepriceResult | null>(null);
  const [inboundTaskId, setInboundTaskId] = useState('');
  const [inboundWarehouseCode, setInboundWarehouseCode] = useState('HK-WH-01');
  const [qcDecision, setQcDecision] = useState('PASS');
  const [qcNote, setQcNote] = useState('抽检通过');
  const [customsOrderId, setCustomsOrderId] = useState('');
  const [declarationNo, setDeclarationNo] = useState('DEC-20260315-001');
  const [complianceChannel, setComplianceChannel] = useState('GENERAL_TRADE');
  const [customsDecision, setCustomsDecision] = useState('APPROVE');
  const [customsComment, setCustomsComment] = useState('资料完整，予以放行');
  const [shipmentOrderId, setShipmentOrderId] = useState('');
  const [shipmentCarrier, setShipmentCarrier] = useState('SF-EXPRESS');
  const [shipmentTrackingNo, setShipmentTrackingNo] = useState('SF202603150001');
  const [shipmentStatus, setShipmentStatus] = useState('IN_TRANSIT');
  const [shipmentLatestNode, setShipmentLatestNode] = useState('深圳分拨中心已揽收');
  const [fulfillmentOrderId, setFulfillmentOrderId] = useState('');
  const [fulfillmentDetail, setFulfillmentDetail] = useState<FulfillmentDetail | null>(null);

  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadProofs = async (): Promise<void> => {
    try {
      const payload = await apiRequest<ProofItem[]>('/api/v1/admin/proofs/pending');
      setProofs(payload);
      if (!proofId && payload.length > 0) {
        setProofId(String(payload[0].proofId));
      }
    } catch (error) {
      setMessage(String(error));
    }
  };

  const loadPendingSkus = async (): Promise<void> => {
    try {
      const payload = await apiRequest<CatalogSku[]>('/api/v1/admin/catalog/skus/pending-audit');
      setPendingSkus(payload);
      if (!skuAuditId && payload.length > 0) {
        setSkuAuditId(String(payload[0].skuId));
      }
      if (!stockSkuId && payload.length > 0) {
        setStockSkuId(String(payload[0].skuId));
      }
    } catch (error) {
      setMessage(String(error));
    }
  };

  const loadOutOfStockAlerts = async (): Promise<void> => {
    try {
      const payload = await apiRequest<CatalogSku[]>('/api/v1/admin/inventory/alerts/out-of-stock');
      setOutOfStockAlerts(payload);
    } catch (error) {
      setMessage(String(error));
    }
  };

  const loadPendingBuyerApplications = async (): Promise<void> => {
    try {
      const payload = await apiRequest<BuyerOnboardingItem[]>('/api/v1/admin/buyer/onboarding/pending');
      setBuyerApplications(payload);
      if (!buyerApplicationId && payload.length > 0) {
        setBuyerApplicationId(String(payload[0].applicationId));
      }
    } catch (error) {
      setMessage(String(error));
    }
  };

  const loadTimeoutCandidates = async (): Promise<void> => {
    try {
      const payload = await apiRequest<TimeoutCandidateItem[]>('/api/v1/admin/tasks/timeout-candidates');
      setTimeoutCandidates(payload);
    } catch (error) {
      setMessage(String(error));
    }
  };

  useEffect(() => {
    void (async () => {
      setBusy(true);
      await loadProofs();
      await loadPendingSkus();
      await loadOutOfStockAlerts();
      await loadPendingBuyerApplications();
      await loadTimeoutCandidates();
      setBusy(false);
      setMessage('管理数据已初始化');
    })();
  }, []);

  const auditProof = async (): Promise<void> => {
    if (!proofId) {
      setMessage('请输入凭证 ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest(`/api/v1/admin/proofs/${proofId}/audit`, {
        method: 'POST',
        body: {
          adminId: Number(adminId),
          decision,
          comment
        }
      });
      setMessage('凭证审核完成');
      await loadProofs();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const createSpu = async (): Promise<void> => {
    setBusy(true);
    try {
      const payload = await apiRequest<{ spuId: number }>('/api/v1/admin/catalog/spus', {
        method: 'POST',
        body: {
          spuName,
          brandName,
          categoryName
        }
      });
      setSpuId(String(payload.spuId));
      setMessage(`SPU 创建成功，spuId=${payload.spuId}`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const batchImportSku = async (): Promise<void> => {
    if (!spuId) {
      setMessage('请先创建 SPU 或输入 spuId');
      return;
    }
    setBusy(true);
    try {
      const payload = await apiRequest<{ importedCount: number; skuIds: number[] }>('/api/v1/admin/catalog/skus/batch-import', {
        method: 'POST',
        body: {
          spuId: Number(spuId),
          items: [
            {
              skuName: importSkuName,
              specValue: importSpec,
              basePrice: Number(basePrice),
              serviceFeeRate: Number(serviceFeeRate),
              availableQty: Number(availableQty),
              alertThreshold: Number(alertThreshold)
            }
          ]
        }
      });
      setMessage(`批量导入完成，新增 SKU ${payload.importedCount} 条：${payload.skuIds.join(',')}`);
      await loadPendingSkus();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const auditSku = async (): Promise<void> => {
    if (!skuAuditId) {
      setMessage('请输入 SKU ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest(`/api/v1/admin/catalog/skus/${skuAuditId}/publish-audit`, {
        method: 'POST',
        body: {
          adminId: Number(adminId),
          decision: skuDecision,
          comment: skuComment
        }
      });
      setMessage('SKU 上架审核完成');
      await loadPendingSkus();
      await loadOutOfStockAlerts();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const adjustStock = async (): Promise<void> => {
    if (!stockSkuId) {
      setMessage('请输入 SKU ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest(`/api/v1/admin/catalog/skus/${stockSkuId}/stock-adjust`, {
        method: 'POST',
        body: {
          availableQty: Number(stockQty),
          alertThreshold: Number(stockAlert),
          reason: stockReason
        }
      });
      setMessage('库存调整完成');
      await loadOutOfStockAlerts();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const auditBuyerApplication = async (): Promise<void> => {
    if (!buyerApplicationId) {
      setMessage('请输入入驻申请 ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest(`/api/v1/admin/buyer/onboarding/${buyerApplicationId}/audit`, {
        method: 'POST',
        body: {
          adminId: Number(adminId),
          decision: buyerDecision,
          comment: buyerComment
        }
      });
      setMessage('买手入驻审核完成');
      await loadPendingBuyerApplications();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const runTimeoutReprice = async (): Promise<void> => {
    setBusy(true);
    try {
      const payload = await apiRequest<TimeoutRepriceResult>('/api/v1/admin/tasks/timeout-reprice/run', {
        method: 'POST'
      });
      setTimeoutRepriceResult(payload);
      await loadTimeoutCandidates();
      setMessage(`自动提价执行完成：提价 ${payload.repricedCount}，终止 ${payload.terminatedCount}`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const scanInbound = async (): Promise<void> => {
    if (!inboundTaskId) {
      setMessage('请输入任务 ID');
      return;
    }
    setBusy(true);
    try {
      const payload = await apiRequest<{ orderId: number }>('/api/v1/admin/fulfillment/inbound/scan', {
        method: 'POST',
        body: {
          taskId: Number(inboundTaskId),
          warehouseCode: inboundWarehouseCode,
          qcDecision,
          qcNote
        }
      });
      setCustomsOrderId(String(payload.orderId));
      setShipmentOrderId(String(payload.orderId));
      setFulfillmentOrderId(String(payload.orderId));
      setMessage('入仓扫描与质检结果已提交');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const submitCustoms = async (): Promise<void> => {
    if (!customsOrderId) {
      setMessage('请输入订单 ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest('/api/v1/admin/fulfillment/customs/submit', {
        method: 'POST',
        body: {
          orderId: Number(customsOrderId),
          declarationNo,
          complianceChannel
        }
      });
      setMessage('清关资料提交成功');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const reviewCustoms = async (): Promise<void> => {
    if (!customsOrderId) {
      setMessage('请输入订单 ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest('/api/v1/admin/fulfillment/customs/review', {
        method: 'POST',
        body: {
          orderId: Number(customsOrderId),
          decision: customsDecision,
          comment: customsComment
        }
      });
      setMessage('清关审核结果已提交');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const updateShipment = async (): Promise<void> => {
    if (!shipmentOrderId) {
      setMessage('请输入订单 ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest('/api/v1/admin/fulfillment/shipment/update', {
        method: 'POST',
        body: {
          orderId: Number(shipmentOrderId),
          carrier: shipmentCarrier,
          trackingNo: shipmentTrackingNo,
          shipmentStatus,
          latestNode: shipmentLatestNode
        }
      });
      setFulfillmentOrderId(shipmentOrderId);
      setMessage('物流轨迹已回传');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const loadFulfillmentDetail = async (): Promise<void> => {
    if (!fulfillmentOrderId) {
      setMessage('请输入订单 ID');
      return;
    }
    setBusy(true);
    try {
      const payload = await apiRequest<FulfillmentDetail>(`/api/v1/admin/fulfillment/orders/${Number(fulfillmentOrderId)}`);
      setFulfillmentDetail(payload);
      setMessage('履约详情已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container">
      <h1>管理后台</h1>

      <div className="card">
        <h2>Sprint 1 凭证审核</h2>
        <button onClick={loadProofs} disabled={busy}>刷新待审核凭证</button>
        <pre>{JSON.stringify(proofs, null, 2)}</pre>
        <div className="grid grid-2">
          <label>
            凭证 ID
            <input value={proofId} onChange={(e) => setProofId(e.target.value)} />
          </label>
          <label>
            管理员 ID
            <input value={adminId} onChange={(e) => setAdminId(e.target.value)} />
          </label>
          <label>
            审核结果
            <select value={decision} onChange={(e) => setDecision(e.target.value)}>
              <option value="APPROVE">APPROVE</option>
              <option value="REJECT">REJECT</option>
            </select>
          </label>
          <label>
            备注
            <textarea value={comment} onChange={(e) => setComment(e.target.value)} />
          </label>
        </div>
        <button onClick={auditProof} disabled={busy}>提交凭证审核</button>
      </div>

      <div className="card">
        <h2>Sprint 4 买手入驻审核</h2>
        <button onClick={loadPendingBuyerApplications} disabled={busy}>刷新待审核入驻申请</button>
        <pre>{JSON.stringify(buyerApplications, null, 2)}</pre>
        <div className="grid grid-2">
          <label>
            入驻申请 ID
            <input value={buyerApplicationId} onChange={(e) => setBuyerApplicationId(e.target.value)} />
          </label>
          <label>
            审核结果
            <select value={buyerDecision} onChange={(e) => setBuyerDecision(e.target.value)}>
              <option value="APPROVE">APPROVE</option>
              <option value="REJECT">REJECT</option>
            </select>
          </label>
          <label>
            审核备注
            <textarea value={buyerComment} onChange={(e) => setBuyerComment(e.target.value)} />
          </label>
        </div>
        <button onClick={auditBuyerApplication} disabled={busy}>提交买手审核</button>
      </div>

      <div className="card">
        <h2>Sprint 5 动态提价与重派</h2>
        <div className="grid grid-2">
          <button onClick={loadTimeoutCandidates} disabled={busy}>刷新超时候选任务</button>
          <button onClick={runTimeoutReprice} disabled={busy}>执行自动提价重派</button>
        </div>
        <h3>候选任务</h3>
        <pre>{JSON.stringify(timeoutCandidates, null, 2)}</pre>
        <h3>最近执行结果</h3>
        <pre>{JSON.stringify(timeoutRepriceResult, null, 2)}</pre>
      </div>

      <div className="card">
        <h2>Sprint 6 仓配清关链路</h2>
        <div className="grid grid-2">
          <label>
            入仓任务 ID
            <input value={inboundTaskId} onChange={(e) => setInboundTaskId(e.target.value)} />
          </label>
          <label>
            仓库编码
            <input value={inboundWarehouseCode} onChange={(e) => setInboundWarehouseCode(e.target.value)} />
          </label>
          <label>
            质检结果
            <select value={qcDecision} onChange={(e) => setQcDecision(e.target.value)}>
              <option value="PASS">PASS</option>
              <option value="FAIL">FAIL</option>
            </select>
          </label>
          <label>
            质检备注
            <textarea value={qcNote} onChange={(e) => setQcNote(e.target.value)} />
          </label>
        </div>
        <button onClick={scanInbound} disabled={busy}>提交入仓扫描</button>

        <div className="grid grid-2" style={{ marginTop: 12 }}>
          <label>
            清关订单 ID
            <input value={customsOrderId} onChange={(e) => setCustomsOrderId(e.target.value)} />
          </label>
          <label>
            申报单号
            <input value={declarationNo} onChange={(e) => setDeclarationNo(e.target.value)} />
          </label>
          <label>
            合规渠道
            <input value={complianceChannel} onChange={(e) => setComplianceChannel(e.target.value)} />
          </label>
          <label>
            清关审核
            <select value={customsDecision} onChange={(e) => setCustomsDecision(e.target.value)}>
              <option value="APPROVE">APPROVE</option>
              <option value="REJECT">REJECT</option>
            </select>
          </label>
          <label>
            清关备注
            <textarea value={customsComment} onChange={(e) => setCustomsComment(e.target.value)} />
          </label>
        </div>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          <button onClick={submitCustoms} disabled={busy}>提交清关资料</button>
          <button onClick={reviewCustoms} disabled={busy}>提交清关审核</button>
        </div>

        <div className="grid grid-2" style={{ marginTop: 12 }}>
          <label>
            物流订单 ID
            <input value={shipmentOrderId} onChange={(e) => setShipmentOrderId(e.target.value)} />
          </label>
          <label>
            承运商
            <input value={shipmentCarrier} onChange={(e) => setShipmentCarrier(e.target.value)} />
          </label>
          <label>
            运单号
            <input value={shipmentTrackingNo} onChange={(e) => setShipmentTrackingNo(e.target.value)} />
          </label>
          <label>
            物流状态
            <select value={shipmentStatus} onChange={(e) => setShipmentStatus(e.target.value)}>
              <option value="IN_TRANSIT">IN_TRANSIT</option>
              <option value="SIGNED">SIGNED</option>
            </select>
          </label>
          <label>
            最新节点
            <textarea value={shipmentLatestNode} onChange={(e) => setShipmentLatestNode(e.target.value)} />
          </label>
        </div>
        <button onClick={updateShipment} disabled={busy}>回传物流状态</button>

        <div className="grid grid-2" style={{ marginTop: 12 }}>
          <label>
            履约查询订单 ID
            <input value={fulfillmentOrderId} onChange={(e) => setFulfillmentOrderId(e.target.value)} />
          </label>
          <button onClick={loadFulfillmentDetail} disabled={busy}>查询履约详情</button>
        </div>
        <pre>{JSON.stringify(fulfillmentDetail, null, 2)}</pre>
      </div>

      <div className="card grid grid-2">
        <h2>商品中台：创建 SPU</h2>
        <label>
          SPU 名称
          <input value={spuName} onChange={(e) => setSpuName(e.target.value)} />
        </label>
        <label>
          品牌
          <input value={brandName} onChange={(e) => setBrandName(e.target.value)} />
        </label>
        <label>
          类目
          <input value={categoryName} onChange={(e) => setCategoryName(e.target.value)} />
        </label>
        <label>
          SPU ID
          <input value={spuId} onChange={(e) => setSpuId(e.target.value)} placeholder="可手工输入已有 SPU" />
        </label>
        <button onClick={createSpu} disabled={busy}>创建 SPU</button>
      </div>

      <div className="card grid grid-2">
        <h2>批量导入 SKU</h2>
        <label>
          SKU 名称
          <input value={importSkuName} onChange={(e) => setImportSkuName(e.target.value)} />
        </label>
        <label>
          规格
          <input value={importSpec} onChange={(e) => setImportSpec(e.target.value)} />
        </label>
        <label>
          基础价
          <input value={basePrice} onChange={(e) => setBasePrice(e.target.value)} />
        </label>
        <label>
          服务费率
          <input value={serviceFeeRate} onChange={(e) => setServiceFeeRate(e.target.value)} />
        </label>
        <label>
          初始库存
          <input value={availableQty} onChange={(e) => setAvailableQty(e.target.value)} />
        </label>
        <label>
          预警阈值
          <input value={alertThreshold} onChange={(e) => setAlertThreshold(e.target.value)} />
        </label>
        <button onClick={batchImportSku} disabled={busy}>导入 1 条 SKU</button>
      </div>

      <div className="card">
        <h2>SKU 上架审核</h2>
        <button onClick={loadPendingSkus} disabled={busy}>刷新待审核 SKU</button>
        <pre>{JSON.stringify(pendingSkus, null, 2)}</pre>
        <div className="grid grid-2">
          <label>
            SKU ID
            <input value={skuAuditId} onChange={(e) => setSkuAuditId(e.target.value)} />
          </label>
          <label>
            审核结果
            <select value={skuDecision} onChange={(e) => setSkuDecision(e.target.value)}>
              <option value="APPROVE">APPROVE</option>
              <option value="REJECT">REJECT</option>
            </select>
          </label>
          <label>
            审核备注
            <textarea value={skuComment} onChange={(e) => setSkuComment(e.target.value)} />
          </label>
        </div>
        <button onClick={auditSku} disabled={busy}>提交上架审核</button>
      </div>

      <div className="card">
        <h2>库存调整与缺货预警</h2>
        <div className="grid grid-2">
          <label>
            SKU ID
            <input value={stockSkuId} onChange={(e) => setStockSkuId(e.target.value)} />
          </label>
          <label>
            可售库存
            <input value={stockQty} onChange={(e) => setStockQty(e.target.value)} />
          </label>
          <label>
            预警阈值
            <input value={stockAlert} onChange={(e) => setStockAlert(e.target.value)} />
          </label>
          <label>
            调整原因
            <input value={stockReason} onChange={(e) => setStockReason(e.target.value)} />
          </label>
        </div>
        <div className="grid grid-2">
          <button onClick={adjustStock} disabled={busy}>提交库存调整</button>
          <button onClick={loadOutOfStockAlerts} disabled={busy}>刷新缺货预警</button>
        </div>
        <pre>{JSON.stringify(outOfStockAlerts, null, 2)}</pre>
      </div>

      <div className="card">
        <strong>消息：</strong> {message}
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<AdminApp />);
