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
package org.flowable.dmn.api;

import org.flowable.engine.common.api.FlowableException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Yvo Swillens
 */
public class DmnDecisionResult {

    protected List<Map<String, Object>> ruleResults = new ArrayList<>();

    public DmnDecisionResult(List<Map<String, Object>> ruleResults) {
        this.ruleResults = ruleResults;
    }

    public List<Map<String, Object>> getRuleResults() {
        return ruleResults;
    }

    public List<Object> getOutputValues(String outputName) {
        List<Object> outputValues = new ArrayList<>();
        for (Map<String, Object> ruleOutputValues : ruleResults) {
            outputValues.add(ruleOutputValues.get(outputName));
        }
        return outputValues;
    }

    public Object getFirstOutputValue(String outputName) {
        List<Object> outputValues = getOutputValues(outputName);
        if (outputValues.size() > 0){
            return outputValues.get(0);
        } else {
            return null;
        }
    }

    public Object getSingleOutputValue(String outputName) {
        List<Object> outputValues = getOutputValues(outputName);
        if (outputValues.isEmpty()) {
            return null;
        } else if (outputValues.size() > 1) {
            throw new FlowableException("Output has multiple values");
        } else {
            return outputValues.get(0);
        }
    }

    public Map<String, Object> getFirstRuleResult() {
        if (ruleResults.size() > 0) {
            return ruleResults.get(0);
        } else {
            return null;
        }
    }

    public Map<String, Object> getSingleRuleResult() {
        if (ruleResults.isEmpty()) {
            return null;
        } else if (ruleResults.size() > 1) {
            throw new FlowableException("Decision has multiple results");
        } else {
            return ruleResults.get(0);
        }
    }
}
