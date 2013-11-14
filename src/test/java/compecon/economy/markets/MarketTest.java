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

package compecon.economy.markets;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.SortedMap;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import compecon.CompEconTestSupport;
import compecon.economy.markets.impl.MarketImpl.MarketPriceFunction;
import compecon.economy.sectors.financial.CreditBank;
import compecon.economy.sectors.financial.Currency;
import compecon.economy.sectors.household.Household;
import compecon.economy.sectors.industry.Factory;
import compecon.economy.sectors.trading.Trader;
import compecon.economy.security.equity.impl.JointStockCompanyImpl;
import compecon.economy.security.equity.impl.ShareImpl;
import compecon.engine.applicationcontext.ApplicationContext;
import compecon.engine.timesystem.ITimeSystemEvent;
import compecon.materia.GoodType;
import compecon.math.price.PriceFunction.PriceFunctionConfig;

public class MarketTest extends CompEconTestSupport {

	@Before
	public void setUp() {
		super.setUp();
	}

	@After
	public void tearDown() {
		super.tearDown();
	}

	@Test
	public void testOfferGoodType() {
		// test market for good type
		Currency currency = Currency.EURO;
		GoodType goodType = GoodType.LABOURHOUR;

		Household household1_EUR = ApplicationContext.getInstance()
				.getHouseholdDAO().findAllByCurrency(currency).get(0);
		Household household2_EUR = ApplicationContext.getInstance()
				.getHouseholdDAO().findAllByCurrency(currency).get(1);
		Factory factory1_WHEAT_EUR = ApplicationContext.getInstance()
				.getFactoryDAO().findAllByCurrency(currency).get(0);

		assertEquals(Double.NaN, ApplicationContext.getInstance()
				.getMarketFactory().getMarket().getPrice(currency, goodType),
				epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household1_EUR,
						household1_EUR.getBankAccountTransactions(), 10, 5);
		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 10, 4);

		assertEquals(4.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), epsilon);
		assertEquals(ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), ApplicationContext
				.getInstance().getMarketFactory().getMarket()
				.getMarketPriceFunction(currency, goodType).getPrice(0.0),
				epsilon);
		assertEquals(ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), ApplicationContext
				.getInstance().getMarketFactory().getMarket()
				.getMarketPriceFunction(currency, goodType).getPrice(1.0),
				epsilon);

		assertEquals(4.5, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getMarketPriceFunction(currency, goodType)
				.getPrice(20.0), epsilon);
		assertEquals(
				4.333333,
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getMarketPriceFunction(currency, goodType)
						.getPrice(15.0), epsilon);
		assertEquals(4.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getMarketPriceFunction(currency, goodType)
				.getMarginalPrice(10.0), epsilon);
		assertEquals(5.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getMarketPriceFunction(currency, goodType)
				.getMarginalPrice(11.0), epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getMarketPriceFunction(currency, goodType)
						.getMarginalPrice(21.0), epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getAveragePrice(currency, goodType, 21.0), epsilon);

		ApplicationContext.getInstance().getMarketFactory().getMarket()
				.removeAllSellingOffers(household2_EUR);
		assertEquals(5.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 10, 3);
		assertEquals(3.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), epsilon);

		ApplicationContext.getInstance().getMarketFactory().getMarket()
				.removeAllSellingOffers(household2_EUR, currency, goodType);
		assertEquals(5.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 10, 3);
		assertEquals(3.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, goodType), epsilon);

		SortedMap<MarketOrder, Double> marketOffers1 = ApplicationContext
				.getInstance().getMarketFactory().getMarket()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 3, goodType);
		assertEquals(1, marketOffers1.size());

		SortedMap<MarketOrder, Double> marketOffers2 = ApplicationContext
				.getInstance().getMarketFactory().getMarket()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 5, goodType);
		assertEquals(2, marketOffers2.size());

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.buy(goodType, 5, Double.NaN, 8, factory1_WHEAT_EUR,
						factory1_WHEAT_EUR.getBankAccountTransactions());

		assertEquals(5, ApplicationContext.getInstance().getPropertyRegister()
				.getBalance(factory1_WHEAT_EUR, goodType), epsilon);
		assertEquals(-15.0, factory1_WHEAT_EUR.getBankAccountTransactions()
				.getBalance(), epsilon);
	}

	@Test
	public void testOfferProperty() {
		Currency currency = Currency.EURO;

		Factory factory1_WHEAT_EUR = ApplicationContext.getInstance()
				.getFactoryDAO().findAllByCurrency(currency).get(0);
		Household household1_EUR = ApplicationContext.getInstance()
				.getHouseholdDAO().findAllByCurrency(currency).get(0);

		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getPrice(currency, ShareImpl.class), epsilon);

		for (ITimeSystemEvent timeSystemEvent : factory1_WHEAT_EUR
				.getTimeSystemEvents()) {
			if (timeSystemEvent instanceof JointStockCompanyImpl.OfferSharesEvent)
				timeSystemEvent.onEvent();
		}

		assertEquals(0.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, ShareImpl.class), epsilon);
		assertEquals(
				ApplicationContext.getInstance().getConfiguration().jointStockCompanyConfig
						.getInitialNumberOfShares(),
				ApplicationContext.getInstance().getPropertyRegister()
						.getProperties(factory1_WHEAT_EUR, ShareImpl.class)
						.size());

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.buy(ShareImpl.class, 1, Double.NaN, Double.NaN,
						household1_EUR,
						household1_EUR.getBankAccountTransactions());

		assertEquals(
				ApplicationContext.getInstance().getConfiguration().jointStockCompanyConfig
						.getInitialNumberOfShares() - 1,
				ApplicationContext.getInstance().getPropertyRegister()
						.getProperties(factory1_WHEAT_EUR, ShareImpl.class)
						.size());
		assertEquals(1, ApplicationContext.getInstance().getPropertyRegister()
				.getProperties(household1_EUR, ShareImpl.class).size());

		ApplicationContext.getInstance().getMarketFactory().getMarket()
				.removeAllSellingOffers(factory1_WHEAT_EUR);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getPrice(currency, ShareImpl.class), epsilon);
	}

	@Test
	public void testOfferCurrency() {
		Currency currency = Currency.EURO;
		Currency commodityCurrency = Currency.USDOLLAR;

		CreditBank creditBank1_EUR = ApplicationContext.getInstance()
				.getCreditBankDAO().findAllByCurrency(currency).get(0);
		CreditBank creditBank2_EUR = ApplicationContext.getInstance()
				.getCreditBankDAO().findAllByCurrency(currency).get(1);
		Trader trader1_EUR = ApplicationContext.getInstance().getTraderDAO()
				.findAllByCurrency(currency).get(0);

		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getPrice(currency, commodityCurrency), epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(
						commodityCurrency,
						creditBank1_EUR,
						creditBank1_EUR.getBankAccountTransactions(),
						10,
						2,
						creditBank1_EUR.getBankAccountsCurrencyTrade().get(
								commodityCurrency));

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(
						commodityCurrency,
						creditBank2_EUR,
						creditBank2_EUR.getBankAccountTransactions(),
						10,
						3,
						creditBank2_EUR.getBankAccountsCurrencyTrade().get(
								commodityCurrency));
		assertEquals(2.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, commodityCurrency), epsilon);

		ApplicationContext.getInstance().getMarketFactory().getMarket()
				.removeAllSellingOffers(creditBank1_EUR);
		assertEquals(3, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, commodityCurrency), epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(
						commodityCurrency,
						creditBank1_EUR,
						creditBank1_EUR.getBankAccountTransactions(),
						10,
						1,
						creditBank1_EUR.getBankAccountsCurrencyTrade().get(
								commodityCurrency));
		assertEquals(1.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, commodityCurrency), epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.removeAllSellingOffers(creditBank1_EUR, currency,
						commodityCurrency);
		assertEquals(3.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, commodityCurrency), epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(
						commodityCurrency,
						creditBank1_EUR,
						creditBank1_EUR.getBankAccountTransactions(),
						10,
						1,
						creditBank1_EUR.getBankAccountsCurrencyTrade().get(
								commodityCurrency));
		assertEquals(1.0, ApplicationContext.getInstance().getMarketFactory()
				.getMarket().getPrice(currency, commodityCurrency), epsilon);

		SortedMap<MarketOrder, Double> marketOffers1 = ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 1,
						commodityCurrency);
		assertEquals(1, marketOffers1.size());

		SortedMap<MarketOrder, Double> marketOffers2 = ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.findBestFulfillmentSet(currency, 20, Double.NaN, 5,
						commodityCurrency);
		assertEquals(2, marketOffers2.size());

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.buy(commodityCurrency,
						5,
						Double.NaN,
						8,
						trader1_EUR,
						trader1_EUR.getBankAccountTransactions(),
						trader1_EUR.getBankAccountsGoodTrade().get(
								commodityCurrency));

		assertEquals(-5.0, trader1_EUR.getBankAccountTransactions()
				.getBalance(), epsilon);
		assertEquals(5.0,
				trader1_EUR.getBankAccountsGoodTrade().get(commodityCurrency)
						.getBalance(), epsilon);
	}

	@Test
	public void testCalculateMarketPriceFunction() {
		Currency currency = Currency.EURO;
		GoodType goodType = GoodType.LABOURHOUR;

		Household household1_EUR = ApplicationContext.getInstance()
				.getHouseholdDAO().findAllByCurrency(currency).get(0);
		Household household2_EUR = ApplicationContext.getInstance()
				.getHouseholdDAO().findAllByCurrency(currency).get(1);

		assertEquals(Double.NaN, ApplicationContext.getInstance()
				.getMarketFactory().getMarket().getPrice(currency, goodType),
				epsilon);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household1_EUR,
						household1_EUR.getBankAccountTransactions(), 10, 5);
		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 10, 4);
		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 10, 6);

		assertValidPriceFunctionConfig(
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getMarketPriceFunction(currency, goodType), 150.0, 3);

		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 100, 2);
		ApplicationContext
				.getInstance()
				.getMarketFactory()
				.getMarket()
				.placeSellingOffer(goodType, household2_EUR,
						household2_EUR.getBankAccountTransactions(), 20, 20);

		assertValidPriceFunctionConfig(
				ApplicationContext.getInstance().getMarketFactory().getMarket()
						.getMarketPriceFunction(currency, goodType), 1500.0, 5);
	}

	private void assertValidPriceFunctionConfig(
			MarketPriceFunction marketPriceFunction, double maxBudget,
			int numberOfOffers) {
		PriceFunctionConfig[] priceFunctionConfigs = marketPriceFunction
				.getAnalyticalPriceFunctionParameters(maxBudget);
		assertEquals(numberOfOffers, priceFunctionConfigs.length);

		// check intervals
		double lastPriceAtIntervalRightBoundary = 0.0;
		for (PriceFunctionConfig priceFunctionConfig : priceFunctionConfigs) {
			// check interval boundaries
			assertNotEquals(priceFunctionConfig.intervalLeftBoundary,
					priceFunctionConfig.intervalRightBoundary, epsilon);

			double intervalMiddle = priceFunctionConfig.intervalRightBoundary
					- ((priceFunctionConfig.intervalRightBoundary - priceFunctionConfig.intervalLeftBoundary) / 2.0);

			// calculate analytical prices
			double priceAtIntervalRightBoundary = priceFunctionConfig.coefficientXPower0
					+ priceFunctionConfig.coefficientXPowerMinus1
					/ priceFunctionConfig.intervalRightBoundary;
			double priceAtIntervalMiddle = priceFunctionConfig.coefficientXPower0
					+ priceFunctionConfig.coefficientXPowerMinus1
					/ intervalMiddle;

			// compare analytical prices with prices from market price function
			assertEquals(priceAtIntervalMiddle,
					marketPriceFunction.getPrice(intervalMiddle), epsilon);
			assertEquals(
					priceAtIntervalRightBoundary,
					marketPriceFunction
							.getPrice(priceFunctionConfig.intervalRightBoundary),
					epsilon);

			if (priceFunctionConfig.intervalLeftBoundary > 0.0) {
				double priceAtIntervalLeftBoundary = priceFunctionConfig.coefficientXPower0
						+ priceFunctionConfig.coefficientXPowerMinus1
						/ priceFunctionConfig.intervalLeftBoundary;

				// assert that the analytical price function does not have
				// discontinuities
				assertEquals(lastPriceAtIntervalRightBoundary,
						priceAtIntervalLeftBoundary, epsilon);

				// assert that the analytical price function is continuous
				assertTrue(priceAtIntervalLeftBoundary < priceAtIntervalMiddle);
				assertTrue(priceAtIntervalMiddle < priceAtIntervalRightBoundary);

				assertEquals(
						priceAtIntervalLeftBoundary,
						marketPriceFunction
								.getPrice(priceFunctionConfig.intervalLeftBoundary),
						epsilon);
			}

			// store the current right interval boundary as the new left
			// interval boundary for the next step of the step price function
			lastPriceAtIntervalRightBoundary = priceAtIntervalRightBoundary;
		}
	}
}
