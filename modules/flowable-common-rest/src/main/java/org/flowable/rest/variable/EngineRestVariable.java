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
package org.flowable.rest.variable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Pojo representing a variable used in REST-service which defines it's name, variable and type.
 *
 * @author Yvo Swillens
 */
public class EngineRestVariable {

    private String name;
    private String type;
    private Object value;
    private String valueUrl;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setValueUrl(String valueUrl) {
        this.valueUrl = valueUrl;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getValueUrl() {
        return valueUrl;
    }

}
