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
package org.flowable.cmmn.engine;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.flowable.cmmn.api.CmmnEngineConfigurationApi;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.PlanItemInstanceCallbackType;
import org.flowable.cmmn.engine.impl.CmmnEngineImpl;
import org.flowable.cmmn.engine.impl.CmmnHistoryServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnRepositoryServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnTaskServiceImpl;
import org.flowable.cmmn.engine.impl.ServiceImpl;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaSessionFactory;
import org.flowable.cmmn.engine.impl.agenda.DefaultCmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.callback.ChildCaseInstanceStateChangeCallback;
import org.flowable.cmmn.engine.impl.callback.DefaultInternalCmmnJobManager;
import org.flowable.cmmn.engine.impl.cfg.DelegateExpressionFieldInjectionMode;
import org.flowable.cmmn.engine.impl.cfg.IdmEngineConfigurator;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.JobRetryCmd;
import org.flowable.cmmn.engine.impl.db.CmmnDbSchemaManager;
import org.flowable.cmmn.engine.impl.db.EntityDependencyOrder;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegateFactory;
import org.flowable.cmmn.engine.impl.delegate.DefaultCmmnClassDelegateFactory;
import org.flowable.cmmn.engine.impl.deployer.CaseDefinitionDiagramHelper;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.el.CmmnExpressionManager;
import org.flowable.cmmn.engine.impl.form.DefaultFormFieldHandler;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryTaskManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryVariableManager;
import org.flowable.cmmn.engine.impl.history.DefaultCmmnHistoryManager;
import org.flowable.cmmn.engine.impl.interceptor.CmmnCommandInvoker;
import org.flowable.cmmn.engine.impl.job.AsyncActivatePlanItemInstanceJobHandler;
import org.flowable.cmmn.engine.impl.job.TriggerTimerEventJobHandler;
import org.flowable.cmmn.engine.impl.parser.CmmnActivityBehaviorFactory;
import org.flowable.cmmn.engine.impl.parser.CmmnParser;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.engine.impl.parser.DefaultCmmnActivityBehaviorFactory;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseDefinitionEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CaseInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnDeploymentEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.CmmnResourceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricCaseInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricMilestoneInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.MilestoneInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.PlanItemInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.SentryPartInstanceEntityManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.CmmnResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricCaseInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.MilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.SentryPartInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.TableDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisPlanItemInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisSentryPartInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.TableDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelperImpl;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.impl.task.DefaultCmmnTaskVariableScopeResolver;
import org.flowable.cmmn.image.CaseDiagramGenerator;
import org.flowable.cmmn.image.impl.DefaultCaseDiagramGenerator;
import org.flowable.engine.common.AbstractEngineConfiguration;
import org.flowable.engine.common.EngineConfigurator;
import org.flowable.engine.common.EngineDeployer;
import org.flowable.engine.common.api.delegate.FlowableFunctionDelegate;
import org.flowable.engine.common.impl.calendar.BusinessCalendarManager;
import org.flowable.engine.common.impl.calendar.CycleBusinessCalendar;
import org.flowable.engine.common.impl.calendar.DueDateBusinessCalendar;
import org.flowable.engine.common.impl.calendar.DurationBusinessCalendar;
import org.flowable.engine.common.impl.calendar.MapBusinessCalendarManager;
import org.flowable.engine.common.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.engine.common.impl.cfg.BeansConfigurationHelper;
import org.flowable.engine.common.impl.db.DbSchemaManager;
import org.flowable.engine.common.impl.el.ExpressionManager;
import org.flowable.engine.common.impl.history.HistoryLevel;
import org.flowable.engine.common.impl.interceptor.Command;
import org.flowable.engine.common.impl.interceptor.CommandInterceptor;
import org.flowable.engine.common.impl.interceptor.EngineConfigurationConstants;
import org.flowable.engine.common.impl.interceptor.SessionFactory;
import org.flowable.engine.common.impl.persistence.GenericManagerFactory;
import org.flowable.engine.common.impl.persistence.cache.EntityCache;
import org.flowable.engine.common.impl.persistence.cache.EntityCacheImpl;
import org.flowable.engine.common.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.engine.common.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.common.impl.util.ReflectUtil;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.db.IdentityLinkDbSchemaManager;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnableFactory;
import org.flowable.job.service.impl.asyncexecutor.FailedJobCommandFactory;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.db.JobDbSchemaManager;
import org.flowable.task.service.InternalTaskVariableScopeResolver;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.db.TaskDbSchemaManager;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.db.IbatisVariableTypeHandler;
import org.flowable.variable.service.impl.db.VariableDbSchemaManager;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

public class CmmnEngineConfiguration extends AbstractEngineConfiguration implements CmmnEngineConfigurationApi {

    protected static final Logger LOGGER = LoggerFactory.getLogger(CmmnEngineConfiguration.class);
    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/cmmn/db/mapping/mappings.xml";
    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_CMMN_";

    protected String cmmnEngineName = "default";

    protected CmmnEngineAgendaFactory cmmnEngineAgendaFactory;

    protected CmmnRuntimeService cmmnRuntimeService = new CmmnRuntimeServiceImpl();
    protected CmmnTaskService cmmnTaskService = new CmmnTaskServiceImpl();
    protected CmmnManagementService cmmnManagementService = new CmmnManagementServiceImpl();
    protected CmmnRepositoryService cmmnRepositoryService = new CmmnRepositoryServiceImpl();
    protected CmmnHistoryService cmmnHistoryService = new CmmnHistoryServiceImpl();

    protected TableDataManager tableDataManager;
    protected CmmnDeploymentDataManager deploymentDataManager;
    protected CmmnResourceDataManager resourceDataManager;
    protected CaseDefinitionDataManager caseDefinitionDataManager;
    protected CaseInstanceDataManager caseInstanceDataManager;
    protected PlanItemInstanceDataManager planItemInstanceDataManager;
    protected SentryPartInstanceDataManager sentryPartInstanceDataManager;
    protected MilestoneInstanceDataManager milestoneInstanceDataManager;
    protected HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager;
    protected HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager;

    protected CmmnDeploymentEntityManager cmmnDeploymentEntityManager;
    protected CmmnResourceEntityManager cmmnResourceEntityManager;
    protected CaseDefinitionEntityManager caseDefinitionEntityManager;
    protected CaseInstanceEntityManager caseInstanceEntityManager;
    protected PlanItemInstanceEntityManager planItemInstanceEntityManager;
    protected SentryPartInstanceEntityManager sentryPartInstanceEntityManager;
    protected MilestoneInstanceEntityManager milestoneInstanceEntityManager;
    protected HistoricCaseInstanceDataManager historicCaseInstanceDataManager;
    protected HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager;

    protected boolean disableIdmEngine;

    protected CaseInstanceHelper caseInstanceHelper;
    protected CmmnHistoryManager cmmnHistoryManager;
    protected ProcessInstanceService processInstanceService;
    protected Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceStateChangeCallbacks;

    protected boolean executeServiceDbSchemaManagers = true;

    protected boolean enableSafeCmmnXml;
    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    protected CmmnClassDelegateFactory classDelegateFactory;
    protected CmmnParser cmmnParser;
    protected CmmnDeployer cmmnDeployer;
    protected CmmnDeploymentManager deploymentManager;
    protected CaseDefinitionDiagramHelper caseDefinitionDiagramHelper;

    protected int caseDefinitionCacheLimit = -1;
    protected DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache;

    protected HistoryLevel historyLevel = HistoryLevel.AUDIT;

    protected ExpressionManager expressionManager;
    protected List<FlowableFunctionDelegate> flowableFunctionDelegates;
    protected List<FlowableFunctionDelegate> customFlowableFunctionDelegates;

    /**
     * Using field injection together with a delegate expression for a service task / execution listener / task listener is not thread-sade , see user guide section 'Field Injection' for more
     * information.
     * <p>
     * Set this flag to false to throw an exception at runtime when a field is injected and a delegateExpression is used.
     */
    protected DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode = DelegateExpressionFieldInjectionMode.MIXED;

    protected DbSchemaManager identityLinkDbSchemaManager;
    protected DbSchemaManager variableDbSchemaManager;
    protected DbSchemaManager taskDbSchemaManager;
    protected DbSchemaManager jobDbSchemaManager;

    /**
     * Case diagram generator. Default value is DefaultCaseDiagramGenerator
     */
    protected CaseDiagramGenerator caseDiagramGenerator;

    protected boolean isCreateDiagramOnDeploy = true;

    protected String activityFontName = "Arial";
    protected String labelFontName = "Arial";
    protected String annotationFontName = "Arial";

    // CONFIGURATORS ////////////////////////////////////////////////////////////

    protected boolean enableConfiguratorServiceLoader = true; // Enabled by default. In certain environments this should be set to false (eg osgi)
    protected List<EngineConfigurator> configurators; // The injected configurators
    protected List<EngineConfigurator> allConfigurators; // Including auto-discovered configurators
    protected EngineConfigurator idmEngineConfigurator;

    // Identitylink support
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;

    // Task support
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected InternalHistoryTaskManager internalHistoryTaskManager;
    protected InternalTaskVariableScopeResolver internalTaskVariableScopeResolver;
    protected boolean isEnableTaskRelationshipCounts = true;
    protected int taskQueryLimit;
    protected int historicTaskQueryLimit;

    protected int caseQueryLimit = 20000;

    // Variable support
    protected VariableTypes variableTypes;
    protected List<VariableType> customPreVariableTypes;
    protected List<VariableType> customPostVariableTypes;
    protected VariableServiceConfiguration variableServiceConfiguration;
    protected InternalHistoryVariableManager internalHistoryVariableManager;
    protected boolean serializableVariableTypeTrackDeserializedObjects = true;
    protected ObjectMapper objectMapper = new ObjectMapper();

    // Set Http Client config defaults
    protected HttpClientConfig httpClientConfig = new HttpClientConfig();

    // Async executor
    protected JobServiceConfiguration jobServiceConfiguration;

    protected AsyncExecutor asyncExecutor;
    protected JobManager jobManager;
    protected List<JobHandler> customJobHandlers;
    protected Map<String, JobHandler> jobHandlers;
    protected InternalJobManager internalJobManager;
    protected List<AsyncRunnableExecutionExceptionHandler> customAsyncRunnableExecutionExceptionHandlers;
    protected boolean addDefaultExceptionHandler = true;
    protected FailedJobCommandFactory failedJobCommandFactory;

    protected FormFieldHandler formFieldHandler;

    /**
     * Boolean flag to be set to activate the {@link AsyncExecutor} automatically after the engine has booted up.
     */
    protected boolean asyncExecutorActivate;

    /**
     * Experimental!
     * <p>
     * Set this to true when using the message queue based job executor.
     */
    protected boolean asyncExecutorMessageQueueMode;

    /**
     * The number of retries for a job.
     */
    protected int asyncExecutorNumberOfRetries = 3;

    /**
     * Define the default lock time for an async job in seconds.
     * The lock time is used when creating an async job and when it expires the async executor assumes that the job has failed.
     * It will be retried again.
     */
    protected int lockTimeAsyncJobWaitTime = 60;

    /**
     * Define the default wait time for a failed job in seconds
     */
    protected int defaultFailedJobWaitTime = 10;

    /**
     * Defines the default wait time for a failed async job in seconds
     */
    protected int asyncFailedJobWaitTime = 10;

    /**
     * The minimal number of threads that are kept alive in the threadpool for job execution.
     * Default value = 2.
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorCorePoolSize = 2;

    /**
     * The maximum number of threads that are created in the threadpool for job execution.
     * Default value = 10.
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorMaxPoolSize = 10;

    /**
     * The time (in milliseconds) a thread used for job execution must be kept alive before it is destroyed.
     * Default setting is 5 seconds. Having a setting > 0 takes resources, but in the case of many
     * job executions it avoids creating new threads all the time.
     * If 0, threads will be destroyed after they've been used for job execution.
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected long asyncExecutorThreadKeepAliveTime = 5000L;

    /**
     * The size of the queue on which jobs to be executed are placed, before they are actually executed.
     * Default value = 100.
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorThreadPoolQueueSize = 100;

    /**
     * The queue onto which jobs will be placed before they are actually executed.
     * Threads form the async executor threadpool will take work from this queue.
     * <p>
     * By default null. If null, an {@link ArrayBlockingQueue} will be created of size {@link #asyncExecutorThreadPoolQueueSize}.
     * <p>
     * When the queue is full, the job will be executed by the calling thread (ThreadPoolExecutor.CallerRunsPolicy())
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected BlockingQueue<Runnable> asyncExecutorThreadPoolQueue;

    /**
     * The time (in seconds) that is waited to gracefully shut down the threadpool used for job execution when
     * a shutdown on the executor (or engine) is requested. Default value = 60.
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected long asyncExecutorSecondsToWaitOnShutdown = 60L;

    /**
     * The number of timer jobs that are acquired during one query
     * Before a job is executed, an acquirement thread fetches jobs from the database and puts them on the queue.
     * <p>
     * Default value = 1, as this lowers the potential on optimistic locking exceptions.
     * A larger value means more timer jobs will be fetched in one request.
     * Change this value if you know what you are doing.
     * <p>
     */
    protected int asyncExecutorMaxTimerJobsPerAcquisition = 1;

    /**
     * The number of async jobs that are acquired during one query (before a job is executed,
     * an acquirement thread fetches jobs from the database and puts them on the queue).
     * <p>
     * Default value = 1, as this lowers the potential on optimistic locking exceptions.
     * A larger value means more jobs will be fetched at the same time.
     * Change this value if you know what you are doing.
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorMaxAsyncJobsDuePerAcquisition = 1;

    /**
     * The time (in milliseconds) the timer acquisition thread will wait to execute the next acquirement query.
     * This happens when no new timer jobs were found or when less timer jobs have been fetched
     * than set in {@link #asyncExecutorMaxTimerJobsPerAcquisition}. Default value = 10 seconds.
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorDefaultTimerJobAcquireWaitTime = 10 * 1000;

    /**
     * The time (in milliseconds) the async job acquisition thread will wait to execute the next acquirement query.
     * This happens when no new async jobs were found or when less async jobs have been
     * fetched than set in {@link #asyncExecutorMaxAsyncJobsDuePerAcquisition}. Default value = 10 seconds.
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorDefaultAsyncJobAcquireWaitTime = 10 * 1000;

    /**
     * The time (in milliseconds) the async job (both timer and async continuations) acquisition thread will wait
     * when the queue is full to execute the next query. By default set to 0 (for backwards compatibility)
     */
    protected int asyncExecutorDefaultQueueSizeFullWaitTime;

    /**
     * When a job is acquired, it is locked so other async executors can't lock and execute it.
     * While doing this, the 'name' of the lock owner is written into a column of the job.
     * <p>
     * By default, a random UUID will be generated when the executor is created.
     * <p>
     * It is important that each async executor instance in a cluster of Flowable engines has a different name!
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected String asyncExecutorLockOwner;

    /**
     * The amount of time (in milliseconds) a timer job is locked when acquired by the async executor.
     * During this period of time, no other async executor will try to acquire and lock this job.
     * <p>
     * Default value = 5 minutes;
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorTimerLockTimeInMillis = 5 * 60 * 1000;

    /**
     * The amount of time (in milliseconds) an async job is locked when acquired by the async executor.
     * During this period of time, no other async executor will try to acquire and lock this job.
     * <p>
     * Default value = 5 minutes;
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorAsyncJobLockTimeInMillis = 5 * 60 * 1000;

    /**
     * The amount of time (in milliseconds) that is between two consecutive checks of 'expired jobs'.
     * Expired jobs are jobs that were locked (a lock owner + time was written by some executor, but the job was never completed).
     * <p>
     * During such a check, jobs that are expired are again made available, meaning the lock owner and lock time will be removed.
     * Other executors will now be able to pick it up.
     * <p>
     * A job is deemed expired if the current time has passed the lock time.
     * <p>
     * By default one minute.
     */
    protected int asyncExecutorResetExpiredJobsInterval = 60 * 1000;

    /**
     * The amount of time (in milliseconds) a job can maximum be in the 'executable' state before being deemed expired.
     * Note that this won't happen when using the threadpool based executor, as the acquire thread will fetch these kind of jobs earlier.
     * However, in the message queue based execution, it could be some job is posted to a queue but then never is locked nor executed.
     * <p>
     * By default 24 hours, as this should be a very exceptional case.
     */
    protected int asyncExecutorResetExpiredJobsMaxTimeout = 24 * 60 * 60 * 1000;

    /**
     * The default {@link AsyncExecutor} has a 'cleanup' thread that resets expired jobs so they can be re-acquired by other executors.
     * This setting defines the size of the page being used when fetching these expired jobs.
     */
    protected int asyncExecutorResetExpiredJobsPageSize = 3;

    protected BusinessCalendarManager businessCalendarManager;

    /**
     * Allows to define a custom factory for creating the {@link Runnable} that is executed by the async executor.
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory;

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromResourceDefault() {
        return createCmmnEngineConfigurationFromResource("flowable.cmmn.cfg.xml", "cmmnEngineConfiguration");
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromResource(String resource) {
        return createCmmnEngineConfigurationFromResource(resource, "cmmnEngineConfiguration");
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromResource(String resource, String beanName) {
        return (CmmnEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromResource(resource, beanName);
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromInputStream(InputStream inputStream) {
        return createCmmnEngineConfigurationFromInputStream(inputStream, "cmmnEngineConfiguration");
    }

    public static CmmnEngineConfiguration createCmmnEngineConfigurationFromInputStream(InputStream inputStream, String beanName) {
        return (CmmnEngineConfiguration) BeansConfigurationHelper.parseEngineConfigurationFromInputStream(inputStream, beanName);
    }

    public static CmmnEngineConfiguration createStandaloneCmmnEngineConfiguration() {
        return new CmmnEngineConfiguration();
    }

    public static CmmnEngineConfiguration createStandaloneInMemCmmnEngineConfiguration() {
        return new StandaloneInMemCmmnEngineConfiguration();
    }

    public CmmnEngine buildCmmnEngine() {
        init();
        return new CmmnEngineImpl(this);
    }

    protected void init() {
        initConfigurators();
        configuratorsBeforeInit();
        initCaseDiagramGenerator();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();
        initExpressionManager();
        initCmmnEngineAgendaFactory();

        if (usingRelationalDatabase) {
            initDataSource();
            initDbSchemaManager();
        }

        initVariableTypes();
        initBeans();
        initTransactionFactory();

        if (usingRelationalDatabase) {
            initSqlSessionFactory();
        }

        initSessionFactories();
        initServices();
        initDataManagers();
        initEntityManagers();
        initClassDelegateFactory();
        initActivityBehaviorFactory();
        initDeployers();
        initCaseDefinitionCache();
        initDeploymentManager();
        initCaseInstanceHelper();
        initHistoryManager();
        initCaseInstanceCallbacks();
        initFormFieldHandler();
        initClock();
        initIdentityLinkServiceConfiguration();
        initVariableServiceConfiguration();
        configuratorsAfterInit();
        initTaskServiceConfiguration();
        initBusinessCalendarManager();
        initJobHandlers();
        initFailedJobCommandFactory();
        initJobServiceConfiguration();
        initAsyncExecutor();
    }

    public void initCaseDiagramGenerator() {
        if (caseDiagramGenerator == null) {
            caseDiagramGenerator = new DefaultCaseDiagramGenerator();
        }
    }

    @Override
    public void initDbSchemaManager() {
        super.initDbSchemaManager();
        initCmmnDbSchemaManager();

        if (executeServiceDbSchemaManagers) {
            initIdentityLinkDbSchemaManager();
            initVariableDbSchemaManager();
            initTaskDbSchemaManager();
            initJobDbSchemaManager();
        }
    }

    protected void initCmmnDbSchemaManager() {
        if (this.dbSchemaManager == null) {
            this.dbSchemaManager = new CmmnDbSchemaManager();
        }
    }

    protected void initVariableDbSchemaManager() {
        if (this.variableDbSchemaManager == null) {
            this.variableDbSchemaManager = new VariableDbSchemaManager();
        }
    }

    protected void initTaskDbSchemaManager() {
        if (this.taskDbSchemaManager == null) {
            this.taskDbSchemaManager = new TaskDbSchemaManager();
        }
    }

    protected void initIdentityLinkDbSchemaManager() {
        if (this.identityLinkDbSchemaManager == null) {
            this.identityLinkDbSchemaManager = new IdentityLinkDbSchemaManager();
        }
    }

    protected void initJobDbSchemaManager() {
        if (this.jobDbSchemaManager == null) {
            this.jobDbSchemaManager = new JobDbSchemaManager();
        }
    }

    @Override
    public void initMybatisTypeHandlers(Configuration configuration) {
        configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler(variableTypes));
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            expressionManager = new CmmnExpressionManager(beans);
        }
        if (flowableFunctionDelegates == null) {
            flowableFunctionDelegates = new ArrayList<>();
        }
        if (customFlowableFunctionDelegates != null) {
            flowableFunctionDelegates.addAll(customFlowableFunctionDelegates);
        }
        expressionManager.setFunctionDelegates(flowableFunctionDelegates);
    }

    public void initCmmnEngineAgendaFactory() {
        if (cmmnEngineAgendaFactory == null) {
            cmmnEngineAgendaFactory = new DefaultCmmnEngineAgendaFactory();
        }
    }

    @Override
    public void initCommandInvoker() {
        if (commandInvoker == null) {
            commandInvoker = new CmmnCommandInvoker();
        }
    }

    public void initSessionFactories() {
        if (sessionFactories == null) {
            sessionFactories = new HashMap<>();

            if (usingRelationalDatabase) {
                initDbSqlSessionFactory();
            }

            addSessionFactory(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));
            commandContextFactory.setSessionFactories(sessionFactories);
        }

        addSessionFactory(new CmmnEngineAgendaSessionFactory(cmmnEngineAgendaFactory));

        if (customSessionFactories != null) {
            for (SessionFactory sessionFactory : customSessionFactories) {
                addSessionFactory(sessionFactory);
            }
        }
    }

    protected void initServices() {
        initService(cmmnRuntimeService);
        initService(cmmnTaskService);
        initService(cmmnManagementService);
        initService(cmmnRepositoryService);
        initService(cmmnHistoryService);
    }

    protected void initService(Object service) {
        if (service instanceof ServiceImpl) {
            ((ServiceImpl) service).setEngineConfig(this);
            ((ServiceImpl) service).setCommandExecutor(commandExecutor);
        }
    }

    public void initDataManagers() {
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl();
        }
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisCmmnDeploymentDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
        if (caseDefinitionDataManager == null) {
            caseDefinitionDataManager = new MybatisCaseDefinitionDataManager(this);
        }
        if (caseInstanceDataManager == null) {
            caseInstanceDataManager = new MybatisCaseInstanceDataManagerImpl(this);
        }
        if (planItemInstanceDataManager == null) {
            planItemInstanceDataManager = new MybatisPlanItemInstanceDataManagerImpl(this);
        }
        if (sentryPartInstanceDataManager == null) {
            sentryPartInstanceDataManager = new MybatisSentryPartInstanceDataManagerImpl(this);
        }
        if (milestoneInstanceDataManager == null) {
            milestoneInstanceDataManager = new MybatisMilestoneInstanceDataManager(this);
        }
        if (historicCaseInstanceDataManager == null) {
            historicCaseInstanceDataManager = new MybatisHistoricCaseInstanceDataManagerImpl(this);
        }
        if (historicMilestoneInstanceDataManager == null) {
            historicMilestoneInstanceDataManager = new MybatisHistoricMilestoneInstanceDataManager(this);
        }
    }

    public void initEntityManagers() {
        if (cmmnDeploymentEntityManager == null) {
            cmmnDeploymentEntityManager = new CmmnDeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (cmmnResourceEntityManager == null) {
            cmmnResourceEntityManager = new CmmnResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (caseDefinitionEntityManager == null) {
            caseDefinitionEntityManager = new CaseDefinitionEntityManagerImpl(this, caseDefinitionDataManager);
        }
        if (caseInstanceEntityManager == null) {
            caseInstanceEntityManager = new CaseInstanceEntityManagerImpl(this, caseInstanceDataManager);
        }
        if (planItemInstanceEntityManager == null) {
            planItemInstanceEntityManager = new PlanItemInstanceEntityManagerImpl(this, planItemInstanceDataManager);
        }
        if (sentryPartInstanceEntityManager == null) {
            sentryPartInstanceEntityManager = new SentryPartInstanceEntityManagerImpl(this, sentryPartInstanceDataManager);
        }
        if (milestoneInstanceEntityManager == null) {
            milestoneInstanceEntityManager = new MilestoneInstanceEntityManagerImpl(this, milestoneInstanceDataManager);
        }
        if (historicCaseInstanceEntityManager == null) {
            historicCaseInstanceEntityManager = new HistoricCaseInstanceEntityManagerImpl(this, historicCaseInstanceDataManager);
        }
        if (historicMilestoneInstanceEntityManager == null) {
            historicMilestoneInstanceEntityManager = new HistoricMilestoneInstanceEntityManagerImpl(this, historicMilestoneInstanceDataManager);
        }
    }

    protected void initClassDelegateFactory() {
        if (classDelegateFactory == null) {
            classDelegateFactory = new DefaultCmmnClassDelegateFactory();
        }
    }

    protected void initActivityBehaviorFactory() {
        if (activityBehaviorFactory == null) {
            DefaultCmmnActivityBehaviorFactory defaultCmmnActivityBehaviorFactory = new DefaultCmmnActivityBehaviorFactory();
            defaultCmmnActivityBehaviorFactory.setClassDelegateFactory(classDelegateFactory);
            defaultCmmnActivityBehaviorFactory.setExpressionManager(expressionManager);
            activityBehaviorFactory = defaultCmmnActivityBehaviorFactory;
        }
    }

    protected void initDeployers() {
        if (this.cmmnDeployer == null) {
            this.deployers = new ArrayList<>();
            if (customPreDeployers != null) {
                this.deployers.addAll(customPreDeployers);
            }
            this.deployers.addAll(getDefaultDeployers());
            if (customPostDeployers != null) {
                this.deployers.addAll(customPostDeployers);
            }
        }
    }

    public Collection<? extends EngineDeployer> getDefaultDeployers() {
        List<EngineDeployer> defaultDeployers = new ArrayList<>();

        if (cmmnDeployer == null) {
            cmmnDeployer = new CmmnDeployer();
        }

        initCmmnParser();
        initCaseDefinitionDiagramHelper();

        cmmnDeployer.setIdGenerator(idGenerator);
        cmmnDeployer.setCmmnParser(cmmnParser);
        cmmnDeployer.setCaseDefinitionDiagramHelper(caseDefinitionDiagramHelper);

        defaultDeployers.add(cmmnDeployer);
        return defaultDeployers;
    }

    protected void initCaseDefinitionCache() {
        if (caseDefinitionCache == null) {
            if (caseDefinitionCacheLimit <= 0) {
                caseDefinitionCache = new DefaultDeploymentCache<>();
            } else {
                caseDefinitionCache = new DefaultDeploymentCache<>(caseDefinitionCacheLimit);
            }
        }
    }

    protected void initDeploymentManager() {
        if (deploymentManager == null) {
            deploymentManager = new CmmnDeploymentManager();
            deploymentManager.setCmmnEngineConfiguration(this);
            deploymentManager.setCaseDefinitionCache(caseDefinitionCache);
            deploymentManager.setDeployers(deployers);
            deploymentManager.setCaseDefinitionEntityManager(caseDefinitionEntityManager);
            deploymentManager.setDeploymentEntityManager(cmmnDeploymentEntityManager);
        }
    }

    public void initCmmnParser() {
        if (cmmnParser == null) {
            CmmnParserImpl cmmnParserImpl = new CmmnParserImpl();
            cmmnParserImpl.setActivityBehaviorFactory(activityBehaviorFactory);
            cmmnParserImpl.setExpressionManager(expressionManager);
            cmmnParser = cmmnParserImpl;
        }
    }

    public void initCaseDefinitionDiagramHelper() {
        if (caseDefinitionDiagramHelper == null) {
            caseDefinitionDiagramHelper = new CaseDefinitionDiagramHelper();
        }
    }

    public void initCaseInstanceHelper() {
        if (caseInstanceHelper == null) {
            caseInstanceHelper = new CaseInstanceHelperImpl();
        }
    }

    public void initHistoryManager() {
        if (cmmnHistoryManager == null) {
            cmmnHistoryManager = new DefaultCmmnHistoryManager(this);
        }
    }

    public void initCaseInstanceCallbacks() {
        if (this.caseInstanceStateChangeCallbacks == null) {
            this.caseInstanceStateChangeCallbacks = new HashMap<>();
        }
        initDefaultCaseInstanceCallbacks();
    }

    public void initFormFieldHandler() {
        if (this.formFieldHandler == null) {
            this.formFieldHandler = new DefaultFormFieldHandler();
        }
    }

    protected void initDefaultCaseInstanceCallbacks() {
        this.caseInstanceStateChangeCallbacks.put(PlanItemInstanceCallbackType.CHILD_CASE,
                Collections.<RuntimeInstanceStateChangeCallback>singletonList(new ChildCaseInstanceStateChangeCallback()));
    }

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG;
    }

    @Override
    public CommandInterceptor createTransactionInterceptor() {
        return null;
    }

    @Override
    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
    }

    @Override
    protected void initDbSqlSessionFactoryEntitySettings() {
        defaultInitDbSqlSessionFactoryEntitySettings(EntityDependencyOrder.INSERT_ORDER, EntityDependencyOrder.DELETE_ORDER);
    }

    public void initConfigurators() {

        allConfigurators = new ArrayList<>();

        if (!disableIdmEngine) {
            if (idmEngineConfigurator != null) {
                allConfigurators.add(idmEngineConfigurator);
            } else {
                allConfigurators.add(new IdmEngineConfigurator());
            }
        }

        // Configurators that are explicitly added to the config
        if (configurators != null) {
            allConfigurators.addAll(configurators);
        }

        // Auto discovery through ServiceLoader
        if (enableConfiguratorServiceLoader) {
            ClassLoader classLoader = getClassLoader();
            if (classLoader == null) {
                classLoader = ReflectUtil.getClassLoader();
            }

            ServiceLoader<EngineConfigurator> configuratorServiceLoader = ServiceLoader.load(EngineConfigurator.class, classLoader);
            int nrOfServiceLoadedConfigurators = 0;
            for (EngineConfigurator configurator : configuratorServiceLoader) {
                allConfigurators.add(configurator);
                nrOfServiceLoadedConfigurators++;
            }

            if (nrOfServiceLoadedConfigurators > 0) {
                LOGGER.info("Found {} auto-discoverable Process Engine Configurator{}", nrOfServiceLoadedConfigurators++, nrOfServiceLoadedConfigurators > 1 ? "s" : "");
            }

            if (!allConfigurators.isEmpty()) {

                // Order them according to the priorities (useful for dependent
                // configurator)
                Collections.sort(allConfigurators, new Comparator<EngineConfigurator>() {
                    @Override
                    public int compare(EngineConfigurator configurator1, EngineConfigurator configurator2) {
                        int priority1 = configurator1.getPriority();
                        int priority2 = configurator2.getPriority();

                        if (priority1 < priority2) {
                            return -1;
                        } else if (priority1 > priority2) {
                            return 1;
                        }
                        return 0;
                    }
                });

                // Execute the configurators
                LOGGER.info("Found {} Engine Configurators in total:", allConfigurators.size());
                for (EngineConfigurator configurator : allConfigurators) {
                    LOGGER.info("{} (priority:{})", configurator.getClass(), configurator.getPriority());
                }

            }

        }
    }

    public void configuratorsBeforeInit() {
        for (EngineConfigurator configurator : allConfigurators) {
            LOGGER.info("Executing beforeInit() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
            configurator.beforeInit(this);
        }
    }

    public void configuratorsAfterInit() {
        for (EngineConfigurator configurator : allConfigurators) {
            LOGGER.info("Executing configure() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
            configurator.configure(this);
        }
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

    public void initVariableServiceConfiguration() {
        this.variableServiceConfiguration = new VariableServiceConfiguration();

        this.variableServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.variableServiceConfiguration.setClock(this.clock);
        this.variableServiceConfiguration.setObjectMapper(this.objectMapper);
        this.variableServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        this.variableServiceConfiguration.setVariableTypes(this.variableTypes);

        if (this.internalHistoryVariableManager != null) {
            this.variableServiceConfiguration.setInternalHistoryVariableManager(this.internalHistoryVariableManager);
        } else {
            this.variableServiceConfiguration.setInternalHistoryVariableManager(new CmmnHistoryVariableManager(cmmnHistoryManager));
        }

        this.variableServiceConfiguration.setMaxLengthString(this.getMaxLengthString());
        this.variableServiceConfiguration.setSerializableVariableTypeTrackDeserializedObjects(this.isSerializableVariableTypeTrackDeserializedObjects());

        this.variableServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG, this.variableServiceConfiguration);
    }

    public void initTaskServiceConfiguration() {
        this.taskServiceConfiguration = new TaskServiceConfiguration();
        this.taskServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.taskServiceConfiguration.setClock(this.clock);
        this.taskServiceConfiguration.setObjectMapper(this.objectMapper);
        this.taskServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        if (this.internalHistoryTaskManager != null) {
            this.taskServiceConfiguration.setInternalHistoryTaskManager(this.internalHistoryTaskManager);
        } else {
            this.taskServiceConfiguration.setInternalHistoryTaskManager(new CmmnHistoryTaskManager(cmmnHistoryManager));
        }

        if (this.internalTaskVariableScopeResolver != null) {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(this.internalTaskVariableScopeResolver);
        } else {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(new DefaultCmmnTaskVariableScopeResolver(this));
        }

        this.taskServiceConfiguration.setEnableTaskRelationshipCounts(this.isEnableTaskRelationshipCounts);
        this.taskServiceConfiguration.setTaskQueryLimit(this.taskQueryLimit);
        this.taskServiceConfiguration.setHistoricTaskQueryLimit(this.historicTaskQueryLimit);

        this.taskServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG, this.taskServiceConfiguration);
    }

    public void initIdentityLinkServiceConfiguration() {
        this.identityLinkServiceConfiguration = new IdentityLinkServiceConfiguration();
        this.identityLinkServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.identityLinkServiceConfiguration.setClock(this.clock);
        this.identityLinkServiceConfiguration.setObjectMapper(this.objectMapper);
        this.identityLinkServiceConfiguration.setEventDispatcher(this.eventDispatcher);

        this.identityLinkServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG, this.identityLinkServiceConfiguration);
    }

    public void initBusinessCalendarManager() {
        if (businessCalendarManager == null) {
            MapBusinessCalendarManager mapBusinessCalendarManager = new MapBusinessCalendarManager();
            mapBusinessCalendarManager.addBusinessCalendar(DurationBusinessCalendar.NAME, new DurationBusinessCalendar(this.clock));
            mapBusinessCalendarManager.addBusinessCalendar(DueDateBusinessCalendar.NAME, new DueDateBusinessCalendar(this.clock));
            mapBusinessCalendarManager.addBusinessCalendar(CycleBusinessCalendar.NAME, new CycleBusinessCalendar(this.clock));

            businessCalendarManager = mapBusinessCalendarManager;
        }
    }

    public void initJobHandlers() {
        jobHandlers = new HashMap<>();
        jobHandlers.put(TriggerTimerEventJobHandler.TYPE, new TriggerTimerEventJobHandler());
        jobHandlers.put(AsyncActivatePlanItemInstanceJobHandler.TYPE, new AsyncActivatePlanItemInstanceJobHandler());

        // if we have custom job handlers, register them
        if (customJobHandlers != null) {
            for (JobHandler customJobHandler : customJobHandlers) {
                jobHandlers.put(customJobHandler.getType(), customJobHandler);
            }
        }
    }

    public void initFailedJobCommandFactory() {
        if (this.failedJobCommandFactory == null) {
            this.failedJobCommandFactory = new FailedJobCommandFactory() {
                @Override
                public Command<Object> getCommand(String jobId, Throwable exception) {
                    return new JobRetryCmd(jobId, exception);
                }
            };
        }
    }

    public void initJobServiceConfiguration() {
        this.jobServiceConfiguration = new JobServiceConfiguration();
        this.jobServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.jobServiceConfiguration.setClock(this.clock);
        this.jobServiceConfiguration.setObjectMapper(this.objectMapper);
        this.jobServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        this.jobServiceConfiguration.setCommandExecutor(this.commandExecutor);
        this.jobServiceConfiguration.setExpressionManager(this.expressionManager);
        this.jobServiceConfiguration.setBusinessCalendarManager(this.businessCalendarManager);

        this.jobServiceConfiguration.setJobHandlers(this.jobHandlers);
        this.jobServiceConfiguration.setFailedJobCommandFactory(this.failedJobCommandFactory);

        List<AsyncRunnableExecutionExceptionHandler> exceptionHandlers = new ArrayList<>();
        if (customAsyncRunnableExecutionExceptionHandlers != null) {
            exceptionHandlers.addAll(customAsyncRunnableExecutionExceptionHandlers);
        }

        if (addDefaultExceptionHandler) {
            exceptionHandlers.add(new DefaultAsyncRunnableExecutionExceptionHandler());
        }

        this.jobServiceConfiguration.setAsyncRunnableExecutionExceptionHandlers(exceptionHandlers);
        this.jobServiceConfiguration.setAsyncExecutorNumberOfRetries(this.asyncExecutorNumberOfRetries);
        this.jobServiceConfiguration.setAsyncExecutorResetExpiredJobsMaxTimeout(this.asyncExecutorResetExpiredJobsMaxTimeout);

        if (this.jobManager != null) {
            this.jobServiceConfiguration.setJobManager(this.jobManager);
        }

        if (this.internalJobManager != null) {
            this.jobServiceConfiguration.setInternalJobManager(this.internalJobManager);
        } else {
            this.jobServiceConfiguration.setInternalJobManager(new DefaultInternalCmmnJobManager(this));
        }

        this.jobServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG, this.jobServiceConfiguration);
    }

    public void initAsyncExecutor() {
        if (asyncExecutor == null) {
            DefaultAsyncJobExecutor defaultAsyncExecutor = new DefaultAsyncJobExecutor();
            if (asyncExecutorExecuteAsyncRunnableFactory != null) {
                defaultAsyncExecutor.setExecuteAsyncRunnableFactory(asyncExecutorExecuteAsyncRunnableFactory);
            }

            // Message queue mode
            defaultAsyncExecutor.setMessageQueueMode(asyncExecutorMessageQueueMode);

            // Thread pool config
            defaultAsyncExecutor.setCorePoolSize(asyncExecutorCorePoolSize);
            defaultAsyncExecutor.setMaxPoolSize(asyncExecutorMaxPoolSize);
            defaultAsyncExecutor.setKeepAliveTime(asyncExecutorThreadKeepAliveTime);

            // Threadpool queue
            if (asyncExecutorThreadPoolQueue != null) {
                defaultAsyncExecutor.setThreadPoolQueue(asyncExecutorThreadPoolQueue);
            }
            defaultAsyncExecutor.setQueueSize(asyncExecutorThreadPoolQueueSize);

            // Acquisition wait time
            defaultAsyncExecutor.setDefaultTimerJobAcquireWaitTimeInMillis(asyncExecutorDefaultTimerJobAcquireWaitTime);
            defaultAsyncExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(asyncExecutorDefaultAsyncJobAcquireWaitTime);

            // Queue full wait time
            defaultAsyncExecutor.setDefaultQueueSizeFullWaitTimeInMillis(asyncExecutorDefaultQueueSizeFullWaitTime);

            // Job locking
            defaultAsyncExecutor.setTimerLockTimeInMillis(asyncExecutorTimerLockTimeInMillis);
            defaultAsyncExecutor.setAsyncJobLockTimeInMillis(asyncExecutorAsyncJobLockTimeInMillis);
            if (asyncExecutorLockOwner != null) {
                defaultAsyncExecutor.setLockOwner(asyncExecutorLockOwner);
            }

            // Reset expired
            defaultAsyncExecutor.setResetExpiredJobsInterval(asyncExecutorResetExpiredJobsInterval);
            defaultAsyncExecutor.setResetExpiredJobsPageSize(asyncExecutorResetExpiredJobsPageSize);

            // Shutdown
            defaultAsyncExecutor.setSecondsToWaitOnShutdown(asyncExecutorSecondsToWaitOnShutdown);

            asyncExecutor = defaultAsyncExecutor;
        }

        asyncExecutor.setJobServiceConfiguration(jobServiceConfiguration);
        asyncExecutor.setAutoActivate(asyncExecutorActivate);
        jobServiceConfiguration.setAsyncExecutor(asyncExecutor);
    }

    @Override
    public String getEngineName() {
        return cmmnEngineName;
    }

    public String getCmmnEngineName() {
        return cmmnEngineName;
    }

    public CmmnEngineConfiguration setCmmnEngineName(String cmmnEngineName) {
        this.cmmnEngineName = cmmnEngineName;
        return this;
    }

    @Override
    public CmmnRuntimeService getCmmnRuntimeService() {
        return cmmnRuntimeService;
    }

    public CmmnEngineConfiguration setCmmnRuntimeService(CmmnRuntimeService cmmnRuntimeService) {
        this.cmmnRuntimeService = cmmnRuntimeService;
        return this;
    }

    public CmmnTaskService getCmmnTaskService() {
        return cmmnTaskService;
    }

    public CmmnEngineConfiguration setCmmnTaskService(CmmnTaskService cmmnTaskService) {
        this.cmmnTaskService = cmmnTaskService;
        return this;
    }

    @Override
    public CmmnManagementService getCmmnManagementService() {
        return cmmnManagementService;
    }

    public CmmnEngineConfiguration setCmmnManagementService(CmmnManagementService cmmnManagementService) {
        this.cmmnManagementService = cmmnManagementService;
        return this;
    }

    @Override
    public CmmnRepositoryService getCmmnRepositoryService() {
        return cmmnRepositoryService;
    }

    public CmmnEngineConfiguration setCmmnRepositoryService(CmmnRepositoryService cmmnRepositoryService) {
        this.cmmnRepositoryService = cmmnRepositoryService;
        return this;
    }

    @Override
    public CmmnHistoryService getCmmnHistoryService() {
        return cmmnHistoryService;
    }

    public CmmnEngineConfiguration setCmmnHistoryService(CmmnHistoryService cmmnHistoryService) {
        this.cmmnHistoryService = cmmnHistoryService;
        return this;
    }
    
    public IdmIdentityService getIdmIdentityService() {
        return ((IdmEngineConfiguration) engineConfigurations.get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)).getIdmIdentityService();
    }

    public CmmnEngineAgendaFactory getCmmnEngineAgendaFactory() {
        return cmmnEngineAgendaFactory;
    }

    public CmmnEngineConfiguration setCmmnEngineAgendaFactory(CmmnEngineAgendaFactory cmmnEngineAgendaFactory) {
        this.cmmnEngineAgendaFactory = cmmnEngineAgendaFactory;
        return this;
    }

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

    public CmmnEngineConfiguration setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    public CmmnDeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public CmmnEngineConfiguration setDeploymentDataManager(CmmnDeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public CmmnResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public CmmnEngineConfiguration setResourceDataManager(CmmnResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public CaseDefinitionDataManager getCaseDefinitionDataManager() {
        return caseDefinitionDataManager;
    }

    public CmmnEngineConfiguration setCaseDefinitionDataManager(CaseDefinitionDataManager caseDefinitionDataManager) {
        this.caseDefinitionDataManager = caseDefinitionDataManager;
        return this;
    }

    public CaseInstanceDataManager getCaseInstanceDataManager() {
        return caseInstanceDataManager;
    }

    public CmmnEngineConfiguration setCaseInstanceDataManager(CaseInstanceDataManager caseInstanceDataManager) {
        this.caseInstanceDataManager = caseInstanceDataManager;
        return this;
    }

    public PlanItemInstanceDataManager getPlanItemInstanceDataManager() {
        return planItemInstanceDataManager;
    }

    public CmmnEngineConfiguration setPlanItemInstanceDataManager(PlanItemInstanceDataManager planItemInstanceDataManager) {
        this.planItemInstanceDataManager = planItemInstanceDataManager;
        return this;
    }

    public SentryPartInstanceDataManager getSentryPartInstanceDataManager() {
        return sentryPartInstanceDataManager;
    }

    public CmmnEngineConfiguration setSentryPartInstanceDataManager(SentryPartInstanceDataManager sentryPartInstanceDataManager) {
        this.sentryPartInstanceDataManager = sentryPartInstanceDataManager;
        return this;
    }

    public MilestoneInstanceDataManager getMilestoneInstanceDataManager() {
        return milestoneInstanceDataManager;
    }

    public CmmnEngineConfiguration setMilestoneInstanceDataManager(MilestoneInstanceDataManager milestoneInstanceDataManager) {
        this.milestoneInstanceDataManager = milestoneInstanceDataManager;
        return this;
    }

    public HistoricCaseInstanceDataManager getHistoricCaseInstanceDataManager() {
        return historicCaseInstanceDataManager;
    }

    public CmmnEngineConfiguration setHistoricCaseInstanceDataManager(HistoricCaseInstanceDataManager historicCaseInstanceDataManager) {
        this.historicCaseInstanceDataManager = historicCaseInstanceDataManager;
        return this;
    }

    public HistoricMilestoneInstanceDataManager getHistoricMilestoneInstanceDataManager() {
        return historicMilestoneInstanceDataManager;
    }

    public CmmnEngineConfiguration setHistoricMilestoneInstanceDataManager(HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager) {
        this.historicMilestoneInstanceDataManager = historicMilestoneInstanceDataManager;
        return this;
    }

    public CmmnDeploymentEntityManager getCmmnDeploymentEntityManager() {
        return cmmnDeploymentEntityManager;
    }

    public CmmnEngineConfiguration setCmmnDeploymentEntityManager(CmmnDeploymentEntityManager cmmnDeploymentEntityManager) {
        this.cmmnDeploymentEntityManager = cmmnDeploymentEntityManager;
        return this;
    }

    public CmmnResourceEntityManager getCmmnResourceEntityManager() {
        return cmmnResourceEntityManager;
    }

    public CmmnEngineConfiguration setCmmnResourceEntityManager(CmmnResourceEntityManager cmmnResourceEntityManager) {
        this.cmmnResourceEntityManager = cmmnResourceEntityManager;
        return this;
    }

    public CaseDefinitionEntityManager getCaseDefinitionEntityManager() {
        return caseDefinitionEntityManager;
    }

    public CmmnEngineConfiguration setCaseDefinitionEntityManager(CaseDefinitionEntityManager caseDefinitionEntityManager) {
        this.caseDefinitionEntityManager = caseDefinitionEntityManager;
        return this;
    }

    public CaseInstanceEntityManager getCaseInstanceEntityManager() {
        return caseInstanceEntityManager;
    }

    public CmmnEngineConfiguration setCaseInstanceEntityManager(CaseInstanceEntityManager caseInstanceEntityManager) {
        this.caseInstanceEntityManager = caseInstanceEntityManager;
        return this;
    }

    public PlanItemInstanceEntityManager getPlanItemInstanceEntityManager() {
        return planItemInstanceEntityManager;
    }

    public CmmnEngineConfiguration setPlanItemInstanceEntityManager(PlanItemInstanceEntityManager planItemInstanceEntityManager) {
        this.planItemInstanceEntityManager = planItemInstanceEntityManager;
        return this;
    }

    public SentryPartInstanceEntityManager getSentryPartInstanceEntityManager() {
        return sentryPartInstanceEntityManager;
    }

    public CmmnEngineConfiguration setSentryPartInstanceEntityManager(SentryPartInstanceEntityManager sentryPartInstanceEntityManager) {
        this.sentryPartInstanceEntityManager = sentryPartInstanceEntityManager;
        return this;
    }

    public MilestoneInstanceEntityManager getMilestoneInstanceEntityManager() {
        return milestoneInstanceEntityManager;
    }

    public CmmnEngineConfiguration setMilestoneInstanceEntityManager(MilestoneInstanceEntityManager milestoneInstanceEntityManager) {
        this.milestoneInstanceEntityManager = milestoneInstanceEntityManager;
        return this;
    }

    public HistoricCaseInstanceEntityManager getHistoricCaseInstanceEntityManager() {
        return historicCaseInstanceEntityManager;
    }

    public CmmnEngineConfiguration setHistoricCaseInstanceEntityManager(HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager) {
        this.historicCaseInstanceEntityManager = historicCaseInstanceEntityManager;
        return this;
    }

    public HistoricMilestoneInstanceEntityManager getHistoricMilestoneInstanceEntityManager() {
        return historicMilestoneInstanceEntityManager;
    }

    public CmmnEngineConfiguration setHistoricMilestoneInstanceEntityManager(HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager) {
        this.historicMilestoneInstanceEntityManager = historicMilestoneInstanceEntityManager;
        return this;
    }

    public CaseInstanceHelper getCaseInstanceHelper() {
        return caseInstanceHelper;
    }

    public CmmnEngineConfiguration setCaseInstanceHelper(CaseInstanceHelper caseInstanceHelper) {
        this.caseInstanceHelper = caseInstanceHelper;
        return this;
    }

    public CmmnHistoryManager getCmmnHistoryManager() {
        return cmmnHistoryManager;
    }

    public CmmnEngineConfiguration setCmmnHistoryManager(CmmnHistoryManager cmmnHistoryManager) {
        this.cmmnHistoryManager = cmmnHistoryManager;
        return this;
    }

    public boolean isEnableSafeCmmnXml() {
        return enableSafeCmmnXml;
    }

    public CmmnEngineConfiguration setEnableSafeCmmnXml(boolean enableSafeCmmnXml) {
        this.enableSafeCmmnXml = enableSafeCmmnXml;
        return this;
    }

    public CmmnParser getCmmnParser() {
        return cmmnParser;
    }

    public CmmnEngineConfiguration setCmmnParser(CmmnParser cmmnParser) {
        this.cmmnParser = cmmnParser;
        return this;
    }

    public CmmnDeployer getCmmnDeployer() {
        return cmmnDeployer;
    }

    public CmmnEngineConfiguration setCmmnDeployer(CmmnDeployer cmmnDeployer) {
        this.cmmnDeployer = cmmnDeployer;
        return this;
    }

    public CmmnDeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public CmmnEngineConfiguration setDeploymentManager(CmmnDeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
        return this;
    }

    public CaseDefinitionDiagramHelper getCaseDefinitionDiagramHelper() {
        return caseDefinitionDiagramHelper;
    }

    public CmmnEngineConfiguration setCaseDefinitionDiagramHelper(CaseDefinitionDiagramHelper caseDefinitionDiagramHelper) {
        this.caseDefinitionDiagramHelper = caseDefinitionDiagramHelper;
        return this;
    }

    public CmmnActivityBehaviorFactory getActivityBehaviorFactory() {
        return activityBehaviorFactory;
    }

    public CmmnEngineConfiguration setActivityBehaviorFactory(CmmnActivityBehaviorFactory activityBehaviorFactory) {
        this.activityBehaviorFactory = activityBehaviorFactory;
        return this;
    }

    public CmmnClassDelegateFactory getClassDelegateFactory() {
        return classDelegateFactory;
    }

    public CmmnEngineConfiguration setClassDelegateFactory(CmmnClassDelegateFactory classDelegateFactory) {
        this.classDelegateFactory = classDelegateFactory;
        return this;
    }

    public int getCaseDefinitionCacheLimit() {
        return caseDefinitionCacheLimit;
    }

    public CmmnEngineConfiguration setCaseDefinitionCacheLimit(int caseDefinitionCacheLimit) {
        this.caseDefinitionCacheLimit = caseDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<CaseDefinitionCacheEntry> getCaseDefinitionCache() {
        return caseDefinitionCache;
    }

    public CmmnEngineConfiguration setCaseDefinitionCache(DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache) {
        this.caseDefinitionCache = caseDefinitionCache;
        return this;
    }

    public ProcessInstanceService getProcessInstanceService() {
        return processInstanceService;
    }

    public CmmnEngineConfiguration setProcessInstanceService(ProcessInstanceService processInstanceService) {
        this.processInstanceService = processInstanceService;
        return this;
    }

    public Map<String, List<RuntimeInstanceStateChangeCallback>> getCaseInstanceStateChangeCallbacks() {
        return caseInstanceStateChangeCallbacks;
    }

    public CmmnEngineConfiguration setCaseInstanceStateChangeCallbacks(Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceStateChangeCallbacks) {
        this.caseInstanceStateChangeCallbacks = caseInstanceStateChangeCallbacks;
        return this;
    }

    @Override
    public CmmnEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public boolean isExecuteServiceDbSchemaManagers() {
        return executeServiceDbSchemaManagers;
    }

    public void setExecuteServiceDbSchemaManagers(boolean executeServiceDbSchemaManagers) {
        this.executeServiceDbSchemaManagers = executeServiceDbSchemaManagers;
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public CmmnEngineConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }

    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    public CmmnEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public DelegateExpressionFieldInjectionMode getDelegateExpressionFieldInjectionMode() {
        return delegateExpressionFieldInjectionMode;
    }

    public CmmnEngineConfiguration setDelegateExpressionFieldInjectionMode(DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode) {
        this.delegateExpressionFieldInjectionMode = delegateExpressionFieldInjectionMode;
        return this;
    }

    public List<FlowableFunctionDelegate> getFlowableFunctionDelegates() {
        return flowableFunctionDelegates;
    }

    public CmmnEngineConfiguration setFlowableFunctionDelegates(List<FlowableFunctionDelegate> flowableFunctionDelegates) {
        this.flowableFunctionDelegates = flowableFunctionDelegates;
        return this;
    }

    public List<FlowableFunctionDelegate> getCustomFlowableFunctionDelegates() {
        return customFlowableFunctionDelegates;
    }

    public CmmnEngineConfiguration setCustomFlowableFunctionDelegates(List<FlowableFunctionDelegate> customFlowableFunctionDelegates) {
        this.customFlowableFunctionDelegates = customFlowableFunctionDelegates;
        return this;
    }

    public DbSchemaManager getIdentityLinkDbSchemaManager() {
        return identityLinkDbSchemaManager;
    }

    public CmmnEngineConfiguration setIdentityLinkDbSchemaManager(DbSchemaManager identityLinkDbSchemaManager) {
        this.identityLinkDbSchemaManager = identityLinkDbSchemaManager;
        return this;
    }

    public DbSchemaManager getVariableDbSchemaManager() {
        return variableDbSchemaManager;
    }

    public CmmnEngineConfiguration setVariableDbSchemaManager(DbSchemaManager variableDbSchemaManager) {
        this.variableDbSchemaManager = variableDbSchemaManager;
        return this;
    }

    public DbSchemaManager getTaskDbSchemaManager() {
        return taskDbSchemaManager;
    }

    public CmmnEngineConfiguration setTaskDbSchemaManager(DbSchemaManager taskDbSchemaManager) {
        this.taskDbSchemaManager = taskDbSchemaManager;
        return this;
    }

    public DbSchemaManager getJobDbSchemaManager() {
        return jobDbSchemaManager;
    }

    public CmmnEngineConfiguration setJobDbSchemaManager(DbSchemaManager jobDbSchemaManager) {
        this.jobDbSchemaManager = jobDbSchemaManager;
        return this;
    }

    public VariableTypes getVariableTypes() {
        return variableTypes;
    }

    public CmmnEngineConfiguration setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
        return this;
    }

    public List<VariableType> getCustomPreVariableTypes() {
        return customPreVariableTypes;
    }

    public CmmnEngineConfiguration setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
        this.customPreVariableTypes = customPreVariableTypes;
        return this;
    }

    public List<VariableType> getCustomPostVariableTypes() {
        return customPostVariableTypes;
    }

    public CmmnEngineConfiguration setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
        this.customPostVariableTypes = customPostVariableTypes;
        return this;
    }

    public IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return identityLinkServiceConfiguration;
    }

    public CmmnEngineConfiguration setIdentityLinkServiceConfiguration(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
        return this;
    }

    public VariableServiceConfiguration getVariableServiceConfiguration() {
        return variableServiceConfiguration;
    }

    public CmmnEngineConfiguration setVariableServiceConfiguration(VariableServiceConfiguration variableServiceConfiguration) {
        this.variableServiceConfiguration = variableServiceConfiguration;
        return this;
    }

    public TaskServiceConfiguration getTaskServiceConfiguration() {
        return taskServiceConfiguration;
    }

    public CmmnEngineConfiguration setTaskServiceConfiguration(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
        return this;
    }

    public InternalHistoryTaskManager getInternalHistoryTaskManager() {
        return internalHistoryTaskManager;
    }

    public CmmnEngineConfiguration setInternalHistoryTaskManager(InternalHistoryTaskManager internalHistoryTaskManager) {
        this.internalHistoryTaskManager = internalHistoryTaskManager;
        return this;
    }

    public InternalTaskVariableScopeResolver getInternalTaskVariableScopeResolver() {
        return internalTaskVariableScopeResolver;
    }

    public CmmnEngineConfiguration setInternalTaskVariableScopeResolver(InternalTaskVariableScopeResolver internalTaskVariableScopeResolver) {
        this.internalTaskVariableScopeResolver = internalTaskVariableScopeResolver;
        return this;
    }

    public boolean isEnableTaskRelationshipCounts() {
        return isEnableTaskRelationshipCounts;
    }

    public CmmnEngineConfiguration setEnableTaskRelationshipCounts(boolean isEnableTaskRelationshipCounts) {
        this.isEnableTaskRelationshipCounts = isEnableTaskRelationshipCounts;
        return this;
    }

    public int getTaskQueryLimit() {
        return taskQueryLimit;
    }

    public CmmnEngineConfiguration setTaskQueryLimit(int taskQueryLimit) {
        this.taskQueryLimit = taskQueryLimit;
        return this;
    }

    public int getHistoricTaskQueryLimit() {
        return historicTaskQueryLimit;
    }

    public CmmnEngineConfiguration setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
        this.historicTaskQueryLimit = historicTaskQueryLimit;
        return this;
    }

    public InternalHistoryVariableManager getInternalHistoryVariableManager() {
        return internalHistoryVariableManager;
    }

    public CmmnEngineConfiguration setInternalHistoryVariableManager(InternalHistoryVariableManager internalHistoryVariableManager) {
        this.internalHistoryVariableManager = internalHistoryVariableManager;
        return this;
    }

    public int getCaseQueryLimit() {
        return caseQueryLimit;
    }

    public CmmnEngineConfiguration setCaseQueryLimit(int caseQueryLimit) {
        this.caseQueryLimit = caseQueryLimit;
        return this;
    }

    public boolean isSerializableVariableTypeTrackDeserializedObjects() {
        return serializableVariableTypeTrackDeserializedObjects;
    }

    public CmmnEngineConfiguration setSerializableVariableTypeTrackDeserializedObjects(boolean serializableVariableTypeTrackDeserializedObjects) {
        this.serializableVariableTypeTrackDeserializedObjects = serializableVariableTypeTrackDeserializedObjects;
        return this;
    }

    public CaseDiagramGenerator getCaseDiagramGenerator() {
        return caseDiagramGenerator;
    }

    public CmmnEngineConfiguration setCaseDiagramGenerator(CaseDiagramGenerator caseDiagramGenerator) {
        this.caseDiagramGenerator = caseDiagramGenerator;
        return this;
    }

    public boolean isCreateDiagramOnDeploy() {
        return isCreateDiagramOnDeploy;
    }

    public CmmnEngineConfiguration setCreateDiagramOnDeploy(boolean isCreateDiagramOnDeploy) {
        this.isCreateDiagramOnDeploy = isCreateDiagramOnDeploy;
        return this;
    }

    public String getActivityFontName() {
        return activityFontName;
    }

    public CmmnEngineConfiguration setActivityFontName(String activityFontName) {
        this.activityFontName = activityFontName;
        return this;
    }

    public String getLabelFontName() {
        return labelFontName;
    }

    public CmmnEngineConfiguration setLabelFontName(String labelFontName) {
        this.labelFontName = labelFontName;
        return this;
    }

    public String getAnnotationFontName() {
        return annotationFontName;
    }

    public CmmnEngineConfiguration setAnnotationFontName(String annotationFontName) {
        this.annotationFontName = annotationFontName;
        return this;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public CmmnEngineConfiguration setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        return this;
    }

    public boolean isDisableIdmEngine() {
        return disableIdmEngine;
    }

    public CmmnEngineConfiguration setDisableIdmEngine(boolean disableIdmEngine) {
        this.disableIdmEngine = disableIdmEngine;
        return this;
    }

    public boolean isEnableConfiguratorServiceLoader() {
        return enableConfiguratorServiceLoader;
    }

    public CmmnEngineConfiguration setEnableConfiguratorServiceLoader(boolean enableConfiguratorServiceLoader) {
        this.enableConfiguratorServiceLoader = enableConfiguratorServiceLoader;
        return this;
    }

    public List<EngineConfigurator> getConfigurators() {
        return configurators;
    }

    public CmmnEngineConfiguration setConfigurators(List<EngineConfigurator> configurators) {
        this.configurators = configurators;
        return this;
    }

    public EngineConfigurator getIdmEngineConfigurator() {
        return idmEngineConfigurator;
    }

    public CmmnEngineConfiguration setIdmEngineConfigurator(EngineConfigurator idmEngineConfigurator) {
        this.idmEngineConfigurator = idmEngineConfigurator;
        return this;
    }

    public JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    public CmmnEngineConfiguration setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
        return this;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public CmmnEngineConfiguration setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
        return this;
    }

    public List<JobHandler> getCustomJobHandlers() {
        return customJobHandlers;
    }

    public CmmnEngineConfiguration setCustomJobHandlers(List<JobHandler> customJobHandlers) {
        this.customJobHandlers = customJobHandlers;
        return this;
    }

    public Map<String, JobHandler> getJobHandlers() {
        return jobHandlers;
    }

    public CmmnEngineConfiguration setJobHandlers(Map<String, JobHandler> jobHandlers) {
        this.jobHandlers = jobHandlers;
        return this;
    }

    public InternalJobManager getInternalJobManager() {
        return internalJobManager;
    }

    public CmmnEngineConfiguration setInternalJobManager(InternalJobManager internalJobManager) {
        this.internalJobManager = internalJobManager;
        return this;
    }

    public List<AsyncRunnableExecutionExceptionHandler> getCustomAsyncRunnableExecutionExceptionHandlers() {
        return customAsyncRunnableExecutionExceptionHandlers;
    }

    public CmmnEngineConfiguration setCustomAsyncRunnableExecutionExceptionHandlers(
            List<AsyncRunnableExecutionExceptionHandler> customAsyncRunnableExecutionExceptionHandlers) {
        this.customAsyncRunnableExecutionExceptionHandlers = customAsyncRunnableExecutionExceptionHandlers;
        return this;
    }

    public boolean isAddDefaultExceptionHandler() {
        return addDefaultExceptionHandler;
    }

    public CmmnEngineConfiguration setAddDefaultExceptionHandler(boolean addDefaultExceptionHandler) {
        this.addDefaultExceptionHandler = addDefaultExceptionHandler;
        return this;
    }

    public FailedJobCommandFactory getFailedJobCommandFactory() {
        return failedJobCommandFactory;
    }

    public CmmnEngineConfiguration setFailedJobCommandFactory(FailedJobCommandFactory failedJobCommandFactory) {
        this.failedJobCommandFactory = failedJobCommandFactory;
        return this;
    }

    public BusinessCalendarManager getBusinessCalendarManager() {
        return businessCalendarManager;
    }

    public CmmnEngineConfiguration setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
        this.businessCalendarManager = businessCalendarManager;
        return this;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public CmmnEngineConfiguration setAsyncExecutor(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        return this;
    }

    public boolean isAsyncExecutorActivate() {
        return asyncExecutorActivate;
    }

    public CmmnEngineConfiguration setAsyncExecutorActivate(boolean asyncExecutorActivate) {
        this.asyncExecutorActivate = asyncExecutorActivate;
        return this;
    }

    public boolean isAsyncExecutorMessageQueueMode() {
        return asyncExecutorMessageQueueMode;
    }

    public CmmnEngineConfiguration setAsyncExecutorMessageQueueMode(boolean asyncExecutorMessageQueueMode) {
        this.asyncExecutorMessageQueueMode = asyncExecutorMessageQueueMode;
        return this;
    }

    public int getAsyncExecutorNumberOfRetries() {
        return asyncExecutorNumberOfRetries;
    }

    public CmmnEngineConfiguration setAsyncExecutorNumberOfRetries(int asyncExecutorNumberOfRetries) {
        this.asyncExecutorNumberOfRetries = asyncExecutorNumberOfRetries;
        return this;
    }

    public int getLockTimeAsyncJobWaitTime() {
        return lockTimeAsyncJobWaitTime;
    }

    public CmmnEngineConfiguration setLockTimeAsyncJobWaitTime(int lockTimeAsyncJobWaitTime) {
        this.lockTimeAsyncJobWaitTime = lockTimeAsyncJobWaitTime;
        return this;
    }

    public int getDefaultFailedJobWaitTime() {
        return defaultFailedJobWaitTime;
    }

    public CmmnEngineConfiguration setDefaultFailedJobWaitTime(int defaultFailedJobWaitTime) {
        this.defaultFailedJobWaitTime = defaultFailedJobWaitTime;
        return this;
    }

    public int getAsyncFailedJobWaitTime() {
        return asyncFailedJobWaitTime;
    }

    public CmmnEngineConfiguration setAsyncFailedJobWaitTime(int asyncFailedJobWaitTime) {
        this.asyncFailedJobWaitTime = asyncFailedJobWaitTime;
        return this;
    }

    public int getAsyncExecutorCorePoolSize() {
        return asyncExecutorCorePoolSize;
    }

    public CmmnEngineConfiguration setAsyncExecutorCorePoolSize(int asyncExecutorCorePoolSize) {
        this.asyncExecutorCorePoolSize = asyncExecutorCorePoolSize;
        return this;
    }

    public int getAsyncExecutorMaxPoolSize() {
        return asyncExecutorMaxPoolSize;
    }

    public CmmnEngineConfiguration setAsyncExecutorMaxPoolSize(int asyncExecutorMaxPoolSize) {
        this.asyncExecutorMaxPoolSize = asyncExecutorMaxPoolSize;
        return this;
    }

    public long getAsyncExecutorThreadKeepAliveTime() {
        return asyncExecutorThreadKeepAliveTime;
    }

    public CmmnEngineConfiguration setAsyncExecutorThreadKeepAliveTime(long asyncExecutorThreadKeepAliveTime) {
        this.asyncExecutorThreadKeepAliveTime = asyncExecutorThreadKeepAliveTime;
        return this;
    }

    public int getAsyncExecutorThreadPoolQueueSize() {
        return asyncExecutorThreadPoolQueueSize;
    }

    public CmmnEngineConfiguration setAsyncExecutorThreadPoolQueueSize(int asyncExecutorThreadPoolQueueSize) {
        this.asyncExecutorThreadPoolQueueSize = asyncExecutorThreadPoolQueueSize;
        return this;
    }

    public BlockingQueue<Runnable> getAsyncExecutorThreadPoolQueue() {
        return asyncExecutorThreadPoolQueue;
    }

    public CmmnEngineConfiguration setAsyncExecutorThreadPoolQueue(BlockingQueue<Runnable> asyncExecutorThreadPoolQueue) {
        this.asyncExecutorThreadPoolQueue = asyncExecutorThreadPoolQueue;
        return this;
    }

    public long getAsyncExecutorSecondsToWaitOnShutdown() {
        return asyncExecutorSecondsToWaitOnShutdown;
    }

    public CmmnEngineConfiguration setAsyncExecutorSecondsToWaitOnShutdown(long asyncExecutorSecondsToWaitOnShutdown) {
        this.asyncExecutorSecondsToWaitOnShutdown = asyncExecutorSecondsToWaitOnShutdown;
        return this;
    }

    public int getAsyncExecutorMaxTimerJobsPerAcquisition() {
        return asyncExecutorMaxTimerJobsPerAcquisition;
    }

    public CmmnEngineConfiguration setAsyncExecutorMaxTimerJobsPerAcquisition(int asyncExecutorMaxTimerJobsPerAcquisition) {
        this.asyncExecutorMaxTimerJobsPerAcquisition = asyncExecutorMaxTimerJobsPerAcquisition;
        return this;
    }

    public int getAsyncExecutorMaxAsyncJobsDuePerAcquisition() {
        return asyncExecutorMaxAsyncJobsDuePerAcquisition;
    }

    public CmmnEngineConfiguration setAsyncExecutorMaxAsyncJobsDuePerAcquisition(int asyncExecutorMaxAsyncJobsDuePerAcquisition) {
        this.asyncExecutorMaxAsyncJobsDuePerAcquisition = asyncExecutorMaxAsyncJobsDuePerAcquisition;
        return this;
    }

    public int getAsyncExecutorDefaultTimerJobAcquireWaitTime() {
        return asyncExecutorDefaultTimerJobAcquireWaitTime;
    }

    public CmmnEngineConfiguration setAsyncExecutorDefaultTimerJobAcquireWaitTime(int asyncExecutorDefaultTimerJobAcquireWaitTime) {
        this.asyncExecutorDefaultTimerJobAcquireWaitTime = asyncExecutorDefaultTimerJobAcquireWaitTime;
        return this;
    }

    public int getAsyncExecutorDefaultAsyncJobAcquireWaitTime() {
        return asyncExecutorDefaultAsyncJobAcquireWaitTime;
    }

    public CmmnEngineConfiguration setAsyncExecutorDefaultAsyncJobAcquireWaitTime(int asyncExecutorDefaultAsyncJobAcquireWaitTime) {
        this.asyncExecutorDefaultAsyncJobAcquireWaitTime = asyncExecutorDefaultAsyncJobAcquireWaitTime;
        return this;
    }

    public int getAsyncExecutorDefaultQueueSizeFullWaitTime() {
        return asyncExecutorDefaultQueueSizeFullWaitTime;
    }

    public CmmnEngineConfiguration setAsyncExecutorDefaultQueueSizeFullWaitTime(int asyncExecutorDefaultQueueSizeFullWaitTime) {
        this.asyncExecutorDefaultQueueSizeFullWaitTime = asyncExecutorDefaultQueueSizeFullWaitTime;
        return this;
    }

    public String getAsyncExecutorLockOwner() {
        return asyncExecutorLockOwner;
    }

    public CmmnEngineConfiguration setAsyncExecutorLockOwner(String asyncExecutorLockOwner) {
        this.asyncExecutorLockOwner = asyncExecutorLockOwner;
        return this;
    }

    public int getAsyncExecutorTimerLockTimeInMillis() {
        return asyncExecutorTimerLockTimeInMillis;
    }

    public CmmnEngineConfiguration setAsyncExecutorTimerLockTimeInMillis(int asyncExecutorTimerLockTimeInMillis) {
        this.asyncExecutorTimerLockTimeInMillis = asyncExecutorTimerLockTimeInMillis;
        return this;
    }

    public int getAsyncExecutorAsyncJobLockTimeInMillis() {
        return asyncExecutorAsyncJobLockTimeInMillis;
    }

    public CmmnEngineConfiguration setAsyncExecutorAsyncJobLockTimeInMillis(int asyncExecutorAsyncJobLockTimeInMillis) {
        this.asyncExecutorAsyncJobLockTimeInMillis = asyncExecutorAsyncJobLockTimeInMillis;
        return this;
    }

    public int getAsyncExecutorResetExpiredJobsInterval() {
        return asyncExecutorResetExpiredJobsInterval;
    }

    public CmmnEngineConfiguration setAsyncExecutorResetExpiredJobsInterval(int asyncExecutorResetExpiredJobsInterval) {
        this.asyncExecutorResetExpiredJobsInterval = asyncExecutorResetExpiredJobsInterval;
        return this;
    }

    public int getAsyncExecutorResetExpiredJobsMaxTimeout() {
        return asyncExecutorResetExpiredJobsMaxTimeout;
    }

    public CmmnEngineConfiguration setAsyncExecutorResetExpiredJobsMaxTimeout(int asyncExecutorResetExpiredJobsMaxTimeout) {
        this.asyncExecutorResetExpiredJobsMaxTimeout = asyncExecutorResetExpiredJobsMaxTimeout;
        return this;
    }

    public int getAsyncExecutorResetExpiredJobsPageSize() {
        return asyncExecutorResetExpiredJobsPageSize;
    }

    public CmmnEngineConfiguration setAsyncExecutorResetExpiredJobsPageSize(int asyncExecutorResetExpiredJobsPageSize) {
        this.asyncExecutorResetExpiredJobsPageSize = asyncExecutorResetExpiredJobsPageSize;
        return this;
    }

    public ExecuteAsyncRunnableFactory getAsyncExecutorExecuteAsyncRunnableFactory() {
        return asyncExecutorExecuteAsyncRunnableFactory;
    }

    public CmmnEngineConfiguration setAsyncExecutorExecuteAsyncRunnableFactory(
            ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory) {
        this.asyncExecutorExecuteAsyncRunnableFactory = asyncExecutorExecuteAsyncRunnableFactory;
        return this;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public void setHttpClientConfig(HttpClientConfig httpClientConfig) {
        this.httpClientConfig.merge(httpClientConfig);
    }

    public FormFieldHandler getFormFieldHandler() {
        return formFieldHandler;
    }

    public void setFormFieldHandler(FormFieldHandler formFieldHandler) {
        this.formFieldHandler = formFieldHandler;
    }
}
