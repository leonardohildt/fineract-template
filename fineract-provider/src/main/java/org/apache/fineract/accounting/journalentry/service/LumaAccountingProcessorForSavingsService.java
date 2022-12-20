/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.accounting.journalentry.service;

import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_SOBREGIROCR;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.annotation.PostConstruct;
import org.apache.fineract.accounting.journalentry.data.BitaCoreData;
import org.apache.fineract.accounting.journalentry.data.LumaBitacoraTransactionTypeEnum;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraDetails;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMaster;
import org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants;
import org.apache.fineract.infrastructure.codes.data.CodeCauseProcessMappingData;
import org.apache.fineract.infrastructure.codes.domain.LumaProccessId;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.apache.fineract.organisation.office.domain.Office;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants;
import org.apache.fineract.portfolio.common.service.BusinessEventListener;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService2;
import org.apache.fineract.portfolio.savings.SavingsTransactionBooleanValues;
import org.apache.fineract.portfolio.savings.data.SavingsAccountData;
import org.apache.fineract.portfolio.savings.data.SavingsAccountTransactionData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountChargePaidBy;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountChargesPaidByData;
import org.apache.fineract.portfolio.savings.domain.SavingsAccountTransaction;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class LumaAccountingProcessorForSavingsService extends LumaAccountingProcessor {

    public static final String STRING_ACTIVO = "ACTIVO";
    public static final String STRING_CLIENT_TYPE_A = "A";
    public static final String STRING_SOBREGIRO_CUENTA = "SOBREGIRO CUENTA: ";

    private CurrencyReadPlatformService currencyReadPlatformService;
    private final BusinessEventNotifierService2 businessEventNotifierService;

    @Autowired
    public LumaAccountingProcessorForSavingsService(CurrencyReadPlatformService currencyReadPlatformService,
            BusinessEventNotifierService2 businessEventNotifierService) {
        this.currencyReadPlatformService = currencyReadPlatformService;
        this.businessEventNotifierService = businessEventNotifierService;
    }

    @PostConstruct
    public void addListeners() {
        this.businessEventNotifierService.addBusinessEventPostListeners(
                BusinessEventNotificationConstants.BusinessEvents.SAVINGS_ADJUST_TRANSACTION, new AdjustFundsOnBusinessEvent());
    }

    private class AdjustFundsOnBusinessEvent implements BusinessEventListener {

        @Override
        public void businessEventToBeExecuted(
                @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.SAVINGS_TRANSACTION);
            if (entity instanceof SavingsAccountTransaction) {
                SavingsAccountTransaction savingsAccountTransaction = (SavingsAccountTransaction) entity;
                reverseLogForTransactionId(savingsAccountTransaction.getId(), BitacoraMasterConstants.ACCOUNT_TYPE_AH);
            }
        }
    }

    public BitaCoreData createJournalEntriesForSavingsAccountInOverdraft(final SavingsAccount savingsAccount, LocalDate transactionDate,
            BigDecimal amount, Long reference, Office office) {

        final List<BitaCoraMaster> masterList = new ArrayList<>();

        transactionDate = transactionDate.plusDays(1);

        masterList.addAll(createJournalEntry(LumaBitacoraTransactionTypeEnum.SAVINGS_ACCOUNT_OVERDRAFT_DB, savingsAccount, transactionDate,
                amount, reference, office));

        // Credits shall be entered with D-1 date
        transactionDate = transactionDate.minusDays(1);

        masterList.addAll(createJournalEntry(LumaBitacoraTransactionTypeEnum.SAVINGS_ACCOUNT_OVERDRAFT_CR, savingsAccount, transactionDate,
                amount, reference, office));

        return new BitaCoreData(masterList, null);
    }

    private List<BitaCoraMaster> createJournalEntry(LumaBitacoraTransactionTypeEnum lumaBitacoraTransactionTypeEnum,
            SavingsAccount savingsAccount, LocalDate transactionLocalDate, BigDecimal amount, Long reference, Office office) {
        final List<BitaCoraMaster> ret = new ArrayList<>();

        final Date transactionDate = Date.from(transactionLocalDate.atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(savingsAccount.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        String groupString = lumaBitacoraTransactionTypeEnum.getGroupTYpe();
        Long transactionId = null;

        CodeCauseProcessMappingData causeProcessMappingData;
        if (lumaBitacoraTransactionTypeEnum.getCode().equals(TRX_TYPE_SOBREGIROCR)) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.OVERDRAFT_CREDIT.getValue(),
                    currencyData.getIntCode());
        } else {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.OVERDRAFT_DEBIT.getValue(),
                    currencyData.getIntCode());
        }

        Integer causal = null;
        String trxType = lumaBitacoraTransactionTypeEnum.getCode();
        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, savingsAccount.getId(),
                BitacoraMasterConstants.ACCOUNT_TYPE_AH, currencyString, amount, STRING_ACTIVO, groupString, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(savingsAccount.getLumaClientId());
        master.setClientType(STRING_CLIENT_TYPE_A);
        master.setReference(reference.toString());

        BigDecimal differentialAmount = amount.multiply(exchangeRate);
        BitaCoraDetails masterDetails = new BitaCoraDetails(STRING_SOBREGIRO_CUENTA + savingsAccount.getAccountNumber(), amount, causal,
                null, null, lumaBitacoraTransactionTypeEnum.getAmountType(), null, null, null, master, differentialAmount);
        master.addBitaCoraDetails(masterDetails);
        ret.add(master);

        return ret;
    }

    public BitaCoraMaster createJournalEntriesForSavingsDeposit(SavingsAccount account, SavingsAccountTransaction deposit,
            final SavingsTransactionBooleanValues transactionBooleanValues) {

        final Date transactionDate = Date
                .from(deposit.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        String heading = "Depósito a cuenta: ".concat(account.getId().toString());
        CodeCauseProcessMappingData causeProcessMappingData = null;
        if (transactionBooleanValues.isAccountTransfer()) {
            if (transactionBooleanValues.isDisburseToSavingsTransaction()) {
                causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.DISBURSE_TO_SAVINGS.getValue(),
                        currencyData.getIntCode());
                heading = "NC a cuenta: " + account.getId();
            }
            if (transactionBooleanValues.isDepositForFDActivation()) {
                causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                        LumaProccessId.FIXED_DEPOSIT_DEPOSIT_FROM_SAVINGS.getValue(), currencyData.getIntCode());
            }
        }

        Integer causal = 1001;
        // Integer causal = getBitacoraDetaislNewValue(account, deposit) != null ? getBitacoraDetaislNewValue(account,
        // deposit) : 1001;
        String trxType = BitacoraMasterConstants.TRX_TYPE_DP;
        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // LH-1132 - set causal for the current transaction
        // if (causal != null) {
        // deposit.setPaymentCauseId(causal);
        // }

        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = deposit.getId();
        final BigDecimal amount = deposit.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final BigDecimal amount_capital = amount;
        final String newValue_capital = null;
        final Integer headingCode_capital = null;
        final String amountType_capital = "CAPITAL";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    /*
     * private Integer getBitacoraDetaislNewValue(SavingsAccount account, SavingsAccountTransaction deposit) {
     *
     * Integer codeVal = null;
     *
     * // If comes from a regular, manual deposit, pick from deposit obj if
     * (Objects.nonNull(deposit.getPaymentCauseId()) && (deposit.getPaymentCauseId() > 0)) { codeVal =
     * deposit.getPaymentCauseId();
     *
     * // If comes from a Regular Savings, Fixed or Recurrent Deposit activation, pick from savings obj } else if
     * (Objects.nonNull(account.getDepositCauseId()) && (account.getDepositCauseId() > 0)) { codeVal =
     * account.getDepositCauseId(); }
     *
     * return codeVal; }
     */

    public BitaCoraMaster createJournalEntriesForSavingsWithdrawal(SavingsAccount account, SavingsAccountTransaction withdrawal,
            final SavingsTransactionBooleanValues transactionBooleanValues) {

        final Date transactionDate = Date
                .from(withdrawal.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());

        CodeCauseProcessMappingData causeProcessMappingData = null;
        if (transactionBooleanValues.isAccountTransfer() && transactionBooleanValues.isWithdrawalToPayCharge()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.CHARGE_ON_DISBURSE_TO_SAVINGS.getValue(),
                    currencyData.getIntCode());
        }
        if (transactionBooleanValues.isAccountTransfer() && transactionBooleanValues.isLoanRepaymentFromSavingsStandingInstruction()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                    LumaProccessId.LOAN_REPAYMENT_FROM_SAVINGS_STANDING_INSTRUCTION.getValue(), currencyData.getIntCode());
        }
        if (transactionBooleanValues.isAccountTransfer() && transactionBooleanValues.isDepositForFDActivation()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                    LumaProccessId.SAVINGS_WITHDRAWAL_TO_FIXED_DEPOSIT.getValue(), currencyData.getIntCode());
        }
        if (transactionBooleanValues.isRegularTransaction() && transactionBooleanValues.isWithdrawalByCheckRejection()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.CHECK_REJECTION.getValue(),
                    currencyData.getIntCode());
        }

        Integer causal = 1002;
        // Integer causal = withdrawal.getPaymentCauseId() != null && withdrawal.getPaymentCauseId() > 0 ?
        // withdrawal.getPaymentCauseId()
        // : 1002; // 1002:

        String trxType = BitacoraMasterConstants.TRX_TYPE_ND;
        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // LH-1132 - set causal for the current transaction
        // if (causal != null) {
        // withdrawal.setPaymentCauseId(causal);
        // }

        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = withdrawal.getId();
        final BigDecimal amount = withdrawal.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final String heading_capital = "ND a cuenta: ".concat(accountId.toString());
        final BigDecimal amount_capital = amount;
        // Retiro
        final String newValue_capital = null;
        final Integer headingCode_capital = null;
        final String amountType_capital = "CAPITAL";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading_capital, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsCharges(SavingsAccount account, SavingsAccountTransaction transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        // CodeCauseProcessMappingData causeProcessMappingData = null;
        // if (transaction.getSavingsAccountChargesPaid().size() == 1) {
        // SavingsAccountChargePaidBy chargePaidBy =
        // transaction.getSavingsAccountChargesPaid().stream().findFirst().get();
        // if (chargePaidBy.getSavingsAccountCharge().isCheckRejectFee()) {
        // causeProcessMappingData =
        // getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.REJECTED_CHECK_CHARGE.getValue(),
        // currencyData.getIntCode());
        // }
        // }

        Integer causal = 1007;
        // Integer causal = transaction.getPaymentCauseId() != null && transaction.getPaymentCauseId() > 0 ?
        // transaction.getPaymentCauseId()
        // : 1007;
        String trxType = BitacoraMasterConstants.TRX_TYPE_NDCARGOS;
        // if (causeProcessMappingData != null) {
        // causal = causeProcessMappingData.getCausalCode();
        // trxType = causeProcessMappingData.getLogTransactionType();
        // }

        // LH-1132 - set causal for the current transaction
        // transaction.setPaymentCauseId(causal);

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final String heading_capital = "ND a cuenta: ".concat(accountId.toString());
        final BigDecimal amount_capital = amount;
        // D�bito
        // por
        // cargo
        final String newValue_capital = null;

        Integer headingCode_capital = null;
        if (transaction.getSavingsAccountChargesPaid() != null) {
            for (SavingsAccountChargePaidBy chargePaidBy : transaction.getSavingsAccountChargesPaid()) {
                if (chargePaidBy.getSavingsAccountCharge() != null && chargePaidBy.getSavingsAccountCharge().getCharge() != null) {
                    headingCode_capital = chargePaidBy.getSavingsAccountCharge().getCharge().getId().intValue();
                    break;
                }
            }
        }
        final String amountType_capital = "CAPITAL";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading_capital, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsInterestAccruals(SavingsAccount account, SavingsAccountTransaction transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        CodeCauseProcessMappingData causeProcessMappingData = null;
        Integer causal = null;
        String trxType = BitacoraMasterConstants.TRX_TYPE_CALCINT;
        if (account.depositAccountType().isFixedDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.ACCRUAL_INTEREST_FIXED_DEPOSIT.getValue(),
                    currencyData.getIntCode());
        } else if (account.depositAccountType().isSavingsDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.ACCRUAL_INTEREST_SAVINGS.getValue(),
                    currencyData.getIntCode());
        }

        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // LH-1132 - set causal for the current transaction
        // if (causal != null) {
        // transaction.setPaymentCauseId(causal);
        // }

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final String heading_capital = "NC a cuenta: ".concat(accountId.toString());
        final BigDecimal amount_capital = amount;
        final String newValue_capital = null;
        final Integer headingCode_capital = null;
        final String amountType_capital = "INTERESES";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading_capital, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsInterestPost(SavingsAccount account, SavingsAccountTransaction transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        CodeCauseProcessMappingData causeProcessMappingData = null;
        Integer causal = null;
        String trxType = BitacoraMasterConstants.TRX_TYPE_CAPINT;
        if (account.depositAccountType().isFixedDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.FIXED_DEPOSIT_INTEREST_POSTING.getValue(),
                    currencyData.getIntCode());
        } else if (account.depositAccountType().isSavingsDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.SAVINGS_INTEREST_POSTING.getValue(),
                    currencyData.getIntCode());
        }

        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // LH-1132 - set causal for the current transaction
        // if (causal != null) {
        // transaction.setPaymentCauseId(causal);
        // }

        return createJournalEntriesForSavingsInterestPost(transactionDate, exchangeRate, currencyString, accountType, group, transactionId,
                amount, accountId, clientId, status, office, causal, trxType);
    }

    public BitaCoraMaster createJournalEntriesForSavingsInterestPost(SavingsAccountData account,
            SavingsAccountTransactionData transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = null;
        // getOffice(account);

        CodeCauseProcessMappingData causeProcessMappingData = null;
        Integer causal = null;
        String trxType = BitacoraMasterConstants.TRX_TYPE_CAPINT;
        if (account.depositAccountType().isFixedDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.FIXED_DEPOSIT_INTEREST_POSTING.getValue(),
                    currencyData.getIntCode());
        } else if (account.depositAccountType().isSavingsDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(LumaProccessId.SAVINGS_INTEREST_POSTING.getValue(),
                    currencyData.getIntCode());
        }

        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // LH-1132 - set causal for the current transaction
        // if (causal != null) {
        // transaction.setPaymentCauseId(causal);
        // }

        return createJournalEntriesForSavingsInterestPost(transactionDate, exchangeRate, currencyString, accountType, group, transactionId,
                amount, accountId, clientId, status, office, causal, trxType);
    }

    private BitaCoraMaster createJournalEntriesForSavingsInterestPost(final Date transactionDate, final BigDecimal exchangeRate,
            String currencyString, final String accountType, final String group, Long transactionId, final BigDecimal amount,
            final Long accountId, final Long clientId, final String status, final Office office, Integer causal, String trxType) {
        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final String heading_capital = "NC a cuenta: ".concat(accountId.toString());
        final BigDecimal amount_capital = amount;
        // Capitalizaci�n
        // de
        // intereses
        final String newValue_capital = null;
        final Integer headingCode_capital = null;
        final String amountType_capital = "CAPITAL";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading_capital, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsISR(SavingsAccount account, SavingsAccountTransaction transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        CodeCauseProcessMappingData causeProcessMappingData = null;
        Integer causal = null;
        String trxType = BitacoraMasterConstants.TRX_TYPE_ISRINT;
        if (account.depositAccountType().isFixedDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                    LumaProccessId.WITHHOLD_TAX_INTEREST_POSTING_FIXED_DEPOSIT.getValue(), currencyData.getIntCode());
        } else if (account.depositAccountType().isSavingsDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                    LumaProccessId.WITHHOLD_TAX_INTEREST_POSTING_SAVINGS.getValue(), currencyData.getIntCode());
        }

        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // set causal for the current transaction
        // if (causal != null) {
        // transaction.setPaymentCauseId(causal);
        // }

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final String heading_capital = "ND a cuenta: ".concat(accountId.toString());
        final BigDecimal amount_capital = amount;
        // Cobro
        // de
        // impuesto
        // sobre
        // intereses
        final String newValue_capital = null;
        Integer headingCode_capital = null;
        if (transaction.getSavingsAccountChargesPaid() != null) {
            for (SavingsAccountChargePaidBy chargePaidBy : transaction.getSavingsAccountChargesPaid()) {
                if (chargePaidBy.getSavingsAccountCharge() != null && chargePaidBy.getSavingsAccountCharge().getCharge() != null) {
                    headingCode_capital = chargePaidBy.getSavingsAccountCharge().getCharge().getId().intValue();
                    break;
                }
            }
        }
        final String amountType_capital = "CAPITAL";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading_capital, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsISR(SavingsAccountData account, SavingsAccountTransactionData transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = null;

        CodeCauseProcessMappingData causeProcessMappingData = null;
        Integer causal = null;
        String trxType = BitacoraMasterConstants.TRX_TYPE_ISRINT;
        if (account.depositAccountType().isFixedDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                    LumaProccessId.WITHHOLD_TAX_INTEREST_POSTING_FIXED_DEPOSIT.getValue(), currencyData.getIntCode());
        } else if (account.depositAccountType().isSavingsDeposit()) {
            causeProcessMappingData = getCausalFromProccessIdAndCurrencyIntCode(
                    LumaProccessId.WITHHOLD_TAX_INTEREST_POSTING_SAVINGS.getValue(), currencyData.getIntCode());
        }

        if (causeProcessMappingData != null) {
            causal = causeProcessMappingData.getCausalCode();
            trxType = causeProcessMappingData.getLogTransactionType();
        }

        // set causal for the current transaction
        // if (causal != null) {
        // transaction.setPaymentCauseId(causal);
        // }

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);
        master.setClientType(STRING_CLIENT_TYPE_A);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();

        final String heading_capital = "ND a cuenta: ".concat(accountId.toString());
        final BigDecimal amount_capital = amount;
        // Cobro
        // de
        // impuesto
        // sobre
        // intereses
        final String newValue_capital = null;
        Integer headingCode_capital = null;
        if (transaction.getSavingsAccountChargesPaid() != null) {
            for (SavingsAccountChargesPaidByData chargePaidBy : transaction.getSavingsAccountChargesPaid()) {
                if (chargePaidBy.getSavingsAccountCharge() != null && chargePaidBy.getSavingsAccountCharge().getCharge() != null) {
                    headingCode_capital = chargePaidBy.getSavingsAccountCharge().getCharge().getId().intValue();
                    break;
                }
            }
        }
        final String amountType_capital = "CAPITAL";
        final Integer destinationAccount_capital = null;
        final Integer destinationBank_capital = null;
        final String destinationClientType_capital = null;
        final BitaCoraMaster master_capital = master;
        final BigDecimal differential_capital = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading_capital, amount_capital, causal, newValue_capital, headingCode_capital,
                amountType_capital, destinationAccount_capital, destinationBank_capital, destinationClientType_capital, master_capital,
                differential_capital);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsReserveTransactionalAmount(SavingsAccount account,
            SavingsAccountTransaction transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        Integer causal = 1007;
        // Integer causal = transaction.getPaymentCauseId() != null && transaction.getPaymentCauseId() > 0 ?
        // transaction.getPaymentCauseId()
        // : 1007;
        String trxType = BitacoraMasterConstants.TRX_TYPE_BLC;

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
        final String heading = "Bloqueo a cuenta: ".concat(accountId.toString());
        final String newValue = Integer.toString(causal);
        Integer headingCode = null;
        final String amountType = "CAPITAL";
        final Integer destinationAccount = null;
        final Integer destinationBank = null;
        final String destinationClientType_capital = null;
        final BigDecimal differential = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading, amount, causal, newValue, headingCode, amountType, destinationAccount,
                destinationBank, destinationClientType_capital, master, differential);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    public BitaCoraMaster createJournalEntriesForSavingsReleaseTransactionalAmount(SavingsAccount account,
            SavingsAccountTransaction transaction) {
        final Date transactionDate = Date
                .from(transaction.getTransactionLocalDate().atStartOfDay(DateUtils.getDateTimeZoneOfTenant()).toInstant());
        CurrencyData currencyData = this.currencyReadPlatformService.retrieveCurrency(account.getCurrency().getCode());
        final BigDecimal exchangeRate = getExchangeRate(currencyData.code(), transactionDate);
        String currencyString = currencyData.getIntCode().toString();
        final String accountType = getAccountTypeFromSavingsAccount(account);
        final String group = "PU";
        Long transactionId = transaction.getId();
        final BigDecimal amount = transaction.getAmount();
        final Long accountId = account.getId();
        final Long clientId = account.getLumaClientId();
        final String status = getStatusStringFromSavings(account);
        final Office office = getOffice(account);

        Integer causal = 1007;
        // Integer causal = transaction.getPaymentCauseId() != null && transaction.getPaymentCauseId() > 0 ?
        // transaction.getPaymentCauseId()
        // : 1007;
        String trxType = BitacoraMasterConstants.TRX_TYPE_DBLC;

        BitaCoraMaster master = new BitaCoraMaster(transactionDate, trxType, transactionId, accountId, accountType, currencyString, amount,
                status, group, null);
        master.setExchangeRate(exchangeRate);
        master.setOffice(office);
        master.setClientId(clientId);

        List<BitaCoraDetails> details = new ArrayList<BitaCoraDetails>();
        final String heading = "Desbloqueo a cuenta: ".concat(accountId.toString());
        final String newValue = Integer.toString(causal);
        Integer headingCode = null;
        final String amountType = "CAPITAL";
        final Integer destinationAccount = null;
        final Integer destinationBank = null;
        final String destinationClientType_capital = null;
        final BigDecimal differential = amount.multiply(exchangeRate);
        BitaCoraDetails capitalDetails = new BitaCoraDetails(heading, amount, causal, newValue, headingCode, amountType, destinationAccount,
                destinationBank, destinationClientType_capital, master, differential);
        details.add(capitalDetails);

        master.setBitaCoraDetails(details);
        return master;
    }

    @Nullable
    private static Office getOffice(SavingsAccount account) {
        Office office;
        if (Objects.nonNull(account.getClient()) && Objects.nonNull(account.getClient().getOffice())) {
            office = account.getClient().getOffice();
        } else if (Objects.nonNull(account.getGroup()) && Objects.nonNull(account.getGroup().getOffice())) {
            office = account.getGroup().getOffice();
        } else {
            office = null;
        }
        return office;
    }

    public void reverseLogForTransactionId(Long savingsTransactionId) {
        this.reverseLogForTransactionId(savingsTransactionId, BitacoraMasterConstants.ACCOUNT_TYPE_AH);
    }
}
