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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.flowable.cmmn.editor.constants.CmmnStencilConstants;
import org.flowable.cmmn.editor.json.converter.CmmnJsonConverterContext;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.dmn.editor.converter.DmnJsonConverterContext;
import org.flowable.editor.language.json.converter.BpmnJsonConverterContext;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.serviceapi.ModelService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Helper class used during import of an app zip.
 *
 * Keeps imported models mapped by key and id and 'old ids' (model reference ids as stored in the imported json).
 *
 * @author Joram Barrez
 */
public class ConverterContext implements BpmnJsonConverterContext, CmmnJsonConverterContext, DmnJsonConverterContext {

    protected ModelService modelService;
    protected ObjectMapper objectMapper;

    // Maps that store key -> json string, as it's read from the zip file
    protected Map<String, String> processKeyToJsonStringMap = new HashMap<>();
    protected Map<String, String> caseKeyToJsonStringMap = new HashMap<>();
    protected Map<String, String> formKeyToJsonStringMap = new HashMap<>();
    protected Map<String, String> decisionTableKeyToJsonStringMap = new HashMap<>();
    protected Map<String, String> decisionServiceKeyToJsonStringMap = new HashMap<>();

    // Maps that store key -> persisted Model
    protected Map<String, Model> processKeyToModelMap = new HashMap<>();
    protected Map<String, Model> caseKeyToModelMap = new HashMap<>();
    protected Map<String, Model> formKeyToModelMap = new HashMap<>();
    protected Map<String, Model> decisionTableKeyToModelMap = new HashMap<>();
    protected Map<String, Model> referencedDecisionTableKeyToModelMap = new HashMap<>();
    protected Map<String, Model> decisionServiceKeyToModelMap = new HashMap<>();

    // Thumbnails part of the app
    protected Map<String, byte[]> modelKeyToThumbnailMap = new HashMap<>();

    // Maps that store model id -> Model
    protected Map<String, Model> processIdToModelMap = new HashMap<>();
    protected Map<String, Model> caseIdToModelMap = new HashMap<>();
    protected Map<String, Model> formIdToModelMap = new HashMap<>();
    protected Map<String, Model> decisionTableIdToModelMap = new HashMap<>();
    protected Map<String, Model> decisionServiceIdToModelMap = new HashMap<>();

    // Maps that store unresolved models. 'unresolved key' -> Model using that key
    // (this can happen due to the order of reading the files in the app zip)
    protected Map<String, List<CmmnModel>> unresolvedProcessModelKeyToCmmnModels = new HashMap<>();
    protected Map<String, List<CmmnModel>> unresolvedCaseModelKeyToCmmnModels = new HashMap<>();

    public ConverterContext(ModelService modelService, ObjectMapper objectMapper) {
        this.modelService = modelService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getFormModelKeyForFormModelId(String formModelId) {
        return getKeyForId(formIdToModelMap, formModelId);
    }

    @Override
    public Map<String, String> getFormModelInfoForFormModelKey(String formModelKey) {
        return modelToModelInfo(formKeyToModelMap, formModelKey);
    }

    @Override
    public String getCaseModelKeyForCaseModelId(String caseModelId) {
        return getKeyForId(caseIdToModelMap, caseModelId);
    }

    @Override
    public Map<String, String> getCaseModelInfoForCaseModelKey(String caseModelKey) {
        return modelToModelInfo(caseKeyToModelMap, caseModelKey);
    }

    @Override
    public String getProcessModelKeyForProcessModelId(String processModelId) {
        return getKeyForId(processIdToModelMap, processModelId);
    }

    @Override
    public Map<String, String> getProcessModelInfoForProcessModelKey(String processModelKey) {
        return modelToModelInfo(processKeyToModelMap, processModelKey);
    }

    @Override
    public String getDecisionTableModelKeyForDecisionTableModelId(String decisionTableModelId) {
        return getKeyForId(decisionTableIdToModelMap, decisionTableModelId);
    }

    @Override
    public Map<String, String> getDecisionTableModelInfoForDecisionTableModelKey(String decisionTableModelKey) {
        return modelToModelInfo(decisionTableKeyToModelMap, decisionTableModelKey);
    }

    @Override
    public String getDecisionServiceModelKeyForDecisionServiceModelId(String decisionServiceModelId) {
        return getKeyForId(decisionServiceIdToModelMap, decisionServiceModelId);
    }

    @Override
    public Map<String, String> getDecisionServiceModelInfoForDecisionServiceModelKey(String decisionServiceModelKey) {
        return modelToModelInfo(decisionServiceKeyToModelMap, decisionServiceModelKey);
    }

    protected String getKeyForId(Map<String, Model> modelMap, String id) {
        Model model = modelMap.get(id);
        if (model != null) {
            return model.getKey();
        }
        return null;
    }

    protected Map<String, String> modelToModelInfo(Map<String, Model> modelMap, String key) {
        Model model = modelMap.get(key);
        if (model != null) {
            Map<String, String> map = new HashMap<>();
            map.put("id", model.getId());
            map.put("name", model.getName());
            map.put("key", model.getKey());
            return map;
        }
        return null;
    }

    /*
     * Add model methods
     */

    public void addProcessModel(Model model) {
        addProcessModel(model, null);
    }

    public void addProcessModel(Model model, String ... oldProcessModelIds) {
        String modelKey = model.getKey();
        this.processKeyToModelMap.put(modelKey, model);
        this.processIdToModelMap.put(model.getId(), model);

        if (oldProcessModelIds != null) {
            for (String oldProcessModelId : oldProcessModelIds) {
                this.processIdToModelMap.put(oldProcessModelId, model);
            }
        }

        // Currently, cmmn -> process task uses this. Process --> process isn't using a reference (hence why not there)
        if (unresolvedProcessModelKeyToCmmnModels != null && !unresolvedProcessModelKeyToCmmnModels.isEmpty()
                && unresolvedProcessModelKeyToCmmnModels.containsKey(modelKey)) {

            Map<String, List<String>> unresolvedProcessModelKeyToCaseModelKey = new HashMap<>();
            for (String key : unresolvedProcessModelKeyToCmmnModels.keySet()) {
                List<CmmnModel> cmmnModels = unresolvedProcessModelKeyToCmmnModels.get(key);
                List<String> cmmnModelKeys = cmmnModels.stream().map(cmmnModel -> cmmnModel.getPrimaryCase().getId()).collect(Collectors.toList());
                unresolvedProcessModelKeyToCaseModelKey.put(key, cmmnModelKeys);
            }

            handleUnresolvedReferences(modelKey, model, unresolvedProcessModelKeyToCaseModelKey, processKeyToModelMap, CmmnStencilConstants.PROPERTY_PROCESS_REFERENCE);
        }
    }

    public void addCaseModel(Model model) {
        addCaseModel(model, null);
    }

    public void addCaseModel(Model model, String ... oldCaseModelIds) {
        String modelKey = model.getKey();
        this.caseKeyToModelMap.put(modelKey, model);
        this.caseIdToModelMap.put(model.getId(), model);

        if (oldCaseModelIds != null) {
            for (String oldCaseModelId : oldCaseModelIds) {
                this.caseIdToModelMap.put(oldCaseModelId, model);
            }
        }

        // Currently, there is only a caseTask in CMMN, not in BPMN (hence why it's not checked)
        if (unresolvedCaseModelKeyToCmmnModels != null && !unresolvedCaseModelKeyToCmmnModels.isEmpty()
                && unresolvedCaseModelKeyToCmmnModels.containsKey(modelKey)) {

            Map<String, List<String>> unresolvedCaseModelKeyToCaseModelKey = new HashMap<>();
            for (String key : unresolvedCaseModelKeyToCmmnModels.keySet()) {
                List<CmmnModel> cmmnModels = unresolvedCaseModelKeyToCmmnModels.get(key);
                List<String> cmmnModelKeys = cmmnModels.stream().map(cmmnModel -> cmmnModel.getPrimaryCase().getId()).collect(Collectors.toList());
                unresolvedCaseModelKeyToCaseModelKey.put(key, cmmnModelKeys);
            }

            handleUnresolvedReferences(modelKey, model, unresolvedCaseModelKeyToCaseModelKey, caseKeyToModelMap, CmmnStencilConstants.PROPERTY_CASE_REFERENCE);
        }
    }

    public void addFormModel(Model model) {
       addFormModel(model, null);
    }

    public void addFormModel(Model model, String ... oldFormModelIds) {
        this.formKeyToModelMap.put(model.getKey(), model);
        this.formIdToModelMap.put(model.getId(), model);

        if (oldFormModelIds != null) {
            for (String oldFormModelId : oldFormModelIds) {
                this.formIdToModelMap.put(oldFormModelId, model);
            }
        }

        // For form models there is no 'unresolved' key handling needed,
        // as the import of forms always happens before the model referencing them
    }

    public void addDecisionTableModel(Model model) {
        addDecisionTableModel(model, null);
    }

    public void addReferencedDecisionTableModel(Model model) {
        this.referencedDecisionTableKeyToModelMap.put(model.getKey(), model);
        this.decisionTableIdToModelMap.put(model.getId(), model);
    }

    public void addDecisionTableModel(Model model, String ... oldDecisionTableModelIds) {
        this.decisionTableKeyToModelMap.put(model.getKey(), model);
        this.decisionTableIdToModelMap.put(model.getId(), model);

        if (oldDecisionTableModelIds != null) {
            for (String oldDecisionTableModelId : oldDecisionTableModelIds) {
                this.decisionTableIdToModelMap.put(oldDecisionTableModelId, model);
            }
        }

        // For decision models there is no 'unresolved' key handling needed,
        // as the import of decisions always happens before the model referencing them
    }

    public void addReferencedDecisionTableModel(Model model, String ... oldDecisionTableModelIds) {
        this.decisionTableKeyToModelMap.put(model.getKey(), model);
        this.decisionTableIdToModelMap.put(model.getId(), model);

        if (oldDecisionTableModelIds != null) {
            for (String oldDecisionTableModelId : oldDecisionTableModelIds) {
                this.decisionTableIdToModelMap.put(oldDecisionTableModelId, model);
            }
        }

        // For decision models there is no 'unresolved' key handling needed,
        // as the import of decisions always happens before the model referencing them
    }

    public void addDecisionServiceModel(Model model) {
        addDecisionServiceModel(model, null);
    }

    public void addDecisionServiceModel(Model model, String ... oldDecisionServiceModelIds) {
        this.decisionServiceKeyToModelMap.put(model.getKey(), model);
        this.decisionServiceIdToModelMap.put(model.getId(), model);

        if (oldDecisionServiceModelIds != null) {
            for (String oldDecisionServiceModelId : oldDecisionServiceModelIds) {
                this.decisionServiceIdToModelMap.put(oldDecisionServiceModelId, model);
            }
        }

        // For decision models there is no 'unresolved' key handling needed,
        // as the import of decisions always happens before the model referencing them
    }

    protected void handleUnresolvedReferences(String modelKey, Model model, Map<String, List<String>> unresolvedReferencesMap,
            Map<String, Model> keyToModelMap, String referenceProperyName) {

        if (unresolvedReferencesMap.containsKey(modelKey)) {
            List<String> referencingModelKeys = unresolvedReferencesMap.get(modelKey);
            for (String referencingCaseModelKey : referencingModelKeys) {
                Model referencingCaseModel = keyToModelMap.get(referencingCaseModelKey);

                try {
                    JsonNode referencingCaseModelJson = objectMapper.readTree(referencingCaseModel.getModelEditorJson());
                    updateModelReferenceProperties(model, referenceProperyName, referencingCaseModelJson);
                    referencingCaseModel.setModelEditorJson(referencingCaseModelJson.toString()); // toString is also done in the regular import
                    modelService.saveModel(referencingCaseModel);

                } catch (Exception e) {
                    throw new FlowableException("Could not read model json", e);
                }
            }

            unresolvedReferencesMap.remove(modelKey);
        }
    }

    protected void updateModelReferenceProperties(Model model, String referenceProperyName, JsonNode referencingCaseModelJson) {
        for (JsonNode childNode : referencingCaseModelJson) {
            if (childNode.has(referenceProperyName)) {
                ObjectNode referenceNode = (ObjectNode) childNode.get(referenceProperyName);
                referenceNode.put("id", model.getId());
                referenceNode.put("name", model.getName());

            } else {
                updateModelReferenceProperties(model, referenceProperyName, childNode);

            }
        }
    }

    /*
     * Model retrieval methods
     */

    public Model getProcessModelByKey(String key) {
        return processKeyToModelMap.get(key);
    }

    public Model getProcessModelById(String id) {
        return processIdToModelMap.get(id);
    }

    public Model getCaseModelByKey(String key) {
        return caseKeyToModelMap.get(key);
    }

    public Model getCaseModelById(String id) {
        return caseIdToModelMap.get(id);
    }

    public Model getFormModelByKey(String key) {
        return formKeyToModelMap.get(key);
    }

    public Model getFormModelById(String id) {
        return formIdToModelMap.get(id);
    }

    public Model getDecisionTableModelByKey(String key) {
        return decisionTableKeyToModelMap.get(key);
    }

    public Model getDecisionTableModelById(String id) {
        return decisionTableIdToModelMap.get(id);
    }

    public Model getDecisionServiceModelByKey(String key) {
        return decisionServiceKeyToModelMap.get(key);
    }

    public Model getDecisionServiceModelById(String id) {
        return decisionServiceIdToModelMap.get(id);
    }
    /*
     * All models retrieval
     */

    public Collection<Model> getAllProcessModels() {
        return processKeyToModelMap.values();
    }

    public Collection<Model> getAllCaseModels() {
        return caseKeyToModelMap.values();
    }

    public Collection<Model> getAllDecisionTableModels() {
        return decisionTableKeyToModelMap.values();
    }

    public Collection<Model> getAllReferencedDecisionTableModels() {
        return referencedDecisionTableKeyToModelMap.values();
    }

    public Collection<Model> getAllDecisionServiceModels() {
        return decisionServiceKeyToModelMap.values();
    }

    public Collection<Model> getAllFormModels() {
        return formKeyToModelMap.values();
    }

    /*
     * Model JSON String retrieval
     */

    public Map<String, String> getProcessKeyToJsonStringMap() {
        return processKeyToJsonStringMap;
    }
    public Map<String, String> getCaseKeyToJsonStringMap() {
        return caseKeyToJsonStringMap;
    }
    public Map<String, String> getFormKeyToJsonStringMap() {
        return formKeyToJsonStringMap;
    }

    @Override
    public Map<String, String> getDecisionTableKeyToJsonStringMap() {
        return decisionTableKeyToJsonStringMap;
    }

    @Override
    public Map<String, String> getDecisionServiceKeyToJsonStringMap() {
        return decisionServiceKeyToJsonStringMap;
    }

    /*
     * Thumbnails
     */

    public Map<String, byte[]> getModelKeyToThumbnailMap() {
        return modelKeyToThumbnailMap;
    }
    public void setModelKeyToThumbnailMap(Map<String, byte[]> modelKeyToThumbnailMap) {
        this.modelKeyToThumbnailMap = modelKeyToThumbnailMap;
    }

    /*
     * Unresolved model references.
     *
     * Due to to the order of importing, this is only needed for cmmn <-> bpmn,
     * as the other model types are imported before them.
     */

    @Override
    public void registerUnresolvedCaseModelReferenceForCaseModel(String unresolvedCaseModelKey, CmmnModel cmmnModel) {
        // The CmmnModel needs to be passed, as the actual key on the CmmnModel (stored as id) will be typically only be set later.
        unresolvedCaseModelKeyToCmmnModels.computeIfAbsent(unresolvedCaseModelKey, key -> new ArrayList<>()).add(cmmnModel);
    }

    @Override
    public void registerUnresolvedProcessModelReferenceForCaseModel(String unresolvedProcessModelKey, CmmnModel cmmnModel) {
        // The CmmnModel needs to be passed, as the actual key on the CmmnModel (stored as id) will be typically only be set later.
        unresolvedProcessModelKeyToCmmnModels.computeIfAbsent(unresolvedProcessModelKey, key -> new ArrayList<>()).add(cmmnModel);
    }
}
