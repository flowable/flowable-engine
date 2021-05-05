/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.flowable.common.engine.impl.db;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.query.Query.NullHandlingOnOrder;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ListQueryParameterObject {
    
    public enum ResultType {
        LIST, LIST_PAGE, SINGLE_RESULT, COUNT
    }

    protected static final OrderBy DEFAULT_ORDER_BY = new OrderBy("RES.ID_", "asc", null);

    public static class OrderBy {

        protected final String columnName;
        protected final String direction;
        protected final NullHandlingOnOrder nullHandlingOnOrder;

        public OrderBy(String columnName, String direction, NullHandlingOnOrder nullHandlingOnOrder) {
            this.columnName = columnName;
            this.direction = direction;
            this.nullHandlingOnOrder = nullHandlingOnOrder;
        }

        public String getColumnName() {
            return columnName;
        }

        public String getDirection() {
            return direction;
        }

        public NullHandlingOnOrder getNullHandlingOnOrder() {
            return nullHandlingOnOrder;
        }
    }
    
    public static final String SORTORDER_ASC = "asc";
    public static final String SORTORDER_DESC = "desc";

    protected int firstResult = -1;
    protected int maxResults = -1;
    protected Object parameter;
    protected Collection<OrderBy> orderByCollection;
    protected OrderBy defaultOrderBy = DEFAULT_ORDER_BY;
    protected QueryProperty orderProperty;
    protected String nullHandlingColumn;
    protected NullHandlingOnOrder nullHandlingOnOrder;
    protected ResultType resultType;
    protected String databaseType;
    
    public ListQueryParameterObject() {
        
    }

    public ListQueryParameterObject(Object parameter, int firstResult, int maxResults) {
        this.parameter = parameter;
        this.firstResult = firstResult;
        this.maxResults = maxResults;
    }
    
    public void addOrder(String column, String sortOrder, NullHandlingOnOrder nullHandlingOnOrder) {
        
        if (orderByCollection == null) {
            orderByCollection = new ArrayList<>(2);
        }

        orderByCollection.add(new OrderBy(column, sortOrder, nullHandlingOnOrder));
    }

    public boolean isNeedsPaging() {
        return firstResult >= 0;
    }
    
    public int getFirstResult() {
        return firstResult;
    }

    public int getFirstRow() {
        return firstResult + 1;
    }

    public int getLastRow() {
        if (maxResults == Integer.MAX_VALUE) {
            return maxResults;
        }
        return firstResult + maxResults + 1;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public Object getParameter() {
        return parameter;
    }

    public void setFirstResult(int firstResult) {
        this.firstResult = firstResult;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setParameter(Object parameter) {
        this.parameter = parameter;
    }

    public boolean hasOrderBy() {
        if (orderByCollection != null && !orderByCollection.isEmpty()) {
            return true;
        }

        return defaultOrderBy != null;
    }

    // This is used for the SQL Server and DB2 order by in a window function / over
    @SuppressWarnings("unused")
    public String getOrderByForWindow() {
        return buildOrderBy();
    }

    protected String buildOrderBy() {
        Collection<OrderBy> orderBy = getOrderByCollectionSafe();

        if (orderBy.isEmpty()) {
            return "";
        }

        return orderBy.stream()
                .map(this::mapOrderByToSql)
                .collect(Collectors.joining(",", "order by ", ""));
    }

    protected Collection<OrderBy> getOrderByCollectionSafe() {
        if (orderByCollection != null && !orderByCollection.isEmpty()) {
            return orderByCollection;
        } else if (defaultOrderBy != null) {
            return Collections.singleton(defaultOrderBy);
        } else {
            return Collections.emptyList();
        }
    }

    @SuppressWarnings("unused")
    public String getOuterJoinOrderBy() {
        if (isNeedsPaging()) {
            if (AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                // SQL Server does not optimize the order by in the outer join.
                // Therefore we use the row number (if paging is enabled)
                return "order by RES.rn asc";
            } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)) {
                // Sometimes we are ordering by columns which are not in the join, e.g. the definition.
                // Therefore we use the row number (if paging is enabled)
                return "order by RES.rnk asc";
            } else if (AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                // Sometimes we are ordering by columns which are not in the join, e.g. the definition.
                // Therefore we use the row number (if paging is enabled)
                return "order by RES.rnum asc";
            }
        }
        // We need to do another order by in order to make sure that the final result entries are correctly sorted.
        // Some DBs will correctly remove this orderBy due to being identical to the on in the sub select.
        // Postgres, Oracle are such DBs.
        return buildOrderBy();
    }

    protected String mapOrderByToSql(OrderBy by) {
        NullHandlingOnOrder nullHandlingOnOrder = by.getNullHandlingOnOrder();
        String columnAndDirection = by.getColumnName() + " " + by.getDirection();
        if (nullHandlingOnOrder == null) {
            return columnAndDirection;
        } else if (nullHandlingOnOrder == NullHandlingOnOrder.NULLS_FIRST) {
            if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_COCKROACHDB.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                return columnAndDirection + " NULLS FIRST";
            } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)
            ) {
                // CASE WHEN <COLUMN_NAME> IS NULL
                // THEN 0 ELSE 1 END ASC,
                return "CASE WHEN " + by.getColumnName() + " IS NULL THEN 0 ELSE 1 END, " + columnAndDirection;

            } else {
                return columnAndDirection + " NULLS FIRST";
            }
        } else {
            if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_COCKROACHDB.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                return columnAndDirection + " NULLS LAST";
            } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)
            ) {
                // CASE WHEN <COLUMN_NAME> IS NULL
                // THEN 1 ELSE 0 END ASC,
                return "CASE WHEN " + by.getColumnName() + " IS NULL THEN 1 ELSE 0 END ASC, " + columnAndDirection;

            } else {
                return columnAndDirection + " NULLS LAST";
            }
        }
    }


    
    public String getOrderBy() {
        // For db2 and sqlserver, when there is paging needed, the limitBefore and limitBetween is used.
        // For those databases, the regular orderBy needs to be empty, 
        // the order will be added in the 'limitBetween' (see mssql/db2.properties).
        if (isNeedsPaging()
                && (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType) || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) ) {
            return "";
        } else {
            return buildOrderBy();
        }
    }
    
    protected boolean hasOrderByForColumn(String name) {
        for (OrderBy orderBy : getOrderByCollectionSafe()) {
            if (name.equals(orderBy.getColumnName())) {
                return true;
            }
        }

        return false;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatabaseType() {
        return databaseType;
    }

    public String getNullHandlingColumn() {
        return nullHandlingColumn;
    }

    public void setNullHandlingColumn(String nullHandlingColumn) {
        this.nullHandlingColumn = nullHandlingColumn;
    }

}
