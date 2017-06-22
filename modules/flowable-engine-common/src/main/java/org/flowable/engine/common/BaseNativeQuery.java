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
package org.flowable.engine.common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.flowable.engine.common.api.query.NativeQuery;

public abstract class BaseNativeQuery<T extends NativeQuery<?, ?>, U> implements NativeQuery<T, U>, Serializable {

    private static final long serialVersionUID = 1L;

    protected static enum ResultType {
        LIST, LIST_PAGE, SINGLE_RESULT, COUNT
    }

    protected int maxResults = -1;
    protected int firstResult = -1;
    protected ResultType resultType;

    protected Map<String, Object> parameters = new HashMap<String, Object>();
    protected String sqlStatement;

    @SuppressWarnings("unchecked")
    public T sql(String sqlStatement) {
        this.sqlStatement = sqlStatement;
        return (T) this;
    }

    @SuppressWarnings("unchecked")
    public T parameter(String name, Object value) {
        parameters.put(name, value);
        return (T) this;
    }

    protected Map<String, Object> getParameterMap() {
        HashMap<String, Object> parameterMap = new HashMap<String, Object>();
        parameterMap.put("sql", sqlStatement);
        parameterMap.putAll(parameters);
        return parameterMap;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

}
