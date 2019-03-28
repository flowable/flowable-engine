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
public interface PlanItemTransition {
    
    // See 5.4.6.3.1 of cmmn 1.1 spec
    
    String CLOSE = "close";
    String COMPLETE = "complete";
    String CREATE = "create";
    String DISABLE = "disable";
    String ENABLE = "enable";
    String EXIT = "exit";
    String FAULT= "fault";
    String MANUAL_START = "manualStart";
    String OCCUR = "occur";
    String PARENT_RESUME = "parentResume";
    String PARENT_SUSPEND = "parentSuspend";
    String REACTIVATE = "reactivate";
    String REENABLE = "reenable";
    String RESUME = "resume";
    String START = "start";
    String SUSPEND = "suspend";
    String TERMINATE = "terminate";
    
    /* Non-spec transition from async-active to active */
    String ASYNC_ACTIVATE = "async-activate";

    /* Flowable-specific: for event listeners only when going from unavailable -> available (and vice versa) */
    String INITIATE = "initiate";
    String DISMISS = "dismiss";
    
}
