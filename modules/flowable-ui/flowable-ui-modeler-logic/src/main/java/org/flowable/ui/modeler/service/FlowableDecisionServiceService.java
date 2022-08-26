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
package org.flowable.ui.modeler.service;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.editor.converter.DmnJsonConverter;
import org.flowable.dmn.editor.converter.DmnJsonConverterContext;
import org.flowable.dmn.model.DmnDefinition;
import org.flowable.dmn.xml.converter.DmnXMLConverter;
import org.flowable.ui.common.model.ResultListDataRepresentation;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.BadRequestException;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.common.util.XmlUtil;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.domain.ModelHistory;
import org.flowable.ui.modeler.model.ModelRepresentation;
import org.flowable.ui.modeler.model.decisionservice.DecisionServiceRepresentation;
import org.flowable.ui.modeler.repository.ModelSort;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author yvoswillens
 * @author erikwinlof
 */
@Service
@Transactional
public class FlowableDecisionServiceService extends BaseFlowableModelService {

    private static final Logger LOGGER = LoggerFactory.getLogger(FlowableDecisionServiceService.class);

    protected static final int MIN_FILTER_LENGTH = 1;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ObjectMapper objectMapper;

    protected DmnJsonConverter dmnJsonConverter = new DmnJsonConverter();
    protected DmnXMLConverter dmnXmlConverter = new DmnXMLConverter();

    public List<DecisionServiceRepresentation> getDecisionServices(String[] decisionServiceIds) {
        List<DecisionServiceRepresentation> decisionServiceRepresentations = new ArrayList<>();
        for (String decisionServiceId : decisionServiceIds) {
            Model model = getModel(decisionServiceId, true, false);
            DecisionServiceRepresentation decisionTableRepresentation = createDecisionServiceRepresentation(model);
            decisionServiceRepresentations.add(decisionTableRepresentation);
        }
        return decisionServiceRepresentations;
    }

    public ResultListDataRepresentation getDecisionServices(String filter) {
        String validFilter = makeValidFilterText(filter);

        List<Model> models = null;

        if (validFilter != null) {
            models = modelRepository.findByModelTypeAndFilter(AbstractModel.MODEL_TYPE_DECISION_SERVICE, validFilter, ModelSort.NAME_ASC);

        } else {
            models = modelRepository.findByModelType(AbstractModel.MODEL_TYPE_DECISION_SERVICE, ModelSort.NAME_ASC);
        }

        List<DecisionServiceRepresentation> reps = new ArrayList<>();

        for (Model model : models) {
            reps.add(new DecisionServiceRepresentation(model));
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(reps);
        result.setTotal(Long.valueOf(models.size()));
        return result;
    }

    public void exportDecisionService(HttpServletResponse response, String decisionServiceId) {
        exportDecisionServiceDefinition(response, getModel(decisionServiceId, true, false));
    }

    public void exportHistoricDecisionService(HttpServletResponse response, String modelId, String modelHistoryId) {
        // Get the historic model
        ModelHistory modelHistory = modelService.getModelHistory(modelId, modelHistoryId);

        // Load model and check we have read rights
        getModel(modelHistory.getModelId(), true, false);

        exportDecisionServiceDefinition(response, modelHistory);
    }

    protected void exportDecisionServiceDefinition(HttpServletResponse response, AbstractModel definitionModel) {
        ConverterContext converterContext = new ConverterContext(modelService, objectMapper);
        List<Model> decisionTableModels = modelRepository.findByModelType(AbstractModel.MODEL_TYPE_DECISION_TABLE, ModelSort.MODIFIED_DESC);
        Map<String, String> decisionTableEditorJSON = decisionTableModels.stream()
                .collect(Collectors.toMap(
                        AbstractModel::getKey,
                        AbstractModel::getModelEditorJson
                ));
        converterContext.getDecisionTableKeyToJsonStringMap().putAll(decisionTableEditorJSON);
        exportDecisionServiceDefinition(response, definitionModel, converterContext);
    }

    protected void exportDecisionServiceDefinition(HttpServletResponse response, AbstractModel definitionModel, DmnJsonConverterContext converterContext) {
        try {

            JsonNode editorJsonNode = objectMapper.readTree(definitionModel.getModelEditorJson());

            // URLEncoder.encode will replace spaces with '+', to keep the actual name replacing '+' to '%20'
            String fileName = URLEncoder.encode(definitionModel.getName(), "UTF-8").replaceAll("\\+", "%20") + ".dmn";
            response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

            ServletOutputStream servletOutputStream = response.getOutputStream();
            response.setContentType("application/xml");

            DmnDefinition dmnDefinition = dmnJsonConverter.convertToDmn(editorJsonNode, definitionModel.getId(), converterContext);
            byte[] xmlBytes = dmnXmlConverter.convertToXML(dmnDefinition);

            BufferedInputStream in = new BufferedInputStream(new ByteArrayInputStream(xmlBytes));

            byte[] buffer = new byte[8096];
            while (true) {
                int count = in.read(buffer);
                if (count == -1) {
                    break;
                }
                servletOutputStream.write(buffer, 0, count);
            }

            // Flush and close stream
            servletOutputStream.flush();
            servletOutputStream.close();

        } catch (Exception e) {
            LOGGER.error("Could not export decision table model", e);
            throw new InternalServerErrorException("Could not export decision table model");
        }
    }

    public ModelRepresentation importDecisionService(HttpServletRequest request, MultipartFile file) {

        String fileName = file.getOriginalFilename();
        if (fileName != null && (fileName.endsWith(".dmn") || fileName.endsWith(".xml"))) {
            try {

                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);

                DmnDefinition dmnDefinition = dmnXmlConverter.convertToDmnModel(xtr);

                if (dmnDefinition.getDecisions().size() == 0) {
                    throw new FlowableException("No decisions found in " + fileName);
                }

                ObjectNode editorJsonNode = dmnJsonConverter.convertToJson(dmnDefinition);

                // remove id to avoid InvalidFormatException when deserializing
                editorJsonNode.remove("id");

                ModelRepresentation modelRepresentation = new ModelRepresentation();
                modelRepresentation.setKey(dmnDefinition.getDecisions().get(0).getId());
                modelRepresentation.setName(dmnDefinition.getName());
                modelRepresentation.setDescription(dmnDefinition.getDescription());
                modelRepresentation.setModelType(AbstractModel.MODEL_TYPE_DECISION_TABLE);
                Model model = modelService.createModel(modelRepresentation, editorJsonNode.toString(), SecurityUtils.getCurrentUserId());
                return new ModelRepresentation(model);

            } catch (Exception e) {
                LOGGER.error("Could not import decision table model", e);
                throw new InternalServerErrorException("Could not import decision table model");
            }
        } else {
            throw new BadRequestException("Invalid file name, only .dmn or .xml files are supported not " + fileName);
        }

    }

    protected String makeValidFilterText(String filterText) {
        String validFilter = null;

        if (filterText != null) {
            String trimmed = StringUtils.trim(filterText);
            if (trimmed.length() >= MIN_FILTER_LENGTH) {
                validFilter = "%" + trimmed.toLowerCase() + "%";
            }
        }
        return validFilter;
    }

    public Model getDecisionServiceModel(String decisionTableId) {

        Model decisionTableModel = getModel(decisionTableId, true, false);

        // convert to new model version
        DecisionTableModelConversionUtil.convertModel(decisionTableModel);

        return decisionTableModel;
    }

    public DecisionServiceRepresentation getDecisionService(String decisionTableId) {
        return createDecisionServiceRepresentation(getDecisionServiceModel(decisionTableId));
    }

    public DecisionServiceRepresentation getDecisionTableRepresentation(AbstractModel decisionTableModel) {
        return createDecisionServiceRepresentation(decisionTableModel);
    }

    public DecisionServiceRepresentation getHistoricDecisionService(String modelHistoryId) {
        // Get the historic model
        ModelHistory modelHistory = modelHistoryRepository.get(modelHistoryId);

        // Load model and check we have read rights
        getModel(modelHistory.getModelId(), true, false);

        return createDecisionServiceRepresentation(modelHistory);
    }

    protected DecisionServiceRepresentation createDecisionServiceRepresentation(AbstractModel model) {
        DecisionServiceRepresentation result = new DecisionServiceRepresentation(model);
        return result;
    }
}
