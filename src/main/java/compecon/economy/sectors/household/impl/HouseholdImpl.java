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

package compecon.economy.sectors.household.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;
import javax.persistence.Transient;

import org.hibernate.annotations.Index;

import compecon.economy.agent.impl.AgentImpl;
import compecon.economy.behaviour.PricingBehaviour;
import compecon.economy.behaviour.impl.PricingBehaviourImpl;
import compecon.economy.bookkeeping.impl.BalanceSheetDTO;
import compecon.economy.markets.SettlementMarket.ISettlementEvent;
import compecon.economy.property.Property;
import compecon.economy.sectors.financial.BankAccount;
import compecon.economy.sectors.financial.BankAccount.MoneyType;
import compecon.economy.sectors.financial.BankAccount.TermType;
import compecon.economy.sectors.financial.Currency;
import compecon.economy.sectors.financial.impl.BankAccountImpl;
import compecon.economy.sectors.household.Household;
import compecon.economy.security.equity.Share;
import compecon.economy.security.equity.ShareOwner;
import compecon.economy.security.equity.impl.ShareImpl;
import compecon.engine.applicationcontext.ApplicationContext;
import compecon.engine.timesystem.ITimeSystemEvent;
import compecon.engine.timesystem.impl.DayType;
import compecon.engine.timesystem.impl.MonthType;
import compecon.engine.util.MathUtil;
import compecon.materia.GoodType;
import compecon.materia.Refreshable;
import compecon.math.intertemporal.IntertemporalConsumptionFunction;
import compecon.math.intertemporal.impl.IrvingFisherIntertemporalConsumptionFunction.Period;
import compecon.math.price.PriceFunction;
import compecon.math.utility.UtilityFunction;

/**
 * Agent type Household offers labour hours and consumes goods.
 */
@Entity
public class HouseholdImpl extends AgentImpl implements Household, ShareOwner {

	// configuration constants ------------------------------

	@Transient
	protected int DAYS_WITHOUT_UTILITY_UNTIL_DESTRUCTOR;

	// dynamic state ------------------------------

	@Column(name = "ageInDays")
	protected int ageInDays = 0;

	@Column(name = "continuousDaysWithUtility")
	protected int continuousDaysWithUtility = 0;

	@Column(name = "daysWithoutUtility")
	protected int daysWithoutUtility = 0;

	@OneToOne(targetEntity = BankAccountImpl.class)
	@JoinColumn(name = "bankAccountSavings_id")
	@Index(name = "IDX_A_BA_SAVINGS")
	// bank account for savings
	protected BankAccount bankAccountSavings;

	@Transient
	protected double dividendSinceLastPeriod;

	@Transient
	protected IntertemporalConsumptionFunction intertemporalConsumptionFunction;

	@Transient
	protected LabourPower labourPower = new LabourPower();

	@Transient
	protected PricingBehaviour pricingBehaviour;

	@Transient
	protected UtilityFunction utilityFunction;

	@Override
	public void initialize() {
		super.initialize();

		this.DAYS_WITHOUT_UTILITY_UNTIL_DESTRUCTOR = ApplicationContext
				.getInstance().getConfiguration().householdConfig
				.getDaysWithoutUtilityUntilDestructor()
				+ this.hashCode()
				% ApplicationContext.getInstance().getConfiguration().householdConfig
						.getDaysWithoutUtilityUntilDestructor();

		// daily life at random HourType
		final ITimeSystemEvent dailyLifeEvent = new DailyLifeEvent();
		this.timeSystemEvents.add(dailyLifeEvent);
		ApplicationContext
				.getInstance()
				.getTimeSystem()
				.addEvent(
						dailyLifeEvent,
						-1,
						MonthType.EVERY,
						DayType.EVERY,
						ApplicationContext.getInstance().getTimeSystem()
								.suggestRandomHourType());

		final double marketPrice = ApplicationContext.getInstance()
				.getMarketFactory().getMarket()
				.getPrice(this.primaryCurrency, GoodType.LABOURHOUR);
		this.pricingBehaviour = new PricingBehaviourImpl(this,
				GoodType.LABOURHOUR, this.primaryCurrency, marketPrice);
		this.labourPower.refresh();
	}

	@Transient
	public void deconstruct() {
		super.deconstruct();

		this.bankAccountSavings = null;
	}

	/*
	 * accessors
	 */

	public int getAgeInDays() {
		return ageInDays;
	}

	public BankAccount getBankAccountSavings() {
		return bankAccountSavings;
	}

	public int getContinuousDaysWithUtility() {
		return continuousDaysWithUtility;
	}

	public int getDaysWithoutUtility() {
		return daysWithoutUtility;
	}

	public IntertemporalConsumptionFunction getIntertemporalConsumptionFunction() {
		return intertemporalConsumptionFunction;
	}

	public PricingBehaviour getPricingBehaviour() {
		return pricingBehaviour;
	}

	public UtilityFunction getUtilityFunction() {
		return utilityFunction;
	}

	public void setAgeInDays(int ageInDays) {
		this.ageInDays = ageInDays;
	}

	public void setBankAccountSavings(BankAccount bankAccountSavings) {
		this.bankAccountSavings = bankAccountSavings;
	}

	public void setContinuousDaysWithUtility(int continuousDaysWithUtility) {
		this.continuousDaysWithUtility = continuousDaysWithUtility;
	}

	public void setDaysWithoutUtility(int daysWithoutUtility) {
		this.daysWithoutUtility = daysWithoutUtility;
	}

	public void setIntertemporalConsumptionFunction(
			final IntertemporalConsumptionFunction intertemporalConsumptionFunction) {
		this.intertemporalConsumptionFunction = intertemporalConsumptionFunction;
	}

	public void setPricingBehaviour(final PricingBehaviour pricingBehaviour) {
		this.pricingBehaviour = pricingBehaviour;
	}

	public void setUtilityFunction(final UtilityFunction utilityFunction) {
		this.utilityFunction = utilityFunction;
	}

	/*
	 * assertions
	 */

	@Transient
	public void assureDividendBankAccount() {
		this.assureBankAccountTransactions();
	}

	@Transient
	public void assureBankAccountSavings() {
		if (this.isDeconstructed)
			return;

		this.assureBankCustomerAccount();

		// initialize bank account
		if (this.bankAccountSavings == null) {
			this.bankAccountSavings = this.primaryBank.openBankAccount(this,
					this.primaryCurrency, false, "savings", TermType.LONG_TERM,
					MoneyType.DEPOSITS);
		}
	}

	/*
	 * business logic
	 */

	@Override
	@Transient
	public BankAccount getDividendBankAccount() {
		this.assureBankAccountTransactions();

		return this.bankAccountTransactions;
	}

	@Override
	@Transient
	protected BalanceSheetDTO issueBalanceSheet() {
		this.assureBankAccountSavings();

		final BalanceSheetDTO balanceSheet = super.issueBalanceSheet();

		// retirement savings
		balanceSheet.addBankAccountBalance(this.bankAccountSavings);

		return balanceSheet;
	}

	@Override
	@Transient
	public void onDividendTransfer(double dividendAmount) {
		this.dividendSinceLastPeriod += dividendAmount;
	}

	@Override
	@Transient
	public void onBankCloseBankAccount(BankAccount bankAccount) {
		if (this.bankAccountSavings != null
				&& this.bankAccountSavings == bankAccount) {
			this.bankAccountSavings = null;
		}

		super.onBankCloseBankAccount(bankAccount);
	}

	@Override
	public String toString() {
		return super.toString() + " [" + this.ageInDays / 365 + " years]";
	}

	public class SettlementMarketEvent implements ISettlementEvent {
		@Override
		public void onEvent(GoodType goodType, double amount,
				double pricePerUnit, Currency currency) {
			if (goodType.equals(GoodType.LABOURHOUR)) {
				HouseholdImpl.this.pricingBehaviour.registerSelling(amount,
						amount * pricePerUnit);
			}
		}

		@Override
		public void onEvent(Currency commodityCurrency, double amount,
				double pricePerUnit, Currency currency) {
		}

		@Override
		public void onEvent(Property property, double pricePerUnit,
				Currency currency) {
		}

	}

	public class DailyLifeEvent implements ITimeSystemEvent {

		@Override
		public void onEvent() {
			assert (!HouseholdImpl.this.isDeconstructed);

			/*
			 * potentially call destructor
			 */
			if (HouseholdImpl.this.ageInDays > ApplicationContext.getInstance()
					.getConfiguration().householdConfig.getLifespanInDays()) {
				HouseholdImpl.this.deconstruct();
				return;
			}

			getLog().household_AmountSold(HouseholdImpl.this.primaryCurrency,
					HouseholdImpl.this.pricingBehaviour.getLastSoldAmount());

			/*
			 * simulation mechanics
			 */
			HouseholdImpl.this.ageInDays++;
			HouseholdImpl.this.labourPower.refresh();
			HouseholdImpl.this.pricingBehaviour.nextPeriod();

			HouseholdImpl.this.assureBankAccountTransactions();
			HouseholdImpl.this.assureBankAccountSavings();

			/*
			 * economic actions
			 */
			double budget = this.saveMoney();

			double numberOfLabourHoursToConsume = this
					.buyOptimalGoodsForBudget(budget);

			double utility = this.consumeGoods(numberOfLabourHoursToConsume);

			this.offerLabourHours();

			this.buyAndOfferShares();

			// households make no debt; safety epsilon due to iterative
			// deviations
			assert (MathUtil.greaterEqual(
					HouseholdImpl.this.bankAccountTransactions.getBalance(),
					0.0));
			assert (MathUtil.greaterEqual(
					HouseholdImpl.this.bankAccountSavings.getBalance(), 0.0));

			checkRequiredUtilityPerDay(utility);

			checkDeriveNewHousehold();

			checkCallDestructor();
		}

		protected double saveMoney() {
			/*
			 * calculate budget
			 */
			final double keyInterestRate = ApplicationContext.getInstance()
					.getAgentFactory()
					.getInstanceCentralBank(HouseholdImpl.this.primaryCurrency)
					.getEffectiveKeyInterestRate();
			final double income = HouseholdImpl.this.bankAccountTransactions
					.getBalance();

			final double budget;
			// do households save for retirement?
			if (ApplicationContext.getInstance().getConfiguration().householdConfig
					.getRetirementSaving()) {
				Map<Period, Double> intertemporalConsumptionPlan = HouseholdImpl.this.intertemporalConsumptionFunction
						.calculateUtilityMaximizingConsumptionPlan(
								income,
								HouseholdImpl.this.bankAccountSavings
										.getBalance(),
								keyInterestRate,
								HouseholdImpl.this.ageInDays,
								ApplicationContext.getInstance()
										.getConfiguration().householdConfig
										.getRetirementAgeInDays(),
								ApplicationContext.getInstance()
										.getConfiguration().householdConfig
										.getLifespanInDays()
										- HouseholdImpl.this.ageInDays);
				budget = intertemporalConsumptionPlan.get(Period.CURRENT);
			} else {
				budget = income;
			}

			final double moneySumToSave = income - budget;
			final double moneySumToConsume = budget;

			/*
			 * logging
			 */
			getLog().household_onIncomeWageDividendConsumptionSaving(
					primaryCurrency, income, moneySumToConsume, moneySumToSave,
					HouseholdImpl.this.pricingBehaviour.getLastSoldValue(),
					HouseholdImpl.this.dividendSinceLastPeriod);

			HouseholdImpl.this.dividendSinceLastPeriod = 0;

			if (moneySumToSave > 0.0) {
				/*
				 * save money for retirement
				 */
				HouseholdImpl.this.bankAccountTransactions.getManagingBank()
						.transferMoney(
								HouseholdImpl.this.bankAccountTransactions,
								HouseholdImpl.this.bankAccountSavings,
								moneySumToSave, "retirement savings");
				if (getLog().isAgentSelectedByClient(HouseholdImpl.this))
					getLog().log(
							HouseholdImpl.this,
							DailyLifeEvent.class,
							"saving "
									+ Currency.formatMoneySum(moneySumToSave)
									+ " "
									+ HouseholdImpl.this.bankAccountTransactions
											.getCurrency().getIso4217Code()
									+ " of "
									+ Currency
											.formatMoneySum(HouseholdImpl.this.bankAccountTransactions
													.getBalance())
									+ " "
									+ HouseholdImpl.this.bankAccountTransactions
											.getCurrency().getIso4217Code()
									+ " income");
			} else if (moneySumToSave < 0.0) {
				/*
				 * dissavings can happen also when not retired, yet, in case of
				 * no income due to average conmption premise (e. g. Modigliani
				 * intertemporal consumption function)
				 */

				/*
				 * spend saved retirement money
				 */
				HouseholdImpl.this.bankAccountTransactions.getManagingBank()
						.transferMoney(HouseholdImpl.this.bankAccountSavings,
								HouseholdImpl.this.bankAccountTransactions,
								-1.0 * moneySumToSave, "retirement dissavings");
				if (getLog().isAgentSelectedByClient(HouseholdImpl.this))
					getLog().log(
							HouseholdImpl.this,
							"unsaving "
									+ Currency.formatMoneySum(-1.0
											* moneySumToSave)
									+ " "
									+ HouseholdImpl.this.bankAccountSavings
											.getCurrency().getIso4217Code());
			}

			return budget;
		}

		protected double buyOptimalGoodsForBudget(double budget) {
			double numberOfLabourHoursToConsume = 0.0;

			if (MathUtil.greater(budget, 0.0)) {
				// get prices for good types
				Map<GoodType, PriceFunction> priceFunctions = ApplicationContext
						.getInstance()
						.getMarketFactory()
						.getMarket()
						.getFixedPriceFunctions(
								HouseholdImpl.this.primaryCurrency,
								HouseholdImpl.this.utilityFunction
										.getInputGoodTypes());

				// calculate optimal consumption plan
				getLog().setAgentCurrentlyActive(HouseholdImpl.this);
				Map<GoodType, Double> plannedConsumptionGoodsBundle = HouseholdImpl.this.utilityFunction
						.calculateUtilityMaximizingInputs(priceFunctions,
								budget);
				numberOfLabourHoursToConsume = plannedConsumptionGoodsBundle
						.get(GoodType.LABOURHOUR);

				// no labour hours should be bought on markets
				plannedConsumptionGoodsBundle.remove(GoodType.LABOURHOUR);

				// buy goods
				double budgetSpent = this.buyGoods(
						plannedConsumptionGoodsBundle, priceFunctions, budget);

				assert (MathUtil.lesserEqual(budgetSpent, budget * 1.1));
			}

			return numberOfLabourHoursToConsume;
		}

		private double buyGoods(final Map<GoodType, Double> goodsToBuy,
				final Map<GoodType, PriceFunction> priceFunctions,
				final double budget) {
			/*
			 * buy production factors; maxPricePerUnit is significantly
			 * important for price equilibrium
			 */
			double budgetSpent = 0.0;
			for (Entry<GoodType, Double> entry : goodsToBuy.entrySet()) {
				GoodType goodTypeToBuy = entry.getKey();
				double amountToBuy = entry.getValue();
				if (MathUtil.greater(amountToBuy, 0.0)) {
					double marginalPrice = priceFunctions.get(goodTypeToBuy)
							.getMarginalPrice(0.0);

					// maxPricePerUnit is significantly important for price
					// equilibrium; also budget, as in the depth of the markets,
					// prices can rise, leading to overspending
					double[] priceAndAmount = ApplicationContext
							.getInstance()
							.getMarketFactory()
							.getMarket()
							.buy(goodTypeToBuy,
									amountToBuy,
									budget,
									marginalPrice
											* ApplicationContext.getInstance()
													.getConfiguration().householdConfig
													.getMaxPricePerUnitMultiplier(),
									HouseholdImpl.this,
									HouseholdImpl.this.bankAccountTransactions);
					budgetSpent += priceAndAmount[0];
				}
			}
			return budgetSpent;
		}

		protected double consumeGoods(double numberOfLabourHoursToConsume) {
			/*
			 * consume goods
			 */
			final Map<GoodType, Double> effectiveConsumptionGoodsBundle = new HashMap<GoodType, Double>();
			for (GoodType goodType : HouseholdImpl.this.utilityFunction
					.getInputGoodTypes()) {
				double balance = ApplicationContext.getInstance()
						.getPropertyRegister()
						.getBalance(HouseholdImpl.this, goodType);
				double amountToConsume;
				if (GoodType.LABOURHOUR.equals(goodType)) {
					amountToConsume = Math.min(numberOfLabourHoursToConsume,
							balance);
				} else {
					amountToConsume = balance;
				}
				effectiveConsumptionGoodsBundle.put(goodType, amountToConsume);
				ApplicationContext
						.getInstance()
						.getPropertyRegister()
						.decrementGoodTypeAmount(HouseholdImpl.this, goodType,
								amountToConsume);
			}
			double utility = HouseholdImpl.this.utilityFunction
					.calculateUtility(effectiveConsumptionGoodsBundle);
			getLog().household_onUtility(HouseholdImpl.this,
					HouseholdImpl.this.bankAccountTransactions.getCurrency(),
					effectiveConsumptionGoodsBundle, utility);
			return utility;
		}

		protected void offerLabourHours() {
			/*
			 * remove labour hour offers
			 */
			ApplicationContext
					.getInstance()
					.getMarketFactory()
					.getMarket()
					.removeAllSellingOffers(
							HouseholdImpl.this,
							HouseholdImpl.this.bankAccountTransactions
									.getCurrency(), GoodType.LABOURHOUR);

			// if not retired
			if (HouseholdImpl.this.ageInDays < ApplicationContext.getInstance()
					.getConfiguration().householdConfig
					.getRetirementAgeInDays()) {
				/*
				 * offer labour hours
				 */
				double amountOfLabourHours = ApplicationContext.getInstance()
						.getPropertyRegister()
						.getBalance(HouseholdImpl.this, GoodType.LABOURHOUR);
				double prices[] = HouseholdImpl.this.pricingBehaviour
						.getCurrentPriceArray();
				for (double price : prices) {
					ApplicationContext
							.getInstance()
							.getMarketFactory()
							.getMarket()
							.placeSettlementSellingOffer(
									GoodType.LABOURHOUR,
									HouseholdImpl.this,
									HouseholdImpl.this.bankAccountTransactions,
									amountOfLabourHours
											/ ((double) prices.length), price,
									new SettlementMarketEvent());
				}
				HouseholdImpl.this.pricingBehaviour
						.registerOfferedAmount(amountOfLabourHours);

				getLog().household_onOfferResult(
						HouseholdImpl.this.primaryCurrency,
						HouseholdImpl.this.pricingBehaviour
								.getLastOfferedAmount(),
						ApplicationContext.getInstance().getConfiguration().householdConfig
								.getNumberOfLabourHoursPerDay());
			}
		}

		protected void buyAndOfferShares() {
			/*
			 * buy shares / capital -> equity savings
			 */
			// FIXME ShareImpl -> Share
			ApplicationContext
					.getInstance()
					.getMarketFactory()
					.getMarket()
					.buy(ShareImpl.class, 1.0, 0.0, 0.0, HouseholdImpl.this,
							HouseholdImpl.this.bankAccountTransactions);

			/*
			 * sell shares that are denominated in an incorrect currency
			 */
			// FIXME ShareImpl -> Share
			ApplicationContext
					.getInstance()
					.getMarketFactory()
					.getMarket()
					.removeAllSellingOffers(
							HouseholdImpl.this,
							HouseholdImpl.this.bankAccountTransactions
									.getCurrency(), ShareImpl.class);
			for (Property property : ApplicationContext.getInstance()
					.getPropertyRegister()
					.getProperties(HouseholdImpl.this, Share.class)) {
				if (property instanceof Share) {
					Share share = (Share) property;
					if (!HouseholdImpl.this.primaryCurrency.equals(share
							.getJointStockCompany().getPrimaryCurrency()))
						ApplicationContext
								.getInstance()
								.getMarketFactory()
								.getMarket()
								.placeSellingOffer(
										property,
										HouseholdImpl.this,
										HouseholdImpl.this
												.getBankAccountTransactions(),
										0.0);
				}
			}
		}

		protected void checkRequiredUtilityPerDay(final double utility) {
			/*
			 * check for required utility
			 */
			if (utility < ApplicationContext.getInstance().getConfiguration().householdConfig
					.getRequiredUtilityPerDay()) {
				HouseholdImpl.this.daysWithoutUtility++;
				HouseholdImpl.this.continuousDaysWithUtility = 0;
				if (getLog().isAgentSelectedByClient(HouseholdImpl.this))
					getLog().log(
							HouseholdImpl.this,
							DailyLifeEvent.class,
							"does not have required utility of "
									+ ApplicationContext.getInstance()
											.getConfiguration().householdConfig
											.getRequiredUtilityPerDay());
			} else {
				if (HouseholdImpl.this.daysWithoutUtility > 0)
					daysWithoutUtility--;
				HouseholdImpl.this.continuousDaysWithUtility++;
			}
		}

		protected void checkDeriveNewHousehold() {
			/*
			 * potentially, derive new household
			 */
			final int NEW_HOUSEHOLD_FROM_X_DAYS = ApplicationContext
					.getInstance().getConfiguration().householdConfig
					.getNewHouseholdFromAgeInDays();
			if (HouseholdImpl.this.ageInDays >= NEW_HOUSEHOLD_FROM_X_DAYS) {
				if ((HouseholdImpl.this.ageInDays - NEW_HOUSEHOLD_FROM_X_DAYS)
						% ApplicationContext.getInstance().getConfiguration().householdConfig
								.getNewHouseholdEveryXDays() == 0) {
					ApplicationContext
							.getInstance()
							.getAgentFactory()
							.newInstanceHousehold(
									HouseholdImpl.this.primaryCurrency);
				}
			}
		}

		protected void checkCallDestructor() {
			/*
			 * potentially, call destructor
			 */
			if (HouseholdImpl.this.daysWithoutUtility > HouseholdImpl.this.DAYS_WITHOUT_UTILITY_UNTIL_DESTRUCTOR) {
				if (!ApplicationContext.getInstance().getTimeSystem()
						.isInitializationPhase()) {
					HouseholdImpl.this.deconstruct();
				}
			}
		}
	}

	protected class LabourPower implements Refreshable {

		public double getNumberOfLabourHoursAvailable() {
			return ApplicationContext.getInstance().getPropertyRegister()
					.getBalance(HouseholdImpl.this, GoodType.LABOURHOUR);
		}

		@Override
		public boolean isExhausted() {
			return ApplicationContext.getInstance().getPropertyRegister()
					.getBalance(HouseholdImpl.this, GoodType.LABOURHOUR) <= 0;
		}

		@Override
		public void exhaust() {
			ApplicationContext
					.getInstance()
					.getPropertyRegister()
					.resetGoodTypeAmount(HouseholdImpl.this,
							GoodType.LABOURHOUR);
		}

		@Override
		public void refresh() {
			exhaust();
			ApplicationContext
					.getInstance()
					.getPropertyRegister()
					.incrementGoodTypeAmount(
							HouseholdImpl.this,
							GoodType.LABOURHOUR,
							ApplicationContext.getInstance().getConfiguration().householdConfig
									.getNumberOfLabourHoursPerDay());
		}
	}
}
