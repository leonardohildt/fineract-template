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

import java.io.Serializable;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

@Embeddable
public class CodeCauseProcessMappingPK implements Serializable {

    @Column(name = "process_id", length = 5, nullable = false)
    private String processId;

    @Column(name = "currency_int_code", nullable = false)
    private Integer currencyIntCode;

    public CodeCauseProcessMappingPK() {
        //
    }

    public CodeCauseProcessMappingPK(String processId, Integer currencyIntCode) {
        this.processId = processId;
        this.currencyIntCode = currencyIntCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof CodeCauseProcessMappingPK)) {
            return false;
        }
        final CodeCauseProcessMappingPK ccpmPK = (CodeCauseProcessMappingPK) obj;
        return new EqualsBuilder() //
                .append(this.processId, ccpmPK.processId) //
                .append(this.currencyIntCode, ccpmPK.currencyIntCode) //
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(8389, 8719) //
                .append(this.processId) //
                .append(this.currencyIntCode) //
                .toHashCode();
    }
}
