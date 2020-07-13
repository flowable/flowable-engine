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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.idm.api.User;
import org.flowable.ui.common.security.SecurityUtils;
import org.flowable.ui.common.service.exception.InternalServerErrorException;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.AppDefinition;
import org.flowable.ui.modeler.domain.AppModelDefinition;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.domain.ModelHistory;
import org.flowable.ui.modeler.model.AppDefinitionRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionSaveRepresentation;
import org.flowable.ui.modeler.model.AppDefinitionUpdateResultRepresentation;
import org.flowable.ui.modeler.repository.ModelHistoryRepository;
import org.flowable.ui.modeler.repository.ModelRepository;
import org.flowable.ui.modeler.repository.ModelSort;
import org.flowable.ui.modeler.serviceapi.AppDefinitionService;
import org.flowable.ui.modeler.serviceapi.AppDefinitionServiceRepresentation;
import org.flowable.ui.modeler.serviceapi.ModelService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AppDefinitionServiceImpl implements AppDefinitionService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppDefinitionServiceImpl.class);

    @Autowired
    protected AppDefinitionPublishService appDefinitionPublishService;

    @Autowired
    protected ModelService modelService;

    @Autowired
    protected ModelRepository modelRepository;

    @Autowired
    protected ModelHistoryRepository modelHistoryRepository;

    @Autowired
    protected ObjectMapper objectMapper;

    public AppDefinitionRepresentation getAppDefinition(String modelId) {
        Model model = modelService.getModel(modelId);
        return createAppDefinitionRepresentation(model);
    }

    public AppDefinitionRepresentation getAppDefinitionHistory(String modelId, String modelHistoryId) {
        ModelHistory model = modelService.getModelHistory(modelId, modelHistoryId);
        return createAppDefinitionRepresentation(model);
    }

    @Override
    public List<AppDefinitionServiceRepresentation> getAppDefinitions() {
        Map<String, AbstractModel> modelMap = new HashMap<>();
        List<AppDefinitionServiceRepresentation> resultList = new ArrayList<>();

        List<Model> createdByModels = modelRepository.findByModelType(AbstractModel.MODEL_TYPE_APP, ModelSort.NAME_ASC);
        for (Model model : createdByModels) {
            modelMap.put(model.getId(), model);
        }

        for (AbstractModel model : modelMap.values()) {
            resultList.add(createAppDefinition(model));
        }

        return resultList;
    }

    /**
     * Gathers all 'deployable' app definitions for the current user.
     * <p>
     * To find these: - All historical app models are fetched. Only the highest version of each app model is retained. - All historical app models shared with the groups the current user is part of
     * are fetched. Only the highest version of each app model is retained.
     */
    @Override
    public List<AppDefinitionServiceRepresentation> getDeployableAppDefinitions(User user) {
        Map<String, ModelHistory> modelMap = new HashMap<>();
        List<AppDefinitionServiceRepresentation> resultList = new ArrayList<>();

        List<ModelHistory> createdByModels = modelHistoryRepository.findByModelTypAndCreatedBy(
                user.getId(), AbstractModel.MODEL_TYPE_APP);
        for (ModelHistory modelHistory : createdByModels) {
            if (modelMap.containsKey(modelHistory.getModelId())) {
                if (modelHistory.getVersion() > modelMap.get(modelHistory.getModelId()).getVersion()) {
                    modelMap.put(modelHistory.getModelId(), modelHistory);
                }
            } else {
                modelMap.put(modelHistory.getModelId(), modelHistory);
            }
        }

        for (ModelHistory model : modelMap.values()) {
            Model latestModel = modelRepository.get(model.getModelId());
            if (latestModel != null) {
                resultList.add(createAppDefinition(model));
            }
        }

        return resultList;
    }

    public AppDefinitionUpdateResultRepresentation updateAppDefinition(String modelId, AppDefinitionSaveRepresentation updatedModel) {

        AppDefinitionUpdateResultRepresentation result = new AppDefinitionUpdateResultRepresentation();

        User user = SecurityUtils.getCurrentUserObject();

        Model model = modelService.getModel(modelId);

        model.setName(updatedModel.getAppDefinition().getName());
        model.setKey(updatedModel.getAppDefinition().getKey());
        model.setDescription(updatedModel.getAppDefinition().getDescription());
        String editorJson = null;
        try {
            editorJson = objectMapper.writeValueAsString(updatedModel.getAppDefinition().getDefinition());
        } catch (Exception e) {
            LOGGER.error("Error while processing app definition json {}", modelId, e);
            throw new InternalServerErrorException("App definition could not be saved " + modelId);
        }

        model = modelService.saveModel(model, editorJson, null, false, null, user);

        AppDefinitionRepresentation appDefinition = createAppDefinitionRepresentation(model);
        if (updatedModel.isPublish()) {
            appDefinitionPublishService.publishAppDefinition(null, model, user);
        } else {
            appDefinition.setDefinition(updatedModel.getAppDefinition().getDefinition());
        }

        result.setAppDefinition(appDefinition);
        return result;
    }

    protected AppDefinitionServiceRepresentation createAppDefinition(AbstractModel model) {
        AppDefinitionServiceRepresentation resultInfo = new AppDefinitionServiceRepresentation();
        if (model instanceof ModelHistory) {
            resultInfo.setId(((ModelHistory) model).getModelId());
        } else {
            resultInfo.setId(model.getId());
        }
        resultInfo.setName(model.getName());
        resultInfo.setDescription(model.getDescription());
        resultInfo.setVersion(model.getVersion());
        resultInfo.setDefinition(model.getModelEditorJson());

        AppDefinition appDefinition = null;
        try {
            appDefinition = objectMapper.readValue(model.getModelEditorJson(), AppDefinition.class);
        } catch (Exception e) {
            LOGGER.error("Error deserializing app {}", model.getId(), e);
            throw new InternalServerErrorException("Could not deserialize app definition");
        }

        if (appDefinition != null) {
            resultInfo.setTheme(appDefinition.getTheme());
            resultInfo.setIcon(appDefinition.getIcon());
            List<AppModelDefinition> models = appDefinition.getModels();
            if (CollectionUtils.isNotEmpty(models)) {
                List<String> modelIds = new ArrayList<>();
                for (AppModelDefinition appModelDef : models) {
                    modelIds.add(appModelDef.getId());
                }
                resultInfo.setModels(modelIds);
            }
        }
        return resultInfo;
    }

    protected AppDefinitionRepresentation createAppDefinitionRepresentation(AbstractModel model) {
        AppDefinition appDefinition = null;
        try {
            appDefinition = objectMapper.readValue(model.getModelEditorJson(), AppDefinition.class);
        } catch (Exception e) {
            LOGGER.error("Error deserializing app {}", model.getId(), e);
            throw new InternalServerErrorException("Could not deserialize app definition");
        }
        AppDefinitionRepresentation result = new AppDefinitionRepresentation(model);
        result.setDefinition(appDefinition);
        return result;
    }

}
