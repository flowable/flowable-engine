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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;

import javax.sql.DataSource;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.flowable.cmmn.api.CallbackTypes;
import org.flowable.cmmn.api.CandidateManager;
import org.flowable.cmmn.api.CmmnEngineConfigurationApi;
import org.flowable.cmmn.api.CmmnHistoryCleaningManager;
import org.flowable.cmmn.api.CmmnHistoryService;
import org.flowable.cmmn.api.CmmnManagementService;
import org.flowable.cmmn.api.CmmnMigrationService;
import org.flowable.cmmn.api.CmmnRepositoryService;
import org.flowable.cmmn.api.CmmnRuntimeService;
import org.flowable.cmmn.api.CmmnTaskService;
import org.flowable.cmmn.api.DecisionTableVariableManager;
import org.flowable.cmmn.api.DynamicCmmnService;
import org.flowable.cmmn.api.delegate.PlanItemVariableAggregator;
import org.flowable.cmmn.api.listener.CaseInstanceLifecycleListener;
import org.flowable.cmmn.api.listener.PlanItemInstanceLifecycleListener;
import org.flowable.cmmn.engine.impl.CmmnEngineImpl;
import org.flowable.cmmn.engine.impl.CmmnHistoryServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnManagementServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnRepositoryServiceImpl;
import org.flowable.cmmn.engine.impl.CmmnTaskServiceImpl;
import org.flowable.cmmn.engine.impl.DefaultCmmnHistoryCleaningManager;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.agenda.CmmnEngineAgendaSessionFactory;
import org.flowable.cmmn.engine.impl.agenda.DefaultCmmnEngineAgendaFactory;
import org.flowable.cmmn.engine.impl.callback.ChildBpmnCaseInstanceStateChangeCallback;
import org.flowable.cmmn.engine.impl.callback.ChildCaseInstanceStateChangeCallback;
import org.flowable.cmmn.engine.impl.callback.DefaultInternalCmmnJobManager;
import org.flowable.cmmn.engine.impl.cfg.DefaultTaskAssignmentManager;
import org.flowable.cmmn.engine.impl.cfg.DelegateExpressionFieldInjectionMode;
import org.flowable.cmmn.engine.impl.cfg.StandaloneInMemCmmnEngineConfiguration;
import org.flowable.cmmn.engine.impl.cmd.JobRetryCmd;
import org.flowable.cmmn.engine.impl.cmd.SchemaOperationsCmmnEngineBuild;
import org.flowable.cmmn.engine.impl.db.CmmnDbSchemaManager;
import org.flowable.cmmn.engine.impl.db.EntityDependencyOrder;
import org.flowable.cmmn.engine.impl.delegate.CmmnClassDelegateFactory;
import org.flowable.cmmn.engine.impl.delegate.DefaultCmmnClassDelegateFactory;
import org.flowable.cmmn.engine.impl.delegate.JsonPlanItemVariableAggregator;
import org.flowable.cmmn.engine.impl.deployer.CaseDefinitionDiagramHelper;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeployer;
import org.flowable.cmmn.engine.impl.deployer.CmmnDeploymentManager;
import org.flowable.cmmn.engine.impl.el.CmmnExpressionManager;
import org.flowable.cmmn.engine.impl.eventregistry.CmmnEventRegistryEventConsumer;
import org.flowable.cmmn.engine.impl.form.DefaultFormFieldHandler;
import org.flowable.cmmn.engine.impl.function.IsPlanItemCompletedExpressionFunction;
import org.flowable.cmmn.engine.impl.function.IsStageCompletableExpressionFunction;
import org.flowable.cmmn.engine.impl.function.TaskGetFunctionDelegate;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryTaskManager;
import org.flowable.cmmn.engine.impl.history.CmmnHistoryVariableManager;
import org.flowable.cmmn.engine.impl.history.DefaultCmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.async.AsyncCmmnHistoryManager;
import org.flowable.cmmn.engine.impl.history.async.CmmnAsyncHistoryConstants;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.CaseInstanceEndHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.CaseInstanceReactivateHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.CaseInstanceStartHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.CaseInstanceUpdateBusinessKeyHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.CaseInstanceUpdateNameHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.EntityLinkCreatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.EntityLinkDeletedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.HistoricCaseInstanceDeletedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.HistoricUserTaskLogDeleteJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.HistoricUserTaskLogRecordJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.IdentityLinkCreatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.IdentityLinkDeletedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.MilestoneReachedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceAvailableHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceCompletedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceCreatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceDisabledHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceEnabledHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceExitHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceFullHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceOccurredHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceStartedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceSuspendedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.PlanItemInstanceTerminatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.TaskCreatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.TaskEndedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.TaskUpdatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.UpdateCaseDefinitionCascadeHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.VariableCreatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.VariableRemovedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.history.async.json.transformer.VariableUpdatedHistoryJsonTransformer;
import org.flowable.cmmn.engine.impl.idm.DefaultCandidateManager;
import org.flowable.cmmn.engine.impl.interceptor.CmmnCommandInvoker;
import org.flowable.cmmn.engine.impl.interceptor.DefaultCmmnIdentityLinkInterceptor;
import org.flowable.cmmn.engine.impl.job.AsyncActivatePlanItemInstanceJobHandler;
import org.flowable.cmmn.engine.impl.job.AsyncInitializePlanModelJobHandler;
import org.flowable.cmmn.engine.impl.job.CmmnHistoryCleanupJobHandler;
import org.flowable.cmmn.engine.impl.job.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.cmmn.engine.impl.job.TriggerTimerEventJobHandler;
import org.flowable.cmmn.engine.impl.listener.CmmnListenerFactory;
import org.flowable.cmmn.engine.impl.listener.CmmnListenerNotificationHelper;
import org.flowable.cmmn.engine.impl.listener.DefaultCmmnListenerFactory;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationManager;
import org.flowable.cmmn.engine.impl.migration.CaseInstanceMigrationManagerImpl;
import org.flowable.cmmn.engine.impl.migration.CmmnMigrationServiceImpl;
import org.flowable.cmmn.engine.impl.parser.CmmnActivityBehaviorFactory;
import org.flowable.cmmn.engine.impl.parser.CmmnParseHandler;
import org.flowable.cmmn.engine.impl.parser.CmmnParseHandlers;
import org.flowable.cmmn.engine.impl.parser.CmmnParser;
import org.flowable.cmmn.engine.impl.parser.CmmnParserImpl;
import org.flowable.cmmn.engine.impl.parser.DefaultCmmnActivityBehaviorFactory;
import org.flowable.cmmn.engine.impl.parser.handler.CasePageTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.CaseParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.CaseTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.DecisionTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.ExternalWorkerServiceTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.GenericEventListenerParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.HttpTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.HumanTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.MilestoneParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.PlanFragmentParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.ProcessTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.ReactivateEventListenerParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.ScriptTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.SendEventServiceTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.ServiceTaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.SignalEventListenerParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.StageParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.TaskParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.TimerEventListenerParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.UserEventListenerParseHandler;
import org.flowable.cmmn.engine.impl.parser.handler.VariableEventListenerParseHandler;
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
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManager;
import org.flowable.cmmn.engine.impl.persistence.entity.HistoricPlanItemInstanceEntityManagerImpl;
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
import org.flowable.cmmn.engine.impl.persistence.entity.data.HistoricPlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.MilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.PlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.SentryPartInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseDefinitionDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisCmmnDeploymentDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricCaseInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisHistoricPlanItemInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisMilestoneInstanceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisPlanItemInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.cmmn.engine.impl.persistence.entity.data.impl.MybatisSentryPartInstanceDataManagerImpl;
import org.flowable.cmmn.engine.impl.persistence.entity.deploy.CaseDefinitionCacheEntry;
import org.flowable.cmmn.engine.impl.process.ProcessInstanceService;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelper;
import org.flowable.cmmn.engine.impl.runtime.CaseInstanceHelperImpl;
import org.flowable.cmmn.engine.impl.runtime.CmmnDynamicStateManager;
import org.flowable.cmmn.engine.impl.runtime.CmmnRuntimeServiceImpl;
import org.flowable.cmmn.engine.impl.runtime.DefaultCmmnDynamicStateManager;
import org.flowable.cmmn.engine.impl.runtime.DynamicCmmnServiceImpl;
import org.flowable.cmmn.engine.impl.scripting.CmmnVariableScopeResolverFactory;
import org.flowable.cmmn.engine.impl.task.DefaultCmmnTaskVariableScopeResolver;
import org.flowable.cmmn.engine.impl.variable.CmmnAggregatedVariableType;
import org.flowable.cmmn.engine.interceptor.CmmnIdentityLinkInterceptor;
import org.flowable.cmmn.engine.interceptor.CreateCasePageTaskInterceptor;
import org.flowable.cmmn.engine.interceptor.CreateCmmnExternalWorkerJobInterceptor;
import org.flowable.cmmn.engine.interceptor.CreateHumanTaskInterceptor;
import org.flowable.cmmn.engine.interceptor.StartCaseInstanceInterceptor;
import org.flowable.cmmn.image.CaseDiagramGenerator;
import org.flowable.cmmn.image.impl.DefaultCaseDiagramGenerator;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.async.AsyncTaskExecutor;
import org.flowable.common.engine.api.async.AsyncTaskInvoker;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.scope.ScopeTypes;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;
import org.flowable.common.engine.impl.EngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.HasExpressionManagerEngineConfiguration;
import org.flowable.common.engine.impl.HasVariableServiceConfiguration;
import org.flowable.common.engine.impl.HasVariableTypes;
import org.flowable.common.engine.impl.ScriptingEngineAwareEngineConfiguration;
import org.flowable.common.engine.impl.async.DefaultAsyncTaskExecutor;
import org.flowable.common.engine.impl.async.DefaultAsyncTaskInvoker;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DurationBusinessCalendar;
import org.flowable.common.engine.impl.calendar.MapBusinessCalendarManager;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.cfg.BeansConfigurationHelper;
import org.flowable.common.engine.impl.cfg.mail.MailServerInfo;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.SchemaManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.el.FlowableAstFunctionCreator;
import org.flowable.common.engine.impl.el.function.VariableBase64ExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableContainsAnyExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableContainsExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableEqualsExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableExistsExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableGetExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableGetOrDefaultExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableGreaterThanExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableGreaterThanOrEqualsExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableIsEmptyExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableIsNotEmptyExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableLowerThanExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableLowerThanOrEqualsExpressionFunction;
import org.flowable.common.engine.impl.el.function.VariableNotEqualsExpressionFunction;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.common.engine.impl.persistence.entity.TableDataManager;
import org.flowable.common.engine.impl.scripting.BeansResolverFactory;
import org.flowable.common.engine.impl.scripting.ResolverFactory;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionFactory;
import org.flowable.entitylink.service.EntityLinkServiceConfiguration;
import org.flowable.entitylink.service.impl.db.EntityLinkDbSchemaManager;
import org.flowable.eventregistry.api.EventRegistryEventConsumer;
import org.flowable.eventregistry.impl.configurator.EventRegistryEngineConfigurator;
import org.flowable.eventsubscription.service.EventSubscriptionServiceConfiguration;
import org.flowable.eventsubscription.service.impl.db.EventSubscriptionDbSchemaManager;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.identitylink.service.IdentityLinkEventHandler;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.db.IdentityLinkDbSchemaManager;
import org.flowable.idm.api.IdmEngineConfigurationApi;
import org.flowable.idm.api.IdmIdentityService;
import org.flowable.idm.engine.configurator.IdmEngineConfigurator;
import org.flowable.job.service.HistoryJobHandler;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.InternalJobParentStateResolver;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.job.service.impl.asyncexecutor.AsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncHistoryJobExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncJobExecutor;
import org.flowable.job.service.impl.asyncexecutor.DefaultAsyncRunnableExecutionExceptionHandler;
import org.flowable.job.service.impl.asyncexecutor.ExecuteAsyncRunnableFactory;
import org.flowable.job.service.impl.asyncexecutor.FailedJobCommandFactory;
import org.flowable.job.service.impl.asyncexecutor.JobManager;
import org.flowable.job.service.impl.db.JobDbSchemaManager;
import org.flowable.job.service.impl.history.async.AsyncHistoryJobHandler;
import org.flowable.job.service.impl.history.async.AsyncHistoryJobZippedHandler;
import org.flowable.job.service.impl.history.async.AsyncHistoryListener;
import org.flowable.job.service.impl.history.async.AsyncHistorySession;
import org.flowable.job.service.impl.history.async.AsyncHistorySessionFactory;
import org.flowable.job.service.impl.history.async.DefaultAsyncHistoryJobProducer;
import org.flowable.job.service.impl.history.async.transformer.HistoryJsonTransformer;
import org.flowable.task.service.InternalTaskAssignmentManager;
import org.flowable.task.service.InternalTaskVariableScopeResolver;
import org.flowable.task.service.TaskPostProcessor;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.DefaultTaskPostProcessor;
import org.flowable.task.service.impl.db.TaskDbSchemaManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl;
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
import org.flowable.variable.service.impl.types.EmptyCollectionType;
import org.flowable.variable.service.impl.types.InstantType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JodaDateTimeType;
import org.flowable.variable.service.impl.types.JodaDateType;
import org.flowable.variable.service.impl.types.JsonType;
import org.flowable.variable.service.impl.types.LocalDateTimeType;
import org.flowable.variable.service.impl.types.LocalDateType;
import org.flowable.variable.service.impl.types.LongStringType;
import org.flowable.variable.service.impl.types.LongType;
import org.flowable.variable.service.impl.types.NullType;
import org.flowable.variable.service.impl.types.SerializableType;
import org.flowable.variable.service.impl.types.ShortType;
import org.flowable.variable.service.impl.types.StringType;
import org.flowable.variable.service.impl.types.UUIDType;

public class CmmnEngineConfiguration extends AbstractEngineConfiguration implements CmmnEngineConfigurationApi,
        ScriptingEngineAwareEngineConfiguration, HasExpressionManagerEngineConfiguration, HasVariableTypes, 
        HasVariableServiceConfiguration {

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/cmmn/db/mapping/mappings.xml";
    public static final String LIQUIBASE_CHANGELOG_PREFIX = "ACT_CMMN_";

    protected String cmmnEngineName = CmmnEngines.NAME_DEFAULT;

    protected CmmnEngineAgendaFactory cmmnEngineAgendaFactory;

    protected CmmnRuntimeService cmmnRuntimeService = new CmmnRuntimeServiceImpl(this);
    protected DynamicCmmnService dynamicCmmnService = new DynamicCmmnServiceImpl(this);
    protected CmmnTaskService cmmnTaskService = new CmmnTaskServiceImpl(this);
    protected CmmnManagementService cmmnManagementService = new CmmnManagementServiceImpl(this);
    protected CmmnRepositoryService cmmnRepositoryService = new CmmnRepositoryServiceImpl(this);
    protected CmmnHistoryService cmmnHistoryService = new CmmnHistoryServiceImpl(this);
    protected CmmnMigrationService cmmnMigrationService = new CmmnMigrationServiceImpl(this);

    protected CmmnDeploymentDataManager deploymentDataManager;
    protected CmmnResourceDataManager resourceDataManager;
    protected CaseDefinitionDataManager caseDefinitionDataManager;
    protected CaseInstanceDataManager caseInstanceDataManager;
    protected PlanItemInstanceDataManager planItemInstanceDataManager;
    protected SentryPartInstanceDataManager sentryPartInstanceDataManager;
    protected MilestoneInstanceDataManager milestoneInstanceDataManager;
    protected HistoricCaseInstanceEntityManager historicCaseInstanceEntityManager;
    protected HistoricMilestoneInstanceDataManager historicMilestoneInstanceDataManager;
    protected HistoricPlanItemInstanceDataManager historicPlanItemInstanceDataManager;

    protected CmmnDeploymentEntityManager cmmnDeploymentEntityManager;
    protected CmmnResourceEntityManager cmmnResourceEntityManager;
    protected CaseDefinitionEntityManager caseDefinitionEntityManager;
    protected CaseInstanceEntityManager caseInstanceEntityManager;
    protected PlanItemInstanceEntityManager planItemInstanceEntityManager;
    protected SentryPartInstanceEntityManager sentryPartInstanceEntityManager;
    protected MilestoneInstanceEntityManager milestoneInstanceEntityManager;
    protected HistoricCaseInstanceDataManager historicCaseInstanceDataManager;
    protected HistoricMilestoneInstanceEntityManager historicMilestoneInstanceEntityManager;
    protected HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager;

    protected boolean disableIdmEngine;
    
    protected boolean disableEventRegistry;
    
    protected CandidateManager candidateManager;
    protected PlanItemVariableAggregator variableAggregator;

    protected DecisionTableVariableManager decisionTableVariableManager;

    protected CaseInstanceHelper caseInstanceHelper;
    protected CmmnHistoryManager cmmnHistoryManager;
    protected ProcessInstanceService processInstanceService;
    protected CmmnDynamicStateManager dynamicStateManager;
    protected CaseInstanceMigrationManager caseInstanceMigrationManager;
    protected Map<String, List<RuntimeInstanceStateChangeCallback>> caseInstanceStateChangeCallbacks;
    protected List<CaseInstanceLifecycleListener> caseInstanceLifecycleListeners;
    protected Map<String, List<PlanItemInstanceLifecycleListener>> planItemInstanceLifecycleListeners;
    protected StartCaseInstanceInterceptor startCaseInstanceInterceptor;
    protected CreateHumanTaskInterceptor createHumanTaskInterceptor;
    protected CreateCasePageTaskInterceptor createCasePageTaskInterceptor;
    protected CreateCmmnExternalWorkerJobInterceptor createCmmnExternalWorkerJobInterceptor;
    protected CmmnIdentityLinkInterceptor identityLinkInterceptor;

    protected boolean executeServiceSchemaManagers = true;

    protected boolean enableSafeCmmnXml;
    protected boolean disableCmmnXmlValidation;
    protected CmmnActivityBehaviorFactory activityBehaviorFactory;
    protected CmmnClassDelegateFactory classDelegateFactory;
    protected CmmnDeployer cmmnDeployer;
    protected CmmnDeploymentManager deploymentManager;
    protected CaseDefinitionDiagramHelper caseDefinitionDiagramHelper;

    protected int caseDefinitionCacheLimit = -1;
    protected DeploymentCache<CaseDefinitionCacheEntry> caseDefinitionCache;

    protected CmmnParser cmmnParser;
    protected List<CmmnParseHandler> preCmmnParseHandlers;
    protected List<CmmnParseHandler> postCmmnParseHandlers;
    protected List<CmmnParseHandler> customCmmnParseHandlers;

    protected CmmnListenerFactory listenerFactory;
    protected CmmnListenerNotificationHelper listenerNotificationHelper;

    protected HistoryLevel historyLevel = HistoryLevel.AUDIT;

    protected ExpressionManager expressionManager;
    protected List<FlowableFunctionDelegate> flowableFunctionDelegates;
    protected List<FlowableFunctionDelegate> customFlowableFunctionDelegates;
    protected List<FlowableAstFunctionCreator> astFunctionCreators;
    protected Collection<ELResolver> preDefaultELResolvers;
    protected Collection<ELResolver> preBeanELResolvers;
    protected Collection<ELResolver> postDefaultELResolvers;

    protected boolean isExpressionCacheEnabled = true;
    protected int expressionCacheSize = 4096;
    protected int expressionTextLengthCacheLimit = -1; // negative value to have no max length

    protected ScriptingEngines scriptingEngines;
    protected List<ResolverFactory> resolverFactories;

    /**
     * Using field injection together with a delegate expression for a service task / execution listener / task listener is not thread-sade , see user guide section 'Field Injection' for more
     * information.
     * <p>
     * Set this flag to false to throw an exception at runtime when a field is injected and a delegateExpression is used.
     */
    protected DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode = DelegateExpressionFieldInjectionMode.MIXED;

    protected SchemaManager identityLinkSchemaManager;
    protected SchemaManager entityLinkSchemaManager;
    protected SchemaManager eventSubscriptionSchemaManager;
    protected SchemaManager variableSchemaManager;
    protected SchemaManager taskSchemaManager;
    protected SchemaManager jobSchemaManager;

    /**
     * Case diagram generator. Default value is DefaultCaseDiagramGenerator
     */
    protected CaseDiagramGenerator caseDiagramGenerator;

    protected boolean isCreateDiagramOnDeploy = true;

    protected String activityFontName = "Arial";
    protected String labelFontName = "Arial";
    protected String annotationFontName = "Arial";

    // Identitylink support
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;
    
    // Entitylink support
    protected EntityLinkServiceConfiguration entityLinkServiceConfiguration;
    protected boolean enableEntityLinks;
    
    // EventSubscription support
    protected EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration;

    // Task support
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected InternalHistoryTaskManager internalHistoryTaskManager;
    protected InternalTaskVariableScopeResolver internalTaskVariableScopeResolver;
    protected InternalTaskAssignmentManager internalTaskAssignmentManager;
    protected IdentityLinkEventHandler identityLinkEventHandler;
    protected boolean isEnableTaskRelationshipCounts = true;

    // Variable support
    protected VariableTypes variableTypes;
    protected List<VariableType> customPreVariableTypes;
    protected List<VariableType> customPostVariableTypes;
    protected VariableServiceConfiguration variableServiceConfiguration;
    protected InternalHistoryVariableManager internalHistoryVariableManager;
    protected boolean serializableVariableTypeTrackDeserializedObjects = true;
    /**
     * This flag determines whether variables of the type 'json' and 'longJson' will be tracked.
     * <p>
     * This means that, when true, in a JavaDelegate you can write:
     * <pre><code>
     *     JsonNode jsonNode = (JsonNode) execution.getVariable("customer");
     *     customer.put("name", "Kermit");
     * </code></pre>
     * And the changes to the JsonNode will be reflected in the database. Otherwise, a manual call to setVariable will be needed.
     */
    protected boolean jsonVariableTypeTrackObjects = true;


    // Set Http Client config defaults
    protected HttpClientConfig httpClientConfig = new HttpClientConfig();

    // Email
    protected String mailServerHost = "localhost";
    protected String mailServerUsername; // by default no name and password are provided, which
    protected String mailServerPassword; // means no authentication for mail server
    protected int mailServerPort = 25;
    protected int mailServerSSLPort = 465;
    protected boolean useSSL;
    protected boolean useTLS;
    protected String mailServerDefaultFrom = "flowable@localhost";
    protected String mailServerForceTo;
    protected String mailSessionJndi;
    protected Map<String, MailServerInfo> mailServers = new HashMap<>();
    protected Map<String, String> mailSessionsJndi = new HashMap<>();

    // Async executor
    protected JobServiceConfiguration jobServiceConfiguration;

    protected AsyncExecutor asyncExecutor;
    protected AsyncTaskExecutor asyncTaskExecutor;
    protected boolean shutdownAsyncTaskExecutor;
    protected AsyncTaskInvoker asyncTaskInvoker;
    protected JobManager jobManager;
    protected List<JobHandler> customJobHandlers;
    protected Map<String, JobHandler> jobHandlers;
    protected InternalJobManager internalJobManager;
    protected List<AsyncRunnableExecutionExceptionHandler> customAsyncRunnableExecutionExceptionHandlers;
    protected boolean addDefaultExceptionHandler = true;
    protected FailedJobCommandFactory failedJobCommandFactory;
    protected InternalJobParentStateResolver internalJobParentStateResolver;
    protected List<String> enabledJobCategories;
    protected String jobExecutionScope = JobServiceConfiguration.JOB_EXECUTION_SCOPE_CMMN;
    protected String historyJobExecutionScope = JobServiceConfiguration.JOB_EXECUTION_SCOPE_CMMN;
    
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
    protected int asyncExecutorCorePoolSize = 8;

    /**
     * The maximum number of threads that are created in the threadpool for job execution.
     * Default value = 10.
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorMaxPoolSize = 8;

    /**
     * The time (in milliseconds) a thread used for job execution must be kept alive before it is destroyed.
     * Default setting is 5 seconds. Having a setting &gt; 0 takes resources, but in the case of many
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
    protected int asyncExecutorThreadPoolQueueSize = 2048;

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
     * Whether or not core threads can time out (which is needed to scale down the threads). Default true.
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected boolean asyncExecutorAllowCoreThreadTimeout = true;

    /**
     * The thread factory that the async task executor should use.
     */
    protected ThreadFactory asyncExecutorThreadFactory;

    protected String asyncExecutorTenantId = AbstractEngineConfiguration.NO_TENANT_ID;

    /**
     * The number of timer jobs that are acquired during one query
     * Before a job is executed, an acquirement thread fetches jobs from the database and puts them on the queue.
     *
     * Consider using the global acquire lock when there are too many nodes competing for the same resources (and thus too many optimistic locking exceptions).
     *
     * A larger value means more timer jobs will be fetched in one request.
     */
    protected int asyncExecutorMaxTimerJobsPerAcquisition = 512;

    /**
     * The number of async jobs that are acquired during one query (before a job is executed,
     * an acquirement thread fetches jobs from the database and puts them on the queue).
     *
     * Consider using the global acquire lock when there are too many nodes competing for the same resources (and thus too many optimistic locking exceptions).
     *
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorMaxAsyncJobsDuePerAcquisition = 512;

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
    protected int asyncExecutorDefaultQueueSizeFullWaitTime = 5 * 1000;

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
     * Whether to unlock jobs that are owned by this executor (have the same {@link #asyncExecutorLockOwner}) at startup or shutdown.
     */
    protected boolean asyncExecutorUnlockOwnedJobs = true;

    /**
     * The amount of time (in milliseconds) a timer job is locked when acquired by the async executor.
     * During this period of time, no other async executor will try to acquire and lock this job.
     * <p>
     * Default value = 60 minutes;
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorTimerLockTimeInMillis = 60 * 60 * 1000;

    /**
     * The amount of time (in milliseconds) an async job is locked when acquired by the async executor.
     * During this period of time, no other async executor will try to acquire and lock this job.
     * <p>
     * Default value = 60 minutes;
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected int asyncExecutorAsyncJobLockTimeInMillis = 60 * 60 * 1000;

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

    /**
     * Allows to define a custom factory for creating the {@link Runnable} that is executed by the async executor.
     * <p>
     * This property is only applicable when using the threadpool-based async executor.
     */
    protected ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory;
    
    /**
     * Flags to control which threads (when using the default threadpool-based async executor) are started.
     * This can be used to boot up engine instances that still execute jobs originating from this instance itself,
     * but don't fetch new jobs themselves.
     */
    protected boolean isAsyncExecutorAsyncJobAcquisitionEnabled = true;
    protected boolean isAsyncExecutorTimerJobAcquisitionEnabled = true;
    protected boolean isAsyncExecutorResetExpiredJobsEnabled = true;
    
    protected AsyncExecutor asyncHistoryExecutor;
    protected AsyncTaskExecutor asyncHistoryTaskExecutor;
    protected boolean shutdownAsyncHistoryTaskExecutor;
    protected boolean isAsyncHistoryEnabled;
    protected boolean asyncHistoryExecutorActivate;
    protected boolean isAsyncHistoryJsonGzipCompressionEnabled;
    protected boolean isAsyncHistoryJsonGroupingEnabled;
    protected boolean asyncHistoryExecutorMessageQueueMode;
    protected int asyncHistoryJsonGroupingThreshold = 10;
    protected AsyncHistoryListener asyncHistoryListener;

    // More info: see similar async executor properties.
    protected int asyncHistoryExecutorNumberOfRetries = 10;
    protected int asyncHistoryExecutorCorePoolSize = 8;
    protected int asyncHistoryExecutorMaxPoolSize = 8;
    protected long asyncHistoryExecutorThreadKeepAliveTime = 5000L;
    protected int asyncHistoryExecutorMaxJobsDuePerAcquisition = 512;
    protected int asyncHistoryExecutorThreadPoolQueueSize = 2048;
    protected BlockingQueue<Runnable> asyncHistoryExecutorThreadPoolQueue;
    protected long asyncHistoryExecutorSecondsToWaitOnShutdown = 60L;
    protected int asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime = 10 * 1000;
    protected int asyncHistoryExecutorDefaultQueueSizeFullWaitTime = 5 * 1000;
    protected String asyncHistoryExecutorLockOwner;
    protected int asyncHistoryExecutorAsyncJobLockTimeInMillis = 60 * 60 * 1000;
    protected int asyncHistoryExecutorResetExpiredJobsInterval = 60 * 1000;
    protected int asyncHistoryExecutorResetExpiredJobsPageSize = 3;
    protected boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled = true;
    protected boolean isAsyncHistoryExecutorResetExpiredJobsEnabled = true;
    
    protected boolean enableHistoryCleaning = false;
    protected String historyCleaningTimeCycleConfig = "0 0 1 * * ?";
    protected int cleanInstancesEndedAfterNumberOfDays = 365;
    protected CmmnHistoryCleaningManager cmmnHistoryCleaningManager;
    
    protected Map<String, HistoryJobHandler> historyJobHandlers;
    protected List<HistoryJobHandler> customHistoryJobHandlers;
    protected List<HistoryJsonTransformer> customHistoryJsonTransformers;

    protected FormFieldHandler formFieldHandler;
    protected boolean isFormFieldValidationEnabled;
    
    protected EventRegistryEventConsumer eventRegistryEventConsumer;

    protected BusinessCalendarManager businessCalendarManager;

    /**
     * Enable user task event logging
     */
    protected boolean enableHistoricTaskLogging;

    /**
     * postprocessor for a task builder
     */
    protected TaskPostProcessor taskPostProcessor;

    protected boolean handleCmmnEngineExecutorsAfterEngineCreate = true;

    protected boolean alwaysUseArraysForDmnMultiHitPolicies = true;

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
        CmmnEngineImpl cmmnEngine = new CmmnEngineImpl(this);

        if (handleCmmnEngineExecutorsAfterEngineCreate) {
            cmmnEngine.startExecutors();
        }

        return cmmnEngine;
    }

    protected void init() {
        initEngineConfigurations();
        initConfigurators();
        configuratorsBeforeInit();
        initClock();
        initObjectMapper();
        initCaseDiagramGenerator();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();
        initFunctionDelegates();
        initAstFunctionCreators();
        initBeans();
        initExpressionManager();
        initCmmnEngineAgendaFactory();

        if (usingRelationalDatabase) {
            initDataSource();
        }
        
        if (usingRelationalDatabase || usingSchemaMgmt) {
            initSchemaManager();
            initSchemaManagementCommand();
        }

        configureVariableServiceConfiguration();
        configureJobServiceConfiguration();
        initVariableTypes();
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
        initListenerFactory();
        initListenerNotificationHelper();
        initDeployers();
        initCaseDefinitionCache();
        initDeploymentManager();
        initCaseInstanceHelper();
        initCandidateManager();
        initVariableAggregator();
        initHistoryManager();
        initDynamicStateManager();
        initCaseInstanceMigrationManager();
        initCaseInstanceCallbacks();
        initFormFieldHandler();
        initIdentityLinkInterceptor();
        initEventDispatcher();
        initIdentityLinkServiceConfiguration();
        initEntityLinkServiceConfiguration();
        initEventSubscriptionServiceConfiguration();
        initVariableServiceConfiguration();
        initTaskServiceConfiguration();
        initBusinessCalendarManager();
        initJobHandlers();
        initHistoryJobHandlers();
        initFailedJobCommandFactory();
        initJobServiceConfiguration();
        initAsyncExecutor();
        initAsyncHistoryExecutor();
        initScriptingEngines();
        configuratorsAfterInit();
        afterInitEventRegistryEventBusConsumer();
        
        initHistoryCleaningManager();
    }

    public void initCaseDiagramGenerator() {
        if (caseDiagramGenerator == null) {
            caseDiagramGenerator = new DefaultCaseDiagramGenerator();
        }
    }

    @Override
    public void initSchemaManager() {
        super.initSchemaManager();
        initCmmnSchemaManager();

        if (executeServiceSchemaManagers) {
            initIdentityLinkSchemaManager();
            initEntityLinkSchemaManager();
            initEventSubscriptionSchemaManager();
            initVariableSchemaManager();
            initTaskSchemaManager();
            initJobSchemaManager();
        }
    }
    
    public void initSchemaManagementCommand() {
        if (schemaManagementCmd == null) {
            if (usingRelationalDatabase && databaseSchemaUpdate != null) {
                this.schemaManagementCmd = new SchemaOperationsCmmnEngineBuild();
            }
        }
    }

    protected void initCmmnSchemaManager() {
        if (this.schemaManager == null) {
            if (DATABASE_TYPE_COCKROACHDB.equals(databaseType)) {
                this.schemaManager = new CmmnDbSchemaManager(CmmnDbSchemaManager.LIQUIBASE_CHANGELOG_CRDB);
            } else {
                this.schemaManager = new CmmnDbSchemaManager(CmmnDbSchemaManager.LIQUIBASE_CHANGELOG);
            }
        }
    }

    protected void initVariableSchemaManager() {
        if (this.variableSchemaManager == null) {
            this.variableSchemaManager = new VariableDbSchemaManager();
        }
    }

    protected void initTaskSchemaManager() {
        if (this.taskSchemaManager == null) {
            this.taskSchemaManager = new TaskDbSchemaManager();
        }
    }

    protected void initIdentityLinkSchemaManager() {
        if (this.identityLinkSchemaManager == null) {
            this.identityLinkSchemaManager = new IdentityLinkDbSchemaManager();
        }
    }
    
    protected void initEntityLinkSchemaManager() {
        if (this.entityLinkSchemaManager == null) {
            this.entityLinkSchemaManager = new EntityLinkDbSchemaManager();
        }
    }
    
    protected void initEventSubscriptionSchemaManager() {
        if (this.eventSubscriptionSchemaManager == null) {
            this.eventSubscriptionSchemaManager = new EventSubscriptionDbSchemaManager();
        }
    }

    protected void initJobSchemaManager() {
        if (this.jobSchemaManager == null) {
            this.jobSchemaManager = new JobDbSchemaManager();
        }
    }

    @Override
    public void initMybatisTypeHandlers(Configuration configuration) {
        super.initMybatisTypeHandlers(configuration);
        configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler(variableTypes));
    }
    
    public void initFunctionDelegates() {
        if (flowableFunctionDelegates == null) {
            flowableFunctionDelegates = new ArrayList<>();

            flowableFunctionDelegates.add(new VariableGetExpressionFunction());
            flowableFunctionDelegates.add(new VariableGetOrDefaultExpressionFunction());

            flowableFunctionDelegates.add(new VariableContainsAnyExpressionFunction());
            flowableFunctionDelegates.add(new VariableContainsExpressionFunction());

            flowableFunctionDelegates.add(new VariableEqualsExpressionFunction());
            flowableFunctionDelegates.add(new VariableNotEqualsExpressionFunction());

            flowableFunctionDelegates.add(new VariableExistsExpressionFunction());
            flowableFunctionDelegates.add(new VariableIsEmptyExpressionFunction());
            flowableFunctionDelegates.add(new VariableIsNotEmptyExpressionFunction());

            flowableFunctionDelegates.add(new VariableLowerThanExpressionFunction());
            flowableFunctionDelegates.add(new VariableLowerThanOrEqualsExpressionFunction());
            flowableFunctionDelegates.add(new VariableGreaterThanExpressionFunction());
            flowableFunctionDelegates.add(new VariableGreaterThanOrEqualsExpressionFunction());

            flowableFunctionDelegates.add(new VariableBase64ExpressionFunction());

            flowableFunctionDelegates.add(new IsStageCompletableExpressionFunction());
            flowableFunctionDelegates.add(new IsPlanItemCompletedExpressionFunction());
            flowableFunctionDelegates.add(new TaskGetFunctionDelegate());
        }
        
        if (customFlowableFunctionDelegates != null) {
            flowableFunctionDelegates.addAll(customFlowableFunctionDelegates);
        }
    }
    
    public void initAstFunctionCreators() {
        List<FlowableAstFunctionCreator> astFunctionCreators = new ArrayList<>();
        for (FlowableFunctionDelegate flowableFunctionDelegate : flowableFunctionDelegates) {
            if (flowableFunctionDelegate instanceof FlowableAstFunctionCreator) {
                astFunctionCreators.add((FlowableAstFunctionCreator) flowableFunctionDelegate);
            }
        }
        
        if (this.astFunctionCreators != null) {
            astFunctionCreators.addAll(this.astFunctionCreators);
        }

        this.astFunctionCreators = astFunctionCreators;
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            CmmnExpressionManager cmmnExpressionManager = new CmmnExpressionManager(beans);

            if (preDefaultELResolvers != null) {
                preDefaultELResolvers.forEach(cmmnExpressionManager::addPreDefaultResolver);
            }

            if (preBeanELResolvers != null) {
                preBeanELResolvers.forEach(cmmnExpressionManager::addPreBeanResolver);
            }

            if (postDefaultELResolvers != null) {
                postDefaultELResolvers.forEach(cmmnExpressionManager::addPostDefaultResolver);
            }
            
            if (isExpressionCacheEnabled) {
                cmmnExpressionManager.setExpressionCache(new DefaultDeploymentCache<>(expressionCacheSize));
                cmmnExpressionManager.setExpressionTextLengthCacheLimit(expressionTextLengthCacheLimit);
            }
            
            expressionManager = cmmnExpressionManager;
        }
        
        expressionManager.setFunctionDelegates(flowableFunctionDelegates);
        expressionManager.setAstFunctionCreators(astFunctionCreators);
    }

    public void initCmmnEngineAgendaFactory() {
        if (cmmnEngineAgendaFactory == null) {
            cmmnEngineAgendaFactory = new DefaultCmmnEngineAgendaFactory();
        }
    }

    @Override
    public void initCommandInvoker() {
        if (this.commandInvoker == null) {
            this.commandInvoker = new CmmnCommandInvoker(agendaOperationRunner);
        }
    }

    @Override
    public void initSessionFactories() {
        super.initSessionFactories();
        addSessionFactory(new CmmnEngineAgendaSessionFactory(cmmnEngineAgendaFactory));
        
        if (isAsyncHistoryEnabled) {
            initAsyncHistorySessionFactory();
        }

        if (!sessionFactories.containsKey(VariableListenerSession.class)) {
            VariableListenerSessionFactory variableListenerSessionFactory = new VariableListenerSessionFactory();
            sessionFactories.put(VariableListenerSession.class, variableListenerSessionFactory);
        }
    }
    
    public void initAsyncHistorySessionFactory() {
        // If another engine has set the asyncHistorySessionFactory already, there's no need to do it again.
        if (!sessionFactories.containsKey(AsyncHistorySession.class)) {
            AsyncHistorySessionFactory asyncHistorySessionFactory = new AsyncHistorySessionFactory();
            if (asyncHistoryListener == null) {
                initDefaultAsyncHistoryListener();
            }
            asyncHistorySessionFactory.setAsyncHistoryListener(asyncHistoryListener);
            sessionFactories.put(AsyncHistorySession.class, asyncHistorySessionFactory);
        }
        
        ((AsyncHistorySessionFactory) sessionFactories.get(AsyncHistorySession.class)).registerJobDataTypes(CmmnAsyncHistoryConstants.ORDERED_TYPES);
    }

    protected void initDefaultAsyncHistoryListener() {
        asyncHistoryListener = new DefaultAsyncHistoryJobProducer();
    }

    protected void initServices() {
        initService(cmmnRuntimeService);
        initService(dynamicCmmnService);
        initService(cmmnTaskService);
        initService(cmmnManagementService);
        initService(cmmnRepositoryService);
        initService(cmmnHistoryService);
        initService(cmmnMigrationService);
    }

    @Override
    public void initDataManagers() {
        super.initDataManagers();
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
        if (dbSqlSessionFactory != null && caseInstanceDataManager instanceof AbstractDataManager) {
            dbSqlSessionFactory.addLogicalEntityClassMapping("caseInstance", ((AbstractDataManager) caseInstanceDataManager).getManagedEntityClass());
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
        if (historicPlanItemInstanceDataManager == null) {
            historicPlanItemInstanceDataManager = new MybatisHistoricPlanItemInstanceDataManager(this);
        }
    }

    @Override
    public void initEntityManagers() {
        super.initEntityManagers();
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
        if (historicPlanItemInstanceEntityManager == null) {
            historicPlanItemInstanceEntityManager = new HistoricPlanItemInstanceEntityManagerImpl(this, historicPlanItemInstanceDataManager);
        }
    }

    protected void initClassDelegateFactory() {
        if (classDelegateFactory == null) {
            classDelegateFactory = new DefaultCmmnClassDelegateFactory(expressionManager);
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

    protected void initListenerFactory() {
        if (listenerFactory == null) {
            listenerFactory = new DefaultCmmnListenerFactory(classDelegateFactory, expressionManager);
        }
    }

    protected void initListenerNotificationHelper() {
        if (listenerNotificationHelper == null) {
            listenerNotificationHelper = new CmmnListenerNotificationHelper();
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
            cmmnDeployer = new CmmnDeployer(this);
        }

        initCmmnParser();
        initCaseDefinitionDiagramHelper();

        cmmnDeployer.setIdGenerator(idGenerator);
        cmmnDeployer.setCmmnParser(cmmnParser);
        cmmnDeployer.setCaseDefinitionDiagramHelper(caseDefinitionDiagramHelper);
        cmmnDeployer.setUsePrefixId(usePrefixId);

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

            List<CmmnParseHandler> parseHandlers = new ArrayList<>();
            if (getPreCmmnParseHandlers() != null) {
                parseHandlers.addAll(getPreCmmnParseHandlers());
            }
            parseHandlers.addAll(getDefaultCmmnParseHandlers());
            if (getPostCmmnParseHandlers() != null) {
                parseHandlers.addAll(getPostCmmnParseHandlers());
            }
            cmmnParserImpl.setCmmnParseHandlers(new CmmnParseHandlers(parseHandlers));

            cmmnParser = cmmnParserImpl;
        }
    }

    public List<CmmnParseHandler> getDefaultCmmnParseHandlers() {
        List<CmmnParseHandler> cmmnParseHandlers = new ArrayList<>();
        cmmnParseHandlers.add(new CaseParseHandler());
        cmmnParseHandlers.add(new CaseTaskParseHandler());
        cmmnParseHandlers.add(new DecisionTaskParseHandler());
        cmmnParseHandlers.add(new HumanTaskParseHandler());
        cmmnParseHandlers.add(new MilestoneParseHandler());
        cmmnParseHandlers.add(new PlanFragmentParseHandler());
        cmmnParseHandlers.add(new ProcessTaskParseHandler());
        cmmnParseHandlers.add(new ScriptTaskParseHandler());
        cmmnParseHandlers.add(new ServiceTaskParseHandler());
        cmmnParseHandlers.add(new SendEventServiceTaskParseHandler());
        cmmnParseHandlers.add(new ExternalWorkerServiceTaskParseHandler());
        cmmnParseHandlers.add(new StageParseHandler());
        cmmnParseHandlers.add(new HttpTaskParseHandler());
        cmmnParseHandlers.add(new CasePageTaskParseHandler());
        cmmnParseHandlers.add(new TaskParseHandler());
        cmmnParseHandlers.add(new GenericEventListenerParseHandler());
        cmmnParseHandlers.add(new SignalEventListenerParseHandler());
        cmmnParseHandlers.add(new TimerEventListenerParseHandler());
        cmmnParseHandlers.add(new UserEventListenerParseHandler());
        cmmnParseHandlers.add(new ReactivateEventListenerParseHandler());
        cmmnParseHandlers.add(new VariableEventListenerParseHandler());

        // Replace any default handler with a custom one (if needed)
        if (getCustomCmmnParseHandlers() != null) {
            Map<Class<?>, CmmnParseHandler> customParseHandlerMap = new HashMap<>();
            for (CmmnParseHandler cmmnParseHandler : getCustomCmmnParseHandlers()) {
                for (Class<?> handledType : cmmnParseHandler.getHandledTypes()) {
                    customParseHandlerMap.put(handledType, cmmnParseHandler);
                }
            }

            for (int i = 0; i < cmmnParseHandlers.size(); i++) {
                // All the default handlers support only one type
                CmmnParseHandler defaultCmmnParseHandler = cmmnParseHandlers.get(i);
                if (defaultCmmnParseHandler.getHandledTypes().size() != 1) {
                    StringBuilder supportedTypes = new StringBuilder();
                    for (Class<?> type : defaultCmmnParseHandler.getHandledTypes()) {
                        supportedTypes.append(" ").append(type.getCanonicalName()).append(" ");
                    }
                    throw new FlowableException("The default CMMN parse handlers should only support one type, but " + defaultCmmnParseHandler.getClass() + " supports " + supportedTypes
                        + ". This is likely a programmatic error");
                } else {
                    Class<?> handledType = defaultCmmnParseHandler.getHandledTypes().iterator().next();
                    if (customParseHandlerMap.containsKey(handledType)) {
                        CmmnParseHandler newBpmnParseHandler = customParseHandlerMap.get(handledType);
                        logger.info("Replacing default CmmnParseHandler {} with {}", defaultCmmnParseHandler.getClass().getName(), newBpmnParseHandler.getClass().getName());
                        cmmnParseHandlers.set(i, newBpmnParseHandler);
                    }
                }
            }
        }

        return cmmnParseHandlers;
    }

    public void initCaseDefinitionDiagramHelper() {
        if (caseDefinitionDiagramHelper == null) {
            caseDefinitionDiagramHelper = new CaseDefinitionDiagramHelper();
        }
    }

    public void initCaseInstanceHelper() {
        if (caseInstanceHelper == null) {
            caseInstanceHelper = new CaseInstanceHelperImpl(this);
        }
    }
    
    public void initCandidateManager() {
        if (candidateManager == null) {
            candidateManager = new DefaultCandidateManager(this);
        }
    }

    public void initVariableAggregator() {
        if (variableAggregator == null) {
            variableAggregator = new JsonPlanItemVariableAggregator(this);
        }
    }

    public void initHistoryManager() {
        if (cmmnHistoryManager == null) {
            if (isAsyncHistoryEnabled) {
                cmmnHistoryManager = new AsyncCmmnHistoryManager(this);
            } else {
                cmmnHistoryManager = new DefaultCmmnHistoryManager(this);
            }
        }
    }
    
    public void initDynamicStateManager() {
        if (dynamicStateManager == null) {
            dynamicStateManager = new DefaultCmmnDynamicStateManager(this);
        }
    }

    public void initCaseInstanceMigrationManager() {
        if (caseInstanceMigrationManager == null) {
            caseInstanceMigrationManager = new CaseInstanceMigrationManagerImpl(this);
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
    
    public void initIdentityLinkInterceptor() {
        if (identityLinkInterceptor == null) {
            identityLinkInterceptor = new DefaultCmmnIdentityLinkInterceptor(this);
        }
    }

    protected void initDefaultCaseInstanceCallbacks() {
        this.caseInstanceStateChangeCallbacks.put(CallbackTypes.PLAN_ITEM_CHILD_CASE,
                Collections.singletonList(new ChildCaseInstanceStateChangeCallback()));
        this.caseInstanceStateChangeCallbacks.put(CallbackTypes.EXECUTION_CHILD_CASE,
                Collections.singletonList(new ChildBpmnCaseInstanceStateChangeCallback()));
    }

    protected void initScriptingEngines() {
        if (resolverFactories == null) {
            resolverFactories = new ArrayList<>();
            resolverFactories.add(new CmmnVariableScopeResolverFactory());
            resolverFactories.add(new BeansResolverFactory());
        }
        if (scriptingEngines == null) {

            scriptingEngines = new ScriptingEngines(new ScriptBindingsFactory(this, resolverFactories));
        }
    }
    
    public void afterInitEventRegistryEventBusConsumer() {
        EventRegistryEventConsumer cmmnEventRegistryEventConsumer = null;
        if (eventRegistryEventConsumer != null) {
            cmmnEventRegistryEventConsumer = eventRegistryEventConsumer;
        } else {
            cmmnEventRegistryEventConsumer = new CmmnEventRegistryEventConsumer(this);
        }
        
        addEventRegistryEventConsumer(cmmnEventRegistryEventConsumer.getConsumerKey(), cmmnEventRegistryEventConsumer);
    }
    
    public void initHistoryCleaningManager() {
        if (cmmnHistoryCleaningManager == null) {
            cmmnHistoryCleaningManager = new DefaultCmmnHistoryCleaningManager(this);
        }
    }

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_CMMN_ENGINE_CONFIG;
    }

    @Override
    public String getEngineScopeType() {
        return ScopeTypes.CMMN;
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

        // Oracle doesn't support bulk inserting for historic task log entries
        if (isBulkInsertEnabled && "oracle".equals(databaseType)) {
            dbSqlSessionFactory.getBulkInserteableEntityClasses().remove(HistoricTaskLogEntryEntityImpl.class);
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
            variableTypes.addType(new InstantType());
            variableTypes.addType(new LocalDateType());
            variableTypes.addType(new LocalDateTimeType());
            variableTypes.addType(new JodaDateType());
            variableTypes.addType(new JodaDateTimeType());
            variableTypes.addType(new DoubleType());
            variableTypes.addType(new UUIDType());
            variableTypes.addType(new JsonType(getMaxLengthString(), objectMapper, jsonVariableTypeTrackObjects));
            // longJsonType only needed for reading purposes
            variableTypes.addType(JsonType.longJsonType(getMaxLengthString(), objectMapper, jsonVariableTypeTrackObjects));
            variableTypes.addType(new CmmnAggregatedVariableType(this));
            variableTypes.addType(new ByteArrayType());
            variableTypes.addType(new EmptyCollectionType());
            variableTypes.addType(new SerializableType(serializableVariableTypeTrackDeserializedObjects));

        } else {
            if (customPreVariableTypes != null) {
                for (int i = customPreVariableTypes.size() - 1; i >= 0; i--) {
                    VariableType customVariableType = customPreVariableTypes.get(i);
                    if (variableTypes.getVariableType(customVariableType.getTypeName()) == null) {
                        variableTypes.addType(customVariableType, 0);
                    }
                }
            }

            if (variableTypes.getVariableType(CmmnAggregatedVariableType.TYPE_NAME) == null) {
                variableTypes.addTypeBefore(new CmmnAggregatedVariableType(this), SerializableType.TYPE_NAME);
            }

            if (variableTypes.getVariableType(EmptyCollectionType.TYPE_NAME) == null) {
                variableTypes.addTypeBefore(new EmptyCollectionType(), SerializableType.TYPE_NAME);
            }
        }

        if (customPostVariableTypes != null) {
            for (VariableType customVariableType : customPostVariableTypes) {
                if (variableTypes.getVariableType(customVariableType.getTypeName()) == null) {
                    variableTypes.addType(customVariableType);
                }
            }
        }
    }

    public void configureVariableServiceConfiguration() {
        this.variableServiceConfiguration = instantiateVariableServiceConfiguration();

        this.variableServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.variableServiceConfiguration.setClock(this.clock);
        this.variableServiceConfiguration.setIdGenerator(this.idGenerator);
        this.variableServiceConfiguration.setObjectMapper(this.objectMapper);
        this.variableServiceConfiguration.setExpressionManager(expressionManager);

        if (this.internalHistoryVariableManager != null) {
            this.variableServiceConfiguration.setInternalHistoryVariableManager(this.internalHistoryVariableManager);
        } else {
            this.variableServiceConfiguration.setInternalHistoryVariableManager(new CmmnHistoryVariableManager(this));
        }

        this.variableServiceConfiguration.setMaxLengthString(this.getMaxLengthString());
        this.variableServiceConfiguration.setSerializableVariableTypeTrackDeserializedObjects(this.isSerializableVariableTypeTrackDeserializedObjects());
        this.variableServiceConfiguration.setLoggingSessionEnabled(isLoggingSessionEnabled());
    }

    public void initVariableServiceConfiguration() {
        this.variableServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        this.variableServiceConfiguration.setVariableTypes(this.variableTypes);

        this.variableServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG, this.variableServiceConfiguration);
    }

    protected VariableServiceConfiguration instantiateVariableServiceConfiguration() {
        return new VariableServiceConfiguration(ScopeTypes.CMMN);
    }

    public void initTaskServiceConfiguration() {
        this.taskServiceConfiguration = instantiateTaskServiceConfiguration();
        this.taskServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.taskServiceConfiguration.setClock(this.clock);
        this.taskServiceConfiguration.setIdGenerator(this.idGenerator);
        this.taskServiceConfiguration.setObjectMapper(this.objectMapper);
        this.taskServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        this.taskServiceConfiguration.setEnableHistoricTaskLogging(this.enableHistoricTaskLogging);

        if (this.taskPostProcessor != null) {
            this.taskServiceConfiguration.setTaskPostProcessor(this.taskPostProcessor);
        } else {
            this.taskServiceConfiguration.setTaskPostProcessor(new DefaultTaskPostProcessor());
        }

        if (this.internalHistoryTaskManager != null) {
            this.taskServiceConfiguration.setInternalHistoryTaskManager(this.internalHistoryTaskManager);
        } else {
            this.taskServiceConfiguration.setInternalHistoryTaskManager(new CmmnHistoryTaskManager(this));
        }

        if (this.internalTaskVariableScopeResolver != null) {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(this.internalTaskVariableScopeResolver);
        } else {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(new DefaultCmmnTaskVariableScopeResolver(this));
        }

        if (this.internalTaskAssignmentManager != null) {
            this.taskServiceConfiguration.setInternalTaskAssignmentManager(this.internalTaskAssignmentManager);
        } else {
            this.taskServiceConfiguration.setInternalTaskAssignmentManager(new DefaultTaskAssignmentManager(this));
        }

        this.taskServiceConfiguration.setEnableTaskRelationshipCounts(this.isEnableTaskRelationshipCounts);

        this.taskServiceConfiguration.init();

        if (dbSqlSessionFactory != null && taskServiceConfiguration.getTaskDataManager() instanceof AbstractDataManager) {
            dbSqlSessionFactory.addLogicalEntityClassMapping("task", ((AbstractDataManager) taskServiceConfiguration.getTaskDataManager()).getManagedEntityClass());
        }

        addServiceConfiguration(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG, this.taskServiceConfiguration);
    }

    protected TaskServiceConfiguration instantiateTaskServiceConfiguration() {
        return new TaskServiceConfiguration(ScopeTypes.CMMN);
    }

    public void initIdentityLinkServiceConfiguration() {
        this.identityLinkServiceConfiguration = instantiateIdentityLinkServiceConfiguration();
        this.identityLinkServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.identityLinkServiceConfiguration.setClock(this.clock);
        this.identityLinkServiceConfiguration.setIdGenerator(this.idGenerator);
        this.identityLinkServiceConfiguration.setObjectMapper(this.objectMapper);
        this.identityLinkServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        this.identityLinkServiceConfiguration.setIdentityLinkEventHandler(this.identityLinkEventHandler);

        this.identityLinkServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_IDENTITY_LINK_SERVICE_CONFIG, this.identityLinkServiceConfiguration);
    }

    protected IdentityLinkServiceConfiguration instantiateIdentityLinkServiceConfiguration() {
        return new IdentityLinkServiceConfiguration(ScopeTypes.CMMN);
    }
    
    public void initEntityLinkServiceConfiguration() {
        if (this.enableEntityLinks) {
            this.entityLinkServiceConfiguration = instantiateEntityLinkServiceConfiguration();
            this.entityLinkServiceConfiguration.setHistoryLevel(this.historyLevel);
            this.entityLinkServiceConfiguration.setClock(this.clock);
            this.entityLinkServiceConfiguration.setIdGenerator(this.idGenerator);
            this.entityLinkServiceConfiguration.setObjectMapper(this.objectMapper);
            this.entityLinkServiceConfiguration.setEventDispatcher(this.eventDispatcher);
    
            this.entityLinkServiceConfiguration.init();
    
            addServiceConfiguration(EngineConfigurationConstants.KEY_ENTITY_LINK_SERVICE_CONFIG, this.entityLinkServiceConfiguration);
        }
    }

    protected EntityLinkServiceConfiguration instantiateEntityLinkServiceConfiguration() {
        return new EntityLinkServiceConfiguration(ScopeTypes.CMMN);
    }
    
    public void initEventSubscriptionServiceConfiguration() {
        this.eventSubscriptionServiceConfiguration = instantiateEventSubscriptionServiceConfiguration();
        this.eventSubscriptionServiceConfiguration.setClock(this.clock);
        this.eventSubscriptionServiceConfiguration.setIdGenerator(this.idGenerator);
        this.eventSubscriptionServiceConfiguration.setObjectMapper(this.objectMapper);
        this.eventSubscriptionServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        
        this.eventSubscriptionServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_EVENT_SUBSCRIPTION_SERVICE_CONFIG, this.eventSubscriptionServiceConfiguration);
    }
    
    protected EventSubscriptionServiceConfiguration instantiateEventSubscriptionServiceConfiguration() {
        return new EventSubscriptionServiceConfiguration(ScopeTypes.CMMN);
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
        jobHandlers.put(AsyncInitializePlanModelJobHandler.TYPE, new AsyncInitializePlanModelJobHandler());
        jobHandlers.put(CmmnHistoryCleanupJobHandler.TYPE, new CmmnHistoryCleanupJobHandler());
        jobHandlers.put(ExternalWorkerTaskCompleteJobHandler.TYPE, new ExternalWorkerTaskCompleteJobHandler(this));

        // if we have custom job handlers, register them
        if (customJobHandlers != null) {
            for (JobHandler customJobHandler : customJobHandlers) {
                jobHandlers.put(customJobHandler.getType(), customJobHandler);
            }
        }
    }
    
    protected void initHistoryJobHandlers() {
        if (isAsyncHistoryEnabled) {
            historyJobHandlers = new HashMap<>();
            
            List<HistoryJsonTransformer> allHistoryJsonTransformers = new ArrayList<>(initDefaultHistoryJsonTransformers());
            if (customHistoryJsonTransformers != null) {
                allHistoryJsonTransformers.addAll(customHistoryJsonTransformers);
            }

            AsyncHistoryJobHandler asyncHistoryJobHandler = new AsyncHistoryJobHandler(CmmnAsyncHistoryConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY);
            allHistoryJsonTransformers.forEach(asyncHistoryJobHandler::addHistoryJsonTransformer);
            asyncHistoryJobHandler.setAsyncHistoryJsonGroupingEnabled(isAsyncHistoryJsonGroupingEnabled);
            historyJobHandlers.put(asyncHistoryJobHandler.getType(), asyncHistoryJobHandler);

            AsyncHistoryJobZippedHandler asyncHistoryJobZippedHandler = new AsyncHistoryJobZippedHandler(CmmnAsyncHistoryConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED);
            allHistoryJsonTransformers.forEach(asyncHistoryJobZippedHandler::addHistoryJsonTransformer);
            asyncHistoryJobZippedHandler.setAsyncHistoryJsonGroupingEnabled(isAsyncHistoryJsonGroupingEnabled);
            historyJobHandlers.put(asyncHistoryJobZippedHandler.getType(), asyncHistoryJobZippedHandler);

            if (getCustomHistoryJobHandlers() != null) {
                for (HistoryJobHandler customJobHandler : getCustomHistoryJobHandlers()) {
                    historyJobHandlers.put(customJobHandler.getType(), customJobHandler);
                }
            }
        }
    }
    
    protected List<HistoryJsonTransformer> initDefaultHistoryJsonTransformers() {
        List<HistoryJsonTransformer> historyJsonTransformers = new ArrayList<>();
        
        historyJsonTransformers.add(new CaseInstanceStartHistoryJsonTransformer(this));
        historyJsonTransformers.add(new CaseInstanceEndHistoryJsonTransformer(this));
        historyJsonTransformers.add(new CaseInstanceReactivateHistoryJsonTransformer(this));
        historyJsonTransformers.add(new CaseInstanceUpdateNameHistoryJsonTransformer(this));
        historyJsonTransformers.add(new CaseInstanceUpdateBusinessKeyHistoryJsonTransformer(this));
        historyJsonTransformers.add(new HistoricCaseInstanceDeletedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new MilestoneReachedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new IdentityLinkCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new IdentityLinkDeletedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new EntityLinkCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new EntityLinkDeletedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new VariableCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new VariableUpdatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new VariableRemovedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new TaskCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new TaskUpdatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new TaskEndedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new PlanItemInstanceFullHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceAvailableHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceCompletedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceDisabledHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceEnabledHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceExitHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceOccurredHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceStartedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceSuspendedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new PlanItemInstanceTerminatedHistoryJsonTransformer(this));
        
        historyJsonTransformers.add(new UpdateCaseDefinitionCascadeHistoryJsonTransformer(this));

        historyJsonTransformers.add(new HistoricUserTaskLogRecordJsonTransformer(this));
        historyJsonTransformers.add(new HistoricUserTaskLogDeleteJsonTransformer(this));

        return historyJsonTransformers;
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

    public void configureJobServiceConfiguration() {
        if (jobServiceConfiguration == null) {
            this.jobServiceConfiguration = instantiateJobServiceConfiguration();
            this.jobServiceConfiguration.setHistoryLevel(this.historyLevel);
            this.jobServiceConfiguration.setClock(this.clock);
            this.jobServiceConfiguration.setIdGenerator(this.idGenerator);
            this.jobServiceConfiguration.setObjectMapper(this.objectMapper);
            this.jobServiceConfiguration.setCommandExecutor(this.commandExecutor);
            this.jobServiceConfiguration.setExpressionManager(this.expressionManager);
    
            List<AsyncRunnableExecutionExceptionHandler> exceptionHandlers = new ArrayList<>();
            if (customAsyncRunnableExecutionExceptionHandlers != null) {
                exceptionHandlers.addAll(customAsyncRunnableExecutionExceptionHandlers);
            }
    
            if (this.internalJobParentStateResolver != null) {
                this.jobServiceConfiguration.setJobParentStateResolver(this.internalJobParentStateResolver);
            } else {
                this.jobServiceConfiguration.setJobParentStateResolver(new DefaultCmmnJobParentStateResolver(this));
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
            
            // Async history job config
            jobServiceConfiguration.setJobTypeAsyncHistory(CmmnAsyncHistoryConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY);
            jobServiceConfiguration.setJobTypeAsyncHistoryZipped(CmmnAsyncHistoryConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED);
            jobServiceConfiguration.setAsyncHistoryJsonGzipCompressionEnabled(isAsyncHistoryJsonGzipCompressionEnabled);
            jobServiceConfiguration.setAsyncHistoryJsonGroupingEnabled(isAsyncHistoryJsonGroupingEnabled);
            jobServiceConfiguration.setAsyncHistoryJsonGroupingThreshold(asyncHistoryJsonGroupingThreshold);
            
            this.jobServiceConfiguration.setJobExecutionScope(this.jobExecutionScope);
            this.jobServiceConfiguration.setHistoryJobExecutionScope(this.historyJobExecutionScope);
            
            if (enabledJobCategories != null) {
                this.jobServiceConfiguration.setEnabledJobCategories(enabledJobCategories);
            }
        }
    }

    public void initJobServiceConfiguration() {
        this.jobServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        this.jobServiceConfiguration.setBusinessCalendarManager(this.businessCalendarManager);
        this.jobServiceConfiguration.setFailedJobCommandFactory(this.failedJobCommandFactory);

        this.jobServiceConfiguration.init();
        
        if (this.jobHandlers != null) {
            for (String type : this.jobHandlers.keySet()) {
                this.jobServiceConfiguration.addJobHandler(type, this.jobHandlers.get(type));
            }
        }
        
        if (this.historyJobHandlers != null) {
            for (String type : this.historyJobHandlers.keySet()) {
                this.jobServiceConfiguration.addHistoryJobHandler(type, this.historyJobHandlers.get(type));
            }
        }

        addServiceConfiguration(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG, this.jobServiceConfiguration);
    }

    protected JobServiceConfiguration instantiateJobServiceConfiguration() {
        return new JobServiceConfiguration(ScopeTypes.CMMN);
    }
    
    public void addJobHandler(JobHandler jobHandler) {
        this.jobHandlers.put(jobHandler.getType(), jobHandler);
        if (this.jobServiceConfiguration != null) {
            this.jobServiceConfiguration.addJobHandler(jobHandler.getType(), jobHandler);
        }
    }
    
    public void addHistoryJobHandler(HistoryJobHandler historyJobHandler) {
        this.historyJobHandlers.put(historyJobHandler.getType(), historyJobHandler);
        if (this.jobServiceConfiguration != null) {
            this.jobServiceConfiguration.addHistoryJobHandler(historyJobHandler.getType(), historyJobHandler);
        }
    }

    protected void initAsyncTaskExecutor() {
        if (this.asyncTaskExecutor == null) {
            DefaultAsyncTaskExecutor defaultAsyncTaskExecutor = new DefaultAsyncTaskExecutor();

            // Thread pool config
            defaultAsyncTaskExecutor.setCorePoolSize(asyncExecutorCorePoolSize);
            defaultAsyncTaskExecutor.setMaxPoolSize(asyncExecutorMaxPoolSize);
            defaultAsyncTaskExecutor.setKeepAliveTime(asyncExecutorThreadKeepAliveTime);

            // Threadpool queue
            if (asyncExecutorThreadPoolQueue != null) {
                defaultAsyncTaskExecutor.setThreadPoolQueue(asyncExecutorThreadPoolQueue);
            }
            defaultAsyncTaskExecutor.setQueueSize(asyncExecutorThreadPoolQueueSize);

            defaultAsyncTaskExecutor.setThreadFactory(asyncExecutorThreadFactory);

            // Core thread timeout
            defaultAsyncTaskExecutor.setAllowCoreThreadTimeout(asyncExecutorAllowCoreThreadTimeout);

            // Shutdown
            defaultAsyncTaskExecutor.setSecondsToWaitOnShutdown(asyncExecutorSecondsToWaitOnShutdown);

            defaultAsyncTaskExecutor.start();
            this.shutdownAsyncTaskExecutor = true;

            this.asyncTaskExecutor = defaultAsyncTaskExecutor;
        }

        if (this.asyncTaskInvoker == null) {
            this.asyncTaskInvoker = new DefaultAsyncTaskInvoker(asyncTaskExecutor);
        }
    }

    public void initAsyncExecutor() {
        initAsyncTaskExecutor();
        if (asyncExecutor == null) {
            DefaultAsyncJobExecutor defaultAsyncExecutor = new DefaultAsyncJobExecutor();
            if (asyncExecutorExecuteAsyncRunnableFactory != null) {
                defaultAsyncExecutor.setExecuteAsyncRunnableFactory(asyncExecutorExecuteAsyncRunnableFactory);
            }

            // Message queue mode
            defaultAsyncExecutor.setMessageQueueMode(asyncExecutorMessageQueueMode);

            // Thread flags
            defaultAsyncExecutor.setAsyncJobAcquisitionEnabled(isAsyncExecutorAsyncJobAcquisitionEnabled);
            defaultAsyncExecutor.setTimerJobAcquisitionEnabled(isAsyncExecutorTimerJobAcquisitionEnabled);
            defaultAsyncExecutor.setResetExpiredJobEnabled(isAsyncExecutorResetExpiredJobsEnabled);

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
            defaultAsyncExecutor.setUnlockOwnedJobs(asyncExecutorUnlockOwnedJobs);

            // Reset expired
            defaultAsyncExecutor.setResetExpiredJobsInterval(asyncExecutorResetExpiredJobsInterval);
            defaultAsyncExecutor.setResetExpiredJobsPageSize(asyncExecutorResetExpiredJobsPageSize);

            // Tenant
            defaultAsyncExecutor.setTenantId(asyncExecutorTenantId);

            asyncExecutor = defaultAsyncExecutor;
        }

        // Task executor
        if (asyncExecutor.getTaskExecutor() == null) {
            asyncExecutor.setTaskExecutor(asyncTaskExecutor);
        }

        asyncExecutor.setJobServiceConfiguration(jobServiceConfiguration);
        asyncExecutor.setAutoActivate(asyncExecutorActivate);
        jobServiceConfiguration.setAsyncExecutor(asyncExecutor);
    }
    
    protected void initAsyncHistoryTaskExecutor() {
        if (this.asyncHistoryTaskExecutor == null) {
            DefaultAsyncTaskExecutor defaultAsyncTaskExecutor = new DefaultAsyncTaskExecutor();

            // Thread pool config
            defaultAsyncTaskExecutor.setCorePoolSize(asyncHistoryExecutorCorePoolSize);
            defaultAsyncTaskExecutor.setMaxPoolSize(asyncHistoryExecutorMaxPoolSize);
            defaultAsyncTaskExecutor.setKeepAliveTime(asyncHistoryExecutorThreadKeepAliveTime);

            // Threadpool queue
            if (asyncHistoryExecutorThreadPoolQueue != null) {
                defaultAsyncTaskExecutor.setThreadPoolQueue(asyncHistoryExecutorThreadPoolQueue);
            }
            defaultAsyncTaskExecutor.setQueueSize(asyncHistoryExecutorThreadPoolQueueSize);

            // Core thread timeout
            //defaultAsyncTaskExecutor.setAllowCoreThreadTimeout(asyncHistoryExecutorAllowCoreThreadTimeout);

            // Shutdown
            defaultAsyncTaskExecutor.setSecondsToWaitOnShutdown(asyncHistoryExecutorSecondsToWaitOnShutdown);

            defaultAsyncTaskExecutor.setThreadPoolNamingPattern("flowable-async-history-job-executor-thread-%d");

            defaultAsyncTaskExecutor.start();
            shutdownAsyncHistoryTaskExecutor = true;

            this.asyncHistoryTaskExecutor = defaultAsyncTaskExecutor;
        }
    }

    public void initAsyncHistoryExecutor() {
        if (isAsyncHistoryEnabled) {
            initAsyncHistoryTaskExecutor();
            
            if (asyncHistoryExecutor == null) {
                DefaultAsyncHistoryJobExecutor defaultAsyncHistoryExecutor = new DefaultAsyncHistoryJobExecutor();
    
                // Message queue mode
                defaultAsyncHistoryExecutor.setMessageQueueMode(asyncHistoryExecutorMessageQueueMode);
    
                // Thread flags
                defaultAsyncHistoryExecutor.setAsyncJobAcquisitionEnabled(isAsyncHistoryExecutorAsyncJobAcquisitionEnabled);
                defaultAsyncHistoryExecutor.setResetExpiredJobEnabled(isAsyncHistoryExecutorResetExpiredJobsEnabled);

                // Page size
                defaultAsyncHistoryExecutor.setMaxAsyncJobsDuePerAcquisition(asyncHistoryExecutorMaxJobsDuePerAcquisition);
    
                // Acquisition wait time
                defaultAsyncHistoryExecutor.setDefaultAsyncJobAcquireWaitTimeInMillis(asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime);
    
                // Queue full wait time
                defaultAsyncHistoryExecutor.setDefaultQueueSizeFullWaitTimeInMillis(asyncHistoryExecutorDefaultQueueSizeFullWaitTime);
    
                // Job locking
                defaultAsyncHistoryExecutor.setAsyncJobLockTimeInMillis(asyncHistoryExecutorAsyncJobLockTimeInMillis);
                if (asyncHistoryExecutorLockOwner != null) {
                    defaultAsyncHistoryExecutor.setLockOwner(asyncHistoryExecutorLockOwner);
                }
    
                // Reset expired
                defaultAsyncHistoryExecutor.setResetExpiredJobsInterval(asyncHistoryExecutorResetExpiredJobsInterval);
                defaultAsyncHistoryExecutor.setResetExpiredJobsPageSize(asyncHistoryExecutorResetExpiredJobsPageSize);
    
                asyncHistoryExecutor = defaultAsyncHistoryExecutor;
                
                if (asyncHistoryExecutor.getJobServiceConfiguration() == null) {
                    asyncHistoryExecutor.setJobServiceConfiguration(jobServiceConfiguration);
                }
                
            } else {
                // In case an async history executor was injected, only the job handlers are set. 
                // In the normal case, these are set on the jobServiceConfiguration, but these are not shared between instances
                if (historyJobHandlers != null) {
                    if (asyncHistoryExecutor.getJobServiceConfiguration() == null) {
                        asyncHistoryExecutor.setJobServiceConfiguration(jobServiceConfiguration);
                    }
                    historyJobHandlers.forEach((type, handler) -> { asyncHistoryExecutor.getJobServiceConfiguration().mergeHistoryJobHandler(handler); });
                }
                
            }
        }

        if (asyncHistoryExecutor != null) {

            // Task executor
            if (asyncHistoryExecutor.getTaskExecutor() == null) {
                asyncHistoryExecutor.setTaskExecutor(asyncHistoryTaskExecutor);
            }
            jobServiceConfiguration.setAsyncHistoryExecutor(asyncHistoryExecutor);
            jobServiceConfiguration.setAsyncHistoryExecutorNumberOfRetries(asyncHistoryExecutorNumberOfRetries);

            asyncHistoryExecutor.setAutoActivate(asyncHistoryExecutorActivate);
        }
    }

    @Override
    public void close() {
        super.close();

        if (asyncTaskExecutor != null && shutdownAsyncTaskExecutor) {
            // Only shutdown if it was created by this configuration
            asyncTaskExecutor.shutdown();
        }

        if (asyncHistoryTaskExecutor != null && shutdownAsyncHistoryTaskExecutor) {
            // Only shutdown if it was created by this configuration
            asyncHistoryTaskExecutor.shutdown();
        }
    }

    @Override
    protected List<EngineConfigurator> getEngineSpecificEngineConfigurators() {
        if (!disableIdmEngine || !disableEventRegistry) {
            List<EngineConfigurator> specificConfigurators = new ArrayList<>();
            
            if (!disableIdmEngine) {
                if (idmEngineConfigurator != null) {
                    specificConfigurators.add(idmEngineConfigurator);
                } else {
                    specificConfigurators.add(new IdmEngineConfigurator());
                }
            }
            
            if (!disableEventRegistry) {
                if (eventRegistryConfigurator != null) {
                    specificConfigurators.add(eventRegistryConfigurator);
                } else {
                    specificConfigurators.add(createDefaultEventRegistryEngineConfigurator());
                }
            }
            
            return specificConfigurators;
        }
        return Collections.emptyList();
    }

    protected EngineConfigurator createDefaultEventRegistryEngineConfigurator() {
        return new EventRegistryEngineConfigurator();
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

    @Override
    public DynamicCmmnService getDynamicCmmnService() {
        return dynamicCmmnService;
    }

    public CmmnEngineConfiguration setDynamicCmmnService(DynamicCmmnService dynamicCmmnService) {
        this.dynamicCmmnService = dynamicCmmnService;
        return this;
    }

    @Override
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

    @Override
    public CmmnMigrationService getCmmnMigrationService() {
        return cmmnMigrationService;
    }

    public void setCmmnMigrationService(CmmnMigrationService cmmnMigrationService) {
        this.cmmnMigrationService = cmmnMigrationService;
    }

    public IdmIdentityService getIdmIdentityService() {
        IdmIdentityService idmIdentityService = null;
        IdmEngineConfigurationApi idmEngineConfiguration = (IdmEngineConfigurationApi) engineConfigurations.get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
        if (idmEngineConfiguration != null) {
            idmIdentityService = idmEngineConfiguration.getIdmIdentityService();
        }

        return idmIdentityService;
    }

    public CmmnEngineAgendaFactory getCmmnEngineAgendaFactory() {
        return cmmnEngineAgendaFactory;
    }

    public CmmnEngineConfiguration setCmmnEngineAgendaFactory(CmmnEngineAgendaFactory cmmnEngineAgendaFactory) {
        this.cmmnEngineAgendaFactory = cmmnEngineAgendaFactory;
        return this;
    }

    @Override
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

    public HistoricPlanItemInstanceEntityManager getHistoricPlanItemInstanceEntityManager() {
        return historicPlanItemInstanceEntityManager;
    }

    public CmmnEngineConfiguration setHistoricPlanItemInstanceEntityManager(HistoricPlanItemInstanceEntityManager historicPlanItemInstanceEntityManager) {
        this.historicPlanItemInstanceEntityManager = historicPlanItemInstanceEntityManager;
        return this;
    }

    public CaseInstanceHelper getCaseInstanceHelper() {
        return caseInstanceHelper;
    }

    public CmmnEngineConfiguration setCaseInstanceHelper(CaseInstanceHelper caseInstanceHelper) {
        this.caseInstanceHelper = caseInstanceHelper;
        return this;
    }
    
    public CandidateManager getCandidateManager() {
        return candidateManager;
    }

    public CmmnEngineConfiguration setCandidateManager(CandidateManager candidateManager) {
        this.candidateManager = candidateManager;
        return this;
    }

    public PlanItemVariableAggregator getVariableAggregator() {
        return variableAggregator;
    }

    public CmmnEngineConfiguration setVariableAggregator(PlanItemVariableAggregator variableAggregator) {
        this.variableAggregator = variableAggregator;
        return this;
    }

    public DecisionTableVariableManager getDecisionTableVariableManager() {
        return decisionTableVariableManager;
    }

    public CmmnEngineConfiguration setDecisionTableVariableManager(DecisionTableVariableManager decisionTableVariableManager) {
        this.decisionTableVariableManager = decisionTableVariableManager;
        return this;
    }

    public CmmnHistoryManager getCmmnHistoryManager() {
        return cmmnHistoryManager;
    }

    public CmmnEngineConfiguration setCmmnHistoryManager(CmmnHistoryManager cmmnHistoryManager) {
        this.cmmnHistoryManager = cmmnHistoryManager;
        return this;
    }

    public CmmnDynamicStateManager getDynamicStateManager() {
        return dynamicStateManager;
    }

    public CmmnEngineConfiguration setDynamicStateManager(CmmnDynamicStateManager dynamicStateManager) {
        this.dynamicStateManager = dynamicStateManager;
        return this;
    }

    public CaseInstanceMigrationManager getCaseInstanceMigrationManager() {
        return caseInstanceMigrationManager;
    }

    public CmmnEngineConfiguration setCaseInstanceMigrationManager(CaseInstanceMigrationManager caseInstanceMigrationManager) {
        this.caseInstanceMigrationManager = caseInstanceMigrationManager;
        return this;
    }

    public boolean isEnableSafeCmmnXml() {
        return enableSafeCmmnXml;
    }

    public CmmnEngineConfiguration setEnableSafeCmmnXml(boolean enableSafeCmmnXml) {
        this.enableSafeCmmnXml = enableSafeCmmnXml;
        return this;
    }
    
    public boolean isDisableCmmnXmlValidation() {
        return disableCmmnXmlValidation;
    }

    public void setDisableCmmnXmlValidation(boolean disableCmmnXmlValidation) {
        this.disableCmmnXmlValidation = disableCmmnXmlValidation;
    }

    public CmmnParser getCmmnParser() {
        return cmmnParser;
    }

    public CmmnEngineConfiguration setCmmnParser(CmmnParser cmmnParser) {
        this.cmmnParser = cmmnParser;
        return this;
    }

    public List<CmmnParseHandler> getPreCmmnParseHandlers() {
        return preCmmnParseHandlers;
    }

    public CmmnEngineConfiguration setPreCmmnParseHandlers(List<CmmnParseHandler> preCmmnParseHandlers) {
        this.preCmmnParseHandlers = preCmmnParseHandlers;
        return this;
    }

    public List<CmmnParseHandler> getPostCmmnParseHandlers() {
        return postCmmnParseHandlers;
    }

    public CmmnEngineConfiguration setPostCmmnParseHandlers(List<CmmnParseHandler> postCmmnParseHandlers) {
        this.postCmmnParseHandlers = postCmmnParseHandlers;
        return this;
    }

    public List<CmmnParseHandler> getCustomCmmnParseHandlers() {
        return customCmmnParseHandlers;
    }

    public CmmnEngineConfiguration setCustomCmmnParseHandlers(List<CmmnParseHandler> customCmmnParseHandlers) {
        this.customCmmnParseHandlers = customCmmnParseHandlers;
        return this;
    }

    public CmmnListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    public CmmnEngineConfiguration setListenerFactory(CmmnListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
        return this;
    }

    public CmmnListenerNotificationHelper getListenerNotificationHelper() {
        return listenerNotificationHelper;
    }

    public CmmnEngineConfiguration setListenerNotificationHelper(CmmnListenerNotificationHelper listenerNotificationHelper) {
        this.listenerNotificationHelper = listenerNotificationHelper;
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

    public Map<String, List<PlanItemInstanceLifecycleListener>> getPlanItemInstanceLifecycleListeners() {
        return planItemInstanceLifecycleListeners;
    }

    public CmmnEngineConfiguration setPlanItemInstanceLifecycleListeners(Map<String, List<PlanItemInstanceLifecycleListener>> planItemInstanceLifecycleListeners) {
        this.planItemInstanceLifecycleListeners = planItemInstanceLifecycleListeners;
        return this;
    }

    public List<CaseInstanceLifecycleListener> getCaseInstanceLifecycleListeners() {
        return caseInstanceLifecycleListeners;
    }

    public CmmnEngineConfiguration setCaseInstanceLifecycleListeners(List<CaseInstanceLifecycleListener> caseInstanceLifecycleListeners) {
        this.caseInstanceLifecycleListeners = caseInstanceLifecycleListeners;
        return this;
    }

    public StartCaseInstanceInterceptor getStartCaseInstanceInterceptor() {
        return startCaseInstanceInterceptor;
    }

    public CmmnEngineConfiguration setStartCaseInstanceInterceptor(StartCaseInstanceInterceptor startCaseInstanceInterceptor) {
        this.startCaseInstanceInterceptor = startCaseInstanceInterceptor;
        return this;
    }
    
    public CreateHumanTaskInterceptor getCreateHumanTaskInterceptor() {
        return createHumanTaskInterceptor;
    }

    public CmmnEngineConfiguration setCreateHumanTaskInterceptor(CreateHumanTaskInterceptor createHumanTaskInterceptor) {
        this.createHumanTaskInterceptor = createHumanTaskInterceptor;
        return this;
    }

    public CreateCasePageTaskInterceptor getCreateCasePageTaskInterceptor() {
        return createCasePageTaskInterceptor;
    }

    public CmmnEngineConfiguration setCreateCasePageTaskInterceptor(CreateCasePageTaskInterceptor createCasePageTaskInterceptor) {
        this.createCasePageTaskInterceptor = createCasePageTaskInterceptor;
        return this;
    }

    public CreateCmmnExternalWorkerJobInterceptor getCreateCmmnExternalWorkerJobInterceptor() {
        return createCmmnExternalWorkerJobInterceptor;
    }

    public CmmnEngineConfiguration setCreateCmmnExternalWorkerJobInterceptor(CreateCmmnExternalWorkerJobInterceptor createCmmnExternalWorkerJobInterceptor) {
        this.createCmmnExternalWorkerJobInterceptor = createCmmnExternalWorkerJobInterceptor;
        return this;
    }

    /**
     * Registers a global {@link PlanItemInstanceLifecycleListener} to listen to {@link org.flowable.cmmn.api.runtime.PlanItemInstance} state changes.
     *
     * @param planItemDefinitionType A string from {@link org.flowable.cmmn.api.runtime.PlanItemDefinitionType}.
     *                               If null is passed, the listener will be invoked for any type.
     * @param planItemInstanceLifeCycleListener The listener instance.
     */
    public void addPlanItemInstanceLifeCycleListener(String planItemDefinitionType, PlanItemInstanceLifecycleListener planItemInstanceLifeCycleListener) {
        if (planItemInstanceLifecycleListeners == null) {
            planItemInstanceLifecycleListeners = new HashMap<>();
        }
        planItemInstanceLifecycleListeners
            .computeIfAbsent(planItemDefinitionType, key -> new ArrayList<>())
            .add(planItemInstanceLifeCycleListener);
    }

    /**
     * Register a global {@link PlanItemInstanceLifecycleListener} to listen to any (all plan item definition types)
     * {@link org.flowable.cmmn.api.runtime.PlanItemInstance} state changes.
     */
    public void addPlanItemInstanceLifeCycleListener(PlanItemInstanceLifecycleListener planItemInstanceLifeCycleListener) {
        addPlanItemInstanceLifeCycleListener(null, planItemInstanceLifeCycleListener);
    }

    /**
     * Registers a global {@link CaseInstanceLifecycleListener} to listen to {@link org.flowable.cmmn.api.runtime.CaseInstance} state changes.
     */
    public void addCaseInstanceLifeCycleListener(CaseInstanceLifecycleListener caseInstanceLifecycleListener) {
        if (caseInstanceLifecycleListeners == null) {
            caseInstanceLifecycleListeners = new ArrayList<>();
        }
        caseInstanceLifecycleListeners.add(caseInstanceLifecycleListener);
    }

    @Override
    public CmmnEngineConfiguration setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
        return this;
    }

    public boolean isExecuteServiceSchemaManagers() {
        return executeServiceSchemaManagers;
    }

    public void setExecuteServiceSchemaManagers(boolean executeServiceSchemaManagers) {
        this.executeServiceSchemaManagers = executeServiceSchemaManagers;
    }

    public HistoryLevel getHistoryLevel() {
        return historyLevel;
    }

    public CmmnEngineConfiguration setHistoryLevel(HistoryLevel historyLevel) {
        this.historyLevel = historyLevel;
        return this;
    }

    @Override
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    @Override
    public CmmnEngineConfiguration setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }
    
    public boolean isExpressionCacheEnabled() {
        return isExpressionCacheEnabled;
    }

    public CmmnEngineConfiguration setExpressionCacheEnabled(boolean isExpressionCacheEnabled) {
        this.isExpressionCacheEnabled = isExpressionCacheEnabled;
        return this;
    }

    public int getExpressionCacheSize() {
        return expressionCacheSize;
    }

    public CmmnEngineConfiguration setExpressionCacheSize(int expressionCacheSize) {
        this.expressionCacheSize = expressionCacheSize;
        return this;
    }

    public int getExpressionTextLengthCacheLimit() {
        return expressionTextLengthCacheLimit;
    }

    public CmmnEngineConfiguration setExpressionTextLengthCacheLimit(int expressionTextLengthCacheLimit) {
        this.expressionTextLengthCacheLimit = expressionTextLengthCacheLimit;
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

    public List<FlowableAstFunctionCreator> getAstFunctionCreators() {
        return astFunctionCreators;
    }

    public void setAstFunctionCreators(List<FlowableAstFunctionCreator> astFunctionCreators) {
        this.astFunctionCreators = astFunctionCreators;
    }

    public Collection<ELResolver> getPreDefaultELResolvers() {
        return preDefaultELResolvers;
    }

    public CmmnEngineConfiguration setPreDefaultELResolvers(Collection<ELResolver> preDefaultELResolvers) {
        this.preDefaultELResolvers = preDefaultELResolvers;
        return this;
    }

    public CmmnEngineConfiguration addPreDefaultELResolver(ELResolver elResolver) {
        if (this.preDefaultELResolvers == null) {
            this.preDefaultELResolvers = new ArrayList<>();
        }

        this.preDefaultELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPreBeanELResolvers() {
        return preBeanELResolvers;
    }

    public CmmnEngineConfiguration setPreBeanELResolvers(Collection<ELResolver> preBeanELResolvers) {
        this.preBeanELResolvers = preBeanELResolvers;
        return this;
    }

    public CmmnEngineConfiguration addPreBeanELResolver(ELResolver elResolver) {
        if (this.preBeanELResolvers == null) {
            this.preBeanELResolvers = new ArrayList<>();
        }

        this.preBeanELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPostDefaultELResolvers() {
        return postDefaultELResolvers;
    }

    public CmmnEngineConfiguration setPostDefaultELResolvers(Collection<ELResolver> postDefaultELResolvers) {
        this.postDefaultELResolvers = postDefaultELResolvers;
        return this;
    }

    public CmmnEngineConfiguration addPostDefaultELResolver(ELResolver elResolver) {
        if (this.postDefaultELResolvers == null) {
            this.postDefaultELResolvers = new ArrayList<>();
        }

        this.postDefaultELResolvers.add(elResolver);
        return this;
    }

    public SchemaManager getIdentityLinkSchemaManager() {
        return identityLinkSchemaManager;
    }

    public CmmnEngineConfiguration setIdentityLinkSchemaManager(SchemaManager identityLinkSchemaManager) {
        this.identityLinkSchemaManager = identityLinkSchemaManager;
        return this;
    }
    
    public SchemaManager getEntityLinkSchemaManager() {
        return entityLinkSchemaManager;
    }

    public CmmnEngineConfiguration setEntityLinkSchemaManager(SchemaManager entityLinkSchemaManager) {
        this.entityLinkSchemaManager = entityLinkSchemaManager;
        return this;
    }

    public SchemaManager getEventSubscriptionSchemaManager() {
        return eventSubscriptionSchemaManager;
    }

    public CmmnEngineConfiguration setEventSubscriptionSchemaManager(SchemaManager eventSubscriptionSchemaManager) {
        this.eventSubscriptionSchemaManager = eventSubscriptionSchemaManager;
        return this;
    }

    public SchemaManager getVariableSchemaManager() {
        return variableSchemaManager;
    }

    public CmmnEngineConfiguration setVariableSchemaManager(SchemaManager variableSchemaManager) {
        this.variableSchemaManager = variableSchemaManager;
        return this;
    }

    public SchemaManager getTaskSchemaManager() {
        return taskSchemaManager;
    }

    public CmmnEngineConfiguration setTaskSchemaManager(SchemaManager taskSchemaManager) {
        this.taskSchemaManager = taskSchemaManager;
        return this;
    }

    public SchemaManager getJobSchemaManager() {
        return jobSchemaManager;
    }

    public CmmnEngineConfiguration setJobSchemaManager(SchemaManager jobSchemaManager) {
        this.jobSchemaManager = jobSchemaManager;
        return this;
    }

    @Override
    public VariableTypes getVariableTypes() {
        return variableTypes;
    }

    @Override
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
    
    public EntityLinkServiceConfiguration getEntityLinkServiceConfiguration() {
        return entityLinkServiceConfiguration;
    }

    public CmmnEngineConfiguration setEntityLinkServiceConfiguration(EntityLinkServiceConfiguration entityLinkServiceConfiguration) {
        this.entityLinkServiceConfiguration = entityLinkServiceConfiguration;
        return this;
    }

    @Override
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

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public CmmnEngineConfiguration setTaskQueryLimit(int taskQueryLimit) {
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public CmmnEngineConfiguration setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
        return this;
    }

    public InternalHistoryVariableManager getInternalHistoryVariableManager() {
        return internalHistoryVariableManager;
    }

    public CmmnEngineConfiguration setInternalHistoryVariableManager(InternalHistoryVariableManager internalHistoryVariableManager) {
        this.internalHistoryVariableManager = internalHistoryVariableManager;
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public CmmnEngineConfiguration setCaseQueryLimit(int caseQueryLimit) {
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public void setHistoricCaseQueryLimit(int historicCaseQueryLimit) {

    }

    public boolean isSerializableVariableTypeTrackDeserializedObjects() {
        return serializableVariableTypeTrackDeserializedObjects;
    }

    public CmmnEngineConfiguration setSerializableVariableTypeTrackDeserializedObjects(boolean serializableVariableTypeTrackDeserializedObjects) {
        this.serializableVariableTypeTrackDeserializedObjects = serializableVariableTypeTrackDeserializedObjects;
        return this;
    }

    public boolean isJsonVariableTypeTrackObjects() {
        return jsonVariableTypeTrackObjects;
    }

    public CmmnEngineConfiguration setJsonVariableTypeTrackObjects(boolean jsonVariableTypeTrackObjects) {
        this.jsonVariableTypeTrackObjects = jsonVariableTypeTrackObjects;
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

    public boolean isDisableIdmEngine() {
        return disableIdmEngine;
    }

    public CmmnEngineConfiguration setDisableIdmEngine(boolean disableIdmEngine) {
        this.disableIdmEngine = disableIdmEngine;
        return this;
    }
    
    public boolean isDisableEventRegistry() {
        return disableEventRegistry;
    }

    public CmmnEngineConfiguration setDisableEventRegistry(boolean disableEventRegistry) {
        this.disableEventRegistry = disableEventRegistry;
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
    
    public EventRegistryEventConsumer getEventRegistryEventConsumer() {
        return eventRegistryEventConsumer;
    }

    public CmmnEngineConfiguration setEventRegistryEventConsumer(EventRegistryEventConsumer eventRegistryEventConsumer) {
        this.eventRegistryEventConsumer = eventRegistryEventConsumer;
        return this;
    }

    public AsyncExecutor getAsyncExecutor() {
        return asyncExecutor;
    }

    public CmmnEngineConfiguration setAsyncExecutor(AsyncExecutor asyncExecutor) {
        this.asyncExecutor = asyncExecutor;
        return this;
    }

    public AsyncTaskExecutor getAsyncTaskExecutor() {
        return asyncTaskExecutor;
    }

    public CmmnEngineConfiguration setAsyncTaskExecutor(AsyncTaskExecutor asyncTaskExecutor) {
        this.asyncTaskExecutor = asyncTaskExecutor;
        return this;
    }

    public AsyncTaskInvoker getAsyncTaskInvoker() {
        return asyncTaskInvoker;
    }

    public CmmnEngineConfiguration setAsyncTaskInvoker(AsyncTaskInvoker asyncTaskInvoker) {
        this.asyncTaskInvoker = asyncTaskInvoker;
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

    public boolean isAsyncExecutorAllowCoreThreadTimeout() {
        return asyncExecutorAllowCoreThreadTimeout;
    }

    public CmmnEngineConfiguration setAsyncExecutorAllowCoreThreadTimeout(boolean asyncExecutorAllowCoreThreadTimeout) {
        this.asyncExecutorAllowCoreThreadTimeout = asyncExecutorAllowCoreThreadTimeout;
        return this;
    }

    public ThreadFactory getAsyncExecutorThreadFactory() {
        return asyncExecutorThreadFactory;
    }

    public CmmnEngineConfiguration setAsyncExecutorThreadFactory(ThreadFactory asyncExecutorThreadFactory) {
        this.asyncExecutorThreadFactory = asyncExecutorThreadFactory;
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

    public boolean isAsyncExecutorUnlockOwnedJobs() {
        return asyncExecutorUnlockOwnedJobs;
    }

    public void setAsyncExecutorUnlockOwnedJobs(boolean asyncExecutorUnlockOwnedJobs) {
        this.asyncExecutorUnlockOwnedJobs = asyncExecutorUnlockOwnedJobs;
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
    
    public AsyncExecutor getAsyncHistoryExecutor() {
        return asyncHistoryExecutor;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutor(AsyncExecutor asyncHistoryExecutor) {
        this.asyncHistoryExecutor = asyncHistoryExecutor;
        return this;
    }
    
    public AsyncTaskExecutor getAsyncHistoryTaskExecutor() {
        return asyncHistoryTaskExecutor;
    }

    public CmmnEngineConfiguration setAsyncHistoryTaskExecutor(AsyncTaskExecutor asyncHistoryTaskExecutor) {
        this.asyncHistoryTaskExecutor = asyncHistoryTaskExecutor;
        return this;
    }

    public HistoricPlanItemInstanceDataManager getHistoricPlanItemInstanceDataManager() {
        return historicPlanItemInstanceDataManager;
    }

    public CmmnEngineConfiguration setHistoricPlanItemInstanceDataManager(HistoricPlanItemInstanceDataManager historicPlanItemInstanceDataManager) {
        this.historicPlanItemInstanceDataManager = historicPlanItemInstanceDataManager;
        return this;
    }

    public InternalTaskAssignmentManager getInternalTaskAssignmentManager() {
        return internalTaskAssignmentManager;
    }

    public CmmnEngineConfiguration setInternalTaskAssignmentManager(InternalTaskAssignmentManager internalTaskAssignmentManager) {
        this.internalTaskAssignmentManager = internalTaskAssignmentManager;
        return this;
    }

    public IdentityLinkEventHandler getIdentityLinkEventHandler() {
        return identityLinkEventHandler;
    }

    public CmmnEngineConfiguration setIdentityLinkEventHandler(IdentityLinkEventHandler identityLinkEventHandler) {
        this.identityLinkEventHandler = identityLinkEventHandler;
        return this;
    }

    public InternalJobParentStateResolver getInternalJobParentStateResolver() {
        return internalJobParentStateResolver;
    }

    public CmmnEngineConfiguration setInternalJobParentStateResolver(InternalJobParentStateResolver internalJobParentStateResolver) {
        this.internalJobParentStateResolver = internalJobParentStateResolver;
        return this;
    }

    public boolean isAsyncHistoryEnabled() {
        return isAsyncHistoryEnabled;
    }

    public CmmnEngineConfiguration setAsyncHistoryEnabled(boolean isAsyncHistoryEnabled) {
        this.isAsyncHistoryEnabled = isAsyncHistoryEnabled;
        return this;
    }
    
    public boolean isAsyncHistoryExecutorActivate() {
        return asyncHistoryExecutorActivate;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorActivate(boolean asyncHistoryExecutorActivate) {
        this.asyncHistoryExecutorActivate = asyncHistoryExecutorActivate;
        return this;
    }

    public boolean isAsyncHistoryJsonGzipCompressionEnabled() {
        return isAsyncHistoryJsonGzipCompressionEnabled;
    }

    public CmmnEngineConfiguration setAsyncHistoryJsonGzipCompressionEnabled(boolean isAsyncHistoryJsonGzipCompressionEnabled) {
        this.isAsyncHistoryJsonGzipCompressionEnabled = isAsyncHistoryJsonGzipCompressionEnabled;
        return this;
    }

    public boolean isAsyncHistoryJsonGroupingEnabled() {
        return isAsyncHistoryJsonGroupingEnabled;
    }

    public CmmnEngineConfiguration setAsyncHistoryJsonGroupingEnabled(boolean isAsyncHistoryJsonGroupingEnabled) {
        this.isAsyncHistoryJsonGroupingEnabled = isAsyncHistoryJsonGroupingEnabled;
        return this;
    }

    public int getAsyncHistoryJsonGroupingThreshold() {
        return asyncHistoryJsonGroupingThreshold;
    }

    public CmmnEngineConfiguration setAsyncHistoryJsonGroupingThreshold(int asyncHistoryJsonGroupingThreshold) {
        this.asyncHistoryJsonGroupingThreshold = asyncHistoryJsonGroupingThreshold;
        return this;
    }
    
    public boolean isAsyncHistoryExecutorMessageQueueMode() {
        return asyncHistoryExecutorMessageQueueMode;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorMessageQueueMode(boolean asyncHistoryExecutorMessageQueueMode) {
        this.asyncHistoryExecutorMessageQueueMode = asyncHistoryExecutorMessageQueueMode;
        return this;
    }

    public AsyncHistoryListener getAsyncHistoryListener() {
        if (asyncHistoryListener == null) {
            asyncHistoryListener = new DefaultAsyncHistoryJobProducer();
        }
        return asyncHistoryListener;
    }

    public CmmnEngineConfiguration setAsyncHistoryListener(AsyncHistoryListener asyncHistoryListener) {
        this.asyncHistoryListener = asyncHistoryListener;
        return this;
    }

    public int getAsyncHistoryExecutorNumberOfRetries() {
        return asyncHistoryExecutorNumberOfRetries;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorNumberOfRetries(int asyncHistoryExecutorNumberOfRetries) {
        this.asyncHistoryExecutorNumberOfRetries = asyncHistoryExecutorNumberOfRetries;
        return this;
    }

    public int getAsyncHistoryExecutorCorePoolSize() {
        return asyncHistoryExecutorCorePoolSize;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorCorePoolSize(int asyncHistoryExecutorCorePoolSize) {
        this.asyncHistoryExecutorCorePoolSize = asyncHistoryExecutorCorePoolSize;
        return this;
    }

    public int getAsyncHistoryExecutorMaxPoolSize() {
        return asyncHistoryExecutorMaxPoolSize;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorMaxPoolSize(int asyncHistoryExecutorMaxPoolSize) {
        this.asyncHistoryExecutorMaxPoolSize = asyncHistoryExecutorMaxPoolSize;
        return this;
    }

    public long getAsyncHistoryExecutorThreadKeepAliveTime() {
        return asyncHistoryExecutorThreadKeepAliveTime;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorThreadKeepAliveTime(long asyncHistoryExecutorThreadKeepAliveTime) {
        this.asyncHistoryExecutorThreadKeepAliveTime = asyncHistoryExecutorThreadKeepAliveTime;
        return this;
    }

    public int getAsyncHistoryExecutorThreadPoolQueueSize() {
        return asyncHistoryExecutorThreadPoolQueueSize;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorThreadPoolQueueSize(int asyncHistoryExecutorThreadPoolQueueSize) {
        this.asyncHistoryExecutorThreadPoolQueueSize = asyncHistoryExecutorThreadPoolQueueSize;
        return this;
    }

    public int getAsyncHistoryExecutorMaxJobsDuePerAcquisition() {
        return asyncHistoryExecutorMaxJobsDuePerAcquisition;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorMaxJobsDuePerAcquisition(int asyncHistoryExecutorMaxJobsDuePerAcquisition) {
        this.asyncHistoryExecutorMaxJobsDuePerAcquisition = asyncHistoryExecutorMaxJobsDuePerAcquisition;
        return this;
    }

    public BlockingQueue<Runnable> getAsyncHistoryExecutorThreadPoolQueue() {
        return asyncHistoryExecutorThreadPoolQueue;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorThreadPoolQueue(BlockingQueue<Runnable> asyncHistoryExecutorThreadPoolQueue) {
        this.asyncHistoryExecutorThreadPoolQueue = asyncHistoryExecutorThreadPoolQueue;
        return this;
    }

    public long getAsyncHistoryExecutorSecondsToWaitOnShutdown() {
        return asyncHistoryExecutorSecondsToWaitOnShutdown;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorSecondsToWaitOnShutdown(long asyncHistoryExecutorSecondsToWaitOnShutdown) {
        this.asyncHistoryExecutorSecondsToWaitOnShutdown = asyncHistoryExecutorSecondsToWaitOnShutdown;
        return this;
    }

    public int getAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime() {
        return asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(int asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime) {
        this.asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime = asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime;
        return this;
    }

    public int getAsyncHistoryExecutorDefaultQueueSizeFullWaitTime() {
        return asyncHistoryExecutorDefaultQueueSizeFullWaitTime;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorDefaultQueueSizeFullWaitTime(int asyncHistoryExecutorDefaultQueueSizeFullWaitTime) {
        this.asyncHistoryExecutorDefaultQueueSizeFullWaitTime = asyncHistoryExecutorDefaultQueueSizeFullWaitTime;
        return this;
    }

    public String getAsyncHistoryExecutorLockOwner() {
        return asyncHistoryExecutorLockOwner;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorLockOwner(String asyncHistoryExecutorLockOwner) {
        this.asyncHistoryExecutorLockOwner = asyncHistoryExecutorLockOwner;
        return this;
    }

    public int getAsyncHistoryExecutorAsyncJobLockTimeInMillis() {
        return asyncHistoryExecutorAsyncJobLockTimeInMillis;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorAsyncJobLockTimeInMillis(int asyncHistoryExecutorAsyncJobLockTimeInMillis) {
        this.asyncHistoryExecutorAsyncJobLockTimeInMillis = asyncHistoryExecutorAsyncJobLockTimeInMillis;
        return this;
    }

    public int getAsyncHistoryExecutorResetExpiredJobsInterval() {
        return asyncHistoryExecutorResetExpiredJobsInterval;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorResetExpiredJobsInterval(int asyncHistoryExecutorResetExpiredJobsInterval) {
        this.asyncHistoryExecutorResetExpiredJobsInterval = asyncHistoryExecutorResetExpiredJobsInterval;
        return this;
    }

    public int getAsyncHistoryExecutorResetExpiredJobsPageSize() {
        return asyncHistoryExecutorResetExpiredJobsPageSize;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorResetExpiredJobsPageSize(int asyncHistoryExecutorResetExpiredJobsPageSize) {
        this.asyncHistoryExecutorResetExpiredJobsPageSize = asyncHistoryExecutorResetExpiredJobsPageSize;
        return this;
    }
    
    public boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled() {
        return isAsyncHistoryExecutorAsyncJobAcquisitionEnabled;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorAsyncJobAcquisitionEnabled(boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled) {
        this.isAsyncHistoryExecutorAsyncJobAcquisitionEnabled = isAsyncHistoryExecutorAsyncJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncHistoryExecutorResetExpiredJobsEnabled() {
        return isAsyncHistoryExecutorResetExpiredJobsEnabled;
    }

    public CmmnEngineConfiguration setAsyncHistoryExecutorResetExpiredJobsEnabled(boolean isAsyncHistoryExecutorResetExpiredJobsEnabled) {
        this.isAsyncHistoryExecutorResetExpiredJobsEnabled = isAsyncHistoryExecutorResetExpiredJobsEnabled;
        return this;
    }

    public boolean isAsyncExecutorAsyncJobAcquisitionEnabled() {
        return isAsyncExecutorAsyncJobAcquisitionEnabled;
    }

    public CmmnEngineConfiguration setAsyncExecutorAsyncJobAcquisitionEnabled(boolean isAsyncExecutorAsyncJobAcquisitionEnabled) {
        this.isAsyncExecutorAsyncJobAcquisitionEnabled = isAsyncExecutorAsyncJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncExecutorTimerJobAcquisitionEnabled() {
        return isAsyncExecutorTimerJobAcquisitionEnabled;
    }

    public CmmnEngineConfiguration setAsyncExecutorTimerJobAcquisitionEnabled(boolean isAsyncExecutorTimerJobAcquisitionEnabled) {
        this.isAsyncExecutorTimerJobAcquisitionEnabled = isAsyncExecutorTimerJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncExecutorResetExpiredJobsEnabled() {
        return isAsyncExecutorResetExpiredJobsEnabled;
    }

    public CmmnEngineConfiguration setAsyncExecutorResetExpiredJobsEnabled(boolean isAsyncExecutorResetExpiredJobsEnabled) {
        this.isAsyncExecutorResetExpiredJobsEnabled = isAsyncExecutorResetExpiredJobsEnabled;
        return this;
    }

    public boolean isEnableEntityLinks() {
        return enableEntityLinks;
    }

    public CmmnEngineConfiguration setEnableEntityLinks(boolean enableEntityLinks) {
        this.enableEntityLinks = enableEntityLinks;
        return this;
    }

    public EventSubscriptionServiceConfiguration getEventSubscriptionServiceConfiguration() {
        return eventSubscriptionServiceConfiguration;
    }

    public CmmnEngineConfiguration setEventSubscriptionServiceConfiguration(EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration) {
        this.eventSubscriptionServiceConfiguration = eventSubscriptionServiceConfiguration;
        return this;
    }

    public Map<String, HistoryJobHandler> getHistoryJobHandlers() {
        return historyJobHandlers;
    }

    public CmmnEngineConfiguration setHistoryJobHandlers(Map<String, HistoryJobHandler> historyJobHandlers) {
        this.historyJobHandlers = historyJobHandlers;
        return this;
    }

    public List<HistoryJobHandler> getCustomHistoryJobHandlers() {
        return customHistoryJobHandlers;
    }

    public CmmnEngineConfiguration setCustomHistoryJobHandlers(List<HistoryJobHandler> customHistoryJobHandlers) {
        this.customHistoryJobHandlers = customHistoryJobHandlers;
        return this;
    }

    public List<HistoryJsonTransformer> getCustomHistoryJsonTransformers() {
        return customHistoryJsonTransformers;
    }

    public CmmnEngineConfiguration setCustomHistoryJsonTransformers(List<HistoryJsonTransformer> customHistoryJsonTransformers) {
        this.customHistoryJsonTransformers = customHistoryJsonTransformers;
        return this;
    }
    
    public List<String> getEnabledJobCategories() {
        return enabledJobCategories;
    }

    public CmmnEngineConfiguration setEnabledJobCategories(List<String> enabledJobCategories) {
        this.enabledJobCategories = enabledJobCategories;
        return this;
    }
    
    public CmmnEngineConfiguration addEnabledJobCategory(String jobCategory) {
        if (enabledJobCategories == null) {
            enabledJobCategories = new ArrayList<>();
        }
        
        enabledJobCategories.add(jobCategory);
        return this;
    }

    public String getJobExecutionScope() {
        return jobExecutionScope;
    }

    public CmmnEngineConfiguration setJobExecutionScope(String jobExecutionScope) {
        this.jobExecutionScope = jobExecutionScope;
        return this;
    }
    
    public String getHistoryJobExecutionScope() {
        return historyJobExecutionScope;
    }

    public CmmnEngineConfiguration setHistoryJobExecutionScope(String historyJobExecutionScope) {
        this.historyJobExecutionScope = historyJobExecutionScope;
        return this;
    }

    public HttpClientConfig getHttpClientConfig() {
        return httpClientConfig;
    }

    public CmmnEngineConfiguration setHttpClientConfig(HttpClientConfig httpClientConfig) {
        this.httpClientConfig.merge(httpClientConfig);
        return this;
    }

    public String getMailServerHost() {
        return mailServerHost;
    }

    public CmmnEngineConfiguration setMailServerHost(String mailServerHost) {
        this.mailServerHost = mailServerHost;
        return this;
    }

    public String getMailServerUsername() {
        return mailServerUsername;
    }

    public CmmnEngineConfiguration setMailServerUsername(String mailServerUsername) {
        this.mailServerUsername = mailServerUsername;
        return this;
    }

    public String getMailServerPassword() {
        return mailServerPassword;
    }

    public CmmnEngineConfiguration setMailServerPassword(String mailServerPassword) {
        this.mailServerPassword = mailServerPassword;
        return this;
    }

    public int getMailServerPort() {
        return mailServerPort;
    }

    public CmmnEngineConfiguration setMailServerPort(int mailServerPort) {
        this.mailServerPort = mailServerPort;
        return this;
    }

    public int getMailServerSSLPort() {
        return mailServerSSLPort;
    }

    public CmmnEngineConfiguration setMailServerSSLPort(int mailServerSSLPort) {
        this.mailServerSSLPort = mailServerSSLPort;
        return this;
    }

    public boolean getMailServerUseSSL() {
        return useSSL;
    }

    public CmmnEngineConfiguration setMailServerUseSSL(boolean useSSL) {
        this.useSSL = useSSL;
        return this;
    }

    public boolean getMailServerUseTLS() {
        return useTLS;
    }

    public CmmnEngineConfiguration setMailServerUseTLS(boolean useTLS) {
        this.useTLS = useTLS;
        return this;
    }

    public String getMailServerDefaultFrom() {
        return mailServerDefaultFrom;
    }

    public CmmnEngineConfiguration setMailServerDefaultFrom(String mailServerDefaultFrom) {
        this.mailServerDefaultFrom = mailServerDefaultFrom;
        return this;
    }

    public String getMailServerForceTo() {
        return mailServerForceTo;
    }

    public CmmnEngineConfiguration setMailServerForceTo(String mailServerForceTo) {
        this.mailServerForceTo = mailServerForceTo;
        return this;
    }

    public String getMailSessionJndi() {
        return mailSessionJndi;
    }

    public CmmnEngineConfiguration setMailSessionJndi(String mailSessionJndi) {
        this.mailSessionJndi = mailSessionJndi;
        return this;
    }

    public Map<String, MailServerInfo> getMailServers() {
        return mailServers;
    }

    public CmmnEngineConfiguration setMailServers(Map<String, MailServerInfo> mailServers) {
        this.mailServers = mailServers;
        return this;
    }

    public MailServerInfo getMailServer(String tenantId) {
        return mailServers.get(tenantId);
    }

    public Map<String, String> getMailSessionsJndi() {
        return mailSessionsJndi;
    }

    public CmmnEngineConfiguration setMailSessionsJndi(Map<String, String> mailSessionsJndi) {
        this.mailSessionsJndi = mailSessionsJndi;
        return this;
    }

    public String getMailSessionJndi(String tenantId) {
        return mailSessionsJndi.get(tenantId);
    }

    public FormFieldHandler getFormFieldHandler() {
        return formFieldHandler;
    }

    public CmmnEngineConfiguration setFormFieldHandler(FormFieldHandler formFieldHandler) {
        this.formFieldHandler = formFieldHandler;
        return this;
    }

    public CmmnIdentityLinkInterceptor getIdentityLinkInterceptor() {
        return identityLinkInterceptor;
    }

    public CmmnEngineConfiguration setIdentityLinkInterceptor(CmmnIdentityLinkInterceptor identityLinkInterceptor) {
        this.identityLinkInterceptor = identityLinkInterceptor;
        return this;
    }

    public boolean isFormFieldValidationEnabled() {
        return this.isFormFieldValidationEnabled;
    }

    public CmmnEngineConfiguration setFormFieldValidationEnabled(boolean flag) {
        this.isFormFieldValidationEnabled = flag;
        return this;
    }

    public TaskPostProcessor getTaskPostProcessor() {
        return taskPostProcessor;
    }

    public CmmnEngineConfiguration setTaskPostProcessor(TaskPostProcessor processor) {
        this.taskPostProcessor = processor;
        return this;
    }

    @Override
    public ScriptingEngines getScriptingEngines() {
        return scriptingEngines;
    }

    @Override
    public CmmnEngineConfiguration setScriptingEngines(ScriptingEngines scriptingEngines) {
        this.scriptingEngines = scriptingEngines;
        return this;
    }
    
    public void resetClock() {
        if (this.clock != null) {
            clock.reset();
        }
    }

    public boolean isEnableHistoricTaskLogging() {
        return enableHistoricTaskLogging;
    }

    public void setEnableHistoricTaskLogging(boolean enableHistoricTaskLogging) {
        this.enableHistoricTaskLogging = enableHistoricTaskLogging;
    }

    public boolean isEnableHistoryCleaning() {
        return enableHistoryCleaning;
    }

    public CmmnEngineConfiguration setEnableHistoryCleaning(boolean enableHistoryCleaning) {
        this.enableHistoryCleaning = enableHistoryCleaning;
        return this;
    }

    public String getHistoryCleaningTimeCycleConfig() {
        return historyCleaningTimeCycleConfig;
    }

    public CmmnEngineConfiguration setHistoryCleaningTimeCycleConfig(String historyCleaningTimeCycleConfig) {
        this.historyCleaningTimeCycleConfig = historyCleaningTimeCycleConfig;
        return this;
    }

    public int getCleanInstancesEndedAfterNumberOfDays() {
        return cleanInstancesEndedAfterNumberOfDays;
    }

    public CmmnEngineConfiguration setCleanInstancesEndedAfterNumberOfDays(int cleanInstancesEndedAfterNumberOfDays) {
        this.cleanInstancesEndedAfterNumberOfDays = cleanInstancesEndedAfterNumberOfDays;
        return this;
    }

    public CmmnHistoryCleaningManager getCmmnHistoryCleaningManager() {
        return cmmnHistoryCleaningManager;
    }

    public CmmnEngineConfiguration setCmmnHistoryCleaningManager(CmmnHistoryCleaningManager cmmnHistoryCleaningManager) {
        this.cmmnHistoryCleaningManager = cmmnHistoryCleaningManager;
        return this;
    }

    public boolean isHandleCmmnEngineExecutorsAfterEngineCreate() {
        return handleCmmnEngineExecutorsAfterEngineCreate;
    }

    public CmmnEngineConfiguration setHandleCmmnEngineExecutorsAfterEngineCreate(boolean handleCmmnEngineExecutorsAfterEngineCreate) {
        this.handleCmmnEngineExecutorsAfterEngineCreate = handleCmmnEngineExecutorsAfterEngineCreate;
        return this;
    }

    public boolean isAlwaysUseArraysForDmnMultiHitPolicies() {
        return alwaysUseArraysForDmnMultiHitPolicies;
    }

    public CmmnEngineConfiguration setAlwaysUseArraysForDmnMultiHitPolicies(boolean alwaysUseArraysForDmnMultiHitPolicies) {
        this.alwaysUseArraysForDmnMultiHitPolicies = alwaysUseArraysForDmnMultiHitPolicies;
        return this;
    }
}
