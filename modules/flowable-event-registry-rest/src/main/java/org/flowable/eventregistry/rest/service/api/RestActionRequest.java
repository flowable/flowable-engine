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

package org.flowable.eventregistry.rest.service.api;

/**
 * Generic class that represents an action to be performed on a resource. Should be subclasses if additional action-parameters are required.
 * 
 * @author Frederik Heremans
 */
public class RestActionRequest {
    
    public static final String EVALUATE_CRITERIA = "evaluateCriteria";
    public static final String TRIGGER = "trigger";
    public static final String ENABLE = "enable";
    public static final String DISABLE = "disable";
    public static final String START = "start";

    private String action;

    public void setAction(String action) {
        this.action = action;
    }

    public String getAction() {
        return action;
    }
}
