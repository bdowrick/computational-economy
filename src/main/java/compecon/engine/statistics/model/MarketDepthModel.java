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

package compecon.engine.statistics.model;

import java.util.Iterator;

import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import compecon.economy.markets.ordertypes.MarketOrder;
import compecon.economy.sectors.financial.Currency;
import compecon.engine.dao.DAOFactory;
import compecon.materia.GoodType;

/**
 * Market depth model uses the compecon notification listener approach instead
 * of Jfreechart dataset-listeners, because this enables redrawing only a subset
 * of all market deptch diagrams.
 */
public class MarketDepthModel extends NotificationListenerModel {

	public XYDataset getMarketDepthDataset(Currency currency, GoodType goodType) {
		XYSeries series = new XYSeries(goodType + " ask");

		Iterator<MarketOrder> iterator = DAOFactory.getMarketOrderDAO()
				.getIteratorThreadsafe(currency, goodType);
		double volume = 0.0;
		while (iterator.hasNext()) {
			MarketOrder marketOrder = iterator.next();
			volume += marketOrder.getAmount();
			// volume available at that price per unit or less
			series.add(marketOrder.getPricePerUnit(), volume);
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.removeAllSeries();
		dataset.addSeries(series);
		return dataset;
	}

	public XYDataset getMarketDepthDataset(Currency currency,
			Currency commodityCurrency) {
		XYSeries series = new XYSeries(commodityCurrency.getIso4217Code()
				+ " ask");

		Iterator<MarketOrder> iterator = DAOFactory.getMarketOrderDAO()
				.getIteratorThreadsafe(currency, commodityCurrency);
		double volume = 0.0;
		while (iterator.hasNext()) {
			MarketOrder marketOrder = iterator.next();
			volume += marketOrder.getAmount();
			// volume available at that price per unit or less
			series.add(marketOrder.getPricePerUnit(), volume);
		}

		XYSeriesCollection dataset = new XYSeriesCollection();
		dataset.removeAllSeries();
		dataset.addSeries(series);
		return dataset;
	}

	public void nextPeriod() {
		this.notifyListeners();
	}
}
