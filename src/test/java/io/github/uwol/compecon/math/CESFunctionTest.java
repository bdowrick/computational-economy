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
import io.github.uwol.compecon.math.impl.CESFunctionImpl;
import io.github.uwol.compecon.math.price.PriceFunction;

public class CESFunctionTest extends CompEconTestSupport {

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
	public void testCalculateForThreeGoodsWithFixedPrices() {
		/*
		 * prepare function
		 */
		final Map<GoodType, Double> coefficients = new HashMap<GoodType, Double>();
		coefficients.put(GoodType.KILOWATT, 0.1);
		coefficients.put(GoodType.COTTON, 0.2);
		coefficients.put(GoodType.WHEAT, 0.7);
		final CESFunctionImpl<GoodType> cesFunction = new CESFunctionImpl<GoodType>(1.0, coefficients, -0.5, 0.4);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, Double> prices = new HashMap<GoodType, Double>();
		prices.put(GoodType.COAL, Double.NaN);
		prices.put(GoodType.KILOWATT, 1.0);
		prices.put(GoodType.COTTON, 3.0);
		prices.put(GoodType.WHEAT, 2.0);

		final Map<GoodType, PriceFunction> priceFunctions = new HashMap<GoodType, PriceFunction>();
		priceFunctions.put(GoodType.COAL, new FixedPriceFunctionImpl(Double.NaN));
		priceFunctions.put(GoodType.KILOWATT, new FixedPriceFunctionImpl(1.0));
		priceFunctions.put(GoodType.COTTON, new FixedPriceFunctionImpl(3.0));
		priceFunctions.put(GoodType.WHEAT, new FixedPriceFunctionImpl(2.0));

		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputsAnalyticalFixedPrices = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithFixedPrices(prices, budget);
		final Map<GoodType, Double> optimalInputsAnalyticalPriceFunctions = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cesFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		// takes some seconds for completion due to large solution space
		final Map<GoodType, Double> optimalInputsBruteForce = cesFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputs = cesFunction.calculateOutputMaximizingInputs(priceFunctions, budget);

		/*
		 * assert inputs
		 */
		assertEquals(0.373, optimalInputsAnalyticalFixedPrices.get(GoodType.KILOWATT), epsilon);
		assertEquals(4.565, optimalInputsAnalyticalFixedPrices.get(GoodType.WHEAT), epsilon);
		assertEquals(0.166, optimalInputsAnalyticalFixedPrices.get(GoodType.COTTON), epsilon);

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
		assertOutputIsOptimalUnderBudget(cesFunction, budget, priceFunctions, optimalInputsAnalyticalFixedPrices);

		/*
		 * assert marginal outputs
		 */
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalyticalFixedPrices, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalyticalPriceFunctions, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsBruteForce, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputs, priceFunctions);
	}

	@Test
	public void testCalculateForThreeGoodsWithNaNFixedPrices() {
		/*
		 * prepare function
		 */
		final Map<GoodType, Double> coefficients = new HashMap<GoodType, Double>();
		coefficients.put(GoodType.COAL, 0.1);
		coefficients.put(GoodType.KILOWATT, 0.3);
		coefficients.put(GoodType.WHEAT, 0.6);
		final CESFunctionImpl<GoodType> cesFunction = new CESFunctionImpl<GoodType>(1.0, coefficients, -0.5, 0.4);

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

		final Map<GoodType, Double> optimalInputsAnalyticalFixedPrices = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithFixedPrices(prices, budget);
		final Map<GoodType, Double> optimalInputsAnalyticalPriceFunctions = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cesFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cesFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputs = cesFunction.calculateOutputMaximizingInputs(priceFunctions, budget);

		/*
		 * assert inputs
		 */
		assertEquals(0.0, optimalInputsAnalyticalFixedPrices.get(GoodType.COAL), epsilon);
		assertEquals(3.333, optimalInputsAnalyticalFixedPrices.get(GoodType.KILOWATT), epsilon);
		assertEquals(3.333, optimalInputsAnalyticalFixedPrices.get(GoodType.WHEAT), epsilon);

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
		assertOutputIsOptimalUnderBudget(cesFunction, budget, priceFunctions, optimalInputsAnalyticalFixedPrices);

		/*
		 * assert marginal outputs
		 */
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalyticalFixedPrices, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalyticalPriceFunctions, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsBruteForce, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputs, priceFunctions);
	}

	@Test
	public void testCalculateForTwoGoodsWithFixedPrices() {
		/*
		 * prepare function
		 */
		final Map<GoodType, Double> coefficients = new HashMap<GoodType, Double>();
		coefficients.put(GoodType.KILOWATT, 0.4);
		coefficients.put(GoodType.WHEAT, 0.6);
		final CESFunctionImpl<GoodType> cesFunction = new CESFunctionImpl<GoodType>(1.0, coefficients, -0.5, 0.4);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, Double> prices = new HashMap<GoodType, Double>();
		prices.put(GoodType.COAL, Double.NaN);
		prices.put(GoodType.KILOWATT, 1.0);
		prices.put(GoodType.WHEAT, 1.0);

		final Map<GoodType, PriceFunction> priceFunctions = new HashMap<GoodType, PriceFunction>();
		priceFunctions.put(GoodType.COAL, new FixedPriceFunctionImpl(Double.NaN));
		priceFunctions.put(GoodType.KILOWATT, new FixedPriceFunctionImpl(1.0));
		priceFunctions.put(GoodType.WHEAT, new FixedPriceFunctionImpl(1.0));

		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputsAnalyticalFixedPrices = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithFixedPrices(prices, budget);
		final Map<GoodType, Double> optimalInputsAnalyticalPriceFunctions = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cesFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cesFunction
				.calculateOutputMaximizingInputsByRangeScan(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputs = cesFunction.calculateOutputMaximizingInputs(priceFunctions, budget);

		/*
		 * assert inputs
		 */
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
		assertOutputIsOptimalUnderBudget(cesFunction, budget, priceFunctions, optimalInputsAnalyticalFixedPrices);

		/*
		 * assert marginal outputs
		 */
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalyticalFixedPrices, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalyticalPriceFunctions, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsBruteForce, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputs, priceFunctions);
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
				household1_EUR.getBankAccountTransactionsDelegate(), 2.0, 1.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.KILOWATT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 5.0, 2.0);

		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household1_EUR,
				household1_EUR.getBankAccountTransactionsDelegate(), 2.0, 1.0);
		ApplicationContext.getInstance().getMarketService().placeSellingOffer(GoodType.WHEAT, household2_EUR,
				household2_EUR.getBankAccountTransactionsDelegate(), 3.0, 2.0);

		/*
		 * prepare function
		 */
		final Map<GoodType, Double> coefficients = new HashMap<GoodType, Double>();
		coefficients.put(GoodType.KILOWATT, 0.4);
		coefficients.put(GoodType.WHEAT, 0.6);
		final CESFunctionImpl<GoodType> cesFunction = new CESFunctionImpl<GoodType>(1.0, coefficients, -0.5, 0.4);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, PriceFunction> priceFunctions = ApplicationContext.getInstance().getMarketService()
				.getMarketPriceFunctions(currency, new GoodType[] { GoodType.KILOWATT, GoodType.WHEAT });

		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputsAnalytical = cesFunction
				.calculateOutputMaximizingInputsAnalyticalWithPriceFunctions(priceFunctions, budget);
		final Map<GoodType, Double> optimalInputsIterative = cesFunction
				.calculateOutputMaximizingInputsIterative(priceFunctions, budget, numberOfIterations);
		final Map<GoodType, Double> optimalInputsBruteForce = cesFunction
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
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsAnalytical, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsIterative, priceFunctions);
		assertPartialDerivativesPerPriceAreEqual(cesFunction, optimalInputsBruteForce, priceFunctions);
	}
}
