/*
 * Decompiled with CFR 0_132.
 * 
 * Could not load the following classes:
 *  base.Base
 *  base.Base$Time
 *  com.google.gson.annotations.Expose
 *  javafx.collections.FXCollections
 *  javafx.collections.ObservableMap
 *  org.apache.commons.math3.stat.descriptive.SummaryStatistics
 *  statistics.Statistic
 *  statistics.Statistic$Type
 */
package core;

import base.Base;
import com.google.gson.annotations.Expose;
import core.Account;
import core.AccountType;
import core.AccountingException;
import core.Agent;
import core.BankLoan;
import core.BaselWeighting;
import core.CentralBank;
import core.Company;
import core.GeneralLedger;
import core.Govt;
import core.Icelandic;
import core.InterbankLoan;
import core.InvestmentType;
import core.Ledger;
import core.LedgerType;
import core.Loan;
import core.Person;
import core.PreferentialShares;
import core.Shares;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;
import statistics.Statistic;

public class Bank
extends Company {
    @Expose
    public long sharePrice = 10L;
    @Expose
    public double capitalPct = 0.2;
    @Expose
    public double capitalDividend = 0.0;
    @Expose
    public int capitalSteps = 12;
    @Expose
    public int interestRateDelta = 0;
    @Expose
    public int writeOffLimit = 6;
    @Expose
    public double lossProvisionPct = 0.01;
    public HashMap<Integer, Account> customerAccounts = new HashMap(100);
    public ObservableMap<Integer, Account> obsAccounts = FXCollections.observableMap(this.customerAccounts);
    private int noIndividualAccounts = 0;
    private int noCompanyAccounts = 0;
    private int unclassifiedAccounts = 0;
    protected boolean validStats = false;
    public boolean applyLossProvision = true;
    static final String LEDGERFILE = "src/resources/ledgers/ledgers.def";
    public long currentIncome = 0L;
    public LinkedList<Shares> prefShares = new LinkedList();
    private BaselWeighting riskw = new BaselWeighting();
    protected Hashtable<Integer, Account> internalAccounts;
    public GeneralLedger gl;
    public boolean zombie = false;
    private boolean capitalConstrained;
    private boolean reserveConstrained;
    private boolean lossProvisionConstrained;
    public int loansPerStep = 1;
    private SummaryStatistics avgLoanDuration = new SummaryStatistics();
    public Loan.Type loantype;
    public double ownLoanPct_B = 0.5;
    private long minimumLoan = 30000L;
    private int govtTreasuryPct = 0;
    private long minBondPurchase = 100000L;
    private int bondDuration = 120;
    Statistic s_defaultTotal;
    Statistic s_newLending;
    Statistic s_newIBLending;
    Statistic s_reserveCash;
    Statistic s_interestIncome;
    Statistic s_zombie;
    public String product = "money";
    private String classname = this.name + " " + this.getClass().getSimpleName();

    public Bank(String string, Govt govt, Bank bank) {
        super(string, 0L, govt, null);
        this.product = "money";
        this.loantype = Loan.Type.valueOf("COMPOUND");
        this.internalAccounts = new Hashtable(10);
        this.gl = new GeneralLedger(LEDGERFILE, this);
        this.setMyAccount(this.gl.ledger("interest_income").getAccount());
        this.capitalConstrained = false;
        this.reserveConstrained = false;
        this.lossProvisionConstrained = false;
        this.initStatistics();
    }

    public Bank(String string, Govt govt, Bank bank, HashMap<String, String> hashMap) {
        this(string, govt, bank);
    }

    public Bank() {
        this.loantype = Loan.Type.valueOf("COMPOUND");
        this.internalAccounts = new Hashtable(10);
        this.gl = new GeneralLedger(LEDGERFILE, this);
        this.setMyAccount(this.gl.ledger("interest_income").getAccount());
        this.capitalConstrained = false;
        this.reserveConstrained = false;
        this.lossProvisionConstrained = false;
    }

    @Override
    public void initStatistics() {
        if (this.name.equals("")) {
            System.out.println("initStatistics called for " + this.getClass() + " with unset name");
            return;
        }
        this.issueShares(10L, 2000000);
        if (!(this instanceof CentralBank)) {
            this.s_newLending = Statistic.getStatistic((String)(this.name + ":New Loans"), (String)"newbanklending", (Statistic.Type)Statistic.Type.COUNTER);
            this.s_reserveCash = Statistic.getStatistic((String)(this.name + ":Reserves"), (String)"reserves", (Statistic.Type)Statistic.Type.COUNTER);
            this.s_defaultTotal = Statistic.getStatistic((String)(this.name + ":Total Defaults"), (String)"totaldefaults", (Statistic.Type)Statistic.Type.COUNTER);
            this.s_newIBLending = Statistic.getStatistic((String)(this.name + ":IB Lending"), (String)"New IB Lending", (Statistic.Type)Statistic.Type.COUNTER);
            this.s_interestIncome = Statistic.getStatistic((String)(this.name + ":Interest Income"), (String)"interestincome", (Statistic.Type)Statistic.Type.SINGLE);
            this.s_zombie = Statistic.getStatistic((String)(this.name + ":Zombie"), (String)"zombie", (Statistic.Type)Statistic.Type.SINGLE);
        }
    }

    @Override
    public void evaluate(boolean bl, int n) {
        long l;
        this.payDebt();
        long l2 = this.gl.ledger("interest_income").total();
        for (Loan loan : this.gl.ledger((String)"ib_debt").getAccount().debts.values()) {
            if (this.gl.ledger("interest_income").getAccount().getDeposit() > loan.getPaymentDue()) {
                this.gl.ledger("reserve").getAccount().payLoan(loan);
                continue;
            }
            this.zombie = true;
            System.out.println("** " + this.getName() + " unable to pay IBL");
        }
        if (this.applyLossProvision) {
            long l3 = (long)((double)this.gl.ledger("loan").total() * this.lossProvisionPct);
            long l4 = l3 - this.gl.ledger("loss_provision").total();
            if (l4 > 0L) {
                if (l4 > this.gl.ledger("interest_income").total()) {
                    l = this.gl.ledger("interest_income").total();
                    this.lossProvisionConstrained = true;
                } else {
                    l = l4;
                    this.lossProvisionConstrained = false;
                }
                if (l > 0L) {
                    this.gl.transfer(this.gl.ledger("interest_income").getAccount(), this.gl.ledger("loss_provision").getAccount(), l, "Increase loss provisions");
                }
            } else if (l4 < 0L) {
                this.lossProvisionConstrained = false;
                this.gl.transfer(this.gl.ledger("loss_provision").getAccount(), this.gl.ledger("interest_income").getAccount(), Math.abs(l4), "Decrease loss provisions");
            }
        }
        if (this.getRequiredReserves() >= this.gl.ledger("reserve").total() && this.getRequiredReserves() > this.gl.ledger("reserve").total()) {
            InterbankLoan interbankLoan;
            l = this.getRequiredReserves() - this.gl.ledger("reserve").total();
            long l5 = Math.min(l, this.gl.ledger("cash").total());
            if (l5 > 0L) {
                this.moveCashToReserves(l5);
                l -= l5;
            }
            if (l > 0L && (interbankLoan = this.govt.centralbank.borrowReserves(this, l)) != null) {
                interbankLoan.borrower = this.gl.ledger("interest_income").getAccount();
                this.gl.post(this.gl.ledger("reserve"), this.gl.ledger("reserve").getAccount(), this.gl.ledger("ib_debt"), this.gl.ledger("ib_debt").getAccount(), interbankLoan, "credit", "Interbank loan from " + interbankLoan.owner.name);
            }
        }
        l = this.gl.ledger("interest_income").total();
        if (n % this.capitalSteps == 0) {
            this.payTax(this.govt.corporateTaxRate, this.govt.corporateCutoff);
            this.currentIncome = this.gl.ledger("interest_income").total() + this.gl.ledger("non-cash").total();
            if (this.getTotalSharesIssued(this.prefShares) > 0 && this.capitalConstrained()) {
                System.out.println("\n\n**** div : " + this.capitalDividend);
                this.payDividend(this.capitalDividend, this.prefShares);
            }
        }
        this.s_reserveCash.add(this.gl.ledger("reserve").total() + this.gl.ledger("cash").total());
        if (this.zombie) {
            this.s_interestIncome.add(-1L);
        } else {
            this.s_interestIncome.add(this.gl.ledger("interest_income").total());
        }
    }

    public void depositCash(long l, Ledger ledger, Account account, String string) {
        this.gl.post(this.gl.ledger("cash"), this.gl.ledger("cash").getAccount(), ledger, account, l, string);
    }

    public void withdrawCash(long l, Account account, String string) {
        this.gl.post(this.gl.ledger("deposit"), account, this.gl.ledger("cash"), this.gl.ledger("cash").getAccount(), l, string);
    }

    public long getMaxLoanAmount(Agent agent, int n) {
        if (this.zombie) {
            return 0L;
        }
        return (long)((double)this.getSpareCapital() / this.riskw.getRiskWeighting(n));
    }

    public long requestInterestRate(int n) {
        return this.govt.centralbank.getBaseRate() + this.interestRateDelta;
    }

    public void closeAccount(Account account, Account account2) {
        assert (account.owner == account2.owner);
        for (Loan loan : account.debts.values()) {
            loan.dbg_transfer = true;
            account2.addLoan(loan);
        }
        account.debts = new ConcurrentHashMap(5);
        for (Loan loan : account.capital_loans.values()) {
            loan.dbg_transfer = true;
            account2.addCapitalLoan(loan);
        }
        account.capital_loans = new ConcurrentHashMap(5);
        System.out.println("Transferred account @ " + this.name);
        System.out.println(account);
        System.out.println(account2);
        this.transfer(account, account2, account.getDeposit(), "Closed account " + account.getId());
        account2.owner.setMyAccount(account2);
        this.gl.ledger(account.ledger).removeAccount(account);
    }

    public Loan requestLoan(Account account, long l, int n, Base.Time time, int n2, Loan.Type type) {
        Base.DEBUG((String)(this.name + ": Request Loan to " + account + " for " + l + " @ " + (this.interestRateDelta + this.govt.centralbank.getBaseRate()) + " / " + n));
        if (l <= 0L) {
            return null;
        }
        if (this.zombie) {
            return null;
        }
        if (!this.reserveConstrained(l) && !this.capitalConstrained(l, n2)) {
            return this.makeLoan(account, n * time.period(), this.govt.centralbank.getBaseRate() + this.interestRateDelta, l, type);
        }
        Base.DEBUG((String)(this.name + "**** Loan for " + l + " refused: " + (this.reserveConstrained ? "reserve" : "capital") + " constrained *****"));
        return null;
    }

    public boolean hasExcessReserves(long l) {
        return this.gl.ledger("reserve").total() - this.getRequiredReserves() > l;
    }

    public InterbankLoan requestIBLoan(Bank bank, long l, double d, int n, int n2) {
        if (this.hasExcessReserves(l)) {
            InterbankLoan interbankLoan = new InterbankLoan(this, l, this.govt.centralbank.interBankRate, n, Base.step, bank.gl.ledger("reserve").getAccount(), "asset");
            this.gl.post(this.gl.ledger("loan"), this.gl.ledger("loan").getAccount(), this.gl.ledger("reserve"), this.gl.ledger("reserve").getAccount(), interbankLoan, "debit", "IB Loan");
            this.s_newIBLending.add(interbankLoan.getLoanAmount());
            System.out.println("** DBG: " + this.name + " interbank loan for " + l + " to " + bank.name);
            return interbankLoan;
        }
        return null;
    }

    public boolean reserveConstrained(long l) {
        this.reserveConstrained = this.govt.getCentralBank().reserveControls && this.getReserveMax() < l;
        return this.reserveConstrained;
    }

    private long getReserveMax() {
        return 100L * (this.gl.ledger("reserve").total() + this.gl.ledger("cash").total()) / (long)this.govt.getCentralBank().cb_reserve - this.gl.ledger("deposit").total();
    }

    public long getSpareCapital() {
        return (long)(100.0 * (double)this.gl.ledger("capital").total() / this.govt.getCentralBank().capitalPct - (double)this.riskWeightedLoansTotal());
    }

    public boolean reserveConstrained() {
        return this.reserveConstrained;
    }

    public boolean lossProvisionConstrained() {
        return this.lossProvisionConstrained;
    }

    public long getRequiredReserves() {
        return !this.govt.getCentralBank().reserveControls ? 0L : (long)((double)(this.gl.ledger("deposit").total() * (long)this.govt.getCentralBank().cb_reserve) / 100.0);
    }

    public long getCBReserves() {
        return this.gl.ledger("reserve").total();
    }

    public boolean adjustReserves(long l) {
        if (l <= this.gl.ledger("cash").total()) {
            this.moveCashToReserves(l);
            return true;
        }
        return false;
    }

    public void moveCashToReserves(long l) {
        this.gl.transfer(this.gl.ledger("cash").getAccount(), this.gl.ledger("reserve").getAccount(), l, "increase reserves from cash");
        this.govt.centralbank.depositCash(l, this.govt.centralbank.gl.ledger(this.name), this.govt.centralbank.gl.ledger(this.name).getAccount(), "Transfer Cash to Reserves");
    }

    public boolean capitalConstrained(long l, int n) {
        double d = 100.0 / this.govt.centralbank.capitalPct;
        this.capitalConstrained = true;
        if ((double)this.getSpareCapital() * d - (double)l * this.riskw.getRiskWeighting(n) > 0.0) {
            this.capitalConstrained = false;
        }
        if (!this.govt.getCentralBank().capitalControls) {
            this.capitalConstrained = false;
        }
        return this.capitalConstrained;
    }

    public boolean capitalConstrained() {
        return this.capitalConstrained;
    }

    public long getTotalCapital() {
        long l = 0L;
        for (Ledger ledger : this.gl.equities.values()) {
            l += ledger.total();
        }
        return l;
    }

    public long getCapitalLimit() {
        return (long)((double)this.getTotalCapital() * this.riskw.getBaselMultiplier());
    }

    public long getCapitalLimit(int n) {
        return (long)((double)this.getTotalCapital() * this.riskw.getBaselMultiplier() * this.riskw.getRiskWeighting(n));
    }

    public long riskWeightedLoansTotal() {
        long l = 0L;
        for (Ledger ledger : this.gl.assets.values()) {
            if (ledger.getType() != AccountType.ASSET || ledger.getLedgerType() != LedgerType.LOAN) continue;
            for (Loan loan : ledger.getAccount().capital_loans.values()) {
                l = (long)((double)l + (double)loan.getCapitalOutstanding() * BaselWeighting.riskWeighting(loan));
            }
        }
        return l;
    }

    Account createAccount(Agent agent) {
        Account account = new Account(agent, this, 0L);
        if (this.customerAccounts.containsKey(account.getId())) {
            throw new RuntimeException("Duplicate account key:" + account.getId());
        }
        this.customerAccounts.put(account.accountId, account);
        this.validStats = false;
        return account;
    }

    public boolean removeAccount(Account account, Agent agent) {
        if (!this.customerAccounts.containsKey(account.getId())) {
            System.out.println("Error: no such account in Bank " + account);
            return false;
        }
        if (account.debts.size() > 0 || account.capital_loans.size() > 0) {
            throw new RuntimeException("Debt unhandled in removeAccount");
        }
        this.transfer(account, agent.getAccount(), account.getDeposit(), "Removing account " + account.getId());
        this.gl.ledger(account.ledger).removeAccount(account);
        this.customerAccounts.remove(account.getId());
        return true;
    }

    public Account createAccount(Ledger ledger, boolean bl) throws AccountingException {
        Account account = new Account(this, ledger);
        this.validStats = false;
        if (bl) {
            ledger.addAccountAndClose(account);
        } else {
            ledger.addAccount(account);
        }
        if (this.gl != null) {
            this.gl.auditAccounts(account);
        }
        if (this.internalAccounts.containsKey(account.getId())) {
            throw new AccountingException("Duplicate internal account key");
        }
        this.internalAccounts.put(account.getId(), account);
        return account;
    }

    public Account getAccount(Integer n) {
        Account account = this.customerAccounts.get(n);
        if (account == null && (account = this.internalAccounts.get(n)) == null) {
            throw new RuntimeException("Unknown account in getAccount" + n);
        }
        return account;
    }

    public Account createAccount(Agent agent, long l) {
        Account account;
        try {
            account = this.createDepositAccount(agent);
        }
        catch (Exception exception) {
            throw new RuntimeException("createAccount failed" + exception);
        }
        if (l != 0L) {
            this.printMoney(account, l, "Create Account");
        }
        return account;
    }

    public void printMoney(Account account, long l, String string) {
        this.gl.post("cash", this.gl.ledger("cash").getAccount(), "deposit", account, l, string);
    }

    public Account createDepositAccount(Agent agent) {
        Account account = this.createAccount(agent);
        try {
            this.gl.liabilities.get("deposit").addAccount(account);
        }
        catch (Exception exception) {
            throw new RuntimeException("Error adding account " + exception);
        }
        return account;
    }

    public boolean transfer(Account account, Account account2, long l, String string) {
        account.outgoing += l;
        account2.incoming += l;
        if (account.bank == account2.bank) {
            if (account.getDeposit() >= l) {
                this.gl.transfer(account, account2, l, string);
                return true;
            }
            System.out.println("Insufficient funds in account: " + account);
            return false;
        }
        if (account.bank instanceof CentralBank) {
            return this.centralBankTransferFrom(account, account2, l, string);
        }
        if (account2.bank instanceof CentralBank) {
            return this.centralBankTransferTo(account, account2, l, string);
        }
        if (!account.bank.name.equals(account2.bank.name)) {
            return this.interBankTransfer(account, account2, l, string);
        }
        throw new RuntimeException("Unknown transfer");
    }

    public boolean payBankLoan(Account account, Loan loan, long l) {
        long[] arrl = new long[2];
        arrl[Loan.CAPITAL] = l;
        arrl[Loan.INTEREST] = 0L;
        return this.payBankLoan(account, loan, arrl);
    }

    public boolean payBankLoan(Account account, Loan loan) {
        long[] arrl = new long[]{0L, 0L};
        if (loan.inDefault) {
            long l = loan.getCapitalOutstanding();
            arrl[Loan.CAPITAL] = account.getDeposit() >= l ? l : this.getDeposit();
        } else {
            arrl = loan.getNextLoanRepayment();
        }
        return this.payBankLoan(account, loan, arrl);
    }

    private boolean payBankLoan(Account account, Loan loan, long[] arrl) {
        long l = arrl[0] + arrl[1];
        assert (loan.borrower == account);
        if (account.bank != this) {
            throw new RuntimeException("Loan payment from account not at this Bank");
        }
        if (account.getDeposit() < l || loan.inWriteOff || loan.inDefault) {
            System.out.println("\n Loan : " + loan.Id + " " + loan + " in default " + loan.defaultCount);
            System.out.println("\tIn Account: " + account.getDeposit() + " vs owed " + l);
            if (loan.incDefault(this.writeOffLimit)) {
                long l2;
                long l3;
                Base.DEBUG((String)("Writing off loan: " + loan));
                Base.DEBUG((String)(account.owner.name + ": " + account.getDeposit()));
                long l4 = loan.getCapitalOutstanding();
                long l5 = this.gl.ledger("loss_provision").total();
                if (l4 >= l5) {
                    l2 = l5;
                    l4 -= l5;
                } else {
                    l2 = l4;
                    l4 = 0L;
                }
                this.gl.postWriteOff("loan", loan, "loss_provision", this.gl.ledger("loss_provision").getAccount(), l2, "loss provision");
                this.s_defaultTotal.add(l2);
                if (l4 == 0L) {
                    return true;
                }
                long l6 = this.gl.ledger("interest_income").total();
                if (l4 >= l6) {
                    l3 = l6;
                    l4 -= l6;
                } else {
                    l3 = l4;
                    l4 = 0L;
                }
                this.gl.postWriteOff("loan", loan, "interest_income", this.gl.ledger("interest_income").getAccount(), l3, "interest income");
                this.s_defaultTotal.add(l3);
                if (l4 == 0L) {
                    return true;
                }
                System.out.println("Placing Bank " + this.name + " into Zombie status");
                this.zombie = true;
                return false;
            }
            return false;
        }
        if (loan.ownerAcct.owner == this) {
            if (loan.negAm()) {
                loan.ownerAcct.bank.gl.postNegAm("deposit", account, "loan", loan, arrl, "");
            } else {
                loan.ownerAcct.bank.gl.post("deposit", account, "loan", loan, arrl, "");
            }
        } else {
            Bank bank = loan.ownerAcct.bank;
            if (this instanceof CentralBank) {
                this.gl.post("deposit", account, bank.name, this.gl.ledger(bank.name).getAccount(), arrl[0] + arrl[1], "Bank loan payment");
                bank.gl.post("reserve", bank.gl.ledger("reserve").getAccount(), "loan", loan, arrl, "Bank loan payment");
            } else {
                assert (this.gl.ledger("reserve").getAccount().getDeposit() == this.govt.centralbank.gl.ledger(this.name).getAccount().getDeposit());
                if (this.gl.ledger("reserve").getAccount().getDeposit() < arrl[0] + arrl[1] && !this.adjustReserves(arrl[0] + arrl[1] - this.gl.ledger("reserve").getAccount().getDeposit())) {
                    throw new RuntimeException("Insufficient Reserves");
                }
                if (!(loan instanceof InterbankLoan)) {
                    this.gl.post(account.ledger, account, "reserve", this.gl.ledger("reserve").getAccount(), Base.sum((long[])arrl), " Bank loan payment");
                }
                this.govt.centralbank.transferReserves(this, bank, l);
                bank.gl.post("reserve", bank.gl.ledger("reserve").getAccount(), "loan", loan, arrl, "Received Bank loan payment");
            }
        }
        if (loan.repaid()) {
            loan.remove();
        }
        return true;
    }

    public boolean payInterbankLoan(Account account, Loan loan) {
        Bank bank = loan.ownerAcct.bank;
        Loan loan2 = bank.gl.ledger("loan").getAccount().getLoanById(loan.Id, loan.ownerAcct.getName());
        long[] arrl = loan2.getNextLoanRepayment();
        long l = Base.sum((long[])arrl);
        if (account.bank != this) {
            throw new RuntimeException("Account not at this Bank/Loan payment");
        }
        if (account.getDeposit() < l) {
            System.out.println("**** Insufficient funds to repay Interbank Loan - Ending Simulation **** ");
            System.exit(-1);
        }
        if (this instanceof CentralBank) {
            throw new RuntimeException("Implement lender of last resort");
        }
        this.gl.post("ib_debt", loan, "reserve", this.gl.ledger("reserve").getAccount(), arrl, "Payment on IB loan");
        this.govt.centralbank.transferReserves(this, bank, l);
        bank.gl.post("reserve", bank.gl.ledger("reserve").getAccount(), "loan", loan2, arrl, "Interest Payment on interbank loan");
        if (loan.repaid()) {
            loan.remove();
        }
        return true;
    }

    public boolean payDebt(Account account, Loan loan) {
        System.out.println("Making non-Bank loan repayments");
        long[] arrl = loan.getNextLoanRepayment();
        long l = Base.sum((long[])arrl);
        assert (loan.borrower == account);
        assert (!(loan instanceof BankLoan));
        if (account.bank != this) {
            throw new RuntimeException("Loan payment from account not at this Bank");
        }
        if (account.getDeposit() < l) {
            System.out.println("Loan : " + loan + " in default");
            loan.incDefault(this.writeOffLimit);
            return false;
        }
        Bank bank = loan.ownerAcct.bank;
        if (this instanceof CentralBank) {
            this.gl.post("deposit", account, bank.name, this.gl.ledger(bank.name).getAccount(), Base.sum((long[])arrl), "Transfer loan payment");
            bank.gl.post("reserve", bank.gl.ledger("reserve").getAccount(), loan.ownerAcct.ledger, loan.ownerAcct, Base.sum((long[])arrl), "Transfer loan payment");
        } else {
            this.transfer(account, loan.ownerAcct, Base.sum((long[])arrl), "Transfer loan payment");
        }
        loan.makePayment(arrl);
        return true;
    }

    public boolean centralBankTransferTo(Account account, Account account2, long l, String string) {
        Bank bank = account.bank;
        CentralBank centralBank = (CentralBank)account2.bank;
        if (bank != this) {
            throw new RuntimeException("Bank transfer error???");
        }
        if (l > this.gl.ledger("reserve").total()) {
            this.adjustReserves(l);
        }
        this.gl.post(account.ledger, account, "reserve", this.gl.ledger("reserve").getAccount(), l, string);
        centralBank.gl.post(bank.name, centralBank.gl.ledger(bank.name).getAccount(), account2.ledger, account2, l, string);
        return true;
    }

    public boolean centralBankTransferFrom(Account account, Account account2, long l, String string) {
        CentralBank centralBank = (CentralBank)account.bank;
        Bank bank = account2.bank;
        if (centralBank != this) {
            throw new RuntimeException("Bank transfer error???");
        }
        this.gl.post(account.ledger, account, bank.name, this.gl.ledger(bank.name).getAccount(), l, string);
        bank.gl.post("reserve", bank.gl.ledger("reserve").getAccount(), account2.ledger, account2, l, string);
        return true;
    }

    public boolean interBankTransfer(Account account, Account account2, long l, String string) {
        Bank bank = account.bank;
        Bank bank2 = account2.bank;
        if (account.bank == account2.bank) {
            throw new RuntimeException("Error:interbank transfer with same Bank");
        }
        if (this.gl.ledger("reserve").total() <= l && !this.adjustReserves(l - this.gl.ledger("reserve").total())) {
            System.out.println("Insufficient clearing balance for transfer: " + l + " < " + this.gl.ledger("reserve").total());
            return false;
        }
        this.gl.post(account.ledger, account, "reserve", this.gl.ledger("reserve").getAccount(), l, string);
        this.govt.centralbank.transferReserves(bank, bank2, l);
        bank2.gl.post("reserve", bank2.gl.ledger("reserve").getAccount(), "deposit", account2, l, string);
        this.gl.audit(false);
        bank2.gl.audit(false);
        return true;
    }

    public Loan makeLoan(Account account, int n, int n2, long l, Loan.Type type) {
        Loan loan = null;
        if (account.bank != this && l > this.gl.ledger("reserve").total() && !this.adjustReserves(l - this.gl.ledger("reserve").total())) {
            Base.DEBUG((String)(this.name + ": loan DENIED to external customer as exceeds reserves " + l));
            return null;
        }
        if (type == Loan.Type.COMPOUND || type == Loan.Type.SIMPLE || type == Loan.Type.VARIABLE) {
            loan = new BankLoan(this, l, n2, n, Base.step, account, type);
        } else if (type == Loan.Type.INDEXED) {
            loan = new Icelandic(this.govt, this, l, n2, n, Base.step, account);
        } else {
            throw new RuntimeException("Unknown loan type:" + (Object)((Object)type));
        }
        account.makeLoan(loan);
        if (account.bank == this) {
            this.gl.post(this.gl.ledger("loan"), this.gl.ledger("loan").getAccount(), this.gl.ledger("deposit"), account, loan, "debit", "Bank loan");
        } else {
            this.gl.post(this.gl.ledger("loan"), this.gl.ledger("loan").getAccount(), this.gl.ledger("reserve"), this.gl.ledger("reserve").getAccount(), loan, "debit", "Bank loan");
            this.govt.centralbank.transferReserves(this, account.bank, l);
            account.bank.gl.post("reserve", account.bank.gl.ledger("reserve").getAccount(), "deposit", account, l, "Bank loan");
        }
        if (!this.zombie) {
            this.s_newLending.add(loan.getLoanAmount());
        } else {
            this.s_newLending.add(-1L);
        }
        return loan;
    }

    public long maxLoanAmount(int n, Bank bank) {
        long l = 0L;
        long l2 = 0L;
        assert (this.govt.getCentralBank().reserveControls || this.govt.getCentralBank().capitalControls);
        if (this.govt.getCentralBank().reserveControls) {
            l = this.getReserveMax();
            assert (l > 0L);
        }
        if (this.govt.getCentralBank().capitalControls) {
            l2 = (long)((double)this.gl.ledger("capital").total() * this.riskw.getBaselMultiplier()) - this.gl.ledger("loan").riskWeightedTotalLoans();
            assert (l2 > 0L);
        }
        if (bank == this) {
            long l3 = 0L;
            l3 = this.govt.getCentralBank().reserveControls && this.govt.getCentralBank().capitalControls ? Math.min(l, l2) : (this.govt.getCentralBank().reserveControls ? l : l2);
            return Math.max(l3, this.minimumLoan);
        }
        long l4 = Math.min((long)(this.ownLoanPct_B * (double)this.gl.ledger("reserve").total()), l2);
        if (l4 < this.minimumLoan) {
            return 0L;
        }
        return l4;
    }

    public Object sellInvestment(Agent agent, long l, InvestmentType investmentType) {
        if (l > this.getShareholding(this.name)) {
            System.out.println(this.name + ": Insufficient shares: " + l + " > " + this.getShareholding(this.name));
            return null;
        }
        if (agent.getDeposit() < l * this.sharePrice) {
            System.out.println(this.name + " share purchase by " + agent.name + " failed, insufficient funds");
        }
        this.transfer(agent.getAccount(), this.gl.ledger("capital").getAccount(), l, "Capital Purchase");
        long l2 = this.transferShares(this.name, l, agent);
        return l2;
    }

    public void sellCapital(Agent agent, long l, long l2, String string) {
        PreferentialShares preferentialShares = new PreferentialShares(this.name, l, l2, this, this.prefShares);
        this.gl.post(this.gl.ledger("cash"), this.gl.ledger("cash").getAccount(), this.gl.ledger("capital"), this.gl.ledger("capital").getAccount(), l * l2, string);
        preferentialShares.transfer(agent);
    }

    public boolean recogniseIncome(long l) {
        if (this.gl.ledger("interest_income").total() < l) {
            System.out.println(this.name + " Error: Insufficient funds to recognise income of " + l);
            return false;
        }
        this.gl.post(this.gl.ledger("interest_income"), this.gl.ledger("interest_income").getAccount(), this.gl.ledger("retained_earnings"), this.gl.ledger("retained_earnings").getAccount(), l, "Recognised interest income");
        return true;
    }

    void recalculateVariableLoans() {
        for (Ledger ledger : this.gl.assets.values()) {
            ledger.recalculateVariableLoans(this.govt.centralbank.getBaseRate());
        }
        for (Ledger ledger : this.gl.liabilities.values()) {
            ledger.recalculateVariableLoans(this.govt.centralbank.getBaseRate());
        }
        for (Ledger ledger : this.gl.equities.values()) {
            ledger.recalculateVariableLoans(this.govt.centralbank.getBaseRate());
        }
    }

    public long getDeposit(Integer n) {
        if (this.customerAccounts.containsKey(n)) {
            return this.customerAccounts.get((Object)n).deposit;
        }
        if (this.internalAccounts.containsKey(n)) {
            return this.internalAccounts.get((Object)n).deposit;
        }
        System.out.println(this.name);
        this.report(this.customerAccounts);
        this.report(this.internalAccounts);
        throw new RuntimeException("Bank " + this.name + " unknown account Id " + n);
    }

    public void recalculateStats() {
        this.recalculateStats(this.gl.ledger((String)"deposits").accounts);
    }

    public double percentageOwnLoans() {
        double d = 0.0;
        double d2 = 0.0;
        for (Loan loan : this.gl.ledger((String)"loan").getAccount().capital_loans.values()) {
            if (loan.borrower.bank == this) {
                d2 += 1.0;
            }
            d += 1.0;
        }
        if (d == 0.0) {
            return 1.0;
        }
        return d2 / d;
    }

    public void setLossProvisions(boolean bl) {
        this.applyLossProvision = bl;
        System.out.println(this.name + ": setting loss provision to " + bl);
    }

    @Override
    public Account getAccount() {
        return this.gl.ledger("interest_income").getAccount();
    }

    public long getTotalLoans() {
        return this.gl.ledger("loan").total();
    }

    public int getTotalDeposits() {
        int n = 0;
        if (this.customerAccounts == null) {
            return 0;
        }
        for (Account account : this.customerAccounts.values()) {
            n = (int)((long)n + account.getDeposit());
        }
        return n;
    }

    public String toString() {
        return this.classname;
    }

    public void report(Hashtable<Integer, Account> hashtable) {
        System.out.println("Bank Account Report");
        hashtable.values().forEach(System.out::println);
    }

    void report(HashMap<Integer, Account> hashMap) {
        System.out.println("Bank Account Report");
        hashMap.values().forEach(System.out::println);
    }

    public long noIndividualAccounts() {
        if (!this.validStats) {
            this.recalculateStats();
        }
        return this.noIndividualAccounts;
    }

    public int noCompanyAccounts() {
        if (!this.validStats) {
            this.recalculateStats();
        }
        return this.noCompanyAccounts;
    }

    public long totalAccounts() {
        return this.customerAccounts.size();
    }

    void recalculateStats(HashMap<Integer, Account> hashMap) {
        this.noIndividualAccounts = 0;
        this.noCompanyAccounts = 0;
        this.unclassifiedAccounts = 0;
        for (Account account : hashMap.values()) {
            if (account.owner instanceof Person) {
                ++this.noIndividualAccounts;
                continue;
            }
            if (account.owner instanceof Company) {
                ++this.noCompanyAccounts;
                continue;
            }
            ++this.unclassifiedAccounts;
        }
        this.validStats = true;
    }

    public long getTotalActiveDeposits() {
        long l = 0L;
        for (Ledger ledger : this.gl.ledgers.values()) {
            l += ledger.getTurnover();
        }
        return l;
    }

    @Override
    public String getCurrentSetup() {
        return this.classname;
    }

    public void printTotals() {
        int n = 0;
        System.out.println("\n");
        for (Account account : this.customerAccounts.values()) {
            n = (int)((long)n + account.getDeposit());
            System.out.println("\t" + account.owner.name + ": " + account.getDeposit());
        }
        System.out.println("Money Supply = " + n + "\n\n");
    }
}

