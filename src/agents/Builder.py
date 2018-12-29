# Program : Threadneedle
#
# Company : 
# 
# Author  : Jacky Mallett
# Date    : April 2015
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
# https://creativecommons.org/licenses/by-nc-nd/4.0/
#/

#
# Builder builds a single house at a time, according to:
#
#    labourInput - total labour required to build house
#    buildTime   - total time required to build house
#
#/
class Builder(Company):
    @Expose private int buildTime       = 100
    private int  HOUSE_TTL      = 120
    private Statistic s_mkt_price
    private boolean building    = false;		 # t/f house being built
    private long totalBuilt     = 0;   		 # total built to this point
    private long totalCost      = 0
    private long salariesPaid   = 0
    long profitMargin     = 25;		 # % profit margin to put on price
    private Widget house = null;		 # House, when completed
    private int    inflation = 0

    # Constructor:
    #
    # @param name       Name for company
    # @param govt       Government for company
    # @param bank       Bank for company
    # @param properties Property map (to interface with fxml)
    # Constructor from gson file
    # TODO add case when arguments are None
    def __init__(self, name: str, govt: Govt, bank: Bank,
                 properties: Dict[str, str]):
        super(name, int(properties.get("initialDeposit")), govt,
              bank)

        self.labourInput = int(properties.get("labourInput"))
        self.buildTime = int(properties.get("buildTime"))
        self.product = properties.get("product")

        if (self.market = govt.markets.getMarket(self.product)) is None:
           self.market = HouseMarket("M-" + self.product, self.product, govt, bank)
           self.market.region = self.region
           govt.markets.addMarket(self.market)

        self.initStatistics()

    # Initialise statistics for this agent.
    def initStatistics() -> None:
        super().initStatistics()

        s_mkt_price = Statistic(Id + ": mkt_price", AVERAGE)

        s_quantityProduced = Statistic.getStatistic(product + "-produced",
                                                    "production", COUNTER)
        s_quantitySold = Statistic(Id + ":q sold", COUNTER)

        print("Builder: buildTime = " + buildTime +
              " labourInput = " + labourInput)

    #* Evaluation method
    #*
    #* @param report T/F print report
    #* @param step   step in model

    def evaluate(report: bool, step: int) -> None:
        long   startDeposit;            # funds available at start of round
        long   salaryBill;              # salaries payable at start of round
        double interestcost
        long   labourcost
        long   interestRate
        int    totalWorkers

        salaryBill     = getSalaryBill()
        startDeposit   = getDeposit()

        # Production function
        #
        # Verify workers are available to hire, work out salary costs,
        # and request loan based on estimated cost to build. Note - 
        # if the company has an outstanding loan, it will not be able
        # to get another one until the existing loan is repaid.
        #
        # If loan is granted, attempt to hire workers.
        #   -- if the company can't hire, it will incur loan costs, while
        #      not building.
        #   -- the alternative is to hire the workers first, and then fire
        #      them if a loan isn't forthcoming.
        #   -- so we cheat, and check the number of workers available
        #      to hire.
        if(!building and getDebt() <= 0 and markets.getMarket(product).getTotalItems() < 2)
        {
           # total workers required is total labour divided by the length of
           # of time the builder should take to build the house.

           totalWorkers = labourInput/buildTime

           if(((LabourMarket)markets.getMarket("Labour")).totalAvailableWorkers() 
                                                                  >= totalWorkers)
           {
             # Work out total cost of building, and request a loan
             #    labourInput is total labour required - each labourInput
             #    costs the salary price for that round. 
             labourcost  = markets.getMarket("Labour").getAskPrice() * labourInput; 

#             labourcost = 50 * labourInput
             interestRate= getBank().requestInterestRate(BaselWeighting.CONSTRUCTION)

             # Use double the labourcost as the amount for the loan:
             #     - profit
             #     - cost of covering loan while waiting for sale

             totalCost = (long)(labourcost * 1.1)
             long totalLoan = (long)(1.5 * labourcost)

Sy  st  em.out.println(name + " req loan for " + totalLoan  + "/" + buildTime*2)

             if(getBank().requestLoan(self.getAccount(),
                          totalLoan,      buildTime * 2, Time.MONTH,
                          BaselWeighting.CONSTRUCTION, Loan.Type.COMPOUND) != null)
             {
                while(employees.size() < totalWorkers)
                {
                    # Hire at market rate. If we are unable to hire all employees
                    # then building will continue with reduced workers - which 
                    # will slightly increase cost of the building if it happens
                    # due to additional interest payments
                    worker: Person = hireEmployee(
                        markets.getMarket("Labour").getAskPrice(),
                        getBank(), None)
                    if worker is None:
                        print("DBG: " + getName() + " failed to hire @" +  markets.getMarket("Labour").getAskPrice())
                        break
                building = True
                getAccount().outgoing = 0
                getAccount().incoming = 0
             }
             else print(name + " failed to get loan " + labourcost)
           }
        }

        if building and Time.endOfMonth():
           totalBuilt += employees.size()
           salariesPaid += paySalaries()

        if (totalBuilt >= labourInput) and building:
            house = Widget(product, HOUSE_TTL, 1)
            s_quantityProduced.add(house.quantity())

            marketPrice: int = markets.getMarket(product).getBidPrice()

            # totalCost += getAccount().getTotalInterestPaid()

            if marketPrice > totalCost:
               totalCost = marketPrice

            for worker in employees:
               worker.setSalary(worker.getSalary() + inflation / 5)
               worker.desiredSalary = worker.getSalary()

            price: int = markets.getMarket(product).sell(house, 
                                              totalCost + inflation, getAccount())

            #System.out.println("On market: " + markets.getMarket(product).getTotalItems())
            print(name + " listed - salaries paid:" + salariesPaid + " sell price " + price )
            print(name + " total cost " + getAccount().outgoing)
            building = False
            house = None
            totalBuilt = 0
            totalCost  = 0
            salariesPaid = 0
            fireAllEmployees()
            #inflation += 1

        # Pay off loan if money available.
        for(Loan loan : getAccount().debts.values()):
            if((loan.getCapitalOutstanding() < getDeposit()) and !building):
               getBank().payBankLoan(getAccount(), loan, loan.getCapitalOutstanding())

        payDebt()  # debt before taxes...
        payTax(govt.corporateTaxRate, govt.corporateCutoff)

    def print(label: str) -> None:
        if label is not None:
            print(label)

        print(f"{name}: {len(employees)}" + f" Input     : {labourInput}" +
              f" Output    : {output}" +
              f" Deposit: ${getDeposit()}")
