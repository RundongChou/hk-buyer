import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { apiRequest } from './api';
import './styles.css';

interface FunnelMetrics {
  payment_success: number;
  payment_failed: number;
  payment_compensated: number;
  task_accepted: number;
  proof_submitted: number;
}

interface CatalogMetrics {
  sku_total: number;
  sku_published: number;
  sku_out_of_stock: number;
}

function DataApp(): JSX.Element {
  const [funnelMetrics, setFunnelMetrics] = useState<FunnelMetrics | null>(null);
  const [catalogMetrics, setCatalogMetrics] = useState<CatalogMetrics | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadMetrics = async (): Promise<void> => {
    setBusy(true);
    try {
      const [funnelPayload, catalogPayload] = await Promise.all([
        apiRequest<FunnelMetrics>('/api/v1/admin/metrics/funnel'),
        apiRequest<CatalogMetrics>('/api/v1/admin/metrics/catalog')
      ]);
      setFunnelMetrics(funnelPayload);
      setCatalogMetrics(catalogPayload);
      setMessage('漏斗与商品库存指标已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void loadMetrics();
  }, []);

  return (
    <div className="container">
      <h1>数据平台</h1>
      <div className="card">
        <button onClick={loadMetrics} disabled={busy}>刷新指标</button>
      </div>
      <div className="card grid grid-2">
        <div>
          <div className="badge">payment_success</div>
          <h2>{funnelMetrics?.payment_success ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">payment_failed</div>
          <h2>{funnelMetrics?.payment_failed ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">payment_compensated</div>
          <h2>{funnelMetrics?.payment_compensated ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">task_accepted</div>
          <h2>{funnelMetrics?.task_accepted ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">proof_submitted</div>
          <h2>{funnelMetrics?.proof_submitted ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">sku_total</div>
          <h2>{catalogMetrics?.sku_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">sku_published</div>
          <h2>{catalogMetrics?.sku_published ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">sku_out_of_stock</div>
          <h2>{catalogMetrics?.sku_out_of_stock ?? '-'}</h2>
        </div>
      </div>
      <div className="card">
        <strong>消息：</strong> {message}
      </div>
      <div className="card">
        <h2>漏斗指标</h2>
        <pre>{JSON.stringify(funnelMetrics, null, 2)}</pre>
      </div>
      <div className="card">
        <h2>商品库存指标</h2>
        <pre>{JSON.stringify(catalogMetrics, null, 2)}</pre>
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<DataApp />);
