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

import java.util.ArrayList;
import java.util.List;

import org.flowable.cmmn.api.listener.PlanItemInstanceLifeCycleListener;
import org.flowable.cmmn.api.runtime.PlanItemInstance;

/**
 * @author Joram Barrez
 */
public abstract class AbstractTestLifeCycleListener implements PlanItemInstanceLifeCycleListener {

    protected List<TestLifeCycleEvent> events = new ArrayList<>();

    @Override
    public void stateChanged(PlanItemInstance planItemInstance, String oldState, String newState) {
        events.add(new TestLifeCycleEvent(planItemInstance, oldState, newState));
    }

    public List<TestLifeCycleEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TestLifeCycleEvent> events) {
        this.events = events;
    }

    public void clear() {
        this.events.clear();;
    }

}
