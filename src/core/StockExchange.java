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
import core.StockMarket;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import statistics.Statistic;

public class StockExchange
extends Company {
    public Statistic s_index;
    public boolean verbose = false;
    public Statistic s_aindex;
    public Statistic s_bindex;
    public List<StockMarket> markets = new LinkedList<StockMarket>();
    public List<Agent> seats = new LinkedList<Agent>();
    public int numSeats;

    public static StockExchange findExchange(String string, Govt govt) {
        for (StockExchange stockExchange : govt.getStockExchanges()) {
            if (!stockExchange.name.equals(string)) continue;
            return stockExchange;
        }
        return null;
    }

    public static StockMarket findMarket(String string, Govt govt) {
        for (StockExchange stockExchange : govt.getStockExchanges()) {
            for (StockMarket stockMarket : stockExchange.markets) {
                if (!stockMarket.name.equals(string)) continue;
                return stockMarket;
            }
        }
        return null;
    }

    public StockExchange() {
        throw new UnsupportedOperationException("No default constructor implemented for StockExchange()");
    }

    public StockExchange(String string, Govt govt, Bank bank, HashMap<String, String> hashMap) {
        super(string, 0L, govt, bank);
        String string2 = hashMap.get("name");
        if (string2 != null) {
            this.name = string2;
        }
        this.verbose = hashMap.get("verbose") != null;
        this.numSeats = hashMap.get("seats") != null ? Integer.parseInt(hashMap.get("seats")) : 10;
        govt.registerStockExchange(this);
        this.s_index = new Statistic(this.name + "-index", "prices", Statistic.Type.SINGLE);
    }

    public boolean giveSeat(Company company) {
        if (this.numSeats > this.seats.size()) {
            this.seats.add(company);
            return true;
        }
        return false;
    }

    public boolean hasSeat(Agent agent) {
        return this.seats.contains(agent);
    }

    public void addStockMarket(StockMarket stockMarket) {
        if (this.markets.contains(stockMarket)) {
            throw new RuntimeException("StockMarket already on StockExchange!");
        }
        this.markets.add(stockMarket);
    }

    public StockMarket getRandom() {
        int n = Base.random.nextInt(this.markets.size());
        return this.markets.get(n);
    }

    public StockMarket getFirstOrRandom(Set<StockMarket> set) {
        Collections.shuffle(this.markets);
        for (StockMarket stockMarket : this.markets) {
            if (set.contains(stockMarket)) continue;
            return stockMarket;
        }
        return this.getRandom();
    }

    @Override
    public void evaluate(boolean bl, int n) {
        long l = 0L;
        long l2 = 0L;
        long l3 = 0L;
        Collections.sort(this.markets, (stockMarket, stockMarket2) -> stockMarket.name.compareToIgnoreCase(stockMarket2.name));
        for (StockMarket stockMarket3 : this.markets) {
            l += stockMarket3.lastPrice;
            l3 += stockMarket3.sellPrice;
            l2 += stockMarket3.getBidPrice();
        }
        if (this.markets.size() > 0) {
            l /= (long)this.markets.size();
            l3 /= (long)this.markets.size();
            l2 /= (long)this.markets.size();
        }
        this.s_index.add(l);
        if (this.verbose) {
            if (this.s_aindex == null || this.s_bindex == null) {
                this.s_aindex = new Statistic(this.name + "-index-ask", "prices", Statistic.Type.SINGLE);
                this.s_bindex = new Statistic(this.name + "-index-bid", "prices", Statistic.Type.SINGLE);
            }
            this.s_aindex.add(l3);
            this.s_bindex.add(l2);
        }
    }

    public void releaseSeat(Agent agent) {
        if (this.hasSeat(agent)) {
            this.seats.remove(agent);
        }
    }
}

