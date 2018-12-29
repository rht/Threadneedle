# Program : Threadneedle
#
# Company : Implements a farm. A farm is a specialisation of company that
#           produces food.
#
# Author  : Jacky Mallett
# Date    : April 2012
#
# Comments:
#   Configuration - inventory name is assumed to stay constant
#
#
#
# Threadneedle is provided free for non-commercial research purposes under 
# the creative commons Attribution-NonCommercial-NoDerivatives 4.0 
# International License:
#
# https:#creativecommons.org/licenses/by-nc-nd/4.0/
#/

class Farm(Company):
    Statistic s_mkt_price

    overUnder: int = 0

    # Local factors for decision making
    savingsRate: float = 0.1  # 10% savings rate

    #*
    # Constructor:
    #
    # @param name       Name for individual farm
    # @param govt       Government this farm belongs to
    # @param bank       Bank farm has deposit at
    # @param properties Property map (to interface with fxml) product Product
    #                   farm is producing initialDeposit initial deposit with
    #                   bank labourInput amount of labour(employees) to produce
    #                   a unit of product
    #/
    def __init__(name: str, govt: Govt, bank: Bank,
                 properties: Dict[str, str]) -> None:
        super().__init__(name, int(properties.get("initialDeposit")), govt, bank)

        self.labourInput = Integer.parseInt(properties.get("labourInput"))
        self.product = properties.get("product")

        self.initStatistics()

    def Farm(self) -> None:
        pass

    def initStatistics(self) -> None:
        super().initStatistics()

        s_mkt_price = Statistic(Id + ": mkt_price", AVERAGE)
        s_quantityProduced = Statistic.getStatistic(product + "-produced",
                                                    "production", COUNTER)
        s_quantitySold = Statistic(Id + ":q sold", COUNTER)

    # Evaluation method for Farm.
    #
    # @param report T/F print report
    # @param step   step in model

    def evaluate(report: bool, step: int) -> None:
        Widget food
        long initialDeposit;                    # Amount on deposit at beginning
        long depositChange;                     # Change in monetary holdings
        long saleQuantity = 0;                  # Amount sold to market
        long salariesPaid = 0;                  # Amount actually paid

        # Determine initial conditions
        initialDeposit = getDeposit()

        # Output is based on employees at start of round.
        output = len(employees) * labourInput

        # Turn output into widgets -- attempt to sell to market
        if (output != 0) and (market.getMaxLot() > 0):
            food = Widget(product, market.ttl, output)

            s_quantityProduced.add(food.quantity())
            DEBUG(name + " produced " + food.quantity() + " # employees "
                       + employees.size())

            if market.getMaxLot() < food.quantity():  # market won't buy total
                saleQuantity = market.getMaxLot()
                lastSoldPrice = market.sell(food.split(saleQuantity), -1,
                                            self.getAccount())
            else:
                saleQuantity = food.quantity()
                lastSoldPrice = market.sell(food, -1, self.getAccount())

            s_quantitySold.add(saleQuantity)  # update statistic

            if lastSoldPrice == -1:
                DEBUG(name + " sale to market failed ")
            else:
                DEBUG(name + " sold " + saleQuantity + " units " + " @ "
                      + lastSoldPrice)
                s_income.add(lastSoldPrice * s_quantitySold.get())
                s_mkt_price.add(lastSoldPrice)
        else:
            DEBUG(name + " no sale to market - maxlot is : "
                  + market.getMaxLot() + " output " + output)

        # Pay salaries. This must be done before any firing decisions are
        # made otherwise they won't get paid for work completed.

        salariesPaid = paySalaries()

        s_labourCost.add(salariesPaid)

        # Determine actions for next round.
        #
        #   Prices are rising - attempt to hire
        #   Prices are falling - reduce employees

        profit: int = lastSoldPrice * saleQuantity - salariesPaid

        #if(offeredSalary > markets.getMarket("Labour").getAskPrice())

        #if((s_mkt_price.increasing(2) || employees.size() == 0) and
        #   (surplus >= 2 * offeredSalary))
        offeredSalary = profit / 2

        if offeredSalary <= govt.minWage:
            offeredSalary = govt.minWage

        if (employees.size() == 0) or getDeposit() > 3 * salariesPaid:
            if (hireEmployee(offeredSalary, None, None) is None):
                offeredSalary += 1
        elif s_income.shrinking(2):
            if offeredSalary > 1:
                decreaseSalaries(1)
            else:
                fireEmployee()

        # Pay Taxes
        payTax(govt.corporateTaxRate, govt.corporateCutoff)

        #print(name + " funds: " + getDeposit())

    def print(label: str) -> None:
        if label is not None:
            print(label)

        print(f"{name}: {len(employees)} Input     : {labourInput} Output    : {output} Deposit: ${getDeposit()}")
