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
package org.flowable.common.engine.api.constant;

/**
 * @author Joram Barrez
 */
public interface ReferenceTypes {

    String PLAN_ITEM_CHILD_CASE = "cmmn-1.1-to-cmmn-1.1-child-case";

    String PLAN_ITEM_CHILD_PROCESS = "cmmn-1.1-to-bpmn-2.0-child-process";

    String PLAN_ITEM_CHILD_HUMAN_TASK = "cmmn-1.1-to-cmmn-1.1-child-human-task";

    String EXECUTION_CHILD_CASE = "bpmn-2.0-to-cmmn-1.1-child-case";

    String EVENT_PROCESS = "event-to-bpmn-2.0-process";

    String EVENT_CASE = "event-to-cmmn-1.1-case";

}
