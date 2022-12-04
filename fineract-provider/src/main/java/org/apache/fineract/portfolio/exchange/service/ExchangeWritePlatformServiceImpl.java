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
package org.apache.fineract.portfolio.exchange.service;

import java.util.Map;
import org.apache.fineract.infrastructure.core.api.JsonCommand;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResultBuilder;
import org.apache.fineract.portfolio.exchange.api.ExchangeApiConstants;
import org.apache.fineract.portfolio.exchange.data.ExchangeDataValidator;
import org.apache.fineract.portfolio.exchange.domain.Exchange;
import org.apache.fineract.portfolio.exchange.domain.ExchangeRepository;
import org.apache.fineract.portfolio.exchange.domain.ExchangeRepositoryWrapper;
import org.apache.fineract.useradministration.domain.AppUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ExchangeWritePlatformServiceImpl implements ExchangeWritePlatformService {

    private final ExchangeDataValidator dataValidator;
    private final ExchangeRepository repository;
    private final ExchangeRepositoryWrapper repositoryWrapper;

    @Autowired
    public ExchangeWritePlatformServiceImpl(ExchangeDataValidator dataValidator, ExchangeRepository repository,
            ExchangeRepositoryWrapper repositoryWrapper) {
        this.dataValidator = dataValidator;
        this.repository = repository;
        this.repositoryWrapper = repositoryWrapper;
    }

    @Override
    public CommandProcessingResult createExchange(JsonCommand command) {
        this.dataValidator.validateForCreate(command.json());
        Exchange exchange = Exchange.createNew(command);
        AppUser authUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        exchange.setCreatedBy(authUser);
        this.repository.save(exchange);
        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(exchange.getId()) //
                .build();
    }

    @Override
    public CommandProcessingResult updateExchange(Long exchangeId, JsonCommand command) {
        this.dataValidator.validateForUpdate(command.json());
        Exchange exchangeToUpdate = this.repositoryWrapper.findOneWithNotFoundDetection(exchangeId);
        final Map<String, Object> changes = exchangeToUpdate.update(command);
        AppUser authUser = (AppUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long currentCreatorsId = null;
        if (exchangeToUpdate.getCreatedBy() != null) {
            currentCreatorsId = exchangeToUpdate.getCreatedBy().getId();
        }
        if (!authUser.getId().equals(currentCreatorsId)) {
            exchangeToUpdate.setCreatedBy(authUser);
            changes.put(ExchangeApiConstants.createdByDataParamName, authUser.getDisplayName());
        }
        this.repository.saveAndFlush(exchangeToUpdate);

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(exchangeId) //
                .with(changes) //
                .build();
    }

    @Override
    public CommandProcessingResult deleteExchange(Long exchangeId, JsonCommand command) {
        Exchange exchangeToDelete = this.repositoryWrapper.findOneWithNotFoundDetection(exchangeId);
        this.repository.delete(exchangeToDelete);
        this.repository.flush();

        return new CommandProcessingResultBuilder() //
                .withCommandId(command.commandId()) //
                .withEntityId(exchangeId) //
                .build();
    }
}
