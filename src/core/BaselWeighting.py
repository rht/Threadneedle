class BaselWeighting:
    BASEL_MULTIPLIER = 10.0
    CONSTRUCTION = 0
    MORTGAGE = 1
    GOVERNMENT = 2
    IBL = 3

    construction = 0.25
    mortgage = 0.5
    government = 1.0
    ibl = 1.0
    riskmatrix = [construction, mortgage, government, ibl]

    def riskWeighting(self, loan: Loan) -> float:
        if loan.risktype == -1:
            return 1.0
        return self.riskmatrix[loan.risktype]

    def getRiskWeighting(self, n: int) -> float:
        return self.riskmatrix[n]

    def getBaselMultiplier(self) -> float:
        return self.BASEL_MULTIPLIER

    def setBaselMultiplier(self, d: float) -> None:
        self.BASEL_MULTIPLIER = d
