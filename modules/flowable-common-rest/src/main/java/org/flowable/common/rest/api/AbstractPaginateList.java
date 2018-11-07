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

package org.flowable.common.rest.api;

import java.util.List;
import java.util.Map;

import org.flowable.common.engine.api.query.Query;
import org.flowable.common.engine.api.query.QueryProperty;

/**
 * @author Tijs Rademakers
 * @deprecated use {@link PaginateListUtil} and {@link ListProcessor} instead
 */
@Deprecated
public abstract class AbstractPaginateList<RES, REQ> {

    /**
     * Uses the pagination parameters form the request and makes sure to order the result and set all pagination attributes for the response to render.
     *
     * @param requestParams
     *            The request containing the pagination parameters
     * @param paginateRequest
     * @param query
     *            The query to get the paged list from
     * @param defaultSort
     *            The default sort column (the rest attribute) that later will be mapped to an internal engine name
     * @param properties
     * @deprecated use {@link PaginateListUtil#paginateList(Map, PaginateRequest, Query, String, Map, ListProcessor)} instead
     */
    @Deprecated
    public DataResponse<RES> paginateList(Map<String, String> requestParams, PaginateRequest paginateRequest, Query<?, REQ> query, String defaultSort, Map<String, QueryProperty> properties) {

        return PaginateListUtil.paginateList(requestParams, paginateRequest, query, defaultSort, properties, this::processList);
    }

    /**
     * Uses the pagination parameters from the request and makes sure to order the result and set all pagination attributes for the response to render.
     *
     * @param requestParams
     *            The request containing the pagination parameters
     * @param query
     *            The query to get the paged list from
     * @param defaultSort
     *            The default sort column (the rest attribute) that later will be mapped to an internal engine name
     * @param properties
     *
     * @deprecated user {@link PaginateListUtil#paginateList(Map, Query, String, Map, ListProcessor)} instead
     */
    @Deprecated
    public DataResponse<RES> paginateList(Map<String, String> requestParams, Query<?, REQ> query, String defaultSort, Map<String, QueryProperty> properties) {
        return paginateList(requestParams, null, query, defaultSort, properties);
    }

    protected abstract List<RES> processList(List<REQ> list);
}
