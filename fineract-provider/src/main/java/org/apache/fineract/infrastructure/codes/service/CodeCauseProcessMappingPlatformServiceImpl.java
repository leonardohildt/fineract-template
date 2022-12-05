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
package org.apache.fineract.infrastructure.codes.service;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import org.apache.fineract.infrastructure.codes.data.CodeCauseProcessMappingData;
import org.apache.fineract.infrastructure.codes.exception.CodeValueNotFoundException;
import org.apache.fineract.infrastructure.core.service.RoutingDataSource;
import org.apache.fineract.infrastructure.security.service.PlatformSecurityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
public class CodeCauseProcessMappingPlatformServiceImpl implements CodeCauseProcessMappingPlatformService {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public CodeCauseProcessMappingPlatformServiceImpl(final PlatformSecurityContext context, final RoutingDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    private static final class CodeCauseProcessMappingDataMapper implements RowMapper<CodeCauseProcessMappingData> {

        public String schema() {
            return " cpm.* from m_causal_process_mapping as cpm ";
        }

        @Override
        public CodeCauseProcessMappingData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final String processId = rs.getString("process_id");
            final Integer currencyIntCode = rs.getInt("currency_int_code");
            final String operation = rs.getString("operation");
            final Integer causalCode = rs.getInt("causal_code");
            final String triggerType = rs.getString("trigger_type");
            final String logTransactionType = rs.getString("log_transaction_type");
            final String destination = rs.getString("destination");
            final String description = rs.getString("description");

            CodeCauseProcessMappingData ret = CodeCauseProcessMappingData.instance(processId, currencyIntCode, operation, causalCode,
                    triggerType, logTransactionType, destination, description);
            return ret;
        }
    }

    @Override
    public Collection<CodeCauseProcessMappingData> retrieveAll() {
        final CodeCauseProcessMappingDataMapper rm = new CodeCauseProcessMappingDataMapper();
        final String sql = "select " + rm.schema();

        return this.jdbcTemplate.query(sql, rm, new Object[] {});
    }

    @Override
    public CodeCauseProcessMappingData retrieveOne(final String processId, final Integer currencyIntCode) {
        try {
            final CodeCauseProcessMappingDataMapper rm = new CodeCauseProcessMappingDataMapper();
            final String sql = "select " + rm.schema() + " where cpm.process_id = ? and cpm.currency_int_code = ? ";

            return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { processId, currencyIntCode });
        } catch (final EmptyResultDataAccessException e) {
            throw new CodeValueNotFoundException(currencyIntCode.longValue(), e);
        }
    }
}
