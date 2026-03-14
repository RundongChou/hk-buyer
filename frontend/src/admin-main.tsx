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

function AdminApp(): JSX.Element {
  const [proofs, setProofs] = useState<ProofItem[]>([]);
  const [proofId, setProofId] = useState('');
  const [adminId, setAdminId] = useState('70001');
  const [decision, setDecision] = useState('APPROVE');
  const [comment, setComment] = useState('符合采购凭证要求');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadProofs = async (): Promise<void> => {
    setBusy(true);
    try {
      const payload = await apiRequest<ProofItem[]>('/api/v1/admin/proofs/pending');
      setProofs(payload);
      if (!proofId && payload.length > 0) {
        setProofId(String(payload[0].proofId));
      }
      setMessage('待审核凭证已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void loadProofs();
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
      setMessage('审核完成');
      await loadProofs();
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
        <button onClick={loadProofs} disabled={busy}>刷新待审核凭证</button>
        <pre>{JSON.stringify(proofs, null, 2)}</pre>
      </div>

      <div className="card grid grid-2">
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

      <div className="card">
        <button onClick={auditProof} disabled={busy}>提交审核</button>
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
