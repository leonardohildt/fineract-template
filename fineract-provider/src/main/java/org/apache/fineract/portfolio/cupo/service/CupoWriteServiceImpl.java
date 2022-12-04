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
package org.apache.fineract.portfolio.cupo.service;

import static org.apache.fineract.portfolio.cupo.api.CupoApiConstants.currencyCodeParamName;

import java.time.LocalDate;
import java.util.Map;
import javax.annotation.PostConstruct;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMaster;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMasterRepository;
import org.apache.fineract.accounting.journalentry.service.LumaAccountingProcessorForLoan;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.organisation.office.domain.OrganisationCurrency;
import org.apache.fineract.organisation.office.domain.OrganisationCurrencyRepositoryWrapper;
import org.apache.fineract.portfolio.account.domain.AccountAssociationType;
import org.apache.fineract.portfolio.account.domain.AccountAssociations;
import org.apache.fineract.portfolio.account.domain.AccountAssociationsRepository;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.client.domain.ClientRepositoryWrapper;
import org.apache.fineract.portfolio.common.BusinessEventNotificationConstants;
import org.apache.fineract.portfolio.common.service.BusinessEventListener;
import org.apache.fineract.portfolio.common.service.BusinessEventNotifierService2;
import org.apache.fineract.portfolio.cupo.api.CupoApiConstants;
import org.apache.fineract.portfolio.cupo.domain.Cupo;
import org.apache.fineract.portfolio.cupo.domain.CupoRepositoryWrapper;
import org.apache.fineract.portfolio.cupo.domain.CupoTransaction;
import org.apache.fineract.portfolio.cupo.domain.CupoTransactionRepository;
import org.apache.fineract.portfolio.cupo.exception.CupoNotInActiveStateException;
import org.apache.fineract.portfolio.cupo.exception.CupoNotWaitingForApprovalCannotBeModified;
import org.apache.fineract.portfolio.cupo.serialization.CupoCommandFromApiJsonDeserializer;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.group.domain.GroupRepositoryWrapper;
import org.apache.fineract.portfolio.loanaccount.domain.Loan;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class CupoWriteServiceImpl implements CupoWriteService {

    private CupoCommandFromApiJsonDeserializer cupoCommandFromApiJsonDeserializer;
    private ClientRepositoryWrapper clientRepositoryWrapper;
    private GroupRepositoryWrapper groupRepositoryWrapper;
    private CupoRepositoryWrapper cupoRepository;
    private OrganisationCurrencyRepositoryWrapper organisationCurrencyRepositoryWrapper;
    private final BusinessEventNotifierService2 businessEventNotifierService;
    private final AccountAssociationsRepository accountAssociationsRepository;
    private final CupoTransactionRepository cupoTransactionRepository;
    private final BitaCoraMasterRepository bitaCoraMasterRepository;
    private final LumaAccountingProcessorForLoan lumaAccountingProcessorForLoan;

    @Autowired
    public CupoWriteServiceImpl(CupoCommandFromApiJsonDeserializer cupoCommandFromApiJsonDeserializer,
            ClientRepositoryWrapper clientRepositoryWrapper, GroupRepositoryWrapper groupRepositoryWrapper,
            CupoRepositoryWrapper cupoRepository, OrganisationCurrencyRepositoryWrapper organisationCurrencyRepositoryWrapper,
            final BusinessEventNotifierService2 businessEventNotifierService,
            final AccountAssociationsRepository accountAssociationsRepository, final CupoTransactionRepository cupoTransactionRepository,
            final BitaCoraMasterRepository bitaCoraMasterRepository, final LumaAccountingProcessorForLoan lumaAccountingProcessorForLoan) {
        this.cupoCommandFromApiJsonDeserializer = cupoCommandFromApiJsonDeserializer;
        this.clientRepositoryWrapper = clientRepositoryWrapper;
        this.groupRepositoryWrapper = groupRepositoryWrapper;
        this.cupoRepository = cupoRepository;
        this.organisationCurrencyRepositoryWrapper = organisationCurrencyRepositoryWrapper;
        this.businessEventNotifierService = businessEventNotifierService;
        this.accountAssociationsRepository = accountAssociationsRepository;
        this.cupoTransactionRepository = cupoTransactionRepository;
        this.bitaCoraMasterRepository = bitaCoraMasterRepository;
        this.lumaAccountingProcessorForLoan = lumaAccountingProcessorForLoan;
    }

    @PostConstruct
    public void addListeners() {
        this.businessEventNotifierService.addBusinessEventPostListeners(BusinessEventNotificationConstants.BusinessEvents.LOAN_APPROVED,
                new OnLoanApprovalTransactionEvent());
        this.businessEventNotifierService.addBusinessEventPostListeners(
                BusinessEventNotificationConstants.BusinessEvents.LOAN_UNDO_APPROVAL, new OnLoanUndoApprovalTransactionEvent());
        this.businessEventNotifierService.addBusinessEventPostListeners(
                BusinessEventNotificationConstants.BusinessEvents.LOAN_MAKE_REPAYMENT, new OnLoanRepaymentTransactionEvent());
        this.businessEventNotifierService.addBusinessEventPostListeners(BusinessEventNotificationConstants.BusinessEvents.LOAN_FORECLOSURE,
                new OnLoanRepaymentTransactionEvent());
    }

    @Override
    public CommandProcessingResult createCupo(JsonCommand command) {
        this.cupoCommandFromApiJsonDeserializer.validateForCreate(command.json());
        Long clientId = command.longValueOfParameterNamed(CupoApiConstants.clientIdParamName);
        Client client = null;
        if (clientId != null) {
            client = clientRepositoryWrapper.findOneWithNotFoundDetection(clientId);
        }
        Long groupId = command.longValueOfParameterNamed(CupoApiConstants.groupIdParamName);
        Group group = null;
        if (groupId != null) {
            group = groupRepositoryWrapper.findOneWithNotFoundDetection(groupId);
        }
        String currencyCode = command.stringValueOfParameterNamed(currencyCodeParamName);
        OrganisationCurrency currency = this.organisationCurrencyRepositoryWrapper.findOneWithNotFoundDetection(currencyCode);
        Cupo cupo = Cupo.fromJson(command, client, group, currency);
        this.cupoRepository.saveAndFlush(cupo);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupo.getId()) //
                .build();
    }

    @Override
    public CommandProcessingResult updateCupo(Long cupoId, JsonCommand command) {
        this.cupoCommandFromApiJsonDeserializer.validateForUpdate(command.json());
        Cupo cupo = this.cupoRepository.findOneWithNotFoundDetection(cupoId);
        if (!cupo.isWaitingForApproval()) {
            throw new CupoNotWaitingForApprovalCannotBeModified(cupoId);
        }
        final Map<String, Object> changes = cupo.update(command);

        this.cupoRepository.saveAndFlush(cupo);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupoId) //
                .with(changes) //
                .build();
    }

    @Override
    public CommandProcessingResult approveCupo(Long cupoId, JsonCommand command) {
        Cupo cupo = this.cupoRepository.findOneWithNotFoundDetection(cupoId);
        if (!cupo.isWaitingForApproval()) {
            throw new CupoNotWaitingForApprovalCannotBeModified(cupoId);
        }
        cupo.approve(command);
        this.cupoRepository.save(cupo);

        BitaCoraMaster bitaCoraMaster = this.lumaAccountingProcessorForLoan.createJournalEntryForCupoApprove(cupo);
        this.bitaCoraMasterRepository.save(bitaCoraMaster);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupoId) //
                .build();
    }

    @Override
    public CommandProcessingResult rejectCupo(Long cupoId, JsonCommand command) {
        Cupo cupo = this.cupoRepository.findOneWithNotFoundDetection(cupoId);
        if (!cupo.isWaitingForApproval()) {
            throw new CupoNotWaitingForApprovalCannotBeModified(cupoId);
        }
        cupo.reject(command);
        this.cupoRepository.save(cupo);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupoId) //
                .build();
    }

    @Override
    public CommandProcessingResult extensionCupo(Long cupoId, JsonCommand command) {
        Cupo cupo = this.cupoRepository.findOneWithNotFoundDetection(cupoId);
        if (!cupo.isActive()) {
            throw new CupoNotInActiveStateException(cupoId);
        }
        var oldAmount = cupo.getAmount();
        final Map<String, Object> changes = cupo.extension(command);

        this.cupoRepository.saveAndFlush(cupo);

        var diff = cupo.getAmount().subtract(oldAmount);
        BitaCoraMaster bitaCoraMaster = this.lumaAccountingProcessorForLoan.createJournalEntryForCupoExtension(cupo, diff);
        this.bitaCoraMasterRepository.save(bitaCoraMaster);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupoId) //
                .with(changes) //
                .build();
    }

    @Override
    public CommandProcessingResult reductionCupo(Long cupoId, JsonCommand command) {
        Cupo cupo = this.cupoRepository.findOneWithNotFoundDetection(cupoId);
        if (!cupo.isActive()) {
            throw new CupoNotInActiveStateException(cupoId);
        }
        var oldAmount = cupo.getAmount();
        final Map<String, Object> changes = cupo.reduction(command);

        this.cupoRepository.saveAndFlush(cupo);

        var diff = oldAmount.subtract(cupo.getAmount());
        BitaCoraMaster bitaCoraMaster = this.lumaAccountingProcessorForLoan.createJournalEntryForCupoReduction(cupo, diff);
        this.bitaCoraMasterRepository.save(bitaCoraMaster);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupoId) //
                .with(changes) //
                .build();
    }

    @Override
    public CommandProcessingResult cancelCupo(Long cupoId, JsonCommand command) {
        Cupo cupo = this.cupoRepository.findOneWithNotFoundDetection(cupoId);
        if (!cupo.isActive()) {
            throw new CupoNotInActiveStateException(cupoId);
        }
        LocalDate transactionDate = command.localDateValueOfParameterNamed("transactionDate");
        var oldAmountAvailable = cupo.getAmountAvailable();
        cupo.cancel(transactionDate);

        this.cupoRepository.saveAndFlush(cupo);

        BitaCoraMaster bitaCoraMaster = this.lumaAccountingProcessorForLoan.createJournalEntryForCupoCancel(cupo, oldAmountAvailable);
        this.bitaCoraMasterRepository.save(bitaCoraMaster);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(cupoId) //
                .build();
    }

    private class OnLoanApprovalTransactionEvent implements BusinessEventListener {

        @Override
        public void businessEventToBeExecuted(
                @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                AccountAssociations accountAssociations = accountAssociationsRepository.findByLoanIdAndType(loan.getId(),
                        AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
                if (accountAssociations != null && accountAssociations.linkedCupo() != null) {
                    Cupo cupo = accountAssociations.linkedCupo();
                    cupo.holdAmountFromLoanApproval(loan.getApprovedPrincipal(), loan.getExpectedDisbursedOnLocalDate(), loan.getId());
                    cupoRepository.save(cupo);
                }
            }
        }
    }

    private class OnLoanRepaymentTransactionEvent implements BusinessEventListener {

        @Override
        public void businessEventToBeExecuted(
                @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN_TRANSACTION);
            if (entity instanceof LoanTransaction) {
                LoanTransaction loanRepaymentTransaction = (LoanTransaction) entity;
                if (loanRepaymentTransaction != null) {
                    AccountAssociations accountAssociations = accountAssociationsRepository.findByLoanIdAndType(
                            loanRepaymentTransaction.getLoan().getId(), AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
                    if (accountAssociations != null && accountAssociations.linkedCupo() != null) {
                        Cupo cupo = accountAssociations.linkedCupo();
                        cupo.releaseAmountFromLoanRepayment(loanRepaymentTransaction);
                        cupoRepository.save(cupo);
                    }
                }
            }
        }
    }

    private class OnLoanUndoApprovalTransactionEvent implements BusinessEventListener {

        @Override
        public void businessEventToBeExecuted(
                @SuppressWarnings("unused") Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {}

        @Override
        public void businessEventWasExecuted(Map<BusinessEventNotificationConstants.BusinessEntity, Object> businessEventEntity) {
            Object entity = businessEventEntity.get(BusinessEventNotificationConstants.BusinessEntity.LOAN);
            if (entity instanceof Loan) {
                Loan loan = (Loan) entity;
                AccountAssociations accountAssociations = accountAssociationsRepository.findByLoanIdAndType(loan.getId(),
                        AccountAssociationType.LINKED_ACCOUNT_ASSOCIATION.getValue());
                if (accountAssociations != null && accountAssociations.linkedCupo() != null) {
                    Cupo cupo = accountAssociations.linkedCupo();
                    CupoTransaction lastDepositFromWithdrawalTransactions = null;
                    for (CupoTransaction cupoTransaction : cupo.getTransactions()) {
                        if (!cupoTransaction.isReversed() && cupoTransaction.getCupoTransactionType().isWithdrawalFromLoanApproval()
                                && cupoTransaction.getLoanId().equals(loan.getId())) {
                            lastDepositFromWithdrawalTransactions = cupoTransaction;
                        }
                    }

                    if (lastDepositFromWithdrawalTransactions != null) {
                        lastDepositFromWithdrawalTransactions.reverse();
                        cupoTransactionRepository.save(lastDepositFromWithdrawalTransactions);
                        cupo.updateBalances();
                        cupo.checkAmounts();
                    }
                }
            }
        }
    }
}
