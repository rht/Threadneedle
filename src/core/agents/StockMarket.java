/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import base.Base;
import core.Agent;
import core.Bank;
import core.Company;
import core.Govt;
import core.Inventory;
import core.Shares;
import core.StockExchange;
import core.StockInvestor;
import core.Widget;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NavigableSet;
import java.util.TreeSet;
import java.util.function.Predicate;
import statistics.Statistic;

public class StockMarket
extends Company {
    private NavigableSet<Order> bids;
    private NavigableSet<Order> asks;
    private StockExchange stockExchange;
    protected long bidPrice;
    protected long sellPrice;
    protected long lastPrice;
    public Statistic s_sellprice;
    public Statistic s_bidprice;
    public Statistic s_tradeprice;
    public Statistic s_avgprice;

    private StockMarket() {
        throw new UnsupportedOperationException("Not implemented: StockMarket default constructor");
    }

    public StockMarket(String string, Govt govt, Bank bank) {
        super(string, 0L, govt, bank);
        this.Initialize();
    }

    public StockMarket(String string, Govt govt, Bank bank, HashMap<String, String> hashMap) {
        StockExchange stockExchange;
        this(string, govt, bank);
        String string2 = hashMap.get("name");
        String string3 = hashMap.get("exchange");
        String string4 = hashMap.get("ipo");
        if (string2 != null) {
            this.name = string2;
        }
        if (string3 != null && (stockExchange = StockExchange.findExchange(string3, this.govt)) != null) {
            stockExchange.addStockMarket(this);
            this.stockExchange = stockExchange;
        }
        if (string4 != null) {
            long l = Long.parseLong(string4);
            this.introduce(l);
        }
    }

    private void introduce(long l) {
        Shares shares = new Shares(this.name, l, 1L, this, this.sharesIssued);
        this.addInvestment(shares);
        this.placeOrder(OrderType.ASK, 1L, l, this, -1L);
    }

    private void Initialize() {
        this.labourInput = 0;
        this.bidPrice = 0L;
        this.sellPrice = 0L;
        this.offeredSalary = 0L;
        this.bids = Collections.synchronizedNavigableSet(new TreeSet());
        this.asks = Collections.synchronizedNavigableSet(new TreeSet());
        this.initStatistics();
    }

    @Override
    public void initStatistics() {
        this.s_sellprice = new Statistic("S-" + this.name + ":ask-price", Statistic.Type.SINGLE);
        this.s_bidprice = new Statistic("S-" + this.name + ":bid-price", Statistic.Type.SINGLE);
        this.s_tradeprice = new Statistic("S-" + this.name + ":price", "StockPrices", Statistic.Type.SINGLE);
        this.s_avgprice = new Statistic("S-" + this.name + ":avg", "AvgStockPrices", Statistic.Type.AVERAGE);
    }

    public boolean placeOrder(OrderType orderType, long l, Agent agent, long l2) {
        if (l == 0L) {
            throw new IllegalArgumentException("Volume is non-positive!");
        }
        long l3 = 0L;
        if (orderType == OrderType.ASK) {
            if (!this.asks.isEmpty()) {
                l3 = this.asks.first().price - 1L;
                if (l3 <= 0L) {
                    l3 = 1L;
                }
                if (!this.bids.isEmpty()) {
                    if (l3 < this.bids.last().price) {
                        l3 = this.bids.last().price;
                    } else if (l3 > this.bids.last().price * 2L) {
                        l3 = Math.max((long)((double)this.bids.last().price * 1.5), this.asks.first().price / 3L);
                    }
                }
            } else {
                l3 = this.bids.isEmpty() ? Math.max(this.sellPrice, 1L) : (long)Math.max((double)this.sellPrice, (double)this.bids.last().price * 1.5 + 1.0);
            }
        } else if (orderType == OrderType.BID) {
            if (!this.bids.isEmpty()) {
                l3 = this.bids.last().price + 1L;
                this.asks.removeIf(order -> order.timeLeft == 0L || order.volume == 0L);
                if (!this.asks.isEmpty() && l3 > this.asks.first().price) {
                    long l4 = Math.min(this.asks.first().volume, l);
                    long l5 = l - l4;
                    boolean bl = this.placeOrder(orderType, this.asks.first().price, l4, agent, l2);
                    if (l5 == 0L) {
                        return bl;
                    }
                    return this.placeOrder(orderType, l5, agent, l2);
                }
            } else if (!this.asks.isEmpty()) {
                l3 = Math.max(1L, this.asks.first().price / 7L);
                l3 = Math.max(this.bidPrice, l3);
                l3 = Math.min(this.sellPrice, l3);
            } else {
                l3 = 1L;
            }
        }
        if (l3 > 0L) {
            return this.placeOrder(orderType, l3, l, agent, 12L);
        }
        return false;
    }

    public boolean placeOrder(OrderType orderType, long l, long l2, Agent agent, long l3) {
        Object object;
        if (l2 <= 0L) {
            throw new IllegalArgumentException("Volume is zero!");
        }
        if (!this.stockExchange.hasSeat(agent)) {
            if (agent instanceof StockInvestor) {
                System.out.println("[NOTIFY] " + agent.name + " trading shares without seat (need arbitrator to be implemented) on " + this.stockExchange);
            } else if (!(agent instanceof StockMarket)) {
                System.err.println("[WARN] " + agent.name + " trying to buy shares on " + this.stockExchange + " without seat!");
            }
        }
        if (orderType == OrderType.ASK) {
            object = agent.shareholdings.get(this.name);
            if (object == null) {
                return false;
            }
            Iterator<Widget> iterator = object.getIterator();
            long l4 = 0L;
            while (iterator.hasNext()) {
                l4 += iterator.next().quantity();
            }
            if (l4 < l2) {
                return false;
            }
        }
        object = new Order(orderType, l, l2, agent, l3);
        if (orderType == OrderType.ASK) {
            this.asks.add((Order)object);
        } else if (orderType == OrderType.BID) {
            this.bids.add((Order)object);
        }
        this.matchOrders();
        return true;
    }

    public boolean transferShares(String string, long l, long l2, Agent agent, Agent agent2) {
        if (agent2.getDeposit() < l2 * l) {
            return false;
        }
        long l3 = agent.getShareholding(string);
        if (l3 < l) {
            return false;
        }
        long l4 = agent.transferShares(string, l, agent2);
        assert (l4 == l);
        if (!agent2.transfer(l2 * l4, agent, "Shares: '" + string + "'x" + l4 + "@" + l2)) {
            agent2.transferShares(string, l4, agent);
            return false;
        }
        int n = 0;
        while ((long)n < l) {
            this.s_avgprice.add(l2);
            ++n;
        }
        return true;
    }

    public void orderTransaction(OrderMatch orderMatch) {
        if (orderMatch.volume == 0L) {
            throw new RuntimeException("0 sized order can't be transacted!");
        }
        if (orderMatch.ask == null || orderMatch.bid == null) {
            Base.DEBUG((String)"Transaction with expired order failed.");
            return;
        }
        if (orderMatch.seller == null || orderMatch.buyer == null) {
            Base.DEBUG((String)"Seller or buyer is null, break!");
            throw new RuntimeException("What the hell happened here! (buyer or seller null in orderTransaction)");
        }
        if (this.transferShares(this.name, orderMatch.volume, orderMatch.price, orderMatch.seller, orderMatch.buyer)) {
            orderMatch.bid.decreaseOrder(orderMatch.volume);
            orderMatch.ask.decreaseOrder(orderMatch.volume);
            if (orderMatch.buyer instanceof StockInvestor) {
                ((StockInvestor)orderMatch.buyer).recordPurchase(orderMatch.price, orderMatch.volume);
            }
            if (orderMatch.seller instanceof StockInvestor) {
                ((StockInvestor)orderMatch.seller).recordSale(orderMatch.price, orderMatch.volume);
            }
            this.lastPrice = orderMatch.price;
        } else {
            if (orderMatch.buyer.getDeposit() < orderMatch.price * orderMatch.volume) {
                this.cancelOrder(orderMatch.bid);
            }
            if (orderMatch.seller.getShareholding(this.name) < orderMatch.volume) {
                this.cancelOrder(orderMatch.ask);
            }
        }
    }

    @Override
    public void evaluate() {
        throw new UnsupportedOperationException("base evaluate is not done!");
    }

    @Override
    protected void evaluate(boolean bl, int n) {
        this.bids.removeIf(order -> order.elapseTime() == 0L || order.volume == 0L);
        this.asks.removeIf(order -> order.elapseTime() == 0L || order.volume == 0L);
        this.matchOrders();
        this.s_sellprice.add(Math.max(Math.max(0L, this.getAskPrice()), this.sellPrice));
        this.s_bidprice.add(this.getBidPrice());
        this.s_tradeprice.add(this.lastPrice);
    }

    private void matchOrders() {
        Order order2;
        Order order3;
        OrderMatch orderMatch;
        this.bids.removeIf(order -> order.timeLeft == 0L || order.volume == 0L);
        this.asks.removeIf(order -> order.timeLeft == 0L || order.volume == 0L);
        this.bids.removeIf(Order::illiquid);
        this.bids.removeIf(order -> order.volume > order.agent.getShareholding(this.name) && order.type == OrderType.ASK);
        TreeSet<Order> treeSet = new TreeSet<Order>();
        TreeSet<Order> treeSet2 = new TreeSet<Order>();
        treeSet.addAll(this.bids);
        treeSet2.addAll(this.asks);
        Iterator iterator = treeSet.descendingIterator();
        Iterator iterator2 = treeSet2.iterator();
        while (iterator.hasNext() && iterator2.hasNext() && (orderMatch = (order2 = (Order)iterator.next()).tryMatch(order3 = (Order)iterator2.next())) != null) {
            this.orderTransaction(orderMatch);
        }
    }

    public long numBids() {
        return this.bids.size();
    }

    public long numAsks() {
        return this.asks.size();
    }

    public TreeSet<Order> ordersBy(Agent agent, OrderType orderType) {
        TreeSet<Order> treeSet = new TreeSet<Order>();
        if (orderType == null || orderType == OrderType.ASK) {
            treeSet.addAll(this.asks);
        }
        if (orderType == null || orderType == OrderType.BID) {
            treeSet.addAll(this.bids);
        }
        treeSet.removeIf(order -> order.agent != agent);
        return treeSet;
    }

    public boolean cancelOrder(Order order) {
        if (order.type == OrderType.BID) {
            return this.bids.remove(order);
        }
        return this.asks.remove(order);
    }

    public long getBidPrice() {
        if (this.bids.isEmpty()) {
            return 0L;
        }
        this.bidPrice = this.bids.last().price;
        return this.bidPrice;
    }

    public long getAskPrice() {
        if (this.asks.isEmpty()) {
            return -1L;
        }
        this.sellPrice = this.asks.first().price;
        return this.sellPrice;
    }

    public void printOrders(Agent agent) {
        for (Order order : this.ordersBy(agent, null)) {
            System.out.println(this.name + " " + order.toString());
        }
    }

    public void printOrders() {
        System.out.println("--------[ASKS]---------");
        Iterator<Order> iterator = this.asks.descendingIterator();
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }
        iterator = this.bids.descendingIterator();
        System.out.println("--------[BIDS]---------");
        while (iterator.hasNext()) {
            System.out.println(iterator.next().toString());
        }
    }

    @Override
    public long getDeposit() {
        return 0L;
    }

    public class Order
    implements Comparable<Order> {
        private OrderType type;
        private long price;
        public long volume;
        private long timeLeft;
        private Agent agent;

        public long getPrice() {
            return this.price;
        }

        public Order(OrderType orderType, long l, long l2, Agent agent) {
            this.type = orderType;
            this.price = l;
            this.volume = l2;
            this.agent = agent;
            this.timeLeft = 12L;
            if (l2 <= 0L) {
                throw new RuntimeException(agent + " attempted to place an order with " + l2 + " volume.");
            }
            if (l <= 0L) {
                throw new RuntimeException(agent + " attempted to place an order with invalid price: " + l);
            }
        }

        public Order(OrderType orderType, long l, long l2, Agent agent, long l3) {
            this(orderType, l, l2, agent);
            this.timeLeft = l3;
        }

        public long decreaseOrder(long l) {
            if (this.volume - l < 0L) {
                throw new RuntimeException("Order decreased to below zero!");
            }
            this.volume -= l;
            return this.volume;
        }

        public long elapseTime() {
            if (this.timeLeft == -1L) {
                return -1L;
            }
            return --this.timeLeft;
        }

        public long getTimeLeft() {
            return this.timeLeft;
        }

        public OrderMatch tryMatch(Order order) {
            if (this.type == order.type) {
                return null;
            }
            if (this.volume == 0L || order.volume == 0L) {
                return null;
            }
            if (this.type == OrderType.BID && this.price >= order.price) {
                return new OrderMatch(this.price, Math.min(this.volume, order.volume), this.agent, order.agent, this, order);
            }
            if (this.type == OrderType.ASK && this.price <= order.price) {
                return new OrderMatch(this.price, Math.min(this.volume, order.volume), order.agent, this.agent, order, this);
            }
            return null;
        }

        public boolean illiquid() {
            if (this.type == OrderType.ASK) {
                return false;
            }
            return this.agent.getDeposit() < this.volume * this.price;
        }

        @Override
        public int compareTo(Order order) {
            int n = 0;
            if (this.price > order.price) {
                n = 1;
            } else if (this.price < order.price) {
                n = -1;
            }
            return n;
        }

        public String toString() {
            return this.type.toString() + "@" + Long.toString(this.price) + "x" + Long.toString(this.volume) + " [" + this.agent.name + "] time left: " + this.timeLeft;
        }
    }

    public class OrderMatch {
        public long price;
        public long volume;
        public Agent buyer;
        public Agent seller;
        public Order bid;
        public Order ask;

        public OrderMatch(long l, long l2, Agent agent, Agent agent2, Order order, Order order2) {
            this.price = l;
            this.volume = l2;
            this.buyer = agent;
            this.seller = agent2;
            this.bid = order;
            this.ask = order2;
        }
    }

    public static enum OrderType {
        BID,
        ASK;
        

        private OrderType() {
        }
    }

}

