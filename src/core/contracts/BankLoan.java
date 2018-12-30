/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Account;
import core.Agent;
import core.Bank;
import core.Loan;

public class BankLoan
extends Loan {
    static String name = "Bank Loan";
    static int frequency = 30;
    double variableInterestRate;

    public BankLoan(Bank bank, long l, double d, int n, int n2, Account account, Loan.Type type) {
        super(l, d, n, frequency, n2, account, type);
        this.ownerAcct = bank.getAccount();
        this.ownerId = this.ownerAcct.owner.Id;
        this.variableInterestRate = d;
        if (this.borrowerId == -1) {
            throw new RuntimeException("Borrower ID unset in BankLoan");
        }
    }

    @Override
    public String toString() {
        return "Bank Loan: " + this.Id + " " + this.capitalAmount + " @ " + this.interestRate + "%/" + this.duration + "[" + this.ownerAcct.getName() + "=>" + this.borrowerId + "/ " + this.loanType.name() + " " + this.borrower.getName() + "]";
    }
}

