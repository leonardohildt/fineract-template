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
package org.apache.fineract.portfolio.cupo.service;

import java.time.LocalDate;
import java.util.List;
import org.apache.fineract.portfolio.cupo.data.CupoData;
import org.apache.fineract.portfolio.cupo.data.CupoTransactionData;
import org.apache.fineract.portfolio.cupo.domain.CupoStatus;

public interface CupoReadService {

    List<CupoData> findAllActiveCuposByClientId(Long clientId, CupoStatus status, String currencyCode);

    List<CupoData> findAllActiveCuposByGroupId(Long groupId, CupoStatus status, String currencyCode);

    CupoData findById(Long cupoId);

    CupoData template(Long clientId, Long groupId);

    LocalDate getLastUseOfCupo(Long cupoId);

    List<CupoTransactionData> getAllTransactionsFromCupo(Long cupoId);
}
