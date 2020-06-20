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

import io.github.uwol.compecon.economy.sectors.financial.Bank;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount;
import io.github.uwol.compecon.economy.sectors.financial.BankCustomer;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.engine.dao.BankAccountDAO;

public class BankAccountDAOImpl extends HibernateDAOImpl<BankAccount> implements BankAccountDAO {

	@Override
	public void deleteAllBankAccounts(final Bank managingBank) {
		final List<BankAccount> bankAccounts = findAllBankAccountsManagedByBank(managingBank);
		for (final BankAccount bankAccount : bankAccounts) {
			delete(bankAccount);
		}
	}

	@Override
	public void deleteAllBankAccounts(final Bank managingBank, final BankCustomer owner) {
		final List<BankAccount> bankAccounts = this.findAll(managingBank, owner);
		for (final BankAccount bankAccount : bankAccounts) {
			delete(bankAccount);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BankAccount> findAll(final Bank managingBank, final BankCustomer owner) {
		final String hql = "FROM BankAccountImpl ba WHERE ba.managingBank = :managingBank AND ba.owner = :owner";
		return getSession().createQuery(hql).setEntity("managingBank", managingBank).setEntity("owner", owner).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BankAccount> findAll(final Bank managingBank, final BankCustomer owner, final Currency currency) {
		final String hql = "FROM BankAccountImpl ba WHERE ba.managingBank = :managingBank AND ba.owner = :owner AND ba.currency = :currency";
		return getSession().createQuery(hql).setEntity("managingBank", managingBank).setEntity("owner", owner)
				.setParameter("currency", currency).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BankAccount> findAllBankAccountsManagedByBank(final Bank managingBank) {
		final String hql = "FROM BankAccountImpl ba WHERE ba.managingBank = :managingBank";
		return getSession().createQuery(hql).setEntity("managingBank", managingBank).list();
	}

	@SuppressWarnings("unchecked")
	@Override
	public List<BankAccount> findAllBankAccountsOfAgent(final BankCustomer owner) {
		final String hql = "FROM BankAccountImpl ba WHERE ba.owner = :owner";
		return getSession().createQuery(hql).setEntity("owner", owner).list();
	}
}
