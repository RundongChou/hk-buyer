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

interface BuyerFulfillmentMetrics {
  buyer_pending_applications: number;
  buyer_approved_total: number;
  task_timeout_unaccepted_72h: number;
  buyer_bronze_total: number;
  buyer_silver_total: number;
  buyer_gold_total: number;
}

interface DynamicPricingMetrics {
  timeout_candidates_total: number;
  timeout_candidates_frequency_limited: number;
  auto_markup_task_total: number;
  auto_markup_applied_total: number;
  task_redispatch_total: number;
  task_timeout_terminated_total: number;
  repriced_task_accepted_total: number;
  repriced_task_accept_rate: number;
}

function DataApp(): JSX.Element {
  const [funnelMetrics, setFunnelMetrics] = useState<FunnelMetrics | null>(null);
  const [catalogMetrics, setCatalogMetrics] = useState<CatalogMetrics | null>(null);
  const [buyerMetrics, setBuyerMetrics] = useState<BuyerFulfillmentMetrics | null>(null);
  const [dynamicMetrics, setDynamicMetrics] = useState<DynamicPricingMetrics | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadMetrics = async (): Promise<void> => {
    setBusy(true);
    try {
      const [funnelPayload, catalogPayload, buyerPayload, dynamicPayload] = await Promise.all([
        apiRequest<FunnelMetrics>('/api/v1/admin/metrics/funnel'),
        apiRequest<CatalogMetrics>('/api/v1/admin/metrics/catalog'),
        apiRequest<BuyerFulfillmentMetrics>('/api/v1/admin/metrics/buyer-fulfillment'),
        apiRequest<DynamicPricingMetrics>('/api/v1/admin/metrics/dynamic-pricing')
      ]);
      setFunnelMetrics(funnelPayload);
      setCatalogMetrics(catalogPayload);
      setBuyerMetrics(buyerPayload);
      setDynamicMetrics(dynamicPayload);
      setMessage('漏斗、商品库存、买手履约、动态提价指标已刷新');
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
        <div>
          <div className="badge">buyer_pending_applications</div>
          <h2>{buyerMetrics?.buyer_pending_applications ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">buyer_approved_total</div>
          <h2>{buyerMetrics?.buyer_approved_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">task_timeout_unaccepted_72h</div>
          <h2>{buyerMetrics?.task_timeout_unaccepted_72h ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">buyer_bronze_total</div>
          <h2>{buyerMetrics?.buyer_bronze_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">buyer_silver_total</div>
          <h2>{buyerMetrics?.buyer_silver_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">buyer_gold_total</div>
          <h2>{buyerMetrics?.buyer_gold_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">timeout_candidates_total</div>
          <h2>{dynamicMetrics?.timeout_candidates_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">auto_markup_applied_total</div>
          <h2>{dynamicMetrics?.auto_markup_applied_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">task_redispatch_total</div>
          <h2>{dynamicMetrics?.task_redispatch_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">task_timeout_terminated_total</div>
          <h2>{dynamicMetrics?.task_timeout_terminated_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">repriced_task_accept_rate</div>
          <h2>{dynamicMetrics?.repriced_task_accept_rate ?? '-'}</h2>
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
      <div className="card">
        <h2>买手履约指标</h2>
        <pre>{JSON.stringify(buyerMetrics, null, 2)}</pre>
      </div>
      <div className="card">
        <h2>动态提价与重派指标</h2>
        <pre>{JSON.stringify(dynamicMetrics, null, 2)}</pre>
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<DataApp />);
