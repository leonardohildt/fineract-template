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
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "m_causal_process_mapping")
public class CodeCauseProcessMapping implements Persistable<CodeCauseProcessMappingPK>, Serializable {

    @EmbeddedId
    private CodeCauseProcessMappingPK id;

    @Column(name = "operation", length = 250, nullable = false)
    private String operation;

    @Column(name = "causal_code", nullable = false)
    private Integer causalCode;

    @Column(name = "trigger_type", length = 250)
    private String triggerType;

    @Column(name = "log_transaction_type", length = 250, nullable = false)
    private String logTransactionType;

    @Column(name = "destination", length = 400)
    private String destination;

    @Column(name = "description", length = 800)
    private String description;

    public CodeCauseProcessMapping() {
        //
    }

    public CodeCauseProcessMapping(String processId, Integer currencyIntCode, String operation, Integer causalCode, String triggerType,
            String logTransactionType, String destination, String description) {
        this.id = new CodeCauseProcessMappingPK(processId, currencyIntCode);
        this.operation = operation;
        this.causalCode = causalCode;
        this.triggerType = triggerType;
        this.logTransactionType = logTransactionType;
        this.destination = destination;
        this.description = description;

    }

    @Override
    public CodeCauseProcessMappingPK getId() {
        return id;
    }

    @Override
    @Transient
    public boolean isNew() {
        return null == this.id;
    }
}
