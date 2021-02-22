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
package org.flowable.ui.modeler.serviceapi;

import java.io.InputStream;
import java.util.List;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.cmmn.model.CmmnModel;
import org.flowable.ui.modeler.domain.AbstractModel;
import org.flowable.ui.modeler.domain.Model;
import org.flowable.ui.modeler.domain.ModelHistory;
import org.flowable.ui.modeler.model.ModelKeyRepresentation;
import org.flowable.ui.modeler.model.ModelRepresentation;
import org.flowable.ui.modeler.model.ReviveModelResultRepresentation;
import org.flowable.ui.modeler.service.ConverterContext;

public interface ModelService {

    Model getModel(String modelId);

    ModelRepresentation getModelRepresentation(String modelId);

    List<AbstractModel> getModelsByModelType(Integer modelType);

    ModelKeyRepresentation validateModelKey(Model model, Integer modelType, String key);

    ModelHistory getModelHistory(String modelId, String modelHistoryId);

    Long getModelCountForUser(String userId, int modelTypeApp);

    BpmnModel getBpmnModel(AbstractModel model, ConverterContext converterContext);

    byte[] getBpmnXML(BpmnModel bpmnMode);

    byte[] getBpmnXML(AbstractModel model);

    BpmnModel getBpmnModel(AbstractModel model);
    
    CmmnModel getCmmnModel(AbstractModel model);

    byte[] getCmmnXML(CmmnModel cmmnModel);

    byte[] getCmmnXML(AbstractModel model);

    CmmnModel getCmmnModel(AbstractModel model, ConverterContext converterContext);
    
    String createModelJson(ModelRepresentation model);

    Model createModel(ModelRepresentation model, String editorJson, String createdBy);

    Model createModel(Model newModel, String createdBy);

    Model saveModel(Model modelObject);

    Model saveModel(Model modelObject, String editorJson, byte[] imageBytes, boolean newVersion, String newVersionComment, String updatedBy);

    Model saveModel(String modelId, String name, String key, String description, String editorJson,
            boolean newVersion, String newVersionComment, String updatedBy);

    ModelRepresentation importNewVersion(String modelId, String fileName, InputStream modelStream);

    Model createNewModelVersion(Model modelObject, String comment, String updatedBy);

    ModelHistory createNewModelVersionAndReturnModelHistory(Model modelObject, String comment, String updatedBy);

    void deleteModel(String modelId);

    ReviveModelResultRepresentation reviveProcessModelHistory(ModelHistory modelHistory, String userId, String newVersionComment);
}
