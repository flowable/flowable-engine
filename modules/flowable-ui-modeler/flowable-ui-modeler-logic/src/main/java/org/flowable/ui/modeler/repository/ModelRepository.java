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
package org.flowable.ui.modeler.repository;

import java.util.List;

import org.flowable.ui.modeler.domain.Model;

public interface ModelRepository {

    void save(Model model);

    void delete(Model model);

    Model get(String id);

    List<Model> findByModelType(Integer modelType, String sort);

    List<Model> findByModelTypeAndFilter(Integer modelType, String filter, String sort);

    List<Model> findByKeyAndType(String key, Integer modelType);

    List<Model> findByParentModelId(String parentModelId);

    Long countByModelTypeAndCreatedBy(int modelType, String createdBy);

}
