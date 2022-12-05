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
package org.apache.fineract.portfolio.cupo.api;

import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.fineract.commands.domain.CommandWrapper;
import org.apache.fineract.commands.service.CommandWrapperBuilder;
import org.apache.fineract.commands.service.PortfolioCommandSourceWritePlatformService;
import org.apache.fineract.infrastructure.core.data.CommandProcessingResult;
import org.apache.fineract.infrastructure.core.exception.UnrecognizedQueryParamException;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.cupo.data.CupoData;
import org.apache.fineract.portfolio.cupo.domain.CupoStatus;
import org.apache.fineract.portfolio.cupo.service.CupoReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/cupos")
@Component
@Scope("singleton")
public class CupoApiResource {

    private final DefaultToApiJsonSerializer<CupoData> toApiJsonSerializer;
    private final PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService;
    private final PlatformSecurityContext context;
    private final CupoReadService cupoReadService;

    @Autowired
    public CupoApiResource(DefaultToApiJsonSerializer<CupoData> toApiJsonSerializer,
            PortfolioCommandSourceWritePlatformService commandsSourceWritePlatformService, PlatformSecurityContext context,
            CupoReadService cupoReadService) {
        this.toApiJsonSerializer = toApiJsonSerializer;
        this.commandsSourceWritePlatformService = commandsSourceWritePlatformService;
        this.context = context;
        this.cupoReadService = cupoReadService;
    }

    @POST
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCupo(final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().createCupo().withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @PUT
    @Path("/{cupoId}")
    @Consumes({ MediaType.APPLICATION_JSON })
    @Produces({ MediaType.APPLICATION_JSON })
    public String createCupo(@PathParam("cupoId") final Long cupoId, final String apiRequestBodyAsJson) {
        final CommandWrapper commandRequest = new CommandWrapperBuilder().updateCupo(cupoId).withJson(apiRequestBodyAsJson).build();

        final CommandProcessingResult result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);

        return this.toApiJsonSerializer.serialize(result);
    }

    @POST
    @Path("/{cupoId}")
    public String cupoActions(@PathParam("cupoId") final Long cupoId, @QueryParam("command") final String commandParam,
            @Parameter(hidden = true) final String apiRequestBodyAsJson) {
        final CommandWrapperBuilder builder = new CommandWrapperBuilder().withJson(apiRequestBodyAsJson);

        CommandProcessingResult result = null;

        if (is(commandParam, "approve")) {
            final CommandWrapper commandRequest = builder.approveCupo(cupoId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "reject")) {
            final CommandWrapper commandRequest = builder.rejectCupo(cupoId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "extension")) {
            final CommandWrapper commandRequest = builder.extensionCupo(cupoId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "cancel")) {
            final CommandWrapper commandRequest = builder.cancelCupo(cupoId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        } else if (is(commandParam, "reduction")) {
            final CommandWrapper commandRequest = builder.reductionCupo(cupoId).build();
            result = this.commandsSourceWritePlatformService.logCommandSource(commandRequest);
        }

        if (result == null) {
            throw new UnrecognizedQueryParamException("command", commandParam);
        }

        return this.toApiJsonSerializer.serialize(result);
    }

    private boolean is(final String commandParam, final String commandValue) {
        return StringUtils.isNotBlank(commandParam) && commandParam.trim().equalsIgnoreCase(commandValue);
    }

    @GET
    @Path("/{cupoId}")
    public String retrieveOne(@PathParam("cupoId") final Long cupoId) {
        this.context.authenticatedUser().validateHasReadPermission(CupoApiConstants.CUPO_RESOURCE_NAME);
        CupoData cupoData = this.cupoReadService.findById(cupoId);
        return this.toApiJsonSerializer.serialize(cupoData);
    }

    @GET
    @Path("/template")
    public String template(@QueryParam("clientId") final Long clientId, @QueryParam("groupId") final Long groupId) {
        CupoData cupoData = this.cupoReadService.template(clientId, groupId);
        return this.toApiJsonSerializer.serialize(cupoData);
    }

    @GET
    @Path("/client/{clientId}")
    public String findAllByClient(@PathParam("clientId") final Long clientId, @QueryParam("status") final Integer status) {
        this.context.authenticatedUser().validateHasReadPermission(CupoApiConstants.CUPO_RESOURCE_NAME);
        List<CupoData> cupos = this.cupoReadService.findAllActiveCuposByClientId(clientId, CupoStatus.fromInt(status), null);
        return this.toApiJsonSerializer.serialize(cupos);
    }

    @GET
    @Path("/group/{groupId}")
    public String findAllByGroup(@PathParam("groupId") final Long groupId, @QueryParam("status") final Integer status) {
        this.context.authenticatedUser().validateHasReadPermission(CupoApiConstants.CUPO_RESOURCE_NAME);
        List<CupoData> cupos = this.cupoReadService.findAllActiveCuposByGroupId(groupId, CupoStatus.fromInt(status), null);
        return this.toApiJsonSerializer.serialize(cupos);
    }
}
