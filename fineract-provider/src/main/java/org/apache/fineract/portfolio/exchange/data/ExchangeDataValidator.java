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
package org.apache.fineract.portfolio.exchange.data;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.apache.fineract.portfolio.exchange.api.ExchangeApiConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ExchangeDataValidator {

    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public ExchangeDataValidator(FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public void validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ExchangeApiConstants.CREATE_REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ExchangeApiConstants.EXCHANGE_RESOURCE_NAME);

        Integer destinationCurrency = this.fromApiJsonHelper
                .extractIntegerSansLocaleNamed(ExchangeApiConstants.destinationCurrencyParamName, element);
        baseDataValidator.reset().parameter(ExchangeApiConstants.destinationCurrencyParamName).value(destinationCurrency).notNull();

        Integer originCurrency = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ExchangeApiConstants.originCurrencyParamName,
                element);
        baseDataValidator.reset().parameter(ExchangeApiConstants.originCurrencyParamName).value(originCurrency).notNull();

        BigDecimal exchangeRate = this.fromApiJsonHelper.extractBigDecimalNamed(ExchangeApiConstants.exchangeRateParamName, element,
                Locale.getDefault());
        baseDataValidator.reset().parameter(ExchangeApiConstants.exchangeRateParamName).value(exchangeRate).notNull();

        LocalDate validFrom = this.fromApiJsonHelper.extractLocalDateNamed(ExchangeApiConstants.validFromParamName, element);
        baseDataValidator.reset().parameter(ExchangeApiConstants.validFromParamName).value(validFrom).notNull();

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException(dataValidationErrors);
        }
    }

    public void validateForUpdate(String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, ExchangeApiConstants.UPDATE_REQUEST_DATA_PARAMETERS);
        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();

        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors)
                .resource(ExchangeApiConstants.EXCHANGE_RESOURCE_NAME);

        if (this.fromApiJsonHelper.parameterExists(ExchangeApiConstants.destinationCurrencyParamName, element)) {
            Integer destinationCurrency = this.fromApiJsonHelper
                    .extractIntegerSansLocaleNamed(ExchangeApiConstants.destinationCurrencyParamName, element);
            baseDataValidator.reset().parameter(ExchangeApiConstants.destinationCurrencyParamName).value(destinationCurrency).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(ExchangeApiConstants.originCurrencyParamName, element)) {
            Integer originCurrency = this.fromApiJsonHelper.extractIntegerSansLocaleNamed(ExchangeApiConstants.originCurrencyParamName,
                    element);
            baseDataValidator.reset().parameter(ExchangeApiConstants.originCurrencyParamName).value(originCurrency).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(ExchangeApiConstants.exchangeRateParamName, element)) {
            BigDecimal exchangeRate = this.fromApiJsonHelper.extractBigDecimalNamed(ExchangeApiConstants.exchangeRateParamName, element,
                    Locale.getDefault());
            baseDataValidator.reset().parameter(ExchangeApiConstants.exchangeRateParamName).value(exchangeRate).notNull();
        }

        if (this.fromApiJsonHelper.parameterExists(ExchangeApiConstants.validFromParamName, element)) {
            LocalDate validFrom = this.fromApiJsonHelper.extractLocalDateNamed(ExchangeApiConstants.validFromParamName, element);
            baseDataValidator.reset().parameter(ExchangeApiConstants.validFromParamName).value(validFrom).notNull();
        }

        throwExceptionIfValidationWarningsExist(dataValidationErrors);
    }
}
