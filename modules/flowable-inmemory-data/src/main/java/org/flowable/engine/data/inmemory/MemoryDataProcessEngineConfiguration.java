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
package org.flowable.engine.data.inmemory;

import java.util.ArrayList;
import java.util.List;

import org.flowable.common.engine.impl.db.DbSqlSessionFactory;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.persistence.entity.data.DataManager;
import org.flowable.engine.data.inmemory.db.MemoryDbSqlSessionFactory;
import org.flowable.engine.data.inmemory.impl.activity.MemoryActivityInstanceDataManager;
import org.flowable.engine.data.inmemory.impl.eventsubscription.MemoryEventSubscriptionDataManager;
import org.flowable.engine.data.inmemory.impl.execution.MemoryExecutionDataManager;
import org.flowable.engine.data.inmemory.impl.identitylink.MemoryIdentityLinkDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryDeadLetterJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryExternalWorkerJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemorySuspendedJobDataManager;
import org.flowable.engine.data.inmemory.impl.job.MemoryTimerJobDataManager;
import org.flowable.engine.data.inmemory.impl.task.MemoryTaskDataManager;
import org.flowable.engine.data.inmemory.impl.variable.MemoryVariableInstanceDataManager;
import org.flowable.engine.data.inmemory.util.ConcurrentHashMapProviderImpl;
import org.flowable.engine.data.inmemory.util.MapProvider;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.job.service.JobServiceConfiguration;

/**
 * A {@link ProcessEngineConfiguration} that uses the In-Memory (non-database)
 * {@link DataManager}s for entity types which have one available.
 * 
 * <p>
 * Entity types that do not have a Memory (non-database) implementation
 * available will fallback to use the default database DataManager
 * implementations. It is suggested that the process engine is configured to use
 * an in-memory database (eg. h2 or hsqldb) when enabling the Memory
 * DataManagers.
 * 
 * @author ikaakkola (Qvantel Finland Oy)
 */
public class MemoryDataProcessEngineConfiguration extends ProcessEngineConfigurationImpl {

    private MapProvider mapProvider = new ConcurrentHashMapProviderImpl();

    private boolean enableMemoryDataManagers = true;

    public MemoryDataProcessEngineConfiguration() {
        this.databaseSchemaUpdate = DB_SCHEMA_UPDATE_CREATE_DROP;
        this.jdbcUrl = "jdbc:h2:mem:flowable";
        this.jdbcMaxActiveConnections = Integer.MAX_VALUE;
        this.historyLevel = HistoryLevel.NONE;
    }

    public MemoryDataProcessEngineConfiguration(boolean enableMemoryDataManagers) {
        this();
        this.enableMemoryDataManagers = enableMemoryDataManagers;
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }

    @Override
    public List<CommandInterceptor> getAdditionalDefaultCommandInterceptors() {
        List<CommandInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new MemoryCompletedCommandInterceptor());
        interceptors.addAll(super.getAdditionalDefaultCommandInterceptors());
        return interceptors;
    }

    /**
     * @return The MapProvider this engine configuration uses
     */
    public MapProvider getMapProvider() {
        return mapProvider;
    }

    /**
     * @param mapProvider
     *            The MapProvider to use. Must be set before the engine
     *            confiuration is initialized.
     */
    public void setMapProvider(MapProvider mapProvider) {
        this.mapProvider = mapProvider;
    }

    @Override
    public DbSqlSessionFactory createDbSqlSessionFactory() {
        if (!enableMemoryDataManagers) {
            return super.createDbSqlSessionFactory();
        }

        // Lazy sessions doesn't actually seem to provide much benefit when
        // using h2 or hsqldb, but makes a big difference if combining the
        // memory data managers with a real database.
        return new MemoryDbSqlSessionFactory(usePrefixId, true);
    }

    @Override
    public void configuratorsAfterInit() {
        super.configuratorsAfterInit();
        if (!enableMemoryDataManagers) {
            return;
        }

        // Configure DataManagers and reinitialize relevant EntityManagers with
        // new DataManagers.

        // Due to the way ProcessEngineConfigurationImpl initializes components,
        // we have to overwrite them after all components are initialized (as
        // there is no way to set the Data Managers of the various service
        // configurations of the process engine, eg. JobServiceConfiguration is
        // created and initialized in a single call with no way to intercept)
        enableMemoryDataManagers(this, mapProvider);
    }

    /**
     * Notify all registered Memory {@link DataManager}s of an event relevant to
     * them.
     * 
     * @param type
     *            Event type
     */
    public void notifyDataManagers(MemoryDataManagerEvent type) {
        if (!enableMemoryDataManagers) {
            return;
        }
        notifyDataManagers(this, type);
    }

    /**
     * Notify all registered Memory {@link DataManager}s of an event relevant to
     * them.
     * 
     * @param target
     *            ProcessEngineConfiguration to lookup DataManagers to notify
     *            from
     * @param type
     *            Event type
     */
    public static void notifyDataManagers(ProcessEngineConfigurationImpl target, MemoryDataManagerEvent type) {
        notifyDataManager(target.getVariableServiceConfiguration().getVariableInstanceDataManager(), type);
        notifyDataManager(target.getExecutionDataManager(), type);
        notifyDataManager(target.getActivityInstanceDataManager(), type);
        notifyDataManager(target.getTaskServiceConfiguration().getTaskDataManager(), type);
        notifyDataManager(target.getEventSubscriptionServiceConfiguration().getEventSubscriptionDataManager(), type);
        notifyDataManager(target.getIdentityLinkServiceConfiguration().getIdentityLinkDataManager(), type);
        JobServiceConfiguration jobConfig = target.getJobServiceConfiguration();
        notifyDataManager(jobConfig.getJobDataManager(), type);
        notifyDataManager(jobConfig.getTimerJobDataManager(), type);
        notifyDataManager(jobConfig.getDeadLetterJobDataManager(), type);
        notifyDataManager(jobConfig.getSuspendedJobDataManager(), type);
        notifyDataManager(jobConfig.getExternalWorkerJobDataManager(), type);
    }

    private static void notifyDataManager(DataManager< ? > dataManager, MemoryDataManagerEvent type) {
        if (!(dataManager instanceof AbstractMemoryDataManager)) {
            return;
        }
        ((AbstractMemoryDataManager< ? >) dataManager).onEvent(type);
    }
    /**
     * Enable In-Memory data managers for the given ProcessEngineConfiguration.
     * <p>
     * The given engine configuration must have been initialized before
     * attempting to enable In-Memory data managers for it. The easiest way to
     * accomplish this, is to either use this
     * {@link org.flowable.engine.ProcessEngineConfiguration} implementation or
     * create a custom {@link org.flowable.engine.ProcessEngineConfiguration}
     * that calls this method in
     * {@link org.flowable.engine.ProcessEngineConfiguration#configuratorsAfterInit()}.
     */
    public static void enableMemoryDataManagers(ProcessEngineConfigurationImpl target, MapProvider mapProvider) {
        // ByteArrays are not (yet) enabled as there are non-memory services
        // that use ACT_GE_BYTEARRAY:
        //
        // - MybatisBatchDataManager
        // - database table references from ACT_RE_MODEL , ACT_PROCDEF_INFO

        // Variables
        target.getVariableServiceConfiguration()
                        .setVariableInstanceDataManager(new MemoryVariableInstanceDataManager(mapProvider, target.getVariableServiceConfiguration()));

        // Executions
        target.setExecutionDataManager(new MemoryExecutionDataManager(mapProvider, target, target.getVariableServiceConfiguration(),
                        target.getEventSubscriptionServiceConfiguration(), target.getIdentityLinkServiceConfiguration(), target.getJobServiceConfiguration()));

        // Activities
        target.setActivityInstanceDataManager(new MemoryActivityInstanceDataManager(mapProvider, target));

        // Tasks
        target.getTaskServiceConfiguration().setTaskDataManager(new MemoryTaskDataManager(mapProvider, target));

        // Event subscriptions
        target.getEventSubscriptionServiceConfiguration().setEventSubscriptionDataManager(new MemoryEventSubscriptionDataManager(mapProvider, target));

        // Identity links
        target.getIdentityLinkServiceConfiguration().setIdentityLinkDataManager(new MemoryIdentityLinkDataManager(mapProvider, target));

        // Jobs
        JobServiceConfiguration jobConfig = target.getJobServiceConfiguration();
        jobConfig.setDeadLetterJobDataManager(new MemoryDeadLetterJobDataManager(mapProvider, target, target.getJobServiceConfiguration()));
        jobConfig.setJobDataManager(new MemoryJobDataManager(mapProvider, target, target.getJobServiceConfiguration()));
        jobConfig.setSuspendedJobDataManager(new MemorySuspendedJobDataManager(mapProvider, target, target.getJobServiceConfiguration()));
        jobConfig.setTimerJobDataManager(new MemoryTimerJobDataManager(mapProvider, target, target.getJobServiceConfiguration()));
        jobConfig.setExternalWorkerJobDataManager(new MemoryExternalWorkerJobDataManager(mapProvider, target, target.getJobServiceConfiguration(),
                        target.getIdentityLinkServiceConfiguration()));
    }
}
