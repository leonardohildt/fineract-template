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
package org.apache.fineract.accounting.journalentry.domain;

import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_PREFIX_REVERSE;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;
import org.apache.fineract.organisation.office.domain.Office;

@Entity
@Table(name = "bitacora_master")
public class BitaCoraMaster extends AbstractAuditableCustom {

    @Column(name = "transaction_date")
    @Temporal(TemporalType.DATE)
    private Date transactionDate;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;// String Constant

    @Column(name = "transaction_id")
    private Long transaction;// loan or savings

    @Column(name = "account_id")
    private Long accountId;// loanId or savingsId

    @Column(name = "account_type")
    private String accountType;// constant String

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "amount", scale = 6, precision = 19)
    private BigDecimal amount;

    @Column(name = "status", length = 8)
    private String status;

    @Column(name = "group_type", length = 4)
    private String group;// loan -> loanpurpose

    @Column(name = "type_of_credit")
    private String typeOfCredit;// loanFund

    @Column(name = "category")
    private String category;// loanCategory

    @Column(name = "type_of_client", length = 100)
    private String clientType;

    @Column(name = "reference", length = 25)
    private String reference;

    @Column(name = "observations", length = 500)
    private String observations;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "office_id")
    private Office office;

    @Column(name = "client_id")
    private Long clientId;

    @Column(name = "ip_dir")
    private String ipDir;

    @Column(name = "terminal")
    private String terminal;

    @Column(name = "exchange_rate", scale = 6, precision = 19)
    private BigDecimal exchangeRate;

    @OneToMany(cascade = CascadeType.ALL, mappedBy = "master", orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BitaCoraDetails> bitaCoraDetails = new ArrayList<>();

    protected BitaCoraMaster() {
        //
    }

    public BitaCoraMaster(Date transactionDate, String transactionType, Long transaction, Long accountId, String accountType,
            String currency, BigDecimal amount, String status, String group, String typeOfCredit) {
        this.transactionDate = transactionDate;
        this.transactionType = transactionType;
        this.transaction = transaction;
        this.accountId = accountId;
        this.accountType = accountType;
        this.currency = currency;
        this.amount = amount;
        this.status = status;
        this.group = group;
        this.typeOfCredit = typeOfCredit;
        this.category = "A";
        this.clientType = "DELPUBLICO";
    }

    public BitaCoraMaster cloneForReverse() {
        StringBuilder transactionTypeStringBuilder = new StringBuilder();
        transactionTypeStringBuilder.append(TRX_TYPE_PREFIX_REVERSE);
        transactionTypeStringBuilder.append(" ");
        transactionTypeStringBuilder.append(this.transactionType);
        BitaCoraMaster newMasterLog = new BitaCoraMaster(this.transactionDate, transactionTypeStringBuilder.toString(), this.transaction,
                this.accountId, this.accountType, this.currency, this.amount, this.status, this.group, this.typeOfCredit);
        newMasterLog.setClientId(this.clientId);
        newMasterLog.setExchangeRate(this.exchangeRate.subtract(BigDecimal.ONE));
        newMasterLog.setOffice(this.office);
        newMasterLog.setClientType(this.clientType);

        if (Objects.nonNull(this.bitaCoraDetails)) {
            for (BitaCoraDetails detail : this.bitaCoraDetails) {
                BitaCoraDetails clonedDetail = detail.clone();
                clonedDetail.setMaster(newMasterLog);
                newMasterLog.addBitaCoraDetails(clonedDetail);
            }
        }

        return newMasterLog;
    }

    public void addBitaCoraDetails(BitaCoraDetails details) {
        this.bitaCoraDetails.add(details);
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getCurrency() {
        return currency;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getStatus() {
        return status;
    }

    public String getGroup() {
        return group;
    }

    public String getTypeOfCredit() {
        return typeOfCredit;
    }

    public String getCategory() {
        return category;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getIpDir() {
        return ipDir;
    }

    public void setIpDir(String ipDir) {
        this.ipDir = ipDir;
    }

    public String getTerminal() {
        return terminal;
    }

    public void setTerminal(String terminal) {
        this.terminal = terminal;
    }

    public List<BitaCoraDetails> getBitaCoraDetails() {
        Comparator<BitaCoraDetails> compareById = Comparator.comparing(BitaCoraDetails::getId);
        bitaCoraDetails = bitaCoraDetails.stream().sorted(compareById).collect(Collectors.toList());

        return bitaCoraDetails;
    }

    /**
     * For tests
     */
    public List<BitaCoraDetails> details() {
        return bitaCoraDetails;
    }

    public void setBitaCoraDetails(List<BitaCoraDetails> bitaCoraDetails) {
        this.bitaCoraDetails = bitaCoraDetails;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public boolean isGreaterThanZero() {
        return this.amount.compareTo(BigDecimal.ZERO) > 0;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate.add(BigDecimal.ONE);
    }

    public void setTransactionDate(Date transactionDate) {
        this.transactionDate = transactionDate;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public Long getTransaction() {
        return transaction;
    }

    public void setTransaction(Long transaction) {
        this.transaction = transaction;
    }

    public void setAccountType(String accountType) {
        this.accountType = accountType;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public void setTypeOfCredit(String typeOfCredit) {
        this.typeOfCredit = typeOfCredit;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Long getOfficeId() {
        return this.office.getId();
    }

    public Long getClientId() {
        return clientId;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setOffice(Office office) {
        this.office = office;
    }
}
