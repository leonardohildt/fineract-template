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
package org.apache.fineract.infrastructure.codes.serialization;

import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.infrastructure.codes.domain.CodeCause;
import org.apache.fineract.infrastructure.core.data.ApiParameterError;
import org.apache.fineract.infrastructure.core.data.DataValidatorBuilder;
import org.apache.fineract.infrastructure.core.exception.InvalidJsonException;
import org.apache.fineract.infrastructure.core.exception.PlatformApiDataValidationException;
import org.apache.fineract.infrastructure.core.serialization.FromJsonHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deserializer for code JSON to validate API request.
 */
@Component
public final class CodeCauseCommandFromApiJsonDeserializer {

    /**
     * The parameters supported for this command.
     */
    private final Set<String> supportedParameters = new HashSet<>(Arrays.asList("id", "codigoAcrm", "description", "shortDescription",
            "operationType", "allowOverdraft", "isCashOperation", "sendAcrm", "isDocument", "currencyCode", "isShownInCashierModule"));
    private final FromJsonHelper fromApiJsonHelper;

    @Autowired
    public CodeCauseCommandFromApiJsonDeserializer(final FromJsonHelper fromApiJsonHelper) {
        this.fromApiJsonHelper = fromApiJsonHelper;
    }

    public CodeCause validateForCreate(final String json) {
        if (StringUtils.isBlank(json)) {
            throw new InvalidJsonException();
        }

        final Type typeOfMap = new TypeToken<Map<String, Object>>() {}.getType();
        this.fromApiJsonHelper.checkForUnsupportedParameters(typeOfMap, json, this.supportedParameters);

        final List<ApiParameterError> dataValidationErrors = new ArrayList<>();
        final DataValidatorBuilder baseDataValidator = new DataValidatorBuilder(dataValidationErrors).resource("code");

        final JsonElement element = this.fromApiJsonHelper.parse(json);

        final Integer id = this.fromApiJsonHelper.extractIntegerNamed("id", element, Locale.getDefault());
        baseDataValidator.reset().parameter("id").value(id).notNull().notLessThanMin(1);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final Integer codigoAcrm = this.fromApiJsonHelper.extractIntegerNamed("codigoAcrm", element, Locale.getDefault());
        baseDataValidator.reset().parameter("codigoAcrm").value(codigoAcrm).notNull().notLessThanMin(1);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final String description = this.fromApiJsonHelper.extractStringNamed("description", element);
        baseDataValidator.reset().parameter("description").value(description).notBlank().notExceedingLengthOf(150);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final String shortDescription = this.fromApiJsonHelper.extractStringNamed("shortDescription", element);
        baseDataValidator.reset().parameter("shortDescription").value(shortDescription).notBlank().notExceedingLengthOf(100);
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final String operationType = this.fromApiJsonHelper.extractStringNamed("operationType", element);
        baseDataValidator.reset().parameter("operationType").value(operationType).notBlank().notExceedingLengthOf(1).isOneOfTheseValues("A",
                "C", "D");
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final Boolean allowOverdraft = this.fromApiJsonHelper.extractBooleanNamed("allowOverdraft", element);
        baseDataValidator.reset().parameter("allowOverdraft").value(allowOverdraft).notNull();
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final Boolean isCashOperation = this.fromApiJsonHelper.extractBooleanNamed("isCashOperation", element);
        baseDataValidator.reset().parameter("isCashOperation").value(isCashOperation).notNull();
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final Boolean sendAcrm = this.fromApiJsonHelper.extractBooleanNamed("sendAcrm", element);
        baseDataValidator.reset().parameter("sendAcrm").value(sendAcrm).notNull();
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final Boolean isDocument = this.fromApiJsonHelper.extractBooleanNamed("isDocument", element);
        baseDataValidator.reset().parameter("isDocument").value(isDocument).notNull();
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        final Integer currencyCode = this.fromApiJsonHelper.extractIntegerNamed("currencyCode", element, Locale.getDefault());
        baseDataValidator.reset().parameter("currencyCode").value(currencyCode).notNull();
        throwExceptionIfValidationWarningsExist(dataValidationErrors);

        return new CodeCause(id, codigoAcrm, description, shortDescription, operationType, allowOverdraft, isCashOperation, sendAcrm,
                isDocument, currencyCode).isNew(true);
    }

    public CodeCause validateForUpdate(final String json) {
        return this.validateForCreate(json).isNew(false);
    }

    private void throwExceptionIfValidationWarningsExist(final List<ApiParameterError> dataValidationErrors) {
        if (!dataValidationErrors.isEmpty()) {
            throw new PlatformApiDataValidationException("validation.msg.validation.errors.exist", "Validation errors exist.",
                    dataValidationErrors);
        }
    }
}
