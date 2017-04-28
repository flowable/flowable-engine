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
package org.flowable.bpm.model.bpmn.impl.instance;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IS_CLOSED;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IS_EXECUTABLE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_PROCESS_TYPE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_PROCESS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_CANDIDATE_STARTER_GROUPS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_CANDIDATE_STARTER_USERS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.ProcessType;
import org.flowable.bpm.model.bpmn.builder.ProcessBuilder;
import org.flowable.bpm.model.bpmn.instance.Artifact;
import org.flowable.bpm.model.bpmn.instance.Auditing;
import org.flowable.bpm.model.bpmn.instance.CallableElement;
import org.flowable.bpm.model.bpmn.instance.CorrelationSubscription;
import org.flowable.bpm.model.bpmn.instance.FlowElement;
import org.flowable.bpm.model.bpmn.instance.LaneSet;
import org.flowable.bpm.model.bpmn.instance.Monitoring;
import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.Property;
import org.flowable.bpm.model.bpmn.instance.ResourceRole;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.impl.util.StringUtil;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElement;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;
import org.flowable.bpm.model.xml.type.reference.ElementReferenceCollection;

import java.util.Collection;
import java.util.List;

/**
 * The BPMN process element
 */
public class ProcessImpl
        extends CallableElementImpl
        implements Process {

    protected static Attribute<ProcessType> processTypeAttribute;
    protected static Attribute<Boolean> isClosedAttribute;
    protected static Attribute<Boolean> isExecutableAttribute;
    // TODO: definitionalCollaborationRef
    protected static ChildElement<Auditing> auditingChild;
    protected static ChildElement<Monitoring> monitoringChild;
    protected static ChildElementCollection<Property> propertyCollection;
    protected static ChildElementCollection<LaneSet> laneSetCollection;
    protected static ChildElementCollection<FlowElement> flowElementCollection;
    protected static ChildElementCollection<Artifact> artifactCollection;
    protected static ChildElementCollection<ResourceRole> resourceRoleCollection;
    protected static ChildElementCollection<CorrelationSubscription> correlationSubscriptionCollection;
    protected static ElementReferenceCollection<Process, Supports> supportsCollection;

    /* Flowable extensions */

    protected static Attribute<String> flowableCandidateStarterGroupsAttribute;
    protected static Attribute<String> flowableCandidateStarterUsersAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(Process.class, BPMN_ELEMENT_PROCESS)
                .namespaceUri(BPMN20_NS)
                .extendsType(CallableElement.class)
                .instanceProvider(new ModelTypeInstanceProvider<Process>() {
                    public Process newInstance(ModelTypeInstanceContext instanceContext) {
                        return new ProcessImpl(instanceContext);
                    }
                });

        processTypeAttribute = typeBuilder.enumAttribute(BPMN_ATTRIBUTE_PROCESS_TYPE, ProcessType.class)
                .defaultValue(ProcessType.None)
                .build();

        isClosedAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_CLOSED)
                .defaultValue(false)
                .build();

        isExecutableAttribute = typeBuilder.booleanAttribute(BPMN_ATTRIBUTE_IS_EXECUTABLE)
                .build();

        // TODO: definitionalCollaborationRef

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        auditingChild = sequenceBuilder.element(Auditing.class)
                .build();

        monitoringChild = sequenceBuilder.element(Monitoring.class)
                .build();

        propertyCollection = sequenceBuilder.elementCollection(Property.class)
                .build();

        laneSetCollection = sequenceBuilder.elementCollection(LaneSet.class)
                .build();

        flowElementCollection = sequenceBuilder.elementCollection(FlowElement.class)
                .build();

        artifactCollection = sequenceBuilder.elementCollection(Artifact.class)
                .build();

        resourceRoleCollection = sequenceBuilder.elementCollection(ResourceRole.class)
                .build();

        correlationSubscriptionCollection = sequenceBuilder.elementCollection(CorrelationSubscription.class)
                .build();

        supportsCollection = sequenceBuilder.elementCollection(Supports.class)
                .qNameElementReferenceCollection(Process.class)
                .build();

        /* Flowable extensions */

        flowableCandidateStarterGroupsAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_CANDIDATE_STARTER_GROUPS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableCandidateStarterUsersAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_CANDIDATE_STARTER_USERS)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public ProcessImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public ProcessBuilder builder() {
        return new ProcessBuilder((BpmnModelInstance) modelInstance, this);
    }

    public ProcessType getProcessType() {
        return processTypeAttribute.getValue(this);
    }

    public void setProcessType(ProcessType processType) {
        processTypeAttribute.setValue(this, processType);
    }

    public boolean isClosed() {
        return isClosedAttribute.getValue(this);
    }

    public void setClosed(boolean closed) {
        isClosedAttribute.setValue(this, closed);
    }

    public boolean isExecutable() {
        return isExecutableAttribute.getValue(this);
    }

    public void setExecutable(boolean executable) {
        isExecutableAttribute.setValue(this, executable);
    }

    public Auditing getAuditing() {
        return auditingChild.getChild(this);
    }

    public void setAuditing(Auditing auditing) {
        auditingChild.setChild(this, auditing);
    }

    public Monitoring getMonitoring() {
        return monitoringChild.getChild(this);
    }

    public void setMonitoring(Monitoring monitoring) {
        monitoringChild.setChild(this, monitoring);
    }

    public Collection<Property> getProperties() {
        return propertyCollection.get(this);
    }

    public Collection<LaneSet> getLaneSets() {
        return laneSetCollection.get(this);
    }

    public Collection<FlowElement> getFlowElements() {
        return flowElementCollection.get(this);
    }

    public Collection<Artifact> getArtifacts() {
        return artifactCollection.get(this);
    }

    public Collection<CorrelationSubscription> getCorrelationSubscriptions() {
        return correlationSubscriptionCollection.get(this);
    }

    public Collection<ResourceRole> getResourceRoles() {
        return resourceRoleCollection.get(this);
    }

    public Collection<Process> getSupports() {
        return supportsCollection.getReferenceTargetElements(this);
    }

    /* Flowable extensions */

    public String getFlowableCandidateStarterGroups() {
        return flowableCandidateStarterGroupsAttribute.getValue(this);
    }

    public void setFlowableCandidateStarterGroups(String flowableCandidateStarterGroups) {
        flowableCandidateStarterGroupsAttribute.setValue(this, flowableCandidateStarterGroups);
    }

    public List<String> getFlowableCandidateStarterGroupsList() {
        String groupsString = flowableCandidateStarterGroupsAttribute.getValue(this);
        return StringUtil.splitCommaSeparatedList(groupsString);
    }

    public void setFlowableCandidateStarterGroupsList(List<String> flowableCandidateStarterGroupsList) {
        String candidateStarterGroups = StringUtil.joinCommaSeparatedList(flowableCandidateStarterGroupsList);
        flowableCandidateStarterGroupsAttribute.setValue(this, candidateStarterGroups);
    }

    public String getFlowableCandidateStarterUsers() {
        return flowableCandidateStarterUsersAttribute.getValue(this);
    }

    public void setFlowableCandidateStarterUsers(String flowableCandidateStarterUsers) {
        flowableCandidateStarterUsersAttribute.setValue(this, flowableCandidateStarterUsers);
    }

    public List<String> getFlowableCandidateStarterUsersList() {
        String candidateStarterUsers = flowableCandidateStarterUsersAttribute.getValue(this);
        return StringUtil.splitCommaSeparatedList(candidateStarterUsers);
    }

    public void setFlowableCandidateStarterUsersList(List<String> flowableCandidateStarterUsersList) {
        String candidateStarterUsers = StringUtil.joinCommaSeparatedList(flowableCandidateStarterUsersList);
        flowableCandidateStarterUsersAttribute.setValue(this, candidateStarterUsers);
    }
}
