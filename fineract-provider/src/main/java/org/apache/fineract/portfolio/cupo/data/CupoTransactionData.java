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
package org.apache.fineract.portfolio.cupo.data;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public class CupoTransactionData {

    private Long id;
    private Long cupoId;
    private LocalDate transactionDate;
    private EnumOptionData type;
    private BigDecimal amount;
    private Long loanTransactionId;
    private Long loanId;

    public CupoTransactionData(Long id, Long cupoId, LocalDate transactionDate, EnumOptionData type, BigDecimal amount,
            Long loanTransactionId, Long loanId) {
        this.id = id;
        this.cupoId = cupoId;
        this.transactionDate = transactionDate;
        this.type = type;
        this.amount = amount;
        this.loanTransactionId = loanTransactionId;
        this.loanId = loanId;
    }
}
