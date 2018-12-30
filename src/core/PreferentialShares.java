/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Company;
import core.Shares;
import java.util.LinkedList;

public class PreferentialShares
extends Shares {
    public char shareType = (char)65;

    public PreferentialShares(String string, long l, long l2, Company company, LinkedList<Shares> linkedList) {
        super(string, l, l2, company, linkedList);
    }
}

