/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Loan;

class Config {
    public String name;
    public String product;
    public String bankname;
    public String branch;
    public String employerName = null;
    Integer id;
    public long initialDeposit = -1L;
    public long initialSalary = -1L;
    public int labourInput = -1;
    public int personaltaxrate;
    public int corporatetaxrate;
    public double debtceiling;
    public Loan.Type loantype;
    public int loanDuration = -1;
    public long loanAmount = 10000L;
    public double capitalPct = 0.0;
    public int capitalSteps = 12;
    public int w;
    public int h;
    public int x;
    public int y;
    public int seed;

    public Config(String string, int n, long l, long l2, int n2, int n3, int n4, int n5) {
        this.name = string;
        this.id = n;
        this.initialDeposit = l;
        this.initialSalary = l2;
        this.x = n4;
        this.y = n5;
    }

    public Config(String string, String string2, int n, long l, int n2, int n3, int n4, int n5, int n6, int n7) {
        this.name = string;
        this.product = string2;
        this.id = n;
        this.initialDeposit = l;
        this.initialSalary = n2;
        this.labourInput = n3;
        this.x = n6;
        this.y = n7;
    }

    public Config(String string, String string2, long l, int n, int n2) {
        this.name = string;
        this.product = string2;
        this.initialDeposit = l;
        this.x = n;
        this.y = n2;
    }

    public Config(String string, long l, Loan.Type type, int n, int n2) {
        this.name = string;
        this.initialDeposit = l;
        this.loantype = type;
        this.x = n;
        this.y = n2;
    }

    public Config(String string, long l, int n, int n2) {
        this.name = string;
        this.initialDeposit = l;
        this.x = n;
        this.y = n2;
    }

    public String toString() {
        return "Id: " + this.id + " " + this.name + " " + this.product + " " + this.initialDeposit + " " + this.initialSalary;
    }
}

