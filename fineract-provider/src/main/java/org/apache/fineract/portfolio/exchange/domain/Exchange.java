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
package org.apache.fineract.portfolio.exchange.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.domain.AbstractPersistableCustom;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.portfolio.exchange.api.ExchangeApiConstants;
import org.apache.fineract.useradministration.domain.AppUser;

@Entity
@Table(name = "m_exchange")
public final class Exchange extends AbstractPersistableCustom {

    @Column(name = "MonedaDestino")
    private Integer destinationCurrency;

    @Column(name = "MonedaOrigen")
    private Integer originCurrency;

    @Column(name = "TipoCambio", scale = 6, precision = 19, nullable = false)
    private BigDecimal exchangeRate;

    @Temporal(TemporalType.DATE)
    @Column(name = "AplicaDesde")
    private Date validFrom;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "FechaIngreso", columnDefinition = "DATETIME")
    private Date createdOn;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "UsuarioIngreso")
    private AppUser createdBy;

    Exchange() {
        this.destinationCurrency = 0;
        this.originCurrency = 0;
        this.exchangeRate = BigDecimal.ZERO;
        this.validFrom = null;
        this.createdOn = new Date();
        this.createdBy = null;
    }

    public Exchange(Integer destinationCurrency, Integer originCurrency, BigDecimal exchangeRate, Date validFrom, Date createdOn,
            AppUser createdBy) {
        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        this.destinationCurrency = destinationCurrency;
        this.originCurrency = originCurrency;
        this.exchangeRate = exchangeRate;
        this.validFrom = validFrom;
        this.createdOn = createdOn;
        this.createdBy = createdBy;

        throwExceptionIfErrors(dataValidationErrors);
    }

    public static Exchange createNew(JsonCommand command) {
        final Integer destinationCurrency = command.integerValueOfParameterNamed(ExchangeApiConstants.destinationCurrencyParamName,
                Locale.getDefault());
        final Integer originCurrency = command.integerValueOfParameterNamed(ExchangeApiConstants.originCurrencyParamName,
                Locale.getDefault());
        final BigDecimal exchangeRate = command.bigDecimalValueOfParameterNamed(ExchangeApiConstants.exchangeRateParamName,
                Locale.getDefault());
        final Date validFrom = convertToDateViaInstant(command.localDateValueOfParameterNamed(ExchangeApiConstants.validFromParamName));

        return new Exchange(destinationCurrency, originCurrency, exchangeRate, validFrom, new Date(), null);
    }

    public Map<String, Object> update(final JsonCommand command) {
        final Map<String, Object> actualChanges = new LinkedHashMap<>(9);

        if (command.isChangeInIntegerParameterNamed(ExchangeApiConstants.destinationCurrencyParamName, this.destinationCurrency)) {
            final Integer newValue = command.integerValueOfParameterNamed(ExchangeApiConstants.destinationCurrencyParamName,
                    Locale.getDefault());
            actualChanges.put(ExchangeApiConstants.destinationCurrencyParamName, newValue);
            this.destinationCurrency = newValue;
        }

        if (command.isChangeInIntegerParameterNamed(ExchangeApiConstants.originCurrencyParamName, this.originCurrency)) {
            final Integer newValue = command.integerValueOfParameterNamed(ExchangeApiConstants.originCurrencyParamName,
                    Locale.getDefault());
            actualChanges.put(ExchangeApiConstants.originCurrencyParamName, newValue);
            this.originCurrency = newValue;
        }

        if (command.isChangeInBigDecimalParameterNamed(ExchangeApiConstants.exchangeRateParamName, this.exchangeRate)) {
            final BigDecimal newValue = command.bigDecimalValueOfParameterNamed(ExchangeApiConstants.exchangeRateParamName,
                    Locale.getDefault());
            actualChanges.put(ExchangeApiConstants.exchangeRateParamName, newValue);
            this.exchangeRate = newValue;
        }

        if (command.isChangeInDateParameterNamed(ExchangeApiConstants.validFromParamName, this.validFrom)) {
            final Date newValue = convertToDateViaInstant(command.localDateValueOfParameterNamed(ExchangeApiConstants.validFromParamName));
            actualChanges.put(ExchangeApiConstants.validFromParamName, newValue);
            this.validFrom = newValue;
        }

        Date now = new Date();
        actualChanges.put(ExchangeApiConstants.createdOnDataParamName, now);
        this.createdOn = now;

        return actualChanges;
    }

    private static Date convertToDateViaInstant(LocalDate dateToConvert) {
        return java.util.Date.from(dateToConvert.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant());
    }

    private void throwExceptionIfErrors(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public Integer getDestinationCurrency() {
        return destinationCurrency;
    }

    public void setDestinationCurrency(Integer destinationCurrency) {
        this.destinationCurrency = destinationCurrency;
    }

    public Integer getOriginCurrency() {
        return originCurrency;
    }

    public void setOriginCurrency(Integer originCurrency) {
        this.originCurrency = originCurrency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public Date getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Date createdOn) {
        this.createdOn = createdOn;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }
}
