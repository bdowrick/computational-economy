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

package io.github.uwol.compecon.engine.service;

import java.util.List;

import io.github.uwol.compecon.economy.materia.GoodType;
import io.github.uwol.compecon.economy.sectors.financial.CentralBank;
import io.github.uwol.compecon.economy.sectors.financial.CreditBank;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.economy.sectors.household.Household;
import io.github.uwol.compecon.economy.sectors.industry.Factory;
import io.github.uwol.compecon.economy.sectors.state.State;
import io.github.uwol.compecon.economy.sectors.trading.Trader;

public interface AgentService {

	public CentralBank findCentralBank(final Currency currency);

	public List<CreditBank> findCreditBanks(final Currency currency);

	public List<Factory> findFactories(final Currency currency);

	public List<Factory> findFactories(final Currency currency, final GoodType producedGoodType);

	public List<Household> findHouseholds(final Currency currency);

	public CreditBank findRandomCreditBank(final Currency currency);

	public Factory findRandomFactory();

	public State findState(final Currency currency);

	public List<Trader> findTraders(final Currency currency);
}
