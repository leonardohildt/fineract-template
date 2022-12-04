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

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import org.apache.fineract.infrastructure.core.serialization.DefaultToApiJsonSerializer;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.apache.fineract.portfolio.cupo.data.CupoTransactionData;
import org.apache.fineract.portfolio.cupo.service.CupoReadService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Path("/cupos/{cupoId}/transactions")
@Component
@Scope("singleton")
public class CupoTransactionsApiResource {

    private final CupoReadService cupoReadService;
    private final PlatformSecurityContext context;
    private final DefaultToApiJsonSerializer<CupoTransactionData> toApiJsonSerializer;

    @Autowired
    public CupoTransactionsApiResource(CupoReadService cupoReadService, PlatformSecurityContext context,
            DefaultToApiJsonSerializer<CupoTransactionData> toApiJsonSerializer) {
        this.cupoReadService = cupoReadService;
        this.context = context;
        this.toApiJsonSerializer = toApiJsonSerializer;
    }

    @GET
    public String retrieveAll(@PathParam("cupoId") final Long cupoId) {
        this.context.authenticatedUser().validateHasReadPermission(CupoApiConstants.CUPO_TRANSACTION_RESOURCE_NAME);
        List<CupoTransactionData> transactions = this.cupoReadService.getAllTransactionsFromCupo(cupoId);
        return this.toApiJsonSerializer.serialize(transactions);
    }
}
