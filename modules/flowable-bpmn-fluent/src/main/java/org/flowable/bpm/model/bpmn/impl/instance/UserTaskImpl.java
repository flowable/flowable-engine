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
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ATTRIBUTE_IMPLEMENTATION;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN_ELEMENT_USER_TASK;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_ASSIGNEE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_CANDIDATE_GROUPS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_CANDIDATE_USERS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_DUE_DATE;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_FORM_HANDLER_CLASS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_FORM_KEY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_ATTRIBUTE_PRIORITY;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import org.flowable.bpm.model.bpmn.BpmnModelInstance;
import org.flowable.bpm.model.bpmn.builder.UserTaskBuilder;
import org.flowable.bpm.model.bpmn.instance.Rendering;
import org.flowable.bpm.model.bpmn.instance.Task;
import org.flowable.bpm.model.bpmn.instance.UserTask;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.impl.instance.ModelTypeInstanceContext;
import org.flowable.bpm.model.xml.impl.util.StringUtil;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder;
import org.flowable.bpm.model.xml.type.ModelElementTypeBuilder.ModelTypeInstanceProvider;
import org.flowable.bpm.model.xml.type.attribute.Attribute;
import org.flowable.bpm.model.xml.type.child.ChildElementCollection;
import org.flowable.bpm.model.xml.type.child.SequenceBuilder;

import java.util.Collection;
import java.util.List;

/**
 * The BPMN userTask element.
 */
public class UserTaskImpl
        extends TaskImpl
        implements UserTask {

    protected static Attribute<String> implementationAttribute;
    protected static ChildElementCollection<Rendering> renderingCollection;

    /* Flowable extensions */

    protected static Attribute<String> flowableAssigneeAttribute;
    protected static Attribute<String> flowableCandidateGroupsAttribute;
    protected static Attribute<String> flowableCandidateUsersAttribute;
    protected static Attribute<String> flowableDueDateAttribute;
    protected static Attribute<String> flowableFormHandlerClassAttribute;
    protected static Attribute<String> flowableFormKeyAttribute;
    protected static Attribute<String> flowablePriorityAttribute;

    public static void registerType(ModelBuilder modelBuilder) {
        ModelElementTypeBuilder typeBuilder = modelBuilder.defineType(UserTask.class, BPMN_ELEMENT_USER_TASK)
                .namespaceUri(BPMN20_NS)
                .extendsType(Task.class)
                .instanceProvider(new ModelTypeInstanceProvider<UserTask>() {
                    @Override
                    public UserTask newInstance(ModelTypeInstanceContext instanceContext) {
                        return new UserTaskImpl(instanceContext);
                    }
                });

        implementationAttribute = typeBuilder.stringAttribute(BPMN_ATTRIBUTE_IMPLEMENTATION)
                .defaultValue("##unspecified")
                .build();

        SequenceBuilder sequenceBuilder = typeBuilder.sequence();

        renderingCollection = sequenceBuilder.elementCollection(Rendering.class)
                .build();

        /* Flowable extensions */

        flowableAssigneeAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_ASSIGNEE)
                .namespace(FLOWABLE_NS)
                .build();

        flowableCandidateGroupsAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_CANDIDATE_GROUPS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableCandidateUsersAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_CANDIDATE_USERS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableDueDateAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_DUE_DATE)
                .namespace(FLOWABLE_NS)
                .build();

        flowableFormHandlerClassAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_FORM_HANDLER_CLASS)
                .namespace(FLOWABLE_NS)
                .build();

        flowableFormKeyAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_FORM_KEY)
                .namespace(FLOWABLE_NS)
                .build();

        flowablePriorityAttribute = typeBuilder.stringAttribute(FLOWABLE_ATTRIBUTE_PRIORITY)
                .namespace(FLOWABLE_NS)
                .build();

        typeBuilder.build();
    }

    public UserTaskImpl(ModelTypeInstanceContext context) {
        super(context);
    }

    @Override
    public UserTaskBuilder builder() {
        return new UserTaskBuilder((BpmnModelInstance) modelInstance, this);
    }

    @Override
    public String getImplementation() {
        return implementationAttribute.getValue(this);
    }

    @Override
    public void setImplementation(String implementation) {
        implementationAttribute.setValue(this, implementation);
    }

    @Override
    public Collection<Rendering> getRenderings() {
        return renderingCollection.get(this);
    }

    /* Flowable extensions */

    @Override
    public String getFlowableAssignee() {
        return flowableAssigneeAttribute.getValue(this);
    }

    @Override
    public void setFlowableAssignee(String flowableAssignee) {
        flowableAssigneeAttribute.setValue(this, flowableAssignee);
    }

    @Override
    public String getFlowableCandidateGroups() {
        return flowableCandidateGroupsAttribute.getValue(this);
    }

    @Override
    public void setFlowableCandidateGroups(String flowableCandidateGroups) {
        flowableCandidateGroupsAttribute.setValue(this, flowableCandidateGroups);
    }

    @Override
    public List<String> getFlowableCandidateGroupsList() {
        String candidateGroups = flowableCandidateGroupsAttribute.getValue(this);
        return StringUtil.splitCommaSeparatedList(candidateGroups);
    }

    @Override
    public void setFlowableCandidateGroupsList(List<String> flowableCandidateGroupsList) {
        String candidateGroups = StringUtil.joinCommaSeparatedList(flowableCandidateGroupsList);
        flowableCandidateGroupsAttribute.setValue(this, candidateGroups);
    }

    @Override
    public String getFlowableCandidateUsers() {
        return flowableCandidateUsersAttribute.getValue(this);
    }

    @Override
    public void setFlowableCandidateUsers(String flowableCandidateUsers) {
        flowableCandidateUsersAttribute.setValue(this, flowableCandidateUsers);
    }

    @Override
    public List<String> getFlowableCandidateUsersList() {
        String candidateUsers = flowableCandidateUsersAttribute.getValue(this);
        return StringUtil.splitCommaSeparatedList(candidateUsers);
    }

    @Override
    public void setFlowableCandidateUsersList(List<String> flowableCandidateUsersList) {
        String candidateUsers = StringUtil.joinCommaSeparatedList(flowableCandidateUsersList);
        flowableCandidateUsersAttribute.setValue(this, candidateUsers);
    }

    @Override
    public String getFlowableDueDate() {
        return flowableDueDateAttribute.getValue(this);
    }

    @Override
    public void setFlowableDueDate(String flowableDueDate) {
        flowableDueDateAttribute.setValue(this, flowableDueDate);
    }

    @Override
    public String getFlowableFormHandlerClass() {
        return flowableFormHandlerClassAttribute.getValue(this);
    }

    @Override
    public void setFlowableFormHandlerClass(String flowableFormHandlerClass) {
        flowableFormHandlerClassAttribute.setValue(this, flowableFormHandlerClass);
    }

    @Override
    public String getFlowableFormKey() {
        return flowableFormKeyAttribute.getValue(this);
    }

    @Override
    public void setFlowableFormKey(String flowableFormKey) {
        flowableFormKeyAttribute.setValue(this, flowableFormKey);
    }

    @Override
    public String getFlowablePriority() {
        return flowablePriorityAttribute.getValue(this);
    }

    @Override
    public void setFlowablePriority(String flowablePriority) {
        flowablePriorityAttribute.setValue(this, flowablePriority);
    }

}
