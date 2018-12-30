/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 */
package core;

import base.Base;

public class Transaction {
    String text;
    int debitAccountId;
    int creditAccountId;
    long amount;
    int time;

    public Transaction(String string, int n, int n2, long l) {
        this.text = string;
        this.debitAccountId = n;
        this.creditAccountId = n2;
        this.amount = l;
        this.time = Base.step;
    }

    public String toString() {
        return "" + this.time + " Debit : " + this.debitAccountId + " Credit: " + this.creditAccountId + " amount  " + this.amount + " [" + this.text + "]";
    }

    public String toString(int n) {
        if (n == this.debitAccountId) {
            return this.toString();
        }
        if (n == this.creditAccountId) {
            return "/" + Base.step + " Credit: " + this.creditAccountId + " Debit : " + this.debitAccountId + " amount  " + this.amount + " [" + this.text + "]";
        }
        return this.toString();
    }
}

