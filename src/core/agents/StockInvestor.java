/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.Expose
 */
package core;

import com.google.gson.annotations.Expose;
import core.Agent;
import core.Bank;
import core.Company;
import core.DefaultInvestorStrategy;
import core.Govt;
import core.InvestmentCompany;
import core.InvestmentStrategyGoal;
import core.InvestorStrategy;
import core.Person;
import core.StockMarket;
import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StockInvestor
extends Person {
    int minInvestAmount = 50;
    @Expose
    StockMarket stockMarket = null;
    String investmentCompany = null;
    InvestorStrategy strategy = new DefaultInvestorStrategy(this);

    @Override
    public void evaluate() {
        throw new RuntimeException("Not implemented?");
    }

    @Override
    public void evaluate(boolean bl, int n) {
        super.evaluate(bl, n);
        this.payDebt();
        if (this.stockMarket != null) {
            long l = this.getDeposit();
            if (l > (long)this.minInvestAmount && this.stockMarket.placeOrder(StockMarket.OrderType.BID, 1L, this, 3L)) {
                System.out.println(this.name + " ordered investment (" + this.stockMarket.getName() + ")");
            }
            if (l < 5L && this.getShareholding(this.stockMarket.name) > 0L && this.stockMarket.placeOrder(StockMarket.OrderType.ASK, 1L, this, 3L)) {
                System.out.println(this.name + " trying to sell investment (" + this.stockMarket.getName() + ")");
            }
        }
    }

    public StockInvestor(String string, Govt govt, Bank bank, long l) {
        super(string, govt, bank, l);
        this.employer = this;
        this.setSalary(0L);
    }

    public StockInvestor(String string, Govt govt, Bank bank) {
        this(string, govt, bank, 0L);
    }

    public StockInvestor() {
    }

    @Override
    public void setSalary(long l) {
        this.salary = l;
        this.desiredSalary = l;
    }

    public void setInvestment(Company company) {
        this.investmentCompany = company.name;
        if (company instanceof StockMarket) {
            this.stockMarket = (StockMarket)company;
        }
    }

    public void buyShares() {
        this.buyShares(1L, 0L, 12L);
    }

    public void buyShares(long l, long l2, long l3) {
        if (this.stockMarket != null) {
            if (l2 != 0L) {
                this.stockMarket.placeOrder(StockMarket.OrderType.BID, l2, l, this.employer, l3);
            } else {
                this.stockMarket.placeOrder(StockMarket.OrderType.BID, l, this.employer, l3);
            }
        }
    }

    public void liquidate() {
        if (this.employer instanceof InvestmentCompany) {
            long l = this.employer.getShareholding(this.stockMarket.getName());
            if (l == 0L) {
                return;
            }
            this.sellShares(l, this.stockMarket.bidPrice, 1L);
        }
    }

    public void sellShares() {
        TreeSet<StockMarket.Order> treeSet = this.getOrders(StockMarket.OrderType.ASK);
        if (treeSet.size() == 0) {
            this.sellShares(1L, 0L, 12L);
            return;
        }
        long l = 1L;
        long l2 = 3L;
        long l3 = Long.MAX_VALUE;
        for (StockMarket.Order order : treeSet) {
            long l4;
            l += order.volume;
            long l5 = order.getTimeLeft();
            if (l2 < l5) {
                l2 = l5;
            }
            if ((l4 = order.getPrice()) < l3) {
                l3 = l4;
            }
            this.stockMarket.cancelOrder(order);
        }
        long l6 = l3 > this.stockMarket.getAskPrice() ? this.stockMarket.sellPrice : l3;
        this.sellShares(l, l6, l2);
    }

    public void sellShares(long l) {
        this.sellShares(l, 0L, 12L);
    }

    public void sellShares(long l, long l2, long l3) {
        if (l <= 0L) {
            return;
        }
        if (this.stockMarket != null) {
            if (l2 != 0L) {
                this.stockMarket.placeOrder(StockMarket.OrderType.ASK, l2, l, this.employer, l3);
            } else {
                this.stockMarket.placeOrder(StockMarket.OrderType.ASK, l, this.employer, l3);
            }
        } else {
            throw new RuntimeException("Stock market is null for shares!");
        }
    }

    public TreeSet<StockMarket.Order> getOrders(StockMarket.OrderType orderType) {
        return this.stockMarket.ordersBy(this.employer, orderType);
    }

    public Object sellInvestment(Agent agent, int n, int n2, String string) {
        throw new RuntimeException("Not implemented for this institution " + this.name);
    }

    public void cancelOrders(TreeSet<StockMarket.Order> treeSet) {
        if (treeSet == null) {
            this.getOrders(StockMarket.OrderType.ASK).forEach(this.stockMarket::cancelOrder);
            this.getOrders(StockMarket.OrderType.BID).forEach(this.stockMarket::cancelOrder);
        } else {
            this.getOrders(StockMarket.OrderType.ASK).stream().filter(treeSet::contains).forEach(this.stockMarket::cancelOrder);
            this.getOrders(StockMarket.OrderType.BID).stream().filter(treeSet::contains).forEach(this.stockMarket::cancelOrder);
        }
    }

    public InvestorStrategy getStrategy() {
        return this.strategy;
    }

    public void setStrategy(InvestorStrategy investorStrategy) {
        this.strategy = investorStrategy;
    }

    public void workWithStrategy(InvestmentStrategyGoal investmentStrategyGoal) {
        this.strategy.executeStrategy(investmentStrategyGoal);
    }

    public void allowSellAt(long l, long l2, long l3) {
        if (l <= this.stockMarket.bidPrice) {
            this.sellShares(this.employer.getShareholding(this.stockMarket.name));
        } else {
            long l4 = this.employer.getShareholding(this.stockMarket.name);
            this.sellShares(l4 / 2L, l, 3L);
            this.sellShares(l4 / 3L, l3, 6L);
        }
        TreeSet<StockMarket.Order> treeSet = new TreeSet<StockMarket.Order>();
        treeSet.addAll(this.getOrders(StockMarket.OrderType.ASK).stream().filter(order -> order.getPrice() < l).collect(Collectors.toList()));
        treeSet.addAll(this.getOrders(StockMarket.OrderType.BID).stream().filter(order -> order.getPrice() > l).collect(Collectors.toList()));
        this.cancelOrders(treeSet);
        if (l2 < l) {
            this.buyShares(1L, l2, 3L);
        }
    }

    public void sellIfBelow(long l) {
        if (this.stockMarket.sellPrice < l) {
            this.sellShares(this.employer.getShareholding(this.stockMarket.name));
        }
    }

    public void recordSale(long l, long l2) {
        int n = 0;
        while ((long)n < l2) {
            this.strategy.recordSale(l);
            ++n;
        }
    }

    public void recordPurchase(long l, long l2) {
        int n = 0;
        while ((long)n < l2) {
            this.strategy.recordPurchase(l);
            ++n;
        }
    }
}

