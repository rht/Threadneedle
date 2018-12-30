/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Account;
import core.Loan;
import java.io.PrintStream;

public class Treasury
extends Loan {
    static String name = "Treasury";
    static int frequency = 1;

    public Treasury(Account account, long l, double d, int n, int n2, Account account2) {
        super(l, d, n, frequency, n2, account, Loan.Type.COMPOUND);
    }

    @Override
    public void remove() {
        System.out.println("remove called on treasury");
        throw new RuntimeException("Non-bank loans cannot be auto-magically removed");
    }

    @Override
    public String toString() {
        return "Treasury: " + this.capitalAmount + "@" + this.interestRate + "%/" + this.duration + "[" + this.ownerAcct.getName() + "=>" + this.ownerId + "]";
    }
}

