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

import java.math.BigDecimal;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import org.apache.fineract.infrastructure.core.domain.AbstractAuditableCustom;

@Entity
@Table(name = "bitacora_details")
public class BitaCoraDetails extends AbstractAuditableCustom {

    @Column(name = "heading", length = 50)
    private String heading;

    @Column(name = "amount", scale = 6, precision = 19)
    private BigDecimal amount;

    @Column(name = "causal", nullable = false)
    private Integer causal;

    @Column(name = "new_value", length = 25)
    private String newValue;

    @Column(name = "heading_code")
    private Integer headingCode;

    @Column(name = "amount_type", length = 25)
    private String amountType;

    @Column(name = "destination_account")
    private Integer destinationAccount;

    @Column(name = "destination_bank")
    private Integer destinationBank;

    @Column(name = "destination_client_type", length = 3)
    private String destinationClientType;

    @ManyToOne(cascade = CascadeType.ALL, optional = false)
    @JoinColumn(name = "bitacora_master_id", nullable = false)
    private BitaCoraMaster master;

    @Column(name = "diferential", scale = 2, precision = 19)
    private BigDecimal differential;

    public BitaCoraDetails() {}

    public BitaCoraDetails(String heading, BigDecimal amount, Integer causal, String newValue, Integer headingCode, String amountType,
            Integer destinationAccount, Integer destinationBank, String destinationClientType, BitaCoraMaster master,
            BigDecimal differential) {
        this.heading = heading;
        this.amount = amount;
        this.causal = causal;
        this.newValue = newValue;
        this.headingCode = headingCode;
        this.amountType = amountType;
        this.destinationAccount = destinationAccount;
        this.destinationBank = destinationBank;
        this.destinationClientType = destinationClientType;
        this.master = master;
        this.differential = differential;
    }

    @Override
    public BitaCoraDetails clone() {
        return new BitaCoraDetails(this.heading, this.amount, this.causal, this.newValue, this.headingCode, this.amountType,
                this.destinationAccount, this.destinationBank, this.destinationClientType, null, this.differential);
    }

    public BitaCoraMaster getMaster() {
        return master;
    }

    public void setMaster(BitaCoraMaster master) {
        this.master = master;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getAmountType() {
        return amountType;
    }

    public boolean isGreaterThanZero() {
        return this.getAmount().compareTo(BigDecimal.ZERO) > 0;
    }

    public BigDecimal getDifferential() {
        return differential;
    }

    public void setDifferential(BigDecimal differential) {
        this.differential = differential;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
