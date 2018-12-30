/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  statistics.Statistic
 */
package core;

import base.Base;
import core.Account;
import core.AccountType;
import core.Bank;
import core.BankLoan;
import core.Banks;
import core.GeneralLedger;
import core.Govt;
import core.InterbankLoan;
import core.Ledger;
import core.LedgerType;
import core.Loan;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import statistics.Statistic;

public class CentralBank
extends Bank {
    private ArrayList<Double> moneysupply = new ArrayList(1200);
    private int baseRate = 2;
    private long baseMoneySupply = 0L;
    public double interBankRate = 1.0;
    public int interBankDuration = 3;
    private static int T_PERIOD = 120;
    public double fixedCPI = 0.05;
    public boolean fixedcpi = true;
    public boolean extendedMoneySupply = true;
    public boolean nonCashIncome = true;
    public boolean capitalControls = false;
    public boolean reserveControls = true;
    public int cb_reserve = 10;
    public double capitalPct = 0.0;

    public CentralBank(String string, Govt govt, String string2) {
        super(string, govt, null);
        this.capitalSteps = 1;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            if (bank instanceof CentralBank) continue;
            this.addReserveAccount(bank);
        }
        this.baseMoneySupply = this.getMoneySupply();
        this.moneysupply.add(Double.valueOf(this.baseMoneySupply));
    }

    public void addReserveAccount(Bank bank) {
        if (bank instanceof CentralBank) {
            return;
        }
        long l = bank.getTotalDeposits();
        try {
            Ledger ledger = this.gl.createLedger(bank.name, AccountType.LIABILITY, LedgerType.DEPOSIT);
            this.createAccount(ledger, true);
        }
        catch (Exception exception) {
            throw new RuntimeException("Unable to create reserve account: " + exception);
        }
        if (l > 0L) {
            throw new RuntimeException("Initialising Bank which already has deposits");
        }
    }

    @Override
    public void evaluate(boolean bl, int n) {
        this.moneysupply.add(Double.valueOf(this.getMoneySupply()));
    }

    public CentralBank(String[] arrstring, Govt govt) {
        this(arrstring[1], govt, "src/resources/ledgers/ledgers.def");
    }

    public boolean makeLoan(Account account, int n, int n2, long l) {
        BankLoan bankLoan = new BankLoan(this, l, n2, n, Base.step, account, Loan.Type.COMPOUND);
        account.makeLoan(bankLoan);
        this.gl.post(this.gl.ledger("loan"), this.gl.ledger("loan").getAccount(), this.gl.ledger("reserve"), account, bankLoan, "debit", "Bank loan");
        return true;
    }

    public long getDepositSupply() {
        long l = 0L;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            if (bank instanceof CentralBank) continue;
            l += (long)bank.getTotalDeposits();
        }
        return l;
    }

    public int getNewBankLending() {
        int n = 0;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            n = (int)((long)n + bank.s_newLending.get());
        }
        return n;
    }

    private int getTotalLedger(String string) {
        int n = 0;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            try {
                n = (int)((long)n + bank.gl.ledger(string).total());
            }
            catch (RuntimeException runtimeException) {}
        }
        return n;
    }

    public int getLoanSupply() {
        int n = 0;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            n = (int)((long)n + bank.getTotalLoans());
        }
        return n;
    }

    public int getCapitalReserveLimit() {
        int n = 0;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            n = (int)((long)n + bank.getCapitalLimit());
        }
        return n;
    }

    public InterbankLoan borrowReserves(Bank bank, long l) {
        InterbankLoan interbankLoan;
        int n = Loan.getMinLoan(this.interBankRate, this.interBankDuration);
        for (Bank bank2 : this.govt.banks.getBankList().values()) {
            interbankLoan = bank2.requestIBLoan(bank, n, this.interBankRate, this.interBankDuration, 3);
            if (interbankLoan == null) continue;
            this.transferReserves(bank2, bank, n);
            return new InterbankLoan(bank2, n, this.interBankRate, this.interBankDuration, Base.step, interbankLoan.Id);
        }
        System.out.println("** No IBL reserve funds available  - requested " + n + " **");
        interbankLoan = this.requestIBLoan(bank, n, this.interBankRate, this.interBankDuration, 3);
        if (interbankLoan != null) {
            System.out.println("** Central Bank is Lending as Last Resort ** ");
            return new InterbankLoan(this, n, this.interBankRate, this.interBankDuration, Base.step, interbankLoan.Id);
        }
        return null;
    }

    public void transferReserves(Bank bank, Bank bank2, long l) {
        if (l > 0L) {
            this.transfer(this.gl.ledger(bank.name).getAccount(), this.gl.ledger(bank2.name).getAccount(), l, "reserve transfer");
        } else {
            System.out.println("** Request to transfer " + l + " reserves blocked");
        }
    }

    public void setBaseRate(int n) {
        this.baseRate = n;
        for (Bank bank : this.govt.banks.getBankList().values()) {
            bank.recalculateVariableLoans();
        }
    }

    public void setReserveRate(int n) {
        this.cb_reserve = n;
    }

    public double getCPI() {
        if (this.fixedcpi) {
            return this.fixedCPI;
        }
        if (this.moneysupply.size() < 2) {
            return 0.0;
        }
        int n = this.moneysupply.size() - 1;
        double d = (this.moneysupply.get(n) - this.moneysupply.get(n - 1)) / this.moneysupply.get(n - 1);
        System.out.println("Calculated CPI = " + d);
        System.out.println(this.moneysupply.get(n) + " " + this.moneysupply.get(n - 1));
        return d;
    }

    public int getBaseRate() {
        return this.baseRate;
    }

    public double getFixedCPI() {
        return this.fixedCPI;
    }

    private long getMoneySupply() {
        long l = this.getDepositSupply();
        if (this.extendedMoneySupply) {
            l += (long)this.getTotalLedger("interest_income");
        }
        if (this.nonCashIncome) {
            l += (long)this.getTotalLedger("non-cash");
        }
        return l;
    }

    @Override
    public InterbankLoan requestIBLoan(Bank bank, long l, double d, int n, int n2) {
        return null;
    }
}

