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
package org.apache.fineract.portfolio.cupo.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformDataIntegrityException;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.organisation.office.domain.OrganisationCurrency;
import org.apache.fineract.portfolio.client.domain.Client;
import org.apache.fineract.portfolio.cupo.api.CupoApiConstants;
import org.apache.fineract.portfolio.group.domain.Group;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "m_cupo")
public class Cupo extends AbstractAuditableCustom {

    @Column(name = "amount_submitted", nullable = false, scale = 6, precision = 19)
    private BigDecimal amountSubmitted;
    @Column(name = "amount_approved", scale = 6, precision = 19)
    private BigDecimal amountApproved;

    @Temporal(TemporalType.DATE)
    @Column(name = "expiration_date", nullable = false)
    private Date expirationDate;
    @Temporal(TemporalType.DATE)
    @Column(name = "approval_date")
    private Date approvalDate;
    @Temporal(TemporalType.DATE)
    @Column(name = "reject_date")
    private Date rejectDate;
    @ManyToOne
    @JoinColumn(name = "client_id", referencedColumnName = "id")
    private Client client;
    @ManyToOne
    @JoinColumn(name = "group_id", referencedColumnName = "id")
    private Group group;
    @Column(name = "status_enum")
    protected Integer status;
    @Column(name = "amount", scale = 6, precision = 19)
    private BigDecimal amount;
    @Column(name = "amount_available", scale = 6, precision = 19)
    private BigDecimal amountAvailable;
    @Column(name = "amount_in_hold", scale = 6, precision = 19)
    private BigDecimal amountInHold;
    @Column(name = "currency_code", length = 3, nullable = false)
    private String currencyCode;
    @OrderBy(value = "transactionDate, id")
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "cupo", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<CupoTransaction> transactions = new ArrayList<>();

    protected Cupo() {
        //
    }

    public static Cupo fromJson(JsonCommand command, Client client, Group group, OrganisationCurrency currency) {
        BigDecimal amount = command.bigDecimalValueOfParameterNamed(CupoApiConstants.amountParamName);
        LocalDate expirationDate = command.localDateValueOfParameterNamed(CupoApiConstants.expirationDateParamName);
        return new Cupo(amount, DateUtils.localDateToDate(expirationDate), client, group,
                CupoStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue(), currency.getCode());
    }

    public Cupo(BigDecimal amountSubmitted, Date expirationDate, Client client, Group group, Integer status, String currencyCode) {
        this.amountSubmitted = amountSubmitted;
        this.expirationDate = expirationDate;
        this.client = client;
        this.group = group;
        this.status = status;
        this.amount = amountSubmitted;
        this.currencyCode = currencyCode;
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        if (command.isChangeInBigDecimalParameterNamed(CupoApiConstants.amountParamName, this.amountSubmitted)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(CupoApiConstants.amountParamName, command.extractLocale());
            actualChanges.put(CupoApiConstants.amountParamName, newValue);
            this.amountSubmitted = newValue;
        }

        if (command.isChangeInDateParameterNamed(CupoApiConstants.expirationDateParamName, this.expirationDate)) {
            final Date newValue = command.dateValueOfParameterNamed2(CupoApiConstants.expirationDateParamName);
            actualChanges.put(CupoApiConstants.expirationDateParamName, newValue);
            this.expirationDate = newValue;
        }

        return actualChanges;
    }

    public Map<String, Object> extension(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);
        BigDecimal addedAmount = BigDecimal.ZERO;
        if (command.isChangeInBigDecimalParameterNamed(CupoApiConstants.amountParamName, this.amount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(CupoApiConstants.amountParamName, command.extractLocale());
            if (newValue.compareTo(this.amount) <= 0) {
                throw new PlatformDataIntegrityException("error.msg.cupo.extension.amount.less.than.current.amount",
                        "New amount can't less than current amount", CupoApiConstants.amountParamName, newValue, this.amount);
            }
            actualChanges.put(CupoApiConstants.amountParamName, newValue);
            addedAmount = newValue.subtract(this.amount);
            this.amount = newValue;
            this.amountAvailable = this.amountAvailable.add(addedAmount);
        }

        if (command.isChangeInDateParameterNamed(CupoApiConstants.expirationDateParamName, this.expirationDate)) {
            final LocalDate newValue = command.localDateValueOfParameterNamed(CupoApiConstants.expirationDateParamName);
            final LocalDate currentExpirationLocalDate = DateUtils.dateToLocalDate(this.expirationDate);
            if (!newValue.isAfter(currentExpirationLocalDate)) {
                throw new PlatformDataIntegrityException("error.msg.cupo.expirationdate.before.current.expirationdate",
                        "New expiration date should be after current expiration date", CupoApiConstants.expirationDateParamName, newValue,
                        currentExpirationLocalDate);
            }
            actualChanges.put(CupoApiConstants.expirationDateParamName, newValue);
            this.expirationDate = DateUtils.localDateToDate(newValue);
        }

        this.transactions.add(CupoTransaction.extensionTransaction(addedAmount, this));
        this.checkAmounts();
        return actualChanges;
    }

    public boolean isWaitingForApproval() {
        return CupoStatus.fromInt(this.status).isWaitingForApproval();
    }

    public boolean isActive() {
        return CupoStatus.fromInt(this.status).isActive();
    }

    public void approve(JsonCommand command) {
        BigDecimal amount = command.bigDecimalValueOfParameterNamed(CupoApiConstants.amountParamName);
        LocalDate approvalDate = command.localDateValueOfParameterNamed(CupoApiConstants.approvalDateParamName);
        if (!approvalDate.isBefore(DateUtils.dateToLocalDate(this.expirationDate))) {
            throw new PlatformDataIntegrityException("error.msg.cupo.approval.date.greater.than.expiration.date",
                    "Approval date cant be greater or equal to expiration date", CupoApiConstants.approvalDateParamName, approvalDate);
        }
        this.amountApproved = amount;
        this.approvalDate = DateUtils.localDateToDate(approvalDate);
        this.status = CupoStatus.ACTIVE.getValue();
        this.amount = amount;
        this.amountInHold = BigDecimal.ZERO;

        this.transactions.add(CupoTransaction.activationTransaction(amount, approvalDate, this));
        this.amountAvailable = amount;
    }

    public void reject(JsonCommand command) {
        LocalDate rejectDate = command.localDateValueOfParameterNamed(CupoApiConstants.rejectDateParamName);
        this.status = CupoStatus.REJECTED.getValue();
        this.rejectDate = DateUtils.localDateToDate(rejectDate);
        this.amount = BigDecimal.ZERO;
    }

    public Long getClientId() {
        if (this.client != null) {
            return this.client.getId();
        }
        return null;
    }

    public Long getGroupId() {
        if (this.group != null) {
            return this.group.getId();
        }
        return null;
    }

    public BigDecimal getAmountAvailable() {
        return amountAvailable;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void holdAmountFromLoanApproval(BigDecimal amountApproved, LocalDate approvalDate, Long loanId) {
        if (amountApproved.compareTo(this.amountAvailable) > 0) {
            throw new PlatformDataIntegrityException("error.msg.loan.disburse.amount.greater.than.available.cupo",
                    "Disbursed amount can not be greater than available amount of linked Cupo", CupoApiConstants.amountParamName,
                    this.amountAvailable, amountApproved);
        }
        this.amountAvailable = this.amountAvailable.subtract(amountApproved);
        this.amountInHold = this.amountInHold.add(amountApproved);
        CupoTransaction cupoTransaction = CupoTransaction.withdrawalFromLoanApproval(amountApproved, approvalDate, this);
        cupoTransaction.setLoanId(loanId);
        this.transactions.add(cupoTransaction);
        this.checkAmounts();
    }

    public void releaseAmountFromLoanRepayment(LoanTransaction loanRepaymentTransaction) {
        BigDecimal amountPaid = loanRepaymentTransaction.getPrincipalPortion();
        if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            this.amountAvailable = this.amountAvailable.add(amountPaid);
            if (this.amountAvailable.compareTo(this.amount) > 0) {
                this.amountAvailable = this.amount;
            }
            this.amountInHold = this.amountInHold.subtract(amountPaid);
            CupoTransaction cupoTransaction = CupoTransaction.depositFromLoanRepaymentTransaction(amountPaid,
                    loanRepaymentTransaction.getTransactionDate(), this);
            cupoTransaction.setLoanTransaction(loanRepaymentTransaction);
            this.transactions.add(cupoTransaction);
        }
        this.checkAmounts();
    }

    public void updateBalances() {
        BigDecimal amountAvailable = BigDecimal.ZERO;
        BigDecimal amountInHold = BigDecimal.ZERO;
        for (CupoTransaction cupoTransaction : this.transactions) {
            if (cupoTransaction.isReversed()) {
                continue;
            }
            if (cupoTransaction.isDeposit()) {
                amountAvailable = amountInHold.add(cupoTransaction.getAmount());
                if (cupoTransaction.getCupoTransactionType().equals(CupoTransactionType.DEPOSIT_FROM_LOAN_REPAYMENT)) {
                    amountInHold = amountAvailable.subtract(cupoTransaction.getAmount());
                }
            } else if (cupoTransaction.isWithdrawal()) {
                amountAvailable = amountAvailable.subtract(cupoTransaction.getAmount());
                if (cupoTransaction.getCupoTransactionType().equals(CupoTransactionType.WITHDRAWAL_FROM_LOAN_APPROVAL)) {
                    amountInHold = amountAvailable.add(cupoTransaction.getAmount());
                }
            }
        }

        this.amountAvailable = amountAvailable;
        this.amountInHold = amountInHold;
        this.checkAmounts();
    }

    public void cancel(LocalDate transactionDate) {
        if (this.amountInHold.compareTo(BigDecimal.ZERO) > 0) {
            // something is holding amount
            throw new PlatformDataIntegrityException("error.msg.cupo.cancel.error.amount.in.hold.greater.than.zero",
                    "Amount in hold can't get greater than zero", CupoApiConstants.amountParamName, this.amountInHold);
        }

        CupoTransaction cancelTransaction = CupoTransaction.withdrawalForCancelation(this.amountAvailable, transactionDate, this);
        this.transactions.add(cancelTransaction);

        this.amountAvailable = BigDecimal.ZERO;
        this.amount = BigDecimal.ZERO;
        this.status = CupoStatus.CANCELED.getValue();
    }

    public LocalDate getExpirationDate() {
        return DateUtils.dateToLocalDate(this.expirationDate);
    }

    public Map<String, Object> reduction(JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(1);
        BigDecimal substractedAmount = BigDecimal.ZERO;
        if (command.isChangeInBigDecimalParameterNamed(CupoApiConstants.amountParamName, this.amount)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(CupoApiConstants.amountParamName, command.extractLocale());
            if (newValue.compareTo(this.amount) >= 0) {
                throw new PlatformDataIntegrityException("error.msg.cupo.extension.amount.greater.than.current.amount",
                        "New amount can not bet greater than current amount", CupoApiConstants.amountParamName, newValue, this.amount);
            }
            actualChanges.put(CupoApiConstants.amountParamName, newValue);
            substractedAmount = this.amount.subtract(newValue);
            this.amount = newValue;
            if (this.amountAvailable.compareTo(BigDecimal.ZERO) > 0) {
                this.amountAvailable = this.amountAvailable.subtract(substractedAmount);
                if (this.amountAvailable.compareTo(BigDecimal.ZERO) < 0) {
                    this.amountAvailable = BigDecimal.ZERO;
                }
            }
        }

        this.transactions.add(CupoTransaction.reductionTransaction(substractedAmount, this));
        this.checkAmounts();
        return actualChanges;
    }

    public void checkAmounts() {
        if (this.amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new PlatformDataIntegrityException("error.msg.cupo.exception", "Current amount is zero or less",
                    CupoApiConstants.amountParamName, this.amount);
        }
        if (this.amountAvailable.compareTo(BigDecimal.ZERO) < 0) {
            throw new PlatformDataIntegrityException("error.msg.cupo.exception", "Current amount available is less than zero",
                    CupoApiConstants.amountParamName, this.amountAvailable);
        }
        if (this.amountAvailable.compareTo(this.amount) > 0) {
            throw new PlatformDataIntegrityException("error.msg.cupo.exception", "Current amount available is greater than total amount",
                    CupoApiConstants.amountParamName, this.amountAvailable, this.amount);
        }
        if (this.amountInHold.compareTo(BigDecimal.ZERO) < 0) {
            throw new PlatformDataIntegrityException("error.msg.cupo.exception", "Current amount in hold is less than zero",
                    CupoApiConstants.amountParamName, this.amountInHold);
        }
        if (this.amountInHold.compareTo(this.amount) > 0) {
            throw new PlatformDataIntegrityException("error.msg.cupo.exception", "Current amount in hold is greater than total amount",
                    CupoApiConstants.amountParamName, this.amountInHold, this.amount);
        }
        BigDecimal summaryAmount = this.amountAvailable.add(this.amountInHold);
        if (summaryAmount.compareTo(this.amount) != 0) {
            throw new PlatformDataIntegrityException("error.msg.cupo.exception", "Amount in hold and available not equal to total amount",
                    CupoApiConstants.amountParamName, this.amountAvailable, this.amountInHold, this.amount);
        }
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getAmountSubmitted() {
        return amountSubmitted;
    }

    public BigDecimal getAmountApproved() {
        return amountApproved;
    }

    public List<CupoTransaction> getTransactions() {
        return transactions;
    }
}
