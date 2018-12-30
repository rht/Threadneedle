/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.InvestmentStrategyGoal;
import core.InvestorStrategy;
import core.StockInvestor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class AbstractInvestorStrategy
implements InvestorStrategy {
    protected StockInvestor investor;
    protected List<Long> purchases;
    protected PurchaseStats pStats;
    protected double profitMargin = 1.05;

    protected AbstractInvestorStrategy(StockInvestor stockInvestor) {
        this.investor = stockInvestor;
        if (stockInvestor.getStrategy() != this) {
            stockInvestor.setStrategy(this);
        }
        this.purchases = new ArrayList<Long>();
    }

    @Override
    public final void executeStrategy() {
        this.executeStrategy(InvestmentStrategyGoal.NONE);
    }

    @Override
    public void recordPurchase(long l) {
        this.purchases.add(l);
    }

    @Override
    public void recordSale(long l) {
        if (this.purchases.isEmpty()) {
            throw new RuntimeException("Selling something without recording previous purchase!?");
        }
        Collections.sort(this.purchases);
        this.purchases.remove(0);
    }

    @Override
    public abstract void executeStrategy(InvestmentStrategyGoal var1);

    protected void profitFromPurchases() {
        this.pStats = new PurchaseStats().invoke();
        long l = this.pStats.getTotal();
        long l2 = this.pStats.getLow();
        long l3 = this.pStats.getHigh();
        if (this.purchases.isEmpty()) {
            return;
        }
        long l4 = (long)((double)(l /= (long)this.purchases.size()) * this.profitMargin);
        this.investor.allowSellAt(l4, l2, l3);
    }

    protected void protectAgainstLosses() {
        if (this.purchases.isEmpty()) {
            return;
        }
        if (this.pStats == null) {
            this.pStats = new PurchaseStats().invoke();
        }
        long l = (long)(1.0 / this.profitMargin * (double)this.pStats.getTotal() / (double)this.purchases.size());
        this.investor.sellIfBelow(l);
    }

    protected class PurchaseStats {
        private long total;
        private long high;
        private long low;

        public PurchaseStats() {
            this.total = 0L;
            this.high = Long.MIN_VALUE;
            this.low = Long.MAX_VALUE;
        }

        public PurchaseStats(long l, long l2, long l3) {
            this.total = l;
            this.high = l2;
            this.low = l3;
        }

        public long getTotal() {
            return this.total;
        }

        public long getHigh() {
            return this.high;
        }

        public long getLow() {
            return this.low;
        }

        public PurchaseStats invoke() {
            for (Long l : AbstractInvestorStrategy.this.purchases) {
                this.total += l.longValue();
                this.high = Math.max(this.high, l);
                this.low = Math.min(this.low, l);
            }
            return this;
        }
    }

}

