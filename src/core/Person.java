/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  base.Base$Time
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.annotations.Expose
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import base.Base;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import core.Account;
import core.Agent;
import core.Bank;
import core.Employee;
import core.Govt;
import core.GsonAgent;
import core.Inventory;
import core.LabourMarket;
import core.Loan;
import core.Market;
import core.Markets;
import core.Need;
import core.Profile;
import core.Want;
import core.Widget;
import java.awt.Color;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.BiConsumer;
import statistics.Statistic;

public class Person
extends Agent {
    @Expose
    public Profile profile = new Profile();
    @Expose
    public long desiredSalary = 50L;
    @Expose
    public boolean randomPurchase;
    public Agent employer = null;
    private int age;
    private int myConsumption;
    private int myPrice = 1;
    private int consumption_Food = 2;
    protected long salary;
    public boolean unemployed = false;
    public int unemployedTime = 0;
    Statistic s_consumption;

    public Person(String string, Govt govt, Bank bank, HashMap<String, String> hashMap) {
        this(string, govt, bank, Integer.parseInt(hashMap.get("initialDeposit")));
    }

    public Person(String string, Govt govt, Bank bank, long l) {
        super(string, l, govt, bank);
    }

    public Person() {
    }

    @Override
    public void init(Govt govt) {
        super.init(govt);
        this.markets = govt.markets;
        if (this.profile == null) {
            this.profile = new Profile();
        }
        this.profile.needs.forEach((string, need) -> need.init());
        this.profile.wants.forEach((string, want) -> want.init());
        this.s_consumption = Statistic.getStatistic((String)"consumption", null, (Statistic.Type)Statistic.Type.COUNTER);
        this.govt.s_population.inc();
        if (this.desiredSalary <= (long)govt.minWage) {
            this.desiredSalary = govt.minWage;
        }
        this.setSalary(this.desiredSalary);
        this.setUnemployed();
    }

    @Override
    public String save() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String string = gson.toJson((Object)this);
        GsonAgent gsonAgent = new GsonAgent(this.getClass().getSimpleName(), string);
        return gson.toJson((Object)gsonAgent);
    }

    public void setEmployer(Agent agent, long l, Color color) {
        this.employer = agent;
        this.myColor = color;
        this.setSalary(l);
    }

    public boolean unemployed() {
        return this.employer == this.markets.getMarket("Labour") || this.employer == null;
    }

    public long paySalary(Account account) {
        if (this.salary == 0L) {
            return 0L;
        }
        this.s_income.add(this.salary);
        if (account.transfer(this.getAccount(), this.salary, "salary:" + account.getName() + "->" + this.name)) {
            return this.salary;
        }
        return 0L;
    }

    public long paySalary(Account account, long l) {
        this.s_income.add(l);
        if (this.salary == 0L) {
            return 0L;
        }
        if (account.transfer(this.getAccount(), l, "salary/" + account.getName())) {
            return l;
        }
        return 0L;
    }

    public long setPaySalary(Account account, long l) {
        this.setSalary(l);
        return this.paySalary(account, this.salary);
    }

    public void reduceSalary(long l) {
        this.setSalary(this.salary - l);
    }

    public void setUnemployed() {
        if (!this.unemployed) {
            LabourMarket labourMarket = (LabourMarket)this.markets.getMarket("Labour");
            labourMarket.sell(new Employee(this, this.desiredSalary, -1, -1));
            this.setEmployer(labourMarket, this.salary, Color.RED);
        }
    }

    public void setSelfEmployed() {
        if (this.unemployed) {
            ((LabourMarket)this.markets.getMarket("Labour")).hire(this);
        } else {
            this.employer.fireEmployee(this, null);
        }
        this.employer = this;
    }

    public int getDemand() {
        return this.profile.getTotalDemand();
    }

    public void setMarket(Markets markets) {
        this.markets = markets;
        if (this.employer == null) {
            LabourMarket labourMarket = this.markets.getLabourMarket();
            if (labourMarket == null) {
                throw new RuntimeException("No labour market for " + this.name);
            }
            labourMarket.sell(new Employee(this, this.desiredSalary, -1, -1));
        }
    }

    @Override
    protected void evaluate(boolean bl, int n) {
        if (!this.paidDebts) {
            this.payDebt();
        }
        ArrayList<Need> arrayList = new ArrayList<Need>(this.profile.needs.values());
        if (this.randomPurchase) {
            Collections.shuffle(arrayList);
        }
        for (Need need : arrayList) {
            Object object;
            if (need.store.getTotalItems() >= (long)need.storeQ) continue;
            Market market = this.markets.getMarket(need.store.product);
            assert (market == null);
            if (market.useLoan) {
                object = market.getLowestPrice();
                long l = this.getDeposit() / 2L;
                if (object == null || this.getDebt() > 0L || this.unemployed() || object.price > this.getSalary() * 100L + l) continue;
                Loan loan = null;
                long l2 = object.price - l;
                int n2 = 120;
                loan = this.getBank().requestLoan(this.getAccount(), l2, n2, Base.Time.MONTH, 1, Loan.Type.COMPOUND);
                if (loan == null) continue;
                if ((object = market.buy((Widget)object, this, object.price)) == null) {
                    throw new RuntimeException("Purchase failed after loan granted");
                }
                need.store.add((Widget)object);
                loan.addCollateral((Widget)object);
                need.lastPricePaid = object.price;
                continue;
            }
            object = market.buy(-1L, need.getRequired(), this.getAccount());
            if (object == null) continue;
            need.store.merge((Inventory)object);
            need.lastPricePaid = object.getFirst().lastSoldPrice;
        }
        for (Need need : this.profile.needs.values()) {
            long l = need.consume();
            if (l <= 0L) continue;
            this.s_consumption.add(l);
        }
        if (this.unemployed()) {
            this.govt.payUnemployment(this.getAccount());
        }
        this.payTax(this.govt.personalTaxRate, this.govt.personalCutoff);
    }

    public void resetRoundStatistics() {
    }

    public String getCurrentSetup() {
        return this.name + " " + this.getBankName() + " " + this.getDeposit() + " " + this.employer.name;
    }

    public long getSalary() {
        return this.salary;
    }

    public void setSalary(long l) {
        this.salary = l >= (long)this.govt.minWage ? l : (long)this.govt.minWage;
    }

    public void setProfile(Profile profile) {
        this.profile = new Profile(profile);
    }

    public String getEmployer() {
        if (this.employer == null || this.employer instanceof LabourMarket) {
            return "unemployed";
        }
        return this.employer.name;
    }

    public void print(String string) {
        if (string != null) {
            System.out.println(string);
        }
        System.out.println(this);
    }

    public String toString() {
        return this.name + ": Salary=" + this.salary + " Deposit: $" + this.getAccount().getDeposit();
    }
}

