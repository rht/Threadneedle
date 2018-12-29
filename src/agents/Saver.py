# Program  : Threadneedle
#
# Investor : Savers are agents who have bank accounts, but don't do anything.
#            They can be used to provide deposit money without side effects
#            on any loans that are taken out by borrowers, or to inject
#            extra cash liquidity for reserve requirements.  Note, if the
#            bank accounts pay interest on these accounts, there is no 
#            automatic counter-flow.
# 
# Author   : (c) Jacky Mallett
# Date     : November 2014
# Comments :
#
# Threadneedle is provided free for non-commercial research purposes under 
# the creative commons Attribution-NonCommercial-NoDerivatives 4.0 
# International License:
#
# https:#creativecommons.org/licenses/by-nc-nd/4.0/
#/

class Saver(Person):
    # Main behaviour loop for Saver.
    #
    # @param report  t/f print report
    # @param step    step being evaluated
    def evaluate(report: bool, step: int) -> None:
        super().evaluate(report, step)
        # Nothing to see here, move along.

    # Constructor.
    # @param name    Unique and identifying name
    # @param deposit Initial bank deposit
    # @param govt    Government
    # @param bank    Bank
    # No parameter constructor for loading from JSON. All @Expose'd variables
    # will be initialised by GSON, and it is the responsibility of the
    # controller to set anything else correctly.
    def __init__(self, name: str = None, govt: Govt = None, bank: Bank = None, deposit: int = None) -> None:
        super().__init__(name, govt, bank, deposit)
