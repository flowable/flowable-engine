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
package org.flowable.form.model;

/**
 * @author Joram Barrez
 */
public interface FormFieldTypes {

    String SINGLE_LINE_TEXT = "text";

    String MULTI_LINE_TEXT = "multi-line-text";

    String INTEGER = "integer";
    
    String DECIMAL = "decimal";

    String AMOUNT = "amount";

    String DATE = "date";

    String BOOLEAN = "boolean";

    String RADIO_BUTTONS = "radio-buttons";

    String DROPDOWN = "dropdown";

    String UPLOAD = "upload";

    String EXPRESSION = "expression";

    String PEOPLE = "people";

    String FUNCTIONAL_GROUP = "functional-group";

    String CONTAINER = "container";
    
    String HYPERLINK = "hyperlink";
    
    String SPACER = "spacer";
    
    String HORIZONTAL_LINE = "horizontal-line";
    
    String HEADLINE = "headline";
    
    String HEADLINE_WITH_LINE = "headline-with-line";
}
