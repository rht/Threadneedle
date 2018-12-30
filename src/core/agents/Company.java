/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.Expose
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import com.google.gson.annotations.Expose;
import core.Agent;
import core.Bank;
import core.Config;
import core.Govt;
import core.Market;
import core.Markets;
import core.Person;
import core.Shares;
import java.io.PrintStream;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ListIterator;
import statistics.Statistic;

public abstract class Company
extends Agent {
    @Expose
    public String product;
    @Expose
    public int labourInput;
    public long lastSoldPrice;
    public Market market;
    public LinkedList<Shares> sharesIssued = new LinkedList();
    public Statistic s_quantitySold;
    public Statistic s_quantityProduced;
    public Statistic s_labourCost;
    boolean constantSalary;

    public Company(String string, long l, Govt govt, Bank bank) {
        super(string, l, govt, bank);
    }

    public Company() {
    }

    public void initStatistics() {
        this.s_labourCost = new Statistic(this.Id + ":labour cost", Statistic.Type.COUNTER);
    }

    public void setMarkets(Markets markets) {
        if (markets == null) {
            throw new RuntimeException("Null market in setMarkets");
        }
        this.markets = markets;
        this.market = this.markets.getMarket(this.product);
    }

    public void setMarket(Market market) {
        this.market = market;
    }

    public void setBank(String string) {
        if (string == null) {
            throw new RuntimeException("Null bank in setBanks:" + this.name);
        }
        if (this.bankname != null) {
            throw new RuntimeException("Need to implement bank account transfer");
        }
        this.bankname = string;
    }

    public int getNoEmployees() {
        return this.employees.size();
    }

    public void changeProduct(String string) {
        this.product = string;
        this.market = this.markets.getMarket(string) == null ? this.markets.createMarket(string) : this.markets.getMarket(string);
        this.s_quantityProduced = Statistic.getStatistic((String)(string + "-produced"), (String)"production", (Statistic.Type)Statistic.Type.COUNTER);
    }

    public void setSalaries(long l) {
        Iterator iterator = this.employees.iterator();
        while (iterator.hasNext()) {
            Person person;
            Person person2 = person = (Person)iterator.next();
            person2.setSalary(l);
        }
    }

    public void decreaseSalaries(long l) {
        ListIterator listIterator = this.employees.listIterator();
        while (listIterator.hasNext()) {
            Person person = (Person)listIterator.next();
            if (person.getSalary() - l > (long)this.govt.minWage) {
                person.setSalary(person.getSalary() - l);
                continue;
            }
            person.setSalary(this.govt.minWage);
        }
        if (this.offeredSalary > (long)this.govt.minWage) {
            --this.offeredSalary;
        }
    }

    public void issueShares(long l, int n) {
        new Shares(this.name, n, l, this, this.sharesIssued).transfer(this);
    }

    public boolean payDividend(double d, LinkedList<Shares> linkedList) {
        int n = 0;
        assert (d >= 0.0);
        for (Shares shares : linkedList) {
            if (shares.owner != null && shares.owner != this) {
                n = (int)((double)n + shares.getDividend(d));
                continue;
            }
            System.out.println("Div: " + shares + " " + this.name);
        }
        if ((long)n > this.getDeposit()) {
            System.err.println(this.name + " unable to pay dividend of " + d + "% total:" + n + " insufficient funds");
            return false;
        }
        for (Shares shares : linkedList) {
            if (shares.owner == this || (int)shares.getDividend(d) <= 0) continue;
            this.transfer((int)shares.getDividend(d), shares.owner, String.format("%.2f", d) + "% Dividend on " + shares.name);
            System.out.println("Paid dividend " + shares.getDividend(d));
        }
        return true;
    }

    public int getTotalSharesIssued(LinkedList<Shares> linkedList) {
        int n = 0;
        for (Shares shares : linkedList) {
            if (!shares.issued()) continue;
            n = (int)((long)n + shares.quantity());
        }
        return n;
    }

    public void setConstantSalary(boolean bl) {
        if (this.constantSalary != bl) {
            System.err.println("Changing constant salary setting to " + bl);
            this.constantSalary = bl;
        }
    }

    public Market getOutputMarket() {
        return this.markets.getMarket(this.product);
    }

    public String getConfig() {
        return this.name + " " + this.config.bankname + " " + this.config.initialDeposit + " " + this.product;
    }

    public String getCurrentSetup() {
        return this.name + " " + this.product + " " + this.getBankName() + ": " + this.getDeposit() + " L x-" + this.labourInput;
    }

    public void print(String string) {
        if (string != null) {
            System.out.println(string);
        }
        System.out.println("Employees : " + this.employees.size() + " Input     : " + this.labourInput + " Output    : " + this.output);
    }
}

