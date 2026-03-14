import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { apiRequest } from './api';
import './styles.css';

interface FunnelMetrics {
  payment_success: number;
  task_accepted: number;
  proof_submitted: number;
}

function DataApp(): JSX.Element {
  const [metrics, setMetrics] = useState<FunnelMetrics | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadMetrics = async (): Promise<void> => {
    setBusy(true);
    try {
      const payload = await apiRequest<FunnelMetrics>('/api/v1/admin/metrics/funnel');
      setMetrics(payload);
      setMessage('漏斗指标已刷新');
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
          <h2>{metrics?.payment_success ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">task_accepted</div>
          <h2>{metrics?.task_accepted ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">proof_submitted</div>
          <h2>{metrics?.proof_submitted ?? '-'}</h2>
        </div>
      </div>
      <div className="card">
        <strong>消息：</strong> {message}
      </div>
      <div className="card">
        <pre>{JSON.stringify(metrics, null, 2)}</pre>
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<DataApp />);
