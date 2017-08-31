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

import java.util.ArrayList;
import java.util.List;

/**
 * @author Joram Barrez
 */
public class Criterion extends CaseElement implements HasAssociations {
    
    protected String name;
    protected String sentryRef;
    protected Sentry sentry;
    protected String attachedToRefId;
    protected BaseElement attachedToRef;
    protected boolean isEntryCriterion;
    protected boolean isExitCriterion;
    protected List<Association> incomingAssociations = new ArrayList<>();
    protected List<Association> outgoingAssociations = new ArrayList<>();
    
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    public String getSentryRef() {
        return sentryRef;
    }
    public void setSentryRef(String sentryRef) {
        this.sentryRef = sentryRef;
    }
    public Sentry getSentry() {
        return sentry;
    }
    public void setSentry(Sentry sentry) {
        this.sentry = sentry;
    }
    public String getAttachedToRefId() {
        return attachedToRefId;
    }
    public void setAttachedToRefId(String attachedToRefId) {
        this.attachedToRefId = attachedToRefId;
    }
    public BaseElement getAttachedToRef() {
        return attachedToRef;
    }
    public void setAttachedToRef(BaseElement attachedToRef) {
        this.attachedToRef = attachedToRef;
    }
    public boolean isEntryCriterion() {
        return isEntryCriterion;
    }
    public void setEntryCriterion(boolean isEntryCriterion) {
        this.isEntryCriterion = isEntryCriterion;
    }
    public boolean isExitCriterion() {
        return isExitCriterion;
    }
    public void setExitCriterion(boolean isExitCriterion) {
        this.isExitCriterion = isExitCriterion;
    }
    public void addIncomingAssociation(Association association) {
        this.incomingAssociations.add(association);
    }
    public List<Association> getIncomingAssociations() {
        return incomingAssociations;
    }
    public void setIncomingAssociations(List<Association> incomingAssociations) {
        this.incomingAssociations = incomingAssociations;
    }
    public void addOutgoingAssociation(Association association) {
        this.outgoingAssociations.add(association);
    }
    public List<Association> getOutgoingAssociations() {
        return outgoingAssociations;
    }
    public void setOutgoingAssociations(List<Association> outgoingAssociations) {
        this.outgoingAssociations = outgoingAssociations;
    }
}
