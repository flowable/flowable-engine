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
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.flowable.common.engine.api.query.Query.NullHandlingOnOrder;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.Direction;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ListQueryParameterObject {
    
    public enum ResultType {
        LIST, LIST_PAGE, SINGLE_RESULT, COUNT
    }

    protected static class OrderBy {

        protected final String columnName;
        protected final String direction;
        protected final NullHandlingOnOrder nullHandlingOnOrder;

        public OrderBy(String columnName, String direction, NullHandlingOnOrder nullHandlingOnOrder) {
            this.columnName = columnName;
            this.direction = direction;
            this.nullHandlingOnOrder = nullHandlingOnOrder;
        }
    }
    
    public static final String SORTORDER_ASC = "asc";
    public static final String SORTORDER_DESC = "desc";

    protected int firstResult = -1;
    protected int maxResults = -1;
    protected Object parameter;
    protected Map<String, Boolean> orderByColumnMap = new TreeMap<>();
    protected Collection<OrderBy> orderByCollection = new ArrayList<>();
    protected OrderBy defaultOrderBy = new OrderBy("RES.ID_", "asc", null);
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
        
        if (Direction.ASCENDING.getName().equals(sortOrder)) {
            orderByColumnMap.put(column, true);
        } else {
            orderByColumnMap.put(column, false);
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
        if (!orderByCollection.isEmpty()) {
            return true;
        }

        return defaultOrderBy != null;
    }

    public String getOrderByForWindow() {
        return buildOrderBy();
    }

    protected String buildOrderBy() {
        Collection<OrderBy> orderBy;
        if (!orderByCollection.isEmpty()) {
            orderBy = orderByCollection;
        } else if (defaultOrderBy != null) {
            orderBy = Collections.singleton(defaultOrderBy);
        } else {
            orderBy = Collections.emptyList();
        }

        return orderBy.stream()
                .map(this::mapOrderByToSql)
                .collect(Collectors.joining(",", "order by ", ""));
    }

    protected String mapOrderByToSql(OrderBy by) {
        NullHandlingOnOrder nullHandlingOnOrder = by.nullHandlingOnOrder;
        if (nullHandlingOnOrder == null) {
            return by.columnName + " " + by.direction;
        } else if (nullHandlingOnOrder == NullHandlingOnOrder.NULLS_FIRST) {
            if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_COCKROACHDB.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                return by.columnName + " " + by.direction + " NULLS FIRST";
            } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)
            ) {
                // CASE WHEN <COLUMN_NAME> IS NULL
                // THEN 0 ELSE 1 END ASC,
                return "CASE WHEN " + by.columnName + " IS NULL THEN 0 ELSE 1 END, " + by.columnName + " " + by.direction;

            } else {
                return by.columnName + " " + by.direction + " NULLS FIRST";
            }
        } else {
            if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_COCKROACHDB.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                return by.columnName + " " + by.direction + " NULLS LAST";
            } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)
                    || AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)
            ) {
                // CASE WHEN <COLUMN_NAME> IS NULL
                // THEN 1 ELSE 0 END ASC,
                return "CASE WHEN " + by.columnName + " IS NULL THEN 1 ELSE 0 END ASC, " + by.columnName + " " + by.direction;

            } else {
                return by.columnName + " " + by.direction + " NULLS LAST";
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
    
    public Map<String, Boolean> getOrderByColumnMap() {
        return orderByColumnMap;
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
