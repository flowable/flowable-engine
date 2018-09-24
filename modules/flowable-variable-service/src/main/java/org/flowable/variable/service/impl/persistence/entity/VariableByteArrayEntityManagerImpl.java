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

package org.flowable.variable.service.impl.persistence.entity;

import java.util.List;

import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.impl.persistence.entity.data.VariableByteArrayDataManager;

/**
 * @author Joram Barrez
 * @author Marcus Klimstra (CGI)
 */
public class VariableByteArrayEntityManagerImpl extends AbstractEntityManager<VariableByteArrayEntity> implements VariableByteArrayEntityManager {

    protected VariableByteArrayDataManager byteArrayDataManager;

    public VariableByteArrayEntityManagerImpl(VariableServiceConfiguration variableServiceConfiguration, VariableByteArrayDataManager byteArrayDataManager) {
        super(variableServiceConfiguration);
        this.byteArrayDataManager = byteArrayDataManager;
    }

    @Override
    protected DataManager<VariableByteArrayEntity> getDataManager() {
        return byteArrayDataManager;
    }

    @Override
    public List<VariableByteArrayEntity> findAll() {
        return byteArrayDataManager.findAll();
    }

    @Override
    public void deleteByteArrayById(String byteArrayEntityId) {
        byteArrayDataManager.deleteByteArrayNoRevisionCheck(byteArrayEntityId);
    }

    public VariableByteArrayDataManager getByteArrayDataManager() {
        return byteArrayDataManager;
    }

    public void setByteArrayDataManager(VariableByteArrayDataManager byteArrayDataManager) {
        this.byteArrayDataManager = byteArrayDataManager;
    }

}
