/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.AbstractInvestorStrategy;
import core.InvestmentStrategyGoal;
import core.StockInvestor;

public class DefaultInvestorStrategy
extends AbstractInvestorStrategy {
    public DefaultInvestorStrategy(StockInvestor stockInvestor) {
        super(stockInvestor);
    }

    @Override
    public void executeStrategy(InvestmentStrategyGoal investmentStrategyGoal) {
        switch (investmentStrategyGoal) {
            case LIQUIDATE: {
                this.investor.liquidate();
                break;
            }
            case CONTRACT: {
                this.investor.sellShares();
                break;
            }
            case EXPAND: {
                this.investor.buyShares();
                break;
            }
            default: {
                this.investor.buyShares();
                this.profitFromPurchases();
            }
        }
        this.protectAgainstLosses();
    }

}

