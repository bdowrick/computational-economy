/*
Copyright (C) 2013 u.wol@wwu.de

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

package io.github.uwol.compecon.engine.statistics;

import java.util.HashMap;
import java.util.Map;

import io.github.uwol.compecon.economy.agent.Agent;
import io.github.uwol.compecon.economy.sectors.financial.BankCustomer;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.engine.statistics.accumulator.PeriodDataAccumulator;

public class MonetaryTransactionsModel extends NotificationListenerModel {

	// stores transaction values in a type-safe way
	protected Map<Class<? extends Agent>, Map<Class<? extends Agent>, PeriodDataAccumulator>> adjacencyMatrix = new HashMap<Class<? extends Agent>, Map<Class<? extends Agent>, PeriodDataAccumulator>>();

	public MonetaryTransactionsModel() {
		// from
		for (final Class<? extends Agent> agentTypeFrom : ApplicationContext.getInstance().getAgentFactory()
				.getAgentTypes()) {
			final Map<Class<? extends Agent>, PeriodDataAccumulator> toMap = new HashMap<Class<? extends Agent>, PeriodDataAccumulator>();
			adjacencyMatrix.put(agentTypeFrom, toMap);

			// to
			for (final Class<? extends Agent> agentTypeTo : ApplicationContext.getInstance().getAgentFactory()
					.getAgentTypes()) {
				toMap.put(agentTypeTo, new PeriodDataAccumulator());
			}
		}
	}

	public void bank_onTransfer(final Class<? extends BankCustomer> from, final Class<? extends BankCustomer> to,
			final Currency currency, final double value) {
		adjacencyMatrix.get(from).get(to).add(value);
	}

	public Map<Class<? extends Agent>, Map<Class<? extends Agent>, PeriodDataAccumulator>> getAdjacencyMatrix() {
		return adjacencyMatrix;
	}

	public void nextPeriod() {
		notifyListeners();

		for (final Class<? extends Agent> agentTypeFrom : ApplicationContext.getInstance().getAgentFactory()
				.getAgentTypes()) {
			for (final Class<? extends Agent> agentTypeTo : ApplicationContext.getInstance().getAgentFactory()
					.getAgentTypes()) {
				adjacencyMatrix.get(agentTypeFrom).get(agentTypeTo).reset();
			}
		}
	}
}
