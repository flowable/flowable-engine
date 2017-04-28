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
package org.flowable.bpm.model.bpmn;

import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.ACTIVITI_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.BPMN20_NS;
import static org.flowable.bpm.model.bpmn.impl.BpmnModelConstants.FLOWABLE_NS;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

import org.flowable.bpm.model.bpmn.builder.ProcessBuilder;
import org.flowable.bpm.model.bpmn.impl.BpmnParser;
import org.flowable.bpm.model.bpmn.impl.instance.ActivationConditionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ActivityImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ArtifactImpl;
import org.flowable.bpm.model.bpmn.impl.instance.AssignmentImpl;
import org.flowable.bpm.model.bpmn.impl.instance.AssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.AuditingImpl;
import org.flowable.bpm.model.bpmn.impl.instance.BaseElementImpl;
import org.flowable.bpm.model.bpmn.impl.instance.BoundaryEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.BusinessRuleTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CallActivityImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CallConversationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CallableElementImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CancelEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CatchEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CategoryValueImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CategoryValueRef;
import org.flowable.bpm.model.bpmn.impl.instance.ChildLaneSet;
import org.flowable.bpm.model.bpmn.impl.instance.CollaborationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CompensateEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CompletionConditionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ComplexBehaviorDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ComplexGatewayImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConditionExpressionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConditionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConditionalEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConversationAssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConversationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConversationLinkImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ConversationNodeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CorrelationKeyImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CorrelationPropertyBindingImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CorrelationPropertyImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CorrelationPropertyRef;
import org.flowable.bpm.model.bpmn.impl.instance.CorrelationPropertyRetrievalExpressionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.CorrelationSubscriptionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataAssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataInputAssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataInputImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataInputRefs;
import org.flowable.bpm.model.bpmn.impl.instance.DataObjectImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataObjectReferenceImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataOutputAssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataOutputImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataOutputRefs;
import org.flowable.bpm.model.bpmn.impl.instance.DataPath;
import org.flowable.bpm.model.bpmn.impl.instance.DataStateImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DataStoreImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DefinitionsImpl;
import org.flowable.bpm.model.bpmn.impl.instance.DocumentationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EndEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EndPointImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EndPointRef;
import org.flowable.bpm.model.bpmn.impl.instance.ErrorEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ErrorImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ErrorRef;
import org.flowable.bpm.model.bpmn.impl.instance.EscalationEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EscalationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EventBasedGatewayImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.EventDefinitionRef;
import org.flowable.bpm.model.bpmn.impl.instance.EventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ExclusiveGatewayImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ExpressionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ExtensionElementsImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ExtensionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.FlowElementImpl;
import org.flowable.bpm.model.bpmn.impl.instance.FlowNodeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.FlowNodeRef;
import org.flowable.bpm.model.bpmn.impl.instance.FormalExpressionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.From;
import org.flowable.bpm.model.bpmn.impl.instance.GatewayImpl;
import org.flowable.bpm.model.bpmn.impl.instance.GlobalConversationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.HumanPerformerImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ImportImpl;
import org.flowable.bpm.model.bpmn.impl.instance.InMessageRef;
import org.flowable.bpm.model.bpmn.impl.instance.InclusiveGatewayImpl;
import org.flowable.bpm.model.bpmn.impl.instance.Incoming;
import org.flowable.bpm.model.bpmn.impl.instance.InnerParticipantRef;
import org.flowable.bpm.model.bpmn.impl.instance.InputDataItemImpl;
import org.flowable.bpm.model.bpmn.impl.instance.InputSetImpl;
import org.flowable.bpm.model.bpmn.impl.instance.InputSetRefs;
import org.flowable.bpm.model.bpmn.impl.instance.InteractionNodeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.InterfaceImpl;
import org.flowable.bpm.model.bpmn.impl.instance.InterfaceRef;
import org.flowable.bpm.model.bpmn.impl.instance.IntermediateCatchEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.IntermediateThrowEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.IoBindingImpl;
import org.flowable.bpm.model.bpmn.impl.instance.IoSpecificationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ItemAwareElementImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ItemDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.LaneImpl;
import org.flowable.bpm.model.bpmn.impl.instance.LaneSetImpl;
import org.flowable.bpm.model.bpmn.impl.instance.LinkEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.LoopCardinalityImpl;
import org.flowable.bpm.model.bpmn.impl.instance.LoopCharacteristicsImpl;
import org.flowable.bpm.model.bpmn.impl.instance.LoopDataInputRef;
import org.flowable.bpm.model.bpmn.impl.instance.LoopDataOutputRef;
import org.flowable.bpm.model.bpmn.impl.instance.ManualTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.MessageEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.MessageFlowAssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.MessageFlowImpl;
import org.flowable.bpm.model.bpmn.impl.instance.MessageFlowRef;
import org.flowable.bpm.model.bpmn.impl.instance.MessageImpl;
import org.flowable.bpm.model.bpmn.impl.instance.MessagePath;
import org.flowable.bpm.model.bpmn.impl.instance.MonitoringImpl;
import org.flowable.bpm.model.bpmn.impl.instance.MultiInstanceLoopCharacteristicsImpl;
import org.flowable.bpm.model.bpmn.impl.instance.OperationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.OperationRef;
import org.flowable.bpm.model.bpmn.impl.instance.OptionalInputRefs;
import org.flowable.bpm.model.bpmn.impl.instance.OptionalOutputRefs;
import org.flowable.bpm.model.bpmn.impl.instance.OutMessageRef;
import org.flowable.bpm.model.bpmn.impl.instance.OuterParticipantRef;
import org.flowable.bpm.model.bpmn.impl.instance.Outgoing;
import org.flowable.bpm.model.bpmn.impl.instance.OutputDataItemImpl;
import org.flowable.bpm.model.bpmn.impl.instance.OutputSetImpl;
import org.flowable.bpm.model.bpmn.impl.instance.OutputSetRefs;
import org.flowable.bpm.model.bpmn.impl.instance.ParallelGatewayImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ParticipantAssociationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ParticipantImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ParticipantMultiplicityImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ParticipantRef;
import org.flowable.bpm.model.bpmn.impl.instance.PartitionElement;
import org.flowable.bpm.model.bpmn.impl.instance.PerformerImpl;
import org.flowable.bpm.model.bpmn.impl.instance.PotentialOwnerImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ProcessImpl;
import org.flowable.bpm.model.bpmn.impl.instance.PropertyImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ReceiveTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.RelationshipImpl;
import org.flowable.bpm.model.bpmn.impl.instance.RenderingImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ResourceAssignmentExpressionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ResourceImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ResourceParameterBindingImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ResourceParameterImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ResourceRef;
import org.flowable.bpm.model.bpmn.impl.instance.ResourceRoleImpl;
import org.flowable.bpm.model.bpmn.impl.instance.RootElementImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ScriptImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ScriptTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SendTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SequenceFlowImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ServiceTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SignalEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SignalImpl;
import org.flowable.bpm.model.bpmn.impl.instance.Source;
import org.flowable.bpm.model.bpmn.impl.instance.SourceRef;
import org.flowable.bpm.model.bpmn.impl.instance.StartEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SubConversationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SubProcessImpl;
import org.flowable.bpm.model.bpmn.impl.instance.SupportedInterfaceRef;
import org.flowable.bpm.model.bpmn.impl.instance.Supports;
import org.flowable.bpm.model.bpmn.impl.instance.Target;
import org.flowable.bpm.model.bpmn.impl.instance.TargetRef;
import org.flowable.bpm.model.bpmn.impl.instance.TaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TerminateEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TextAnnotationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TextImpl;
import org.flowable.bpm.model.bpmn.impl.instance.ThrowEventImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TimeCycleImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TimeDateImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TimeDurationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.TimerEventDefinitionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.To;
import org.flowable.bpm.model.bpmn.impl.instance.TransactionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.Transformation;
import org.flowable.bpm.model.bpmn.impl.instance.UserTaskImpl;
import org.flowable.bpm.model.bpmn.impl.instance.WhileExecutingInputRefs;
import org.flowable.bpm.model.bpmn.impl.instance.WhileExecutingOutputRefs;
import org.flowable.bpm.model.bpmn.impl.instance.bpmndi.BpmnDiagramImpl;
import org.flowable.bpm.model.bpmn.impl.instance.bpmndi.BpmnEdgeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.bpmndi.BpmnLabelImpl;
import org.flowable.bpm.model.bpmn.impl.instance.bpmndi.BpmnLabelStyleImpl;
import org.flowable.bpm.model.bpmn.impl.instance.bpmndi.BpmnPlaneImpl;
import org.flowable.bpm.model.bpmn.impl.instance.bpmndi.BpmnShapeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.dc.BoundsImpl;
import org.flowable.bpm.model.bpmn.impl.instance.dc.FontImpl;
import org.flowable.bpm.model.bpmn.impl.instance.dc.PointImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.DiagramElementImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.DiagramImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.EdgeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.LabelImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.LabeledEdgeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.LabeledShapeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.NodeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.PlaneImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.ShapeImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.StyleImpl;
import org.flowable.bpm.model.bpmn.impl.instance.di.WaypointImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableConnectorIdImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableConnectorImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableConstraintImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableEntryImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableExecutionListenerImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableExpressionImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableFailedJobRetryTimeCycleImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableFieldImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableFormDataImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableFormFieldImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableFormPropertyImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableInImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableInputOutputImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableInputParameterImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableListImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableMapImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableOutImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableOutputParameterImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowablePotentialStarterImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowablePropertiesImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowablePropertyImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableScriptImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableStringImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableTaskListenerImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableValidationImpl;
import org.flowable.bpm.model.bpmn.impl.instance.flowable.FlowableValueImpl;
import org.flowable.bpm.model.bpmn.instance.Definitions;
import org.flowable.bpm.model.bpmn.instance.Process;
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnDiagram;
import org.flowable.bpm.model.bpmn.instance.bpmndi.BpmnPlane;
import org.flowable.bpm.model.xml.Model;
import org.flowable.bpm.model.xml.ModelBuilder;
import org.flowable.bpm.model.xml.ModelException;
import org.flowable.bpm.model.xml.ModelParseException;
import org.flowable.bpm.model.xml.ModelValidationException;
import org.flowable.bpm.model.xml.impl.instance.ModelElementInstanceImpl;
import org.flowable.bpm.model.xml.impl.util.IoUtil;

/**
 * Provides access to the Flowable BPMN model api.
 */
public class BpmnModelBuilder {

    /**
     * The singleton instance of {@link BpmnModelBuilder}. If you want to customize the behavior of BpmnModelBuilder, replace this instance with an
     * instance of a custom subclass of {@link BpmnModelBuilder}.
     */
    public static BpmnModelBuilder INSTANCE = new BpmnModelBuilder();

    /**
     * the parser used by the BpmnModelBuilder implementation.
     */
    private BpmnParser bpmnParser = new BpmnParser();
    private final ModelBuilder bpmnModelBuilder;

    /**
     * The {@link Model}
     */
    private Model bpmnModel;

    /**
     * Allows reading a {@link BpmnModelInstance} from a File.
     *
     * @param file the {@link File} to read the {@link BpmnModelInstance} from
     * @return the model read
     * @throws BpmnModelException if the model cannot be read
     */
    public static BpmnModelInstance readModelFromFile(File file) {
        return INSTANCE.doReadModelFromFile(file);
    }

    /**
     * Allows reading a {@link BpmnModelInstance} from an {@link InputStream}
     *
     * @param stream the {@link InputStream} to read the {@link BpmnModelInstance} from
     * @return the model read
     * @throws ModelParseException if the model cannot be read
     */
    public static BpmnModelInstance readModelFromStream(InputStream stream) {
        return INSTANCE.doReadModelFromInputStream(stream);
    }

    /**
     * Allows writing a {@link BpmnModelInstance} to a File. It will be validated before writing.
     *
     * @param file the {@link File} to write the {@link BpmnModelInstance} to
     * @param modelInstance the {@link BpmnModelInstance} to write
     * @throws BpmnModelException if the model cannot be written
     * @throws ModelValidationException if the model is not valid
     */
    public static void writeModelToFile(File file, BpmnModelInstance modelInstance) {
        INSTANCE.doWriteModelToFile(file, modelInstance);
    }

    /**
     * Allows writing a {@link BpmnModelInstance} to an {@link OutputStream}. It will be validated before writing.
     *
     * @param stream the {@link OutputStream} to write the {@link BpmnModelInstance} to
     * @param modelInstance the {@link BpmnModelInstance} to write
     * @throws ModelException if the model cannot be written
     * @throws ModelValidationException if the model is not valid
     */
    public static void writeModelToStream(OutputStream stream, BpmnModelInstance modelInstance) {
        INSTANCE.doWriteModelToOutputStream(stream, modelInstance);
    }

    /**
     * Allows the conversion of a {@link BpmnModelInstance} to an {@link String}. It will be validated before conversion.
     *
     * @param modelInstance the model instance to convert
     * @return the XML string representation of the model instance
     */
    public static String convertToString(BpmnModelInstance modelInstance) {
        return INSTANCE.doConvertToString(modelInstance);
    }

    /**
     * Validate model DOM document
     *
     * @param modelInstance the {@link BpmnModelInstance} to validate
     * @throws ModelValidationException if the model is not valid
     */
    public static void validateModel(BpmnModelInstance modelInstance) {
        INSTANCE.doValidateModel(modelInstance);
    }

    /**
     * Allows creating an new, empty {@link BpmnModelInstance}.
     *
     * @return the empty model.
     */
    public static BpmnModelInstance createEmptyModel() {
        return INSTANCE.doCreateEmptyModel();
    }

    public static ProcessBuilder createProcess() {
        BpmnModelInstance modelInstance = INSTANCE.doCreateEmptyModel();
        Definitions definitions = modelInstance.newInstance(Definitions.class);
        definitions.setTargetNamespace(BPMN20_NS);
        definitions.getDomElement().registerNamespace("flowable", FLOWABLE_NS);
        modelInstance.setDefinitions(definitions);
        Process process = modelInstance.newInstance(Process.class);
        definitions.addChildElement(process);

        BpmnDiagram bpmnDiagram = modelInstance.newInstance(BpmnDiagram.class);

        BpmnPlane bpmnPlane = modelInstance.newInstance(BpmnPlane.class);
        bpmnPlane.setBpmnElement(process);

        bpmnDiagram.addChildElement(bpmnPlane);
        definitions.addChildElement(bpmnDiagram);

        return process.builder();
    }

    public static ProcessBuilder createProcess(String processId) {
        return createProcess().id(processId);
    }

    public static ProcessBuilder createExecutableProcess() {
        return createProcess().executable();
    }

    public static ProcessBuilder createExecutableProcess(String processId) {
        return createProcess(processId).executable();
    }

    /**
     * Register known types of the BPMN model
     */
    protected BpmnModelBuilder() {
        bpmnModelBuilder = ModelBuilder.createInstance("BPMN Model");
        bpmnModelBuilder.alternativeNamespace(ACTIVITI_NS, FLOWABLE_NS);
        doRegisterTypes(bpmnModelBuilder);
        bpmnModel = bpmnModelBuilder.build();
    }

    protected BpmnModelInstance doReadModelFromFile(File file) {
        InputStream is = null;
        try {
            is = new FileInputStream(file);
            return doReadModelFromInputStream(is);

        }
        catch (FileNotFoundException e) {
            throw new BpmnModelException("Cannot read model from file " + file + ": file does not exist.");

        }
        finally {
            IoUtil.closeSilently(is);

        }
    }

    protected BpmnModelInstance doReadModelFromInputStream(InputStream is) {
        return bpmnParser.parseModelFromStream(is);
    }

    protected void doWriteModelToFile(File file, BpmnModelInstance modelInstance) {
        OutputStream os = null;
        try {
            os = new FileOutputStream(file);
            doWriteModelToOutputStream(os, modelInstance);
        }
        catch (FileNotFoundException e) {
            throw new BpmnModelException("Cannot write model to file " + file + ": file does not exist.");
        }
        finally {
            IoUtil.closeSilently(os);
        }
    }

    protected void doWriteModelToOutputStream(OutputStream os, BpmnModelInstance modelInstance) {
        // validate DOM document
        doValidateModel(modelInstance);
        // write XML
        IoUtil.writeDocumentToOutputStream(modelInstance.getDocument(), os);
    }

    protected String doConvertToString(BpmnModelInstance modelInstance) {
        // validate DOM document
        doValidateModel(modelInstance);
        // convert to XML string
        return IoUtil.convertXmlDocumentToString(modelInstance.getDocument());
    }

    protected void doValidateModel(BpmnModelInstance modelInstance) {
        bpmnParser.validateModel(modelInstance.getDocument());
    }

    protected BpmnModelInstance doCreateEmptyModel() {
        return bpmnParser.getEmptyModel();
    }

    protected void doRegisterTypes(ModelBuilder bpmnModelBuilder) {
        ActivationConditionImpl.registerType(bpmnModelBuilder);
        ActivityImpl.registerType(bpmnModelBuilder);
        ArtifactImpl.registerType(bpmnModelBuilder);
        AssignmentImpl.registerType(bpmnModelBuilder);
        AssociationImpl.registerType(bpmnModelBuilder);
        AuditingImpl.registerType(bpmnModelBuilder);
        BaseElementImpl.registerType(bpmnModelBuilder);
        BoundaryEventImpl.registerType(bpmnModelBuilder);
        BusinessRuleTaskImpl.registerType(bpmnModelBuilder);
        CallableElementImpl.registerType(bpmnModelBuilder);
        CallActivityImpl.registerType(bpmnModelBuilder);
        CallConversationImpl.registerType(bpmnModelBuilder);
        CancelEventDefinitionImpl.registerType(bpmnModelBuilder);
        CatchEventImpl.registerType(bpmnModelBuilder);
        CategoryValueImpl.registerType(bpmnModelBuilder);
        CategoryValueRef.registerType(bpmnModelBuilder);
        ChildLaneSet.registerType(bpmnModelBuilder);
        CollaborationImpl.registerType(bpmnModelBuilder);
        CompensateEventDefinitionImpl.registerType(bpmnModelBuilder);
        ConditionImpl.registerType(bpmnModelBuilder);
        ConditionalEventDefinitionImpl.registerType(bpmnModelBuilder);
        CompletionConditionImpl.registerType(bpmnModelBuilder);
        ComplexBehaviorDefinitionImpl.registerType(bpmnModelBuilder);
        ComplexGatewayImpl.registerType(bpmnModelBuilder);
        ConditionExpressionImpl.registerType(bpmnModelBuilder);
        ConversationAssociationImpl.registerType(bpmnModelBuilder);
        ConversationImpl.registerType(bpmnModelBuilder);
        ConversationLinkImpl.registerType(bpmnModelBuilder);
        ConversationNodeImpl.registerType(bpmnModelBuilder);
        CorrelationKeyImpl.registerType(bpmnModelBuilder);
        CorrelationPropertyBindingImpl.registerType(bpmnModelBuilder);
        CorrelationPropertyImpl.registerType(bpmnModelBuilder);
        CorrelationPropertyRef.registerType(bpmnModelBuilder);
        CorrelationPropertyRetrievalExpressionImpl.registerType(bpmnModelBuilder);
        CorrelationSubscriptionImpl.registerType(bpmnModelBuilder);
        DataAssociationImpl.registerType(bpmnModelBuilder);
        DataInputAssociationImpl.registerType(bpmnModelBuilder);
        DataInputImpl.registerType(bpmnModelBuilder);
        DataInputRefs.registerType(bpmnModelBuilder);
        DataOutputAssociationImpl.registerType(bpmnModelBuilder);
        DataOutputImpl.registerType(bpmnModelBuilder);
        DataOutputRefs.registerType(bpmnModelBuilder);
        DataPath.registerType(bpmnModelBuilder);
        DataStateImpl.registerType(bpmnModelBuilder);
        DataObjectImpl.registerType(bpmnModelBuilder);
        DataObjectReferenceImpl.registerType(bpmnModelBuilder);
        DataStoreImpl.registerType(bpmnModelBuilder);
        DefinitionsImpl.registerType(bpmnModelBuilder);
        DocumentationImpl.registerType(bpmnModelBuilder);
        EndEventImpl.registerType(bpmnModelBuilder);
        EndPointImpl.registerType(bpmnModelBuilder);
        EndPointRef.registerType(bpmnModelBuilder);
        ErrorEventDefinitionImpl.registerType(bpmnModelBuilder);
        ErrorImpl.registerType(bpmnModelBuilder);
        ErrorRef.registerType(bpmnModelBuilder);
        EscalationImpl.registerType(bpmnModelBuilder);
        EscalationEventDefinitionImpl.registerType(bpmnModelBuilder);
        EventBasedGatewayImpl.registerType(bpmnModelBuilder);
        EventDefinitionImpl.registerType(bpmnModelBuilder);
        EventDefinitionRef.registerType(bpmnModelBuilder);
        EventImpl.registerType(bpmnModelBuilder);
        ExclusiveGatewayImpl.registerType(bpmnModelBuilder);
        ExpressionImpl.registerType(bpmnModelBuilder);
        ExtensionElementsImpl.registerType(bpmnModelBuilder);
        ExtensionImpl.registerType(bpmnModelBuilder);
        FlowElementImpl.registerType(bpmnModelBuilder);
        FlowNodeImpl.registerType(bpmnModelBuilder);
        FlowNodeRef.registerType(bpmnModelBuilder);
        FormalExpressionImpl.registerType(bpmnModelBuilder);
        From.registerType(bpmnModelBuilder);
        GatewayImpl.registerType(bpmnModelBuilder);
        GlobalConversationImpl.registerType(bpmnModelBuilder);
        HumanPerformerImpl.registerType(bpmnModelBuilder);
        ImportImpl.registerType(bpmnModelBuilder);
        InclusiveGatewayImpl.registerType(bpmnModelBuilder);
        Incoming.registerType(bpmnModelBuilder);
        InMessageRef.registerType(bpmnModelBuilder);
        InnerParticipantRef.registerType(bpmnModelBuilder);
        InputDataItemImpl.registerType(bpmnModelBuilder);
        InputSetImpl.registerType(bpmnModelBuilder);
        InputSetRefs.registerType(bpmnModelBuilder);
        InteractionNodeImpl.registerType(bpmnModelBuilder);
        InterfaceImpl.registerType(bpmnModelBuilder);
        InterfaceRef.registerType(bpmnModelBuilder);
        IntermediateCatchEventImpl.registerType(bpmnModelBuilder);
        IntermediateThrowEventImpl.registerType(bpmnModelBuilder);
        IoBindingImpl.registerType(bpmnModelBuilder);
        IoSpecificationImpl.registerType(bpmnModelBuilder);
        ItemAwareElementImpl.registerType(bpmnModelBuilder);
        ItemDefinitionImpl.registerType(bpmnModelBuilder);
        LaneImpl.registerType(bpmnModelBuilder);
        LaneSetImpl.registerType(bpmnModelBuilder);
        LinkEventDefinitionImpl.registerType(bpmnModelBuilder);
        LoopCardinalityImpl.registerType(bpmnModelBuilder);
        LoopCharacteristicsImpl.registerType(bpmnModelBuilder);
        LoopDataInputRef.registerType(bpmnModelBuilder);
        LoopDataOutputRef.registerType(bpmnModelBuilder);
        ManualTaskImpl.registerType(bpmnModelBuilder);
        MessageEventDefinitionImpl.registerType(bpmnModelBuilder);
        MessageFlowAssociationImpl.registerType(bpmnModelBuilder);
        MessageFlowImpl.registerType(bpmnModelBuilder);
        MessageFlowRef.registerType(bpmnModelBuilder);
        MessageImpl.registerType(bpmnModelBuilder);
        MessagePath.registerType(bpmnModelBuilder);
        ModelElementInstanceImpl.registerType(bpmnModelBuilder);
        MonitoringImpl.registerType(bpmnModelBuilder);
        MultiInstanceLoopCharacteristicsImpl.registerType(bpmnModelBuilder);
        OperationImpl.registerType(bpmnModelBuilder);
        OperationRef.registerType(bpmnModelBuilder);
        OptionalInputRefs.registerType(bpmnModelBuilder);
        OptionalOutputRefs.registerType(bpmnModelBuilder);
        OuterParticipantRef.registerType(bpmnModelBuilder);
        OutMessageRef.registerType(bpmnModelBuilder);
        Outgoing.registerType(bpmnModelBuilder);
        OutputDataItemImpl.registerType(bpmnModelBuilder);
        OutputSetImpl.registerType(bpmnModelBuilder);
        OutputSetRefs.registerType(bpmnModelBuilder);
        ParallelGatewayImpl.registerType(bpmnModelBuilder);
        ParticipantAssociationImpl.registerType(bpmnModelBuilder);
        ParticipantImpl.registerType(bpmnModelBuilder);
        ParticipantMultiplicityImpl.registerType(bpmnModelBuilder);
        ParticipantRef.registerType(bpmnModelBuilder);
        PartitionElement.registerType(bpmnModelBuilder);
        PerformerImpl.registerType(bpmnModelBuilder);
        PotentialOwnerImpl.registerType(bpmnModelBuilder);
        ProcessImpl.registerType(bpmnModelBuilder);
        PropertyImpl.registerType(bpmnModelBuilder);
        ReceiveTaskImpl.registerType(bpmnModelBuilder);
        RelationshipImpl.registerType(bpmnModelBuilder);
        RenderingImpl.registerType(bpmnModelBuilder);
        ResourceAssignmentExpressionImpl.registerType(bpmnModelBuilder);
        ResourceImpl.registerType(bpmnModelBuilder);
        ResourceParameterBindingImpl.registerType(bpmnModelBuilder);
        ResourceParameterImpl.registerType(bpmnModelBuilder);
        ResourceRef.registerType(bpmnModelBuilder);
        ResourceRoleImpl.registerType(bpmnModelBuilder);
        RootElementImpl.registerType(bpmnModelBuilder);
        ScriptImpl.registerType(bpmnModelBuilder);
        ScriptTaskImpl.registerType(bpmnModelBuilder);
        SendTaskImpl.registerType(bpmnModelBuilder);
        SequenceFlowImpl.registerType(bpmnModelBuilder);
        ServiceTaskImpl.registerType(bpmnModelBuilder);
        SignalEventDefinitionImpl.registerType(bpmnModelBuilder);
        SignalImpl.registerType(bpmnModelBuilder);
        Source.registerType(bpmnModelBuilder);
        SourceRef.registerType(bpmnModelBuilder);
        StartEventImpl.registerType(bpmnModelBuilder);
        SubConversationImpl.registerType(bpmnModelBuilder);
        SubProcessImpl.registerType(bpmnModelBuilder);
        SupportedInterfaceRef.registerType(bpmnModelBuilder);
        Supports.registerType(bpmnModelBuilder);
        Target.registerType(bpmnModelBuilder);
        TargetRef.registerType(bpmnModelBuilder);
        TaskImpl.registerType(bpmnModelBuilder);
        TerminateEventDefinitionImpl.registerType(bpmnModelBuilder);
        TextImpl.registerType(bpmnModelBuilder);
        TextAnnotationImpl.registerType(bpmnModelBuilder);
        ThrowEventImpl.registerType(bpmnModelBuilder);
        TimeCycleImpl.registerType(bpmnModelBuilder);
        TimeDateImpl.registerType(bpmnModelBuilder);
        TimeDurationImpl.registerType(bpmnModelBuilder);
        TimerEventDefinitionImpl.registerType(bpmnModelBuilder);
        To.registerType(bpmnModelBuilder);
        TransactionImpl.registerType(bpmnModelBuilder);
        Transformation.registerType(bpmnModelBuilder);
        UserTaskImpl.registerType(bpmnModelBuilder);
        WhileExecutingInputRefs.registerType(bpmnModelBuilder);
        WhileExecutingOutputRefs.registerType(bpmnModelBuilder);

        /* DC */
        FontImpl.registerType(bpmnModelBuilder);
        PointImpl.registerType(bpmnModelBuilder);
        BoundsImpl.registerType(bpmnModelBuilder);

        /* DI */
        DiagramImpl.registerType(bpmnModelBuilder);
        DiagramElementImpl.registerType(bpmnModelBuilder);
        EdgeImpl.registerType(bpmnModelBuilder);
        org.flowable.bpm.model.bpmn.impl.instance.di.ExtensionImpl.registerType(bpmnModelBuilder);
        LabelImpl.registerType(bpmnModelBuilder);
        LabeledEdgeImpl.registerType(bpmnModelBuilder);
        LabeledShapeImpl.registerType(bpmnModelBuilder);
        NodeImpl.registerType(bpmnModelBuilder);
        PlaneImpl.registerType(bpmnModelBuilder);
        ShapeImpl.registerType(bpmnModelBuilder);
        StyleImpl.registerType(bpmnModelBuilder);
        WaypointImpl.registerType(bpmnModelBuilder);

        /* BPMNDI */
        BpmnDiagramImpl.registerType(bpmnModelBuilder);
        BpmnEdgeImpl.registerType(bpmnModelBuilder);
        BpmnLabelImpl.registerType(bpmnModelBuilder);
        BpmnLabelStyleImpl.registerType(bpmnModelBuilder);
        BpmnPlaneImpl.registerType(bpmnModelBuilder);
        BpmnShapeImpl.registerType(bpmnModelBuilder);

        /* Flowable extensions */
        FlowableConnectorImpl.registerType(bpmnModelBuilder);
        FlowableConnectorIdImpl.registerType(bpmnModelBuilder);
        FlowableConstraintImpl.registerType(bpmnModelBuilder);
        FlowableEntryImpl.registerType(bpmnModelBuilder);
        FlowableExecutionListenerImpl.registerType(bpmnModelBuilder);
        FlowableExpressionImpl.registerType(bpmnModelBuilder);
        FlowableFailedJobRetryTimeCycleImpl.registerType(bpmnModelBuilder);
        FlowableFieldImpl.registerType(bpmnModelBuilder);
        FlowableFormDataImpl.registerType(bpmnModelBuilder);
        FlowableFormFieldImpl.registerType(bpmnModelBuilder);
        FlowableFormPropertyImpl.registerType(bpmnModelBuilder);
        FlowableInImpl.registerType(bpmnModelBuilder);
        FlowableInputOutputImpl.registerType(bpmnModelBuilder);
        FlowableInputParameterImpl.registerType(bpmnModelBuilder);
        FlowableListImpl.registerType(bpmnModelBuilder);
        FlowableMapImpl.registerType(bpmnModelBuilder);
        FlowableOutputParameterImpl.registerType(bpmnModelBuilder);
        FlowableOutImpl.registerType(bpmnModelBuilder);
        FlowablePotentialStarterImpl.registerType(bpmnModelBuilder);
        FlowablePropertiesImpl.registerType(bpmnModelBuilder);
        FlowablePropertyImpl.registerType(bpmnModelBuilder);
        FlowableScriptImpl.registerType(bpmnModelBuilder);
        FlowableStringImpl.registerType(bpmnModelBuilder);
        FlowableTaskListenerImpl.registerType(bpmnModelBuilder);
        FlowableValidationImpl.registerType(bpmnModelBuilder);
        FlowableValueImpl.registerType(bpmnModelBuilder);
    }

    /**
     * @return the {@link Model} instance to use
     */
    public Model getBpmnModel() {
        return bpmnModel;
    }

    public ModelBuilder getBpmnModelBuilder() {
        return bpmnModelBuilder;
    }

    /**
     * @param bpmnModel the bpmnModel to set
     */
    public void setBpmnModel(Model bpmnModel) {
        this.bpmnModel = bpmnModel;
    }

}
