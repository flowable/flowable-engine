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
package org.flowable.cmmn.api;

/**
 * @author Joram Barrez
 */
public interface CallbackTypes {
    
    String PLAN_ITEM_CHILD_CASE = "cmmn-1.1-to-cmmn-1.1-child-case";
    
    String PLAN_ITEM_CHILD_PROCESS = "cmmn-1.1-to-bpmn-2.0-child-process";

    String CASE_ADHOC_CHILD = "cmmn-1.1-child";

}
