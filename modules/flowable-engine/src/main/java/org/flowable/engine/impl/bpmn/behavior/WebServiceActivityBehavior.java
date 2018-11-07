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
package org.flowable.engine.impl.bpmn.behavior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.DataAssociation;
import org.flowable.bpmn.model.DataSpec;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.IOSpecification;
import org.flowable.bpmn.model.Import;
import org.flowable.bpmn.model.Interface;
import org.flowable.bpmn.model.Message;
import org.flowable.bpmn.model.SendTask;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.Expression;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.delegate.BpmnError;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.impl.bpmn.data.AbstractDataAssociation;
import org.flowable.engine.impl.bpmn.data.Assignment;
import org.flowable.engine.impl.bpmn.data.ClassStructureDefinition;
import org.flowable.engine.impl.bpmn.data.ItemDefinition;
import org.flowable.engine.impl.bpmn.data.ItemInstance;
import org.flowable.engine.impl.bpmn.data.ItemKind;
import org.flowable.engine.impl.bpmn.data.SimpleDataInputAssociation;
import org.flowable.engine.impl.bpmn.data.StructureDefinition;
import org.flowable.engine.impl.bpmn.data.TransformationDataOutputAssociation;
import org.flowable.engine.impl.bpmn.helper.ErrorPropagation;
import org.flowable.engine.impl.bpmn.parser.XMLImporter;
import org.flowable.engine.impl.bpmn.webservice.BpmnInterface;
import org.flowable.engine.impl.bpmn.webservice.MessageDefinition;
import org.flowable.engine.impl.bpmn.webservice.MessageImplicitDataInputAssociation;
import org.flowable.engine.impl.bpmn.webservice.MessageImplicitDataOutputAssociation;
import org.flowable.engine.impl.bpmn.webservice.MessageInstance;
import org.flowable.engine.impl.bpmn.webservice.Operation;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.util.CommandContextUtil;
import org.flowable.engine.impl.util.ProcessDefinitionUtil;
import org.flowable.engine.impl.webservice.WSOperation;
import org.flowable.engine.impl.webservice.WSService;

/**
 * An activity behavior that allows calling Web services
 * 
 * @author Esteban Robles Luna
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class WebServiceActivityBehavior extends AbstractBpmnActivityBehavior {

    private static final long serialVersionUID = 1L;

    public static final String CURRENT_MESSAGE = "org.flowable.engine.impl.bpmn.CURRENT_MESSAGE";

    protected Map<String, XMLImporter> xmlImporterMap = new HashMap<>();
    protected Map<String, WSOperation> wsOperationMap = new HashMap<>();
    protected Map<String, StructureDefinition> structureDefinitionMap = new HashMap<>();
    protected Map<String, WSService> wsServiceMap = new HashMap<>();
    protected Map<String, Operation> operationMap = new HashMap<>();
    protected Map<String, ItemDefinition> itemDefinitionMap = new HashMap<>();
    protected Map<String, MessageDefinition> messageDefinitionMap = new HashMap<>();

    public WebServiceActivityBehavior(BpmnModel bpmnModel) {
        itemDefinitionMap.put("http://www.w3.org/2001/XMLSchema:string", new ItemDefinition("http://www.w3.org/2001/XMLSchema:string", new ClassStructureDefinition(String.class)));
        fillDefinitionMaps(bpmnModel);
    }

    @Override
    public void execute(DelegateExecution execution) {
        BpmnModel bpmnModel = ProcessDefinitionUtil.getBpmnModel(execution.getProcessDefinitionId());
        FlowElement flowElement = execution.getCurrentFlowElement();

        IOSpecification ioSpecification = null;
        String operationRef = null;
        List<DataAssociation> dataInputAssociations = null;
        List<DataAssociation> dataOutputAssociations = null;

        if (flowElement instanceof SendTask) {
            SendTask sendTask = (SendTask) flowElement;
            ioSpecification = sendTask.getIoSpecification();
            operationRef = sendTask.getOperationRef();
            dataInputAssociations = sendTask.getDataInputAssociations();
            dataOutputAssociations = sendTask.getDataOutputAssociations();

        } else if (flowElement instanceof ServiceTask) {
            ServiceTask serviceTask = (ServiceTask) flowElement;
            ioSpecification = serviceTask.getIoSpecification();
            operationRef = serviceTask.getOperationRef();
            dataInputAssociations = serviceTask.getDataInputAssociations();
            dataOutputAssociations = serviceTask.getDataOutputAssociations();

        } else {
            throw new FlowableException("Unsupported flow element type " + flowElement);
        }

        MessageInstance message = null;

        Operation operation = operationMap.get(operationRef);

        try {

            if (ioSpecification != null) {
                initializeIoSpecification(ioSpecification, execution, bpmnModel);
                if (ioSpecification.getDataInputRefs().size() > 0) {
                    String firstDataInputName = ioSpecification.getDataInputRefs().get(0);
                    ItemInstance inputItem = (ItemInstance) execution.getVariable(firstDataInputName);
                    message = new MessageInstance(operation.getInMessage(), inputItem);
                }

            } else {
                message = operation.getInMessage().createInstance();
            }

            execution.setVariable(CURRENT_MESSAGE, message);

            fillMessage(dataInputAssociations, execution);

            ProcessEngineConfigurationImpl processEngineConfig = CommandContextUtil.getProcessEngineConfiguration();
            MessageInstance receivedMessage = operation.sendMessage(message,
                    processEngineConfig.getWsOverridenEndpointAddresses());

            execution.setVariable(CURRENT_MESSAGE, receivedMessage);

            if (ioSpecification != null && ioSpecification.getDataOutputRefs().size() > 0) {
                String firstDataOutputName = ioSpecification.getDataOutputRefs().get(0);
                if (firstDataOutputName != null) {
                    ItemInstance outputItem = (ItemInstance) execution.getVariable(firstDataOutputName);
                    outputItem.getStructureInstance().loadFrom(receivedMessage.getStructureInstance().toArray());
                }
            }

            returnMessage(dataOutputAssociations, execution);

            execution.setVariable(CURRENT_MESSAGE, null);
            leave(execution);
        } catch (Exception exc) {

            Throwable cause = exc;
            BpmnError error = null;
            while (cause != null) {
                if (cause instanceof BpmnError) {
                    error = (BpmnError) cause;
                    break;
                }
                cause = cause.getCause();
            }

            if (error != null) {
                ErrorPropagation.propagateError(error, execution);
            } else if (exc instanceof RuntimeException) {
                throw (RuntimeException) exc;
            }
        }
    }

    protected void initializeIoSpecification(IOSpecification activityIoSpecification, DelegateExecution execution, BpmnModel bpmnModel) {

        for (DataSpec dataSpec : activityIoSpecification.getDataInputs()) {
            ItemDefinition itemDefinition = itemDefinitionMap.get(dataSpec.getItemSubjectRef());
            execution.setVariable(dataSpec.getId(), itemDefinition.createInstance());
        }

        for (DataSpec dataSpec : activityIoSpecification.getDataOutputs()) {
            ItemDefinition itemDefinition = itemDefinitionMap.get(dataSpec.getItemSubjectRef());
            execution.setVariable(dataSpec.getId(), itemDefinition.createInstance());
        }
    }

    protected void fillDefinitionMaps(BpmnModel bpmnModel) {

        for (Import theImport : bpmnModel.getImports()) {
            fillImporterInfo(theImport, bpmnModel.getSourceSystemId());
        }

        createItemDefinitions(bpmnModel);
        createMessages(bpmnModel);
        createOperations(bpmnModel);
    }

    protected void createItemDefinitions(BpmnModel bpmnModel) {

        for (org.flowable.bpmn.model.ItemDefinition itemDefinitionElement : bpmnModel.getItemDefinitions().values()) {

            if (!itemDefinitionMap.containsKey(itemDefinitionElement.getId())) {
                StructureDefinition structure = null;

                try {
                    // it is a class
                    Class<?> classStructure = ReflectUtil.loadClass(itemDefinitionElement.getStructureRef());
                    structure = new ClassStructureDefinition(classStructure);
                } catch (FlowableException e) {
                    // it is a reference to a different structure
                    structure = structureDefinitionMap.get(itemDefinitionElement.getStructureRef());
                }

                ItemDefinition itemDefinition = new ItemDefinition(itemDefinitionElement.getId(), structure);
                if (StringUtils.isNotEmpty(itemDefinitionElement.getItemKind())) {
                    itemDefinition.setItemKind(ItemKind.valueOf(itemDefinitionElement.getItemKind()));
                }

                itemDefinitionMap.put(itemDefinition.getId(), itemDefinition);
            }
        }
    }

    public void createMessages(BpmnModel bpmnModel) {
        for (Message messageElement : bpmnModel.getMessages()) {
            if (!messageDefinitionMap.containsKey(messageElement.getId())) {
                MessageDefinition messageDefinition = new MessageDefinition(messageElement.getId());
                if (StringUtils.isNotEmpty(messageElement.getItemRef())) {
                    if (itemDefinitionMap.containsKey(messageElement.getItemRef())) {
                        ItemDefinition itemDefinition = itemDefinitionMap.get(messageElement.getItemRef());
                        messageDefinition.setItemDefinition(itemDefinition);
                    }
                }

                messageDefinitionMap.put(messageDefinition.getId(), messageDefinition);
            }
        }
    }

    protected void createOperations(BpmnModel bpmnModel) {
        for (Interface interfaceObject : bpmnModel.getInterfaces()) {
            BpmnInterface bpmnInterface = new BpmnInterface(interfaceObject.getId(), interfaceObject.getName());
            bpmnInterface.setImplementation(wsServiceMap.get(interfaceObject.getImplementationRef()));

            for (org.flowable.bpmn.model.Operation operationObject : interfaceObject.getOperations()) {

                if (!operationMap.containsKey(operationObject.getId())) {
                    MessageDefinition inMessage = messageDefinitionMap.get(operationObject.getInMessageRef());
                    Operation operation = new Operation(operationObject.getId(), operationObject.getName(), bpmnInterface, inMessage);
                    operation.setImplementation(wsOperationMap.get(operationObject.getImplementationRef()));

                    if (StringUtils.isNotEmpty(operationObject.getOutMessageRef())) {
                        if (messageDefinitionMap.containsKey(operationObject.getOutMessageRef())) {
                            MessageDefinition outMessage = messageDefinitionMap.get(operationObject.getOutMessageRef());
                            operation.setOutMessage(outMessage);
                        }
                    }

                    operationMap.put(operation.getId(), operation);
                }
            }
        }
    }

    protected void fillImporterInfo(Import theImport, String sourceSystemId) {
        if (!xmlImporterMap.containsKey(theImport.getNamespace())) {

            if (theImport.getImportType().equals("http://schemas.xmlsoap.org/wsdl/")) {
                try {
                    ProcessEngineConfigurationImpl processEngineConfig = CommandContextUtil.getProcessEngineConfiguration();
                    XMLImporter importerInstance = processEngineConfig.getWsdlImporterFactory()
                            .createXMLImporter(theImport);

                    xmlImporterMap.put(theImport.getNamespace(), importerInstance);
                    importerInstance.importFrom(theImport, sourceSystemId);

                    structureDefinitionMap.putAll(importerInstance.getStructures());
                    wsServiceMap.putAll(importerInstance.getServices());
                    wsOperationMap.putAll(importerInstance.getOperations());

                } catch (FlowableException e) {
                    throw e;
                } catch (Exception e) {
                    throw new FlowableException(String.format("Error importing '%s' as '%s'", theImport.getLocation(),
                            theImport.getImportType()), e);
                }

            } else {
                throw new FlowableException(String.format("Unsupported import type '%s'", theImport.getImportType()));
            }
        }
    }

    protected void returnMessage(List<DataAssociation> dataOutputAssociations, DelegateExecution execution) {
        for (DataAssociation dataAssociationElement : dataOutputAssociations) {
            AbstractDataAssociation dataAssociation = createDataOutputAssociation(dataAssociationElement);
            dataAssociation.evaluate(execution);
        }
    }

    protected void fillMessage(List<DataAssociation> dataInputAssociations, DelegateExecution execution) {
        for (DataAssociation dataAssociationElement : dataInputAssociations) {
            AbstractDataAssociation dataAssociation = createDataInputAssociation(dataAssociationElement);
            dataAssociation.evaluate(execution);
        }
    }

    protected AbstractDataAssociation createDataInputAssociation(DataAssociation dataAssociationElement) {
        if (dataAssociationElement.getAssignments().isEmpty()) {
            return new MessageImplicitDataInputAssociation(dataAssociationElement.getSourceRef(), dataAssociationElement.getTargetRef());
        } else {
            SimpleDataInputAssociation dataAssociation = new SimpleDataInputAssociation(dataAssociationElement.getSourceRef(), dataAssociationElement.getTargetRef());
            ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();

            for (org.flowable.bpmn.model.Assignment assignmentElement : dataAssociationElement.getAssignments()) {
                if (StringUtils.isNotEmpty(assignmentElement.getFrom()) && StringUtils.isNotEmpty(assignmentElement.getTo())) {
                    Expression from = expressionManager.createExpression(assignmentElement.getFrom());
                    Expression to = expressionManager.createExpression(assignmentElement.getTo());
                    Assignment assignment = new Assignment(from, to);
                    dataAssociation.addAssignment(assignment);
                }
            }
            return dataAssociation;
        }
    }

    protected AbstractDataAssociation createDataOutputAssociation(DataAssociation dataAssociationElement) {
        if (StringUtils.isNotEmpty(dataAssociationElement.getSourceRef())) {
            return new MessageImplicitDataOutputAssociation(dataAssociationElement.getTargetRef(), dataAssociationElement.getSourceRef());
        } else {
            ExpressionManager expressionManager = CommandContextUtil.getProcessEngineConfiguration().getExpressionManager();
            Expression transformation = expressionManager.createExpression(dataAssociationElement.getTransformation());
            AbstractDataAssociation dataOutputAssociation = new TransformationDataOutputAssociation(null, dataAssociationElement.getTargetRef(), transformation);
            return dataOutputAssociation;
        }
    }
}
