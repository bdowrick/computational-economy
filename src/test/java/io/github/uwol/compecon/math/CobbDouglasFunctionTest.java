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

package io.github.uwol.compecon.math;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.github.uwol.compecon.CompEconTestSupport;
import io.github.uwol.compecon.economy.materia.GoodType;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.economy.sectors.household.Household;
import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.engine.service.impl.FixedPriceFunctionImpl;
import io.github.uwol.compecon.math.impl.CobbDouglasFunctionImpl;
import io.github.uwol.compecon.math.price.PriceFunction;

public class CobbDouglasFunctionTest extends CompEconTestSupport {

	final int numberOfIterations = 500;

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
	public void testCalculateForFourGoodsWithNaNMarketPrices() {

		/*
		 * prepare market
		 */

		final Currency currency = Currency.EURO;

		final Household household1_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(0);
		final Household household2_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(1);

		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.KILOWATT),
				epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.WHEAT),
				epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.COAL),
				epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.IRON),
				epsilon);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 200.0, 2.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 50.0, 1.0);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 200.0, 4.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 40.0, 1.0);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.COAL, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 150.0, 5.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.COAL, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 60.0, 1.0);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.IRON, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 220.0, 3.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.IRON, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 770.0, 1.0);

		/*
		 * prepare function
		 */
		final Map<GoodType, Double> exponents = new HashMap<GoodType, Double>();
		exponents.put(GoodType.KILOWATT, 0.25);
		exponents.put(GoodType.WHEAT, 0.25);
		exponents.put(GoodType.COAL, 0.25);
		exponents.put(GoodType.IRON, 0.25);
		final CobbDouglasFunctionImpl<GoodType> cobbDouglasFunction = new CobbDouglasFunctionImpl<GoodType>(1.0,
				exponents);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, PriceFunction> priceFunctions = ApplicationContext.getInstance().getMarketService()
				.getMarketPriceFunctions(currency,
						new GoodType[] { GoodType.KILOWATT, GoodType.WHEAT, GoodType.COAL, GoodType.IRON });

		final double budget = 1000;

		final Map<GoodType, Double> optimalInputsIterative = cobbDouglasFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);

		/*
		 * assert inputs
		 */

		/*
		 * assert output
		 */

		/*
		 * assert marginal outputs
		 */
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsIterative, priceFunctions);
	}

	@Test
	public void testCalculateForThreeGoodsWithNaNFixedPrices() {
		/*
		 * prepare function
		 */
		final Map<GoodType, Double> exponents = new HashMap<GoodType, Double>();
		exponents.put(GoodType.COAL, 0.1);
		exponents.put(GoodType.KILOWATT, 0.3);
		exponents.put(GoodType.WHEAT, 0.6);
		final CobbDouglasFunctionImpl<GoodType> cobbDouglasFunction = new CobbDouglasFunctionImpl<GoodType>(1.0,
				exponents);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, Double> prices = new HashMap<GoodType, Double>();
		prices.put(GoodType.COAL, Double.NaN);
		prices.put(GoodType.KILOWATT, 1.0);
		prices.put(GoodType.WHEAT, 2.0);

		final Map<GoodType, PriceFunction> priceFunctions = new HashMap<GoodType, PriceFunction>();
		priceFunctions.put(GoodType.COAL, new FixedPriceFunctionImpl(Double.NaN));
		priceFunctions.put(GoodType.KILOWATT, new FixedPriceFunctionImpl(1.0));
		priceFunctions.put(GoodType.WHEAT, new FixedPriceFunctionImpl(2.0));

		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputsAnalyticalFixedPrices = cobbDouglasFunction
				.calculateOutputMaximizingInputsAnalyticalWithFixedPrices(prices, budget);
		final Map<GoodType, Double> optimalInputsAnalyticalPriceFunctions = cobbDouglasFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cobbDouglasFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cobbDouglasFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputs = cobbDouglasFunction.calculateOutputMaximizingInputs(priceFunctions,
				budget);

		/*
		 * assert inputs
		 */
		assertEquals(0.0, optimalInputsAnalyticalFixedPrices.get(GoodType.COAL), epsilon);
		assertEquals(0.0, optimalInputsAnalyticalFixedPrices.get(GoodType.KILOWATT), epsilon);
		assertEquals(0.0, optimalInputsAnalyticalFixedPrices.get(GoodType.WHEAT), epsilon);

		for (final GoodType goodType : optimalInputsAnalyticalFixedPrices.keySet()) {
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType),
					optimalInputsAnalyticalPriceFunctions.get(goodType), epsilon);
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType), optimalInputsIterative.get(goodType),
					epsilon);
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType), optimalInputsBruteForce.get(goodType),
					epsilon);
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType), optimalInputs.get(goodType), epsilon);
		}

		/*
		 * assert output
		 */
		assertEquals(0.0, cobbDouglasFunction.f(optimalInputsAnalyticalFixedPrices), epsilon);

		assertOutputIsOptimalUnderBudget(cobbDouglasFunction, budget, priceFunctions,
				optimalInputsAnalyticalFixedPrices);

		/*
		 * assert marginal outputs
		 */
		assertEquals(0.0, cobbDouglasFunction.partialDerivative(optimalInputsAnalyticalFixedPrices, GoodType.KILOWATT),
				epsilon);
		assertEquals(0.0, cobbDouglasFunction.partialDerivative(optimalInputsAnalyticalFixedPrices, GoodType.WHEAT),
				epsilon);

		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsAnalyticalFixedPrices,
				priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsAnalyticalPriceFunctions,
				priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsBruteForce, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputs, priceFunctions);
	}

	@Test
	public void testCalculateForTwoGoodsWithFixedPrices() {
		/*
		 * prepare function
		 */
		final Map<GoodType, Double> exponents = new HashMap<GoodType, Double>();
		exponents.put(GoodType.KILOWATT, 0.4);
		exponents.put(GoodType.WHEAT, 0.6);
		final CobbDouglasFunctionImpl<GoodType> cobbDouglasFunction = new CobbDouglasFunctionImpl<GoodType>(1.0,
				exponents);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, Double> prices = new HashMap<GoodType, Double>();
		prices.put(GoodType.COAL, Double.NaN);
		prices.put(GoodType.KILOWATT, 1.0);
		prices.put(GoodType.WHEAT, 2.0);

		final Map<GoodType, PriceFunction> priceFunctions = new HashMap<GoodType, PriceFunction>();
		priceFunctions.put(GoodType.COAL, new FixedPriceFunctionImpl(Double.NaN));
		priceFunctions.put(GoodType.KILOWATT, new FixedPriceFunctionImpl(1.0));
		priceFunctions.put(GoodType.WHEAT, new FixedPriceFunctionImpl(2.0));

		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputsAnalyticalFixedPrices = cobbDouglasFunction
				.calculateOutputMaximizingInputsAnalyticalWithFixedPrices(prices, budget);
		final Map<GoodType, Double> optimalInputsAnalyticalPriceFunctions = cobbDouglasFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cobbDouglasFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cobbDouglasFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputs = cobbDouglasFunction.calculateOutputMaximizingInputs(priceFunctions,
				budget);

		/*
		 * assert inputs
		 */
		assertEquals(4., optimalInputsAnalyticalFixedPrices.get(GoodType.KILOWATT), epsilon);
		assertEquals(3., optimalInputsAnalyticalFixedPrices.get(GoodType.WHEAT), epsilon);

		for (final GoodType goodType : optimalInputsAnalyticalFixedPrices.keySet()) {
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType),
					optimalInputsAnalyticalPriceFunctions.get(goodType), epsilon);
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType), optimalInputsIterative.get(goodType),
					epsilon);
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType), optimalInputsBruteForce.get(goodType),
					epsilon);
			assertEquals(optimalInputsAnalyticalFixedPrices.get(goodType), optimalInputs.get(goodType), epsilon);
		}

		/*
		 * assert output
		 */
		assertEquals(3.36586, cobbDouglasFunction.f(optimalInputsAnalyticalFixedPrices), epsilon);

		assertOutputIsOptimalUnderBudget(cobbDouglasFunction, budget, priceFunctions,
				optimalInputsAnalyticalFixedPrices);

		/*
		 * assert marginal outputs
		 */
		assertEquals(0.336586,
				cobbDouglasFunction.partialDerivative(optimalInputsAnalyticalFixedPrices, GoodType.KILOWATT), epsilon);
		assertEquals(0.673173,
				cobbDouglasFunction.partialDerivative(optimalInputsAnalyticalFixedPrices, GoodType.WHEAT), epsilon);

		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsAnalyticalFixedPrices,
				priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsAnalyticalPriceFunctions,
				priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsBruteForce, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputs, priceFunctions);
	}

	@Test
	public void testCalculateForTwoGoodsWithMarketPrices() {

		/*
		 * prepare market
		 */

		final Currency currency = Currency.EURO;

		final Household household1_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(0);
		final Household household2_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(1);

		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.KILOWATT),
				epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.WHEAT),
				epsilon);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 5.0, 2.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 5.0, 1.0);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 3.0, 2.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 2.0, 1.0);

		/*
		 * prepare function
		 */
		final Map<GoodType, Double> exponents = new HashMap<GoodType, Double>();
		exponents.put(GoodType.KILOWATT, 0.4);
		exponents.put(GoodType.WHEAT, 0.6);
		final CobbDouglasFunctionImpl<GoodType> cobbDouglasFunction = new CobbDouglasFunctionImpl<GoodType>(1.0,
				exponents);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, PriceFunction> priceFunctions = ApplicationContext.getInstance().getMarketService()
				.getMarketPriceFunctions(currency, new GoodType[] { GoodType.KILOWATT, GoodType.WHEAT });

		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputsAnalytical = cobbDouglasFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cobbDouglasFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cobbDouglasFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);

		/*
		 * assert inputs
		 */
		for (final GoodType goodType : optimalInputsAnalytical.keySet()) {
			assertEquals(optimalInputsAnalytical.get(goodType), optimalInputsIterative.get(goodType), epsilon);
			assertEquals(optimalInputsAnalytical.get(goodType), optimalInputsBruteForce.get(goodType), epsilon);
		}

		/*
		 * assert output
		 */

		/*
		 * assert marginal outputs
		 */
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsAnalytical, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsBruteForce, priceFunctions);
	}

	@Test
	public void testCalculateForTwoGoodsWithNaNMarketPrices() {

		/*
		 * prepare market
		 */

		final Currency currency = Currency.EURO;

		final Household household1_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(0);
		final Household household2_EUR = ApplicationContext.getInstance().getAgentService().findHouseholds(currency)
				.get(1);

		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.KILOWATT),
				epsilon);
		assertEquals(Double.NaN,
				ApplicationContext.getInstance().getMarketService().getMarginalMarketPrice(currency, GoodType.WHEAT),
				epsilon);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 20.0, 2.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 5.0, 1.0);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 20.0, 2.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 4.0, 1.0);

		/*
		 * prepare function
		 */
		final Map<GoodType, Double> exponents = new HashMap<GoodType, Double>();
		exponents.put(GoodType.KILOWATT, 0.4);
		exponents.put(GoodType.WHEAT, 0.6);
		final CobbDouglasFunctionImpl<GoodType> cobbDouglasFunction = new CobbDouglasFunctionImpl<GoodType>(1.0,
				exponents);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, PriceFunction> priceFunctions = ApplicationContext.getInstance().getMarketService()
				.getMarketPriceFunctions(currency, new GoodType[] { GoodType.KILOWATT, GoodType.WHEAT });

		// TODO: problematic with 6.7 < budget < 15.8 as
		// partialDerivativesPerPrice
		// are not equal -> analytical solution is not found
		final double budget = 21;

		final Map<GoodType, Double> optimalInputsAnalytical = cobbDouglasFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cobbDouglasFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cobbDouglasFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);

		/*
		 * assert inputs
		 */
		for (final GoodType goodType : optimalInputsAnalytical.keySet()) {
			assertEquals(optimalInputsAnalytical.get(goodType), optimalInputsIterative.get(goodType), epsilon);
			assertEquals(optimalInputsAnalytical.get(goodType), optimalInputsBruteForce.get(goodType), epsilon);
		}

		/*
		 * assert output
		 */

		/*
		 * assert marginal outputs
		 */
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsAnalytical, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cobbDouglasFunction, optimalInputsBruteForce, priceFunctions);
	}
}
