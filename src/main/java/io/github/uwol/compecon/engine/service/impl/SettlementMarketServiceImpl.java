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

package io.github.uwol.compecon.engine.service.impl;

import java.util.Map.Entry;
import java.util.SortedMap;

import io.github.uwol.compecon.economy.markets.MarketOrder;
import io.github.uwol.compecon.economy.markets.MarketParticipant;
import io.github.uwol.compecon.economy.materia.GoodType;
import io.github.uwol.compecon.economy.property.Property;
import io.github.uwol.compecon.economy.sectors.financial.Bank;
import io.github.uwol.compecon.economy.sectors.financial.BankAccountDelegate;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.engine.service.SettlementMarketService;
import io.github.uwol.compecon.math.util.MathUtil;

/**
 * The settlement market is a special market that transfers ownership of offered
 * goods and money, automatically.
 */
public class SettlementMarketServiceImpl extends MarketServiceImpl implements SettlementMarketService {

	@Override
	public double[] buy(final Class<? extends Property> propertyClass, final double maxAmount,
			final double maxTotalPrice, final double maxPricePerUnit, final MarketParticipant buyer,
			final BankAccountDelegate buyersBankAccountDelegate) {
		return this.buy(null, null, propertyClass, maxAmount, maxTotalPrice, maxPricePerUnit, true, buyer,
				buyersBankAccountDelegate, null);
	}

	@Override
	public double[] buy(final Currency commodityCurrency, final double maxAmount, final double maxTotalPrice,
			final double maxPricePerUnit, final MarketParticipant buyer,
			final BankAccountDelegate buyersBankAccountDelegate,
			final BankAccountDelegate buyersBankAccountForCommodityCurrencyDelegate) {
		return this.buy(null, commodityCurrency, null, maxAmount, maxTotalPrice, maxPricePerUnit, false, buyer,
				buyersBankAccountDelegate, buyersBankAccountForCommodityCurrencyDelegate);
	}

	/**
	 * @return total price and total amount
	 */
	protected double[] buy(final GoodType goodType, final Currency commodityCurrency,
			final Class<? extends Property> propertyClass, final double maxAmount, final double maxTotalPrice,
			final double maxPricePerUnit, final boolean wholeNumber, final MarketParticipant buyer,
			final BankAccountDelegate buyersBankAccountDelegate,
			final BankAccountDelegate buyersBankAccountForCommodityCurrencyDelegate) {

		final SortedMap<MarketOrder, Double> marketOffers = this.findBestFulfillmentSet(
				buyersBankAccountDelegate.getBankAccount().getCurrency(), maxAmount, maxTotalPrice, maxPricePerUnit,
				wholeNumber, goodType, commodityCurrency, propertyClass);

		final Bank buyersBank = buyersBankAccountDelegate.getBankAccount().getManagingBank();

		double moneySpentSum = 0;
		double amountSum = 0;
		final double[] priceAndAmount = new double[2];

		for (final Entry<MarketOrder, Double> entry : marketOffers.entrySet()) {
			final MarketOrder marketOrder = entry.getKey();
			final double amount = entry.getValue();

			// empty market order should not exist, as they are deleted
			// after execution in this method
			assert (marketOrder.getAmount() > 0);
			assert (marketOrder.getOfferor() == marketOrder.getOfferorsBankAcountDelegate().getBankAccount()
					.getOwner());

			// if the offeror's bank account is identical to the buyer's bank
			// account
			if (buyersBankAccountDelegate.getBankAccount() == marketOrder.getOfferorsBankAcountDelegate()
					.getBankAccount()) {
				continue;
			}

			// if the offeror is identical to the buyer
			if (buyersBankAccountDelegate.getBankAccount().getOwner() == marketOrder.getOfferorsBankAcountDelegate()
					.getBankAccount().getOwner()) {
				continue;
			}

			// transfer money
			buyersBank.transferMoney(buyersBankAccountDelegate.getBankAccount(),
					marketOrder.getOfferorsBankAcountDelegate().getBankAccount(),
					amount * marketOrder.getPricePerUnit(),
					"price for " + MathUtil.round(amount) + " units of " + marketOrder.getCommodity());

			// transfer ownership
			switch (marketOrder.getCommodityType()) {
			case GOODTYPE:
				// transfer goods
				ApplicationContext.getInstance().getPropertyService().transferGoodTypeAmount(marketOrder.getGoodType(),
						marketOrder.getOfferor(), buyer, amount);

				// decrement amount in market order
				marketOrder.decrementAmount(amount);

				// inform event listener
				marketOrder.getOfferor().onMarketSettlement(marketOrder.getGoodType(), amount,
						marketOrder.getPricePerUnit(),
						marketOrder.getOfferorsBankAcountDelegate().getBankAccount().getCurrency());

				// register market tick
				getLog().market_onTick(marketOrder.getPricePerUnit(), marketOrder.getGoodType(),
						marketOrder.getOfferorsBankAcountDelegate().getBankAccount().getCurrency(), amount);

				// optionally, delete market order
				if (MathUtil.lesserEqual(marketOrder.getAmount(), 0)) {
					removeSellingOffer(marketOrder);
				}
				break;
			case CURRENCY:
				final Bank bank = marketOrder.getCommodityCurrencyOfferorsBankAccountDelegate().getBankAccount()
						.getManagingBank();

				// transfer commodity currency
				bank.transferMoney(marketOrder.getCommodityCurrencyOfferorsBankAccountDelegate().getBankAccount(),
						buyersBankAccountForCommodityCurrencyDelegate.getBankAccount(), amount,
						"transfer of " + Currency.formatMoneySum(amount) + " units of commoditycurrency "
								+ marketOrder.getCommodity());

				// decrement amount in market order
				marketOrder.decrementAmount(amount);

				// inform event listener
				marketOrder.getOfferor().onMarketSettlement(marketOrder.getCommodityCurrency(), amount,
						marketOrder.getPricePerUnit(),
						marketOrder.getOfferorsBankAcountDelegate().getBankAccount().getCurrency());

				// register market tick
				getLog().market_onTick(marketOrder.getPricePerUnit(), marketOrder.getCommodityCurrency(),
						marketOrder.getOfferorsBankAcountDelegate().getBankAccount().getCurrency(), amount);

				// optionally, delete market order
				if (MathUtil.lesserEqual(marketOrder.getAmount(), 0)) {
					removeSellingOffer(marketOrder);
				}
				break;
			case PROPERTY:
				assert (marketOrder.getProperty().getOwner() == marketOrder.getOfferor());

				// transfer property
				ApplicationContext.getInstance().getPropertyService().transferProperty(marketOrder.getProperty(),
						marketOrder.getOfferor(), buyer);

				// inform event listener
				marketOrder.getOfferor().onMarketSettlement(marketOrder.getProperty(), marketOrder.getPricePerUnit(),
						marketOrder.getOfferorsBankAcountDelegate().getBankAccount().getCurrency());

				// delete market order
				removeSellingOffer(marketOrder);
				break;
			default:
				throw new RuntimeException("CommodityType unknown");
			}

			moneySpentSum += amount * marketOrder.getPricePerUnit();
			amountSum += amount;
		}

		priceAndAmount[0] = moneySpentSum;
		priceAndAmount[1] = amountSum;

		if (getLog().isAgentSelectedByClient(buyer)) {
			if (priceAndAmount[1] > 0) {
				getLog().log(buyer,
						"bought %s units of %s for %s %s under constraints [maxAmount: %s, maxTotalPrice: %s %s, maxPricePerUnit: %s %s]",
						MathUtil.round(priceAndAmount[1]),
						determineCommodityName(goodType, commodityCurrency, propertyClass),
						Currency.formatMoneySum(priceAndAmount[0]),
						buyersBankAccountDelegate.getBankAccount().getCurrency(), MathUtil.round(maxAmount),
						Currency.formatMoneySum(maxTotalPrice),
						buyersBankAccountDelegate.getBankAccount().getCurrency(),
						Currency.formatMoneySum(maxPricePerUnit),
						buyersBankAccountDelegate.getBankAccount().getCurrency());
			} else {
				getLog().log(buyer,
						"cannot buy %s, since no matching offers for %s under constraints [maxAmount: %s, maxTotalPrice: %s %s, maxPricePerUnit: %s %s]",
						determineCommodityName(goodType, commodityCurrency, propertyClass),
						determineCommodityName(goodType, commodityCurrency, propertyClass), MathUtil.round(maxAmount),
						Currency.formatMoneySum(maxTotalPrice),
						buyersBankAccountDelegate.getBankAccount().getCurrency(),
						Currency.formatMoneySum(maxPricePerUnit),
						buyersBankAccountDelegate.getBankAccount().getCurrency());
			}
		}

		return priceAndAmount;
	}

	@Override
	public double[] buy(final GoodType goodType, final double maxAmount, final double maxTotalPrice,
			final double maxPricePerUnit, final MarketParticipant buyer,
			final BankAccountDelegate buyersBankAccountDelegate) {
		return this.buy(goodType, null, null, maxAmount, maxTotalPrice, maxPricePerUnit, goodType.isWholeNumber(),
				buyer, buyersBankAccountDelegate, null);
	}

	private String determineCommodityName(final GoodType goodType, final Currency commodityCurrency,
			final Class<? extends Property> propertyClass) {
		if (commodityCurrency != null) {
			return commodityCurrency.getIso4217Code();
		}

		if (propertyClass != null) {
			return propertyClass.getSimpleName();
		}

		return goodType.toString();
	}
}
