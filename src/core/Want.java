/*
 * Decompiled with CFR 0_132.
 */
package core;

import core.Need;

class Want
extends Need {
    int priority;

    public Want(String string, int n, int n2, int n3, int n4) {
        super(string, n, n2, n3, n4, true, false);
    }

    @Override
    public boolean equals(Object object) {
        throw new RuntimeException("Want comparison not implemented");
    }
}

