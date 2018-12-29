# 
# Program     : Threadneedle
#
# BankInvestor: An Agent that buys preferential shares in a bank.
#
# Author   : Jacky Mallett
# Date     : October 2012
#
# Behaviour: Bank Investor's have 0 salary and receive all income from
#            dividends/interest. Iff the current deposit is greater than
#            the minimum specified investment, they will attempt to buy
#            more bank shares from the specified bank they are an investor
#            in. This does not imply that the bank has to sell them any.
#
#
# Threadneedle is provided free for non-commercial research purposes under 
# the creative commons Attribution-NonCommercial-NoDerivatives 4.0 
# International License:
#
# https:#creativecommons.org/licenses/by-nc-nd/4.0/
#/

class BankInvestor(Person):
    investmentCompany: str = ''  # Bank to buy capital of
    initialCapital: int = 0

    investment: Bank = None            # Target for investor's purchases
    minInvestAmount: int = 50

    def evaluate(report: bool, step: int) -> None:
        shares: Shares = None  # investment purchased from company
        purchased: int = 0
        bought: bool = False

        super().evaluate(report, step)

        # Attempt to buy bank shares

        if getDeposit() > minInvestAmount:
            investment.sellInvestment(
                self, getDeposit() / investment.sharePrice,
                InvestmentType.ORDINARY)

    #
    # Constructor within model.
    #
    # @param name    Unique and identifying name
    # @param g       Government
    # @param b       Bank for BankInvestor account/capital purchase
    # @param deposit Initial deposit
    #

    def __init__(name: str, g: Govt, b: Bank, deposit: int) -> None:
        super().__init__(name, g, b, deposit)
        self.employer = self

    def init(g: Govt) -> None:
        super().init(g)
        if investmentCompany is not None:
            setInvestment(govt.getBank(investmentCompany), initialCapital)

    # todo: fix investors getting accidentally hired
    def unemployed(self) -> bool:
        return False

    # Allow BankInvestor's to have a salary of 0
    #
    # @param newSalary New value for salary
    def setSalary(newSalary: int) -> None:
        self.salary = newSalary

    # @param bank    Bank  investor will invest in.
    # @param capital Initial capital to invest
    def setInvestment(bank: Bank, capital: int) -> None:
        investment = bank
        investmentCompany = bank.name
        initialCapital = capital

        long noShares = initialCapital / bank.sharePrice
        bank.sellCapital(self, noShares, bank.sharePrice, "BankInvestor Capital")

    # Todo: Sell investment (allow investors to trade)
    #
    # @param to     Agent to sell investment to
    # @param amount Amount to sell for
    # @param period Period of investment to sell (loans)
    # @param type   Type of loan
    # @return Investment being sold
    def sellInvestment(to: Agent, amount: int, period: int, _type: str) -> object:
        raise RuntimeException("Not implemented for this institution" + self.name)
