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
package org.flowable.cmmn.engine.impl.el;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.flowable.common.engine.api.delegate.FlowableExpressionEnhancer;

/**
 * @author Joram Barrez
 */
public class VariableEqualsExpressionEnhancer implements FlowableExpressionEnhancer {
    
    // Regex:
    // - starts with variables or vars or var, followed by :
    // - followed by equals or eq
    // - followed by 0 or more whitespaces
    // - followed by a paranthese
    // - followed by 0 or more whitespaces
    // - Optionally followed by a single our double quote
    // - word group
    // - Optionally followed by a single our double quote
    // - followed by 0 or more whitespaces
    private static Pattern PATTERN = Pattern.compile("(variables|vars|var):(equals|eq)\\s*\\(\\s*'?\"?(.*?)'?\"?\\s*,");
    
    private static String REPLACE_PATTERN = "variables:equals(planItemInstance,'$3',"; // 3th word group: variables/equals are two first groups

    @Override
    public String enhance(String expressionText) {
        Matcher matcher = PATTERN.matcher(expressionText);
        if (matcher.find()) {
            return matcher.replaceAll(REPLACE_PATTERN);
        }
        return expressionText;
    }
    
}
