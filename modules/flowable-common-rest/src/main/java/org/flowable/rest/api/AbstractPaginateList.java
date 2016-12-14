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

package org.flowable.rest.api;

import java.util.List;
import java.util.Map;

import org.flowable.engine.common.api.FlowableIllegalArgumentException;
import org.flowable.engine.common.api.query.Query;
import org.flowable.engine.common.api.query.QueryProperty;

/**
 * @author Tijs Rademakers
 */
public abstract class AbstractPaginateList {

    /**
     * Uses the pagination parameters form the request and makes sure to order the result and set all pagination attributes for the response to render
     *
     * @param requestParams The request containing the pagination parameters
     * @param query         The query to get the paged list from
     * @param defaultSort   The default sort column (the rest attribute) that later will be mapped to an internal engine name
     * @param listName      The name model attribute name to use for the result list
     * @param model         The model to put the list and the pagination attributes in
     */
    @SuppressWarnings("rawtypes")
    public DataResponse paginateList(Map<String, String> requestParams, PaginateRequest paginateRequest, Object query, String defaultSort, Map properties) {

        if (paginateRequest == null) {
            paginateRequest = new PaginateRequest();
        }

        // In case pagination request is incomplete, fill with values found in URL if possible
        if (paginateRequest.getStart() == null) {
            paginateRequest.setStart(RequestUtil.getInteger(requestParams, "start", 0));
        }

        if (paginateRequest.getSize() == null) {
            paginateRequest.setSize(RequestUtil.getInteger(requestParams, "size", 10));
        }

        if (paginateRequest.getOrder() == null) {
            paginateRequest.setOrder(requestParams.get("order"));
        }

        if (paginateRequest.getSort() == null) {
            paginateRequest.setSort(requestParams.get("sort"));
        }

        // Use defaults for paging, if not set in the PaginationRequest, nor in the URL
        Integer start = paginateRequest.getStart();
        if (start == null || start < 0) {
            start = 0;
        }

        Integer size = paginateRequest.getSize();
        if (size == null || size < 0) {
            size = 10;
        }

        String sort = paginateRequest.getSort();
        if (sort == null) {
            sort = defaultSort;
        }
        String order = paginateRequest.getOrder();
        if (order == null) {
            order = "asc";
        }

        // Sort order
        if (sort != null && !properties.isEmpty()) {
            Object qp = properties.get(sort);
            if (qp == null) {
                throw new FlowableIllegalArgumentException("Value for param 'sort' is not valid, '" + sort + "' is not a valid property");
            }

            if (query instanceof Query) {
                Query queryObject = (Query) query;
                QueryProperty queryProperty = (QueryProperty) qp;
                queryObject.orderBy(queryProperty);
                if (order.equals("asc")) {
                    queryObject.asc();
                } else if (order.equals("desc")) {
                    queryObject.desc();
                } else {
                    throw new FlowableIllegalArgumentException("Value for param 'order' is not valid : '" + order + "', must be 'asc' or 'desc'");
                }
            }
        }

        DataResponse response = new DataResponse();
        response.setStart(start);
        response.setSort(sort);
        response.setOrder(order);

        Query queryObject = (Query) query;

        // Get result and set pagination parameters
        List list = processList(queryObject.listPage(start, size));
        if (start == 0 && list.size() < size) {
          response.setTotal(list.size());
        } else {
          response.setTotal(queryObject.count());
        }
        
        response.setSize(list.size());
        response.setData(list);

        return response;
    }

    /**
     * Uses the pagination parameters from the request and makes sure to order the result and set all pagination attributes for the response to render
     *
     * @param requestParams The request containing the pagination parameters
     * @param query         The query to get the paged list from
     * @param defaultSort   The default sort column (the rest attribute) that later will be mapped to an internal engine name
     * @param listName      The name model attribute name to use for the result list
     * @param model         The model to put the list and the pagination attributes in
     */
    @SuppressWarnings("rawtypes")
    public DataResponse paginateList(Map<String, String> requestParams, Object query, String defaultSort, Map properties) {
        return paginateList(requestParams, null, query, defaultSort, properties);
    }

    @SuppressWarnings("rawtypes")
    protected abstract List processList(List list);
}
