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
package org.activiti.app.repository.runtime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.activiti.app.domain.runtime.RelatedContent;
import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RelatedContentRepositoryImpl implements RelatedContentRepository {

  private static final String NAMESPACE = "org.activiti.app.domain.runtime.RelatedContent";

  @Autowired
  protected SqlSessionTemplate sqlSessionTemplate;

  @Autowired
  protected UuidIdGenerator idGenerator;

  @Override
  public RelatedContent get(String id) {
    return sqlSessionTemplate.selectOne(NAMESPACE + "selectRelatedContent", id);
  }

  @Override
  public void save(RelatedContent relatedContent) {
    if (relatedContent.getId() == null) {
      relatedContent.setId(idGenerator.generateId());
      sqlSessionTemplate.insert(NAMESPACE + "insertRelatedContent", relatedContent);
    } else {
      sqlSessionTemplate.update(NAMESPACE + "updateRelatedContent", relatedContent);
    }
  }
  
  @Override
  public List<RelatedContent> findBySourceAndSourceId(String source, String sourceId) {
    return findByParameters("source", source, "sourceId", sourceId);
  }
  
  @Override
  public List<RelatedContent> findAllRelatedByTaskId(String taskId) {
    return findByParameters("taskId", taskId, "isRelatedContent", true);
  }
  
  @Override
  public List<RelatedContent> findAllFieldBasedContentByTaskId(String taskId) {
    return findByParameters("taskId", taskId, "isRelatedContent", false);
  }
  
  @Override
  public List<RelatedContent> findAllByTaskIdAndField(String taskId, String field) {
    return findByParameters("taskId", taskId, "field", field);
  }
  
  @Override
  public List<RelatedContent> findAllRelatedByProcessInstanceId(String processId) {
    return findByParameters("processInstanceId", processId, "isRelatedContent", true);
  }
  
  @Override
  public List<RelatedContent> findAllFieldBasedContentByProcessInstanceId(String processId) {
    return findByParameters("processInstanceId", processId, "isRelatedContent", false);
  }
  
  @Override
  public List<RelatedContent> findAllContentByProcessInstanceId(String processId) {
    return findByParameters("processInstanceId", processId);
  }
  
  @Override
  public List<RelatedContent> findAllByProcessInstanceIdAndField(String processId, String field) {
    return findByParameters("processInstanceId", processId, "field", field);
  }
  
  @Override
  public void delete(RelatedContent relatedContent) {
    sqlSessionTemplate.delete(NAMESPACE + "deleteRelatedContent", relatedContent);
  }
  
  @Override
  public void deleteAllContentByProcessInstanceId(String processInstanceId) {
    sqlSessionTemplate.delete(NAMESPACE + "deleteRelatedContentByProcessInstanceId", processInstanceId);
  }
  
  protected List<RelatedContent> findByParameters(Object...parameters) {
    Map<String, Object> params = new HashMap<String, Object>();
    for (int i=0; i<parameters.length; i+=2) {
      params.put(parameters[i].toString(), parameters[i+1]);
    }
    return sqlSessionTemplate.selectList(NAMESPACE + "selectRelatedContentByParameters", params);
  }

}
