/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Account;
import core.AccountType;
import core.AccountingException;
import core.Agent;
import core.Bank;
import core.BaselWeighting;
import core.LedgerType;
import core.Loan;
import core.Transaction;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Ledger {
    public String name;
    private boolean debug = false;
    public static boolean postTransactions = true;
    private long balance = 0L;
    private boolean frozen = false;
    private boolean recordTransactions = false;
    public int accountId = -1;
    private AccountType type;
    public LedgerType ledgertype;
    public List<Transaction> transactions;
    public HashMap<Integer, Account> accounts;
    private long turnover = 0L;
    private long lastTotalDeposits;
    boolean changed = true;

    public Ledger(String string, String string2, AccountType accountType, LedgerType ledgerType) {
        this.name = string2;
        this.type = accountType;
        this.ledgertype = ledgerType;
        this.transactions = new ArrayList<Transaction>();
        this.accounts = new HashMap(100);
    }

    public int getAccountNo() {
        if (this.frozen) {
            return this.accountId;
        }
        return -1;
    }

    public void debit(Account account, long l, Transaction transaction) {
        long l2;
        if (this.debug) {
            System.out.println("debit ledger: " + this.name + " " + l);
            if (!this.accounts.containsValue(account)) {
                this.audit();
                throw new RuntimeException("Attempt to debit account not in ledger " + this.name + " " + account);
            }
        }
        if (account.deposit + (l2 = l * (long)(-1 * this.type.polarity())) < 0L) {
            throw new RuntimeException("Negative Balance in Account after debit " + account);
        }
        account.deposit += l2;
        this.addTransaction(transaction);
        if (this.type.polarity() < 0) {
            this.turnover += l;
        }
    }

    public void debit(Account account, Loan loan, Transaction transaction) {
        if (this.debug) {
            System.out.println("debit ledger: " + this.name + " " + loan);
            if (!this.accounts.containsValue(account)) {
                this.audit();
                throw new RuntimeException("Attempt to debit account not in ledger" + account);
            }
        }
        if (this.ledgertype == LedgerType.LOAN && this.type == AccountType.ASSET) {
            account.addCapitalLoan(loan);
        } else {
            account.addLoan(loan);
        }
        this.addTransaction(transaction);
        this.turnover += loan.getCapitalOutstanding();
    }

    public void credit(Account account, Loan loan, Transaction transaction) {
        if (this.debug) {
            System.out.println("credit ledger: " + this.name + " " + loan);
            if (!this.accounts.containsValue(account)) {
                throw new RuntimeException("Attempt to debit account not in ledger" + account);
            }
        }
        if (this.ledgertype == LedgerType.LOAN && this.type == AccountType.ASSET) {
            account.addCapitalLoan(loan);
        } else {
            account.addLoan(loan);
        }
        this.addTransaction(transaction);
        this.turnover += loan.getCapitalOutstanding();
    }

    public void credit(Account account, long l, Transaction transaction) {
        long l2;
        if (this.debug) {
            this.audit();
            System.out.println("credit ledger = " + this.name + " : " + l + " from " + account.owner.name);
            if (!this.accounts.containsValue(account)) {
                this.audit();
                throw new RuntimeException("Attempt to credit account not in ledger " + this.name + ": " + account);
            }
        }
        if (account.deposit + (l2 = l * (long)this.type.polarity()) < 0L) {
            throw new RuntimeException("Negative Balance in Account after credit " + account);
        }
        account.deposit += l2;
        this.addTransaction(transaction);
        if (this.type.polarity() > 0) {
            this.turnover += l;
        }
    }

    public void payLoan(Loan loan, long[] arrl, Transaction transaction) {
        loan.makePayment(arrl);
        this.addTransaction(transaction);
    }

    public void credit(Loan loan, long l, Transaction transaction) {
        if (loan.writeOff(l)) {
            loan.remove();
        }
        this.addTransaction(transaction);
    }

    public void credit(long l, Transaction transaction) {
        if (!this.frozen || this.accounts.size() != 1) {
            throw new RuntimeException("Cannot use with multi-account ledger");
        }
        this.credit(this.getAccount(), l, transaction);
        if (this.type.polarity() > 0) {
            this.turnover += l;
        }
    }

    public void debit(long l, Transaction transaction) {
        if (!this.frozen || this.accounts.size() != 1) {
            throw new RuntimeException("Cannot use with multi-account ledger");
        }
        this.debit(this.getAccount(), l, transaction);
        if (this.type.polarity() < 0) {
            this.turnover += l;
        }
    }

    public void addAccount(Account account) throws AccountingException {
        if (account.deposit != 0L) {
            throw new AccountingException("Account deposit != 0 " + account.getId());
        }
        if (this.accounts.containsKey(account.getId())) {
            throw new AccountingException("Ledger already contains account" + account.getId());
        }
        if (this.frozen) {
            throw new AccountingException("Attempt to add account to frozen ledger" + account.getId());
        }
        if (this.accounts.containsKey(account.getId())) {
            throw new AccountingException("Duplicate account key in ledger: " + this.name);
        }
        this.accounts.put(account.getId(), account);
        account.ledger = this.name;
    }

    public void addAccountAndClose(Account account) throws AccountingException {
        if (account.deposit != 0L) {
            throw new AccountingException("Account deposit != 0 " + account.getId());
        }
        this.addAccount(account);
        this.frozen = true;
        if (this.accounts.size() == 1) {
            this.accountId = account.accountId;
        }
    }

    public void removeAccount(Account account) {
        try {
            this.accounts.remove(account.accountId);
        }
        catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }

    public Account getAccount() {
        if (this.accounts.size() != 1) {
            throw new RuntimeException("Requested single account from multi-account ledger:" + this.name);
        }
        Object[] arrobject = this.accounts.values().toArray();
        assert (arrobject.length == 1);
        return (Account)arrobject[0];
    }

    public Account getAccount(Integer n) {
        return this.accounts.get(n);
    }

    public int debitPolarity() {
        return this.type.polarity() * -1;
    }

    public int creditPolarity() {
        return this.type.polarity();
    }

    public AccountType getType() {
        return this.type;
    }

    public LedgerType getLedgerType() {
        return this.ledgertype;
    }

    public boolean containsLoan(Loan loan) {
        return this.getAccount().holdsLoan(loan);
    }

    public void audit() {
        System.out.println("Audit : " + this.name);
        for (Account account : this.accounts.values()) {
            System.out.println("\t" + account.accountId + " " + account.bank.name);
        }
    }

    public long total() {
        switch (this.ledgertype) {
            case LOAN: {
                if (this.type == AccountType.ASSET) {
                    return this.totalCapital();
                }
                return this.totalLoans();
            }
            case CAPITAL: {
                return this.totalCapital();
            }
            case CASH: 
            case DEPOSIT: {
                if (this.changed) {
                    this.lastTotalDeposits = this.totalDeposits();
                    this.changed = false;
                }
                return this.lastTotalDeposits;
            }
        }
        throw new RuntimeException("Unhandled entry type in ledger (new enum??)");
    }

    private long totalDeposits() {
        long l = 0L;
        if (this.accounts != null) {
            for (Account account : this.accounts.values()) {
                l += account.getDeposit();
            }
            return l;
        }
        return 0L;
    }

    private long totalCapital() {
        long l = 0L;
        if (this.accounts != null) {
            for (Account account : this.accounts.values()) {
                l += account.getTotalCapital();
                if (this.type != AccountType.EQUITY) continue;
                assert (this.accounts.size() == 1);
                l += account.getDeposit();
            }
        }
        return l;
    }

    public void recalculateVariableLoans(double d) {
        if (this.accounts != null) {
            for (Account account : this.accounts.values()) {
                for (Loan loan : account.debts.values()) {
                    loan.interestRate = d;
                    loan.setCompoundSchedules(loan.payIndex);
                }
            }
        }
    }

    public long totalLoans() {
        long l = 0L;
        if (this.accounts != null) {
            for (Account account : this.accounts.values()) {
                l += account.getTotalDebt();
            }
        }
        return l;
    }

    public long riskWeightedTotalLoans() {
        long l = 0L;
        assert (!this.name.equals("loan"));
        if (this.accounts != null) {
            for (Account account : this.accounts.values()) {
                for (Loan loan : account.capital_loans.values()) {
                    l = (long)((double)l + (double)loan.getCapitalOutstanding() * BaselWeighting.riskWeighting(loan));
                }
            }
        }
        return l;
    }

    private void addTransaction(Transaction transaction) {
        this.changed = true;
        if (postTransactions) {
            this.transactions.add(transaction);
        }
    }

    public long getTurnover() {
        long l = this.turnover;
        this.turnover = 0L;
        return l;
    }

    public String getName() {
        return this.name;
    }

    public String toString() {
        return this.name;
    }

    public void printAccounts() {
        System.out.println("Ledger: " + this.name);
        for (Account account : this.accounts.values()) {
            System.out.println("Deposits");
            System.out.println("\t" + account);
            if (account.debts.size() > 0) {
                System.out.println("\nLoans");
                for (Loan loan : account.debts.values()) {
                    System.out.println("\t\tDebt:\t" + loan);
                }
            }
            if (account.capital_loans.size() <= 0) continue;
            System.out.println("\nCapital");
            for (Loan loan : account.capital_loans.values()) {
                System.out.println("\t\tCapital:\t" + loan);
            }
        }
        System.out.println("\n");
    }

}

