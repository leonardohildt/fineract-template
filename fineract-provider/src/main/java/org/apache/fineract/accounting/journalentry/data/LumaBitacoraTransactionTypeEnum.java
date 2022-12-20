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
package org.apache.fineract.accounting.journalentry.data;

import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.ACCOUNT_TYPE_AH;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.ACCOUNT_TYPE_DV;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.ACCOUNT_TYPE_PR;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_APROBACION;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_COMPRA;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_FORMALIZACION;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_FORMALIZACION_MULTITRANCHES;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_SOBREGIROCR;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_SOBREGIRODB;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.TRX_TYPE_VENTA;

/**
 * Immutable data object represent loan and Savings status enumerations.
 */
@SuppressWarnings("unused")
public enum LumaBitacoraTransactionTypeEnum {

    SAVINGS_ACCOUNT_OVERDRAFT_CR("savings", TRX_TYPE_SOBREGIROCR, "Savings Overdraft details", ACCOUNT_TYPE_AH, "PU", "CAPITAL",
            "SOBREGIRO CUENTA: ", "ACTIVO"), SAVINGS_ACCOUNT_OVERDRAFT_DB("savings", TRX_TYPE_SOBREGIRODB, "Savings Overdraft details",
                    ACCOUNT_TYPE_AH, "PU", "CAPITAL", "SOBREGIRO CUENTA: ", "ACTIVO"),

    LOANS_APPROVAL("loans", TRX_TYPE_APROBACION, "Approval ", ACCOUNT_TYPE_PR, "1", "CAPITAL", "Aprobacion de monto no formalizado ",
            "VIG"), LOANS_FORMALIZATION("loans", TRX_TYPE_FORMALIZACION, "Formalization ", ACCOUNT_TYPE_PR, "1", "CAPITAL",
                    "Aprrobacion de monto formalizado ", "VIG"), LOANS_FORMALIZATION_MULTITRANCHE("loans",
                            TRX_TYPE_FORMALIZACION_MULTITRANCHES, "Formalization EG ", ACCOUNT_TYPE_PR, "1", "CAPITAL",
                            "Aprobacion de monto formalizado EG ", "VIG"),

    LOANS_FORMALIZATION_CUSTODY("loans", "ALTACUSTODIA", "Alta Custodia ", ACCOUNT_TYPE_PR, "1", "ALTACUSTODIA",
            "Alta Custodia de monto formalizado ", "VIG"),

    CURRENCY_SALE("cvlog", TRX_TYPE_VENTA, "Currency Sale", ACCOUNT_TYPE_DV, "1", "CAPITAL", "Currency Sale", "VIG"), CURRENCY_PURCHASE(
            "cvlog", TRX_TYPE_COMPRA, "Currency Purchase", ACCOUNT_TYPE_DV, "1", "CAPITAL", "Currency Purchase", "VIG");

    private final String entity;
    private final String code;
    private final String description;
    private final String accountType;
    private final String groupTYpe;
    private final String amountType;
    private final String heading;

    LumaBitacoraTransactionTypeEnum(String entity, String code, String description, String accountType, String groupTYpe, String amountType,
            String heading, String status) {
        this.entity = entity;
        this.code = code;
        this.description = description;
        this.accountType = accountType;
        this.groupTYpe = groupTYpe;
        this.amountType = amountType;
        this.heading = heading;
    }

    public String getEntity() {
        return entity;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public String getAccountType() {
        return accountType;
    }

    public String getGroupTYpe() {
        return groupTYpe;
    }

    public String getAmountType() {
        return amountType;
    }

    public String getHeading() {
        return heading;
    }

}
