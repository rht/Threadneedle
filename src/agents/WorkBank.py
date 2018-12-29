# Program  : Threadneedle
#
# WorkBank : WorkBank is a Bank agent that hires workers.
#
# Author   : (c) Jacky Mallett
# Date     : October  2015
# Comments :
#
# Licencesing: Commercial Commons 4.0 (see LICENSE)
#
#
# Threadneedle is provided free for non-commercial research purposes under
# the creative commons Attribution-NonCommercial-NoDerivatives 4.0
# International License:
#
# https://creativecommons.org/licenses/by-nc-nd/4.0/
#/


# Simple bank that hires workers if it has sufficient interest
# income, and fires them if it doesn't.

class WorkBank(Bank):
    incomePctage: float = 0.20

    # Provide properties map (unused) for interface with fxml, main screen.
    #
    # @param name  Name of agent
    # @param g     Government
    # @param b     Bank (unused)
    # @param properties Properties string passed in by fxml (unused)
    def __init__(self, name: str = None, g: Govt = None, b: Bank = None, properties: Dict[str, str] = None):
        super().__init__(name, g, b)

    # Evaluation function for Bank.
    #
    # @param report t/f Print detailed report.
    # @param step       evaluation step

    def evaluate(report: bool, step: int) -> None:
       super().evaluate(report, step)

       # Hire people if there are sufficient funds.

       labourcost: int = markets.getMarket("Labour").getAskPrice()

       if len(employees) * gl.ledger("interest_income").total() > labourcost * 12 * len(employees):
          hireEmployee()
       elif len(employees) * gl.ledger("interest_income").total() > labourcost * 2 * len(employees):
          fireEmployee()

       paySalaries()
