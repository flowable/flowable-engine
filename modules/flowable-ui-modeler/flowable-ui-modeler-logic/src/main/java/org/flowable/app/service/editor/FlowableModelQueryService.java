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
package org.flowable.app.service.editor;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamReader;

import org.activiti.editor.language.json.converter.RDSBpmnJsonConverter;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.model.common.ResultListDataRepresentation;
import org.flowable.app.model.editor.AppDefinitionListModelRepresentation;
import org.flowable.app.model.editor.ModelRepresentation;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.repository.editor.ModelSort;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.app.util.XmlUtil;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.converter.BpmnXMLConverter;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.ExtensionElement;
import org.flowable.editor.language.json.converter.BpmnJsonConverter;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.editor.language.json.model.ModelInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableModelQueryService {

    private static final Logger logger = LoggerFactory.getLogger(FlowableModelQueryService.class);

    protected static final String FILTER_SHARED_WITH_ME = "sharedWithMe";
    protected static final String FILTER_SHARED_WITH_OTHERS = "sharedWithOthers";
    protected static final String FILTER_FAVORITE = "favorite";

    protected static final int MIN_FILTER_LENGTH = 1;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ObjectMapper objectMapper;

    protected BpmnXMLConverter bpmnXmlConverter = new BpmnXMLConverter();
    protected RDSBpmnJsonConverter bpmnJsonConverter = new RDSBpmnJsonConverter();

    public ResultListDataRepresentation getModels(String filter, String sort, Integer modelType, HttpServletRequest request) {

        // need to parse the filterText parameter ourselves, due to encoding issues with the default parsing.
        String filterText = null;
        String tagId = null;
        List<NameValuePair> params = URLEncodedUtils.parse(request.getQueryString(), Charset.forName("UTF-8"));
        if (params != null) {
            for (NameValuePair nameValuePair : params) {
                if ("filterText".equalsIgnoreCase(nameValuePair.getName())) {
                    filterText = nameValuePair.getValue();
                } else if("tagId".equalsIgnoreCase(nameValuePair.getName())) {
                  tagId = nameValuePair.getValue();
                }
            }
        }

        List<ModelRepresentation> resultList = new ArrayList<ModelRepresentation>();
        List<Model> models = null;

        String validFilter = makeValidFilterText(filterText);
        
        if(tagId !=null) {
          models = modelRepository.findByModelTypeAndTag(modelType, tagId, sort);
        } else if (validFilter != null) {
            models = modelRepository.findByModelTypeAndFilter(modelType, validFilter, sort);

        } else {
            models = modelRepository.findByModelType(modelType, sort);
        }

        if (CollectionUtils.isNotEmpty(models)) {
            List<String> addedModelIds = new ArrayList<String>();
            for (Model model : models) {
                if (!addedModelIds.contains(model.getId())) {
                    addedModelIds.add(model.getId());
                    ModelRepresentation representation = createModelRepresentation(model);
                    resultList.add(representation);
                }
            }
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        return result;
    }

    public ResultListDataRepresentation getModelsToIncludeInAppDefinition() {

        List<ModelRepresentation> resultList = new ArrayList<ModelRepresentation>();

        List<String> addedModelIds = new ArrayList<String>();
        List<Model> models = modelRepository.findByModelType(AbstractModel.MODEL_TYPE_BPMN, ModelSort.MODIFIED_DESC);

        if (CollectionUtils.isNotEmpty(models)) {
            for (Model model : models) {
                if (!addedModelIds.contains(model.getId())) {
                    addedModelIds.add(model.getId());
                    ModelRepresentation representation = createModelRepresentation(model);
                    resultList.add(representation);
                }
            }
        }

        ResultListDataRepresentation result = new ResultListDataRepresentation(resultList);
        return result;
    }

    public Object importProcessModel(HttpServletRequest request, MultipartFile file, boolean overwrite) {

        String fileName = file.getOriginalFilename();
        if (fileName != null && (fileName.endsWith(".bpmn") || fileName.endsWith(".bpmn20.xml"))) {
            try {
                XMLInputFactory xif = XmlUtil.createSafeXmlInputFactory();
                InputStreamReader xmlIn = new InputStreamReader(file.getInputStream(), "UTF-8");
                XMLStreamReader xtr = xif.createXMLStreamReader(xmlIn);
                BpmnModel bpmnModel = bpmnXmlConverter.convertToBpmnModel(xtr);
                if (CollectionUtils.isEmpty(bpmnModel.getProcesses())) {
                    throw new BadRequestException("No process found in definition " + fileName);
                }

                if (bpmnModel.getLocationMap().size() == 0) {
                    BpmnAutoLayout bpmnLayout = new BpmnAutoLayout(bpmnModel);
                    bpmnLayout.execute();
                }

                org.flowable.bpmn.model.Process process = bpmnModel.getMainProcess();

                List existingProcessModels = modelRepository.findByKeyAndType(process.getId(), AbstractModel.MODEL_TYPE_BPMN);
                Map validationResult = new HashMap();
                if(!overwrite && existingProcessModels.size() > 0) {
                    validationResult.put("message", "Process with same key already exists");
                    validationResult.put("existingProcess", process.getId());
                }

                Map formKeyMap = new HashMap();
                List existingForms = new ArrayList();

                //First find any existing form with same key
                for(ExtensionElement eeForm: process.getExtensionElements().get("form")) {
                    String formkey = eeForm.getAttributeValue(null, "formkey");
                    List existingFormModels = modelRepository.findByKeyAndType(formkey, AbstractModel.MODEL_TYPE_FORM_RDS);
                    if(existingFormModels.size() > 0)
                    {
                        existingForms.add(formkey);
                    }
                }

                // Return validation result if process with same key or any form with same form key exists if overwrite is false
                if(!overwrite && (validationResult.size() > 0 || existingForms.size() > 0)) {
                    validationResult.put("existingForms", existingForms);
                    return validationResult;
                }

                for(ExtensionElement eeForm: process.getExtensionElements().get("form")) {
                    String formkey = eeForm.getAttributeValue(null, "formkey");
                    String formName = eeForm.getAttributeValue(null, "name");
                    if(formName == null) {
                        formName = formkey;
                    }
                    String formDesc = eeForm.getAttributeValue(null, "description");
                    String formDefinition = eeForm.getElementText();
                    ModelRepresentation model = new ModelRepresentation();
                    model.setKey(formkey);
                    model.setName(formName);
                    model.setDescription(formDesc);
                    model.setModelType(AbstractModel.MODEL_TYPE_FORM_RDS);

                    List existingFormModels = modelRepository.findByKeyAndType(formkey, AbstractModel.MODEL_TYPE_FORM_RDS);
                    if(existingFormModels.size() > 0) {
                        Model existFormModel = (Model) existingFormModels.get(0);
                        modelService.saveModel(existFormModel, formDefinition, null,true, "imported", SecurityUtils.getCurrentUserObject());
                        ModelInfo modelInfo = new ModelInfo(existFormModel.getId(), existFormModel.getName(), formkey);
                        formKeyMap.put(formkey, modelInfo);

                    } else {
                        Model formModel = modelService
                            .createModel(model, formDefinition, SecurityUtils.getCurrentUserObject());
                        ModelInfo modelInfo = new ModelInfo(formModel.getId(), formModel.getName(), formModel.getKey());
                        formKeyMap.put(formkey, modelInfo);
                    }

                }

                ObjectNode modelNode = bpmnJsonConverter.convertToJson(bpmnModel, formKeyMap, null);


                String name = process.getId();
                if (StringUtils.isNotEmpty(process.getName())) {
                    name = process.getName();
                }
                String description = process.getDocumentation();

                ModelRepresentation model = new ModelRepresentation();
                model.setKey(process.getId());
                model.setName(name);
                model.setDescription(description);
                model.setModelType(AbstractModel.MODEL_TYPE_BPMN);
                if(existingProcessModels.size() > 0) {
                    Model existProcessModel = (Model) existingProcessModels.get(0);
                    modelService.saveModel(existProcessModel, modelNode.toString(), null, true, "imported", SecurityUtils.getCurrentUserObject());
                    return new ModelRepresentation(existProcessModel);
                } else
                {
                    Model newModel = modelService
                        .createModel(model, modelNode.toString(), SecurityUtils.getCurrentUserObject());
                    return new ModelRepresentation(newModel);
                }
            } catch (BadRequestException e) {
                throw e;

            } catch (Exception e) {
                logger.error("Import failed for {}", fileName, e);
                throw new BadRequestException("Import failed for " + fileName + ", error message " + e.getMessage());
            }
        } else {
            throw new BadRequestException("Invalid file name, only .bpmn and .bpmn20.xml files are supported not " + fileName);
        }
    }

    protected ModelRepresentation createModelRepresentation(AbstractModel model) {
        ModelRepresentation representation = null;
        if (model.getModelType() != null && model.getModelType() == 3) {
            representation = new AppDefinitionListModelRepresentation(model);

            AppDefinition appDefinition = null;
            try {
                appDefinition = objectMapper.readValue(model.getModelEditorJson(), AppDefinition.class);
            } catch (Exception e) {
                logger.error("Error deserializing app {}", model.getId(), e);
                throw new InternalServerErrorException("Could not deserialize app definition");
            }
            ((AppDefinitionListModelRepresentation) representation).setAppDefinition(appDefinition);

        } else {
            representation = new ModelRepresentation(model);
        }
        return representation;
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

}
