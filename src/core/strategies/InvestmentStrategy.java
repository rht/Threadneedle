/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.AbstractInvestorStrategy;
import core.InvestmentStrategyGoal;
import core.InvestorStrategy;
import core.StockInvestor;

public interface InvestmentStrategy {
    default public InvestmentStrategyGoal getGoalFromStrategy() {
        return InvestmentStrategyGoal.NONE;
    }

    public void updateStrategy(long var1, long var3);

    public InvestorStrategy getInvestorStrategy(StockInvestor var1);

    public void setInvestorStrategy(Class<? extends AbstractInvestorStrategy> var1);

    public long neededFunds();

    public int loanDuration();
}

