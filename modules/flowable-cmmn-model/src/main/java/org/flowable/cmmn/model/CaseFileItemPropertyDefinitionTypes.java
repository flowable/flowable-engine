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
package org.flowable.cmmn.model;

/**
 * @author Joram Barrez
 */
public interface CaseFileItemPropertyDefinitionTypes {

    String TYPE_STRING = "http://www.omg.org/spec/CMMN/PropertyType/string";
    String TYPE_BOOLEAN = "http://www.omg.org/spec/CMMN/PropertyType/boolean";
    String TYPE_INTEGER = "http://www.omg.org/spec/CMMN/PropertyType/integer";
    String TYPE_FLOAT = "http://www.omg.org/spec/CMMN/PropertyType/float";
    String TYPE_DECIMAL = "http://www.omg.org/spec/CMMN/PropertyType/decimal";
    String TYPE_DOULE = "http://www.omg.org/spec/CMMN/PropertyType/double";
    String TYPE_DURATION = "http://www.omg.org/spec/CMMN/PropertyType/duration";
    String TYPE_DATE_TIME = "http://www.omg.org/spec/CMMN/PropertyType/dateTime";
    String TYPE_TIME = "http://www.omg.org/spec/CMMN/PropertyType/time";
    String TYPE_DATE = "http://www.omg.org/spec/CMMN/PropertyType/date";
    String TYPE_YEAR_MONTH = "http://www.omg.org/spec/CMMN/PropertyType/gYearMonth";
    String TYPE_YEAR = "http://www.omg.org/spec/CMMN/PropertyType/gYear";
    String TYPE_MONTH_DAY = "http://www.omg.org/spec/CMMN/PropertyType/gMonthDay";
    String TYPE_DAY = "http://www.omg.org/spec/CMMN/PropertyType/gDay";
    String TYPE_MONTH = "http://www.omg.org/spec/CMMN/PropertyType/gMonth";
    String TYPE_HEX_BINARY = "http://www.omg.org/spec/CMMN/PropertyType/hexBinary";
    String TYPE_BASE64_BINARY = "http://www.omg.org/spec/CMMN/PropertyType/base64Binary";
    String TYPE_ANY_URI = "http://www.omg.org/spec/CMMN/PropertyType/anyURI";
    String TYPE_QNAME = "http://www.omg.org/spec/CMMN/PropertyType/QName";
    String TYPE_UNSPECIFIED = "http://www.omg.org/spec/CMMN/PropertyType/Unspecified";

}
