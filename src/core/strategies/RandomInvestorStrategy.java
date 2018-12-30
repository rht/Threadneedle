/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 */
package core;

import base.Base;
import core.AbstractInvestorStrategy;
import core.InvestmentStrategyGoal;
import core.StockInvestor;
import core.StockMarket;
import java.util.Random;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class RandomInvestorStrategy
extends AbstractInvestorStrategy {
    public RandomInvestorStrategy(StockInvestor stockInvestor) {
        super(stockInvestor);
    }

    @Override
    public void executeStrategy(InvestmentStrategyGoal investmentStrategyGoal) {
        switch (investmentStrategyGoal) {
            case CONTRACT: 
            case LIQUIDATE: {
                this.TradeConservatively();
                break;
            }
            default: {
                this.TradeRandomly();
            }
        }
    }

    private void TradeConservatively() {
        int n = Base.random.nextInt(3);
        switch (n) {
            case 0: 
            case 1: {
                this.investor.sellShares();
                break;
            }
            case 2: {
                this.investor.liquidate();
                break;
            }
        }
    }

    private void TradeRandomly() {
        int n = Base.random.nextInt(8);
        switch (n) {
            case 0: 
            case 2: {
                if (Base.random.nextBoolean()) {
                    this.investor.buyShares(Base.random.nextInt(10) + 1, 0L, Base.random.nextInt(33) + 3);
                }
            }
            case 1: 
            case 3: {
                if (Base.random.nextBoolean()) {
                    this.investor.sellShares();
                }
            }
            case 4: {
                if (Base.random.nextBoolean()) {
                    TreeSet<StockMarket.Order> treeSet = this.investor.getOrders(null);
                    TreeSet treeSet2 = treeSet.stream().filter(order -> Base.random.nextBoolean()).collect(Collectors.toCollection(TreeSet::new));
                    this.investor.cancelOrders(treeSet2);
                }
            }
            case 5: {
                if (Base.random.nextBoolean()) {
                    this.profitFromPurchases();
                }
            }
            case 6: {
                this.protectAgainstLosses();
            }
        }
    }

}

