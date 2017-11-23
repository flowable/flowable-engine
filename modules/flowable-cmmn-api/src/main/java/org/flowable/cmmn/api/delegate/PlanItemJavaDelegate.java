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
package org.flowable.cmmn.api.delegate;

/**
 * Convenience class to be used when needing to execute custom logic for a plan item by delegating to a Java class.
 * After the logic has been executed, the engine will continue executing the case model by automatically completing associated plan item.
 * 
 * Use the more generic CmmnActivityBehavior or CmmnTriggerableActivityBehavior when wait state behavior is wanted.  
 * 
 * @author Joram Barrez
 */
public interface PlanItemJavaDelegate {
    
    void execute(DelegatePlanItemInstance planItemInstance);

}
