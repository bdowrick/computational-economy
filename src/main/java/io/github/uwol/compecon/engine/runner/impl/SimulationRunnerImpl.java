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

package io.github.uwol.compecon.engine.runner.impl;

import java.util.Date;

import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.engine.runner.SimulationRunner;
import io.github.uwol.compecon.engine.timesystem.impl.HourType;

public class SimulationRunnerImpl implements SimulationRunner {

	protected boolean killFlag = false;

	protected int millisecondsToSleepPerHourType = 0;

	protected boolean paused = false;

	protected boolean singleDayStep = false;

	protected boolean singleHourStep = false;

	@Override
	public void run() {
		run(null);
	}

	@Override
	public void run(final Date endDate) {
		try {
			// start simulation
			while (true) {
				// explicit end of simulation
				if (killFlag) {
					break;
				}
				// end date reached
				else if (endDate != null
						&& ApplicationContext.getInstance().getTimeSystem().getCurrentDate().after(endDate)) {
					break;
				}
				// normal mode
				else if (!paused) {
					// step hour-wise; triggers events in time system
					ApplicationContext.getInstance().getTimeSystem().nextHour();
					Thread.sleep(millisecondsToSleepPerHourType);
				}
				// paused mode, only proceeding with singleDayStep interaction
				// by user
				else if (paused && singleDayStep) {
					ApplicationContext.getInstance().getTimeSystem().nextHour();
					if (HourType.HOUR_00
							.equals(ApplicationContext.getInstance().getTimeSystem().getCurrentHourType())) {
						singleDayStep = false;
					}
				}
				// paused mode, only proceeding with singleHourStep interaction
				// by user
				else if (paused && singleHourStep) {
					singleHourStep = false;
					ApplicationContext.getInstance().getTimeSystem().nextHour();
				}
				// wait until next iteration
				else {
					Thread.sleep(50);
				}
			}
		} catch (final InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void setMillisecondsToSleepPerHourType(final int millisecondsToSleepPerHourType) {
		this.millisecondsToSleepPerHourType = millisecondsToSleepPerHourType;
	}

	@Override
	public void setPaused(final boolean paused) {
		this.paused = paused;
	}

	@Override
	public void stepSingleDay() {
		singleDayStep = true;
	}

	@Override
	public void stepSingleHour() {
		singleHourStep = true;
	}

	@Override
	public void stop() {
		killFlag = true;
	}
}
