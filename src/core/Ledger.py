import core.Account
import core.AccountType
import core.AccountingException
import core.Agent
import core.Bank
import core.BaselWeighting
import core.Loan
import core.Transaction


class LedgerType:
    LOAN = 1
    CAPITAL = 2
    DEPOSIT = 3
    CASH = 4


class Ledger:
    def __init__(self, string: str, string2: str, accountType: AccountType, ledgerType: LedgerType) -> None:
        self.name: str = string2
        self.type: AccountType = accountType
        self.ledgertype: LedgerType = ledgerType
        self.transactions: List[Transaction] = []
        self.accounts: Dict[int, Account] = {}

        self.debug = False
        self.postTransactions = True
        self.long_balance = 0
        self.frozen = False
        self.recordTransactions = False
        self.accountId = -1
        self.turnover = 0
        self.lastTotalDeposits = 0
        self.changed = True

    def getAccountNo() -> int:
        return self.accountId if self.frozen else -1

    def debit(transaction: Transaction, account: Account = None, loan: Loan = None, l: int = None) -> None:
        if loan is not None:
            if self.debug:
                print("debit ledger: " + self.name + " " + loan)
                if account not in self.accounts:
                    self.audit()
                    raise RuntimeException("Attempt to debit account not in ledger" + account)

            if (self.ledgertype == LedgerType.LOAN and self.type == AccountType.ASSET):
                account.addCapitalLoan(loan)
            else:
                account.addLoan(loan)

            self.addTransaction(transaction)
            self.turnover += loan.getCapitalOutstanding()
        elif l is not None and account is not None:
            if self.debug:
                print("debit ledger: " + self.name + " " + l)
                if account not in self.accounts:
                    self.audit()
                    raise RuntimeException("Attempt to debit account not in ledger " + self.name + " " + account)
            l2: int = l * -1 * self.type.polarity()
            if account.deposit + l2 < 0:
                raise RuntimeException("Negative Balance in Account after debit " + account)
            account.deposit += l2
            self.addTransaction(transaction)
            if self.type.polarity() < 0:
                self.turnover += l
        elif l is not None:
            if (not self.frozen) or len(self.accounts) != 1:
                raise RuntimeException("Cannot use with multi-account ledger")
            self.debit(transaction, self.getAccount(), l)
            if self.type.polarity() < 0:
                self.turnover += l
        else:
            raise Exception("not implemented")

    def credit(transaction: Transaction, account: Account = None, loan: Loan = None, l: int = None) -> None:
        if loan is not None and account is not None:
            if self.debug:
                print("credit ledger: " + self.name + " " + loan)
                if account not in self.accounts:
                    raise RuntimeException("Attempt to debit account not in ledger" + account)

            if self.ledgertype == LedgerType.LOAN and self.type == AccountType.ASSET:
                account.addCapitalLoan(loan)
            else:
                account.addLoan(loan)

            self.addTransaction(transaction)
            self.turnover += loan.getCapitalOutstanding()
        elif loan is not None and l is not None:
            if loan.writeOff(l):
                loan.remove()
            self.addTransaction(transaction)
        elif l is not None and account is not None:
            if self.debug:
                self.audit()
                print("credit ledger = " + self.name + " : " + l + " from " + account.owner.name)
                if account not in self.accounts:
                    self.audit()
                    raise RuntimeException("Attempt to credit account not in ledger " + self.name + ": " + account)
            l2: int = l * self.type.polarity()
            if (account.deposit + l2 < 0):
                raise RuntimeException("Negative Balance in Account after credit " + account)
            account.deposit += l2
            self.addTransaction(transaction)
            if self.type.polarity() > 0:
                self.turnover += l
        elif l is not None:
            if (not self.frozen) or self.accounts.size() != 1:
                raise RuntimeException("Cannot use with multi-account ledger")
            self.credit(self.getAccount(), l, transaction)
            if self.type.polarity() > 0:
                self.turnover += l
        else:
            raise Exception("not implemented")

    def payLoan(loan: Loan, arrl: List[int], transaction: Transaction) -> None:
        loan.makePayment(arrl)
        self.addTransaction(transaction)

    def addAccount(account: Account) -> None:
        if account.deposit != 0:
            raise AccountingException("Account deposit != 0 " + account.getId())
        if account.getId() in self.accounts:
            raise AccountingException("Ledger already contains account" + account.getId())
        if self.frozen:
            raise AccountingException("Attempt to add account to frozen ledger" + account.getId())
        if account.getID() in self.accounts:
            raise AccountingException("Duplicate account key in ledger: " + self.name)
        self.accounts.put(account.getId(), account)
        account.ledger = self.name

    def addAccountAndClose(account: Account) -> None:
        if account.deposit != 0:
            raise AccountingException("Account deposit != 0 " + account.getId())
        self.addAccount(account)
        self.frozen = True
        if len(self.accounts) == 1:
            self.accountId = account.accountId

    def removeAccount(account: Account) -> None:
        try:
            self.accounts.remove(account.accountId)
        except Exception as e:
            raise RuntimeException(e)

    def getAccount() -> Account:
        if len(self.accounts) != 1:
            raise RuntimeException("Requested single account from multi-account ledger:" + self.name)
        return self.accounts.values()[0]

    def getAccount(n: int) -> Account:
        return self.accounts.get(n)

    def debitPolarity() -> int:
        return self.type.polarity() * -1

    def creditPolarity() -> int:
        return self.type.polarity()

    def getType() -> AccountType:
        return self.type

    def getLedgerType() -> LedgerType:
        return self.ledgertype

    def containsLoan(loan: Loan) -> bool:
        return self.getAccount().holdsLoan(loan)

    def audit() -> None:
        print("Audit : " + self.name)
        for account in self.accounts.values():
            System.out.println("\t" + account.accountId + " " + account.bank.name)

    def total() -> int:
        if self.ledgertype == LOAN:
            if self.type == AccountType.ASSET:
                return self.totalCapital()
            return self.totalLoans()
        elif self.ledgertype == CAPITAL:
            return self.totalCapital()
        elif self.ledgertype == DEPOSIT:
            if self.changed:
                self.lastTotalDeposits = self.totalDeposits()
                self.changed = false
            return self.lastTotalDeposits
        raise RuntimeException("Unhandled entry type in ledger (new enum??)")

    def totalDeposits() -> int:
        if self.accounts is not None:
            return sum(account.getDeposit() for account in self.accounts.values())
        return 0

    def totalCapital() -> int:
        if self.accounts is not None:
            if self.type == AccountType.EQUITY:
                assert len(self.accounts) == 1
                account = self.accounts.values()[0]
                return account.getTotalCapital() + account.getDeposit()
            else:
                return sum(account.getTotalCapital() for account in self.accounts.values())
        return 0

    def recalculateVariableLoans(d: float) -> None:
        if self.accounts is None:
            return
        for account in self.accounts.values():
            for loan in account.debts.values():
                loan.interestRate = d
                loan.setCompoundSchedules(loan.payIndex)

    def totalLoans() -> int:
        if self.accounts is not None:
            return sum(account.getTotalDebt() for account in self.accounts.values())
        return 0

    def riskWeightedTotalLoans() -> int:
        out: int = 0
        assert self.name != "loan"
        if self.accounts is not None:
            for (Account account : self.accounts.values()) {
                l += sum(loan.getCapitalOutstanding() * BaselWeighting.riskWeighting(loan) for loan in account.capital_loans.values())
        return l

    def addTransaction(transaction: Transaction) -> None:
        self.changed = True
        if self.postTransactions:
            self.transactions.add(transaction)

    def getTurnover() -> int:
        l: int = self.turnover
        self.turnover = 0
        return l

    def getName() -> str:
        return self.name

    def toString() -> str:
        return self.name

    def printAccounts() -> None:
        print("Ledger: " + self.name)
        for account in self.accounts.values():
            print("Deposits")
            print("\t" + account)
            if len(account.debts) > 0:
                print("\nLoans")
                for loan in account.debts.values():
                    print("\t\tDebt:\t" + loan)
            if len(account.capital_loans) <= 0:
                continue
            print("\nCapital")
            for loan in account.capital_loans.values():
                print("\t\tCapital:\t" + loan)
        print()
