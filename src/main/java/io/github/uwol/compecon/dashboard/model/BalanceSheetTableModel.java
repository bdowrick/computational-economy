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

package io.github.uwol.compecon.dashboard.model;

import javax.swing.table.AbstractTableModel;

import io.github.uwol.compecon.economy.bookkeeping.impl.BalanceSheetDTO;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount.MoneyType;
import io.github.uwol.compecon.economy.sectors.financial.BankAccount.TermType;
import io.github.uwol.compecon.economy.sectors.financial.Currency;
import io.github.uwol.compecon.engine.applicationcontext.ApplicationContext;
import io.github.uwol.compecon.engine.statistics.NotificationListenerModel.ModelListener;

public abstract class BalanceSheetTableModel extends AbstractTableModel implements ModelListener {

	private static final long serialVersionUID = 1L;

	protected BalanceSheetDTO balanceSheet;

	protected final String columnNames[] = { "Active Account", "Value", "Passive Account", "Value" };

	public BalanceSheetTableModel(final Currency currency) {
		ApplicationContext.getInstance().getModelRegistry().getNationalEconomyModel(currency).balanceSheetsModel
				.registerListener(this);
	}

	protected abstract BalanceSheetDTO getBalanceSheet();

	@Override
	public int getColumnCount() {
		return columnNames.length;
	}

	@Override
	public String getColumnName(final int columnIndex) {
		return columnNames[columnIndex];
	}

	@Override
	public int getRowCount() {
		return 11;
	}

	@Override
	public Object getValueAt(final int rowIndex, final int columnIndex) {

		// active accounts
		if (columnIndex == 0) {
			switch (rowIndex) {
			case 0:
				return "Hard Cash";

			case 1:
				return "Cash " + MoneyType.DEPOSITS + " " + TermType.SHORT_TERM;
			case 2:
				return "Cash " + MoneyType.DEPOSITS + " " + TermType.LONG_TERM;
			case 3:
				return "Cash " + MoneyType.CENTRALBANK_MONEY + " " + TermType.SHORT_TERM;
			case 4:
				return "Cash " + MoneyType.CENTRALBANK_MONEY + " " + TermType.LONG_TERM;

			case 5:
				return "Cash Foreign Currency";
			case 6:
				return "Fin. Assets (Bonds)";
			case 7:
				return "Bank Loans";
			case 8:
				return "Inventory";
			case 9:
				return "--------";
			case 10:
				return "Balance";
			default:
				return null;
			}
		}
		// active values
		else if (columnIndex == 1) {
			switch (rowIndex) {
			case 0:
				return Currency.formatMoneySum(balanceSheet.hardCash);

			case 1:
				return Currency.formatMoneySum(balanceSheet.cashGiroShortTerm);
			case 2:
				return Currency.formatMoneySum(balanceSheet.cashGiroLongTerm);
			case 3:
				return Currency.formatMoneySum(balanceSheet.cashCentralBankShortTerm);
			case 4:
				return Currency.formatMoneySum(balanceSheet.cashCentralBankLongTerm);

			case 5:
				return Currency.formatMoneySum(balanceSheet.cashForeignCurrency);
			case 6:
				return Currency.formatMoneySum(balanceSheet.bonds);
			case 7:
				return Currency.formatMoneySum(balanceSheet.bankLoans);
			case 8:
				return Currency.formatMoneySum(balanceSheet.inventoryValue);
			case 10:
				return Currency.formatMoneySum(balanceSheet.getBalanceActive());
			default:
				return null;
			}
		}
		// passive accounts
		else if (columnIndex == 2) {
			switch (rowIndex) {
			case 1:
				return "Loans " + MoneyType.DEPOSITS + " " + TermType.SHORT_TERM;
			case 2:
				return "Loans " + MoneyType.DEPOSITS + " " + TermType.LONG_TERM;
			case 3:
				return "Loans " + MoneyType.CENTRALBANK_MONEY + " " + TermType.SHORT_TERM;
			case 4:
				return "Loans " + MoneyType.CENTRALBANK_MONEY + " " + TermType.LONG_TERM;

			case 6:
				return "Fin. Liabilities (Bonds)";
			case 7:
				return "Bank Borrowings";
			case 8:
				return "Equity";
			case 9:
				return "--------";
			case 10:
				return "Balance";
			default:
				return null;
			}
		}
		// passive values
		else if (columnIndex == 3) {
			switch (rowIndex) {
			case 1:
				return Currency.formatMoneySum(balanceSheet.loansGiroShortTerm);
			case 2:
				return Currency.formatMoneySum(balanceSheet.loansGiroLongTerm);
			case 3:
				return Currency.formatMoneySum(balanceSheet.loansCentralBankShortTerm);
			case 4:
				return Currency.formatMoneySum(balanceSheet.loansCentralBankLongTerm);

			case 6:
				return Currency.formatMoneySum(balanceSheet.financialLiabilities);
			case 7:
				return Currency.formatMoneySum(balanceSheet.bankBorrowings);
			case 8:
				return Currency.formatMoneySum(balanceSheet.getEquity());
			case 10:
				return Currency.formatMoneySum(balanceSheet.getBalancePassive());
			default:
				return null;
			}
		} else {
			return null;
		}
	}

	@Override
	public synchronized void notifyListener() {
		balanceSheet = getBalanceSheet();
		fireTableDataChanged();
	}
}