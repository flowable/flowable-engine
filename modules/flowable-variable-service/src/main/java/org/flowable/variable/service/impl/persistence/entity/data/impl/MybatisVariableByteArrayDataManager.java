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
package org.flowable.variable.service.impl.persistence.entity.data.impl;

import java.util.List;

import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntity;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityImpl;
import org.flowable.variable.service.impl.persistence.entity.data.VariableByteArrayDataManager;

/**
 * @author Joram Barrez
 */
public class MybatisVariableByteArrayDataManager extends AbstractDataManager<VariableByteArrayEntity> implements VariableByteArrayDataManager {

    @Override
    public VariableByteArrayEntity create() {
        return new VariableByteArrayEntityImpl();
    }

    @Override
    public Class<? extends VariableByteArrayEntity> getManagedEntityClass() {
        return VariableByteArrayEntityImpl.class;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<VariableByteArrayEntity> findAll() {
        return getDbSqlSession().selectList("selectVariableByteArrays");
    }

    @Override
    public void deleteByteArrayNoRevisionCheck(String byteArrayEntityId) {
        getDbSqlSession().delete("deleteVariableByteArrayNoRevisionCheck", byteArrayEntityId, VariableByteArrayEntityImpl.class);
    }

}
