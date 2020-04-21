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
package org.flowable.eventregistry.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

/**
 * @author Joram Barrez
 */
@JsonInclude(Include.NON_NULL)
public class ChannelEventKeyDetection {

    protected String fixedValue;
    protected String jsonField;
    protected String jsonPointerExpression;
    protected String xmlXPathExpression;
    protected String delegateExpression;
    
    public String getFixedValue() {
        return fixedValue;
    }
    
    public void setFixedValue(String fixedValue) {
        this.fixedValue = fixedValue;
    }
    
    public String getJsonField() {
        return jsonField;
    }
    
    public void setJsonField(String jsonField) {
        this.jsonField = jsonField;
    }
    
    public String getJsonPointerExpression() {
        return jsonPointerExpression;
    }
    
    public void setJsonPointerExpression(String jsonPointerExpression) {
        this.jsonPointerExpression = jsonPointerExpression;
    }

    public String getXmlXPathExpression() {
        return xmlXPathExpression;
    }

    public void setXmlXPathExpression(String xmlXPathExpression) {
        this.xmlXPathExpression = xmlXPathExpression;
    }

    public String getDelegateExpression() {
        return delegateExpression;
    }

    public void setDelegateExpression(String delegateExpression) {
        this.delegateExpression = delegateExpression;
    }
}
