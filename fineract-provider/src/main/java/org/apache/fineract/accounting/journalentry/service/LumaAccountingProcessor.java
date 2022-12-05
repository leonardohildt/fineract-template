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
package org.apache.fineract.accounting.journalentry.service;

import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.ACCOUNT_TYPE_AH;
import static org.apache.fineract.accounting.journalentry.domain.BitacoraMasterConstants.ACCOUNT_TYPE_PF;

import java.math.BigDecimal;
import java.util.Date;
import java.util.Optional;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMaster;
import org.apache.fineract.accounting.journalentry.domain.BitaCoraMasterRepository;
import org.apache.fineract.infrastructure.codes.data.CodeCauseProcessMappingData;
import org.apache.fineract.infrastructure.codes.service.CodeCauseProcessMappingPlatformService;
import org.apache.fineract.portfolio.exchange.domain.Exchange;
import org.apache.fineract.portfolio.exchange.domain.ExchangeRepository;
import org.apache.fineract.portfolio.savings.domain.SavingsAccount;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
public class LumaAccountingProcessor {

    public static final String STRING_GTQ = "GTQ";
    @Autowired
    private CodeCauseProcessMappingPlatformService codeCauseProcessMappingPlatformService;
    @Autowired
    private ExchangeRepository exchangeRepository;
    @Autowired
    private BitaCoraMasterRepository bitaCoraMasterRepository;

    protected void reverseLogForTransactionId(Long transactionId, String accountType) {
        BitaCoraMaster existingLog = findMasterLogForTransactionId(transactionId, accountType);
        reverseMasterLogAndDetails(existingLog);
    }

    protected void reverseMasterLogAndDetails(BitaCoraMaster logToRevert) {
        if (logToRevert != null) {
            BitaCoraMaster newMasterLog = logToRevert.cloneForReverse();
            bitaCoraMasterRepository.save(newMasterLog);
        }
    }

    public void saveMasterLogAndDetails(BitaCoraMaster bitaCoraMaster) {
        this.bitaCoraMasterRepository.save(bitaCoraMaster);
    }

    protected Optional<BitaCoraMaster> findLastMasterLogForAccountIdAndTypeandTrxType(Long accountId, String accountType,
            String transactionType) {
        try {
            return bitaCoraMasterRepository.findFirstByAccountIdAndAccountTypeAndTransactionTypeOrderByIdDesc(accountId, accountType,
                    transactionType);
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public BitaCoraMaster findMasterLogForAccountIdAndTrxTypeAndDateAndAccountType(Long accountId, String transactionType,
            Date transactionDate, String accountType) {
        try {
            return bitaCoraMasterRepository.findByAccountIdAndTransactionTypeAndTransactionDateAndAccountType(accountId, transactionType,
                    transactionDate, accountType);
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    public BitaCoraMaster findMasterLogForTransactionId(Long transactionId, String accountType) {
        try {
            return bitaCoraMasterRepository.findByTransactionAndAccountType(transactionId, accountType);
        } catch (IncorrectResultSizeDataAccessException e) {
            return null;
        }
    }

    protected BigDecimal getExchangeRate(String currencyCode, Date transactionDate) {
        BigDecimal exchangeRate = BigDecimal.ONE;
        if (!currencyCode.equals(STRING_GTQ)) {
            Page<Exchange> exchangePage = this.exchangeRepository.retrieveExchangeByOriginAndValidFrom(currencyCode, transactionDate,
                    PageRequest.of(0, 1));
            Optional<Exchange> exchange = exchangePage.stream().findFirst();
            if (exchange.isPresent()) {
                exchangeRate = exchange.get().getExchangeRate();
            }
        }
        return exchangeRate.subtract(BigDecimal.ONE);
    }

    public String getAccountTypeFromSavingsAccount(SavingsAccount account) {
        return account.depositAccountType().isFixedDeposit() ? ACCOUNT_TYPE_PF : ACCOUNT_TYPE_AH;
    }

    public String getStatusStringFromSavings(SavingsAccount account) {
        return account.isActive() ? "ACTIVO" : "INACTIVO";
    }

    public String getStatusStringFromLoanNpa(boolean isNpa) {
        String status = "VIG";
        if (isNpa) {
            status = "VEN";
        }
        return status;
    }

    /*
     * public String getGroupStringFromLoan(Loan loan) { String groupString = loan.getLoanPurpose() == null ? null :
     * String.valueOf(loan.getLoanPurpose().position()); return groupString; }
     */

    /*
     * public String getTypeOfLoanString(Loan loan) { String loanTypeString = Objects.isNull(loan.getFund()) ? null :
     * String.valueOf(loan.getFund().getId()); return loanTypeString; }
     */

    public CodeCauseProcessMappingData getCausalFromProccessIdAndCurrencyIntCode(String processId, Integer currencyIntCode) {
        return this.codeCauseProcessMappingPlatformService.retrieveOne(processId, currencyIntCode);
    }
}
