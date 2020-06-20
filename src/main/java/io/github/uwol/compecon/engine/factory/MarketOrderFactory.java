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

package io.github.uwol.compecon.engine.factory;

import io.github.uwol.compecon.economy.markets.MarketOrder;
import io.github.uwol.compecon.economy.markets.MarketParticipant;
import io.github.uwol.compecon.economy.materia.GoodType;
import io.github.uwol.compecon.economy.property.Property;
import io.github.uwol.compecon.economy.sectors.financial.BankAccountDelegate;
import io.github.uwol.compecon.economy.sectors.financial.Currency;

public interface MarketOrderFactory {

	public void deleteAllSellingOrders(final MarketParticipant offeror);

	public void deleteAllSellingOrders(final MarketParticipant offeror, final Currency denominatedInCurrency,
			final Class<? extends Property> propertyClass);

	public void deleteAllSellingOrders(final MarketParticipant offeror, final Currency denominatedInCurrency,
			final Currency commodityCurrency);

	public void deleteAllSellingOrders(final MarketParticipant offeror, final Currency denominatedInCurrency,
			final GoodType goodType);

	public void deleteSellingOrder(final MarketOrder marketOrder);

	public MarketOrder newInstanceCurrencyMarketOrder(final Currency currencyToBeOffered,
			final MarketParticipant offeror, final BankAccountDelegate offerorsBankAcountDelegate, final double amount,
			final double pricePerUnit, final BankAccountDelegate commodityCurrencyOfferorsBankAcountDelegate);

	public MarketOrder newInstanceGoodTypeMarketOrder(final GoodType goodType, final MarketParticipant offeror,
			final BankAccountDelegate offerorsBankAcountDelegate, final double amount, final double pricePerUnit);

	public MarketOrder newInstancePropertyMarketOrder(final Property property, final MarketParticipant offeror,
			final BankAccountDelegate offerorsBankAcountDelegate, final double pricePerUnit);
}
