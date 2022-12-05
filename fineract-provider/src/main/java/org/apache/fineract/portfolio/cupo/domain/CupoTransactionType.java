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
package org.apache.fineract.portfolio.cupo.domain;

import java.util.HashMap;
import java.util.Map;
import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public enum CupoTransactionType {

    DEPOSIT_FROM_APPROVAL(1, "cupoTransactionType.deposit.approval"), //
    DEPOSIT_FROM_LOAN_REPAYMENT(2, "cupoTransactionType.deposit.loan.repayment"), //
    DEPOSIT_FROM_EXTENSION(3, "cupoTransactionType.deposit.extension"), //
    WITHDRAWAL_FROM_LOAN_APPROVAL(4, "cupoTransactionType.withdrawal.loan.disbursement"), WITHDRAWAL_REDUCTION(5,
            "cupoTransactionType.withdrawal"), WITHDRAWAL_FROM_CANCELATION(6, "cupoTransactionType.cancelation"); //

    private final Integer value;
    private final String code;

    CupoTransactionType(final Integer value, final String code) {
        this.value = value;
        this.code = code;
    }

    public Integer getValue() {
        return this.value;
    }

    public String getCode() {
        return this.code;
    }

    private static final Map<Integer, CupoTransactionType> intToEnumMap = new HashMap<>();

    static {
        for (final CupoTransactionType type : CupoTransactionType.values()) {
            intToEnumMap.put(type.value, type);
        }
    }

    public static CupoTransactionType fromInt(final int i) {
        final CupoTransactionType type = intToEnumMap.get(Integer.valueOf(i));
        return type;
    }

    public boolean isDeposit() {
        if (this.name().startsWith("DEPOSIT_")) {
            return true;
        }
        return false;
    }

    public boolean isWithdrawal() {
        if (this.name().startsWith("WITHDRAWAL_")) {
            return true;
        }
        return false;
    }

    public static EnumOptionData toDataEnum(CupoTransactionType transactionType) {
        switch (transactionType) {
            case DEPOSIT_FROM_APPROVAL:
                return new EnumOptionData(transactionType.getValue().longValue(), transactionType.getCode(), "Deposit from approval");
            case DEPOSIT_FROM_LOAN_REPAYMENT:
                return new EnumOptionData(transactionType.getValue().longValue(), transactionType.getCode(), "Deposit from loan repayment");
            case DEPOSIT_FROM_EXTENSION:
                return new EnumOptionData(transactionType.getValue().longValue(), transactionType.getCode(), "Deposit from extension");
            case WITHDRAWAL_FROM_LOAN_APPROVAL:
                return new EnumOptionData(transactionType.getValue().longValue(), transactionType.getCode(),
                        "Withdrawal from loan approval");
            case WITHDRAWAL_REDUCTION:
                return new EnumOptionData(transactionType.getValue().longValue(), transactionType.getCode(), "Withdrawal reduction");
            case WITHDRAWAL_FROM_CANCELATION:
                return new EnumOptionData(transactionType.getValue().longValue(), transactionType.getCode(), "Withdrawal cancelation");
        }
        return null;
    }

    public boolean isWithdrawalFromLoanApproval() {
        return this.value.equals(CupoTransactionType.WITHDRAWAL_FROM_LOAN_APPROVAL.getValue());
    }
}
