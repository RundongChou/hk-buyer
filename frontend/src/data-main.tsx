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

interface FulfillmentMetrics {
  warehouse_inbound_completed_total: number;
  customs_submitted_total: number;
  customs_released_total: number;
  customs_rejected_total: number;
  customs_success_rate: number;
  compliance_clearance_success_rate: number;
  shipment_in_transit_total: number;
  shipment_signed_total: number;
  signed_within_7_15_days_total: number;
  signed_within_7_15_days_rate: number;
}

interface AfterSaleRiskMetrics {
  after_sale_open_cases_total: number;
  after_sale_pending_arbitration_total: number;
  after_sale_resolved_total: number;
  counterfeit_dispute_total: number;
  counterfeit_complaint_rate: number;
  partial_refund_approved_total: number;
  order_cancelled_total: number;
  order_cancel_rate: number;
}

interface SettlementMetrics {
  settlement_ledger_total: number;
  settlement_pending_total: number;
  settlement_payout_requested_total: number;
  settlement_settled_total: number;
  settlement_reconciliation_matched_total: number;
  settlement_reconciliation_exception_total: number;
  settlement_completion_rate: number;
  settlement_reconciliation_accuracy_rate: number;
}

interface GrowthMetrics {
  membership_profile_total: number;
  membership_bronze_total: number;
  membership_silver_total: number;
  membership_gold_total: number;
  growth_campaign_total: number;
  growth_campaign_active_total: number;
  growth_touch_sent_total: number;
  growth_touched_user_total: number;
  growth_coupon_used_order_total: number;
  growth_touch_to_coupon_conversion_rate: number;
  paid_user_30d_total: number;
  repurchase_user_30d_total: number;
  repurchase_rate_30d: number;
}

function DataApp(): JSX.Element {
  const [funnelMetrics, setFunnelMetrics] = useState<FunnelMetrics | null>(null);
  const [catalogMetrics, setCatalogMetrics] = useState<CatalogMetrics | null>(null);
  const [buyerMetrics, setBuyerMetrics] = useState<BuyerFulfillmentMetrics | null>(null);
  const [dynamicMetrics, setDynamicMetrics] = useState<DynamicPricingMetrics | null>(null);
  const [fulfillmentMetrics, setFulfillmentMetrics] = useState<FulfillmentMetrics | null>(null);
  const [afterSaleRiskMetrics, setAfterSaleRiskMetrics] = useState<AfterSaleRiskMetrics | null>(null);
  const [settlementMetrics, setSettlementMetrics] = useState<SettlementMetrics | null>(null);
  const [growthMetrics, setGrowthMetrics] = useState<GrowthMetrics | null>(null);
  const [message, setMessage] = useState('');
  const [busy, setBusy] = useState(false);

  const loadMetrics = async (): Promise<void> => {
    setBusy(true);
    try {
      const [
        funnelPayload,
        catalogPayload,
        buyerPayload,
        dynamicPayload,
        fulfillmentPayload,
        afterSaleRiskPayload,
        settlementPayload,
        growthPayload
      ] = await Promise.all([
        apiRequest<FunnelMetrics>('/api/v1/admin/metrics/funnel'),
        apiRequest<CatalogMetrics>('/api/v1/admin/metrics/catalog'),
        apiRequest<BuyerFulfillmentMetrics>('/api/v1/admin/metrics/buyer-fulfillment'),
        apiRequest<DynamicPricingMetrics>('/api/v1/admin/metrics/dynamic-pricing'),
        apiRequest<FulfillmentMetrics>('/api/v1/admin/metrics/fulfillment'),
        apiRequest<AfterSaleRiskMetrics>('/api/v1/admin/metrics/after-sale-risk'),
        apiRequest<SettlementMetrics>('/api/v1/admin/metrics/settlement'),
        apiRequest<GrowthMetrics>('/api/v1/admin/metrics/growth')
      ]);
      setFunnelMetrics(funnelPayload);
      setCatalogMetrics(catalogPayload);
      setBuyerMetrics(buyerPayload);
      setDynamicMetrics(dynamicPayload);
      setFulfillmentMetrics(fulfillmentPayload);
      setAfterSaleRiskMetrics(afterSaleRiskPayload);
      setSettlementMetrics(settlementPayload);
      setGrowthMetrics(growthPayload);
      setMessage('漏斗、商品库存、买手履约、动态提价、仓配清关、售后风控、结算对账、增长复购指标已刷新');
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
        <div>
          <div className="badge">warehouse_inbound_completed_total</div>
          <h2>{fulfillmentMetrics?.warehouse_inbound_completed_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">customs_submitted_total</div>
          <h2>{fulfillmentMetrics?.customs_submitted_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">customs_released_total</div>
          <h2>{fulfillmentMetrics?.customs_released_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">customs_success_rate</div>
          <h2>{fulfillmentMetrics?.customs_success_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">compliance_clearance_success_rate</div>
          <h2>{fulfillmentMetrics?.compliance_clearance_success_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">shipment_in_transit_total</div>
          <h2>{fulfillmentMetrics?.shipment_in_transit_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">shipment_signed_total</div>
          <h2>{fulfillmentMetrics?.shipment_signed_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">signed_within_7_15_days_total</div>
          <h2>{fulfillmentMetrics?.signed_within_7_15_days_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">signed_within_7_15_days_rate</div>
          <h2>{fulfillmentMetrics?.signed_within_7_15_days_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">after_sale_open_cases_total</div>
          <h2>{afterSaleRiskMetrics?.after_sale_open_cases_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">after_sale_pending_arbitration_total</div>
          <h2>{afterSaleRiskMetrics?.after_sale_pending_arbitration_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">after_sale_resolved_total</div>
          <h2>{afterSaleRiskMetrics?.after_sale_resolved_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">counterfeit_dispute_total</div>
          <h2>{afterSaleRiskMetrics?.counterfeit_dispute_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">counterfeit_complaint_rate</div>
          <h2>{afterSaleRiskMetrics?.counterfeit_complaint_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">partial_refund_approved_total</div>
          <h2>{afterSaleRiskMetrics?.partial_refund_approved_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">order_cancelled_total</div>
          <h2>{afterSaleRiskMetrics?.order_cancelled_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">order_cancel_rate</div>
          <h2>{afterSaleRiskMetrics?.order_cancel_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">settlement_ledger_total</div>
          <h2>{settlementMetrics?.settlement_ledger_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">settlement_pending_total</div>
          <h2>{settlementMetrics?.settlement_pending_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">settlement_payout_requested_total</div>
          <h2>{settlementMetrics?.settlement_payout_requested_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">settlement_settled_total</div>
          <h2>{settlementMetrics?.settlement_settled_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">settlement_completion_rate</div>
          <h2>{settlementMetrics?.settlement_completion_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">settlement_reconciliation_accuracy_rate</div>
          <h2>{settlementMetrics?.settlement_reconciliation_accuracy_rate ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">membership_profile_total</div>
          <h2>{growthMetrics?.membership_profile_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">growth_campaign_active_total</div>
          <h2>{growthMetrics?.growth_campaign_active_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">growth_touch_sent_total</div>
          <h2>{growthMetrics?.growth_touch_sent_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">growth_coupon_used_order_total</div>
          <h2>{growthMetrics?.growth_coupon_used_order_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">repurchase_user_30d_total</div>
          <h2>{growthMetrics?.repurchase_user_30d_total ?? '-'}</h2>
        </div>
        <div>
          <div className="badge">repurchase_rate_30d</div>
          <h2>{growthMetrics?.repurchase_rate_30d ?? '-'}</h2>
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
      <div className="card">
        <h2>仓配清关链路指标</h2>
        <pre>{JSON.stringify(fulfillmentMetrics, null, 2)}</pre>
      </div>
      <div className="card">
        <h2>售后风控指标</h2>
        <pre>{JSON.stringify(afterSaleRiskMetrics, null, 2)}</pre>
      </div>
      <div className="card">
        <h2>结算与对账指标</h2>
        <pre>{JSON.stringify(settlementMetrics, null, 2)}</pre>
      </div>
      <div className="card">
        <h2>运营增长与复购指标</h2>
        <pre>{JSON.stringify(growthMetrics, null, 2)}</pre>
      </div>
    </div>
  );
}

const container = document.getElementById('root');
if (!container) {
  throw new Error('root container not found');
}
createRoot(container).render(<DataApp />);
