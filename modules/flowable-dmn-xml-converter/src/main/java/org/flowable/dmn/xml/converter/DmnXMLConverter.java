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
package org.flowable.dmn.xml.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.stax.StAXSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.io.InputStreamProvider;
import org.flowable.dmn.converter.util.DmnXMLUtil;
import org.flowable.dmn.model.AuthorityRequirement;
import org.flowable.dmn.model.BuiltinAggregator;
import org.flowable.dmn.model.Decision;
import org.flowable.dmn.model.DecisionRule;
import org.flowable.dmn.model.DecisionService;
import org.flowable.dmn.model.DecisionTable;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.model.DmnElement;
import org.flowable.dmn.model.DmnElementReference;
import org.flowable.dmn.model.DmnExtensionElement;
import org.flowable.dmn.model.HitPolicy;
import org.flowable.dmn.model.InformationItem;
import org.flowable.dmn.model.InformationRequirement;
import org.flowable.dmn.model.InputClause;
import org.flowable.dmn.model.InputData;
import org.flowable.dmn.model.ItemDefinition;
import org.flowable.dmn.model.OutputClause;
import org.flowable.dmn.model.RuleInputClauseContainer;
import org.flowable.dmn.model.RuleOutputClauseContainer;
import org.flowable.dmn.xml.constants.DmnXMLConstants;
import org.flowable.dmn.xml.exception.DmnXMLException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

/**
 * @author Tijs Rademakers
 * @author Yvo Swillens
 * @author Bassam Al-Sarori
 * @author Zheng Ji
 */
public class DmnXMLConverter implements DmnXMLConstants {

    protected static final Logger LOGGER = LoggerFactory.getLogger(DmnXMLConverter.class);

    protected static final String DMN_XSD = "org/flowable/impl/dmn/parser/DMN13.xsd";
    protected static final String DMN_11_XSD = "org/flowable/impl/dmn/parser/dmn.xsd";
    protected static final String DMN_12_XSD = "org/flowable/impl/dmn/parser/DMN12.xsd";
    protected static final String DMN_12_TARGET_NAMESPACE = "http://www.omg.org/spec/DMN/20180521/MODEL/";
    protected static final String DMN_13_TARGET_NAMESPACE = "https://www.omg.org/spec/DMN/20191111/MODEL/";
    protected static final String DEFAULT_ENCODING = "UTF-8";

    protected static Map<String, BaseDmnXMLConverter> convertersToDmnMap = new HashMap<>();

    protected ClassLoader classloader;

    static {
        addConverter(new InputClauseXMLConverter());
        addConverter(new OutputClauseXMLConverter());
        addConverter(new DecisionRuleXMLConverter());
        addConverter(new InformationRequirementConverter());
        addConverter(new AuthorityRequirementConverter());
        addConverter(new ItemDefinitionXMLConverter());
        addConverter(new InputDataXMLConverter());
        addConverter(new VariableXMLConverter());
        addConverter(new DecisionServiceXMLConverter());
        addConverter(new DmnDiDiagramXmlConverter());
        addConverter(new DmnDiShapeXmlConverter());
        addConverter(new DmnDiBoundsXmlConverter());
        addConverter(new DmnDiEdgeXmlConverter());
        addConverter(new DmnDiWaypointXmlConverter());
        addConverter(new DmnDiSizeXmlConverter());
        addConverter(new DmnDiDecisionServiceDividerLineXmlConverter());
    }

    public static void addConverter(BaseDmnXMLConverter converter) {
        convertersToDmnMap.put(converter.getXMLElementName(), converter);
    }

    public void setClassloader(ClassLoader classloader) {
        this.classloader = classloader;
    }

    public void validateModel(InputStreamProvider inputStreamProvider) throws Exception {
        Schema schema;
        String targetNameSpace = getTargetNameSpace(inputStreamProvider.getInputStream());
        if (DMN_13_TARGET_NAMESPACE.equals(targetNameSpace)) {
            schema = createSchema(DMN_XSD);
        } else if (DMN_12_TARGET_NAMESPACE.equals(targetNameSpace)) {
            schema = createSchema(DMN_12_XSD);
        } else {
            schema = createSchema(DMN_11_XSD);
        }

        Validator validator = schema.newValidator();
        validator.validate(new StreamSource(inputStreamProvider.getInputStream()));
    }

    public void validateModel(XMLStreamReader xmlStreamReader) throws Exception {
        Schema schema;
        String targetNameSpace = getTargetNameSpace(xmlStreamReader);
        if (DMN_13_TARGET_NAMESPACE.equals(targetNameSpace)) {
            schema = createSchema(DMN_XSD);
        } else if (DMN_12_TARGET_NAMESPACE.equals(targetNameSpace)) {
            schema = createSchema(DMN_12_XSD);
        } else {
            schema = createSchema(DMN_11_XSD);
        }
        Validator validator = schema.newValidator();
        validator.validate(new StAXSource(xmlStreamReader));
    }

    protected String getTargetNameSpace(InputStream is) {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader xtr = xif.createXMLStreamReader(is);

            return getTargetNameSpace(xtr);
        } catch (XMLStreamException e) {
            LOGGER.error("Error processing DMN document", e);
            throw new DmnXMLException("Error processing DMN document", e);
        }
    }

    protected String getTargetNameSpace(XMLStreamReader xmlStreamReader) {
        String targetNameSpace = null;
        try {
            while (xmlStreamReader.hasNext()) {
                try {
                    xmlStreamReader.next();
                } catch (Exception e) {
                    LOGGER.debug("Error reading XML document", e);
                    throw new DmnXMLException("Error reading XML", e);
                }
                targetNameSpace = xmlStreamReader.getNamespaceURI();
                break;
            }
        } catch (XMLStreamException e) {
            LOGGER.error("Error processing DMN document", e);
            throw new DmnXMLException("Error processing DMN document", e);
        }

        return targetNameSpace;
    }

    protected Schema createSchema(String xsd) throws SAXException {
        SchemaFactory factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = null;
        if (classloader != null) {
            schema = factory.newSchema(classloader.getResource(xsd));
        }

        if (schema == null) {
            schema = factory.newSchema(this.getClass().getClassLoader().getResource(xsd));
        }

        if (schema == null) {
            throw new DmnXMLException("DMN XSD could not be found");
        }
        return schema;
    }

    public DmnDefinition convertToDmnModel(InputStreamProvider inputStreamProvider, boolean validateSchema, boolean enableSafeDmnXml) {
        return convertToDmnModel(inputStreamProvider, validateSchema, enableSafeDmnXml, DEFAULT_ENCODING);
    }

    public DmnDefinition convertToDmnModel(InputStreamProvider inputStreamProvider, boolean validateSchema, boolean enableSafeDmnXml, String encoding) {
        XMLInputFactory xif = XMLInputFactory.newInstance();

        if (xif.isPropertySupported(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES)) {
            xif.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, false);
        }

        if (xif.isPropertySupported(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES)) {
            xif.setProperty(XMLInputFactory.IS_SUPPORTING_EXTERNAL_ENTITIES, false);
        }

        if (xif.isPropertySupported(XMLInputFactory.SUPPORT_DTD)) {
            xif.setProperty(XMLInputFactory.SUPPORT_DTD, false);
        }

        if (validateSchema) {
            try (InputStreamReader in = new InputStreamReader(inputStreamProvider.getInputStream(), encoding)) {
                if (!enableSafeDmnXml) {
                    validateModel(inputStreamProvider);
                } else {
                    validateModel(xif.createXMLStreamReader(in));
                }
            } catch (UnsupportedEncodingException e) {
                throw new DmnXMLException("The dmn xml is not properly encoded", e);
            } catch (XMLStreamException e) {
                throw new DmnXMLException("Error while reading the dmn xml file", e);
            } catch (Exception e) {
                throw new DmnXMLException(e.getMessage(), e);
            }
        }
        // The input stream is closed after schema validation
        try (InputStreamReader in = new InputStreamReader(inputStreamProvider.getInputStream(), encoding)) {
            // XML conversion
            return convertToDmnModel(xif.createXMLStreamReader(in));
        } catch (UnsupportedEncodingException e) {
            throw new DmnXMLException("The dmn xml is not properly encoded", e);
        } catch (XMLStreamException e) {
            throw new DmnXMLException("Error while reading the dmn xml file", e);
        } catch (IOException e) {
            throw new DmnXMLException(e.getMessage(), e);
        }
    }

    public DmnDefinition convertToDmnModel(XMLStreamReader xtr) {
        DmnDefinition model = new DmnDefinition();
        DmnElement parentElement = null;
        Decision currentDecision = null;
        DecisionTable currentDecisionTable = null;

        ConversionHelper conversionHelper = new ConversionHelper();
        conversionHelper.setDmnDefinition(model);

        try {
            while (xtr.hasNext()) {
                try {
                    xtr.next();
                } catch (Exception e) {
                    LOGGER.debug("Error reading XML document", e);
                    throw new DmnXMLException("Error reading XML", e);
                }

                if (!xtr.isStartElement()) {
                    continue;
                }

                if (ELEMENT_DEFINITIONS.equals(xtr.getLocalName())) {
                    model.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
                    model.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));
                    model.setExporter(xtr.getAttributeValue(null, ATTRIBUTE_EXPORTER));
                    model.setExporterVersion(xtr.getAttributeValue(null, ATTRIBUTE_EXPORTER_VERSION));
                    model.setNamespace(MODEL_NAMESPACE);
                    parentElement = model;
                } else if (ELEMENT_DECISION.equals(xtr.getLocalName())) {

                    // reset element counters
                    convertersToDmnMap.get(ELEMENT_RULE).initializeElementCounter();
                    convertersToDmnMap.get(ELEMENT_INPUT_CLAUSE).initializeElementCounter();
                    convertersToDmnMap.get(ELEMENT_OUTPUT_CLAUSE).initializeElementCounter();

                    currentDecision = new Decision();
                    currentDecision.setDmnDefinition(model);
                    model.addDecision(currentDecision);
                    currentDecision.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));
                    currentDecision.setName(xtr.getAttributeValue(null, ATTRIBUTE_NAME));

                    if (Boolean.parseBoolean(xtr.getAttributeValue(FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORCE_DMN_11))) {
                        currentDecision.setForceDMN11(true);
                    }

                    parentElement = currentDecision;
                    conversionHelper.setCurrentDecision(currentDecision);
                } else if (ELEMENT_DECISION_TABLE.equals(xtr.getLocalName())) {
                    currentDecisionTable = new DecisionTable();
                    currentDecisionTable.setId(xtr.getAttributeValue(null, ATTRIBUTE_ID));

                    if (xtr.getAttributeValue(null, ATTRIBUTE_HIT_POLICY) != null) {
                        currentDecisionTable.setHitPolicy(HitPolicy.get(xtr.getAttributeValue(null, ATTRIBUTE_HIT_POLICY)));
                    } else {
                        currentDecisionTable.setHitPolicy(HitPolicy.FIRST);
                    }

                    if (xtr.getAttributeValue(null, ATTRIBUTE_AGGREGATION) != null) {
                        currentDecisionTable.setAggregation(BuiltinAggregator.get(xtr.getAttributeValue(null, ATTRIBUTE_AGGREGATION)));
                    }
                    currentDecision.setExpression(currentDecisionTable);
                    parentElement = currentDecisionTable;
                } else if (ELEMENT_DESCRIPTION.equals(xtr.getLocalName())) {
                    // limit description to 255 characters
                    parentElement.setDescription(StringUtils.abbreviate(xtr.getElementText(), 255));
                } else if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                    while (xtr.hasNext()) {
                        xtr.next();
                        if (xtr.isStartElement()) {
                            DmnExtensionElement extensionElement = DmnXMLUtil.parseExtensionElement(xtr);
                            parentElement.addExtensionElement(extensionElement);
                        } else if (xtr.isEndElement()) {
                            if (ELEMENT_EXTENSIONS.equals(xtr.getLocalName())) {
                                break;
                            }
                        }
                    }
                } else if (convertersToDmnMap.containsKey(xtr.getLocalName())) {
                    BaseDmnXMLConverter converter = convertersToDmnMap.get(xtr.getLocalName());
                    converter.convertToDmnModel(xtr, conversionHelper);
                }
            }
        } catch (DmnXMLException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.error("Error processing DMN document", e);
            throw new DmnXMLException("Error processing DMN document", e);
        }

        processDiElements(conversionHelper);
        return model;
    }

    protected void processDiElements(ConversionHelper conversionHelper) {
        DmnDefinition dmnDefinition = conversionHelper.getDmnDefinition();

        conversionHelper.getDiDiagrams()
            .forEach(diDiagram -> {
                dmnDefinition.addDiDiagram(diDiagram);
                if (conversionHelper.getDiShapes(diDiagram.getId()) != null) {
                    conversionHelper.getDiShapes(diDiagram.getId())
                        .forEach(dmnDiShape -> {
                            dmnDefinition.addGraphicInfoByDiagramId(diDiagram.getId(), dmnDiShape.getDmnElementRef(), dmnDiShape.getGraphicInfo());
                            if (dmnDiShape.getDecisionServiceDividerLine() != null) {
                                dmnDefinition.addDecisionServiceDividerGraphicInfoListByDiagramId(diDiagram.getId(), dmnDiShape.getDmnElementRef(),
                                    dmnDiShape.getDecisionServiceDividerLine().getWaypoints());
                            }
                        });
                }
                if (conversionHelper.getDiEdges(diDiagram.getId()) != null) {
                    conversionHelper.getDiEdges(diDiagram.getId())
                        .forEach(dmnDiEdge -> {
                            if (dmnDiEdge.getId() != null) {
                                dmnDefinition.addFlowGraphicInfoListByDiagramId(diDiagram.getId(), dmnDiEdge.getDmnElementRef(), dmnDiEdge.getWaypoints());
                            }
                        });
                }
            });
    }

    public byte[] convertToXML(DmnDefinition model) {
        return convertToXML(model, DEFAULT_ENCODING);
    }

    public byte[] convertToXML(DmnDefinition model, String encoding) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            OutputStreamWriter out = new OutputStreamWriter(outputStream, encoding);

            XMLStreamWriter writer = xof.createXMLStreamWriter(out);
            XMLStreamWriter xtw = new IndentingXMLStreamWriter(writer);

            xtw.writeStartElement(ELEMENT_DEFINITIONS);
            xtw.writeDefaultNamespace(DMN_NAMESPACE);
            xtw.writeNamespace(DMNDI_PREFIX, DMNDI_NAMESPACE);
            xtw.writeNamespace(OMGDC_PREFIX, OMGDC_NAMESPACE);
            xtw.writeNamespace(OMGDI_PREFIX, OMGDI_NAMESPACE);
            xtw.writeAttribute(ATTRIBUTE_ID, model.getId());
            if (StringUtils.isNotEmpty(model.getName())) {
                xtw.writeAttribute(ATTRIBUTE_NAME, model.getName());
            }
            xtw.writeAttribute(ATTRIBUTE_NAMESPACE, MODEL_NAMESPACE);
            if (StringUtils.isNotEmpty(model.getExporter())) {
                xtw.writeAttribute(DmnXMLConstants.ATTRIBUTE_EXPORTER, model.getExporter());
            }
            if (StringUtils.isNotEmpty(model.getExporterVersion())) {
                xtw.writeAttribute(DmnXMLConstants.ATTRIBUTE_EXPORTER_VERSION, model.getExporterVersion());
            }

            DmnXMLUtil.writeElementDescription(model, xtw);
            DmnXMLUtil.writeExtensionElements(model, xtw);

            for (InputData inputData : model.getInputData()) {
                xtw.writeStartElement(ELEMENT_INPUT_DATA);
                xtw.writeAttribute(ATTRIBUTE_ID, inputData.getId());
                if (StringUtils.isNotEmpty(inputData.getName())) {
                    xtw.writeAttribute(ATTRIBUTE_NAME, inputData.getName());
                }

                if (inputData.getVariable() != null) {
                    InformationItem variable = inputData.getVariable();
                    xtw.writeStartElement(ELEMENT_VARIABLE);
                    xtw.writeAttribute(ATTRIBUTE_ID, variable.getId());
                    xtw.writeAttribute(ATTRIBUTE_TYPE_REF, variable.getTypeRef());
                    if (StringUtils.isNotEmpty(variable.getName())) {
                        xtw.writeAttribute(ATTRIBUTE_NAME, variable.getName());
                    }
                    xtw.writeEndElement();
                }

                DmnXMLUtil.writeElementDescription(inputData, xtw);
                DmnXMLUtil.writeExtensionElements(inputData, xtw);

                xtw.writeEndElement();
            }

            writeItemDefinition(model.getItemDefinitions(), xtw);

            for (Decision decision : model.getDecisions()) {
                xtw.writeStartElement(ELEMENT_DECISION);
                xtw.writeAttribute(ATTRIBUTE_ID, decision.getId());
                if (StringUtils.isNotEmpty(decision.getName())) {
                    xtw.writeAttribute(ATTRIBUTE_NAME, decision.getName());
                }

                if (decision.isForceDMN11()) {
                    xtw.writeNamespace(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE);
                    xtw.writeAttribute(FLOWABLE_EXTENSIONS_PREFIX, FLOWABLE_EXTENSIONS_NAMESPACE, ATTRIBUTE_FORCE_DMN_11, "true");
                }

                DmnXMLUtil.writeElementDescription(decision, xtw);
                DmnXMLUtil.writeExtensionElements(decision, xtw);

                if (decision.getVariable() != null) {
                    xtw.writeStartElement(ELEMENT_VARIABLE);
                    if (StringUtils.isNotEmpty(decision.getVariable().getId())) {
                        xtw.writeAttribute(ATTRIBUTE_ID, decision.getVariable().getId());
                    }
                    if (StringUtils.isNotEmpty(decision.getVariable().getName())) {
                        xtw.writeAttribute(ATTRIBUTE_NAME, decision.getVariable().getName());
                    }
                    if (StringUtils.isNotEmpty(decision.getVariable().getTypeRef())) {
                        xtw.writeAttribute(ATTRIBUTE_TYPE_REF, decision.getVariable().getTypeRef());
                    }
                    xtw.writeEndElement();
                }

                for (InformationRequirement informationRequirement : decision.getRequiredDecisions()) {
                    xtw.writeStartElement(ELEMENT_INFORMATION_REQUIREMENT);
                    xtw.writeAttribute(ATTRIBUTE_ID, informationRequirement.getId());

                    if (informationRequirement.getRequiredDecision() != null) {
                        xtw.writeStartElement(ELEMENT_REQUIRED_DECISION);
                        xtw.writeAttribute(ATTRIBUTE_HREF, informationRequirement.getRequiredDecision().getHref());
                        xtw.writeEndElement();
                    }

                    xtw.writeEndElement();
                }

                for (InformationRequirement informationRequirement : decision.getRequiredInputs()) {
                    xtw.writeStartElement(ELEMENT_INFORMATION_REQUIREMENT);
                    xtw.writeAttribute(ATTRIBUTE_ID, informationRequirement.getId());

                    if (informationRequirement.getRequiredInput() != null) {
                        xtw.writeStartElement(ELEMENT_REQUIRED_INPUT);
                        xtw.writeAttribute(ATTRIBUTE_HREF, informationRequirement.getRequiredInput().getHref());
                        xtw.writeEndElement();
                    }

                    xtw.writeEndElement();
                }

                for (AuthorityRequirement authorityRequirement : decision.getAuthorityRequirements()) {
                    xtw.writeStartElement(ELEMENT_AUTHORITY_REQUIREMENT);
                    xtw.writeAttribute(ATTRIBUTE_ID, authorityRequirement.getId());

                    if (authorityRequirement.getRequiredAuthority() != null) {
                        xtw.writeStartElement(ELEMENT_REQUIRED_AUTHORITY);
                        xtw.writeAttribute(ATTRIBUTE_HREF, authorityRequirement.getRequiredAuthority().getHref());
                        xtw.writeEndElement();
                    }

                    xtw.writeEndElement();
                }

                // decision table
                if (decision.getExpression() != null) {
                    DecisionTable decisionTable = (DecisionTable) decision.getExpression();

                    xtw.writeStartElement(ELEMENT_DECISION_TABLE);
                    xtw.writeAttribute(ATTRIBUTE_ID, decisionTable.getId());

                    if (decisionTable.getHitPolicy() != null) {
                        xtw.writeAttribute(ATTRIBUTE_HIT_POLICY, decisionTable.getHitPolicy().getValue());
                    }

                    if (decisionTable.getAggregation() != null) {
                        xtw.writeAttribute(ATTRIBUTE_AGGREGATION, decisionTable.getAggregation().toString());
                    }

                    DmnXMLUtil.writeElementDescription(decisionTable, xtw);
                    DmnXMLUtil.writeExtensionElements(decisionTable, xtw);

                    for (InputClause clause : decisionTable.getInputs()) {
                        xtw.writeStartElement(ELEMENT_INPUT_CLAUSE);
                        if (StringUtils.isNotEmpty(clause.getId())) {
                            xtw.writeAttribute(ATTRIBUTE_ID, clause.getId());
                        }
                        if (StringUtils.isNotEmpty(clause.getLabel())) {
                            xtw.writeAttribute(ATTRIBUTE_LABEL, clause.getLabel());
                        }

                        DmnXMLUtil.writeElementDescription(clause, xtw);
                        DmnXMLUtil.writeExtensionElements(clause, xtw);

                        if (clause.getInputExpression() != null) {
                            xtw.writeStartElement(ELEMENT_INPUT_EXPRESSION);

                            if (StringUtils.isNotEmpty(clause.getInputExpression().getId())) {
                                xtw.writeAttribute(ATTRIBUTE_ID, clause.getInputExpression().getId());
                            }

                            if (StringUtils.isNotEmpty(clause.getInputExpression().getTypeRef())) {
                                xtw.writeAttribute(ATTRIBUTE_TYPE_REF, clause.getInputExpression().getTypeRef());
                            }

                            if (StringUtils.isNotEmpty(clause.getInputExpression().getText())) {
                                xtw.writeStartElement(ELEMENT_TEXT);
                                xtw.writeCharacters(clause.getInputExpression().getText());
                                xtw.writeEndElement();
                            }

                            xtw.writeEndElement();
                        }

                        if (clause.getInputValues() != null && StringUtils.isNotEmpty(clause.getInputValues().getText())) {
                            xtw.writeStartElement(ELEMENT_INPUT_VALUES);
                            xtw.writeStartElement(ELEMENT_TEXT);
                            xtw.writeCharacters(clause.getInputValues().getText());
                            xtw.writeEndElement();
                            xtw.writeEndElement();
                        }

                        xtw.writeEndElement();
                    }

                    for (OutputClause clause : decisionTable.getOutputs()) {
                        xtw.writeStartElement(ELEMENT_OUTPUT_CLAUSE);
                        if (StringUtils.isNotEmpty(clause.getId())) {
                            xtw.writeAttribute(ATTRIBUTE_ID, clause.getId());
                        }
                        if (StringUtils.isNotEmpty(clause.getLabel())) {
                            xtw.writeAttribute(ATTRIBUTE_LABEL, clause.getLabel());
                        }
                        if (StringUtils.isNotEmpty(clause.getName())) {
                            xtw.writeAttribute(ATTRIBUTE_NAME, clause.getName());
                        }
                        if (StringUtils.isNotEmpty(clause.getTypeRef())) {
                            xtw.writeAttribute(ATTRIBUTE_TYPE_REF, clause.getTypeRef());
                        }

                        if (clause.getOutputValues() != null && StringUtils.isNotEmpty(clause.getOutputValues().getText())) {
                            xtw.writeStartElement(ELEMENT_OUTPUT_VALUES);
                            xtw.writeStartElement(ELEMENT_TEXT);
                            xtw.writeCharacters(clause.getOutputValues().getText());
                            xtw.writeEndElement();
                            xtw.writeEndElement();
                        }

                        DmnXMLUtil.writeElementDescription(clause, xtw);
                        DmnXMLUtil.writeExtensionElements(clause, xtw);

                        xtw.writeEndElement();
                    }

                    for (DecisionRule rule : decisionTable.getRules()) {
                        xtw.writeStartElement(ELEMENT_RULE);
                        if (StringUtils.isNotEmpty(rule.getId())) {
                            xtw.writeAttribute(ATTRIBUTE_ID, rule.getId());
                        }

                        DmnXMLUtil.writeElementDescription(rule, xtw);
                        DmnXMLUtil.writeExtensionElements(rule, xtw);

                        for (RuleInputClauseContainer container : rule.getInputEntries()) {
                            xtw.writeStartElement(ELEMENT_INPUT_ENTRY);
                            xtw.writeAttribute(ATTRIBUTE_ID, container.getInputEntry().getId());

                            DmnXMLUtil.writeExtensionElements(container.getInputEntry(), xtw);

                            if (StringUtils.isNotEmpty(container.getInputEntry().getText())) {
                                xtw.writeStartElement(ELEMENT_TEXT);
                                xtw.writeCData(container.getInputEntry().getText());
                                xtw.writeEndElement();
                            }

                            xtw.writeEndElement();
                        }

                        for (RuleOutputClauseContainer container : rule.getOutputEntries()) {
                            xtw.writeStartElement(ELEMENT_OUTPUT_ENTRY);
                            xtw.writeAttribute(ATTRIBUTE_ID, container.getOutputEntry().getId());

                            if (StringUtils.isNotEmpty(container.getOutputEntry().getText())) {
                                xtw.writeStartElement(ELEMENT_TEXT);
                                xtw.writeCData(container.getOutputEntry().getText());
                                xtw.writeEndElement();
                            }

                            xtw.writeEndElement();
                        }

                        xtw.writeEndElement();
                    }
                    xtw.writeEndElement();
                }
                xtw.writeEndElement();
            }

            for (DecisionService decisionService : model.getDecisionServices()) {
                xtw.writeStartElement(ELEMENT_DECISION_SERVICE);
                xtw.writeAttribute(ATTRIBUTE_ID, decisionService.getId());
                if (StringUtils.isNotEmpty(decisionService.getName())) {
                    xtw.writeAttribute(ATTRIBUTE_NAME, decisionService.getName());
                }

                for (DmnElementReference reference : decisionService.getOutputDecisions()) {
                    xtw.writeStartElement(ELEMENT_OUTPUT_DECISION);
                    xtw.writeAttribute(ATTRIBUTE_HREF, reference.getHref());
                    xtw.writeEndElement();
                }

                for (DmnElementReference reference : decisionService.getEncapsulatedDecisions()) {
                    xtw.writeStartElement(ELEMENT_ENCAPSULATED_DECISION);
                    xtw.writeAttribute(ATTRIBUTE_HREF, reference.getHref());
                    xtw.writeEndElement();
                }

                for (DmnElementReference reference : decisionService.getInputData()) {
                    xtw.writeStartElement(ELEMENT_INPUT_DATA);
                    xtw.writeAttribute(ATTRIBUTE_HREF, reference.getHref());
                    xtw.writeEndElement();
                }

                xtw.writeEndElement();
            }

            DMNDIExport.writeDMNDI(model, xtw);

            // end definitions root element
            xtw.writeEndElement();
            xtw.writeEndDocument();

            xtw.flush();

            outputStream.close();

            xtw.close();

            return outputStream.toByteArray();

        } catch (Exception e) {
            LOGGER.error("Error writing DMN XML", e);
            throw new DmnXMLException("Error writing DMN XML", e);
        }
    }

    protected void writeItemDefinition(List<ItemDefinition> itemDefinitions, XMLStreamWriter xtw) throws Exception {
        writeItemDefinition(itemDefinitions, false, xtw);
    }

    protected void writeItemDefinition(List<ItemDefinition> itemDefinitions, boolean isItemComponent, XMLStreamWriter xtw) throws Exception {
        if (itemDefinitions == null) {
            return;
        }

        for (ItemDefinition itemDefinition : itemDefinitions) {
            if (isItemComponent) {
                xtw.writeStartElement(ELEMENT_ITEM_COMPONENT);
            } else {
                xtw.writeStartElement(ELEMENT_ITEM_DEFINITION);
            }
            if (StringUtils.isNotEmpty(itemDefinition.getId())) {
                xtw.writeAttribute(ATTRIBUTE_ID, itemDefinition.getId());
            }
            if (StringUtils.isNotEmpty(itemDefinition.getName())) {
                xtw.writeAttribute(ATTRIBUTE_NAME, itemDefinition.getName());
            }
            if (StringUtils.isNotEmpty(itemDefinition.getLabel())) {
                xtw.writeAttribute(ATTRIBUTE_LABEL, itemDefinition.getLabel());
            }
            if (itemDefinition.isCollection()) {
                xtw.writeAttribute(ATTRIBUTE_IS_COLLECTION, "true");
            }

            DmnXMLUtil.writeElementDescription(itemDefinition, xtw);
            DmnXMLUtil.writeExtensionElements(itemDefinition, xtw);

            if (itemDefinition.getTypeRef() != null) {
                xtw.writeStartElement(ELEMENT_TYPE_REF);
                xtw.writeCharacters(itemDefinition.getTypeRef());
                xtw.writeEndElement();
            }

            if (itemDefinition.getAllowedValues() != null) {
                xtw.writeStartElement(ELEMENT_REQUIRED_AUTHORITY);
                xtw.writeStartElement(ELEMENT_TEXT);
                xtw.writeCharacters(itemDefinition.getAllowedValues().getText());
                xtw.writeEndElement();
                xtw.writeEndElement();
            }

            if (itemDefinition.getItemComponents().size() > 0) {
                writeItemDefinition(itemDefinition.getItemComponents(), true, xtw);
            }

            xtw.writeEndElement();
        }

    }
}
