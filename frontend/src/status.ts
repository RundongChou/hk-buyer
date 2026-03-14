export const ORDER_STATUS_LABELS: Record<string, string> = {
  PENDING_PAYMENT: '待支付',
  PAID_WAIT_ACCEPT: '待接单',
  BUYER_PROCUREMENT: '采购中',
  PROOF_UNDER_REVIEW: '凭证审核中',
  WAIT_INBOUND: '待入仓',
  CUSTOMS_CLEARANCE: '清关中',
  IN_TRANSIT: '运输中',
  SIGNED: '已签收',
  CANCELLED: '已取消'
};

export function toOrderStatusLabel(status: string): string {
  return ORDER_STATUS_LABELS[status] ?? status;
}
