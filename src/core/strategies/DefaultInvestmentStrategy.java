/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.AbstractInvestmentStrategy;
import core.InvestmentStrategyGoal;

public class DefaultInvestmentStrategy
extends AbstractInvestmentStrategy {
    private long minDeposit = 100L;
    private long desiredDeposit = 500L;
    private double prosperityFactor = 2.0;

    @Override
    public void updateStrategy(long l, long l2) {
        super.updateStrategy(l, l2);
        if (this.desiredDeposit < l2) {
            this.desiredDeposit += l2 / 3L;
        }
    }

    @Override
    public long neededFunds() {
        return (long)(1.5 * (double)this.desiredDeposit * this.prosperityFactor - (double)this.deposit);
    }

    @Override
    public int loanDuration() {
        return 120;
    }

    @Override
    public InvestmentStrategyGoal getGoalFromStrategy() {
        if (this.deposit < this.minDeposit || (double)this.deposit < (double)this.upcomingExpenses * Math.max(1.0, this.prosperityFactor)) {
            return InvestmentStrategyGoal.LIQUIDATE;
        }
        if (this.deposit < this.desiredDeposit) {
            return InvestmentStrategyGoal.CONTRACT;
        }
        if ((double)this.deposit > (double)this.desiredDeposit * this.prosperityFactor) {
            return InvestmentStrategyGoal.EXPAND;
        }
        return InvestmentStrategyGoal.NONE;
    }

    public void setMinDeposit(long l) {
        this.minDeposit = l;
    }

    public void setDesiredDeposit(long l) {
        this.desiredDeposit = l;
    }

    public void setProsperityFactor(double d) {
        this.prosperityFactor = d;
    }
}

