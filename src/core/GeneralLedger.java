/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  au.com.bytecode.opencsv.CSVReader
 */
package core;

import au.com.bytecode.opencsv.CSVReader;
import core.Account;
import core.AccountType;
import core.Bank;
import core.Ledger;
import core.LedgerType;
import core.Loan;
import core.Transaction;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.function.Consumer;

public class GeneralLedger {
    public Bank myBank;
    public LinkedHashMap<String, Ledger> ledgers = new LinkedHashMap(30);
    public LinkedHashMap<String, Ledger> assets = new LinkedHashMap(10);
    public LinkedHashMap<String, Ledger> liabilities = new LinkedHashMap(10);
    public LinkedHashMap<String, Ledger> equities = new LinkedHashMap(10);

    public GeneralLedger(String string, Bank bank) {
        this.myBank = bank;
        this.setupLedgers(string);
    }

    public void transfer(Account account, Account account2, long l, String string) {
        if (this.ledger(account.ledger).creditPolarity() != this.ledger(account2.ledger).creditPolarity()) {
            throw new RuntimeException("Ledger type mismatch");
        }
        if (this.ledger(account.ledger).getType() == AccountType.ASSET) {
            this.post(account2.ledger, account2, account.ledger, account, l, string);
        } else {
            this.post(account.ledger, account, account2.ledger, account2, l, string);
        }
    }

    public void post(String string, Account account, String string2, Account account2, long l, String string3) {
        Ledger ledger = this.ledgers.get(string);
        Ledger ledger2 = this.ledgers.get(string2);
        if (ledger == null || ledger2 == null) {
            throw new RuntimeException("Unknown ledger in post: " + (ledger == null ? string : string2));
        }
        if (account2 == null || account == null) {
            throw new RuntimeException("Null " + (account == null ? "debit " : "credit ") + "account in post to " + (account == null ? string : string2));
        }
        this.post(ledger, account, ledger2, account2, l, string3);
    }

    public void post(Ledger ledger, Account account, Ledger ledger2, Account account2, long l, String string) {
        if (!this.checkPolarity(ledger, ledger2)) {
            System.out.println("" + ledger.debitPolarity() + " " + ledger2.creditPolarity());
            throw new RuntimeException("Unbalanced post attempted" + ledger.name + " " + ledger2.name);
        }
        Transaction transaction = new Transaction(string, account.getId(), account2.getId(), l);
        ledger.debit(account, l, transaction);
        ledger2.credit(account2, l, transaction);
    }

    public void post(Ledger ledger, Account account, Ledger ledger2, Account account2, Loan loan, String string, String string2) {
        if (!this.checkPolarity(ledger, ledger2)) {
            System.out.println("" + ledger.debitPolarity() + " " + ledger2.creditPolarity());
            throw new RuntimeException("Unbalanced post attempted" + ledger.name + " " + ledger2.name);
        }
        Transaction transaction = new Transaction(string2, account.getId(), account2.getId(), loan.capitalAmount);
        switch (string) {
            case "credit": {
                ledger.debit(account, loan.getCapitalOutstanding(), transaction);
                ledger2.credit(account2, loan, transaction);
                break;
            }
            case "debit": {
                ledger.debit(account, loan, transaction);
                ledger2.credit(account2, loan.getCapitalOutstanding(), transaction);
                break;
            }
            default: {
                throw new RuntimeException("Unrecognised side" + string);
            }
        }
    }

    public void postNegAm(String string, Account account, String string2, Loan loan, long[] arrl, String string3) {
        if (!this.ledger(string2).containsLoan(loan)) {
            throw new RuntimeException("post on loan not in ledger " + loan);
        }
        assert (loan.negAm);
        Transaction transaction = new Transaction("Loan payment ", account.getId(), this.ledger(string2).getAccount().getId(), arrl[0] + arrl[1]);
        this.ledger(string).debit(account, arrl[0] + arrl[1], transaction);
        long l = loan.getPrincipalIncrease();
        transaction = new Transaction("Neg-am adjust", this.ledger(string2).getAccount().getId(), this.ledger("non-cash").getAccount().getId(), l);
        this.ledger("non-cash").credit(l, transaction);
        loan.negAmCapital += l;
        transaction = new Transaction("Interest payment " + string3, account.getId(), loan.ownerAcct.getId(), arrl[Loan.INTEREST]);
        this.ledger(loan.ownerAcct.ledger).credit(loan.ownerAcct, arrl[Loan.INTEREST], transaction);
        long l2 = loan.getNegamDecrease(arrl[Loan.CAPITAL]);
        transaction = new Transaction("Neg-am capital repayment " + string3, this.ledger("non-cash").getAccount().getId(), this.ledger("interest_income").getAccount().getId(), l2);
        this.ledger("non-cash").debit(l2, transaction);
        this.ledger("interest_income").credit(l2, transaction);
        loan.negAmCapitalRecognised += l2;
        transaction = new Transaction("Principal payment " + string3, account.getId(), this.ledger(string2).getAccount().getId(), arrl[Loan.CAPITAL]);
        this.ledger(string2).payLoan(loan, arrl, transaction);
    }

    public void post(String string, Loan loan, String string2, Account account, long[] arrl, String string3) {
        if (!this.ledger(string).containsLoan(loan)) {
            throw new RuntimeException("post on loan not in ledger " + loan);
        }
        Transaction transaction = new Transaction("Loan payment ", account.getId(), this.ledger(string).getAccount().getId(), arrl[0] + arrl[1]);
        this.ledger(string2).credit(account, arrl[0] + arrl[1], transaction);
        this.ledger(string).payLoan(loan, arrl, transaction);
        transaction = new Transaction("Interest payment " + string3, account.getId(), loan.ownerAcct.getId(), arrl[0]);
        this.ledger(loan.borrower.ledger).debit(loan.borrower, arrl[0], transaction);
    }

    public void post(String string, Account account, String string2, Loan loan, long[] arrl, String string3) {
        if (!this.ledger(string2).containsLoan(loan)) {
            throw new RuntimeException("post on loan not in ledger " + loan);
        }
        Transaction transaction = new Transaction("Loan payment ", account.getId(), this.ledger(string2).getAccount().getId(), arrl[0] + arrl[1]);
        this.ledger(string).debit(account, arrl[0] + arrl[1], transaction);
        transaction = new Transaction("Capital payment " + string3, account.getId(), this.ledger(string2).getAccount().getId(), arrl[1]);
        this.ledger(string2).payLoan(loan, arrl, transaction);
        transaction = new Transaction("Interest payment " + string3, account.getId(), loan.ownerAcct.getId(), arrl[0]);
        this.ledger(loan.ownerAcct.ledger).credit(loan.ownerAcct, arrl[0], transaction);
    }

    public void postWriteOff(String string, Loan loan, String string2, Account account, long l, String string3) {
        if (l == 0L) {
            return;
        }
        Ledger ledger = this.ledger(string);
        Ledger ledger2 = this.ledger(string2);
        Transaction transaction = new Transaction("Loan write off vs " + string3 + ": " + loan.borrower.getName(), ledger.getAccount().getId(), account.getId(), l);
        ledger.credit(loan, l, transaction);
        ledger2.debit(account, l, transaction);
    }

    public Ledger ledger(String string) {
        if (this.ledgers.containsKey(string)) {
            return this.ledgers.get(string);
        }
        throw new RuntimeException("Unknown ledger: " + string);
    }

    public Ledger ledger(Object object) {
        return this.ledgers.get(object);
    }

    public Ledger ledger(String string, AccountType accountType) {
        switch (accountType) {
            case ASSET: {
                return this.assets.get(string);
            }
            case LIABILITY: {
                return this.liabilities.get(string);
            }
            case EQUITY: {
                return this.equities.get(string);
            }
        }
        throw new RuntimeException("Unknown AccountType");
    }

    public void printLedgers() {
        System.out.println("\n\nAssets\t\tLiabilities\t\tEquity");
        int n = this.getMaxLedgerLength();
        String[] arrstring = this.getBalanceSheetTitles(this.assets);
        String[] arrstring2 = this.getBalanceSheetTitles(this.liabilities);
        String[] arrstring3 = this.getBalanceSheetTitles(this.equities);
        int n2 = 0;
        int n3 = 0;
        int n4 = 0;
        for (int i = 0; i < n; ++i) {
            System.out.println(arrstring[n2] + "\t\t" + arrstring2[n3] + "\t\t\t" + arrstring3[n4]);
            if (n2 < arrstring.length - 1) {
                ++n2;
            }
            if (n3 < arrstring2.length - 1) {
                ++n3;
            }
            if (n4 >= arrstring3.length - 1) continue;
            ++n4;
        }
        System.out.println("\n");
    }

    private void setupLedgers(String string) {
        CSVReader cSVReader = null;
        try {
            cSVReader = new CSVReader((Reader)new FileReader(string));
        }
        catch (Exception exception) {
            System.out.println("Failed to open ledger definition file: " + string);
            System.exit(1);
        }
        try {
            String[] arrstring;
            while ((arrstring = cSVReader.readNext()) != null) {
                if (arrstring[0].equals("#")) continue;
                Ledger ledger = this.createLedger(arrstring[0], AccountType.valueOf(arrstring[1].trim()), LedgerType.valueOf(arrstring[2].trim()));
                if (!arrstring[3].trim().equals("single")) continue;
                Account account = this.myBank.createAccount(ledger, true);
                this.auditAccounts(account);
            }
        }
        catch (Exception exception) {
            throw new RuntimeException("Error reading ledger file" + exception);
        }
    }

    public Ledger createLedger(String string, AccountType accountType, LedgerType ledgerType) {
        Ledger ledger = new Ledger(this.myBank.getName(), string, accountType, ledgerType);
        if (this.ledgers.containsKey(ledger.name)) {
            throw new RuntimeException("Config failure, duplicate ledger name " + ledger.name);
        }
        this.ledgers.put(ledger.name, ledger);
        switch (ledger.getType()) {
            case ASSET: {
                this.assets.put(ledger.name, ledger);
                break;
            }
            case LIABILITY: {
                this.liabilities.put(ledger.name, ledger);
                break;
            }
            case EQUITY: {
                this.equities.put(ledger.name, ledger);
                break;
            }
            default: {
                throw new RuntimeException("Unknown account type in GeneralLedger" + (Object)((Object)ledger.getType()));
            }
        }
        return ledger;
    }

    public boolean audit(boolean bl) {
        long l = this.totalLedgers(this.assets);
        long l2 = this.totalLedgers(this.liabilities);
        long l3 = this.totalLedgers(this.equities);
        if (bl || l != l2 + l3) {
            System.out.println("Audit @ " + this.myBank.name);
            System.out.println("Assets: " + l + " Liabilities: " + l2 + " Equities: " + l3);
        }
        if (l != l2 + l3) {
            throw new RuntimeException(" *** Audit Failed *** ");
        }
        return true;
    }

    public void auditAccounts(Account account) {
        int n = 0;
        for (Ledger ledger : this.ledgers.values()) {
            if (ledger.getAccount(account.getId()) == null) continue;
            ++n;
        }
        if (n > 1) {
            throw new RuntimeException("Account in more than one ledger" + account);
        }
    }

    public void printAccounts() {
        System.out.println("\n=== " + this.myBank.name + "   Assets ====\n");
        this.assets.values().forEach(Ledger::printAccounts);
        System.out.println("\n===   Liabilities ====\n");
        this.liabilities.values().forEach(Ledger::printAccounts);
        System.out.println("\n====   Equity      ====\n");
        this.equities.values().forEach(Ledger::printAccounts);
    }

    public void audit(Ledger ledger) {
        ledger.printAccounts();
    }

    public long totalLiabilities() {
        return this.totalLedgers(this.liabilities) + this.totalLedgers(this.equities);
    }

    public long totalAssets() {
        return this.totalLedgers(this.assets);
    }

    private long totalLedgers(LinkedHashMap<String, Ledger> linkedHashMap) {
        long l = 0L;
        for (Ledger ledger : linkedHashMap.values()) {
            l += ledger.total();
        }
        return l;
    }

    private String[] getBalanceSheetTitles(LinkedHashMap<String, Ledger> linkedHashMap) {
        String[] arrstring = new String[linkedHashMap.size() + 1];
        int n = 0;
        for (Ledger ledger : linkedHashMap.values()) {
            arrstring[n++] = ledger.name;
        }
        arrstring[n] = "\t\t";
        return arrstring;
    }

    private int getMaxLedgerLength() {
        return Math.max(Math.max(this.assets.size(), this.equities.size()), this.liabilities.size());
    }

    private boolean checkPolarity(Ledger ledger, Ledger ledger2) {
        return Math.abs(ledger.debitPolarity() + ledger2.creditPolarity()) != 1;
    }

    private String getTitle(Ledger ledger) {
        if (ledger != null) {
            return ledger.name;
        }
        return "     ";
    }

    public long getTotalLoans() {
        long l = 0L;
        for (Ledger ledger : this.assets.values()) {
            if (ledger.getLedgerType() != LedgerType.LOAN) continue;
            l += ledger.total();
        }
        return l;
    }

}

