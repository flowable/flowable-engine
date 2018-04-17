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
package org.flowable.variable.service;

import org.flowable.common.engine.impl.AbstractServiceConfiguration;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.HistoricVariableServiceImpl;
import org.flowable.variable.service.impl.VariableServiceImpl;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityManager;
import org.flowable.variable.service.impl.persistence.entity.HistoricVariableInstanceEntityManagerImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableByteArrayEntityManagerImpl;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManager;
import org.flowable.variable.service.impl.persistence.entity.VariableInstanceEntityManagerImpl;
import org.flowable.variable.service.impl.persistence.entity.data.HistoricVariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.VariableByteArrayDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.VariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.MybatisHistoricVariableInstanceDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.MybatisVariableByteArrayDataManager;
import org.flowable.variable.service.impl.persistence.entity.data.impl.MybatisVariableInstanceDataManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Tijs Rademakers
 */
public class VariableServiceConfiguration extends AbstractServiceConfiguration {

    protected static final Logger LOGGER = LoggerFactory.getLogger(VariableServiceConfiguration.class);
    
    public static final int DEFAULT_GENERIC_MAX_LENGTH_STRING = 4000;
    public static final int DEFAULT_ORACLE_MAX_LENGTH_STRING = 2000;

    // SERVICES
    // /////////////////////////////////////////////////////////////////

    protected VariableService variableService = new VariableServiceImpl(this);
    protected HistoricVariableService historicVariableService = new HistoricVariableServiceImpl(this);

    // DATA MANAGERS ///////////////////////////////////////////////////

    protected VariableInstanceDataManager variableInstanceDataManager;
    protected VariableByteArrayDataManager byteArrayDataManager;
    protected HistoricVariableInstanceDataManager historicVariableInstanceDataManager;

    // ENTITY MANAGERS /////////////////////////////////////////////////
    
    protected VariableInstanceEntityManager variableInstanceEntityManager;
    protected VariableByteArrayEntityManager byteArrayEntityManager;
    protected HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager;
    
    protected VariableTypes variableTypes;
    
    protected InternalHistoryVariableManager internalHistoryVariableManager;
    
    protected int maxLengthString;
    
    /**
     * This flag determines whether variables of the type 'serializable' will be tracked. This means that, when true, in a JavaDelegate you can write
     *
     * MySerializableVariable myVariable = (MySerializableVariable) execution.getVariable("myVariable"); myVariable.setNumber(123);
     *
     * And the changes to the java object will be reflected in the database. Otherwise, a manual call to setVariable will be needed.
     *
     * By default true for backwards compatibility.
     */
    protected boolean serializableVariableTypeTrackDeserializedObjects = true;

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        initDataManagers();
        initEntityManagers();
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (variableInstanceDataManager == null) {
            variableInstanceDataManager = new MybatisVariableInstanceDataManager();
        }
        if (byteArrayDataManager == null) {
            byteArrayDataManager = new MybatisVariableByteArrayDataManager();
        }
        if (historicVariableInstanceDataManager == null) {
            historicVariableInstanceDataManager = new MybatisHistoricVariableInstanceDataManager();
        }
    }

    public void initEntityManagers() {
        if (variableInstanceEntityManager == null) {
            variableInstanceEntityManager = new VariableInstanceEntityManagerImpl(this, variableInstanceDataManager);
        }
        if (byteArrayEntityManager == null) {
            byteArrayEntityManager = new VariableByteArrayEntityManagerImpl(this, byteArrayDataManager);
        }
        if (historicVariableInstanceEntityManager == null) {
            historicVariableInstanceEntityManager = new HistoricVariableInstanceEntityManagerImpl(this, historicVariableInstanceDataManager);
        }
    }

    // getters and setters
    // //////////////////////////////////////////////////////

    public VariableServiceConfiguration getVariableServiceConfiguration() {
        return this;
    }
    
    public VariableService getVariableService() {
        return variableService;
    }

    public VariableServiceConfiguration setVariableService(VariableService variableService) {
        this.variableService = variableService;
        return this;
    }
    
    public HistoricVariableService getHistoricVariableService() {
        return historicVariableService;
    }

    public VariableServiceConfiguration setHistoricVariableService(HistoricVariableService historicVariableService) {
        this.historicVariableService = historicVariableService;
        return this;
    }

    public VariableInstanceDataManager getVariableInstanceDataManager() {
        return variableInstanceDataManager;
    }

    public VariableServiceConfiguration setVariableInstanceDataManager(VariableInstanceDataManager variableInstanceDataManager) {
        this.variableInstanceDataManager = variableInstanceDataManager;
        return this;
    }
    
    public VariableByteArrayDataManager getByteArrayDataManager() {
        return byteArrayDataManager;
    }

    public VariableServiceConfiguration setByteArrayDataManager(VariableByteArrayDataManager byteArrayDataManager) {
        this.byteArrayDataManager = byteArrayDataManager;
        return this;
    }
    
    public HistoricVariableInstanceDataManager getHistoricVariableInstanceDataManager() {
        return historicVariableInstanceDataManager;
    }

    public VariableServiceConfiguration setHistoricVariableInstanceDataManager(HistoricVariableInstanceDataManager historicVariableInstanceDataManager) {
        this.historicVariableInstanceDataManager = historicVariableInstanceDataManager;
        return this;
    }

    public VariableInstanceEntityManager getVariableInstanceEntityManager() {
        return variableInstanceEntityManager;
    }

    public VariableServiceConfiguration setVariableInstanceEntityManager(VariableInstanceEntityManager variableInstanceEntityManager) {
        this.variableInstanceEntityManager = variableInstanceEntityManager;
        return this;
    }
    
    public VariableByteArrayEntityManager getByteArrayEntityManager() {
        return byteArrayEntityManager;
    }

    public VariableServiceConfiguration setByteArrayEntityManager(VariableByteArrayEntityManager byteArrayEntityManager) {
        this.byteArrayEntityManager = byteArrayEntityManager;
        return this;
    }
    
    public HistoricVariableInstanceEntityManager getHistoricVariableInstanceEntityManager() {
        return historicVariableInstanceEntityManager;
    }

    public VariableServiceConfiguration setHistoricVariableInstanceEntityManager(HistoricVariableInstanceEntityManager historicVariableInstanceEntityManager) {
        this.historicVariableInstanceEntityManager = historicVariableInstanceEntityManager;
        return this;
    }
    
    public VariableTypes getVariableTypes() {
        return variableTypes;
    }
    
    public VariableServiceConfiguration setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
        return this;
    }
    
    public InternalHistoryVariableManager getInternalHistoryVariableManager() {
        return internalHistoryVariableManager;
    }

    public VariableServiceConfiguration setInternalHistoryVariableManager(InternalHistoryVariableManager internalHistoryVariableManager) {
        this.internalHistoryVariableManager = internalHistoryVariableManager;
        return this;
    }

    public int getMaxLengthString() {
        return maxLengthString;
    }

    public VariableServiceConfiguration setMaxLengthString(int maxLengthString) {
        this.maxLengthString = maxLengthString;
        return this;
    }
    
    public boolean isSerializableVariableTypeTrackDeserializedObjects() {
        return serializableVariableTypeTrackDeserializedObjects;
    }

    public void setSerializableVariableTypeTrackDeserializedObjects(boolean serializableVariableTypeTrackDeserializedObjects) {
        this.serializableVariableTypeTrackDeserializedObjects = serializableVariableTypeTrackDeserializedObjects;
    }
}
