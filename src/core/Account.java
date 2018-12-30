/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  base.Base$Time
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableMap
 */
package core;

import base.Base;
import core.Agent;
import core.Bank;
import core.BankLoan;
import core.Icelandic;
import core.InterbankLoan;
import core.Ledger;
import core.Loan;
import core.Treasury;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

public class Account {
    public Agent owner;
    public Integer accountId;
    public Bank bank;
    public String ledger;
    public long deposit = 0L;
    public long incoming;
    public long outgoing;
    public ConcurrentHashMap<Integer, Loan> debts;
    public ConcurrentHashMap<Integer, Loan> capital_loans;
    private ObservableMap<Integer, Loan> obsLoans;
    private static final int BASE_ACCOUNTNO = 1000000;
    private static int nextIdNo = 1000000;

    public Account(Agent agent, Bank bank, long l) {
        this.bank = bank;
        this.owner = agent;
        this.deposit = l;
        this.accountId = Account.getNewAccountId();
        this.ledger = "deposit";
        this.debts = new ConcurrentHashMap(5);
        this.capital_loans = new ConcurrentHashMap(5);
        this.obsLoans = FXCollections.observableMap(this.capital_loans);
    }

    public Account(Bank bank, Ledger ledger) {
        this.bank = bank;
        this.owner = bank;
        this.deposit = 0L;
        this.ledger = ledger.name;
        this.accountId = Account.getNewAccountId();
        this.debts = new ConcurrentHashMap(5);
        this.capital_loans = new ConcurrentHashMap(5);
        this.obsLoans = FXCollections.observableMap(this.capital_loans);
    }

    public String getName() {
        return this.owner.getName();
    }

    public Integer getId() {
        return this.accountId;
    }

    public long getDeposit() {
        return this.bank.getDeposit(this.accountId);
    }

    public long getTotalDebt() {
        long l = 0L;
        for (Loan loan : this.debts.values()) {
            l += loan.getCapitalOutstanding();
        }
        return l;
    }

    public long getTotalBankDebt() {
        long l = 0L;
        for (Loan loan : this.debts.values()) {
            if (!(loan.ownerAcct.owner instanceof Bank)) continue;
            l += loan.getCapitalOutstanding();
        }
        return l;
    }

    public long getTotalCapital() {
        long l = 0L;
        for (Loan loan : this.capital_loans.values()) {
            l += loan.getCapitalOutstanding();
        }
        return l;
    }

    public boolean transfer(Account account, long l, String string) {
        if (this.getDeposit() < l) {
            System.out.println("@ " + this.getName() + "Insufficient funds (" + l + "/" + this.getDeposit() + ") for transfer to " + account.getName());
            return false;
        }
        return this.bank.transfer(this, account, l, string);
    }

    public Loan requestLoan(long l, int n, Base.Time time, int n2, Loan.Type type) {
        return this.bank.requestLoan(this, l, n, time, n2, type);
    }

    public void makeLoan(Loan loan) {
        if (this.debts.containsValue(loan)) {
            throw new RuntimeException("Loan already in debts container" + loan);
        }
        this.addLoan(loan, this.debts);
    }

    public void addCapitalLoan(Loan loan) {
        if (this.debts.containsValue(loan)) {
            throw new RuntimeException("Loan already in capitals container" + loan);
        }
        this.addLoan(loan, this.capital_loans);
    }

    public void addLoan(Loan loan) {
        System.out.println("Account addloan : " + loan);
        this.addLoan(loan, this.debts);
    }

    private void addLoan(Loan loan, ConcurrentHashMap<Integer, Loan> concurrentHashMap) {
        concurrentHashMap.put(loan.Id, loan);
        loan.addAccount(this);
    }

    public boolean payLoan(Loan loan) {
        if (loan instanceof Treasury) {
            return this.bank.payDebt(this, loan);
        }
        if (loan instanceof BankLoan || loan instanceof Icelandic) {
            return this.bank.payBankLoan(this, loan);
        }
        if (loan instanceof InterbankLoan) {
            return this.bank.payInterbankLoan(this, loan);
        }
        throw new RuntimeException("Unknown loan type in payLoan");
    }

    public boolean holdsLoan(Loan loan) {
        return this.capital_loans.containsKey(loan.Id) || this.debts.containsKey(loan.Id);
    }

    public Loan getLoanById(Integer n, String string) {
        if (!this.owner.name.equals(string)) {
            throw new RuntimeException("Request for loan on wrong account");
        }
        if (this.debts.containsKey(n)) {
            return this.debts.get(n);
        }
        if (this.capital_loans.containsKey(n)) {
            return this.capital_loans.get(n);
        }
        return null;
    }

    public void removeLoan(Loan loan) {
        if (this.capital_loans.containsKey(loan.Id)) {
            this.capital_loans.remove(loan.Id);
        } else if (this.debts.containsKey(loan.Id)) {
            this.debts.remove(loan.Id);
        } else {
            throw new RuntimeException("Remove on loan not controlled by account" + loan);
        }
    }

    public int debtOutstanding() {
        int n = 0;
        for (Loan loan : this.debts.values()) {
            n = (int)((long)n + loan.getCapitalOutstanding());
        }
        return n;
    }

    public long getNextRepayment() {
        long l = 0L;
        for (Loan loan : this.debts.values()) {
            long[] arrl = loan.getNextLoanRepayment();
            l += arrl[0] + arrl[1];
        }
        return l;
    }

    public long getNextInterestRepayment() {
        long l = 0L;
        for (Loan loan : this.debts.values()) {
            l += loan.getNextInterestRepayment();
        }
        return l;
    }

    public long getTotalInterestPaid() {
        long l = 0L;
        for (Loan loan : this.debts.values()) {
            l += loan.interestPaid;
        }
        return l;
    }

    public long capitalOutstanding() {
        long l = 0L;
        for (Loan loan : this.capital_loans.values()) {
            l += loan.getCapitalOutstanding();
        }
        return l;
    }

    public Loan getRandomCapitalLoan() {
        Loan[] arrloan = new Loan[this.capital_loans.size()];
        this.capital_loans.values().toArray(arrloan);
        if (this.capital_loans.size() > 0) {
            return arrloan[Base.random.nextInt(this.capital_loans.size())];
        }
        return null;
    }

    private static int getNewAccountId() {
        return nextIdNo++;
    }

    public void audit() {
        System.out.println("Audit: " + this.owner.name);
        if (this.debts.size() > 0) {
            System.out.println("Debts: ");
        }
        this.debts.values().forEach(System.out::println);
        if (this.capital_loans.size() > 0) {
            System.out.println("Owed: ");
        }
        for (Loan loan : this.capital_loans.values()) {
            System.out.println(loan);
        }
        System.out.println();
    }

    public String toString() {
        return "Account: " + this.accountId + "(" + this.owner.name + ") @ " + this.bank.name + "  Deposit: " + this.deposit + " Ledger / " + this.ledger;
    }
}

