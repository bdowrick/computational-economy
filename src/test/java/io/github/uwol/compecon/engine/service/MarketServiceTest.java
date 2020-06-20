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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.SortedMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.github.uwol.compecon.CompEconTestSupport;
import io.github.uwol.compecon.economy.markets.MarketOrder;
import io.github.uwol.compecon.economy.materia.GoodType;
import io.github.uwol.compecon.economy.sectors.financial.CreditBank;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.economy.sectors.household.Household;
import io.github.uwol.compecon.economy.sectors.industry.Factory;
import io.github.uwol.compecon.economy.sectors.trading.Trader;
import io.github.uwol.compecon.economy.security.equity.Share;
import io.github.uwol.compecon.economy.security.equity.impl.ShareImpl;
import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.math.price.PriceFunction;
import io.github.uwol.compecon.math.price.PriceFunction.PriceFunctionConfig;

public class MarketServiceTest extends CompEconTestSupport {

	private void assertValidPriceFunctionConfig(final PriceFunction marketPriceFunction, final double maxBudget,
			final int numberOfOffers) {
		final PriceFunctionConfig[] priceFunctionConfigs = marketPriceFunction
				.getAnalyticalPriceFunctionParameters(maxBudget);
		assertEquals(numberOfOffers, priceFunctionConfigs.length);

		// check intervals
		double lastPriceAtIntervalRightBoundary = 0.0;
		for (final PriceFunctionConfig priceFunctionConfig : priceFunctionConfigs) {
			// check interval boundaries
			assertNotEquals(priceFunctionConfig.intervalLeftBoundary, priceFunctionConfig.intervalRightBoundary,
					epsilon);

			final double intervalMiddle = priceFunctionConfig.intervalRightBoundary
					- ((priceFunctionConfig.intervalRightBoundary - priceFunctionConfig.intervalLeftBoundary) / 2.0);

			// calculate analytical prices
			final double priceAtIntervalRightBoundary = priceFunctionConfig.coefficientXPower0
					+ priceFunctionConfig.coefficientXPowerMinus1 / priceFunctionConfig.intervalRightBoundary;
			final double priceAtIntervalMiddle = priceFunctionConfig.coefficientXPower0
					+ priceFunctionConfig.coefficientXPowerMinus1 / intervalMiddle;

			// compare analytical prices with prices from market price function
			assertEquals(priceAtIntervalMiddle, marketPriceFunction.getPrice(intervalMiddle), epsilon);
			assertEquals(priceAtIntervalRightBoundary,
					marketPriceFunction.getPrice(priceFunctionConfig.intervalRightBoundary), epsilon);

			if (priceFunctionConfig.intervalLeftBoundary > 0.0) {
				final double priceAtIntervalLeftBoundary = priceFunctionConfig.coefficientXPower0
						+ priceFunctionConfig.coefficientXPowerMinus1 / priceFunctionConfig.intervalLeftBoundary;

				// assert that the analytical price function does not have
				// discontinuities
				assertEquals(lastPriceAtIntervalRightBoundary, priceAtIntervalLeftBoundary, epsilon);

				// assert that the analytical price function is continuous
				assertTrue(priceAtIntervalLeftBoundary < priceAtIntervalMiddle);
				assertTrue(priceAtIntervalMiddle < priceAtIntervalRightBoundary);

				assertEquals(priceAtIntervalLeftBoundary,
						marketPriceFunction.getPrice(priceFunctionConfig.intervalLeftBoundary), epsilon);
			}

			// store the current right interval boundary as the new left
			// interval boundary for the next step of the step price function
			lastPriceAtIntervalRightBoundary = priceAtIntervalRightBoundary;
		}
	}

	@Before
	public void setup() throws IOException {
		super.setUpApplicationContext(testConfigurationPropertiesFilename);
		super.setUpTestAgents();
	}

	@Override
	@After
	public void tearDown() {
		super.tearDown();
	}

	@Test
	public void testCalculateMarketPriceFunction() {
		final Currency currency = Currency.EURO;
		final GoodType goodType = GoodType.LABOURHOUR;

		final Household household1_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(0);
		final Household household2_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(1);

		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, goodType),
				epsilon);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 10, 5);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 10, 4);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 10, 6);

		assertValidPriceFunctionConfig(
				ApplicationContext.getInstance().getMarketService().getMarketPriceFunction(currency, goodType), 150.0,
				3);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 100, 2);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 20, 20);

		assertValidPriceFunctionConfig(
				ApplicationContext.getInstance().getMarketService().getMarketPriceFunction(currency, goodType), 1500.0,
				5);
	}

	@Test
	public void testOfferCurrency() {
		final Currency currency = Currency.EURO;
		final Currency commodityCurrency = Currency.USDOLLAR;

		final CreditBank creditBank1_EUR = ApplicationContext.getInstance().getAgentService().findCreditBanks(currency)
				.get(0);
		final CreditBank creditBank2_EUR = ApplicationContext.getInstance().getAgentService().findCreditBanks(currency)
				.get(1);
		final Trader trader1_EUR = ApplicationContext.getInstance().getAgentService().findTraders(currency).get(0);

		// check marginal price
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, commodityCurrency),
				epsilon);

		// offer currency
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(commodityCurrency, creditBank1_EUR,
				creditBank1_EUR.getBankAccountTransactionsDelegate(), 10, 2,
				creditBank1_EUR.getBankAccountCurrencyTradeDelegate(commodityCurrency));

		// offer currency
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(commodityCurrency, creditBank2_EUR,
				creditBank2_EUR.getBankAccountTransactionsDelegate(), 10, 3,
				creditBank2_EUR.getBankAccountCurrencyTradeDelegate(commodityCurrency));

		// check marginal price
		assertEquals(2.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, commodityCurrency),
				epsilon);

		// remove all offers of credit bank 1
		ApplicationContext.getInstance().getMarketService().removeAllSellingOffers(creditBank1_EUR);

		// check marginal price
		assertEquals(3,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, commodityCurrency),
				epsilon);

		// offer currency
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(commodityCurrency, creditBank1_EUR,
				creditBank1_EUR.getBankAccountTransactionsDelegate(), 10, 1,
				creditBank1_EUR.getBankAccountCurrencyTradeDelegate(commodityCurrency));

		// check marginal price
		assertEquals(1.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, commodityCurrency),
				epsilon);

		// remove all offers of credit bank 1
		ApplicationContext.getInstance().getMarketService().removeAllSellingOffers(creditBank1_EUR, currency,
				commodityCurrency);

		// check marginal price
		assertEquals(3.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, commodityCurrency),
				epsilon);

		// offer curency
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(commodityCurrency, creditBank1_EUR,
				creditBank1_EUR.getBankAccountTransactionsDelegate(), 10, 1,
				creditBank1_EUR.getBankAccountCurrencyTradeDelegate(commodityCurrency));

		// check marginal price
		assertEquals(1.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, commodityCurrency),
				epsilon);

		// find best fulfillment set
		final SortedMap<MarketOrder, Double> marketOffers1 = ApplicationContext.getInstance().getMarketService()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 1, commodityCurrency);
		assertEquals(1, marketOffers1.size());

		final SortedMap<MarketOrder, Double> marketOffers2 = ApplicationContext.getInstance().getMarketService()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 5, commodityCurrency);
		assertEquals(2, marketOffers2.size());

		// buy
		ApplicationContext.getInstance().getMarketService().buy(commodityCurrency, 5, Double.NaN, 8, trader1_EUR,
				trader1_EUR.getBankAccountTransactionsDelegate(),
				trader1_EUR.getBankAccountGoodsTradeDelegate(commodityCurrency));

		// check transaction
		assertEquals(-5.0, trader1_EUR.getBankAccountTransactionsDelegate().getBankAccount().getBalance(), epsilon);
		assertEquals(5.0, trader1_EUR.getBankAccountGoodsTradeDelegate(commodityCurrency).getBankAccount().getBalance(),
				epsilon);
	}

	@Test
	public void testOfferGoodType() {
		// test market for good type
		final Currency currency = Currency.EURO;
		final GoodType goodType = GoodType.LABOURHOUR;

		final Household household1_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(0);
		final Household household2_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(1);
		final Factory factory1_WHEAT_EUR = ApplicationContext.getInstance().getAgentService().findFactories(currency)
				.get(0);

		final MarketPriceFunction marketPriceFunction = ApplicationContext.getInstance().getMarketService()
				.getMarketPriceFunction(currency, goodType);

		assertEquals(Double.NaN, marketPriceFunction.getMarginalPrice(0.0), epsilon);

		// offer goods: household 1
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 10, 5);

		// offer goods: household 2
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 10, 4);

		// -> market price function has to be renewed
		marketPriceFunction.reset();

		// check marginal price
		assertEquals(4.0, marketPriceFunction.getMarginalPrice(0.0), epsilon);
		assertEquals(4.0, marketPriceFunction.getMarginalPrice(10.0), epsilon);
		assertEquals(5.0, marketPriceFunction.getMarginalPrice(10.1), epsilon);

		// check average price
		assertEquals(4.5, marketPriceFunction.getPrice(20.0), epsilon);
		assertEquals(4.333333, marketPriceFunction.getPrice(15.0), epsilon);
		assertEquals(Double.NaN, marketPriceFunction.getPrice(21.0), epsilon);

		// check marginal price
		assertEquals(5.0, marketPriceFunction.getMarginalPrice(11.0), epsilon);
		assertEquals(5.0, marketPriceFunction.getMarginalPrice(20.0), epsilon);
		assertEquals(Double.NaN, marketPriceFunction.getMarginalPrice(21.0), epsilon);

		// compare marginal price of price function with marginal price
		// from service
		assertEquals(ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, goodType),
				marketPriceFunction.getMarginalPrice(0.0), epsilon);
		assertEquals(ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, goodType),
				marketPriceFunction.getPrice(1.0), epsilon);

		// remove all selling offers of household 2
		ApplicationContext.getInstance().getMarketService().removeAllSellingOffers(household2_EUR);

		// -> market price function has to be renewed
		marketPriceFunction.reset();

		// check marginal price
		assertEquals(5.0, marketPriceFunction.getMarginalPrice(0), epsilon);

		// offer goods: household 2
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 10, 3);

		// -> market price function has to be renewed
		marketPriceFunction.reset();

		// check marginal price
		assertEquals(3.0, marketPriceFunction.getMarginalPrice(0), epsilon);
		assertEquals(3.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, goodType),
				epsilon);

		// remove all selling offers of household 2
		ApplicationContext.getInstance().getMarketService().removeAllSellingOffers(household2_EUR, currency, goodType);

		// -> market price function has to be renewed
		marketPriceFunction.reset();

		// check marginal price
		assertEquals(5.0, marketPriceFunction.getMarginalPrice(0.0), epsilon);

		// offer goods: household 2
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(goodType, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 10, 3);

		// -> market price function has to be renewed
		marketPriceFunction.reset();

		// check marginal price
		assertEquals(3.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, goodType),
				epsilon);

		// find best fulfillment sets
		final SortedMap<MarketOrder, Double> marketOffers1 = ApplicationContext.getInstance().getMarketService()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 3, goodType);
		assertEquals(1, marketOffers1.size());

		final SortedMap<MarketOrder, Double> marketOffers2 = ApplicationContext.getInstance().getMarketService()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 5, goodType);
		assertEquals(2, marketOffers2.size());

		// buy goods
		ApplicationContext.getInstance().getMarketService().buy(goodType, 5, Double.NaN, 8, factory1_WHEAT_EUR,
				factory1_WHEAT_EUR.getBankAccountTransactionsDelegate());

		// check property and money transaction
		assertEquals(5,
				ApplicationContext.getInstance().getPropertyService().getGoodTypeBalance(factory1_WHEAT_EUR, goodType),
				epsilon);
		assertEquals(-15.0, factory1_WHEAT_EUR.getBankAccountTransactionsDelegate().getBankAccount().getBalance(),
				epsilon);
	}

	@Test
	public void testOfferProperty() {
		final Currency currency = Currency.EURO;

		final Household household1_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(0);
		final Factory factory1_WHEAT_EUR = ApplicationContext.getInstance().getAgentService().findFactories(currency)
				.get(0);

		// check marginal price
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, Share.class),
				epsilon);

		// IPO
		factory1_WHEAT_EUR.issueShares();

		// check marginal price
		assertEquals(0.0,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, ShareImpl.class),
				epsilon);

		// check number of offered shares
		assertEquals(
				ApplicationContext.getInstance().getConfiguration().jointStockCompanyConfig.getInitialNumberOfShares(),
				ApplicationContext.getInstance().getPropertyService()
						.findAllPropertiesOfPropertyOwner(factory1_WHEAT_EUR, Share.class).size());

		// buy one share
		ApplicationContext.getInstance().getMarketService().buy(ShareImpl.class, 1, Double.NaN, Double.NaN,
				household1_EUR, household1_EUR.getBankAccountTransactionsDelegate());

		// check transaction / settlement
		assertEquals(
				ApplicationContext.getInstance().getConfiguration().jointStockCompanyConfig.getInitialNumberOfShares()
						- 1,
				ApplicationContext.getInstance().getPropertyService()
						.findAllPropertiesOfPropertyOwner(factory1_WHEAT_EUR, Share.class).size());
		assertEquals(1, ApplicationContext.getInstance().getPropertyService()
				.findAllPropertiesOfPropertyOwner(household1_EUR, Share.class).size());

		// remove all selling offers
		ApplicationContext.getInstance().getMarketService().removeAllSellingOffers(factory1_WHEAT_EUR);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, Share.class),
				epsilon);
	}
}
