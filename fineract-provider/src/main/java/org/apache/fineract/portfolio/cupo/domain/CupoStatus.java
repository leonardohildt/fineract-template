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

import org.apache.fineract.infrastructure.core.data.EnumOptionData;

public enum CupoStatus {

    INVALID(0, "CupoStatusEnum.none"), SUBMITTED_AND_PENDING_APPROVAL(100, "CupoStatusEnum.submitted"), ACTIVE(200,
            "CupoStatusEnum.active"), EXPIRED(300,
                    "CupoStatusEnum.expired"), REJECTED(400, "CupoStatusEnum.rejected"), CANCELED(500, "CupoStatusEnum.canceled");

    private final Integer value;
    private final String code;

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    CupoStatus(Integer value, String code) {
        this.value = value;
        this.code = code;
    }

    public static CupoStatus fromInt(final Integer statusValue) {
        CupoStatus cupoStatus = CupoStatus.INVALID;
        if (statusValue == null) {
            return cupoStatus;
        }
        switch (statusValue) {
            case 100:
                return CupoStatus.SUBMITTED_AND_PENDING_APPROVAL;
            case 200:
                return CupoStatus.ACTIVE;
            case 300:
                return CupoStatus.EXPIRED;
            case 400:
                return CupoStatus.REJECTED;
            case 500:
                return CupoStatus.CANCELED;
        }
        return cupoStatus;
    }

    public static EnumOptionData toDataEnum(final CupoStatus cupoStatus) {
        switch (cupoStatus) {
            case SUBMITTED_AND_PENDING_APPROVAL:
                return new EnumOptionData(cupoStatus.getValue().longValue(), cupoStatus.getCode(), "Submitted");
            case ACTIVE:
                return new EnumOptionData(cupoStatus.getValue().longValue(), cupoStatus.getCode(), "Active");
            case EXPIRED:
                return new EnumOptionData(cupoStatus.getValue().longValue(), cupoStatus.getCode(), "Expired");
            case REJECTED:
                return new EnumOptionData(cupoStatus.getValue().longValue(), cupoStatus.getCode(), "Rejected");
            case CANCELED:
                return new EnumOptionData(cupoStatus.getValue().longValue(), cupoStatus.getCode(), "Canceled");
            case INVALID:
                return null;
        }
        return null;
    }

    public boolean isValid() {
        return !this.value.equals(CupoStatus.INVALID.getValue());
    }

    public boolean isWaitingForApproval() {
        return this.value.equals(CupoStatus.SUBMITTED_AND_PENDING_APPROVAL.getValue());
    }

    public boolean isActive() {
        return this.value.equals(CupoStatus.ACTIVE.getValue());
    }

    public boolean isRejected() {
        return this.value.equals(CupoStatus.REJECTED.getValue());
    }

    public boolean isExpired() {
        return this.value.equals(CupoStatus.EXPIRED.getValue());
    }
}
