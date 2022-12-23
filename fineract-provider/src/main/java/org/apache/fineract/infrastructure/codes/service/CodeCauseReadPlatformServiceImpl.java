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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.fineract.infrastructure.codes.data.CodeCauseData;
import org.apache.fineract.infrastructure.codes.exception.CodeValueNotFoundException;
import org.apache.fineract.infrastructure.core.service.Page;
import org.apache.fineract.infrastructure.core.service.PaginationHelper;
import org.apache.fineract.infrastructure.security.utils.SQLInjectionValidator;
import org.apache.fineract.organisation.monetary.data.CurrencyData;
import org.apache.fineract.organisation.monetary.service.CurrencyReadPlatformService;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor

public class CodeCauseReadPlatformServiceImpl implements CodeCauseReadPlatformService {

    private final JdbcTemplate jdbcTemplate;
    private final PaginationHelper paginationHelper;
    private final CurrencyReadPlatformService currencyReadPlatformService;

    private static final class CodeCauseDataMapper implements RowMapper<CodeCauseData> {

        public String schema() {
            StringBuilder sqlBuilder = new StringBuilder(500);

            sqlBuilder.append(" cc.id as causalId, cc.description as descripcion, cc.allow_overdraft as allowOverdraft, ");
            sqlBuilder.append(" cc.is_cash_operation as isCashOperation, cc.operation_type as operationType, cc.send_acrm as sendAcrm, ");
            sqlBuilder.append(" cc.codigo_acrm as codigoAcrm, cc.short_description as shortDescription, cc.is_document as isDocument, ");
            sqlBuilder.append(
                    " cc.currency_int_code as currencyIntCode, cc.is_shown_in_cashier_module as isShownInCashierModule, oc.code as currencyCode, oc.name as currencyName, ");
            sqlBuilder.append(
                    " oc.internationalized_name_code as internationalCode, oc.display_symbol as displaySymbol, oc.decimal_places as decimalPlaces, ");
            sqlBuilder.append(" oc.currency_multiplesof as multiplesOf, oc.int_code as intCode ");
            sqlBuilder.append(" from m_code_causal as cc ");
            sqlBuilder.append(" left join m_organisation_currency oc on oc.int_code = cc.currency_int_code ");

            return sqlBuilder.toString();
        }

        @Override
        public CodeCauseData mapRow(final ResultSet rs, @SuppressWarnings("unused") final int rowNum) throws SQLException {

            final Integer id = rs.getInt("causalId");
            final String description = rs.getString("descripcion");
            final Boolean allowOverdraft = rs.getBoolean("allowOverdraft");
            final Boolean isCashOperation = rs.getBoolean("isCashOperation");
            final String operationType = rs.getString("operationType");
            final Boolean sendAcrm = rs.getBoolean("sendAcrm");
            final Integer codigoAcrm = rs.getInt("codigoAcrm");
            final String shortDescription = rs.getString("shortDescription");
            final Boolean isDocument = rs.getBoolean("isDocument");
            final Integer currencyIntCode = rs.getInt("currencyIntCode");
            final Boolean isShownInCashierModule = rs.getBoolean("isShownInCashierModule");

            final String currencyCode = rs.getString("currencyCode");
            final String currencyName = rs.getString("currencyName");
            final String currencyNameCode = rs.getString("internationalCode");
            final String currencyDisplaySymbol = rs.getString("displaySymbol");
            final Integer currencyDecimalPlaces = rs.getInt("decimalPlaces");
            final Integer inMultiplesOf = rs.getInt("multiplesOf");
            final Integer intCode = rs.getInt("intCode");

            final CurrencyData currency = new CurrencyData(currencyCode, currencyName, currencyDecimalPlaces, inMultiplesOf,
                    currencyDisplaySymbol, currencyNameCode, intCode);

            CodeCauseData ret = CodeCauseData.instance(id, description, allowOverdraft, isCashOperation, operationType, sendAcrm,
                    codigoAcrm, shortDescription, isDocument, currencyIntCode, currency, isShownInCashierModule);

            return ret;
        }
    }

    @Override
    public Collection<CodeCauseData> retrieveAll() {
        final CodeCauseDataMapper rm = new CodeCauseDataMapper();
        final String sql = "select " + rm.schema();

        return this.jdbcTemplate.query(sql, rm, new Object[] {});
    }

    @Override
    public Page<CodeCauseData> retrieveAll(Integer offset, Integer limit, String searchText) {
        CodeCauseDataMapper mapper = new CodeCauseDataMapper();
        List<Object> paramList = new ArrayList<>();
        final StringBuilder sqlBuilder = new StringBuilder(400);
        sqlBuilder.append("select ");
        sqlBuilder.append(mapper.schema());

        if ((searchText != null) && !searchText.isEmpty()) {
            SQLInjectionValidator.validateSQLInput(searchText);
            sqlBuilder.append("  WHERE cc.codigo_acrm like ? ");
            paramList.add("%" + searchText);
            sqlBuilder.append("  OR cc.description like ? ");
            paramList.add("%" + searchText + "%");
        }

        return this.paginationHelper.fetchPage(this.jdbcTemplate, sqlBuilder.toString(), paramList.toArray(), mapper);
    }

    @Override
    public CodeCauseData retrieveByCodigoAcrm(final Integer codigoAcrm) {
        try {
            final CodeCauseDataMapper rm = new CodeCauseDataMapper();
            final String sql = "select " + rm.schema() + " where cc.codigo_acrm = ? ";

            return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { codigoAcrm });
        } catch (final EmptyResultDataAccessException e) {
            throw new CodeValueNotFoundException(codigoAcrm.longValue(), e);
        }
    }

    @Override
    public Collection<CodeCauseData> retrieveByAllowOverdraft(final Boolean allowOverdraft) {

        final CodeCauseDataMapper rm = new CodeCauseDataMapper();
        final String sql = "select " + rm.schema() + " where cc.allow_overdraft = ? ";

        return this.jdbcTemplate.query(sql, rm, new Object[] { allowOverdraft });
    }

    @Override
    public Collection<CodeCauseData> retrieveByCurrencyCode(final Integer currencyCodeInt) {
        final CodeCauseDataMapper rm = new CodeCauseDataMapper();
        final String sql = "select " + rm.schema() + " where cc.currency_int_code = ? ";

        return this.jdbcTemplate.query(sql, rm, new Object[] { currencyCodeInt });
    }

    @Override
    public CodeCauseData retrieveByCauseId(final Long causeId) {
        try {
            final CodeCauseDataMapper rm = new CodeCauseDataMapper();
            final String sql = "select " + rm.schema() + " where cc.id = ? ";

            return this.jdbcTemplate.queryForObject(sql, rm, new Object[] { causeId });
        } catch (final EmptyResultDataAccessException e) {
            throw new CodeValueNotFoundException(causeId, e);
        }
    }

    @Override
    public CodeCauseData retrieveTemplate() {
        final Collection<CurrencyData> currencyOptions = this.currencyReadPlatformService.retrieveAllowedCurrencies();
        return CodeCauseData.template(currencyOptions);
    }
}
