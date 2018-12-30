/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.AbstractInvestorStrategy;
import core.DefaultInvestorStrategy;
import core.InvestmentStrategy;
import core.InvestmentStrategyGoal;
import core.InvestorStrategy;
import core.StockInvestor;
import java.io.PrintStream;
import java.lang.reflect.Constructor;

public abstract class AbstractInvestmentStrategy
implements InvestmentStrategy {
    long deposit = 0L;
    long upcomingExpenses;
    private Class<? extends InvestorStrategy> investorStrategy = DefaultInvestorStrategy.class;

    AbstractInvestmentStrategy() {
    }

    @Override
    public abstract InvestmentStrategyGoal getGoalFromStrategy();

    @Override
    public void updateStrategy(long l, long l2) {
        this.deposit = l;
        this.upcomingExpenses = l2;
    }

    @Override
    public InvestorStrategy getInvestorStrategy(StockInvestor stockInvestor) {
        try {
            return this.investorStrategy.getConstructor(StockInvestor.class).newInstance(stockInvestor);
        }
        catch (Exception exception) {
            throw new RuntimeException("Couldn't find strategy: " + this.investorStrategy.getSimpleName());
        }
    }

    @Override
    public void setInvestorStrategy(Class<? extends AbstractInvestorStrategy> class_) {
        try {
            class_.getConstructor(StockInvestor.class);
            this.investorStrategy = class_;
        }
        catch (NoSuchMethodException noSuchMethodException) {
            System.err.println("Couldn't change investor strategy: " + noSuchMethodException.getMessage());
        }
    }
}

