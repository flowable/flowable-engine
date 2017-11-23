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

package org.flowable.rest.service.api.management;

import io.swagger.annotations.ApiModelProperty;

/**
 * @author Frederik Heremans
 */
public class TableResponse {

    protected String name;
    protected String url;
    protected Long count;

    @ApiModelProperty(example = "ACT_RU_VARIABLE")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "http://localhost:8080/flowable-rest/service/management/tables/ACT_RU_VARIABLE")
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    @ApiModelProperty(example = "4528")
    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
