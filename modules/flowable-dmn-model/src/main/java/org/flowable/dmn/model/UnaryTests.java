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
package org.flowable.dmn.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * @author Yvo Swillens
 */
public class UnaryTests extends DmnElement {

    protected String text;
    protected List<Object> textValues;
    protected String expressionLanguage;

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<Object> getTextValues() {
        return textValues;
    }

    public void setTextValues(List<Object> textValues) {
        this.textValues = textValues;
        this.text = "\"" + StringUtils.join(textValues, "\",\"") + "\"";

    }

    public String getExpressionLanguage() {
        return expressionLanguage;
    }

    public void setExpressionLanguage(String expressionLanguage) {
        this.expressionLanguage = expressionLanguage;
    }
}
