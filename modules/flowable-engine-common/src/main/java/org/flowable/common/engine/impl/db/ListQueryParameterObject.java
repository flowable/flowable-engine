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

import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.engine.api.query.Query.NullHandlingOnOrder;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;

/**
 * @author Tijs Rademakers
 * @author Joram Barrez
 */
public class ListQueryParameterObject {
    
    public static enum ResultType {
        LIST, LIST_PAGE, SINGLE_RESULT, COUNT
    }
    
    public static final String SORTORDER_ASC = "asc";
    public static final String SORTORDER_DESC = "desc";

    protected int firstResult = -1;
    protected int maxResults = -1;
    protected Object parameter;
    protected String orderByColumns;
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
    
    protected void addOrder(String column, String sortOrder, NullHandlingOnOrder nullHandlingOnOrder) {

        if (orderByColumns == null) {
            orderByColumns = "";
        } else {
            orderByColumns = orderByColumns + ", ";
        }

        String defaultOrderByClause = column + " " + sortOrder;

        if (nullHandlingOnOrder != null) {

            if (nullHandlingOnOrder == NullHandlingOnOrder.NULLS_FIRST) {

                if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                        || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                    orderByColumns = orderByColumns + defaultOrderByClause + " NULLS FIRST";
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)) {
                    orderByColumns = orderByColumns + "isnull(" + column + ") desc," + defaultOrderByClause;
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType) || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                    if (nullHandlingColumn == null) {
                        nullHandlingColumn = "";
                    } else {
                        nullHandlingColumn = nullHandlingColumn + ", ";
                    }
                    String columnName = column.replace("RES.", "") + "_order_null";
                    nullHandlingColumn = nullHandlingColumn + "case when " + column + " is null then 0 else 1 end " + columnName;
                    orderByColumns = orderByColumns + columnName + "," + defaultOrderByClause;
                } else {
                    orderByColumns = orderByColumns + defaultOrderByClause;
                }

            } else if (nullHandlingOnOrder == NullHandlingOnOrder.NULLS_LAST) {

                if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                        || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                    orderByColumns = orderByColumns + column + " " + sortOrder + " NULLS LAST";
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)) {
                    orderByColumns = orderByColumns + "isnull(" + column + ") asc," + defaultOrderByClause;
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType) || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                    if (nullHandlingColumn == null) {
                        nullHandlingColumn = "";
                    } else {
                        nullHandlingColumn = nullHandlingColumn + ", ";
                    }
                    String columnName = column.replace("RES.", "") + "_order_null";
                    nullHandlingColumn = nullHandlingColumn + "case when " + column + " is null then 1 else 0 end " + columnName;
                    orderByColumns = orderByColumns + columnName + "," + defaultOrderByClause;
                } else {
                    orderByColumns = orderByColumns + defaultOrderByClause;
                }

            }

        } else {
            orderByColumns = orderByColumns + defaultOrderByClause;
        }

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
    
    public String getOrderBy() {
        // For db2 and sqlserver, when there is paging needed, the limitBefore and limitBetween is used.
        // For those databases, the regular orderBy needs to be empty, 
        // the order will be added in the 'limitBetween' (see mssql/db2.properties). 
        if (firstResult >= 0 
                && (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType) || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) ) {
            return "";
        } else {
            return "order by " + getOrderByColumns();
        }
    }
    
    public void setOrderByColumns(String orderByColumns) {
        this.orderByColumns = orderByColumns;
    }

    public String getOrderByColumns() {
        if (orderByColumns != null) {
            return orderByColumns;
        } else {
            return "RES.ID_ asc";
        }
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
