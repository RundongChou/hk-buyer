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

function H5App(): JSX.Element {
  const [userId, setUserId] = useState('10001');
  const [skuId, setSkuId] = useState('9001');
  const [qty, setQty] = useState('1');
  const [unitPrice, setUnitPrice] = useState('128.00');
  const [orderId, setOrderId] = useState('');
  const [orderDetail, setOrderDetail] = useState<unknown>(null);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState('');

  const createOrder = async (): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<OrderCreateResponse>('/api/v1/orders', {
        method: 'POST',
        body: {
          userId: Number(userId),
          items: [
            {
              skuId: Number(skuId),
              qty: Number(qty),
              unitPrice: Number(unitPrice)
            }
          ]
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
          单价
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
