/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Bank;
import core.Govt;

public class Region
extends Govt {
    public Region(String string, Govt govt, Bank bank) {
        super(string, bank.name, 0L);
        this.country = govt.country;
        govt.addRegion(this);
    }

    public Region() {
    }
}

