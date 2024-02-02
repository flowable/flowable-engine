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

    public static final String EXIT_TYPE_DEFAULT = "default";
    public static final String EXIT_TYPE_ACTIVE_INSTANCES = "activeInstances";
    public static final String EXIT_TYPE_ACTIVE_AND_ENABLED_INSTANCES = "activeAndEnabledInstances";

    public static final String EXIT_EVENT_TYPE_EXIT = "exit";
    public static final String EXIT_EVENT_TYPE_COMPLETE = "complete";
    public static final String EXIT_EVENT_TYPE_FORCE_COMPLETE = "forceComplete";

    protected String technicalId;
    protected String sentryRef;
    protected Sentry sentry;
    protected String attachedToRefId;
    protected boolean isEntryCriterion;
    protected boolean isExitCriterion;

    /**
     * The exit type is only available (used) on an exit sentry attached to a plan item and represents on how it affects its plan item instances when triggered.
     */
    protected String exitType;

    /**
     * The exit event type is only available (used) on an exit sentry attached to a case plan model or a stage and represents which event will be triggered
     * when exiting the stage or case plan model.
     */
    protected String exitEventType;
    protected List<Association> incomingAssociations = new ArrayList<>();
    protected List<Association> outgoingAssociations = new ArrayList<>();

    public String getTechnicalId() {
        return technicalId;
    }
    public void setTechnicalId(String technicalId) {
        this.technicalId = technicalId;
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
    public String getExitType() {
        return exitType;
    }
    public void setExitType(String exitType) {
        this.exitType = exitType;
    }
    public String getExitEventType() {
        return exitEventType;
    }
    public void setExitEventType(String exitEventType) {
        this.exitEventType = exitEventType;
    }
    @Override
    public void addIncomingAssociation(Association association) {
        this.incomingAssociations.add(association);
    }
    @Override
    public List<Association> getIncomingAssociations() {
        return incomingAssociations;
    }
    @Override
    public void setIncomingAssociations(List<Association> incomingAssociations) {
        this.incomingAssociations = incomingAssociations;
    }
    @Override
    public void addOutgoingAssociation(Association association) {
        this.outgoingAssociations.add(association);
    }
    @Override
    public List<Association> getOutgoingAssociations() {
        return outgoingAssociations;
    }
    @Override
    public void setOutgoingAssociations(List<Association> outgoingAssociations) {
        this.outgoingAssociations = outgoingAssociations;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (isEntryCriterion) {
            stringBuilder.append("Entry criterion");
        } else {
            stringBuilder.append("Exit criterion");
        }
        stringBuilder.append(" with id '").append(id).append("'");
        return stringBuilder.toString();
    }
}
