import React, { useState } from 'react';
import { createRoot } from 'react-dom/client';
import { apiRequest } from './api';
import { toOrderStatusLabel } from './status';
import './styles.css';

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

interface CartItem {
  cartItemId: number;
  userId: number;
  skuId: number;
  qty: number;
  selected: boolean;
  updatedAt: string;
  sku: CatalogSku;
}

interface CheckoutQuote {
  userId: number;
  goodsAmount: string;
  couponDiscount: string;
  taxAmount: string;
  payableAmount: string;
  taxRate: string;
  coupon: {
    couponCode: string;
    couponName: string;
    discountType: string;
    discountAmount: string;
    minOrderAmount: string;
  } | null;
  items: Array<{
    skuId: number;
    qty: number;
    unitPrice: string;
    lineAmount: string;
  }>;
}

interface PayOrderResponse {
  orderId: number;
  orderStatus: string;
  payStatus: string;
  taskId?: number;
  compensationToken?: string;
  failureReason?: string;
}

interface FulfillmentDetail {
  orderId: number;
  orderStatus: string;
  warehouse: Record<string, unknown> | null;
  customs: Record<string, unknown> | null;
  shipment: Record<string, unknown> | null;
  timeline: TimelineEvent[];
}

interface AfterSaleCaseItem {
  caseId: number;
  orderId: number;
  taskId: number | null;
  caseType: string;
  caseStatus: string;
  issueReason: string;
  replacementSkuName: string | null;
  suggestedRefundAmount: string | null;
  negotiatedRefundAmount: string | null;
  userDecision: string | null;
  arbitrationResult: string | null;
  riskLevel: string;
  createdAt: string;
  updatedAt: string;
}

interface MembershipProfile {
  userId: number;
  memberLevel: string;
  totalPaidOrders: number;
  totalPaidAmount: string;
  memberPoints: number;
  benefits: string[];
  nextLevel: string;
  refreshedAt: string;
}

interface GrowthCouponTouch {
  touchId: number;
  campaignId: number;
  campaignName: string;
  targetMemberLevel: string;
  campaignStatus: string;
  userId: number;
  touchChannel: string;
  touchStatus: string;
  couponCode: string;
  touchedAt: string;
  startAt: string;
  endAt: string;
}

interface GrowthCouponsPayload {
  userId: number;
  memberLevel: string;
  couponTouches: GrowthCouponTouch[];
  total: number;
}

interface GrowthRecommendationsPayload {
  userId: number;
  repurchaseRecommendations: CatalogSku[];
  categoryRecommendations: CatalogSku[];
  generatedAt: string;
}

function H5App(): JSX.Element {
  const [userId, setUserId] = useState('10001');
  const [qty, setQty] = useState('1');
  const [keyword, setKeyword] = useState('');
  const [catalogSkus, setCatalogSkus] = useState<CatalogSku[]>([]);
  const [cartItems, setCartItems] = useState<CartItem[]>([]);
  const [couponCode, setCouponCode] = useState('SPRINT3OFF20');
  const [quote, setQuote] = useState<CheckoutQuote | null>(null);
  const [orderId, setOrderId] = useState('');
  const [paymentScenario, setPaymentScenario] = useState<'SUCCESS' | 'FAIL_TIMEOUT'>('SUCCESS');
  const [compensationToken, setCompensationToken] = useState('');
  const [orderDetail, setOrderDetail] = useState<unknown>(null);
  const [timeline, setTimeline] = useState<TimelineEvent[]>([]);
  const [fulfillmentDetail, setFulfillmentDetail] = useState<FulfillmentDetail | null>(null);
  const [afterSaleCases, setAfterSaleCases] = useState<AfterSaleCaseItem[]>([]);
  const [membershipProfile, setMembershipProfile] = useState<MembershipProfile | null>(null);
  const [growthCouponTouches, setGrowthCouponTouches] = useState<GrowthCouponTouch[]>([]);
  const [growthRecommendations, setGrowthRecommendations] = useState<GrowthRecommendationsPayload | null>(null);
  const [authIssueReason, setAuthIssueReason] = useState('怀疑商品非正品，申请平台核验');
  const [authEvidenceUrl, setAuthEvidenceUrl] = useState('https://example.com/evidence.jpg');
  const [afterSaleCaseId, setAfterSaleCaseId] = useState('');
  const [afterSaleDecision, setAfterSaleDecision] = useState<'ACCEPT_REPLACEMENT' | 'REQUEST_PARTIAL_REFUND'>('ACCEPT_REPLACEMENT');
  const [afterSaleComment, setAfterSaleComment] = useState('同意按平台建议处理');
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

  const loadCart = async (): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<CartItem[]>(`/api/v1/cart/items?userId=${Number(userId)}`);
      setCartItems(payload);
      setMessage(`购物车已刷新，共 ${payload.length} 项`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const addToCart = async (skuId: number): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      await apiRequest('/api/v1/cart/items/upsert', {
        method: 'POST',
        body: {
          userId: Number(userId),
          skuId,
          qty: Number(qty)
        }
      });
      setMessage(`已加入购物车：SKU ${skuId}`);
      await loadCart();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const removeFromCart = async (skuId: number): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      await apiRequest('/api/v1/cart/items/remove', {
        method: 'POST',
        body: {
          userId: Number(userId),
          skuId
        }
      });
      setMessage(`已移除购物车 SKU ${skuId}`);
      await loadCart();
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const quoteCheckout = async (): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<CheckoutQuote>('/api/v1/checkout/quote', {
        method: 'POST',
        body: {
          userId: Number(userId),
          couponCode: couponCode.trim() || undefined
        }
      });
      setQuote(payload);
      setMessage('结算报价已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const createOrderFromCart = async (): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<{ orderId: number; orderStatus: string; quote: CheckoutQuote }>('/api/v1/checkout/orders', {
        method: 'POST',
        body: {
          userId: Number(userId),
          couponCode: couponCode.trim() || undefined
        }
      });
      setOrderId(String(payload.orderId));
      setQuote(payload.quote);
      setCompensationToken('');
      setMessage(`购物车下单成功，状态：${toOrderStatusLabel(payload.orderStatus)}`);
      await loadCart();
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
      const payload = await apiRequest<PayOrderResponse>(`/api/v1/orders/${orderId}/pay`, {
        method: 'POST',
        body: {
          paymentChannel: 'WECHAT',
          paymentScenario
        }
      });

      if (payload.payStatus === 'FAILED') {
        setCompensationToken(payload.compensationToken ?? '');
        setMessage(`支付失败：${payload.failureReason}，可发起补偿支付`);
      } else {
        setCompensationToken('');
        setMessage(`支付成功，状态：${toOrderStatusLabel(payload.orderStatus)}，任务ID：${payload.taskId}`);
      }
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const compensatePay = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先输入订单 ID');
      return;
    }
    if (!compensationToken.trim()) {
      setMessage('缺少补偿 token');
      return;
    }

    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<PayOrderResponse>(`/api/v1/orders/${orderId}/pay-compensate`, {
        method: 'POST',
        body: {
          paymentChannel: 'WECHAT',
          compensationToken: compensationToken.trim()
        }
      });
      setCompensationToken('');
      setMessage(`补偿支付成功，状态：${toOrderStatusLabel(payload.orderStatus)}，任务ID：${payload.taskId}`);
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

  const loadFulfillment = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先输入订单号');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<FulfillmentDetail>(`/api/v1/orders/${orderId}/fulfillment`);
      setFulfillmentDetail(payload);
      setMessage('履约详情已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const loadAfterSaleCases = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先输入订单号');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      const payload = await apiRequest<AfterSaleCaseItem[]>(
        `/api/v1/orders/${orderId}/after-sale/cases?userId=${Number(userId)}`
      );
      setAfterSaleCases(payload);
      if (!afterSaleCaseId && payload.length > 0) {
        setAfterSaleCaseId(String(payload[0].caseId));
      }
      setMessage(`售后工单已刷新，共 ${payload.length} 条`);
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const createAuthenticityDispute = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先输入订单号');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      await apiRequest(`/api/v1/orders/${orderId}/after-sale/authenticity`, {
        method: 'POST',
        body: {
          userId: Number(userId),
          issueReason: authIssueReason,
          evidenceUrl: authEvidenceUrl.trim() || undefined
        }
      });
      await loadAfterSaleCases();
      setMessage('真伪争议工单已提交');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const submitAfterSaleUserDecision = async (): Promise<void> => {
    if (!orderId) {
      setMessage('请先输入订单号');
      return;
    }
    if (!afterSaleCaseId) {
      setMessage('请输入售后工单 ID');
      return;
    }
    setBusy(true);
    setMessage('');
    try {
      await apiRequest(`/api/v1/orders/${orderId}/after-sale/cases/${afterSaleCaseId}/decision`, {
        method: 'POST',
        body: {
          userId: Number(userId),
          decision: afterSaleDecision,
          comment: afterSaleComment.trim() || undefined
        }
      });
      await loadAfterSaleCases();
      setMessage('售后方案决策已提交，等待平台仲裁');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const loadGrowthCenter = async (): Promise<void> => {
    setBusy(true);
    setMessage('');
    try {
      const [membershipPayload, couponsPayload, recommendationsPayload] = await Promise.all([
        apiRequest<MembershipProfile>(`/api/v1/growth/membership?userId=${Number(userId)}`),
        apiRequest<GrowthCouponsPayload>(`/api/v1/growth/coupons?userId=${Number(userId)}`),
        apiRequest<GrowthRecommendationsPayload>(`/api/v1/growth/recommendations?userId=${Number(userId)}`)
      ]);
      setMembershipProfile(membershipPayload);
      setGrowthCouponTouches(couponsPayload.couponTouches);
      setGrowthRecommendations(recommendationsPayload);
      if (!couponCode.trim() && couponsPayload.couponTouches.length > 0) {
        setCouponCode(couponsPayload.couponTouches[0].couponCode);
      }
      setMessage('会员权益、活动券包与推荐商品已刷新');
    } catch (error) {
      setMessage(String(error));
    } finally {
      setBusy(false);
    }
  };

  const applyGrowthCoupon = (touch: GrowthCouponTouch): void => {
    setCouponCode(touch.couponCode);
    setMessage(`已应用活动券：${touch.couponCode}`);
  };

  return (
    <div className="container">
      <h1>H5 用户端（交易稳态 V2）</h1>

      <div className="card grid grid-2">
        <label>
          用户 ID
          <input value={userId} onChange={(e) => setUserId(e.target.value)} />
        </label>
        <label>
          加购数量
          <input value={qty} onChange={(e) => setQty(e.target.value)} />
        </label>
      </div>

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
            <button key={sku.skuId} onClick={() => addToCart(sku.skuId)} disabled={busy || !sku.saleable}>
              加购 #{sku.skuId} {sku.skuName} / {sku.stockStatus}
            </button>
          ))}
        </div>
      </div>

      <div className="card">
        <h2>购物车</h2>
        <div className="grid grid-2">
          <button onClick={loadCart} disabled={busy}>刷新购物车</button>
          <button onClick={quoteCheckout} disabled={busy}>结算报价</button>
        </div>
        <pre>{JSON.stringify(cartItems, null, 2)}</pre>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          {cartItems.slice(0, 6).map((item) => (
            <button key={item.cartItemId} onClick={() => removeFromCart(item.skuId)} disabled={busy}>
              移除 #{item.skuId}
            </button>
          ))}
        </div>
      </div>

      <div className="card grid grid-2">
        <label>
          优惠券
          <input value={couponCode} onChange={(e) => setCouponCode(e.target.value)} placeholder="如 SPRINT3OFF20" />
        </label>
        <div style={{ alignSelf: 'end' }}>
          <button onClick={createOrderFromCart} disabled={busy}>购物车下单</button>
        </div>
      </div>

      <div className="card">
        <h2>Sprint 9 会员与复购增长中心</h2>
        <button onClick={loadGrowthCenter} disabled={busy}>刷新会员/券包/推荐</button>
        <h3>会员档案</h3>
        <pre>{JSON.stringify(membershipProfile, null, 2)}</pre>
        <h3>活动券包</h3>
        <pre>{JSON.stringify(growthCouponTouches, null, 2)}</pre>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          {growthCouponTouches.slice(0, 6).map((touch) => (
            <button key={touch.touchId} onClick={() => applyGrowthCoupon(touch)} disabled={busy}>
              应用券 {touch.couponCode}
            </button>
          ))}
        </div>
        <h3>复购推荐（可一键加购）</h3>
        <pre>{JSON.stringify(growthRecommendations, null, 2)}</pre>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          {growthRecommendations?.repurchaseRecommendations.slice(0, 4).map((sku) => (
            <button key={`repurchase-${sku.skuId}`} onClick={() => addToCart(sku.skuId)} disabled={busy || !sku.saleable}>
              复购加购 #{sku.skuId} {sku.skuName}
            </button>
          )) ?? null}
          {growthRecommendations?.categoryRecommendations.slice(0, 4).map((sku) => (
            <button key={`category-${sku.skuId}`} onClick={() => addToCart(sku.skuId)} disabled={busy || !sku.saleable}>
              同类推荐加购 #{sku.skuId} {sku.skuName}
            </button>
          )) ?? null}
        </div>
      </div>

      <div className="card">
        <h2>结算报价</h2>
        <pre>{JSON.stringify(quote, null, 2)}</pre>
      </div>

      <div className="card grid grid-2">
        <label>
          订单 ID
          <input value={orderId} onChange={(e) => setOrderId(e.target.value)} />
        </label>
        <label>
          支付场景
          <select value={paymentScenario} onChange={(e) => setPaymentScenario(e.target.value as 'SUCCESS' | 'FAIL_TIMEOUT')}>
            <option value="SUCCESS">SUCCESS</option>
            <option value="FAIL_TIMEOUT">FAIL_TIMEOUT</option>
          </select>
        </label>
        <label>
          补偿 Token
          <input value={compensationToken} onChange={(e) => setCompensationToken(e.target.value)} />
        </label>
      </div>

      <div className="card grid grid-2">
        <button onClick={payOrder} disabled={busy}>发起支付</button>
        <button onClick={compensatePay} disabled={busy}>补偿支付</button>
      </div>

      <div className="card">
        <div className="grid grid-2">
          <button onClick={loadOrder} disabled={busy}>刷新订单与时间线</button>
          <button onClick={loadFulfillment} disabled={busy}>刷新履约详情</button>
        </div>
      </div>

      <div className="card">
        <h2>Sprint 7 售后中心</h2>
        <div className="grid grid-2">
          <label>
            真伪争议说明
            <textarea value={authIssueReason} onChange={(e) => setAuthIssueReason(e.target.value)} />
          </label>
          <label>
            证据 URL
            <input value={authEvidenceUrl} onChange={(e) => setAuthEvidenceUrl(e.target.value)} />
          </label>
        </div>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          <button onClick={createAuthenticityDispute} disabled={busy}>发起真伪争议</button>
          <button onClick={loadAfterSaleCases} disabled={busy}>刷新售后工单</button>
        </div>
        <div className="grid grid-2" style={{ marginTop: 12 }}>
          <label>
            售后工单 ID
            <input value={afterSaleCaseId} onChange={(e) => setAfterSaleCaseId(e.target.value)} />
          </label>
          <label>
            用户决策
            <select value={afterSaleDecision} onChange={(e) => setAfterSaleDecision(e.target.value as 'ACCEPT_REPLACEMENT' | 'REQUEST_PARTIAL_REFUND')}>
              <option value="ACCEPT_REPLACEMENT">ACCEPT_REPLACEMENT</option>
              <option value="REQUEST_PARTIAL_REFUND">REQUEST_PARTIAL_REFUND</option>
            </select>
          </label>
          <label>
            决策备注
            <textarea value={afterSaleComment} onChange={(e) => setAfterSaleComment(e.target.value)} />
          </label>
        </div>
        <button onClick={submitAfterSaleUserDecision} disabled={busy}>提交售后决策</button>
        <pre>{JSON.stringify(afterSaleCases, null, 2)}</pre>
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

      <div className="card">
        <h2>仓配清关履约详情</h2>
        <pre>{JSON.stringify(fulfillmentDetail, null, 2)}</pre>
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<H5App />);
