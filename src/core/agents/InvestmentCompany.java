/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  base.Base$Time
 */
package core;

import base.Base;
import core.AbstractInvestorStrategy;
import core.Account;
import core.Agent;
import core.Bank;
import core.Company;
import core.DefaultInvestmentStrategy;
import core.Employee;
import core.Govt;
import core.Inventory;
import core.InvestmentStrategy;
import core.InvestmentStrategyGoal;
import core.InvestorStrategy;
import core.LabourMarket;
import core.Loan;
import core.Markets;
import core.Person;
import core.StockExchange;
import core.StockInvestor;
import core.StockMarket;
import core.Widget;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class InvestmentCompany
extends Company {
    private StockExchange exchange;
    private InvestmentStrategy strategy;
    public boolean bankrupt;

    private static HashMap<String, String> createDefaultPropertiesMap() {
        HashMap<String, String> hashMap = new HashMap<String, String>();
        hashMap.put("initialDeposit", "300");
        hashMap.put("labourInput", "1");
        hashMap.put("product", "none");
        return hashMap;
    }

    public InvestmentCompany(String string, long l, Govt govt, Bank bank) {
        super(string, l, govt, bank);
        this.exchange = null;
        this.strategy = new DefaultInvestmentStrategy();
        this.bankrupt = false;
    }

    public InvestmentCompany(String string, Govt govt, Bank bank) {
        this(string, govt, bank, InvestmentCompany.createDefaultPropertiesMap());
        System.err.println("Warning: Using default values for InvestmentCompany.");
    }

    public InvestmentCompany(String string, Govt govt, Bank bank, HashMap<String, String> hashMap) {
        super(string, hashMap.get("initialDeposit") != null ? (long)Integer.parseInt(hashMap.get("initialDeposit")) : 0L, govt, bank);
        this.exchange = null;
        this.strategy = new DefaultInvestmentStrategy();
        this.bankrupt = false;
        String string2 = hashMap.get("name");
        if (string2 != null) {
            this.name = string2;
        }
        this.labourInput = hashMap.get("labourInput") != null ? Integer.parseInt(hashMap.get("labourInput")) : 1;
        this.product = hashMap.get("product");
        this.offeredSalary = hashMap.get("offeredSalary") != null ? Long.parseLong(hashMap.get("offeredSalary")) : 1L;
        this.exchange = StockExchange.findExchange(hashMap.get("exchange"), govt);
        String string3 = hashMap.get("strategy") != null ? hashMap.get("strategy") : "default";
        try {
            string3 = String.format("%s%s", string3.substring(0, 1).toUpperCase(), string3.substring(1).toLowerCase());
            try {
                Class<?> class_ = Class.forName("core." + string3 + "InvestmentStrategy");
                if (!InvestmentStrategy.class.isAssignableFrom(class_)) {
                    throw new ClassNotFoundException("Not a strategy: " + string3);
                }
                this.strategy = (InvestmentStrategy)class_.getConstructor(new Class[0]).newInstance(new Object[0]);
            }
            catch (ClassNotFoundException classNotFoundException) {
                Class<?> class_ = Class.forName("core." + string3 + "InvestorStrategy");
                if (InvestorStrategy.class.isAssignableFrom(class_)) {
                    if (this.strategy == null) {
                        this.strategy = new DefaultInvestmentStrategy();
                    }
                    this.strategy.setInvestorStrategy(class_);
                }
            }
        }
        catch (ClassNotFoundException classNotFoundException) {
            System.err.println("Couldn't find strategy: core." + String.format("%s%s", string3.substring(0, 1), string3.substring(1)));
            System.err.println(classNotFoundException.getMessage());
        }
        catch (Exception exception) {
            throw new RuntimeException("Unable to instantiate strategy: " + exception.getMessage());
        }
        finally {
            if (this.strategy == null) {
                this.strategy = new DefaultInvestmentStrategy();
            }
        }
        this.requestSeat();
    }

    public InvestmentCompany() {
        this.exchange = null;
        this.strategy = new DefaultInvestmentStrategy();
        this.bankrupt = false;
    }

    @Override
    protected void evaluate(boolean bl, int n) {
        this.payDebt();
        if (this.exchange == null) {
            System.err.println(this.name + " not participating on any exchanges!");
            return;
        }
        long l = this.getDeposit();
        if (!this.bankrupt && l == 0L && this.shareValue() + l < this.getDebt() && this.shareValue() == 0L) {
            this.bankrupt = true;
            this.exchange.releaseSeat(this);
        }
        if (this.bankrupt && this.totalOwnedShares() == 0L) {
            if (this.exchange.hasSeat(this)) {
                this.exchange.releaseSeat(this);
            }
            return;
        }
        if (this.bankrupt && this.shareValue() > 0L) {
            this.requestSeat();
            if (this.exchange.hasSeat(this)) {
                this.employees.stream().filter(person -> person instanceof StockInvestor).forEach(person -> ((StockInvestor)person).workWithStrategy(InvestmentStrategyGoal.LIQUIDATE));
            }
            this.exchange.releaseSeat(this);
        }
        if (!this.exchange.hasSeat(this) && !this.requestSeat()) {
            return;
        }
        long l2 = this.getAccount().getNextRepayment() + this.getSalaryBill();
        this.strategy.updateStrategy(l, l2);
        InvestmentStrategyGoal investmentStrategyGoal = this.strategy.getGoalFromStrategy();
        int n2 = 1;
        switch (investmentStrategyGoal) {
            case LIQUIDATE: 
            case CONTRACT: {
                this.seekFunding(this.strategy.neededFunds(), this.strategy.loanDuration());
                break;
            }
            default: {
                n2 = this.exchange.markets.size();
            }
        }
        if (this.employees.size() < n2) {
            this.hireInvestor(this.offeredSalary);
        }
        this.employees.stream().filter(person -> person instanceof StockInvestor).forEach(person -> ((StockInvestor)person).workWithStrategy(investmentStrategyGoal));
        for (Person person2 : this.employees) {
            person2.paySalary(this.getAccount());
        }
    }

    public boolean requestSeat() {
        if (this.exchange != null) {
            return this.exchange.giveSeat(this);
        }
        return false;
    }

    private boolean seekFunding(long l, int n) {
        if (this.bankrupt) {
            return false;
        }
        if (this.getAccount().debts.size() == 0) {
            if (this.getBank().zombie) {
                System.out.println("[WARNING] :: Bank is defunct!");
                System.err.println("Warning. Bank is zombie.");
            }
            try {
                return this.getBank().requestLoan(this.getAccount(), l, n, Base.Time.MONTH, 1, Loan.Type.COMPOUND) != null;
            }
            catch (RuntimeException runtimeException) {
                System.err.println(runtimeException.getMessage());
                runtimeException.printStackTrace();
                return false;
            }
        }
        return false;
    }

    public StockInvestor hireInvestor(long l) {
        return this.hireInvestor(l, null);
    }

    public StockInvestor hireInvestor(long l, String string) {
        Inventory inventory = this.markets.getLabourMarket().hire(l, string, StockInvestor.class);
        if (inventory == null || inventory.getTotalItems() == 0L) {
            Base.DEBUG((String)(this.name + " failed to hire @ " + l + "labour: " + this.markets.getLabourMarket().totalAvailableWorkers()));
            return null;
        }
        if (inventory.getTotalItems() == 1L) {
            StockInvestor stockInvestor = (StockInvestor)((Employee)inventory.remove()).person;
            if (stockInvestor == null) {
                System.err.println("Error: null investor");
            } else {
                this.hireEmployee(stockInvestor, -1L);
                stockInvestor.setInvestment(this.exchange.getFirstOrRandom(this.getCoverage()));
            }
            stockInvestor.setStrategy(this.strategy.getInvestorStrategy(stockInvestor));
            return stockInvestor;
        }
        throw new RuntimeException("Sanity failed: Expected one item inventory, got " + inventory.getTotalItems());
    }

    public Set<StockMarket> getCoverage() {
        HashSet<StockMarket> hashSet = new HashSet<StockMarket>();
        for (Person person : this.employees) {
            if (!(person instanceof StockInvestor)) continue;
            hashSet.add(((StockInvestor)person).stockMarket);
        }
        return hashSet;
    }

    @Override
    public String info() {
        String string = this.name;
        string = !this.bankrupt ? string + "\n+ " + String.format("%6d", this.getDeposit()) + "\n- " + String.format("%6d", this.getDebt()) + "\n---------\n  " + String.format("%6d", this.getDeposit() - this.getDebt()) + this.shareString() : string + " (bankrupt)";
        return string;
    }

    public String shareString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("\n");
        for (Inventory inventory : this.shareholdings.values()) {
            long l = inventory.getTotalItems();
            if (l == 0L) continue;
            stringBuilder.append(inventory.product);
            stringBuilder.append(": ");
            stringBuilder.append(String.format("%3d", l));
            StockMarket stockMarket = StockExchange.findMarket(inventory.product, this.govt);
            if (stockMarket != null) {
                stringBuilder.append(" (valued at: ");
                stringBuilder.append(stockMarket.getBidPrice());
                stringBuilder.append(" each, total ");
                stringBuilder.append(stockMarket.getBidPrice() * l);
                stringBuilder.append(") - Last traded at ");
                stringBuilder.append(stockMarket.lastPrice);
            }
            stringBuilder.append("\n");
        }
        return stringBuilder.toString();
    }

    public long shareValue() {
        long l = 0L;
        for (Inventory inventory : this.shareholdings.values()) {
            StockMarket stockMarket = StockExchange.findMarket(inventory.product, this.govt);
            if (stockMarket == null) continue;
            l += stockMarket.bidPrice * inventory.getTotalItems();
        }
        return l;
    }

    public long totalOwnedShares() {
        long l = 0L;
        for (Inventory inventory : this.shareholdings.values()) {
            l += inventory.getTotalItems();
        }
        return l;
    }

}

