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
public class TimerEventListener extends EventListener {
    
    protected String timerExpression;
    protected String timerStartTriggerSourceRef;
    protected PlanItem timerStartTriggerPlanItem;
    protected String timerStartTriggerStandardEvent;
    
    public String getTimerExpression() {
        return timerExpression;
    }
    public void setTimerExpression(String timerExpression) {
        this.timerExpression = timerExpression;
    }
    public String getTimerStartTriggerSourceRef() {
        return timerStartTriggerSourceRef;
    }
    public void setTimerStartTriggerSourceRef(String timerStartTriggerSourceRef) {
        this.timerStartTriggerSourceRef = timerStartTriggerSourceRef;
    }
    public PlanItem getTimerStartTriggerPlanItem() {
        return timerStartTriggerPlanItem;
    }
    public void setTimerStartTriggerPlanItem(PlanItem timerStartTriggerPlanItem) {
        this.timerStartTriggerPlanItem = timerStartTriggerPlanItem;
    }
    public String getTimerStartTriggerStandardEvent() {
        return timerStartTriggerStandardEvent;
    }
    public void setTimerStartTriggerStandardEvent(String timerStartTriggerStandardEvent) {
        this.timerStartTriggerStandardEvent = timerStartTriggerStandardEvent;
    }
    
}
