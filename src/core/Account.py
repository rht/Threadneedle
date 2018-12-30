import base.Base
import core.Agent
import core.Bank
import core.BankLoan
import core.Icelandic
import core.InterbankLoan
import core.Ledger
import core.Loan
import core.Treasury
import java.io.PrintStream
import java.util.Collection
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import javafx.collections.FXCollections
import javafx.collections.ObservableMap

class Account:
    # long incoming  # TODO seems to be unused
    # long outgoing
    BASE_ACCOUNTNO: int = 1000000
    nextIdNo: int = 1000000

    def __init__(self, bank: Bank, ledger: Ledger = None, agent: Agent = None, l: int = 0):
        self.bank: Bank = bank
        if agent is not None:
            self.owner: Agent = agent
        self.deposit: int = l
        self.accountId: int = Account.getNewAccountId()
        self.ledger: str = "deposit" if ledger is None else ledger.name
        self.debts: Dict[int, Loan] = ConcurrentHashMap(5)
        self.capital_loans: Dict[int, Loan] = ConcurrentHashMap(5)
        self.obsLoans: Dict[int, Loan] = FXCollections.observableMap(self.capital_loans)

    def getName() -> str:
        return self.owner.getName()

    def getId() -> int:
        return self.accountId

    def getDeposit() -> int:
        return self.bank.getDeposit(self.accountId)

    def getTotalDebt() -> int:
        return sum(loan.getCapitalOutstanding() for loan in self.debts.values())

    def getTotalBankDebt() -> int:
        return sum(loan.getCapitalOutstanding() for loan in self.debts.values() if isinstance(loan.ownerAcct.owner, Bank))

    def getTotalCapital() -> int:
        return sum(loan.getCapitalOutstanding() for loan in self.capital_loans.values())

    def transfer(account: Account, l: int, string: str) -> bool:
        if self.getDeposit() < l:
            print("@ " + self.getName() + "Insufficient funds (" + l + "/" + self.getDeposit() + ") for transfer to " + account.getName())
            return False
        return self.bank.transfer(self, account, l, string)

    def requestLoan(l: int, n: int, time: Base.Time, n2: int, _type: Loan.Type) -> Loan:
        return self.bank.requestLoan(this, l, n, time, n2, _type)

    def makeLoan(Loan loan) -> None:
        if (self.debts.containsValue(loan)) {
            throw new RuntimeException("Loan already in debts container" + loan)
        }
        self.addLoan(loan, self.debts)

    def addCapitalLoan(loan: Loan) -> None:
        if self.debts.containsValue(loan):
            raise RuntimeException("Loan already in capitals container" + loan)
        self.addLoan(loan, self.capital_loans)

    def addLoan(loan: Loan) -> None:
        print("Account addloan : " + loan)
        self.addLoan(loan, self.debts)

    def addLoan(loan: Loan, hash_map: Dict[int, Loan]) -> None:
        hash_map.put(loan.Id, loan)
        loan.addAccount(self)

    def payLoan(loan: Loan) -> bool:
        if isinstance(loan, Treasury):
            return self.bank.payDebt(self, loan)
        if isinstance(loan, BankLoan) or isinstance(loan, Icelandic):
            return self.bank.payBankLoan(self, loan)
        if isinstance(loan, InterbankLoan):
            return self.bank.payInterbankLoan(this, loan)
        raise RuntimeException("Unknown loan type in payLoan")

    def holdsLoan(loan: Loan) -> bool:
        return (loan.Id in self.capital_loans) or (loan.Id in self.debts)

    def getLoanById(n: int, string: str) -> Loan:
        if not self.owner.name == string:
            raise RuntimeException("Request for loan on wrong account")
        if n in self.debts:
            return self.debts.get(n)
        if n in self.capital_loans:
            return self.capital_loans.get(n)
        return None

    def removeLoan(Loan loan) -> None:
        if loan.Id in self.capital_loans:
            self.capital_loans.remove(loan.Id)
        elif loan.Id in self.debts:
            self.debts.remove(loan.Id)
        else:
            raise RuntimeException("Remove on loan not controlled by account" + loan)

    def debtOutstanding() -> int:
        return sum(loan.getCapitalOutstanding() for loan in self.debts.values())

    def getNextRepayment() -> int:
        return sum(sum(loan.getNextLoanRepayment()[:2]) for loan in self.debts.values())

    def getNextInterestRepayment() -> int:
        return sum(loan.getNextInterestRepayment() for loan in self.debts.values())

    def getTotalInterestPaid() -> int:
        return sum(loan.interestPaid for loan in self.debts.values())

    def capitalOutstanding() -> int:
        return sum(loan.getCapitalOutstanding() for loan in self.capital_loans.values())

    def getRandomCapitalLoan() -> Loan:
        if len(self.capital_loans) > 0:
            return random.choice(self.capital_loans.values())
        return None

    def getNewAccountId() -> int:
        self.nextIdNo += 1
        return self.nextIdNo

    def audit() -> None:
        print("Audit: " + self.owner.name)
        if len(self.debts) > 0:
            print("Debts: ")
        for d in self.debts.values():
            print(d)
        if len(self.capital_loans) > 0:
            print("Owed: ")
        for loan in self.capital_loans.values():
            print(loan)
        print()

    def toString() -> str:
        return "Account: " + self.accountId + "(" + self.owner.name + ") @ " + self.bank.name + "  Deposit: " + self.deposit + " Ledger / " + self.ledger

