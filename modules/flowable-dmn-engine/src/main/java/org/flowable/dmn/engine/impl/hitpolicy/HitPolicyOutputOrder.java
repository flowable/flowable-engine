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
package org.flowable.dmn.engine.impl.hitpolicy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.builder.CompareToBuilder;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.engine.impl.el.ELExecutionContext;
import org.flowable.dmn.engine.impl.util.CommandContextUtil;
import org.flowable.dmn.model.HitPolicy;

/**
 * @author Yvo Swillens
 */
public class HitPolicyOutputOrder extends AbstractHitPolicy implements ComposeDecisionResultBehavior {

    @Override
    public String getHitPolicyName() {
        return HitPolicy.OUTPUT_ORDER.getValue();
    }

    @Override
    public void composeDecisionResults(final ELExecutionContext executionContext) {
        List<Map<String, Object>> ruleResults = new ArrayList<>(executionContext.getRuleResults().values());
        
        boolean outputValuesPresent = false;
        for (Map.Entry<String, List<Object>> entry : executionContext.getOutputValues().entrySet()) {
            List<Object> outputValues = entry.getValue();
            if (outputValues != null && !outputValues.isEmpty()) {
                outputValuesPresent = true;
                break;
            }
        }
        
        if (!outputValuesPresent) {
            String hitPolicyViolatedMessage = String.format("HitPolicy: %s violated; no output values present", getHitPolicyName());
            if (CommandContextUtil.getDmnEngineConfiguration().isStrictMode()) {
                throw new FlowableException(hitPolicyViolatedMessage);
            } else {
                executionContext.getAuditContainer().setValidationMessage(hitPolicyViolatedMessage);
            }
        }

        // sort on predefined list(s) of output values
        Collections.sort(ruleResults, new Comparator<Map<String, Object>>() {

            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                CompareToBuilder compareToBuilder = new CompareToBuilder();
                for (Map.Entry<String, List<Object>> entry : executionContext.getOutputValues().entrySet()) {
                    List<Object> outputValues = entry.getValue();
                    if (outputValues != null && !outputValues.isEmpty()) {
                        compareToBuilder.append(o1.get(entry.getKey()), o2.get(entry.getKey()), 
                                        new OutputOrderComparator<>(outputValues.toArray(new Comparable[outputValues.size()])));
                        compareToBuilder.toComparison();
                    }
                }
                return compareToBuilder.toComparison();
            }
        });

        executionContext.getAuditContainer().setDecisionResult(ruleResults);
    }
}
