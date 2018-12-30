/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.InvestmentStrategyGoal;

public interface InvestorStrategy {
    public void recordPurchase(long var1);

    public void recordSale(long var1);

    public void executeStrategy();

    public void executeStrategy(InvestmentStrategyGoal var1);
}

