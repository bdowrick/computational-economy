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

package io.github.uwol.compecon.math.utility;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import io.github.uwol.compecon.CompEconTestSupport;
import io.github.uwol.compecon.economy.materia.GoodType;
import io.github.uwol.compecon.engine.service.impl.FixedPriceFunctionImpl;
import io.github.uwol.compecon.math.price.PriceFunction;
import io.github.uwol.compecon.math.utility.impl.CobbDouglasUtilityFunctionImpl;

public class CobbDouglasUtilityFunctionTest extends CompEconTestSupport {

	@Test
	public void testCalculateUtilityWithFixedPrices() {
		/*
		 * prepare function
		 */
		final Map<GoodType, Double> preferences = new HashMap<GoodType, Double>();
		preferences.put(GoodType.KILOWATT, 0.4);
		preferences.put(GoodType.WHEAT, 0.6);
		final CobbDouglasUtilityFunctionImpl cobbDouglasUtilityFunction = new CobbDouglasUtilityFunctionImpl(1.0,
				preferences);

		/*
		 * maximize output under budget restriction
		 */
		final Map<GoodType, PriceFunction> prices = new HashMap<GoodType, PriceFunction>();
		prices.put(GoodType.KILOWATT, new FixedPriceFunctionImpl(1.0));
		prices.put(GoodType.WHEAT, new FixedPriceFunctionImpl(2.0));
		final double budget = 10.0;

		final Map<GoodType, Double> optimalInputs = cobbDouglasUtilityFunction.calculateUtilityMaximizingInputs(prices,
				budget);
		assertEquals(4.0, optimalInputs.get(GoodType.KILOWATT), epsilon);
		assertEquals(3.0, optimalInputs.get(GoodType.WHEAT), epsilon);

		/*
		 * assert output
		 */
		assertEquals(3.36586, cobbDouglasUtilityFunction.calculateUtility(optimalInputs), epsilon);

		/*
		 * assert marginal outputs
		 */
		assertEquals(0.336586, cobbDouglasUtilityFunction.calculateMarginalUtility(optimalInputs, GoodType.KILOWATT),
				epsilon);
		assertEquals(0.673173, cobbDouglasUtilityFunction.calculateMarginalUtility(optimalInputs, GoodType.WHEAT),
				epsilon);
	}
}
