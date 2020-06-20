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

package io.github.uwol.compecon.engine.factory;

import io.github.uwol.compecon.economy.sectors.financial.Bank;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount.MoneyType;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount.TermType;
import io.github.uwol.compecon.economy.sectors.financial.BankCustomer;
import io.github.uwol.compecon.economy.sectors.financial.Currency;

public interface BankAccountFactory {

	public void deleteAllBankAccounts(final Bank managingBank);

	public void deleteAllBankAccounts(final Bank managingBank, final BankCustomer customer);

	public void deleteBankAccount(final BankAccount bankAccount);

	public BankAccount newInstanceBankAccount(final BankCustomer owner, final Currency currency,
			final boolean overdraftPossible, final Bank managingBank, final String name, final TermType termType,
			final MoneyType moneyType);
}
