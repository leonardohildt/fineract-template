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
package org.apache.fineract.infrastructure.codes.domain;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "m_code_causal")
public class CodeCause implements Persistable<Integer>, Serializable {

    @Id
    private Integer id;

    @Column(name = "codigo_acrm")
    private Integer codigoAcrm;

    @Column(name = "description", length = 150)
    private String description;

    @Column(name = "short_description", length = 100)
    private String shortDescription;

    @Column(name = "operation_type", length = 1)
    private String operationType;

    @Column(name = "allow_overdraft")
    private Boolean allowOverdraft;

    @Column(name = "is_cash_operation")
    private Boolean isCashOperation;

    @Column(name = "send_acrm")
    private Boolean sendAcrm;

    @Column(name = "is_document")
    private Boolean isDocument;

    @Column(name = "currency_int_code")
    private Integer currencyCode;

    @Transient
    private Boolean isNew = false;

    public CodeCause() {
        //
    }

    public CodeCause(final Integer id, final Integer codigoAcrm, final String description, final String shortDescription,
            final String operationType, final Boolean allowOverdraft, final Boolean isCashOperation, final Boolean sendAcrm,
            final Boolean isDocument, final Integer currencyCode) {
        this.id = id;
        this.codigoAcrm = codigoAcrm;
        this.description = description;
        this.shortDescription = shortDescription;
        this.operationType = operationType;
        this.allowOverdraft = allowOverdraft;
        this.isCashOperation = isCashOperation;
        this.sendAcrm = sendAcrm;
        this.isDocument = isDocument;
        this.currencyCode = currencyCode;
    }

    @Override
    public Integer getId() {
        return id;
    }

    @Override
    @Transient
    public boolean isNew() {
        return isNew;
    }

    @Transient
    public CodeCause isNew(boolean isNew) {
        this.isNew = isNew;
        return this;
    }

    public Map<String, Object> update(final JsonCommand command) {

        final Map<String, Object> actualChanges = new LinkedHashMap<>(2);

        final String descriptionParamName = "description";
        if (command.isChangeInStringParameterNamed(descriptionParamName, this.description)) {
            final String newValue = command.stringValueOfParameterNamed(descriptionParamName);
            actualChanges.put(descriptionParamName, newValue);
            this.description = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String shortDescriptionParamName = "shortDescription";
        if (command.isChangeInStringParameterNamed(shortDescriptionParamName, this.shortDescription)) {
            final String newValue = command.stringValueOfParameterNamed(shortDescriptionParamName);
            actualChanges.put(shortDescriptionParamName, newValue);
            this.shortDescription = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String codigoAcrmParamName = "codigoAcrm";
        if (command.isChangeInIntegerSansLocaleParameterNamed(codigoAcrmParamName, this.codigoAcrm)) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(codigoAcrmParamName);
            actualChanges.put(codigoAcrmParamName, newValue);
            this.codigoAcrm = newValue;
        }

        final String operationTypeParamName = "operationType";
        if (command.isChangeInStringParameterNamed(operationTypeParamName, this.operationType)) {
            final String newValue = command.stringValueOfParameterNamed(operationTypeParamName);
            actualChanges.put(operationTypeParamName, newValue);
            this.operationType = StringUtils.defaultIfEmpty(newValue, null);
        }

        final String allowOverdraftParamName = "allowOverdraft";
        if (command.isChangeInBooleanParameterNamed(allowOverdraftParamName, this.allowOverdraft)) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(allowOverdraftParamName);
            actualChanges.put(allowOverdraftParamName, newValue);
            this.allowOverdraft = newValue;
        }

        final String isCashOperationParamName = "isCashOperation";
        if (command.isChangeInBooleanParameterNamed(isCashOperationParamName, this.isCashOperation)) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isCashOperationParamName);
            actualChanges.put(isCashOperationParamName, newValue);
            this.isCashOperation = newValue;
        }

        final String sendAcrmParamName = "sendAcrm";
        if (command.isChangeInBooleanParameterNamed(sendAcrmParamName, this.sendAcrm)) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(sendAcrmParamName);
            actualChanges.put(sendAcrmParamName, newValue);
            this.sendAcrm = newValue;
        }

        final String isDocumentParamName = "isDocument";
        if (command.isChangeInBooleanParameterNamed(isDocumentParamName, this.isDocument)) {
            final Boolean newValue = command.booleanPrimitiveValueOfParameterNamed(isDocumentParamName);
            actualChanges.put(isDocumentParamName, newValue);
            this.isDocument = newValue;
        }

        final String currencyCodeParamName = "currencyCode";
        if (command.isChangeInIntegerSansLocaleParameterNamed(currencyCodeParamName, this.currencyCode)) {
            final Integer newValue = command.integerValueSansLocaleOfParameterNamed(currencyCodeParamName);
            actualChanges.put(currencyCodeParamName, newValue);
            this.currencyCode = newValue;
        }

        return actualChanges;
    }

}
