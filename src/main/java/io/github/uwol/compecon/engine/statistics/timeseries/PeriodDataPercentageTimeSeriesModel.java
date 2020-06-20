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

package io.github.uwol.compecon.engine.statistics.timeseries;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.jfree.data.time.Day;

import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.engine.statistics.accumulator.PeriodDataAccumulator;

public class PeriodDataPercentageTimeSeriesModel<I> extends AbstractPeriodDataMultipleTimeSeriesModel<I> {

	protected final Map<I, PeriodDataAccumulator> periodDataAccumulator = new HashMap<I, PeriodDataAccumulator>();

	public PeriodDataPercentageTimeSeriesModel(final I[] initialIndexTypes, final String title) {
		super(initialIndexTypes, title);

		for (final I indexType : initialIndexTypes) {
			this.periodDataAccumulator.put(indexType, new PeriodDataAccumulator());
		}
	}

	public void add(final I indexType, final double amount) {
		this.periodDataAccumulator.get(indexType).add(amount);
	}

	public double getValue(final I indexType) {
		return this.periodDataAccumulator.get(indexType).getAmount();
	}

	@Override
	public void nextPeriod() {
		double sum = 0;
		for (final PeriodDataAccumulator periodDataAccumulator : this.periodDataAccumulator.values()) {
			sum += periodDataAccumulator.getAmount();
		}

		for (final Entry<I, PeriodDataAccumulator> entry : this.periodDataAccumulator.entrySet()) {
			// write into time series
			timeSeries.get(entry.getKey()).addOrUpdate(
					new Day(ApplicationContext.getInstance().getTimeSystem().getCurrentDate()),
					entry.getValue().getAmount() / sum);
			entry.getValue();
		}
	}
}
