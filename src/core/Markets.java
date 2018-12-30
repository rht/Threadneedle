/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableList
 */
package core;

import core.Bank;
import core.Govt;
import core.LabourMarket;
import core.Market;
import core.Region;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class Markets {
    private Govt govt;
    Bank defaultbank;
    private long defaultdeposit;
    public LinkedList<Market> markets = new LinkedList();
    public ObservableList<Market> obsMarkets = FXCollections.observableList(this.markets);

    public Markets(Bank bank, Govt govt, long l) {
        this.govt = govt;
        this.defaultbank = bank;
        this.defaultdeposit = l;
    }

    public String createMarket(String string, String string2, Bank bank, long l, Region region) {
        if (this.getMarket(string2) != null) {
            return "A market for " + string2 + " already exists";
        }
        if (bank == null) {
            bank = this.defaultbank;
        }
        Market market = new Market(string, string2, this.govt, bank, l);
        this.addMarket(market);
        if (region != null) {
            market.setRegion(region);
        }
        return null;
    }

    public Market createMarket(String string) {
        if (this.getMarket(string) == null) {
            Market market = new Market("M-" + string, string, this.govt, this.defaultbank, this.defaultdeposit);
            this.addMarket(market);
        }
        return this.getMarket(string);
    }

    public void removeAll() {
        this.obsMarkets.removeAll((Object[])new Market[0]);
        this.markets.clear();
        this.govt = null;
        this.defaultbank = null;
    }

    public void addMarket(Market market) {
        for (Market market2 : this.markets) {
            if (!market2.getProduct().equals(market.getProduct())) continue;
            System.out.println("Error: Market < " + market.getProduct() + " > already in list");
            return;
        }
        this.obsMarkets.add((Object)market);
    }

    public void removeMarket(Market market) {
        this.markets.remove(market);
    }

    public Market getMarket(String string) {
        String string2 = null;
        for (Market market : this.markets) {
            if (market.getProduct().equals(string)) {
                return market;
            }
            if (!market.getProduct().equalsIgnoreCase(string)) continue;
            string2 = market.getProduct();
        }
        if (string2 != null) {
            System.out.println("***Market search is case sensitive and failed to find :" + string + "***\nDid find caseInsensitive match " + string2);
        }
        return null;
    }

    public LabourMarket getLabourMarket() {
        return (LabourMarket)this.getMarket("Labour");
    }

    public Iterator<Market> getIterator() {
        return this.markets.listIterator();
    }

    public void evaluate(int n, boolean bl) {
        for (Market market : this.markets) {
            if (market instanceof LabourMarket) {
                ((LabourMarket)market).evaluate(n, bl);
                continue;
            }
            market.evaluate(n, bl);
        }
    }

    public void print() {
        System.out.println("#Markets =" + this.markets.size());
        for (Market market : this.markets) {
            market.print();
        }
        System.out.println("\n");
    }
}

