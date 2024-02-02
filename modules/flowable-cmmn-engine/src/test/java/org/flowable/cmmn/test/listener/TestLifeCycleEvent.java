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
package org.flowable.cmmn.test.listener;

import org.flowable.cmmn.api.runtime.PlanItemInstance;

/**
 * @author Joram Barrez
 */
public class TestLifeCycleEvent {

    protected PlanItemInstance planItemInstance;
    protected String oldState;
    protected String newState;

    public TestLifeCycleEvent(PlanItemInstance planItemInstance, String oldState, String newState) {
        this.planItemInstance = planItemInstance;
        this.oldState = oldState;
        this.newState = newState;
    }

    public PlanItemInstance getPlanItemInstance() {
        return planItemInstance;
    }
    public void setPlanItemInstance(PlanItemInstance planItemInstance) {
        this.planItemInstance = planItemInstance;
    }
    public String getOldState() {
        return oldState;
    }
    public void setOldState(String oldState) {
        this.oldState = oldState;
    }
    public String getNewState() {
        return newState;
    }
    public void setNewState(String newState) {
        this.newState = newState;
    }

}
