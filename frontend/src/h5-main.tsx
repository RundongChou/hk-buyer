import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { apiRequest } from './api';
import { toOrderStatusLabel } from './status';
import './styles.css';

interface OrderCreateResponse {
  orderId: number;
  orderStatus: string;
  totalAmount: string;
}

interface TimelineEvent {
  eventId: number;
  eventType: string;
  eventDescription: string;
  createdAt: string;
}

interface CatalogSku {
  skuId: number;
  spuId: number;
  spuName: string;
  skuName: string;
  specValue: string;
  publishStatus: string;
  finalPrice: string;
  availableQty: number;
  stockStatus: string;
  saleable: boolean;
}

function H5App(): JSX.Element {
  const [userId, setUserId] = useState('10001');
  const [skuId, setSkuId] = useState('');
  const [qty, setQty] = useState('1');
  const [unitPrice, setUnitPrice] = useState('');
  const [keyword, setKeyword] = useState('');
  const [catalogSkus, setCatalogSkus] = useState<CatalogSku[]>([]);
  const [orderId, setOrderId] = useState('');
  const [orderDetail, setOrderDetail] = useState<unknown>(null);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const loadCatalog = async (): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      const query = keyword.trim();
      const path = query.length > 0
        ? `/api/v1/catalog/skus?keyword=${encodeURIComponent(query)}&onlyPublished=true`
        : '/api/v1/catalog/skus?onlyPublished=true';
      const payload = await apiRequest<CatalogSku[]>(path);
      setCatalogSkus(payload);
      setMessage(`SKU 列表已刷新，共 ${payload.length} 条`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const pickSku = (sku: CatalogSku): void => {
    setSkuId(String(sku.skuId));
    setUnitPrice(String(sku.finalPrice));
    if (!sku.saleable) {
      setMessage(`SKU ${sku.skuId} 当前缺货，仅可查看不可下单`);
      return;
    }
    setMessage(`已选择 SKU ${sku.skuId}，将按系统价 ${sku.finalPrice} 下单`);
  };

  const createOrder = async (): Promise<void> => {
    if (!skuId) {
      setMessage('请先选择 SKU 或输入 SKU ID');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      const itemPayload: Record<string, number> = {
        skuId: Number(skuId),
        qty: Number(qty)
      };
      const normalizedPrice = unitPrice.trim();
      if (normalizedPrice) {
        itemPayload.unitPrice = Number(normalizedPrice);
      }

      const payload = await apiRequest<OrderCreateResponse>('/api/v1/orders', {
        method: 'POST',
        body: {
          userId: Number(userId),
          items: [itemPayload]
        }
      });
      setOrderId(String(payload.orderId));
      setMessage(`订单创建成功，状态：${toOrderStatusLabel(payload.orderStatus)}`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const payOrder = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先创建订单');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<{ orderStatus: string; taskId: number }>(`/api/v1/orders/${orderId}/pay`, {
        method: 'POST',
        body: {
          paymentChannel: 'WECHAT'
        }
      });
      setMessage(`支付成功，状态：${toOrderStatusLabel(payload.orderStatus)}，任务ID：${payload.taskId}`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const loadOrder = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先输入订单号');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      const detail = await apiRequest<{ orderStatus: string }>(`/api/v1/orders/${orderId}`);
      const events = await apiRequest<TimelineEvent[]>(`/api/v1/orders/${orderId}/timeline`);
      setOrderDetail(detail);
      setTimeline(events);
      setMessage('订单详情已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container">
      <h1>H5 用户端</h1>
      <div className="card grid grid-2">
        <label>
          搜索关键词
          <input value={keyword} onChange={(e) => setKeyword(e.target.value)} placeholder="商品名/品牌" />
        </label>
        <div style={{ alignSelf: 'end' }}>
          <button onClick={loadCatalog} disabled={busy}>搜索 SKU</button>
        </div>
      </div>

      <div className="card">
        <h2>SKU 搜索结果（仅已上架）</h2>
        <pre>{JSON.stringify(catalogSkus, null, 2)}</pre>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          {catalogSkus.slice(0, 6).map((sku) => (
            <button key={sku.skuId} onClick={() => pickSku(sku)} disabled={busy}>
              选中 #{sku.skuId} {sku.skuName} / {sku.stockStatus}
            </button>
          ))}
        </div>
      </div>

      <div className="card grid grid-2">
        <label>
          用户 ID
          <input value={userId} onChange={(e) => setUserId(e.target.value)} />
        </label>
        <label>
          SKU ID
          <input value={skuId} onChange={(e) => setSkuId(e.target.value)} />
        </label>
        <label>
          数量
          <input value={qty} onChange={(e) => setQty(e.target.value)} />
        </label>
        <label>
          单价（可选，优先走系统价）
          <input value={unitPrice} onChange={(e) => setUnitPrice(e.target.value)} />
        </label>
      </div>

      <div className="card">
        <div className="grid grid-2">
          <button onClick={createOrder} disabled={busy}>创建订单</button>
          <button onClick={payOrder} disabled={busy}>支付订单</button>
        </div>
        <label>
          订单 ID
          <input value={orderId} onChange={(e) => setOrderId(e.target.value)} />
        </label>
        <button onClick={loadOrder} disabled={busy}>刷新订单与时间线</button>
      </div>

      {message ? (
        <div className="card">
          <strong>消息：</strong> {message}
        </div>
      ) : null}

      <div className="card">
        <h2>订单详情</h2>
        <pre>{JSON.stringify(orderDetail, null, 2)}</pre>
      </div>

      <div className="card">
        <h2>订单时间线</h2>
        <pre>{JSON.stringify(timeline, null, 2)}</pre>
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<H5App />);
