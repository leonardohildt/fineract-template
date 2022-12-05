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
package org.apache.fineract.accounting.journalentry.domain;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BitaCoraMasterRepository extends JpaRepository<BitaCoraMaster, Long>, JpaSpecificationExecutor<BitaCoraMaster> {

    @Query("select bm from BitaCoraMaster bm where bm.transactionDate = :transactionDate AND bm.transactionType like :transactionType%")
    List<BitaCoraMaster> getPreviousExecutionforSavings(@Param("transactionDate") Date transactionDate,
            @Param("transactionType") String transactionType);

    @Modifying
    @Query("delete from BitaCoraMaster bm where bm.transactionDate = :transactionDate AND bm.accountId = :accountId AND bm.transactionType = :transactionType")
    void deletePreviousExecutionforSavings(@Param("transactionDate") Date transactionDate, @Param("accountId") Long accountId,
            @Param("transactionType") Long transactionType);

    @Modifying
    @Query("delete from BitaCoraMaster bm where bm.transactionDate = :transactionDate AND bm.transactionType = :transactionType")
    void deletePreviousExecutionforSavings(@Param("transactionDate") Date transactionDate, @Param("transactionType") Long transactionType);

    @Query("select bm from BitaCoraMaster bm where bm.transactionDate = :transactionDate AND bm.reference = :reference")
    List<BitaCoraMaster> getFindCounterpartyByReference(@Param("transactionDate") Date transactionDate,
            @Param("reference") String reference);

    BitaCoraMaster findByTransactionAndAccountType(Long transactionId, String accountType);

    Optional<BitaCoraMaster> findFirstByAccountIdAndAccountTypeAndTransactionTypeOrderByIdDesc(Long accountId, String accountType,
            String transactionType);

    BitaCoraMaster findByAccountIdAndTransactionTypeAndTransactionDateAndAccountType(Long accountId, String transactionType,
            Date transactionDate, String accountType);

    @Query("select bm from BitaCoraMaster bm where bm.transaction = :transaction AND bm.accountType in ('DV','DC') AND bm.transactionType in ('VENTA','COMPRA')")
    Optional<BitaCoraMaster> getFindBByCashierTransactionId(@Param("transaction") Long transaction);

    @Query("select bm from BitaCoraMaster bm where bm.transaction = :transaction AND bm.accountType in ('DV','DC') AND bm.transactionType in ('REVERSION VENTA','REVERSION COMPRA')")
    Optional<BitaCoraMaster> getFindBByCashierTransactionIdReversed(@Param("transaction") Long transaction);
}
