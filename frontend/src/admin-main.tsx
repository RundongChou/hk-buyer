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

function AdminApp(): JSX.Element {
  const [proofs, setProofs] = useState<ProofItem[]>([]);
  const [proofId, setProofId] = useState('');
  const [adminId, setAdminId] = useState('70001');
  const [decision, setDecision] = useState('APPROVE');
  const [comment, setComment] = useState('符合采购凭证要求');

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

  useEffect(() => {
    void (async () => {
      setBusy(true);
      await loadProofs();
      await loadPendingSkus();
      await loadOutOfStockAlerts();
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
