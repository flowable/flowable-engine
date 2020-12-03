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
package org.flowable.common.engine.api.scope;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * @author Joram Barrez
 * @author Filip Hrisafov
 */
public interface ScopeTypes {

    String APP = "app";
    String BPMN = "bpmn";
    String CMMN = "cmmn";
    String DMN = "dmn";
    String EVENT_REGISTRY = "eventRegistry";
    String FORM = "form";
    String PLAN_ITEM = "planItem";
    String TASK = "task";
    String EXTERNAL_WORKER = "externalWorker";

    String BPMN_VARIABLE_AGGREGATION = "bpmnVariableAggregation";
    String CMMN_VARIABLE_AGGREGATION = "cmmnVariableAggregation";

    String CMMN_EXTERNAL_WORKER = "cmmnExternalWorker";
    String BPMN_EXTERNAL_WORKER = "bpmnExternalWorker";

    Collection<String> CMMN_DEPENDENT = new HashSet<>(Arrays.asList(CMMN, CMMN_EXTERNAL_WORKER, CMMN_VARIABLE_AGGREGATION));
    Collection<String> BPMN_DEPENDENT = new HashSet<>(Arrays.asList(BPMN_EXTERNAL_WORKER, BPMN_VARIABLE_AGGREGATION));
}
