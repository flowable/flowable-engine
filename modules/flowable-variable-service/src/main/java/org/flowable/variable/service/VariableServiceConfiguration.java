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

import java.util.List;

import org.flowable.engine.common.AbstractServiceConfiguration;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.variable.service.impl.HistoricVariableServiceImpl;
import org.flowable.variable.service.impl.ServiceImpl;
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
import org.flowable.variable.service.impl.types.BooleanType;
import org.flowable.variable.service.impl.types.ByteArrayType;
import org.flowable.variable.service.impl.types.DateType;
import org.flowable.variable.service.impl.types.DefaultVariableTypes;
import org.flowable.variable.service.impl.types.DoubleType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JodaDateTimeType;
import org.flowable.variable.service.impl.types.JodaDateType;
import org.flowable.variable.service.impl.types.JsonType;
import org.flowable.variable.service.impl.types.LongJsonType;
import org.flowable.variable.service.impl.types.LongStringType;
import org.flowable.variable.service.impl.types.LongType;
import org.flowable.variable.service.impl.types.NullType;
import org.flowable.variable.service.impl.types.SerializableType;
import org.flowable.variable.service.impl.types.ShortType;
import org.flowable.variable.service.impl.types.StringType;
import org.flowable.variable.service.impl.types.UUIDType;
import org.flowable.variable.service.impl.types.VariableType;
import org.flowable.variable.service.impl.types.VariableTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

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
    
    protected HistoryLevel historyLevel;
    
    protected ObjectMapper objectMapper;
    
    protected List<VariableType> customPreVariableTypes;
    protected List<VariableType> customPostVariableTypes;
    protected VariableTypes variableTypes;
    
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
        initVariableTypes();
        initServices();
        initDataManagers();
        initEntityManagers();
    }
    
    public boolean isHistoryLevelAtLeast(HistoryLevel level) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}, level required: {}", historyLevel, level);
        }
        // Comparing enums actually compares the location of values declared in the enum
        return historyLevel.isAtLeast(level);
    }

    public boolean isHistoryEnabled() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Current history level: {}", historyLevel);
        }
        return historyLevel != HistoryLevel.NONE;
    }
    
    public void initVariableTypes() {
        if (variableTypes == null) {
            variableTypes = new DefaultVariableTypes();
            if (customPreVariableTypes != null) {
                for (VariableType customVariableType : customPreVariableTypes) {
                    variableTypes.addType(customVariableType);
                }
            }
            variableTypes.addType(new NullType());
            variableTypes.addType(new StringType(getMaxLengthString()));
            variableTypes.addType(new LongStringType(getMaxLengthString() + 1));
            variableTypes.addType(new BooleanType());
            variableTypes.addType(new ShortType());
            variableTypes.addType(new IntegerType());
            variableTypes.addType(new LongType());
            variableTypes.addType(new DateType());
            variableTypes.addType(new JodaDateType());
            variableTypes.addType(new JodaDateTimeType());
            variableTypes.addType(new DoubleType());
            variableTypes.addType(new UUIDType());
            variableTypes.addType(new JsonType(getMaxLengthString(), objectMapper));
            variableTypes.addType(new LongJsonType(getMaxLengthString() + 1, objectMapper));
            variableTypes.addType(new ByteArrayType());
            variableTypes.addType(new SerializableType(serializableVariableTypeTrackDeserializedObjects));
            if (customPostVariableTypes != null) {
                for (VariableType customVariableType : customPostVariableTypes) {
                    variableTypes.addType(customVariableType);
                }
            }
        }
    }

    // services
    // /////////////////////////////////////////////////////////////////

    protected void initServices() {
        initService(variableService);
        initService(historicVariableService);
    }

    protected void initService(Object service) {
        if (service instanceof ServiceImpl) {
            ((ServiceImpl) service).setCommandExecutor(commandExecutor);
        }
    }

    // Data managers
    ///////////////////////////////////////////////////////////

    public void initDataManagers() {
        if (variableInstanceDataManager == null) {
            variableInstanceDataManager = new MybatisVariableInstanceDataManager(this);
        }
        if (byteArrayDataManager == null) {
            byteArrayDataManager = new MybatisVariableByteArrayDataManager(this);
        }
        if (historicVariableInstanceDataManager == null) {
            historicVariableInstanceDataManager = new MybatisHistoricVariableInstanceDataManager(this);
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
    
    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }
    
    public VariableServiceConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }
    
    public List<VariableType> getCustomPreVariableTypes() {
        return customPreVariableTypes;
    }
    
    public VariableServiceConfiguration setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
        this.customPreVariableTypes = customPreVariableTypes;
        return this;
    }
    
    public VariableTypes getVariableTypes() {
        return variableTypes;
    }
    
    public VariableServiceConfiguration setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
        return this;
    }
    
    public List<VariableType> getCustomPostVariableTypes() {
        return customPostVariableTypes;
    }
    
    public VariableServiceConfiguration setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
        this.customPostVariableTypes = customPostVariableTypes;
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
    
    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public VariableServiceConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }
}
