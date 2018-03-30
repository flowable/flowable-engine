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
package org.flowable.common.rest.variable;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.annotations.ApiModelProperty;

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

    @ApiModelProperty(example = "myVariable", value = "Name of the variable")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @ApiModelProperty(example = "string", value = "Type of the variable.", notes = " When writing a variable and this value is omitted, the type will be deducted from the raw JSON-attribute request type and is limited to either string, double, integer and boolean. It’s advised to always include a type to make sure no wrong assumption about the type can be done.")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @ApiModelProperty(example = "test", value = "Value of the variable.", notes = "When writing a variable and value is omitted, null will be used as value.")
    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @ApiModelProperty(example = "http://....", notes = "When reading a variable of type binary or serializable, this attribute will point to the URL where the raw binary data can be fetched from.")
    public void setValueUrl(String valueUrl) {
        this.valueUrl = valueUrl;
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public String getValueUrl() {
        return valueUrl;
    }

}
