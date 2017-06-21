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

package org.flowable.engine.common.impl.db;

import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.api.query.Query.NullHandlingOnOrder;
import org.flowable.engine.common.api.query.QueryProperty;

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

    protected int firstResult = 0;
    protected int maxResults = Integer.MAX_VALUE;
    protected Object parameter;
    protected String orderBy;
    protected QueryProperty orderProperty;
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

        if (orderBy == null) {
            orderBy = "";
        } else {
            orderBy = orderBy + ", ";
        }

        String defaultOrderByClause = column + " " + sortOrder;

        if (nullHandlingOnOrder != null) {

            if (nullHandlingOnOrder == NullHandlingOnOrder.NULLS_FIRST) {

                if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                        || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                    orderBy = orderBy + defaultOrderByClause + " NULLS FIRST";
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)) {
                    orderBy = orderBy + "isnull(" + column + ") desc," + defaultOrderByClause;
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType) || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                    orderBy = orderBy + "case when " + column + " is null then 0 else 1 end," + defaultOrderByClause;
                } else {
                    orderBy = orderBy + defaultOrderByClause;
                }

            } else if (nullHandlingOnOrder == NullHandlingOnOrder.NULLS_LAST) {

                if (AbstractEngineConfiguration.DATABASE_TYPE_H2.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_HSQL.equals(databaseType)
                        || AbstractEngineConfiguration.DATABASE_TYPE_POSTGRES.equals(databaseType) 
                        || AbstractEngineConfiguration.DATABASE_TYPE_ORACLE.equals(databaseType)) {
                    orderBy = orderBy + column + " " + sortOrder + " NULLS LAST";
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_MYSQL.equals(databaseType)) {
                    orderBy = orderBy + "isnull(" + column + ") asc," + defaultOrderByClause;
                } else if (AbstractEngineConfiguration.DATABASE_TYPE_DB2.equals(databaseType) || AbstractEngineConfiguration.DATABASE_TYPE_MSSQL.equals(databaseType)) {
                    orderBy = orderBy + "case when " + column + " is null then 1 else 0 end," + defaultOrderByClause;
                } else {
                    orderBy = orderBy + defaultOrderByClause;
                }

            }

        } else {
            orderBy = orderBy + defaultOrderByClause;
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
        if (orderBy == null) {
            // the default order column
            return "RES.ID_ asc";
        } else {
            return orderBy;
        }
    }

    public String getOrderByColumns() {
        return getOrderBy();
    }
    
    public boolean isLimitActive() {
        return resultType == ResultType.LIST_PAGE;
    }

    public void setDatabaseType(String databaseType) {
        this.databaseType = databaseType;
    }

    public String getDatabaseType() {
        return databaseType;
    }

}
