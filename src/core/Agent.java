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
import core.Bank;
import core.Config;
import core.Employee;
import core.Govt;
import core.GsonAgent;
import core.Inventory;
import core.LabourMarket;
import core.Loan;
import core.Market;
import core.Markets;
import core.Person;
import core.Region;
import core.Shares;
import core.Treasury;
import core.Widget;
import java.awt.Color;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import statistics.Statistic;

public abstract class Agent {
    @Expose
    public String name = "";
    @Expose
    public long initialDeposit = 0L;
    @Expose
    public String bankname = "";
    @Expose
    public double x;
    @Expose
    public double y;
    @Expose
    public double defaultProb = 0.0;
    @Expose
    public String regionName = "";
    public long offeredSalary = 1L;
    public Govt govt;
    public Region region = null;
    public Markets markets;
    public Integer Id = Base.assignID();
    public Color myColor;
    Config config = null;
    public int output;
    public int bankrupt;
    private Account[] accounts = new Account[1];
    public HashMap<String, Inventory> shareholdings = new HashMap();
    private LinkedList<Treasury> treasuries = new LinkedList();
    public LinkedList<Person> employees = new LinkedList();
    public Statistic s_income;
    public long c_salariesPaid = 0L;
    public boolean paidTaxes;
    public boolean paidDebts;

    Agent(String string, long l, Govt govt, Bank bank) {
        this();
        this.name = string == null ? "ID-" + this.Id : string;
        this.initialDeposit = l;
        if (bank != null) {
            this.bankname = bank.name;
        }
        if (govt != null) {
            this.init(govt);
        }
    }

    Agent() {
        if (!this.regionName.equals("")) {
            this.region = this.govt.regions.get(this.regionName);
            if (this.region == null) {
                throw new RuntimeException("Error: region " + this.regionName + " set but no region available.");
            }
        }
        this.s_income = new Statistic(this.Id + ":income", Statistic.Type.COUNTER);
    }

    public void init(Govt govt) {
        Object object;
        this.govt = govt;
        if (!this.regionName.equals("")) {
            this.region = this.govt.regions.get(this.regionName);
        }
        if (this.govt != null) {
            this.markets = this.govt.markets;
        } else {
            System.out.println("**Error: govt == null in Agent init()**");
        }
        if (this.name == null) {
            this.name = "ID-" + this.Id;
        } else {
            object = this.name.split("-");
            if (((String[])object).length == 2) {
                try {
                    if (Integer.parseInt(object[1]) >= Base.getNextID()) {
                        Base.setID((int)(Integer.parseInt(object[1]) + 1));
                    }
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
        }
        object = this.govt.getBank(this.bankname);
        if (object != null) {
            try {
                this.accounts[0] = object.createAccount(this, this.initialDeposit);
            }
            catch (Exception exception) {
                throw new RuntimeException("Error creating account: " + this.name + " @ " + object.name + " " + exception + "\n Verify that this agent type has empty constructor");
            }
        } else if (!(this instanceof Bank) && !(this instanceof Govt)) {
            System.out.println("null Bank in Agent initialisation " + this.name + " " + this.bankname);
        }
    }

    public void evaluate(int n, boolean bl) {
        if (this.getAccount() != null && this.getAccount().debts.size() > 1) {
            System.out.println("**  size = " + this.getAccount().debts.size() + " " + this.name);
            throw new RuntimeException("Account has more than one loan");
        }
        this.paidDebts = false;
        this.paidTaxes = false;
        this.evaluate(bl, n);
    }

    public void evaluate() {
    }

    public Account getAccount() {
        return this.accounts[0];
    }

    public void addAccount(Account account) {
        Account[] arraccount = new Account[this.accounts.length + 1];
        for (int i = 0; i < this.accounts.length; ++i) {
            arraccount[i] = this.accounts[i];
        }
        arraccount[i] = account;
        this.accounts = arraccount;
    }

    public long getDeposit() {
        try {
            return this.getAccount().getDeposit();
        }
        catch (Exception exception) {
            System.out.println("No account for: " + this.name + "  " + exception);
            return 0L;
        }
    }

    public String getCountryName() {
        return this.govt.country;
    }

    public String getRegionName() {
        if (this.region != null) {
            return this.region.name;
        }
        return null;
    }

    public String getBankName() {
        return this.getAccount().bank.name;
    }

    public Bank getBank() {
        if (this.getAccount() != null) {
            return this.getAccount().bank;
        }
        return null;
    }

    public void addLoan(Loan loan) {
        if (!loan.ownerId.equals(this.Id)) {
            throw new RuntimeException("E: Loan has incorrect toId for agent");
        }
        if (this.getAccount().capital_loans.get(loan.Id) != null) {
            throw new RuntimeException("Duplicate loan id in addLoan" + loan);
        }
        this.getAccount().capital_loans.put(loan.Id, loan);
    }

    public void addDebt(Loan loan) {
        if (loan.borrowerId.equals(this.Id)) {
            throw new RuntimeException("E: Loan is already owned by agent");
        }
        if (this.getAccount().debts.get(loan.Id) != null) {
            throw new RuntimeException("Duplicate loan id in addLoan" + loan);
        }
        this.getAccount().debts.put(loan.Id, loan);
    }

    public boolean loanPaymentDue() {
        for (Loan loan : this.getAccount().debts.values()) {
            if (!loan.installmentDue()) continue;
            return true;
        }
        return false;
    }

    public void payDebt() {
        this.paidDebts = true;
        for (Loan loan : this.getAccount().debts.values()) {
            if (!loan.installmentDue()) {
                return;
            }
            if (loan.inDefault && loan.hasCollateral()) {
                System.out.println(this.name + " liquidating " + loan.collateral.name);
                System.out.println("Remaining capital " + loan.getCapitalOutstanding());
                long l = this.markets.getMarket(loan.collateral.name).getAskPrice();
                this.markets.getMarket(loan.collateral.name).sell(loan.collateral, l - 10L, this.getAccount());
                loan.collateral = null;
                loan.inDefault = false;
                loan.inWriteOff = false;
            }
            if (Base.random.nextDouble() <= this.defaultProb) {
                loan.putLoanIntoDefault();
            }
            this.getAccount().payLoan(loan);
            if (!loan.repaid()) continue;
            this.getAccount().debts.remove(loan.Id);
        }
    }

    public boolean transfer(long l, Agent agent, String string) {
        if (this.getDeposit() >= l) {
            this.getAccount().transfer(agent.getAccount(), l, string);
            return true;
        }
        return false;
    }

    public boolean buyInvestment(Agent agent, long l, int n, String string) {
        System.out.println(this.name + " buying investment from " + agent.name);
        if (l > this.getDeposit()) {
            throw new RuntimeException("BankInvestor buyCapital exceeds funds");
        }
        Object object = agent.sellInvestment(this, l, n, string);
        return object != null;
    }

    public boolean buyInvestment(Agent agent, long l, String string) {
        if (l > this.getDeposit()) {
            throw new RuntimeException("BankInvestor buyCapital exceeds funds");
        }
        Object object = agent.sellInvestment(this, l, string);
        return object != null;
    }

    public void addInvestment(Object object) {
        if (object instanceof Shares) {
            Shares shares = (Shares)object;
            Inventory inventory = this.shareholdings.get(shares.name);
            if (inventory == null) {
                inventory = new Inventory(shares.name, shares.hasTTL(), false);
                inventory.add(shares);
                this.shareholdings.put(shares.name, inventory);
            } else {
                inventory.add(shares);
                if (shares.quantity == 0L) {
                    shares.masterList.remove(shares);
                }
            }
        } else if (object instanceof Treasury) {
            this.treasuries.add((Treasury)object);
        } else {
            throw new RuntimeException("*** Unknown investment in addInvestment ***");
        }
    }

    public long getShareholding(String string) {
        if (this.shareholdings.get(string) != null) {
            return this.shareholdings.get(string).getTotalItems();
        }
        return 0L;
    }

    public long transferShares(String string, long l, Agent agent) {
        if (l <= 0L) {
            throw new RuntimeException("Invalid transfer quantity: " + l);
        }
        if (agent == null) {
            throw new NullPointerException("Share recipient is null!");
        }
        Inventory inventory = this.shareholdings.get(string);
        if (inventory == null) {
            return 0L;
        }
        Shares shares = (Shares)inventory.remove(l);
        if (shares == null) {
            throw new RuntimeException("No shares found in inventory!");
        }
        shares.transfer(agent);
        return l;
    }

    public void printShareholding() {
        System.out.println("\nAgent " + this.name + " shareholding");
        for (Inventory inventory : this.shareholdings.values()) {
            System.out.println(inventory.product + ": " + inventory.getTotalItems());
        }
        System.out.println("\n");
    }

    public boolean payTax(int n, int n2) {
        this.paidTaxes = true;
        if (this.s_income.get() < (long)n2) {
            return true;
        }
        int n3 = (int)((double)((this.s_income.get() - (long)n2) * (long)n) / 100.0);
        if (this.getDeposit() > (long)n3 && n3 > 0) {
            this.govt.payPersonalTax(this.getAccount(), n3);
            return true;
        }
        return false;
    }

    public void setMyAccount(Account account) {
        this.accounts[0] = account;
    }

    public boolean hasDebt() {
        int n = 0;
        for (Account account : this.accounts) {
            n = (int)((long)n + account.getTotalDebt());
        }
        return n != 0;
    }

    public long getDebt() {
        long l = 0L;
        for (Account account : this.accounts) {
            l += account.getTotalDebt();
        }
        return l;
    }

    public boolean localBorrower() {
        if (!this.hasDebt()) {
            return true;
        }
        for (int i = 1; i < this.accounts.length; ++i) {
            if (this.accounts[0].bank == this.accounts[i].bank) continue;
            return false;
        }
        return true;
    }

    public boolean requestBankLoan(long l, int n, Base.Time time, int n2, Loan.Type type) {
        Loan loan = this.getAccount().requestLoan(l, n, time, n2, type);
        if (loan == null) {
            for (Bank bank : this.govt.getBankList().values()) {
                loan = bank.requestLoan(this.getAccount(), l, n, time, n2, type);
                if (loan == null) continue;
                return true;
            }
        }
        return false;
    }

    protected abstract void evaluate(boolean var1, int var2);

    public int getTreasuriesValue() {
        int n = 0;
        for (Treasury treasury : this.treasuries) {
            n = (int)((long)n + treasury.getCapitalOutstanding());
        }
        return n;
    }

    public long getSalaryBill() {
        int n = 0;
        for (Person person : this.employees) {
            n = (int)((long)n + person.getSalary());
        }
        return n;
    }

    public long paySalaries() {
        this.c_salariesPaid = 0L;
        Collections.shuffle(this.employees, Base.random);
        Iterator<Person> iterator = this.employees.iterator();
        while (iterator.hasNext()) {
            Person person = iterator.next();
            if (this.getDeposit() >= person.getSalary()) {
                Base.DEBUG((String)(this.name + " paying salary to " + person.name + " : " + person.getSalary()));
                this.c_salariesPaid += person.paySalary(this.getAccount());
                continue;
            }
            Base.DEBUG((String)(this.name + " Unable to pay :" + person.name + " [" + this.getDeposit() + "/" + person.getSalary() + "] insufficient funds (firing employee)"));
            this.fireEmployee(person, iterator);
        }
        return this.c_salariesPaid;
    }

    public Person hireEmployee() {
        return this.hireEmployee(this.markets.getLabourMarket().getAskPrice(), null, null);
    }

    public Person hireEmployee(long l, Bank bank, String string) {
        Inventory inventory = this.markets.getLabourMarket().hire(l, bank, string);
        if (inventory == null || inventory.getTotalItems() == 0L) {
            Base.DEBUG((String)(this.name + " failed to hire @ " + l + " available labour: " + this.markets.getLabourMarket().totalAvailableWorkers()));
            return null;
        }
        if (inventory.getTotalItems() == 1L) {
            Person person = ((Employee)inventory.remove()).person;
            if (person == null) {
                System.err.println("Error: null person");
            } else {
                this.hireEmployee(person, l);
            }
            return person;
        }
        throw new RuntimeException("Sanity failed: Expected one item inventory, got " + inventory.getTotalItems());
    }

    public void hireEmployee(Person person, long l) {
        if (person != null) {
            this.employees.addFirst(person);
            if (l < (long)this.govt.minWage) {
                System.out.println("Salary forced to min wage - was " + l);
                System.out.println(this.name);
                l = this.govt.minWage;
            }
            person.setEmployer(this, l, this.myColor);
        } else {
            System.err.println("Error: null person in hireEmployee");
        }
    }

    public Person fireEmployee(Person person, Iterator<Person> iterator) {
        if (iterator == null) {
            this.employees.remove(person);
        } else {
            iterator.remove();
        }
        person.setUnemployed();
        return person;
    }

    public Person fireEmployee() {
        Person person = null;
        if (this.employees.size() > 0) {
            person = this.employees.removeLast();
            person.setUnemployed();
        }
        return person;
    }

    public void fireAllEmployees() {
        Iterator<Person> iterator = this.employees.iterator();
        while (iterator.hasNext()) {
            Person person = iterator.next();
            iterator.remove();
            person.setUnemployed();
        }
    }

    public void increaseSalaries(long l) {
        Iterator<Person> iterator = this.employees.iterator();
        while (iterator.hasNext()) {
            Person person;
            Person person2 = person = iterator.next();
            person2.setSalary(person2.getSalary() + l);
        }
    }

    public String getName() {
        return this.name;
    }

    public String getInfo() {
        return this.govt.name + " " + this.getBank().name + " " + this.getDeposit();
    }

    public long getInitialDeposit() {
        return this.initialDeposit;
    }

    public Object sellInvestment(Agent agent, long l, String string) {
        System.out.println("Not implemented for this institution: " + this.name);
        return null;
    }

    public Object sellInvestment(Agent agent, long l, int n, String string) {
        System.out.println("Not implemented for this institution: " + this.name);
        return null;
    }

    public String save() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String string = gson.toJson((Object)this);
        GsonAgent gsonAgent = new GsonAgent(this.getClass().getSimpleName(), string);
        return gson.toJson((Object)gsonAgent);
    }

    public void setRegion(Region region) {
        if (region != null) {
            this.region = region;
            this.regionName = region.name;
        }
    }

    public String info() {
        return this.toString();
    }

    public static class Comparators {
        public static final Comparator<Agent> NAME = (agent, agent2) -> agent.name.compareTo(agent2.name);
    }

}

