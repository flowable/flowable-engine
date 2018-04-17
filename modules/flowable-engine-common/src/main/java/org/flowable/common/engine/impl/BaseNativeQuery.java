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
package org.flowable.common.engine.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.flowable.common.engine.api.query.NativeQuery;

public abstract class BaseNativeQuery<T extends NativeQuery<?, ?>, U> implements NativeQuery<T, U>, Serializable {

    private static final long serialVersionUID = 1L;

    protected static enum ResultType {
        LIST, LIST_PAGE, SINGLE_RESULT, COUNT
    }

    protected int maxResults = -1;
    protected int firstResult = -1;
    protected ResultType resultType;

    protected Map<String, Object> parameters = new HashMap<>();
    protected String sqlStatement;

    @SuppressWarnings("unchecked")
    @Override
    public T sql(String sqlStatement) {
        this.sqlStatement = sqlStatement;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T parameter(String name, Object value) {
        parameters.put(name, value);
        return (T) this;
    }
    
    protected Map<String, Object> generateParameterMap() {
        Map<String, Object> parameterMap = new HashMap<>();
        parameterMap.putAll(parameters);
        parameterMap.put("sql", sqlStatement);
        parameterMap.put("resultType", resultType.toString());
        parameterMap.put("firstResult", firstResult);
        parameterMap.put("maxResults", maxResults);
        String orderBy = (String) parameterMap.get("orderBy");
        if (orderBy != null && !"".equals(orderBy)) {
            String columns = "RES." + orderBy;
            parameterMap.put("orderBy", columns);
            parameterMap.put("orderByColumns", columns);
        } else {
            parameterMap.put("orderBy", "order by RES.ID_ asc");
            parameterMap.put("orderByColumns", "RES.ID_ asc");
        }

        int firstRow = firstResult + 1;
        parameterMap.put("firstRow", firstRow);
        int lastRow = 0;
        if (maxResults == Integer.MAX_VALUE) {
            lastRow = maxResults;
        } else {
            lastRow = firstResult + maxResults + 1;
        }
        parameterMap.put("lastRow", lastRow);
        return parameterMap;
    }

    protected Map<String, Object> __generateParameterMap() {
        HashMap<String, Object> parameterMap = new HashMap<>();
        parameterMap.put("sql", sqlStatement);
        parameterMap.putAll(parameters);
        return parameterMap;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

}
