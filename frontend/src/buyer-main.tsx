import React, { useEffect, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { apiRequest } from './api';
import './styles.css';

interface TaskItem {
  taskId: number;
  orderId: number;
  taskStatus: string;
  acceptDeadline: string;
  suggestedMarkup: string;
  taskTier: string;
  requiredBuyerLevel: string;
  targetRegion: string;
  targetCategory: string;
  slaHours: number;
}

interface BuyerProfile {
  buyerId: number;
  displayName: string;
  auditStatus: string;
  buyerLevel: string;
  creditScore: number;
  rewardPoints: number;
  penaltyPoints: number;
  acceptedTaskCount: number;
  approvedProofCount: number;
  rejectedProofCount: number;
  serviceRegion: string;
  specialtyCategory: string;
  lastActiveAt: string | null;
  updatedAt: string;
}

function BuyerApp(): JSX.Element {
  const [tasks, setTasks] = useState<TaskItem[]>([]);
  const [profile, setProfile] = useState<BuyerProfile | null>(null);
  const [taskId, setTaskId] = useState('');
  const [buyerId, setBuyerId] = useState('30001');
  const [realName, setRealName] = useState('陈买手');
  const [idCardSuffix, setIdCardSuffix] = useState('A1234');
  const [serviceRegion, setServiceRegion] = useState('HK');
  const [specialtyCategory, setSpecialtyCategory] = useState('药妆护肤');
  const [settlementAccount, setSettlementAccount] = useState('buyer30001@wallet');
  const [storeName, setStoreName] = useState('香港药妆店A');
  const [receiptUrl, setReceiptUrl] = useState('https://example.com/receipt.jpg');
  const [batchNo, setBatchNo] = useState('BATCH-20260314');
  const [expiryDate, setExpiryDate] = useState('2027-03-14');
  const [productPhotoUrl, setProductPhotoUrl] = useState('https://example.com/product.jpg');
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadTasks = async (): Promise<void> => {
    setBusy(true);
    try {
      const payload = await apiRequest<TaskItem[]>(`/api/v1/buyer/tasks?buyerId=${Number(buyerId)}`);
      setTasks(payload);
      if (!taskId && payload.length > 0) {
        setTaskId(String(payload[0].taskId));
      }
      setMessage(`任务列表已刷新，共 ${payload.length} 条`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  useEffect(() => {
    void (async () => {
      await loadBuyerProfile();
      await loadTasks();
    })();
  }, []);

  const submitOnboarding = async (): Promise<void> => {
    setBusy(true);
    try {
      await apiRequest('/api/v1/buyer/onboarding/applications', {
        method: 'POST',
        body: {
          buyerId: Number(buyerId),
          realName,
          idCardSuffix,
          serviceRegion,
          specialtyCategory,
          settlementAccount
        }
      });
      setMessage('入驻申请已提交，等待后台审核');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const loadBuyerProfile = async (): Promise<void> => {
    setBusy(true);
    try {
      const payload = await apiRequest<BuyerProfile>(`/api/v1/buyer/profile/${Number(buyerId)}`);
      setProfile(payload);
      setMessage(`买手档案已刷新，当前等级：${payload.buyerLevel}`);
    } catch (error) {
      setProfile(null);
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const acceptTask = async (): Promise<void> => {
    if (!taskId) {
      setMessage('请输入任务 ID');
      return;
    }
    setBusy(true);
    try {
      await apiRequest(`/api/v1/buyer/tasks/${taskId}/accept`, {
        method: 'POST',
        body: { buyerId: Number(buyerId) }
      });
      setMessage('接单成功');
      await loadTasks();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const submitProof = async (): Promise<void> => {
    if (!taskId) {
      setMessage('请输入任务 ID');
      return;
    }
    setBusy(true);
    try {
      const payload = await apiRequest<{ proofId: number }>(`/api/v1/buyer/tasks/${taskId}/proofs`, {
        method: 'POST',
        body: {
          buyerId: Number(buyerId),
          storeName,
          receiptUrl,
          batchNo,
          expiryDate,
          productPhotoUrl
        }
      });
      setMessage(`凭证提交成功，proofId=${payload.proofId}`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  return (
    <div className="container">
      <h1>买手端</h1>

      <div className="card grid grid-2">
        <label>
          买手 ID
          <input value={buyerId} onChange={(e) => setBuyerId(e.target.value)} />
        </label>
        <label>
          姓名
          <input value={realName} onChange={(e) => setRealName(e.target.value)} />
        </label>
        <label>
          证件后缀
          <input value={idCardSuffix} onChange={(e) => setIdCardSuffix(e.target.value)} />
        </label>
        <label>
          服务区域
          <input value={serviceRegion} onChange={(e) => setServiceRegion(e.target.value)} />
        </label>
        <label>
          擅长品类
          <input value={specialtyCategory} onChange={(e) => setSpecialtyCategory(e.target.value)} />
        </label>
        <label>
          结算账户
          <input value={settlementAccount} onChange={(e) => setSettlementAccount(e.target.value)} />
        </label>
      </div>

      <div className="card">
        <button onClick={submitOnboarding} disabled={busy}>提交入驻申请</button>
        <button onClick={loadBuyerProfile} disabled={busy}>刷新买手档案</button>
        <pre>{JSON.stringify(profile, null, 2)}</pre>
      </div>

      <div className="card">
        <button onClick={loadTasks} disabled={busy}>刷新任务列表</button>
        <pre>{JSON.stringify(tasks, null, 2)}</pre>
      </div>

      <div className="card grid grid-2">
        <label>
          任务 ID
          <input value={taskId} onChange={(e) => setTaskId(e.target.value)} />
        </label>
      </div>

      <div className="card">
        <button onClick={acceptTask} disabled={busy}>接单</button>
      </div>

      <div className="card grid grid-2">
        <label>
          门店
          <input value={storeName} onChange={(e) => setStoreName(e.target.value)} />
        </label>
        <label>
          批次号
          <input value={batchNo} onChange={(e) => setBatchNo(e.target.value)} />
        </label>
        <label>
          有效期 (yyyy-MM-dd)
          <input value={expiryDate} onChange={(e) => setExpiryDate(e.target.value)} />
        </label>
        <label>
          小票 URL
          <input value={receiptUrl} onChange={(e) => setReceiptUrl(e.target.value)} />
        </label>
        <label>
          实拍 URL
          <input value={productPhotoUrl} onChange={(e) => setProductPhotoUrl(e.target.value)} />
        </label>
      </div>

      <div className="card">
        <button onClick={submitProof} disabled={busy}>提交凭证</button>
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
createRoot(container).render(<BuyerApp />);
