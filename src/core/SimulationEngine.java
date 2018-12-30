/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  com.google.gson.Gson
 *  com.google.gson.GsonBuilder
 *  com.google.gson.JsonArray
 *  com.google.gson.JsonElement
 *  com.google.gson.JsonObject
 *  com.google.gson.JsonPrimitive
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import base.Base;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import core.Account;
import core.Agent;
import core.Bank;
import core.Banks;
import core.BaselGovt;
import core.Borrower;
import core.Company;
import core.Govt;
import core.LabourMarket;
import core.Market;
import core.Markets;
import core.Person;
import core.Region;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Observable;
import java.util.Random;
import java.util.WeakHashMap;
import statistics.Statistic;

public class SimulationEngine
extends Observable {
    public Govt govt = null;
    public String defaultBankName = null;
    long defaultdeposit = 0L;
    public ArrayList<Person> employees = new ArrayList(20);
    public ArrayList<Company> companies = new ArrayList(10);
    public String description = "";
    public HashMap<String, Agent> objectList = new HashMap();
    Statistic s_totalWorkerDeposits = Statistic.getStatistic((String)"totalWorkerDeposits", (String)"distribution", (Statistic.Type)Statistic.Type.COUNTER);
    Statistic s_totalMarketDeposits = Statistic.getStatistic((String)"totalMarketDeposits", (String)"distribution", (Statistic.Type)Statistic.Type.COUNTER);
    Statistic s_totalCompanyDeposits = Statistic.getStatistic((String)"totalCompanyDeposits", (String)"distribution", (Statistic.Type)Statistic.Type.COUNTER);
    Statistic s_totalValueGoodsSold = Statistic.getStatistic((String)"mvpt-PT", (String)"mvpt", (Statistic.Type)Statistic.Type.COUNTER, (int)12);
    Statistic s_calculatedVelocity = Statistic.getStatistic((String)"mvpt-V", (String)"mvpt", (Statistic.Type)Statistic.Type.COUNTER, (int)12);
    WeakHashMap<Agent, String> weakHashMap = new WeakHashMap();

    public void resetAll() {
        this.description = "";
        this.govt = new BaselGovt(this.govt.name, "Central Bank", 0L);
        this.govt.hasCentralBank = true;
        this.govt.setBanks(this.employees);
        this.employees.clear();
        this.companies.clear();
        this.govt.markets.removeAll();
        this.objectList.clear();
        Base.resetAll();
        Statistic.resetAll();
        this.setChanged();
        this.notifyObservers();
        this.s_totalWorkerDeposits = Statistic.getStatistic((String)"totalWorkerDeposits", null, (Statistic.Type)Statistic.Type.COUNTER);
        this.s_totalMarketDeposits = Statistic.getStatistic((String)"totalMarketDeposits", null, (Statistic.Type)Statistic.Type.COUNTER);
        this.s_totalCompanyDeposits = Statistic.getStatistic((String)"totalCompanyDeposits", null, (Statistic.Type)Statistic.Type.COUNTER);
        this.s_totalValueGoodsSold = Statistic.getStatistic((String)"mvpt-PT", (String)"mvpt", (Statistic.Type)Statistic.Type.COUNTER, (int)12);
        this.s_calculatedVelocity = Statistic.getStatistic((String)"mvpt-V", (String)"mvpt", (Statistic.Type)Statistic.Type.COUNTER, (int)12);
    }

    public void evaluate() {
        int n;
        boolean bl = false;
        int n2 = 0;
        boolean bl2 = false;
        int n3 = 0;
        int n4 = 0;
        int n5 = 0;
        Base.DEBUG((String)"====================================================");
        Collections.shuffle(this.companies, Base.random);
        Collections.shuffle(this.employees, Base.random);
        this.govt.evaluate(Base.step, false);
        this.govt.markets.evaluate(Base.step, false);
        for (n = 0; n < this.companies.size(); ++n) {
            this.companies.get(n).evaluate(Base.step, false);
        }
        for (n = 0; n < this.employees.size(); ++n) {
            this.employees.get(n).evaluate(Base.step, true);
            this.s_totalWorkerDeposits.add(this.employees.get(n).getDeposit());
            n4 = (int)((long)n4 + this.employees.get((int)n).s_income.get());
            n2 += this.employees.get(n).getDemand();
            if (!this.employees.get(n).unemployed()) {
                ++n3;
            }
            if (this.employees.get(n).getAccount().getTotalBankDebt() > 0L) {
                ++n5;
            }
            this.employees.get(n).resetRoundStatistics();
        }
        bl2 = false;
        for (Company object : this.companies) {
            this.s_totalCompanyDeposits.add(object.getDeposit());
        }
        for (Bank bank : this.govt.getBankList().values()) {
            bank.evaluate(Base.step, false);
        }
        Iterator<Company> iterator = this.govt.markets.getIterator();
        while (iterator.hasNext()) {
            Market market = (Market)iterator.next();
            this.s_totalMarketDeposits.add(market.getDeposit());
            this.s_totalValueGoodsSold.add(market.resetTotalSaleValue());
        }
        this.s_totalValueGoodsSold.add((long)n4);
        if (this.govt.s_totalActiveMoneySupply.getCurrent() != 0L) {
            this.s_calculatedVelocity.add(this.s_totalValueGoodsSold.getCurrent() / this.govt.s_totalActiveMoneySupply.getCurrent());
        }
        ++Base.step;
        Statistic.rolloverAll();
        if (this.weakHashMap.size() > 0) {
            System.out.println("DBG: Check container removal failed for: ");
            for (String string : this.weakHashMap.values()) {
                System.out.println("\t" + string);
            }
        }
    }

    public boolean loadSimulation(File file) {
        ArrayList arrayList = new ArrayList(20);
        try {
            Gson gson = new Gson();
            BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
            JsonObject jsonObject = (JsonObject)gson.fromJson((Reader)bufferedReader, JsonObject.class);
            this.description = jsonObject.getAsJsonPrimitive("description").getAsString();
            JsonArray jsonArray = jsonObject.getAsJsonArray("GsonAgent");
            for (int i = 0; i < jsonArray.size(); ++i) {
                Class<?> class_;
                Object object = jsonArray.get(i).getAsJsonObject();
                try {
                    class_ = Class.forName("core." + object.get("clss").getAsString());
                }
                catch (Exception exception) {
                    try {
                        class_ = Class.forName("agents." + object.get("clss").getAsString());
                    }
                    catch (Exception exception2) {
                        System.out.println("Error: Failed to find agent class " + object.get("clss").getAsString());
                        return false;
                    }
                }
                Object obj = SimulationEngine.getObject(object.get("json").getAsString(), class_);
                if (Govt.class.isAssignableFrom(obj.getClass())) {
                    this.govt = (Govt)obj;
                    this.initGovt(this.govt);
                    this.setChanged();
                    this.notifyObservers();
                    continue;
                }
                arrayList.add(obj);
            }
            System.out.println("Loading config file: " + file);
            for (Object object : arrayList) {
                this.validateModel(object);
            }
        }
        catch (FileNotFoundException fileNotFoundException) {
            return false;
        }
        catch (Exception exception) {
            exception.printStackTrace();
            System.out.println("Unable to read configuration file: " + file);
            return false;
        }
        return true;
    }

    public void initGovt(Govt govt) {
        this.govt = govt;
        this.govt.setBanks(this.employees);
        this.govt.initStatistics();
        if (govt.getClass().equals(Govt.class)) {
            this.govt.markets.defaultbank = this.govt.getBank();
        }
        this.addToContainers(this.govt);
    }

    public /* varargs */ void addToContainers(Agent ... arragent) {
        for (Agent agent : arragent) {
            this.objectList.put(agent.name, agent);
            if (agent instanceof Region) {
                this.govt.regions.put(agent.name, (Region)agent);
                continue;
            }
            if (agent instanceof Market) {
                if (this.govt.markets.getMarket(((Market)agent).product) == null) {
                    this.govt.markets.addMarket((Market)agent);
                    continue;
                }
                System.out.println("Warning: duplicate market detected " + ((Market)agent).product);
                continue;
            }
            if (agent instanceof Company && !(agent instanceof Bank)) {
                this.checkContainerAdd(this.companies, agent);
                continue;
            }
            if (agent instanceof Person) {
                this.checkContainerAdd(this.employees, agent);
                continue;
            }
            if (!(agent instanceof Bank) || this.govt.banks.getBank(agent.name) != null) continue;
            this.govt.banks.addBank((Bank)agent);
        }
    }

    private void checkContainerAdd(ArrayList arrayList, Agent agent) {
        if (arrayList.contains(agent)) {
            throw new RuntimeException("Duplicate add in Simeng");
        }
        arrayList.add(agent);
    }

    public /* varargs */ void removeFromContainers(Agent ... arragent) {
        for (Agent agent : arragent) {
            this.weakHashMap.put(agent, agent.name);
            if (!(agent instanceof Person)) continue;
            ((Person)agent).setSelfEmployed();
            this.employees.remove(agent);
        }
    }

    public void createGovt(String string, String string2, String string3) {
        Object object;
        try {
            object = Class.forName("core." + string2);
            Constructor<?> constructor = object.getDeclaredConstructor(String.class, String.class, Long.TYPE);
            if (string3.equals("Basel Capital")) {
                this.govt = (Govt)constructor.newInstance(string, "Central Bank", 0);
                this.govt.hasCentralBank = true;
                this.setChanged();
                this.notifyObservers();
                this.addToContainers(this.govt);
                this.govt.setBanks(this.employees);
            } else {
                System.out.println("Unrecognised banking system " + string3);
                System.exit(0);
            }
        }
        catch (Exception exception) {
            System.out.println("Unable to instantiate Govt : " + string2);
            exception.printStackTrace();
        }
        object = new LabourMarket("Labour", "Labour", this.govt, this.govt.getBank());
        this.addToContainers(new Agent[]{object});
    }

    public String createMarket(String string, String string2, Govt govt, Bank bank, long l, Region region) {
        String string3 = govt.markets.createMarket(string, string2, bank, l, region);
        return string;
    }

    public Object addEntity(Class class_, Map map, Bank bank, String string) {
        Object object;
        Object var5_5 = null;
        String string2 = bank == null ? this.defaultBankName : bank.name;
        try {
            if (this.objectList.get(string) != null) {
                System.out.println(string + ": Already defined, using default ID");
            }
            String string3 = string == null || this.objectList.get(string) != null ? class_.getSimpleName() + "-" + Base.getNextID() : string;
            if (map.size() == 0) {
                object = class_.getConstructor(String.class, Govt.class, Bank.class, HashMap.class);
            } else {
                object = class_.getConstructor(String.class, Govt.class, Bank.class, HashMap.class);
                HashMap hashMap = new HashMap(map);
                var5_5 = object.newInstance(string3, this.govt, this.objectList.get(string2), hashMap);
            }
        }
        catch (Exception exception) {
            System.out.println("Unable to instantiate agent: " + class_);
            if (map.size() == 0) {
                System.out.println("\t - this may be because no properties were provided for agent");
            }
            return "Error: Unable to instantiate agent";
        }
        object = this.validateModel(var5_5);
        if (object != null) {
            return object;
        }
        return var5_5;
    }

    /*
     * Enabled force condition propagation
     * Lifted jumps to return sites
     */
    public String validateModel(Object object) {
        Object object2;
        if (object instanceof BaselGovt) {
            if (this.govt != null) return "Government already defined";
            this.govt = (Govt)object;
        } else {
            this.addToContainers((Agent)object);
        }
        if (this.govt == null) {
            return "Government has not been defined";
        }
        if (object == null) return "Object is null";
        if (((Agent)object).govt == null) {
            ((Agent)object).init(this.govt);
        }
        if (object instanceof Market && ((Market)object).getProduct() == null) {
            ((Market)object).setProduct();
        }
        if (object instanceof Bank) {
            if (this.defaultBankName == null) {
                this.defaultBankName = ((Bank)object).getName();
            }
            if (this.govt.getBank(((Bank)object).name) == null) {
                this.govt.addBank((Bank)object);
            }
        }
        if (object instanceof Company || object instanceof Bank) {
            object2 = (Company)object;
            object2.setMarkets(this.govt.markets);
            object2.setMarket(this.govt.markets.getMarket(object2.product));
            if (object2.bankname == null) {
                object2.setBank(object2.bankname);
            }
            object2.initStatistics();
        }
        try {
            if (((Agent)object).govt == null) {
                object2 = object.getClass().getMethod("init", Govt.class);
                object2.invoke(object, this.govt);
            }
        }
        catch (Exception exception) {
            System.out.println(exception);
        }
        if (!(object instanceof Borrower) || ((Borrower)object).lender != null) return null;
        object2 = (Company)this.objectList.get(((Borrower)object).lendername);
        if (object2 == null) {
            System.out.println("No lender matching borrower " + ((Borrower)object).name + " : " + ((Borrower)object).lendername);
        } else {
            ((Borrower)object).lender = object2;
        }
        if (!((Borrower)object).bankEmployee) return null;
        ((Borrower)object).getBank().hireEmployee((Borrower)object, -1L);
        return null;
    }

    public String getTitle() {
        if (this.govt == null) {
            return "Threadneedle";
        }
        return this.govt.country + " Year " + Base.step / 360 + " Month " + Base.step % 360 / 30 + " Day " + Base.step % 360 % 30;
    }

    public void printCurrentConfig() {
        for (Bank agent : this.govt.banks.getBankList().values()) {
            System.out.println(agent.getCurrentSetup());
        }
        for (Market market : this.govt.markets.markets) {
            System.out.println(market.getCurrentSetup());
        }
        for (Company company : this.companies) {
            System.out.println(company.getCurrentSetup());
        }
        for (Person person : this.employees) {
            System.out.println(person.getCurrentSetup());
        }
    }

    public String getCurrentSetup() {
        String string = "";
        for (Market agent : this.govt.markets.markets) {
            string = string + agent.getCurrentSetup() + "\n";
        }
        for (Company company : this.companies) {
            string = string + company.getCurrentSetup() + "\n";
        }
        for (Person person : this.employees) {
            string = string + person.getCurrentSetup() + "\n";
        }
        return string;
    }

    public static <T> T getObject(String string, Class<T> class_) {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return (T)gson.fromJson(string, class_);
    }

    public Agent getAgent(String string) {
        Agent agent = this.objectList.get(string);
        if (agent instanceof Agent) {
            return agent;
        }
        return null;
    }

    public void auditWorkers() {
        System.out.println("Worker Audit");
        for (Person person : this.employees) {
            System.out.println(person.name + ": " + person.employer.name);
        }
    }
}

