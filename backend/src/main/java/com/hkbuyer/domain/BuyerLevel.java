package com.hkbuyer.domain;

public enum BuyerLevel {
    BRONZE(1),
    SILVER(2),
    GOLD(3);

    private final int rank;

    BuyerLevel(int rank) {
        this.rank = rank;
    }

    public int getRank() {
        return rank;
    }

    public boolean isAtLeast(BuyerLevel required) {
        return this.rank >= required.rank;
    }

    public static BuyerLevel fromCreditScore(int creditScore) {
        if (creditScore >= 85) {
            return GOLD;
        }
        if (creditScore >= 70) {
            return SILVER;
        }
        return BRONZE;
    }
}
