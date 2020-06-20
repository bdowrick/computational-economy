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

package io.github.uwol.compecon.engine.dao.hibernate.impl;

import java.util.List;

import org.hibernate.criterion.Restrictions;

import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.economy.sectors.trading.Trader;
import io.github.uwol.compecon.economy.sectors.trading.impl.TraderImpl;
import io.github.uwol.compecon.engine.dao.TraderDAO;

public class TraderDAOImpl extends HibernateDAOImpl<Trader> implements TraderDAO {

	@SuppressWarnings("unchecked")
	@Override
	public List<Trader> findAllByCurrency(final Currency currency) {
		return getSession().createCriteria(TraderImpl.class).add(Restrictions.eq("primaryCurrency", currency)).list();
	}
}
