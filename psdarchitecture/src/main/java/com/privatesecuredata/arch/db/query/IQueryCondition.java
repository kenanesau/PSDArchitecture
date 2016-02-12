package com.privatesecuredata.arch.db.query;

import com.privatesecuredata.arch.db.SqlDataField;

import java.util.Map;

/**
 * Interface for all Query-Conditions used by the QueryBuilder
 *
 * @sa com.privatesecuredata.arch.db.query.QueryCondition
 * @sa com.privatesecuredata.arch.db.query.QueryConditionContainer
 */
public interface IQueryCondition {
    /**
     * @return Array of parameters used in this condition (and subsequent conditions)
     */
    QueryParameter[] parameters();

    /**
     * @return unique ID of condition
     */
    String id();

    /**
     * Append condition to an SQL-Query
     *
     * @param fields All fields contained in the persister (Map Sql-Fieldname -> SqlDataField)
     * @param sb Stringbuilder which contains the SQL-Query to append the condition to
     * @return SQL-Query with appended condition
     */
    StringBuilder append(Map<String, SqlDataField> fields, StringBuilder sb);

    /**
     * @return returns a deep copy of the QueryCondition
     */
    IQueryCondition clone();

    /**
     * Called by the query when all unset query-parameters have to be set to default-values
     */
    void setDefaultValues(Map<String, SqlDataField> fields);
}
