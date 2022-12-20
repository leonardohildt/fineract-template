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

package org.apache.fineract.infrastructure.codes.domain;

public enum LumaProccessId {

    DISBURSE_TO_SAVINGS("P001", "Disburse to savings from loan account"), CHARGE_ON_DISBURSE_TO_SAVINGS("P002",
            "Pay charge from savings account when disbursing to savings"), LOAN_REPAYMENT_FROM_SAVINGS_STANDING_INSTRUCTION("P003",
                    "Loan Repayment transaction from savings standing instruction"), LOAN_REPAYMENT_TRANSFER("P004",
                            "Normal repayment to be attached to a manual savings withdrawal"), SAVINGS_WITHDRAWAL_TO_FIXED_DEPOSIT("P005",
                                    "Savings withdrawal to savings so fixed deposit is funded"), FIXED_DEPOSIT_DEPOSIT_FROM_SAVINGS("P006",
                                            "Deposit to fixed deposit from linked savings"), FIXED_DEPOSIT_INTEREST_POSTING("P007",
                                                    "Interes posting for fixed deposit"), SAVINGS_INTEREST_POSTING("P008",
                                                            "Interes posting for savings"), WITHHOLD_TAX_INTEREST_POSTING_SAVINGS("P009",
                                                                    "Withhold tax from interest posting for savings"), WITHHOLD_TAX_INTEREST_POSTING_FIXED_DEPOSIT(
                                                                            "P010",
                                                                            "Withhold tax from interest posting for fixed deposit"), ACCRUAL_INTEREST_SAVINGS(
                                                                                    "P011",
                                                                                    "Accrual interest from savings"), ACCRUAL_INTEREST_FIXED_DEPOSIT(
                                                                                            "P012",
                                                                                            "Accrual interest from fixed deposit"), REJECTED_CHECK_CHARGE(
                                                                                                    "P013",
                                                                                                    "Charge generated after rejecting a check"), OVERDRAFT_CREDIT(
                                                                                                            "P014",
                                                                                                            "For logs when overdraft, credit"), OVERDRAFT_DEBIT(
                                                                                                                    "P015",
                                                                                                                    "For logs when overdraft, debit"), CHECK_REJECTION(
                                                                                                                            "P016",
                                                                                                                            "For logs when rejecting a check");

    LumaProccessId(String value, String description) {
        this.value = value;
        this.description = description;
    }

    private final String value;
    private final String description;

    public String getValue() {
        return value;
    }
}
