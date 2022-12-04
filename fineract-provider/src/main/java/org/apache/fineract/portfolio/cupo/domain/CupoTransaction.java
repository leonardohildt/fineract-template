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
import java.util.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.infrastructure.core.service.DateUtils;
import org.apache.fineract.portfolio.loanaccount.domain.LoanTransaction;
import org.springframework.stereotype.Component;

@Entity
@Component
@Table(name = "m_cupo_transaction")
public class CupoTransaction extends AbstractAuditableCustom {

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "cupo_id", nullable = false)
    private Cupo cupo;
    @Column(name = "amount", nullable = false, scale = 6, precision = 19)
    private BigDecimal amount;
    @Temporal(TemporalType.DATE)
    @Column(name = "transaction_date", nullable = false)
    private Date transactionDate;
    @Column(name = "transaction_type_enum", nullable = false)
    private Integer typeOf;
    @Column(name = "is_reversed", nullable = false)
    private boolean reversed;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loan_transaction_id")
    private LoanTransaction loanTransaction;
    @Column(name = "loan_id")
    private Long loanId;

    protected CupoTransaction() {
        //
    }

    public static CupoTransaction activationTransaction(BigDecimal amount, LocalDate activationDate, Cupo cupo) {
        return new CupoTransaction(amount, DateUtils.localDateToDate(activationDate), CupoTransactionType.DEPOSIT_FROM_APPROVAL.getValue(),
                false, cupo);
    }

    public static CupoTransaction extensionTransaction(BigDecimal amount, Cupo cupo) {
        return new CupoTransaction(amount, DateUtils.getDateOfTenant(), CupoTransactionType.DEPOSIT_FROM_EXTENSION.getValue(), false, cupo);
    }

    public static CupoTransaction withdrawalFromLoanApproval(BigDecimal amount, LocalDate transactionDate, Cupo cupo) {
        return new CupoTransaction(amount, DateUtils.localDateToDate(transactionDate),
                CupoTransactionType.WITHDRAWAL_FROM_LOAN_APPROVAL.getValue(), false, cupo);
    }

    private CupoTransaction(BigDecimal amount, Date transactionDate, Integer typeOf, boolean reversed, Cupo cupo) {
        this.amount = amount;
        this.transactionDate = transactionDate;
        this.typeOf = typeOf;
        this.reversed = reversed;
        this.cupo = cupo;
    }

    public static CupoTransaction depositFromLoanRepaymentTransaction(BigDecimal amountPaid, LocalDate transactionDate, Cupo cupo) {
        return new CupoTransaction(amountPaid, DateUtils.localDateToDate(transactionDate),
                CupoTransactionType.DEPOSIT_FROM_LOAN_REPAYMENT.getValue(), false, cupo);
    }

    public static CupoTransaction withdrawalForCancelation(BigDecimal amount, LocalDate transactionDate, Cupo cupo) {
        return new CupoTransaction(amount, DateUtils.localDateToDate(transactionDate),
                CupoTransactionType.WITHDRAWAL_FROM_CANCELATION.getValue(), false, cupo);
    }

    public static CupoTransaction reductionTransaction(BigDecimal substractedAmount, Cupo cupo) {
        return new CupoTransaction(substractedAmount, DateUtils.getDateOfTenant(), CupoTransactionType.WITHDRAWAL_REDUCTION.getValue(),
                false, cupo);
    }

    public void setLoanTransaction(LoanTransaction loanTransaction) {
        this.loanTransaction = loanTransaction;
    }

    public void reverse() {
        this.reversed = true;
    }

    public Cupo getCupo() {
        return cupo;
    }

    public boolean isDeposit() {
        return CupoTransactionType.fromInt(this.typeOf).isDeposit();
    }

    public boolean isWithdrawal() {
        return CupoTransactionType.fromInt(this.typeOf).isWithdrawal();
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public boolean isReversed() {
        return reversed;
    }

    public CupoTransactionType getCupoTransactionType() {
        return CupoTransactionType.fromInt(this.typeOf);
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getLoanId() {
        return loanId;
    }
}
