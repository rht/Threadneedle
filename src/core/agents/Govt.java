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
import core.Account;
import core.Agent;
import core.Bank;
import core.Banks;
import core.CentralBank;
import core.Markets;
import core.Person;
import core.Region;
import core.StockExchange;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import statistics.Statistic;

public class Govt
extends Agent {
    @Expose
    public String country;
    @Expose
    public double capitalPct = 10.0;
    @Expose
    public int reservePct = 10;
    @Expose
    public boolean reserveControls = true;
    @Expose
    public boolean capitalControls = false;
    @Expose
    public boolean payUnemployment = false;
    @Expose
    public int unemploymentPayment = 0;
    @Expose
    public int personalTaxRate = 0;
    @Expose
    public int corporateTaxRate = 0;
    @Expose
    public int personalCutoff = 0;
    @Expose
    public int corporateCutoff = 0;
    @Expose
    public int maxCivilServants = 0;
    @Expose
    public double treasuryRate = 10.0;
    @Expose
    public int minWage = 1;
    public static String CB_LEDGERS = "ledgers/CB.def";
    public Bank govtBank;
    public Banks banks = new Banks();
    public HashMap<String, Region> regions = new HashMap();
    public List<StockExchange> stockExchanges = new ArrayList<StockExchange>();
    protected Account incomeTaxAccount;
    public CentralBank centralbank = null;
    public boolean hasCentralBank = false;
    public Account centralBankAccount = null;
    public Statistic s_totalMoneySupply;
    public Statistic s_totalActiveMoneySupply;
    public Statistic s_totalBankLoans;
    public Statistic s_totalRevenue;
    public Statistic s_totalCivilServants;
    public Statistic s_population;

    Govt() {
        super("", 0L, null, null);
        this.govt = this;
        this.Id = 0;
        this.country = "Unset";
        this.initialDeposit = 100L;
        this.markets = new Markets(this.getBank(), this, this.initialDeposit);
    }

    Govt(String string, String string2, long l) {
        super(string, l, null, null);
        this.govt = this;
        this.Id = 0;
        this.bankname = string2;
        this.country = string;
        this.init(this);
        this.markets = new Markets(this.getBank(), this, l);
        this.initStatistics();
    }

    public void initStatistics() {
        this.s_totalMoneySupply = Statistic.getStatistic((String)(this.name + ":Money Supply"), (String)"money", (Statistic.Type)Statistic.Type.AVERAGE);
        this.s_totalActiveMoneySupply = Statistic.getStatistic((String)"mvpt-M", (String)"mvpt", (Statistic.Type)Statistic.Type.COUNTER);
        this.s_totalBankLoans = Statistic.getStatistic((String)(this.name + ":Bank Credit Supply"), (String)"money", (Statistic.Type)Statistic.Type.AVERAGE);
        this.s_totalCivilServants = Statistic.getStatistic((String)(this.name + ":Employees"), (String)"civilServants", (Statistic.Type)Statistic.Type.COUNTER);
        this.s_totalRevenue = Statistic.getStatistic((String)(this.name + ":Revenue"), (String)"revenue", (Statistic.Type)Statistic.Type.COUNTER);
        this.s_population = Statistic.getStatistic((String)"population", null, (Statistic.Type)Statistic.Type.NUMBER);
    }

    @Override
    public void evaluate(boolean bl, int n) {
        if (this.getBank() == this.banks.centralBank) {
            this.s_totalMoneySupply.add(this.getDepositSupply() + this.getDeposit());
        } else {
            this.s_totalMoneySupply.add(this.getDepositSupply());
        }
        this.s_totalBankLoans.add(this.getTotalBankLoans());
        this.s_totalActiveMoneySupply.add(this.getTotalActiveDeposits());
        this.s_totalCivilServants.add((long)this.employees.size());
    }

    public void addRegion(Region region) {
        this.regions.put(region.name, region);
    }

    public void setGovtBank(Bank bank) {
        if (bank == null) {
            this.govtBank = this.banks.centralBank;
            System.out.println("No Bank specified in setGovtBank - using central bank: " + this.govt.govtBank);
        } else {
            this.govtBank = bank;
            this.bankname = bank.name;
            this.banks.addBank(bank);
        }
    }

    @Override
    public Bank getBank() {
        return this.govtBank;
    }

    public void addBank(Bank bank) {
        if (this.banks.getBank(bank.name) != null) {
            throw new RuntimeException("Attempt to add bank already in banklist: " + bank.name);
        }
        this.banks.addBank(bank);
    }

    public Bank getBank(String string) {
        return this.banks.getBank(string);
    }

    public final Banks setBanks(ArrayList<Person> arrayList) {
        if (this.hasCentralBank && this.centralbank == null) {
            this.centralbank = new CentralBank("Central Bank", this.govt, CB_LEDGERS);
            this.centralbank.capitalPct = this.capitalPct;
            this.centralbank.cb_reserve = this.reservePct;
            this.centralbank.capitalControls = this.capitalControls;
            this.centralbank.reserveControls = this.reserveControls;
            this.banks.addBank(this.centralbank);
            this.govtBank = this.banks.getBank(this.bankname);
            for (Region region : this.regions.values()) {
                region.centralbank = this.centralbank;
                region.hasCentralBank = true;
            }
        }
        if (this.govtBank instanceof CentralBank) {
            this.incomeTaxAccount = this.centralBankAccount = this.centralbank.createAccount(this, this.initialDeposit);
            this.setMyAccount(this.centralBankAccount);
        } else {
            this.centralBankAccount = this.centralbank.createAccount(this, 0L);
            this.setMyAccount(this.banks.getBank(this.bankname).createAccount(this, this.initialDeposit));
            this.incomeTaxAccount = this.getAccount();
        }
        return this.banks;
    }

    public long getDepositSupply() {
        return this.banks.getTotalBankDeposits();
    }

    public long getTotalBankLoans() {
        return this.banks.getTotalBankLoans();
    }

    public long getTotalActiveDeposits() {
        long l = 0L;
        for (Bank bank : this.banks.getBankList().values()) {
            l += bank.getTotalActiveDeposits();
        }
        return l;
    }

    public HashMap<String, Bank> getBankList() {
        return this.banks.getBankList();
    }

    public Bank lookupBank(String string) {
        if (this.banks == null) {
            System.out.println("** List of Banks has not been initialised **");
        }
        return this.banks.getBank(string);
    }

    public boolean payPersonalTax(Account account, int n) {
        this.s_totalRevenue.add((long)n);
        return this.payGovtTax(account, n);
    }

    public boolean payCorporateTax(Account account, int n) {
        this.s_totalRevenue.add((long)n);
        return this.payGovtTax(account, n);
    }

    private boolean payGovtTax(Account account, int n) {
        if (account.getDeposit() < (long)n) {
            throw new RuntimeException("Insufficient funds to pay tax");
        }
        if (n == 0) {
            return true;
        }
        account.transfer(this.incomeTaxAccount, n, "Tax payment");
        return true;
    }

    public int getTotalDebt() {
        return this.getAccount().debtOutstanding();
    }

    public void payUnemployment(Account account) {
        if (!this.payUnemployment) {
            return;
        }
        long l = this.unemploymentPayment;
        if (l < this.getDeposit()) {
            this.getAccount().transfer(account, l, "Unemployment pay");
        }
    }

    public boolean getExtendedMoneySupply() {
        return this.centralbank != null && this.centralbank.extendedMoneySupply;
    }

    public boolean getNonCashIncome() {
        return false;
    }

    public long getTotalBankIncome() {
        long l = 0L;
        for (Bank bank : this.banks.getBankList().values()) {
            l += bank.s_income.get();
        }
        return l;
    }

    public double getCPI() {
        if (this.centralbank != null) {
            return this.centralbank.getCPI();
        }
        return 0.0;
    }

    public long getMaxCivilServants() {
        double d;
        try {
            d = Math.ceil((double)(Statistic.getStatistic((String)"population").getCurrent() * (long)this.maxCivilServants) / 100.0);
        }
        catch (NullPointerException nullPointerException) {
            return 0L;
        }
        return Math.round(d);
    }

    public String getCountry() {
        return this.country;
    }

    public void setCountry(String string) {
        this.country = string;
    }

    public void print(String string) {
        System.out.println(string + ":" + this.name);
    }

    public CentralBank getCentralBank() {
        return this.centralbank;
    }

    public Iterable<? extends StockExchange> getStockExchanges() {
        return this.stockExchanges;
    }

    public void registerStockExchange(StockExchange stockExchange) {
        this.stockExchanges.add(stockExchange);
    }
}

