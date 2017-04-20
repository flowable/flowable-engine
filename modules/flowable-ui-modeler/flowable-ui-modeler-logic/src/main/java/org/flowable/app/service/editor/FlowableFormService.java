/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.flowable.app.service.editor;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.flowable.app.domain.editor.AbstractModel;
import org.flowable.app.domain.editor.Model;
import org.flowable.app.domain.editor.ModelHistory;
import org.flowable.app.model.editor.FormSaveRepresentation;
import org.flowable.app.model.editor.ModelKeyRepresentation;
import org.flowable.app.model.editor.form.FormRepresentation;
import org.flowable.app.repository.editor.ModelRepository;
import org.flowable.app.security.SecurityUtils;
import org.flowable.app.service.api.ModelService;
import org.flowable.app.service.exception.BadRequestException;
import org.flowable.app.service.exception.InternalServerErrorException;
import org.flowable.form.model.FormModel;
import org.flowable.idm.api.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author Tijs Rademakers
 */
@Service
@Transactional
public class FlowableFormService {

  private static final Logger logger = LoggerFactory.getLogger(FlowableFormService.class);

  @Autowired
  protected ModelService modelService;

  @Autowired
  protected ObjectMapper objectMapper;

  @Autowired
  protected ModelRepository modelRepository;

  public FormRepresentation getForm(String formId)
  {
    Model model = modelService.getModel(formId);
    FormRepresentation form = createFormRepresentation(model);
    return form;
  }

  public String getRdsForm(String formKey)
  {    
    List<Model> models =  modelRepository.findByKeyAndType(formKey, AbstractModel.MODEL_TYPE_FORM_RDS);
    Assert.isTrue(models.size()==1, "Should get 1 and only 1 rds_form by key, but now get " + models.size());
    Model model = models.get(0);
    ObjectNode objectNode = objectMapper.createObjectNode();
    objectNode.put("key", model.getKey());
    objectNode.put("definition", model.getModelEditorJson());
    return objectNode.toString();
  }

  public FormRepresentation getFormHistory(String formId, String formHistoryId)
  {
    ModelHistory model = modelService.getModelHistory(formId, formHistoryId);
    FormRepresentation form = createFormRepresentation(model);
    return form;
  }

  public List<FormRepresentation> getForms(String[] formIds) {
    List<FormRepresentation> formRepresentations = new ArrayList<FormRepresentation>();

    if (formIds == null || formIds.length == 0) {
      throw new BadRequestException("No formIds provided in the request");
    }

    for (String formId : formIds) {
      Model model = modelService.getModel(formId);

      FormRepresentation form = createFormRepresentation(model);
      formRepresentations.add(form);
    }

    return formRepresentations;
  }

  public FormRepresentation saveForm(String formId, FormSaveRepresentation saveRepresentation) {
    User user = SecurityUtils.getCurrentUserObject();
    Model model = modelService.getModel(formId);

    String formKey = saveRepresentation.getFormRepresentation().getKey();
    ModelKeyRepresentation modelKeyInfo = modelService.validateModelKey(model, model.getModelType(), formKey);
    if (modelKeyInfo.isKeyAlreadyExists()) {
      throw new BadRequestException("Model with provided key already exists " + formKey);
    }

    model.setName(saveRepresentation.getFormRepresentation().getName());
    model.setKey(formKey);
    model.setDescription(saveRepresentation.getFormRepresentation().getDescription());

    String editorJson = null;
    try {
      editorJson = objectMapper.writeValueAsString(saveRepresentation.getFormRepresentation().getFormDefinition());
    } catch (Exception e) {
      logger.error("Error while processing form json", e);
      throw new InternalServerErrorException("Form could not be saved " + formId);
    }

    String filteredImageString = saveRepresentation.getFormImageBase64().replace("data:image/png;base64,", "");
    byte[] imageBytes = Base64.decodeBase64(filteredImageString);
    model = modelService.saveModel(model, editorJson, imageBytes, saveRepresentation.isNewVersion(), saveRepresentation.getComment(), user);
    FormRepresentation result = new FormRepresentation(model);
    result.setFormDefinition(saveRepresentation.getFormRepresentation().getFormDefinition());
    return result;
  }

  public FormRepresentation saveForm(String jsonFormDefinition)
  {
    JsonNode jsonNode;
    try
    {
      jsonNode = objectMapper.readValue(jsonFormDefinition, JsonNode.class);
    }
    catch (Exception e)
    {
      throw new InternalServerErrorException("Could not deserialize form definition");
    }
    String formkey = jsonNode.get("key").textValue();

    User user = SecurityUtils.getCurrentUserObject();
    List<Model> models = modelRepository.findByKeyAndType(formkey, AbstractModel.MODEL_TYPE_FORM_RDS);
    Model model = models.get(0);

    String editorJson = jsonNode.get("definition").toString();

    String filteredImageString = jsonNode.get("formImageBase64").textValue().replace("data:image/png;base64,", "");
    byte[] imageBytes = Base64.decodeBase64(filteredImageString);

    model = modelService.saveModel(model, editorJson, imageBytes, false, "", user);
    FormRepresentation result = new FormRepresentation(model);
    // result.setFormDefinition(saveRepresentation.getFormRepresentation().getFormDefinition());
    return result;
  }

  protected FormRepresentation createFormRepresentation(AbstractModel model)
  {
    FormModel formDefinition = null;
    try {
      formDefinition = objectMapper.readValue(model.getModelEditorJson(), FormModel.class);
    } catch (Exception e) {
      logger.error("Error deserializing form", e);
      throw new InternalServerErrorException("Could not deserialize form definition");
    }

    FormRepresentation result = new FormRepresentation(model);
    result.setFormDefinition(formDefinition);
    return result;
  }
}
