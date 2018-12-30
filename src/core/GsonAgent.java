/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.Expose
 */
package core;

import com.google.gson.annotations.Expose;

public class GsonAgent {
    @Expose
    public String clss;
    @Expose
    public String json;

    public GsonAgent(String string, String string2) {
        this.clss = string;
        this.json = string2;
    }
}

