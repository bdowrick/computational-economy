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

package io.github.uwol.compecon.math.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import io.github.uwol.compecon.engine.service.impl.FixedPriceFunctionImpl;
import io.github.uwol.compecon.math.price.PriceFunction;
import io.github.uwol.compecon.math.price.PriceFunction.PriceFunctionConfig;
import io.github.uwol.compecon.math.util.MathUtil;

public class CESFunctionImpl<T> extends AnalyticalConvexFunctionImpl<T> {

	protected final Map<T, Double> coefficients;

	protected final double homogenityFactor;

	protected double mainCoefficient;

	protected final double substitutionFactor;

	public CESFunctionImpl(final double mainCoefficient, final Map<T, Double> coefficients,
			final double substitutionFactor, final double homogenityFactor) {
		super(false);

		// mainCoefficient has to be > 0
		assert (MathUtil.greater(mainCoefficient, 0.0));

		// interior exponent has to be in interval ]-1, 0[ for a convex
		// maximization problem
		assert (substitutionFactor > -1.0 && substitutionFactor < 0.0);

		// homogenityFactor has to be > 0
		assert (MathUtil.greater(homogenityFactor, 0.0));

		final double exteriorExponent = -homogenityFactor / substitutionFactor;

		// exterior exponent has to be in interval ]0, 1[ for a convex function
		assert (exteriorExponent > 0.0 && exteriorExponent < 1.0);

		this.mainCoefficient = mainCoefficient;
		this.coefficients = coefficients;
		this.substitutionFactor = substitutionFactor;
		this.homogenityFactor = homogenityFactor;
	}

	@Override
	public Map<T, Double> calculateOutputMaximizingInputs(final Map<T, PriceFunction> priceFunctionsOfInputs,
			final double budget) {
		// check whether the analytical solution is viable
		final Map<T, Double> fixedPrices = new HashMap<T, Double>();

		for (final Entry<T, PriceFunction> priceFunctionEntry : priceFunctionsOfInputs.entrySet()) {
			if (priceFunctionEntry.getValue() instanceof FixedPriceFunctionImpl) {
				fixedPrices.put(priceFunctionEntry.getKey(), priceFunctionEntry.getValue().getPrice(0.0));
			} else {
				break;
			}
		}

		// dispatch
		if (fixedPrices.size() == priceFunctionsOfInputs.size()) {
			return this.calculateOutputMaximizingInputsAnalyticalWithFixedPrices(fixedPrices, budget);
		} else {
			return super.calculateOutputMaximizingInputs(priceFunctionsOfInputs, budget);
		}
	}

	/**
	 * This method implements the analytical solution for the lagrange function of
	 * an optimization problem under budget constraints. It overwrites the general
	 * solution for convex functions for performance reasons. <br />
	 * <br />
	 * L(x_1, ..., x_n, l) = a * [c_1 * (x_1)^(-r) + ... + c_n * (x_n)^(r)]^(-h/r) +
	 * l(p_1 * x_1 + ... + p_n * x_n - b) <br />
	 * <br />
	 * => <br />
	 * dL(x_1, ..., x_n, l) / dx_1 = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n *
	 * (x_n)^(r)]^(-h/r - 1)] * [-r * c_1 * (x_1)^(-r-1)] + l * p_1 <br />
	 * dL(x_1, ..., x_n, l) / dx_2 = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n *
	 * (x_n)^(r)]^(-h/r - 1)] * [-r * c_2 * (x_2)^(-r-1)] + l * p_2 <br />
	 * dL(x_1, ..., x_n, l) / dl = p_1 * x_1 + ... + p_n * x_n - b <br />
	 * <br />
	 * <br />
	 * dL(x_1, ..., x_n, l) / dx_1 = 0 <br />
	 * dL(x_1, ..., x_n, l) / dx_2 = 0 <br />
	 * dL(x_1, ..., x_n, l) / dl = 0 <br />
	 * <br />
	 * => <br />
	 * l = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n * (x_n)^(r)]^(-h/r - 1)] * [-r
	 * * c_1 * (x_1)^(-r-1)] / p_1 <br />
	 * l = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n * (x_n)^(r)]^(-h/r - 1)] * [-r
	 * * c_2 * (x_2)^(-r-1)] / p_2 <br />
	 * p_1 * x_1 + ... + p_n * x_n = b <br />
	 * <br />
	 * => (1) = (2)<br />
	 * p_1 * c_2 * (x_2)^(-r-1) = p_2 * c_1 * (x_1)^(-r-1) <br />
	 * => <br />
	 * x_2 = [(p_1 * c_2) / (p_2 * c_1)]^(1/(r+1)) * x_1 <br />
	 * x_n = [(p_1 * c_n) / (p_n * c_1)]^(1/(r+1)) * x_1 <br />
	 * <br />
	 * b = p_1 * x_1 + p_2 * [(p_1 * c_2) / (p_2 * c_1)]^(1/(r+1)) * x_1 + ... + p_n
	 * * [(p_1 * c_n) / (p_n * c_1)]^(1/(r+1)) * x_1 <br />
	 * <br />
	 * x_1 = b / [p_1 + p_2 * [(p_1 * c_2) / (p_2 * c_1)]^(1/(r+1)) + ... + p_n
	 * [(p_1 * c_n) / (p_n * c_1)]^(1/(r+1))]
	 */
	public Map<T, Double> calculateOutputMaximizingInputsAnalyticalWithFixedPrices(final Map<T, Double> pricesOfInputs,
			final double budget) {
		final Map<T, Double> bundleOfInputs = new LinkedHashMap<T, Double>();

		/*
		 * analytical formula for the optimal solution of a CES function under given
		 * budget restriction -> lagrange function
		 */
		for (final T referenceInputType : this.getInputTypes()) {
			final double referenceInputTypePrice = pricesOfInputs.get(referenceInputType);
			double divisor = referenceInputTypePrice;

			for (final T inputType : this.getInputTypes()) {
				if (!referenceInputType.equals(inputType)) {
					final double currentInputTypePrice = pricesOfInputs.get(inputType);
					if (!Double.isNaN(referenceInputTypePrice) && !Double.isNaN(currentInputTypePrice)) {
						final double base = referenceInputTypePrice * this.coefficients.get(inputType)
								/ (currentInputTypePrice * this.coefficients.get(referenceInputType));
						final double exponent = 1.0 / (this.substitutionFactor + 1.0);
						divisor += currentInputTypePrice * Math.pow(base, exponent);
					}
				}
			}

			double optimalAmount = budget / divisor;

			if (Double.isNaN(optimalAmount)) {
				optimalAmount = 0.0;
			}

			bundleOfInputs.put(referenceInputType, optimalAmount);
		}

		return bundleOfInputs;
	}

	/**
	 * This method implements the analytical solution for the lagrange function of
	 * an optimization problem under budget constraints and a step price function.
	 * It overwrites the general solution for convex functions for performance
	 * reasons. <br />
	 * <br />
	 * L(x_1, ..., x_n, l) = a * [c_1 * (x_1)^(-r) + ... + c_n * (x_n)^(r)]^(-h/r) +
	 * l(p_1(x_1) * x_1 + ... + p_n(x_n) * x_n - b) <br />
	 * <br />
	 * p_1(x_1) = c_x0_(x_1) + c_xMinus1_(x_1) / x_1 <br />
	 * ... <br />
	 * p_n(x_n) = c_x0_(x_n) + c_xMinus1_(x_n) / x_n <br />
	 * <br />
	 * => <br />
	 * L(x_1, ..., x_n, l) = a * [c_1 * (x_1)^(-r) + ... + c_n * (x_n)^(r)]^(-h/r) +
	 * l((c_x0_(x_1) + c_xMinus1_(x_1) / x_1) * x_1 + ... + (c_x0_(x_n) +
	 * c_xMinus1_(x_n) / x_n) * x_n - b) <br />
	 * => <br />
	 * L(x_1, ..., x_n, l) = a * [c_1 * (x_1)^(-r) + ... + c_n * (x_n)^(r)]^(-h/r) +
	 * l((c_x0_(x_1) * x_1 + c_xMinus1_(x_1) + ... + c_x0_(x_n) * x_n +
	 * c_xMinus1_(x_n) - b) <br />
	 * <br />
	 * => <br />
	 * <br />
	 * dL(x_1, ..., x_n, l) / dx_1 = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n *
	 * (x_n)^(r)]^(-h/r - 1)] * [-r * c_1 * (x_1)^(-r-1)] + l * c_x0_(x_1) <br />
	 * dL(x_1, ..., x_n, l) / dx_2 = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n *
	 * (x_n)^(r)]^(-h/r - 1)] * [-r * c_2 * (x_2)^(-r-1)] + l * c_x0_(x_2) <br />
	 * dL(x_1, ..., x_n, l) / dl = c_x0_(x_1) * x_1 + c_xMinus1_(x_1) + ... +
	 * c_x0_(x_n) * x_n + c_xMinus1_(x_n) - b <br />
	 * <br />
	 * <br />
	 * dL(x_1, ..., x_n, l) / dx_1 = 0 <br />
	 * dL(x_1, ..., x_n, l) / dx_2 = 0 <br />
	 * dL(x_1, ..., x_n, l) / dl = 0 <br />
	 * <br />
	 * => <br />
	 * l = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n * (x_n)^(r)]^(-h/r - 1)] * [-r
	 * * c_1 * (x_1)^(-r-1)] / c_x0_(x_1) <br />
	 * l = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n * (x_n)^(r)]^(-h/r - 1)] * [-r
	 * * c_2 * (x_2)^(-r-1)] / c_x0_(x_2) <br />
	 * c_x0_(x_1) * x_1 + c_xMinus1_(x_1) + ... + c_x0_(x_n) * x_n + c_xMinus1_(x_n)
	 * = b <br />
	 * <br />
	 * => (1) = (2)<br />
	 * c_x0_(x_1) * c_2 * (x_2)^(-r-1) = c_x0_(x_2) * c_1 * (x_1)^(-r-1) <br />
	 * => <br />
	 * x_2 = [(c_x0_(x_1) * c_2) / (c_x0_(x_2) * c_1)]^(1/(r+1)) * x_1 <br />
	 * x_n = [(c_x0_(x_1) * c_n) / (c_x0_(x_n) * c_1)]^(1/(r+1)) * x_1 <br />
	 * <br />
	 * b = (c_x0_(x_1) + c_xMinus1_(x_1) / x_1) * x_1 + ... + (c_x0_(x_n) +
	 * c_xMinus1_(x_n) / x_n) * x_n <br />
	 * => <br />
	 * b = c_x0_(x_1) * x_1 + c_xMinus1_(x_1) + ... + c_x0_(x_n) * x_n +
	 * c_xMinus1_(x_n) <br />
	 * => <br />
	 * b = c_xMinus1_(x_1) + c_xMinus1_(x_2) + ... + c_xMinus1_(x_n) + c_x0_(x_1) *
	 * x_1 + c_x0_(x_2) * [(c_x0_(x_1) * c_2) / (c_x0_(x_2) * c_1)]^(1/(r+1)) * x_1
	 * + ... + c_x0_(x_n) * [(c_x0_(x_1) * c_n) / (c_x0_(x_n) * c_1)]^(1/(r+1)) *
	 * x_1 <br />
	 * <br />
	 * x_1 = [b - c_xMinus1_(x_1) - c_xMinus1_(x_2) - ... - c_xMinus1_(x_n)] /
	 * [c_x0_(x_1) + c_x0_(x_2) * [(c_x0_(x_1) * c_2) / (c_x0_(x_2) *
	 * c_1)]^(1/(r+1)) + ... + c_x0_(x_n) [(c_x0_(x_1) * c_n) / (c_x0_(x_n) *
	 * c_1)]^(1/(r+1))]
	 */
	@Override
	protected Map<T, Double> calculatePossiblyValidOutputMaximizingInputsAnalyticalWithMarketPrices(
			final Map<T, PriceFunctionConfig> priceFunctionConfigs, final double budget) {
		final Map<T, Double> bundleOfInputs = new LinkedHashMap<T, Double>();

		/*
		 * analytical formula for the optimal solution of a CES function under given
		 * budget restriction -> lagrange function
		 */
		double sumOfCoefficientXPowerMinus1 = 0.0;

		for (final PriceFunctionConfig priceFunctionConfig : priceFunctionConfigs.values()) {
			sumOfCoefficientXPowerMinus1 += priceFunctionConfig.coefficientXPowerMinus1;
		}

		for (final T referenceInputType : this.getInputTypes()) {
			final double referenceCoefficientXPower0 = priceFunctionConfigs.get(referenceInputType).coefficientXPower0;
			double divisor = referenceCoefficientXPower0;

			for (final T inputType : this.getInputTypes()) {
				if (!referenceInputType.equals(inputType)) {
					final double currentCoefficientXPower0 = priceFunctionConfigs.get(inputType).coefficientXPower0;
					if (!Double.isNaN(referenceCoefficientXPower0) && !Double.isNaN(currentCoefficientXPower0)) {
						final double base = referenceCoefficientXPower0 * this.coefficients.get(inputType)
								/ (currentCoefficientXPower0 * this.coefficients.get(referenceInputType));
						final double exponent = 1.0 / (this.substitutionFactor + 1.0);
						divisor += currentCoefficientXPower0 * Math.pow(base, exponent);
					}
				}
			}

			double optimalAmount = (budget - sumOfCoefficientXPowerMinus1) / divisor;
			if (Double.isNaN(optimalAmount)) {
				optimalAmount = 0.0;
			}

			bundleOfInputs.put(referenceInputType, optimalAmount);
		}

		return bundleOfInputs;
	}

	/**
	 * y = a * [c_1 * (x_1)^(-r) + ... + c_n * (x_n)^(r)]^(-h/r)
	 */
	@Override
	public double f(final Map<T, Double> bundleOfInputs) {
		double sum = 0.0;

		for (final Entry<T, Double> entry : this.coefficients.entrySet()) {
			final T inputType = entry.getKey();
			final double coefficient = entry.getValue();
			final double input = bundleOfInputs.get(inputType);
			sum += coefficient * Math.pow(input, -this.substitutionFactor);
		}

		return this.mainCoefficient * Math.pow(sum, (-1.0 * this.homogenityFactor) / this.substitutionFactor);
	}

	@Override
	public Set<T> getInputTypes() {
		return this.coefficients.keySet();
	}

	public double getMainCoefficient() {
		return this.mainCoefficient;
	}

	/**
	 * dy/d(x_1) = [(-h/r) * a * [c_1 * (x_1)^(r) + ... + c_n * (x_n)^(r)]^(-h/r -
	 * 1)] * [-r * c_1 * (x_1)^(-r-1)]
	 */
	@Override
	public double partialDerivative(final Map<T, Double> forBundleOfInputs, final T withRespectToInputType) {
		/*
		 * exterior derivative
		 */
		double sum = 0.0;

		for (final Entry<T, Double> entry : this.coefficients.entrySet()) {
			final T inputType = entry.getKey();
			final double coefficient = entry.getValue();
			final double input = forBundleOfInputs.get(inputType);
			sum += coefficient * Math.pow(input, -1.0 * this.substitutionFactor);
		}

		final double exponent = (-1.0 * this.homogenityFactor / this.substitutionFactor) - 1.0;
		final double exteriorDerivative = (-1.0 * this.homogenityFactor / this.substitutionFactor)
				* this.mainCoefficient * Math.pow(sum, exponent);

		/*
		 * interior derivative
		 */
		final double coefficient = this.coefficients.get(withRespectToInputType);
		final double differentialInput = forBundleOfInputs.get(withRespectToInputType);
		final double interiorDerivative = -1.0 * this.substitutionFactor * coefficient
				* Math.pow(differentialInput, (-1.0 * this.substitutionFactor) - 1.0);

		// Java returns Double.NaN for 0 * Double.INFINITE -> return 0
		if (exteriorDerivative == 0.0 && Double.isInfinite(interiorDerivative)) {
			return 0.0;
		}

		if (interiorDerivative == 0.0 && Double.isInfinite(exteriorDerivative)) {
			return 0.0;
		}

		return exteriorDerivative * interiorDerivative;
	}

	public void setMainCoefficient(final double mainCoefficient) {
		this.mainCoefficient = mainCoefficient;
	}
}
