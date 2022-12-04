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
package org.apache.fineract.portfolio.exchange.api;

import io.swagger.v3.oas.annotations.Parameter;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.api.ApiRequestParameterHelper;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.serialization.ApiRequestJsonSerializationSettings;
import org.apache.fineract.infrastructure.core.serialization.ToApiJsonSerializer;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.SearchParameters;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.exchange.data.ExchangeData;
import org.apache.fineract.portfolio.exchange.service.ExchangeReadPlatformService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/exchanges")
@Component
@Scope("singleton")
public class ExchangeApiResource {

    private final PlatformSecurityContext context;
    private final ToApiJsonSerializer<Object> toApiJsonSerializer;
    private final ApiRequestParameterHelper apiRequestParameterHelper;
    private final ExchangeReadPlatformService exchangeReadPlatformService;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;

    @Autowired
    public ExchangeApiResource(final PlatformSecurityContext context, final ToApiJsonSerializer<Object> toApiJsonSerializer,
            final ApiRequestParameterHelper apiRequestParameterHelper, ExchangeReadPlatformService exchangeReadPlatformService,
            final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService) {
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.apiRequestParameterHelper = apiRequestParameterHelper;
        this.exchangeReadPlatformService = exchangeReadPlatformService;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    public String retrieveAll(@Context final UriInfo uriInfo, @QueryParam("offset") @Parameter(description = "offset") final Integer offset,
            @QueryParam("limit") @Parameter(description = "limit") final Integer limit,
            @QueryParam("orderBy") @Parameter(description = "orderBy") final String orderBy,
            @QueryParam("sortOrder") @Parameter(description = "sortOrder") final String sortOrder) {
        this.context.authenticatedUser().validateHasReadPermission(ExchangeApiConstants.EXCHANGE_RESOURCE_NAME);

        final SearchParameters searchParameters = SearchParameters.forExchanges(offset, limit, orderBy, sortOrder);

        final Page<ExchangeData> exchangeData = this.exchangeReadPlatformService.retrieveAll(searchParameters);

        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, exchangeData, ExchangeApiConstants.EXCHANGE_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/template")
    public String retrieveTemplate(@Context final UriInfo uriInfo) {
        this.context.authenticatedUser().validateHasReadPermission(ExchangeApiConstants.EXCHANGE_RESOURCE_NAME);
        ExchangeData exchangeData = this.exchangeReadPlatformService.retrieveTemplate();
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, exchangeData, ExchangeApiConstants.EXCHANGE_RESPONSE_DATA_PARAMETERS);
    }

    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("/{exchangeId}")
    public String retrieveOne(@Context final UriInfo uriInfo, @PathParam("exchangeId") final Long exchangeId) {
        this.context.authenticatedUser().validateHasReadPermission(ExchangeApiConstants.EXCHANGE_RESOURCE_NAME);
        ExchangeData exchangeData = this.exchangeReadPlatformService.retrieveOne(exchangeId);
        final ApiRequestJsonSerializationSettings settings = this.apiRequestParameterHelper.process(uriInfo.getQueryParameters());
        return this.toApiJsonSerializer.serialize(settings, exchangeData, ExchangeApiConstants.EXCHANGE_RESPONSE_DATA_PARAMETERS);
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String create(@Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().createExchange().withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("{exchangeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String update(@PathParam("exchangeId") @Parameter(description = "exchangeId") final Long exchangeId,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateExchange(exchangeId).withJson(apiRequestBodyAsJson).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }

    @DELETE
    @Path("{exchangeId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String delete(@PathParam("exchangeId") @Parameter(description = "exchangeId") final Long exchangeId) {

        final CommandWrapper commandRequest = new CommandWrapperBuilder().deleteExchange(exchangeId).build();
        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        return this.toApiJsonSerializer.serialize(result);
    }
}
