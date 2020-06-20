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

package io.github.uwol.compecon.engine.dao.inmemory.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import io.github.uwol.compecon.economy.sectors.financial.Bank;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount;
import io.github.uwol.compecon.economy.sectors.financial.BankCustomer;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.engine.dao.BankAccountDAO;

public class BankAccountDAOImpl extends AbstractIndexedInMemoryDAOImpl<BankCustomer, BankAccount>
		implements BankAccountDAO {

	protected Map<Bank, List<BankAccount>> bankAccounts = new HashMap<Bank, List<BankAccount>>();

	/*
	 * helpers
	 */

	private void assureInitializedDataStructure(final Bank bank) {
		if (!bankAccounts.containsKey(bank)) {
			bankAccounts.put(bank, new ArrayList<BankAccount>());
		}
	}

	/*
	 * actions
	 */

	@Override
	public synchronized void delete(final BankAccount bankAccount) {
		final List<BankAccount> bankAccountsOfBank = bankAccounts.get(bankAccount.getManagingBank());
		if (bankAccountsOfBank != null) {
			bankAccountsOfBank.remove(bankAccount);
		}

		super.delete(bankAccount);
	}

	@Override
	public synchronized void deleteAllBankAccounts(final Bank managingBank) {
		final List<BankAccount> bankAccountsOfBank = bankAccounts.get(managingBank);
		if (bankAccountsOfBank != null) {
			for (final BankAccount bankAccount : new HashSet<BankAccount>(bankAccountsOfBank)) {
				delete(bankAccount);
			}
		}
		bankAccounts.remove(managingBank);
	}

	@Override
	public synchronized void deleteAllBankAccounts(final Bank managingBank, final BankCustomer owner) {
		final List<BankAccount> bankAccountsOfOwner = getInstancesForKey(owner);
		if (bankAccountsOfOwner != null) {
			for (final BankAccount bankAccount : new HashSet<BankAccount>(bankAccountsOfOwner)) {
				if (bankAccount.getManagingBank() == managingBank) {
					delete(bankAccount);
				}
			}
		}
	}

	@Override
	public synchronized List<BankAccount> findAll(final Bank managingBank, final BankCustomer owner) {
		final List<BankAccount> bankAccounts = new ArrayList<BankAccount>();
		for (final BankAccount bankAccount : findAllBankAccountsOfAgent(owner)) {
			if (bankAccount.getManagingBank() == managingBank) {
				bankAccounts.add(bankAccount);
			}
		}
		return bankAccounts;
	}

	@Override
	public synchronized List<BankAccount> findAll(final Bank managingBank, final BankCustomer owner,
			final Currency currency) {
		final List<BankAccount> bankAccounts = new ArrayList<BankAccount>();
		for (final BankAccount bankAccount : findAllBankAccountsOfAgent(owner)) {
			if (bankAccount.getManagingBank() == managingBank && currency.equals(bankAccount.getCurrency())) {
				bankAccounts.add(bankAccount);
			}
		}
		return bankAccounts;
	}

	@Override
	public synchronized List<BankAccount> findAllBankAccountsManagedByBank(final Bank managingBank) {
		assureInitializedDataStructure(managingBank);

		final List<BankAccount> bankAccountManagedByBank = bankAccounts.get(managingBank);
		return new ArrayList<BankAccount>(bankAccountManagedByBank);
	}

	@Override
	public synchronized List<BankAccount> findAllBankAccountsOfAgent(final BankCustomer owner) {
		final List<BankAccount> bankAccounts = getInstancesForKey(owner);
		if (bankAccounts != null) {
			return new ArrayList<BankAccount>(bankAccounts);
		}
		return new ArrayList<BankAccount>();
	}

	@Override
	public synchronized void save(final BankAccount bankAccount) {
		assureInitializedDataStructure(bankAccount.getManagingBank());

		bankAccounts.get(bankAccount.getManagingBank()).add(bankAccount);
		super.save(bankAccount.getOwner(), bankAccount);
	}
}
