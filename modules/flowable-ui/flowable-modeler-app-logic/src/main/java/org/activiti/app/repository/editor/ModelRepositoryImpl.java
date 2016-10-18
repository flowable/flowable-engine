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
package org.activiti.app.repository.editor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.app.domain.editor.Model;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ModelRepositoryImpl implements ModelRepository {
  
  private static final String NAMESPACE = "org.activiti.app.domain.editor.Model.";
  
  @Autowired
  protected SqlSessionTemplate sqlSessionTemplate;
  
  @Autowired
  protected UuidIdGenerator idGenerator;

  @Override
  public Model findOne(String id) {
    return sqlSessionTemplate.selectOne(NAMESPACE + "selectModel");
  }
  
  @Override
  public boolean exists(String id) {
    return findOne(id) != null;
  }
  
  @Override
  public List<Model> findModelsByModelType(Integer modelType) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("modelType", modelType);
    return findModelsByParameters(params);
  }
  
  @Override
  public List<Model> findModelsByModelType(Integer modelType, String filter) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("modelType", modelType);
    params.put("filter", filter);
    return findModelsByParameters(params);
  }

  @Override
  public List<Model> findModelsCreatedBy(String createdBy, Integer modelType, String sort) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("modelType", modelType);
    params.put("createdBy", createdBy);
    params.put("sort", sort);
    return findModelsByParameters(params);
  }
  
  @Override
  public List<Model> findModelsCreatedBy(String createdBy, Integer modelType, String filter, String sort) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("modelType", modelType);
    params.put("createdBy", createdBy);
    params.put("filter", filter);
    params.put("sort", sort);
    return findModelsByParameters(params);
  }
  
  @Override
  public List<Model> findModelsByKeyAndType(String key, Integer modelType) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("key", key);
    params.put("modelType", modelType);
    return findModelsByParameters(params);
  }
  
  @Override
  public List<Model> findModelsByParentModelId(String parentModelId) {
    return sqlSessionTemplate.selectList(NAMESPACE + "selectModelByParentModelId", parentModelId);
  }
  
  @Override
  public Long countByModelTypeAndUser(int modelType, String createdBy) {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("createdBy", createdBy);
    params.put("modelType", modelType);
    return sqlSessionTemplate.selectOne(NAMESPACE + "countByModelTypeAndCreatedBy", params);
  }
  
  protected List<Model> findModelsByParameters(Map<String, Object> parameters) {
    return sqlSessionTemplate.selectList(NAMESPACE + "selectModelByParameters", parameters);
  }
  
  @Override
  public void save(Model model) {
    if (model.getId() == null) {
      model.setId(idGenerator.generateId());
    }
    sqlSessionTemplate.insert(NAMESPACE + "insertModel", model);
  }
  
  @Override
  public void delete(Model model) {
    sqlSessionTemplate.delete(NAMESPACE + "deleteModel", model);
  }

}
