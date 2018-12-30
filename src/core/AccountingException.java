/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Ledger;

class AccountingException
extends Exception {
    public AccountingException(Ledger ledger, Ledger ledger2, String string) {
        super(string + " (" + ledger.name + "," + ledger2.name + ")");
    }

    public AccountingException(String string) {
        super(string);
    }
}

