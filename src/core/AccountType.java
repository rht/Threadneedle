/*
 * Decompiled with CFR 0_132.
 */
package core;

public enum AccountType {
    ASSET(-1),
    LIABILITY(1),
    EQUITY(1);
    
    private final int polarity;

    private AccountType(int n2) {
        this.polarity = n2;
    }

    int polarity() {
        return this.polarity;
    }
}

