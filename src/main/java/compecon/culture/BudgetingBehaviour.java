/*
This file is part of ComputationalEconomy.

ComputationalEconomy is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

ComputationalEconomy is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with ComputationalEconomy. If not, see <http://www.gnu.org/licenses/>.
 */

package compecon.culture;

import compecon.culture.sectors.financial.Currency;
import compecon.engine.Agent;
import compecon.engine.AgentFactory;
import compecon.engine.jmx.Log;
import compecon.engine.util.MathUtil;

/**
 * This behaviour controls buying decisions.It is injected into an agent (thus
 * compositions instead of inheritance). <br />
 * The key interest rate influences the buying behaviour via a simulated
 * transmission mechanism.
 */
public class BudgetingBehaviour {

	protected final Agent agent;

	protected double lastCredit = Double.NaN;

	public BudgetingBehaviour(Agent agent) {
		this.agent = agent;
	}

	public double calculateTransmissionBasedBudgetForPeriod(Currency currency,
			double bankAccountBalance, double maxTotalCredit) {
		return this.calculateTransmissionBasedBudgetWithoutUpperBound(currency,
				bankAccountBalance, maxTotalCredit);
	}

	protected double calculateTransmissionBasedBudgetWithoutUpperBound(
			Currency currency, double bankAccountBalance, double maxTotalCredit) {
		if (Double.isNaN(lastCredit)) {
			lastCredit = maxTotalCredit;
		}

		if (MathUtil.equal(maxTotalCredit, 0.0)) {
			return bankAccountBalance;
		} else {
			double keyInterestRate = AgentFactory.getInstanceCentralBank(
					currency).getEffectiveKeyInterestRate();
			double difference = keyInterestRate - 0.1;
			// double factor = 1 - Math.cbrt((keyInterestRate - 0.1) / 10.0);
			double factor = 1 - (difference / 50);
			double credit = lastCredit * factor;
			double budget = bankAccountBalance + credit;
			lastCredit = credit;
			return budget;
		}
	}

	/*
	 * maxCredit defines the maximum additional debt the buyer is going to
	 * accept -> nexus with the monetary sphere
	 */
	protected double calculateTransmissionBasedBudgetForPeriodWitUpperBound(
			Currency currency, double bankAccountBalance, double maxTotalCredit) {

		/*
		 * transmission mechanism
		 */
		// when key interest rate is high -> agents borrow less money
		double transmissionDamper = calculateTransmissionDamper(currency);
		double creditBasedBudget = Math.max(0,
				(bankAccountBalance + maxTotalCredit)
						* (1 - transmissionDamper));
		if (Log.isAgentSelectedByClient(BudgetingBehaviour.this.agent))
			Log.log(BudgetingBehaviour.this.agent,
					Currency.round(creditBasedBudget) + " "
							+ currency.getIso4217Code() + " budget = ("
							+ Currency.round(bankAccountBalance) + " "
							+ currency.getIso4217Code()
							+ " bankAccountBalance + " + maxTotalCredit + " "
							+ currency.getIso4217Code() + " maxCredit)" + " * "
							+ MathUtil.round(1 - transmissionDamper));
		return creditBasedBudget;
	}

	/**
	 * number in [0, 1] with 1 as max dampening and 0 as no dampening
	 */
	protected double calculateTransmissionDamper(Currency currency) {
		return this.calculateKeyInterestRateElasticity(currency)
				* AgentFactory.getInstanceCentralBank(currency)
						.getEffectiveKeyInterestRate();
	}

	protected double calculateKeyInterestRateElasticity(Currency currency) {
		return 1 / AgentFactory.getInstanceCentralBank(currency)
				.getMaxEffectiveKeyInterestRate();
	}
}
