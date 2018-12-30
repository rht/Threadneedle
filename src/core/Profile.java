/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  com.google.gson.annotations.Expose
 */
package core;

import com.google.gson.annotations.Expose;
import core.Need;
import core.Want;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentSkipListMap;

public class Profile {
    @Expose
    public ConcurrentSkipListMap<String, Need> needs = new ConcurrentSkipListMap();
    public ConcurrentSkipListMap<String, Want> wants = new ConcurrentSkipListMap();

    public Profile() {
    }

    public Profile(Profile profile) {
        for (Need need : profile.needs.values()) {
            this.addNeed(need.product, need.quantity, need.storeQ, need.stepFrequency, need.consumption, need.consumable, need.useLoan);
        }
    }

    public boolean equals(Object object) {
        if (object == null) {
            return false;
        }
        if (this.getClass() != object.getClass()) {
            return false;
        }
        Profile profile = (Profile)object;
        Object object2 = profile.needs.clone();
        for (Need need : this.needs.values()) {
            if (object2.get(need.product) != null) {
                if (((Need)object2.get(need.product)).equals(need)) {
                    object2.remove(need.product);
                    continue;
                }
                return false;
            }
            System.out.println("Comparison failed on " + need);
            return false;
        }
        return object2.size() == 0;
    }

    public Need addNeed(String string, int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
        Need need = null;
        if (this.getNeed(string) == null) {
            need = new Need(string, n, n2, n3, n4, bl, bl2);
            this.needs.put(need.product, need);
        } else {
            System.err.println("Error: Need already present " + string);
        }
        return this.getNeed(need.product);
    }

    public Need getNext(String string) {
        if (this.needs.higherEntry(string) == null) {
            return this.needs.get(this.needs.firstKey());
        }
        return this.needs.higherEntry(string).getValue();
    }

    public void addWant(String string, int n, int n2, int n3, int n4, boolean bl, boolean bl2) {
        if (this.getWant(string) == null) {
            this.wants.put(string, new Want(string, n, n2, n3, n4));
        } else {
            System.err.println("Error: Want already present " + string);
        }
    }

    public int needsSize() {
        return this.needs.size();
    }

    public int wantsSize() {
        return this.needs.size();
    }

    public void addTotals(HashMap<String, Double> hashMap) {
        for (Need need : this.needs.values()) {
            need.addAvgQuantity(hashMap);
        }
        for (Want want : this.wants.values()) {
            want.addAvgQuantity(hashMap);
        }
    }

    public Profile reset() {
        for (Need need : this.needs.values()) {
            need.reset();
        }
        for (Want want : this.wants.values()) {
            want.reset();
        }
        return this;
    }

    public Want getWant(String string) {
        return this.wants.get(string);
    }

    public Need getNeed(String string) {
        return this.needs.get(string);
    }

    public int getTotalDemand() {
        int n = 0;
        for (Need need : this.needs.values()) {
            n = (int)((long)n + need.getRequired());
        }
        for (Want want : this.wants.values()) {
            n = (int)((long)n + want.getRequired());
        }
        return n;
    }

    public String report() {
        String string = "";
        for (Need need : this.needs.values()) {
            string = string + need.toString() + "\n";
        }
        for (Want want : this.wants.values()) {
            string = string + want.toString() + "\n";
        }
        return string;
    }
}

