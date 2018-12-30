/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Account;
import core.Agent;
import core.Bank;
import core.Loan;
import java.io.PrintStream;

public class InterbankLoan
extends Loan {
    static String name = "Interbank Loan";
    static int frequency = 1;
    Bank owner = null;
    String type = "";

    public InterbankLoan(Bank bank, long l, double d, int n, int n2, Account account, String string) {
        super(l, d, n, frequency, n2, account, Loan.Type.INTERBANKLOAN);
        this.ownerAcct = bank.getAccount();
        this.ownerId = this.ownerAcct.owner.Id;
        this.owner = bank;
        this.type = string;
        if (this.borrowerId == -1) {
            throw new RuntimeException("Borrower ID unset in BankLoan");
        }
    }

    public InterbankLoan(Bank bank, long l, double d, int n, int n2, Integer n3) {
        super(l, d, n, frequency, n2, null, Loan.Type.INTERBANKLOAN);
        this.ownerAcct = bank.getAccount();
        this.owner = bank;
        this.Id = n3;
    }

    @Override
    public String toString() {
        System.out.println(" ==> " + this.owner + " : " + this.borrower);
        return "Bank Loan: " + this.Id + " " + this.capitalAmount + " @ " + this.interestRate + "%/" + this.duration + "[" + this.owner.name + "=>" + this.borrowerId + "/ " + this.loanType.name() + " ]";
    }
}

