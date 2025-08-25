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

import io.swagger.annotations.ApiParam;

/**
 * Interface representing a paginated request object, use when paging is needed without using URL-parameters.
 * 
 * @see PaginateListUtil
 * 
 * @author Frederik Heremans
 */
public class PaginateRequest {

    protected Integer start;

    protected Integer size;

    protected String sort;

    protected String order;

    public Integer getStart() {
        return start;
    }

    @ApiParam(value = "From the paginate request. Index of the first row to fetch. Defaults to 0.")
    public void setStart(Integer start) {
        this.start = start;
    }

    public Integer getSize() {
        return size;
    }

    @ApiParam(name = "size", type = "integer", value = "From the paginate request. Number of rows to fetch, starting from start. Defaults to 10.")
    public void setSize(Integer size) {
        this.size = size;
    }

    public String getSort() {
        return sort;
    }

    @ApiParam(value = "Property to sort the results on")
    public void setSort(String sort) {
        this.sort = sort;
    }

    public String getOrder() {
        return order;
    }

    @ApiParam(name = "order", type = "string", value = "From the paginate request.  The sort order, either 'asc' or 'desc'. Defaults to 'asc'.")
    public void setOrder(String order) {
        this.order = order;
    }
}
