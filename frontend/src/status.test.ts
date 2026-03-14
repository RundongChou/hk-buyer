import { describe, expect, it } from 'vitest';
import { toOrderStatusLabel } from './status';

describe('toOrderStatusLabel', () => {
  it('maps known status to zh-cn label', () => {
    expect(toOrderStatusLabel('PAID_WAIT_ACCEPT')).toBe('待接单');
  });

  it('maps customs status to zh-cn label', () => {
    expect(toOrderStatusLabel('CUSTOMS_CLEARANCE')).toBe('清关中');
  });

  it('returns fallback for unknown status', () => {
    expect(toOrderStatusLabel('UNKNOWN')).toBe('UNKNOWN');
  });
});
