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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.AppDefinition;
import org.flowable.app.domain.editor.AppModelDefinition;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.domain.editor.ModelHistory;
import org.flowable.app.model.editor.AppDefinitionRepresentation;
import org.flowable.app.model.editor.AppDefinitionSaveRepresentation;
import org.flowable.app.model.editor.AppDefinitionUpdateResultRepresentation;
import org.flowable.app.repository.editor.ModelHistoryRepository;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.repository.editor.ModelSort;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.AppDefinitionService;
import org.flowable.app.service.api.AppDefinitionServiceRepresentation;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.editor.language.json.converter.util.CollectionUtils;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
@Transactional
public class AppDefinitionServiceImpl implements AppDefinitionService {

  private final Logger logger = LoggerFactory.getLogger(AppDefinitionServiceImpl.class);

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
    Map<String, AbstractModel> modelMap = new HashMap<String, AbstractModel>();
    List<AppDefinitionServiceRepresentation> resultList = new ArrayList<AppDefinitionServiceRepresentation>();

    User user = SecurityUtils.getCurrentUserObject();
    List<Model> createdByModels = modelRepository.findByModelTypeAndCreatedBy(user.getId(), AbstractModel.MODEL_TYPE_APP, ModelSort.NAME_ASC); 
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
   * 
   * To find these: - All historical app models are fetched. Only the highest version of each app model is retained. - All historical app models shared with the groups the current user is part of are
   * fetched. Only the highest version of each app model is retained.
   */
  @Override
  public List<AppDefinitionServiceRepresentation> getDeployableAppDefinitions(User user) {
    Map<String, ModelHistory> modelMap = new HashMap<String, ModelHistory>();
    List<AppDefinitionServiceRepresentation> resultList = new ArrayList<AppDefinitionServiceRepresentation>();

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
      logger.error("Error while processing app definition json {}", modelId, e);
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
      logger.error("Error deserializing app {}", model.getId(), e);
      throw new InternalServerErrorException("Could not deserialize app definition");
    }

    if (appDefinition != null) {
      resultInfo.setTheme(appDefinition.getTheme());
      resultInfo.setIcon(appDefinition.getIcon());
      List<AppModelDefinition> models = appDefinition.getModels();
      if (CollectionUtils.isNotEmpty(models)) {
        List<String> modelIds = new ArrayList<String>();
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
      logger.error("Error deserializing app {}", model.getId(), e);
      throw new InternalServerErrorException("Could not deserialize app definition");
    }
    AppDefinitionRepresentation result = new AppDefinitionRepresentation(model);
    result.setDefinition(appDefinition);
    return result;
  }

}
