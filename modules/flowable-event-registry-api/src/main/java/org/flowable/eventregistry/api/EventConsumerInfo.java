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
package org.flowable.eventregistry.api;

import java.util.StringJoiner;

public class EventConsumerInfo {

    protected String eventSubscriptionId;
    protected String subScopeId;
    protected String scopeDefinitionId;
    protected String scopeType;
    protected boolean hasExistingInstancesForUniqueCorrelation;
    
    public EventConsumerInfo() {}
    
    public EventConsumerInfo(String eventSubscriptionId, String subScopeId, String scopeDefinitionId, String scopeType) {
        this.eventSubscriptionId = eventSubscriptionId;
        this.subScopeId = subScopeId;
        this.scopeDefinitionId = scopeDefinitionId;
        this.scopeType = scopeType;
    }
    
    public String getEventSubscriptionId() {
        return eventSubscriptionId;
    }
    public void setEventSubscriptionId(String eventSubscriptionId) {
        this.eventSubscriptionId = eventSubscriptionId;
    }
    public String getSubScopeId() {
        return subScopeId;
    }
    public void setSubScopeId(String subScopeId) {
        this.subScopeId = subScopeId;
    }
    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }
    public void setScopeDefinitionId(String scopeDefinitionId) {
        this.scopeDefinitionId = scopeDefinitionId;
    }
    public String getScopeType() {
        return scopeType;
    }
    public void setScopeType(String scopeType) {
        this.scopeType = scopeType;
    }
    public boolean isHasExistingInstancesForUniqueCorrelation() {
        return hasExistingInstancesForUniqueCorrelation;
    }
    public void setHasExistingInstancesForUniqueCorrelation(boolean hasExistingInstancesForUniqueCorrelation) {
        this.hasExistingInstancesForUniqueCorrelation = hasExistingInstancesForUniqueCorrelation;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", getClass().getSimpleName() + "[", "]")
                .add("eventSubscriptionId='" + eventSubscriptionId + "'")
                .add("subScopeId='" + subScopeId + "'")
                .add("scopeType='" + scopeType + "'")
                .add("scopeDefinitionId='" + scopeDefinitionId + "'")
                .add("hasExistingInstancesForUniqueCorrelation=" + hasExistingInstancesForUniqueCorrelation)
                .toString();
    }
}
