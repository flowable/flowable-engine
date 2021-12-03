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

package org.flowable.engine.impl.cfg;

import java.io.InputStream;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadFactory;

import javax.xml.namespace.QName;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.flowable.batch.service.BatchServiceConfiguration;
import org.flowable.batch.service.impl.db.BatchDbSchemaManager;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.api.scope.ScopeTypes;
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
import org.flowable.common.engine.impl.cfg.IdGenerator;
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
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.interceptor.SessionFactory;
import org.flowable.common.engine.impl.javax.el.ELResolver;
import org.flowable.common.engine.impl.logging.LoggingSession;
import org.flowable.common.engine.impl.logging.LoggingSessionFactory;
import org.flowable.common.engine.impl.persistence.GenericManagerFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.cache.EntityCacheImpl;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.common.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.common.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.common.engine.impl.persistence.entity.TableDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.ByteArrayDataManager;
import org.flowable.common.engine.impl.persistence.entity.data.PropertyDataManager;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.scripting.BeansResolverFactory;
import org.flowable.common.engine.impl.scripting.ResolverFactory;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.common.engine.impl.tenant.ChangeTenantIdManager;
import org.flowable.common.engine.impl.tenant.MyBatisChangeTenantIdManager;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSession;
import org.flowable.common.engine.impl.variablelistener.VariableListenerSessionFactory;
import org.flowable.engine.BpmnChangeTenantIdEntityTypes;
import org.flowable.engine.CandidateManager;
import org.flowable.engine.DecisionTableVariableManager;
import org.flowable.engine.DefaultCandidateManager;
import org.flowable.engine.DefaultHistoryCleaningManager;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.FlowableEngineAgendaFactory;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.InternalProcessLocalizationManager;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.ProcessMigrationService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.app.AppResourceConverter;
import org.flowable.engine.compatibility.DefaultFlowable5CompatibilityHandlerFactory;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandlerFactory;
import org.flowable.engine.delegate.event.impl.BpmnModelEventDispatchAction;
import org.flowable.engine.delegate.variable.VariableAggregator;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.form.AbstractFormType;
import org.flowable.engine.impl.DefaultProcessJobParentStateResolver;
import org.flowable.engine.impl.DefaultProcessLocalizationManager;
import org.flowable.engine.impl.DynamicBpmnServiceImpl;
import org.flowable.engine.impl.FormServiceImpl;
import org.flowable.engine.impl.HistoryServiceImpl;
import org.flowable.engine.impl.IdentityServiceImpl;
import org.flowable.engine.impl.ManagementServiceImpl;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.ProcessMigrationServiceImpl;
import org.flowable.engine.impl.RepositoryServiceImpl;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.impl.SchemaOperationProcessEngineClose;
import org.flowable.engine.impl.SchemaOperationsProcessEngineBuild;
import org.flowable.engine.impl.TaskServiceImpl;
import org.flowable.engine.impl.agenda.AgendaSessionFactory;
import org.flowable.engine.impl.agenda.DefaultFlowableEngineAgendaFactory;
import org.flowable.engine.impl.app.AppDeployer;
import org.flowable.engine.impl.app.AppResourceConverterImpl;
import org.flowable.engine.impl.bpmn.deployer.BpmnDeployer;
import org.flowable.engine.impl.bpmn.deployer.BpmnDeploymentHelper;
import org.flowable.engine.impl.bpmn.deployer.CachingAndArtifactsManager;
import org.flowable.engine.impl.bpmn.deployer.EventSubscriptionManager;
import org.flowable.engine.impl.bpmn.deployer.ParsedDeploymentBuilderFactory;
import org.flowable.engine.impl.bpmn.deployer.ProcessDefinitionDiagramHelper;
import org.flowable.engine.impl.bpmn.deployer.TimerManager;
import org.flowable.engine.impl.bpmn.listener.ListenerNotificationHelper;
import org.flowable.engine.impl.bpmn.parser.BpmnParseHandlers;
import org.flowable.engine.impl.bpmn.parser.BpmnParser;
import org.flowable.engine.impl.bpmn.parser.factory.AbstractBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.ActivityBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultActivityBehaviorFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultListenerFactory;
import org.flowable.engine.impl.bpmn.parser.factory.DefaultXMLImporterFactory;
import org.flowable.engine.impl.bpmn.parser.factory.ListenerFactory;
import org.flowable.engine.impl.bpmn.parser.factory.XMLImporterFactory;
import org.flowable.engine.impl.bpmn.parser.handler.AdhocSubProcessParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.BoundaryEventParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.BusinessRuleParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.CallActivityParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.CancelEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.CaseServiceTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.CompensateEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ConditionalEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EndEventParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ErrorEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EscalationEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EventBasedGatewayParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EventSubProcessParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ExclusiveGatewayParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ExternalWorkerServiceTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.HttpServiceTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.InclusiveGatewayParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.IntermediateCatchEventParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.IntermediateThrowEventParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ManualTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.MessageEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ParallelGatewayParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ProcessParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ReceiveTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ScriptTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.SendEventServiceTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.SendTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.SequenceFlowParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ServiceTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.SignalEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.StartEventParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.SubProcessParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.TaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.TimerEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.TransactionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.UserTaskParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.VariableListenerEventDefinitionParseHandler;
import org.flowable.engine.impl.cmd.ClearProcessInstanceLockTimesCmd;
import org.flowable.engine.impl.cmd.RedeployV5ProcessDefinitionsCmd;
import org.flowable.engine.impl.cmd.ValidateExecutionRelatedEntityCountCfgCmd;
import org.flowable.engine.impl.cmd.ValidateTaskRelatedEntityCountCfgCmd;
import org.flowable.engine.impl.cmd.ValidateV5EntitiesCmd;
import org.flowable.engine.impl.cmmn.CaseInstanceService;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.impl.db.EntityDependencyOrder;
import org.flowable.engine.impl.db.ProcessDbSchemaManager;
import org.flowable.engine.impl.delegate.JsonVariableAggregator;
import org.flowable.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
import org.flowable.engine.impl.delete.ComputeDeleteHistoricProcessInstanceIdsJobHandler;
import org.flowable.engine.impl.delete.ComputeDeleteHistoricProcessInstanceStatusJobHandler;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstanceIdsJobHandler;
import org.flowable.engine.impl.delete.DeleteHistoricProcessInstanceIdsStatusJobHandler;
import org.flowable.engine.impl.dynamic.DefaultDynamicStateManager;
import org.flowable.engine.impl.el.FlowableDateFunctionDelegate;
import org.flowable.engine.impl.el.ProcessExpressionManager;
import org.flowable.engine.impl.event.CompensationEventHandler;
import org.flowable.engine.impl.event.EventHandler;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.event.SignalEventHandler;
import org.flowable.engine.impl.event.logger.EventLogger;
import org.flowable.engine.impl.eventregistry.BpmnEventRegistryEventConsumer;
import org.flowable.engine.impl.form.BooleanFormType;
import org.flowable.engine.impl.form.DateFormType;
import org.flowable.engine.impl.form.DoubleFormType;
import org.flowable.engine.impl.form.FormEngine;
import org.flowable.engine.impl.form.FormHandlerHelper;
import org.flowable.engine.impl.form.FormTypes;
import org.flowable.engine.impl.form.JuelFormEngine;
import org.flowable.engine.impl.form.LongFormType;
import org.flowable.engine.impl.form.StringFormType;
import org.flowable.engine.impl.formhandler.DefaultFormFieldHandler;
import org.flowable.engine.impl.function.TaskGetFunctionDelegate;
import org.flowable.engine.impl.history.DefaultHistoryManager;
import org.flowable.engine.impl.history.DefaultHistoryTaskManager;
import org.flowable.engine.impl.history.DefaultHistoryVariableManager;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.history.async.AsyncHistoryManager;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.history.async.json.transformer.ActivityEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityFullHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityUpdateHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.EntityLinkCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.EntityLinkDeletedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.FormPropertiesSubmittedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.HistoricDetailVariableUpdateHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.HistoricUserTaskLogDeleteJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.HistoricUserTaskLogRecordJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.IdentityLinkCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.IdentityLinkDeletedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceDeleteHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstancePropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ProcessInstanceStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.SetProcessDefinitionHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.SubProcessInstanceStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskAssigneeChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskEndedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskOwnerChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.TaskPropertyChangedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.UpdateProcessDefinitionCascadeHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableCreatedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableRemovedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.VariableUpdatedHistoryJsonTransformer;
import org.flowable.engine.impl.interceptor.BpmnOverrideContextInterceptor;
import org.flowable.engine.impl.interceptor.CommandInvoker;
import org.flowable.engine.impl.interceptor.DefaultIdentityLinkInterceptor;
import org.flowable.engine.impl.interceptor.DelegateInterceptor;
import org.flowable.engine.impl.interceptor.LoggingExecutionTreeCommandInvoker;
import org.flowable.engine.impl.jobexecutor.AsyncCompleteCallActivityJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncSendEventJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncTriggerJobHandler;
import org.flowable.engine.impl.jobexecutor.BpmnHistoryCleanupJobHandler;
import org.flowable.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.flowable.engine.impl.jobexecutor.ExternalWorkerTaskCompleteJobHandler;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceActivityCompletionJobHandler;
import org.flowable.engine.impl.jobexecutor.ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler;
import org.flowable.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationJobHandler;
import org.flowable.engine.impl.jobexecutor.ProcessInstanceMigrationStatusJobHandler;
import org.flowable.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.migration.ProcessInstanceMigrationManagerImpl;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionInfoCache;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionInfoCacheObject;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.ActivityInstanceEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.persistence.entity.CommentEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityImpl;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManager;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricActivityInstanceEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricDetailEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManager;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ModelEntityManager;
import org.flowable.engine.impl.persistence.entity.ModelEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManager;
import org.flowable.engine.impl.persistence.entity.ProcessDefinitionInfoEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.data.ActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.AttachmentDataManager;
import org.flowable.engine.impl.persistence.entity.data.CommentDataManager;
import org.flowable.engine.impl.persistence.entity.data.DeploymentDataManager;
import org.flowable.engine.impl.persistence.entity.data.EventLogEntryDataManager;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricDetailDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricProcessInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.ModelDataManager;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionDataManager;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionInfoDataManager;
import org.flowable.engine.impl.persistence.entity.data.ResourceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisAttachmentDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisCommentDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisDeploymentDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisEventLogEntryDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisExecutionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisHistoricActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisHistoricDetailDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisHistoricProcessInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisModelDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisProcessDefinitionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisProcessDefinitionInfoDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.engine.impl.repository.DefaultProcessDefinitionLocalizationManager;
import org.flowable.engine.impl.scripting.VariableScopeResolverFactory;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.impl.variable.BpmnAggregatedVariableType;
import org.flowable.engine.impl.variable.ParallelMultiInstanceLoopVariableType;
import org.flowable.engine.interceptor.CreateExternalWorkerJobInterceptor;
import org.flowable.engine.interceptor.CreateUserTaskInterceptor;
import org.flowable.engine.interceptor.ExecutionQueryInterceptor;
import org.flowable.engine.interceptor.HistoricProcessInstanceQueryInterceptor;
import org.flowable.engine.interceptor.IdentityLinkInterceptor;
import org.flowable.engine.interceptor.ProcessInstanceQueryInterceptor;
import org.flowable.engine.interceptor.StartProcessInstanceInterceptor;
import org.flowable.engine.migration.ProcessInstanceMigrationManager;
import org.flowable.engine.migration.ProcessInstanceMigrationCallback;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.engine.repository.InternalProcessDefinitionLocalizationManager;
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
import org.flowable.idm.engine.configurator.IdmEngineConfigurator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.job.service.HistoryJobHandler;
import org.flowable.job.service.HistoryJobProcessor;
import org.flowable.job.service.InternalJobCompatibilityManager;
import org.flowable.job.service.InternalJobManager;
import org.flowable.job.service.InternalJobParentStateResolver;
import org.flowable.job.service.JobHandler;
import org.flowable.job.service.JobProcessor;
import org.flowable.job.service.JobServiceConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncJobExecutorConfiguration;
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
import org.flowable.task.api.TaskQueryInterceptor;
import org.flowable.task.api.history.HistoricTaskQueryInterceptor;
import org.flowable.task.service.InternalTaskAssignmentManager;
import org.flowable.task.service.InternalTaskLocalizationManager;
import org.flowable.task.service.InternalTaskVariableScopeResolver;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.DefaultTaskPostProcessor;
import org.flowable.task.service.impl.db.TaskDbSchemaManager;
import org.flowable.task.service.impl.persistence.entity.HistoricTaskLogEntryEntityImpl;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.validation.validator.impl.ServiceTaskValidator;
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
import org.flowable.variable.service.impl.types.EntityManagerSession;
import org.flowable.variable.service.impl.types.EntityManagerSessionFactory;
import org.flowable.variable.service.impl.types.InstantType;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JPAEntityListVariableType;
import org.flowable.variable.service.impl.types.JPAEntityVariableType;
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

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class ProcessEngineConfigurationImpl extends ProcessEngineConfiguration implements
        ScriptingEngineAwareEngineConfiguration, HasExpressionManagerEngineConfiguration, HasVariableTypes, 
        HasVariableServiceConfiguration {

    public static final String DEFAULT_WS_SYNC_FACTORY = "org.flowable.engine.impl.webservice.CxfWebServiceClientFactory";

    public static final String DEFAULT_WS_IMPORTER = "org.flowable.engine.impl.webservice.CxfWSDLImporter";

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/db/mapping/mappings.xml";

    // SERVICES /////////////////////////////////////////////////////////////////

    protected RepositoryService repositoryService = new RepositoryServiceImpl();
    protected RuntimeService runtimeService = new RuntimeServiceImpl(this);
    protected HistoryService historyService = new HistoryServiceImpl(this);
    protected IdentityService identityService = new IdentityServiceImpl(this);
    protected TaskService taskService = new TaskServiceImpl(this);
    protected FormService formService = new FormServiceImpl();
    protected ManagementService managementService = new ManagementServiceImpl(this);
    protected DynamicBpmnService dynamicBpmnService = new DynamicBpmnServiceImpl(this);
    protected ProcessMigrationService processInstanceMigrationService = new ProcessMigrationServiceImpl(this);

    // IDM ENGINE /////////////////////////////////////////////////////
    protected boolean disableIdmEngine;
    
    // EVENT REGISTRY /////////////////////////////////////////////////////
    protected boolean disableEventRegistry;

    // DATA MANAGERS /////////////////////////////////////////////////////////////

    protected AttachmentDataManager attachmentDataManager;
    protected CommentDataManager commentDataManager;
    protected DeploymentDataManager deploymentDataManager;
    protected EventLogEntryDataManager eventLogEntryDataManager;
    protected ExecutionDataManager executionDataManager;
    protected ActivityInstanceDataManager activityInstanceDataManager;
    protected HistoricActivityInstanceDataManager historicActivityInstanceDataManager;
    protected HistoricDetailDataManager historicDetailDataManager;
    protected HistoricProcessInstanceDataManager historicProcessInstanceDataManager;
    protected ModelDataManager modelDataManager;
    protected ProcessDefinitionDataManager processDefinitionDataManager;
    protected ProcessDefinitionInfoDataManager processDefinitionInfoDataManager;
    protected ResourceDataManager resourceDataManager;

    // ENTITY MANAGERS ///////////////////////////////////////////////////////////

    protected AttachmentEntityManager attachmentEntityManager;
    protected CommentEntityManager commentEntityManager;
    protected DeploymentEntityManager deploymentEntityManager;
    protected EventLogEntryEntityManager eventLogEntryEntityManager;
    protected ExecutionEntityManager executionEntityManager;
    protected ActivityInstanceEntityManager activityInstanceEntityManager;
    protected HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager;
    protected HistoricDetailEntityManager historicDetailEntityManager;
    protected HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager;
    protected ModelEntityManager modelEntityManager;
    protected ProcessDefinitionEntityManager processDefinitionEntityManager;
    protected ProcessDefinitionInfoEntityManager processDefinitionInfoEntityManager;
    protected ResourceEntityManager resourceEntityManager;

    // Candidate Manager

    protected CandidateManager candidateManager;

    // History Manager

    protected HistoryManager historyManager;

    protected boolean isAsyncHistoryEnabled;
    protected boolean isAsyncHistoryJsonGzipCompressionEnabled;
    protected boolean isAsyncHistoryJsonGroupingEnabled;
    protected int asyncHistoryJsonGroupingThreshold = 10;
    protected AsyncHistoryListener asyncHistoryListener;

    // Change Tenant ID Manager

    protected ChangeTenantIdManager changeTenantIdManager;
    protected Set<String> changeTenantEntityTypes;

    // Job Manager

    protected JobManager jobManager;

    // Dynamic state manager

    protected DynamicStateManager dynamicStateManager;

    protected ProcessInstanceMigrationManager processInstanceMigrationManager;
    
    // Decision table variable manager
    
    protected DecisionTableVariableManager decisionTableVariableManager;

    protected VariableServiceConfiguration variableServiceConfiguration;
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;
    protected EntityLinkServiceConfiguration entityLinkServiceConfiguration;
    protected EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration;
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected JobServiceConfiguration jobServiceConfiguration;
    protected BatchServiceConfiguration batchServiceConfiguration;

    protected boolean enableEntityLinks;

    // Variable Aggregation

    protected VariableAggregator variableAggregator;

    // DEPLOYERS //////////////////////////////////////////////////////////////////

    protected BpmnDeployer bpmnDeployer;
    protected AppDeployer appDeployer;
    protected BpmnParser bpmnParser;
    protected ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory;
    protected TimerManager timerManager;
    protected EventSubscriptionManager eventSubscriptionManager;
    protected BpmnDeploymentHelper bpmnDeploymentHelper;
    protected CachingAndArtifactsManager cachingAndArtifactsManager;
    protected ProcessDefinitionDiagramHelper processDefinitionDiagramHelper;
    protected DeploymentManager deploymentManager;

    protected int processDefinitionCacheLimit = -1; // By default, no limit
    protected DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache;

    protected int processDefinitionInfoCacheLimit = -1; // By default, no limit
    protected DeploymentCache<ProcessDefinitionInfoCacheObject> processDefinitionInfoCache;

    protected int knowledgeBaseCacheLimit = -1;
    protected DeploymentCache<Object> knowledgeBaseCache;

    protected int appResourceCacheLimit = -1;
    protected DeploymentCache<Object> appResourceCache;

    protected AppResourceConverter appResourceConverter;

    // JOB EXECUTOR /////////////////////////////////////////////////////////////

    protected List<JobHandler> customJobHandlers;
    protected Map<String, JobHandler> jobHandlers;
    protected List<AsyncRunnableExecutionExceptionHandler> customAsyncRunnableExecutionExceptionHandlers;
    protected boolean addDefaultExceptionHandler = true;

    protected Map<String, HistoryJobHandler> historyJobHandlers;
    protected List<HistoryJobHandler> customHistoryJobHandlers;
    protected List<HistoryJsonTransformer> customHistoryJsonTransformers;

    // HELPERS //////////////////////////////////////////////////////////////////
    protected ProcessInstanceHelper processInstanceHelper;
    protected ListenerNotificationHelper listenerNotificationHelper;
    protected FormHandlerHelper formHandlerHelper;
    
    protected CaseInstanceService caseInstanceService;

    // ASYNC EXECUTOR ///////////////////////////////////////////////////////////

    /**
     * The number of retries for a job.
     */
    protected int asyncExecutorNumberOfRetries = 3;

    /**
     * The minimal number of threads that are kept alive in the threadpool for job execution. Default value = 2. (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorCorePoolSize = 8;

    /**
     * The maximum number of threads that are created in the threadpool for job execution. Default value = 10. (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorMaxPoolSize = 8;

    /**
     * The time (in milliseconds) a thread used for job execution must be kept alive before it is destroyed. Default setting is 5 seconds. Having a setting &gt; 0 takes resources, but in the case of many
     * job executions it avoids creating new threads all the time. If 0, threads will be destroyed after they've been used for job execution.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected long asyncExecutorThreadKeepAliveTime = 5000L;

    /**
     * The size of the queue on which jobs to be executed are placed, before they are actually executed.
     * Default value = 2048. (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorThreadPoolQueueSize = 2048;

    /**
     * The queue onto which jobs will be placed before they are actually executed. Threads form the async executor threadpool will take work from this queue.
     * <p>
     * By default null. If null, an {@link ArrayBlockingQueue} will be created of size {@link #asyncExecutorThreadPoolQueueSize}.
     * <p>
     * When the queue is full, the job will be executed by the calling thread (ThreadPoolExecutor.CallerRunsPolicy())
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected BlockingQueue<Runnable> asyncExecutorThreadPoolQueue;

    /**
     * The time (in seconds) that is waited to gracefully shut down the threadpool used for job execution when the a shutdown on the executor (or process engine) is requested. Default value = 60.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
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

    /**
     * The amount of time (in milliseconds) a job can maximum be in the 'executable' state before being deemed expired.
     * Note that this won't happen when using the threadpool based executor, as the acquire thread will fetch these kind of jobs earlier.
     * However, in the message queue based execution, it could be some job is posted to a queue but then never is locked nor executed.
     * <p>
     * By default 24 hours, as this should be a very exceptional case.
     */
    protected int asyncExecutorResetExpiredJobsMaxTimeout = 24 * 60 * 60 * 1000;

    /**
     * Experimental!
     * <p>
     * Set this to true when using the message queue based job executor.
     */
    protected boolean asyncExecutorMessageQueueMode;

    protected AsyncJobExecutorConfiguration asyncExecutorConfiguration = new AsyncJobExecutorConfiguration();
    protected AsyncJobExecutorConfiguration asyncHistoryExecutorConfiguration;

    // More info: see similar async executor properties.
    protected boolean asyncHistoryExecutorMessageQueueMode;
    protected int asyncHistoryExecutorNumberOfRetries = 10;
    protected int asyncHistoryExecutorCorePoolSize = 8;
    protected int asyncHistoryExecutorMaxPoolSize = 8;
    protected long asyncHistoryExecutorThreadKeepAliveTime = 5000L;
    protected int asyncHistoryExecutorThreadPoolQueueSize = 2048;
    protected BlockingQueue<Runnable> asyncHistoryExecutorThreadPoolQueue;
    protected long asyncHistoryExecutorSecondsToWaitOnShutdown = 60L;

    protected List<String> enabledJobCategories;
    protected String jobExecutionScope;
    protected String historyJobExecutionScope;

    protected String batchStatusTimeCycleConfig = "30 * * * * ?";

    /**
     * Allows to define a custom factory for creating the {@link Runnable} that is executed by the async executor.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory;
    protected InternalJobParentStateResolver internalJobParentStateResolver;

    // JUEL functions ///////////////////////////////////////////////////////////
    protected List<FlowableFunctionDelegate> flowableFunctionDelegates;
    protected List<FlowableFunctionDelegate> customFlowableFunctionDelegates;
    protected List<FlowableAstFunctionCreator> astFunctionCreators;

    // BPMN PARSER //////////////////////////////////////////////////////////////

    protected List<BpmnParseHandler> preBpmnParseHandlers;
    protected List<BpmnParseHandler> postBpmnParseHandlers;
    protected List<BpmnParseHandler> customDefaultBpmnParseHandlers;
    protected ActivityBehaviorFactory activityBehaviorFactory;
    protected ListenerFactory listenerFactory;
    protected BpmnParseFactory bpmnParseFactory;

    // PROCESS VALIDATION ///////////////////////////////////////////////////////

    protected ProcessValidator processValidator;
    protected ServiceTaskValidator customServiceTaskValidator;

    // OTHER ////////////////////////////////////////////////////////////////////

    protected List<FormEngine> customFormEngines;
    protected Map<String, FormEngine> formEngines;

    protected List<AbstractFormType> customFormTypes;
    protected FormTypes formTypes;

    protected List<VariableType> customPreVariableTypes;
    protected List<VariableType> customPostVariableTypes;
    protected VariableTypes variableTypes;

    protected InternalHistoryVariableManager internalHistoryVariableManager;
    protected InternalTaskVariableScopeResolver internalTaskVariableScopeResolver;
    protected InternalHistoryTaskManager internalHistoryTaskManager;
    protected InternalTaskAssignmentManager internalTaskAssignmentManager;
    protected IdentityLinkEventHandler identityLinkEventHandler;
    protected InternalTaskLocalizationManager internalTaskLocalizationManager;
    protected InternalProcessLocalizationManager internalProcessLocalizationManager;
    protected InternalProcessDefinitionLocalizationManager internalProcessDefinitionLocalizationManager;
    protected InternalJobManager internalJobManager;
    protected InternalJobCompatibilityManager internalJobCompatibilityManager;

    protected Map<String, List<RuntimeInstanceStateChangeCallback>> processInstanceStateChangedCallbacks;

    protected List<ProcessInstanceMigrationCallback> processInstanceMigrationCallbacks;
    
    /**
     * This flag determines whether variables of the type 'serializable' will be tracked. This means that, when true, in a JavaDelegate you can write
     * <p>
     * MySerializableVariable myVariable = (MySerializableVariable) execution.getVariable("myVariable"); myVariable.setNumber(123);
     * <p>
     * And the changes to the java object will be reflected in the database. Otherwise, a manual call to setVariable will be needed.
     * <p>
     * By default true for backwards compatibility.
     */
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

    /**
     * Whether the Parallel Multi instance should perform the leave operation through an async exclusive job.
     * When this is true then non exclusive parallel multi instances can run in non exclusive asynchronously without an exception being thrown.
     */
    protected boolean parallelMultiInstanceAsyncLeave = true;

    protected ExpressionManager expressionManager;
    protected Collection<ELResolver> preDefaultELResolvers;
    protected Collection<ELResolver> preBeanELResolvers;
    protected Collection<ELResolver> postDefaultELResolvers;
    protected List<String> customScriptingEngineClasses;
    protected ScriptingEngines scriptingEngines;
    protected List<ResolverFactory> resolverFactories;

    protected boolean isExpressionCacheEnabled = true;
    protected int expressionCacheSize = 4096;
    protected int expressionTextLengthCacheLimit = -1; // negative value to have no max length

    protected BusinessCalendarManager businessCalendarManager;

    protected StartProcessInstanceInterceptor startProcessInstanceInterceptor;
    protected CreateUserTaskInterceptor createUserTaskInterceptor;
    protected CreateExternalWorkerJobInterceptor createExternalWorkerJobInterceptor;
    protected IdentityLinkInterceptor identityLinkInterceptor;
    protected ProcessInstanceQueryInterceptor processInstanceQueryInterceptor;
    protected ExecutionQueryInterceptor executionQueryInterceptor;
    protected HistoricProcessInstanceQueryInterceptor historicProcessInstanceQueryInterceptor;
    protected TaskQueryInterceptor taskQueryInterceptor;
    protected HistoricTaskQueryInterceptor historicTaskQueryInterceptor;

    protected String wsSyncFactoryClassName = DEFAULT_WS_SYNC_FACTORY;
    protected XMLImporterFactory wsWsdlImporterFactory;
    protected ConcurrentMap<QName, URL> wsOverridenEndpointAddresses = new ConcurrentHashMap<>();

    protected DelegateInterceptor delegateInterceptor;

    protected Map<String, EventHandler> eventHandlers;
    protected List<EventHandler> customEventHandlers;

    protected FailedJobCommandFactory failedJobCommandFactory;

    protected FormFieldHandler formFieldHandler;
    protected boolean isFormFieldValidationEnabled;
    
    protected EventRegistryEventConsumer eventRegistryEventConsumer;
    /**
     * Whether process instances should be start asynchronously by the default {@link EventRegistryEventConsumer}.
     * This is a fallback applied for all events. We suggest modelling your processes appropriately, i.e. making the start event async.
     */
    protected boolean eventRegistryStartProcessInstanceAsync = false;

    /**
     * Set this to true if you want to have extra checks on the BPMN xml that is parsed. See http://www.jorambarrez.be/blog/2013/02/19/uploading-a-funny-xml -can-bring-down-your-server/
     * <p>
     * Unfortunately, this feature is not available on some platforms (JDK 6, JBoss), hence the reason why it is disabled by default. If your platform allows the use of StaxSource during XML parsing,
     * do enable it.
     */
    protected boolean enableSafeBpmnXml;

    /**
     * The following settings will determine the amount of entities loaded at once when the engine needs to load multiple entities (eg. when suspending a process definition with all its process
     * instances).
     * <p>
     * The default setting is quite low, as not to surprise anyone with sudden memory spikes. Change it to something higher if the environment Flowable runs in allows it.
     */
    protected int batchSizeProcessInstances = 25;
    protected int batchSizeTasks = 25;

    // Event logging to database
    protected boolean enableDatabaseEventLogging;
    protected boolean enableHistoricTaskLogging;

    /**
     * Using field injection together with a delegate expression for a service task / execution listener / task listener is not thread-sade , see user guide section 'Field Injection' for more
     * information.
     * <p>
     * Set this flag to false to throw an exception at runtime when a field is injected and a delegateExpression is used.
     *
     * @since 5.21
     */
    protected DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode = DelegateExpressionFieldInjectionMode.MIXED;

    protected List<Object> flowable5JobProcessors = Collections.emptyList();
    protected List<JobProcessor> jobProcessors = Collections.emptyList();
    protected List<HistoryJobProcessor> historyJobProcessors = Collections.emptyList();

    /**
     * Enabled a very verbose debug output of the execution tree whilst executing operations. Most useful for core engine developers or people fiddling around with the execution tree.
     */
    protected boolean enableVerboseExecutionTreeLogging;

    protected PerformanceSettings performanceSettings = new PerformanceSettings();

    // agenda factory
    protected FlowableEngineAgendaFactory agendaFactory;

    protected SchemaManager identityLinkSchemaManager;
    protected SchemaManager entityLinkSchemaManager;
    protected SchemaManager eventSubscriptionSchemaManager;
    protected SchemaManager variableSchemaManager;
    protected SchemaManager taskSchemaManager;
    protected SchemaManager jobSchemaManager;
    protected SchemaManager batchSchemaManager;

    protected boolean handleProcessEngineExecutorsAfterEngineCreate = true;

    // Backwards compatibility //////////////////////////////////////////////////////////////

    protected boolean flowable5CompatibilityEnabled; // Default flowable 5 backwards compatibility is disabled!
    protected boolean validateFlowable5EntitiesEnabled = true; // When disabled no checks are performed for existing flowable 5 entities in the db
    protected boolean redeployFlowable5ProcessDefinitions;
    protected Flowable5CompatibilityHandlerFactory flowable5CompatibilityHandlerFactory;
    protected Flowable5CompatibilityHandler flowable5CompatibilityHandler;

    // Can't have a dependency on the Flowable5-engine module
    protected Object flowable5ActivityBehaviorFactory;
    protected Object flowable5ListenerFactory;
    protected List<Object> flowable5PreBpmnParseHandlers;
    protected List<Object> flowable5PostBpmnParseHandlers;
    protected List<Object> flowable5CustomDefaultBpmnParseHandlers;
    protected Set<Class<?>> flowable5CustomMybatisMappers;
    protected Set<String> flowable5CustomMybatisXMLMappers;
    protected Object flowable5ExpressionManager;

    public ProcessEngineConfigurationImpl() {
        mybatisMappingFile = DEFAULT_MYBATIS_MAPPING_FILE;
    }

    // buildProcessEngine
    // ///////////////////////////////////////////////////////

    @Override
    public ProcessEngine buildProcessEngine() {
        init();
        ProcessEngineImpl processEngine = new ProcessEngineImpl(this);

        if (handleProcessEngineExecutorsAfterEngineCreate) {
            processEngine.startExecutors();
        }

        // trigger build of Flowable 5 Engine
        if (flowable5CompatibilityEnabled && flowable5CompatibilityHandler != null) {
            commandExecutor.execute(new Command<Void>() {

                @Override
                public Void execute(CommandContext commandContext) {
                    flowable5CompatibilityHandler.getRawProcessEngine();
                    return null;
                }
            });
        }

        postProcessEngineInitialisation();

        return processEngine;
    }

    // init
    // /////////////////////////////////////////////////////////////////////

    public void init() {
        initEngineConfigurations();
        initConfigurators();
        configuratorsBeforeInit();
        initClock();
        initObjectMapper();
        initProcessDiagramGenerator();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initIdGenerator();
        initHistoryLevel();
        initFunctionDelegates();
        initAstFunctionCreators();
        initDelegateInterceptor();
        initBeans();
        initExpressionManager();
        initAgendaFactory();

        if (usingRelationalDatabase) {
            initDataSource();
        } else {
            initNonRelationalDataSource();
        }

        if (usingRelationalDatabase || usingSchemaMgmt) {
            initSchemaManager();
            initSchemaManagementCommand();
        }
        
        configureVariableServiceConfiguration();
        configureJobServiceConfiguration();

        initHelpers();
        initVariableTypes();
        initFormEngines();
        initFormTypes();
        initScriptingEngines();
        initBusinessCalendarManager();
        initServices();
        initWsdlImporterFactory();
        initBehaviorFactory();
        initListenerFactory();
        initBpmnParser();
        initProcessDefinitionCache();
        initProcessDefinitionInfoCache();
        initAppResourceCache();
        initKnowledgeBaseCache();
        initJobHandlers();
        initHistoryJobHandlers();

        initTransactionFactory();

        if (usingRelationalDatabase) {
            initSqlSessionFactory();
        }

        initSessionFactories();
        initDataManagers();
        initEntityManagers();
        initCandidateManager();
        initVariableAggregator();
        initHistoryManager();
        initChangeTenantIdManager();
        initDynamicStateManager();
        initProcessInstanceMigrationValidationManager();
        initIdentityLinkInterceptor();
        initJpa();
        initDeployers();
        initEventHandlers();
        initFailedJobCommandFactory();
        initEventDispatcher();
        initProcessValidator();
        initFormFieldHandler();
        initDatabaseEventLogging();
        initFlowable5CompatibilityHandler();
        initVariableServiceConfiguration();
        initIdentityLinkServiceConfiguration();
        initEntityLinkServiceConfiguration();
        initEventSubscriptionServiceConfiguration();
        initTaskServiceConfiguration();
        initJobServiceConfiguration();
        initBatchServiceConfiguration();
        initAsyncExecutor();
        initAsyncHistoryExecutor();

        configuratorsAfterInit();
        afterInitTaskServiceConfiguration();
        afterInitEventRegistryEventBusConsumer();
        
        initHistoryCleaningManager();
        initLocalizationManagers();
    }

    // failedJobCommandFactory
    // ////////////////////////////////////////////////////////

    public void initFailedJobCommandFactory() {
        if (failedJobCommandFactory == null) {
            failedJobCommandFactory = new DefaultFailedJobCommandFactory();
        }
    }

    // command executors
    // ////////////////////////////////////////////////////////

    @Override
    public void initCommandExecutors() {
        initDefaultCommandConfig();
        initSchemaCommandConfig();
        initCommandInvoker();
        initCommandInterceptors();
        initCommandExecutor();
    }

    @Override
    public void initCommandInvoker() {
        if (commandInvoker == null) {
            if (enableVerboseExecutionTreeLogging) {
                this.commandInvoker = new LoggingExecutionTreeCommandInvoker(agendaOperationRunner);

            } else {
                this.commandInvoker = new CommandInvoker(agendaOperationRunner);
            }
        }
    }

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG;
    }
    
    @Override
    public String getEngineScopeType() {
        return ScopeTypes.BPMN;
    }

    @Override
    public List<CommandInterceptor> getAdditionalDefaultCommandInterceptors() {
        return Collections.singletonList(new BpmnOverrideContextInterceptor());
    }
    // services
    // /////////////////////////////////////////////////////////////////

    public void initServices() {
        initService(repositoryService);
        initService(runtimeService);
        initService(historyService);
        initService(identityService);
        initService(taskService);
        initService(formService);
        initService(managementService);
        initService(dynamicBpmnService);
        initService(processInstanceMigrationService);
    }

    @Override
    public void initSchemaManager() {
        super.initSchemaManager();

        initProcessSchemaManager();
        initIdentityLinkSchemaManager();
        initEntityLinkSchemaManager();
        initEventSubscriptionSchemaManager();
        initVariableSchemaManager();
        initTaskSchemaManager();
        initJobSchemaManager();
        initBatchSchemaManager();
    }

    public void initNonRelationalDataSource() {
        // for subclassing
    }

    protected void initProcessSchemaManager() {
        if (this.schemaManager == null) {
            this.schemaManager = new ProcessDbSchemaManager();
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
    
    protected void initBatchSchemaManager() {
        if (this.batchSchemaManager == null) {
            this.batchSchemaManager = new BatchDbSchemaManager();
        }
    }

    public void initSchemaManagementCommand() {
        if (schemaManagementCmd == null) {
            if (usingRelationalDatabase && databaseSchemaUpdate != null) {
                this.schemaManagementCmd = new SchemaOperationsProcessEngineBuild();
            }
        }
    }

    @Override
    public void initMybatisTypeHandlers(Configuration configuration) {
        super.initMybatisTypeHandlers(configuration);
        configuration.getTypeHandlerRegistry().register(VariableType.class, JdbcType.VARCHAR, new IbatisVariableTypeHandler(variableTypes));
    }

    @Override
    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(mybatisMappingFile);
    }

    @Override
    public ProcessEngineConfigurationImpl setCustomMybatisMappers(Set<Class<?>> customMybatisMappers) {
        this.customMybatisMappers = customMybatisMappers;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setCustomMybatisXMLMappers(Set<String> customMybatisXMLMappers) {
        this.customMybatisXMLMappers = customMybatisXMLMappers;
        return this;
    }

    // Data managers ///////////////////////////////////////////////////////////

    @Override
    @SuppressWarnings("rawtypes")
    public void initDataManagers() {
        super.initDataManagers();
        if (attachmentDataManager == null) {
            attachmentDataManager = new MybatisAttachmentDataManager(this);
        }
        if (commentDataManager == null) {
            commentDataManager = new MybatisCommentDataManager(this);
        }
        if (deploymentDataManager == null) {
            deploymentDataManager = new MybatisDeploymentDataManager(this);
        }
        if (eventLogEntryDataManager == null) {
            eventLogEntryDataManager = new MybatisEventLogEntryDataManager(this);
        }
        if (executionDataManager == null) {
            executionDataManager = new MybatisExecutionDataManager(this);
        }
        if (dbSqlSessionFactory != null && executionDataManager instanceof AbstractDataManager) {
            dbSqlSessionFactory.addLogicalEntityClassMapping("execution", ((AbstractDataManager) executionDataManager).getManagedEntityClass());
        }
        if (historicActivityInstanceDataManager == null) {
            historicActivityInstanceDataManager = new MybatisHistoricActivityInstanceDataManager(this);
        }
        if (activityInstanceDataManager == null) {
            activityInstanceDataManager = new MybatisActivityInstanceDataManager(this);
        }
        if (historicDetailDataManager == null) {
            historicDetailDataManager = new MybatisHistoricDetailDataManager(this);
        }
        if (historicProcessInstanceDataManager == null) {
            historicProcessInstanceDataManager = new MybatisHistoricProcessInstanceDataManager(this);
        }
        if (modelDataManager == null) {
            modelDataManager = new MybatisModelDataManager(this);
        }
        if (processDefinitionDataManager == null) {
            processDefinitionDataManager = new MybatisProcessDefinitionDataManager(this);
        }
        if (processDefinitionInfoDataManager == null) {
            processDefinitionInfoDataManager = new MybatisProcessDefinitionInfoDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
    }

    // Entity managers //////////////////////////////////////////////////////////

    @Override
    public void initEntityManagers() {
        super.initEntityManagers();
        if (attachmentEntityManager == null) {
            attachmentEntityManager = new AttachmentEntityManagerImpl(this, attachmentDataManager);
        }
        if (commentEntityManager == null) {
            commentEntityManager = new CommentEntityManagerImpl(this, commentDataManager);
        }
        if (deploymentEntityManager == null) {
            deploymentEntityManager = new DeploymentEntityManagerImpl(this, deploymentDataManager);
        }
        if (eventLogEntryEntityManager == null) {
            eventLogEntryEntityManager = new EventLogEntryEntityManagerImpl(this, eventLogEntryDataManager);
        }
        if (executionEntityManager == null) {
            executionEntityManager = new ExecutionEntityManagerImpl(this, executionDataManager);
        }
        if (activityInstanceEntityManager == null) {
            activityInstanceEntityManager = new ActivityInstanceEntityManagerImpl(this, activityInstanceDataManager);
        }
        if (historicActivityInstanceEntityManager == null) {
            historicActivityInstanceEntityManager = new HistoricActivityInstanceEntityManagerImpl(this, historicActivityInstanceDataManager);
        }
        if (historicDetailEntityManager == null) {
            historicDetailEntityManager = new HistoricDetailEntityManagerImpl(this, historicDetailDataManager);
        }
        if (historicProcessInstanceEntityManager == null) {
            historicProcessInstanceEntityManager = new HistoricProcessInstanceEntityManagerImpl(this, historicProcessInstanceDataManager);
        }
        if (modelEntityManager == null) {
            modelEntityManager = new ModelEntityManagerImpl(this, modelDataManager);
        }
        if (processDefinitionEntityManager == null) {
            processDefinitionEntityManager = new ProcessDefinitionEntityManagerImpl(this, processDefinitionDataManager);
        }
        if (processDefinitionInfoEntityManager == null) {
            processDefinitionInfoEntityManager = new ProcessDefinitionInfoEntityManagerImpl(this, processDefinitionInfoDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new ResourceEntityManagerImpl(this, resourceDataManager);
        }
    }

    // CandidateManager //////////////////////////////

    public void initCandidateManager() {
        if (candidateManager == null) {
            candidateManager = new DefaultCandidateManager(this);
        }
    }

    // Variable Aggregator

    public void initVariableAggregator() {
        if (variableAggregator == null) {
            variableAggregator = new JsonVariableAggregator(this);
        }
    }

    // History manager ///////////////////////////////////////////////////////////

    public void initHistoryManager() {
        if (historyManager == null) {
            if (isAsyncHistoryEnabled) {
                historyManager = new AsyncHistoryManager(this);
            } else {
                historyManager = new DefaultHistoryManager(this);
            }
        }
    }

    // Change Tenant ID manager ////////////////////////////////////////////////////

    public void initChangeTenantIdManager() {
        if (changeTenantEntityTypes == null) {
            changeTenantEntityTypes = new LinkedHashSet<>();
        }
        changeTenantEntityTypes.addAll(BpmnChangeTenantIdEntityTypes.RUNTIME_TYPES);

        if (isDbHistoryUsed) {
            changeTenantEntityTypes.addAll(BpmnChangeTenantIdEntityTypes.HISTORIC_TYPES);
        }

        if (changeTenantIdManager == null) {
            changeTenantIdManager = new MyBatisChangeTenantIdManager(commandExecutor, ScopeTypes.BPMN, changeTenantEntityTypes);
        }
    }

    // Dynamic state manager ////////////////////////////////////////////////////

    public void initDynamicStateManager() {
        if (dynamicStateManager == null) {
            dynamicStateManager = new DefaultDynamicStateManager();
        }
    }

    public void initProcessInstanceMigrationValidationManager() {
        if (processInstanceMigrationManager == null) {
            processInstanceMigrationManager = new ProcessInstanceMigrationManagerImpl();
        }
    }
    
    // identity link interceptor ///////////////////////////////////////////////
    public void initIdentityLinkInterceptor() {
        if (identityLinkInterceptor == null) {
            identityLinkInterceptor = new DefaultIdentityLinkInterceptor();
        }
    }

    // session factories ////////////////////////////////////////////////////////

    @Override
    public void initSessionFactories() {
        if (sessionFactories == null) {
            sessionFactories = new HashMap<>();

            if (usingRelationalDatabase) {
                initDbSqlSessionFactory();
            }

            if (isAsyncHistoryEnabled) {
                initAsyncHistorySessionFactory();
            }

            if (agendaFactory != null) {
                addSessionFactory(new AgendaSessionFactory(agendaFactory));
            }

            addSessionFactory(new GenericManagerFactory(EntityCache.class, EntityCacheImpl.class));

            commandContextFactory.setSessionFactories(sessionFactories);

        } else {
            if (usingRelationalDatabase) {
                initDbSqlSessionFactoryEntitySettings();
            }
            
            if (isAsyncHistoryEnabled) {
                if (!sessionFactories.containsKey(AsyncHistorySession.class)) {
                    initAsyncHistorySessionFactory();
                }
            }

            if (!sessionFactories.containsKey(FlowableEngineAgenda.class)) {
                if (agendaFactory != null) {
                    addSessionFactory(new AgendaSessionFactory(agendaFactory));
                }
            }
        }
        
        if (isLoggingSessionEnabled()) {
            if (!sessionFactories.containsKey(LoggingSession.class)) {
                LoggingSessionFactory loggingSessionFactory = new LoggingSessionFactory();
                loggingSessionFactory.setLoggingListener(loggingListener);
                loggingSessionFactory.setObjectMapper(objectMapper);
                sessionFactories.put(LoggingSession.class, loggingSessionFactory);
            }
        }
        
        if (!sessionFactories.containsKey(VariableListenerSession.class)) {
            VariableListenerSessionFactory variableListenerSessionFactory = new VariableListenerSessionFactory();
            sessionFactories.put(VariableListenerSession.class, variableListenerSessionFactory);
        }

        if (customSessionFactories != null) {
            for (SessionFactory sessionFactory : customSessionFactories) {
                addSessionFactory(sessionFactory);
            }
        }

    }

    @Override
    protected void initDbSqlSessionFactoryEntitySettings() {
        defaultInitDbSqlSessionFactoryEntitySettings(EntityDependencyOrder.INSERT_ORDER, EntityDependencyOrder.DELETE_ORDER);

        // Oracle doesn't support bulk inserting for event log entries and historic task log entries
        if (isBulkInsertEnabled && "oracle".equals(databaseType)) {
            dbSqlSessionFactory.getBulkInserteableEntityClasses().remove(EventLogEntryEntityImpl.class);
            dbSqlSessionFactory.getBulkInserteableEntityClasses().remove(HistoricTaskLogEntryEntityImpl.class);
        }
    }

    public void initAsyncHistorySessionFactory() {
        if (!sessionFactories.containsKey(AsyncHistorySession.class)) {
            AsyncHistorySessionFactory asyncHistorySessionFactory = new AsyncHistorySessionFactory();
            if (asyncHistoryListener == null) {
                initDefaultAsyncHistoryListener();
            }
            asyncHistorySessionFactory.setAsyncHistoryListener(asyncHistoryListener);
            sessionFactories.put(AsyncHistorySession.class, asyncHistorySessionFactory);
        }

        ((AsyncHistorySessionFactory) sessionFactories.get(AsyncHistorySession.class)).registerJobDataTypes(HistoryJsonConstants.ORDERED_TYPES);
    }

    protected void initDefaultAsyncHistoryListener() {
        asyncHistoryListener = new DefaultAsyncHistoryJobProducer();
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
            this.variableServiceConfiguration.setInternalHistoryVariableManager(new DefaultHistoryVariableManager(this));
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
        return new VariableServiceConfiguration(ScopeTypes.BPMN);
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
        return new IdentityLinkServiceConfiguration(ScopeTypes.BPMN);
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
        return new EntityLinkServiceConfiguration(ScopeTypes.BPMN);
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
        return new EventSubscriptionServiceConfiguration(ScopeTypes.BPMN);
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
            this.taskServiceConfiguration.setInternalHistoryTaskManager(new DefaultHistoryTaskManager(this));
        }

        if (this.internalTaskVariableScopeResolver != null) {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(this.internalTaskVariableScopeResolver);
        } else {
            this.taskServiceConfiguration.setInternalTaskVariableScopeResolver(new DefaultTaskVariableScopeResolver(this));
        }

        if (this.internalTaskAssignmentManager != null) {
            this.taskServiceConfiguration.setInternalTaskAssignmentManager(this.internalTaskAssignmentManager);
        } else {
            this.taskServiceConfiguration.setInternalTaskAssignmentManager(new DefaultTaskAssignmentManager());
        }

        if (this.internalTaskLocalizationManager != null) {
            this.taskServiceConfiguration.setInternalTaskLocalizationManager(this.internalTaskLocalizationManager);
        } else {
            this.taskServiceConfiguration.setInternalTaskLocalizationManager(new DefaultTaskLocalizationManager(this));
        }

        this.taskServiceConfiguration.setEnableTaskRelationshipCounts(this.performanceSettings.isEnableTaskRelationshipCounts());
        this.taskServiceConfiguration.setEnableLocalization(this.performanceSettings.isEnableLocalization());
        this.taskServiceConfiguration.setTaskQueryInterceptor(this.taskQueryInterceptor);
        this.taskServiceConfiguration.setHistoricTaskQueryInterceptor(this.historicTaskQueryInterceptor);

        this.taskServiceConfiguration.init();

        if (dbSqlSessionFactory != null && taskServiceConfiguration.getTaskDataManager() instanceof AbstractDataManager) {
            dbSqlSessionFactory.addLogicalEntityClassMapping("task", ((AbstractDataManager) taskServiceConfiguration.getTaskDataManager()).getManagedEntityClass());
        }

        addServiceConfiguration(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG, this.taskServiceConfiguration);
    }

    protected TaskServiceConfiguration instantiateTaskServiceConfiguration() {
        return new TaskServiceConfiguration(ScopeTypes.BPMN);
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

            if (addDefaultExceptionHandler) {
                exceptionHandlers.add(new DefaultAsyncRunnableExecutionExceptionHandler());
            }

            if (internalJobParentStateResolver != null) {
                this.jobServiceConfiguration.setJobParentStateResolver(internalJobParentStateResolver);
            } else {
                this.jobServiceConfiguration.setJobParentStateResolver(new DefaultProcessJobParentStateResolver(this));
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
                this.jobServiceConfiguration.setInternalJobManager(new DefaultInternalJobManager(this));
            }

            if (this.internalJobCompatibilityManager != null) {
                this.jobServiceConfiguration.setInternalJobCompatibilityManager(internalJobCompatibilityManager);
            } else {
                this.jobServiceConfiguration.setInternalJobCompatibilityManager(new DefaultInternalJobCompatibilityManager(this));
            }

            // Async history job config
            jobServiceConfiguration.setJobTypeAsyncHistory(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY);
            jobServiceConfiguration.setJobTypeAsyncHistoryZipped(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED);
            jobServiceConfiguration.setAsyncHistoryJsonGzipCompressionEnabled(isAsyncHistoryJsonGzipCompressionEnabled);
            jobServiceConfiguration.setAsyncHistoryJsonGroupingEnabled(isAsyncHistoryJsonGroupingEnabled);
            jobServiceConfiguration.setAsyncHistoryJsonGroupingThreshold(asyncHistoryJsonGroupingThreshold);

            // set the job processors
            this.jobServiceConfiguration.setJobProcessors(this.jobProcessors);
            this.jobServiceConfiguration.setHistoryJobProcessors(this.historyJobProcessors);

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
        return new JobServiceConfiguration(ScopeTypes.BPMN);
    }

    public void addJobHandler(JobHandler jobHandler) {
        this.jobHandlers.put(jobHandler.getType(), jobHandler);
        if (this.jobServiceConfiguration != null) {
            this.jobServiceConfiguration.addJobHandler(jobHandler.getType(), jobHandler);
        }
    }

    public void removeJobHandler(String jobHandlerType) {
        this.jobHandlers.remove(jobHandlerType);
        if (this.jobServiceConfiguration != null) {
            this.jobServiceConfiguration.getJobHandlers().remove(jobHandlerType);
        }
    }

    public void addHistoryJobHandler(HistoryJobHandler historyJobHandler) {
        this.historyJobHandlers.put(historyJobHandler.getType(), historyJobHandler);
        if (this.jobServiceConfiguration != null) {
            this.jobServiceConfiguration.addHistoryJobHandler(historyJobHandler.getType(), historyJobHandler);
        }
    }
    
    public void initBatchServiceConfiguration() {
        if (batchServiceConfiguration == null) {
            this.batchServiceConfiguration = instantiateBatchServiceConfiguration();
            this.batchServiceConfiguration.setClock(this.clock);
            this.batchServiceConfiguration.setIdGenerator(this.idGenerator);
            this.batchServiceConfiguration.setObjectMapper(this.objectMapper);
            this.batchServiceConfiguration.setEventDispatcher(this.eventDispatcher);

            this.batchServiceConfiguration.init();
        }

        addServiceConfiguration(EngineConfigurationConstants.KEY_BATCH_SERVICE_CONFIG, this.batchServiceConfiguration);
    }
    
    protected BatchServiceConfiguration instantiateBatchServiceConfiguration() {
        return new BatchServiceConfiguration(ScopeTypes.BPMN);
    }

    public void afterInitTaskServiceConfiguration() {
        if (engineConfigurations.containsKey(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)) {
            IdmEngineConfigurationApi idmEngineConfiguration = (IdmEngineConfigurationApi) engineConfigurations.get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
            this.taskServiceConfiguration.setIdmIdentityService(idmEngineConfiguration.getIdmIdentityService());
        }
    }
    
    public void afterInitEventRegistryEventBusConsumer() {
        EventRegistryEventConsumer bpmnEventRegistryEventConsumer = null;
        if (eventRegistryEventConsumer != null) {
            bpmnEventRegistryEventConsumer = eventRegistryEventConsumer;
        } else {
            bpmnEventRegistryEventConsumer = new BpmnEventRegistryEventConsumer(this);
        }
        
        addEventRegistryEventConsumer(bpmnEventRegistryEventConsumer.getConsumerKey(), bpmnEventRegistryEventConsumer);
    }
    
    public void initHistoryCleaningManager() {
        if (historyCleaningManager == null) {
            historyCleaningManager = new DefaultHistoryCleaningManager(this);
        }
    }

    public void removeHistoryJobHandler(String historyJobHandlerType) {
        this.historyJobHandlers.remove(historyJobHandlerType);
        if (this.jobServiceConfiguration != null) {
            this.jobServiceConfiguration.getHistoryJobHandlers().remove(historyJobHandlerType);
        }
    }

    // deployers
    // ////////////////////////////////////////////////////////////////

    public void initProcessDefinitionCache() {
        if (processDefinitionCache == null) {
            if (processDefinitionCacheLimit <= 0) {
                processDefinitionCache = new DefaultDeploymentCache<>();
            } else {
                processDefinitionCache = new DefaultDeploymentCache<>(processDefinitionCacheLimit);
            }
        }
    }

    public void initProcessDefinitionInfoCache() {
        if (processDefinitionInfoCache == null) {
            if (processDefinitionInfoCacheLimit <= 0) {
                processDefinitionInfoCache = new ProcessDefinitionInfoCache(commandExecutor);
            } else {
                processDefinitionInfoCache = new ProcessDefinitionInfoCache(commandExecutor, processDefinitionInfoCacheLimit);
            }
        }
    }

    public void initAppResourceCache() {
        if (appResourceCache == null) {
            if (appResourceCacheLimit <= 0) {
                appResourceCache = new DefaultDeploymentCache<>();
            } else {
                appResourceCache = new DefaultDeploymentCache<>(appResourceCacheLimit);
            }
        }
    }

    public void initKnowledgeBaseCache() {
        if (knowledgeBaseCache == null) {
            if (knowledgeBaseCacheLimit <= 0) {
                knowledgeBaseCache = new DefaultDeploymentCache<>();
            } else {
                knowledgeBaseCache = new DefaultDeploymentCache<>(knowledgeBaseCacheLimit);
            }
        }
    }

    public void initDeployers() {
        if (this.deployers == null) {
            this.deployers = new ArrayList<>();
            if (customPreDeployers != null) {
                this.deployers.addAll(customPreDeployers);
            }
            this.deployers.addAll(getDefaultDeployers());
            if (customPostDeployers != null) {
                this.deployers.addAll(customPostDeployers);
            }
        }

        if (deploymentManager == null) {
            deploymentManager = new DeploymentManager();
            deploymentManager.setDeployers(deployers);

            deploymentManager.setProcessDefinitionCache(processDefinitionCache);
            deploymentManager.setProcessDefinitionInfoCache(processDefinitionInfoCache);
            deploymentManager.setAppResourceCache(appResourceCache);
            deploymentManager.setKnowledgeBaseCache(knowledgeBaseCache);
            deploymentManager.setProcessEngineConfiguration(this);
            deploymentManager.setProcessDefinitionEntityManager(processDefinitionEntityManager);
            deploymentManager.setDeploymentEntityManager(deploymentEntityManager);
        }

        if (appResourceConverter == null) {
            appResourceConverter = new AppResourceConverterImpl(objectMapper);
        }
    }

    public void initBpmnDeployerDependencies() {

        if (parsedDeploymentBuilderFactory == null) {
            parsedDeploymentBuilderFactory = new ParsedDeploymentBuilderFactory();
        }
        if (parsedDeploymentBuilderFactory.getBpmnParser() == null) {
            parsedDeploymentBuilderFactory.setBpmnParser(bpmnParser);
        }

        if (timerManager == null) {
            timerManager = new TimerManager();
        }

        if (eventSubscriptionManager == null) {
            eventSubscriptionManager = new EventSubscriptionManager();
        }

        if (bpmnDeploymentHelper == null) {
            bpmnDeploymentHelper = new BpmnDeploymentHelper();
        }
        if (bpmnDeploymentHelper.getTimerManager() == null) {
            bpmnDeploymentHelper.setTimerManager(timerManager);
        }
        if (bpmnDeploymentHelper.getEventSubscriptionManager() == null) {
            bpmnDeploymentHelper.setEventSubscriptionManager(eventSubscriptionManager);
        }

        if (cachingAndArtifactsManager == null) {
            cachingAndArtifactsManager = new CachingAndArtifactsManager();
        }

        if (processDefinitionDiagramHelper == null) {
            processDefinitionDiagramHelper = new ProcessDefinitionDiagramHelper();
        }
    }

    public Collection<? extends EngineDeployer> getDefaultDeployers() {
        List<EngineDeployer> defaultDeployers = new ArrayList<>();

        if (bpmnDeployer == null) {
            bpmnDeployer = new BpmnDeployer();
        }

        initBpmnDeployerDependencies();

        bpmnDeployer.setIdGenerator(idGenerator);
        bpmnDeployer.setParsedDeploymentBuilderFactory(parsedDeploymentBuilderFactory);
        bpmnDeployer.setBpmnDeploymentHelper(bpmnDeploymentHelper);
        bpmnDeployer.setCachingAndArtifactsManager(cachingAndArtifactsManager);
        bpmnDeployer.setProcessDefinitionDiagramHelper(processDefinitionDiagramHelper);
        bpmnDeployer.setUsePrefixId(usePrefixId);

        defaultDeployers.add(bpmnDeployer);

        if (appDeployer == null) {
            appDeployer = new AppDeployer();
        }

        defaultDeployers.add(appDeployer);

        return defaultDeployers;
    }

    public void initListenerFactory() {
        if (listenerFactory == null) {
            DefaultListenerFactory defaultListenerFactory = new DefaultListenerFactory();
            defaultListenerFactory.setExpressionManager(expressionManager);
            listenerFactory = defaultListenerFactory;
        } else if ((listenerFactory instanceof AbstractBehaviorFactory) && ((AbstractBehaviorFactory) listenerFactory).getExpressionManager() == null) {
            ((AbstractBehaviorFactory) listenerFactory).setExpressionManager(expressionManager);
        }
    }

    public void initWsdlImporterFactory() {
        if (wsWsdlImporterFactory == null) {
            DefaultXMLImporterFactory defaultListenerFactory = new DefaultXMLImporterFactory();
            wsWsdlImporterFactory = defaultListenerFactory;
        }
    }

    public void initBehaviorFactory() {
        if (activityBehaviorFactory == null) {
            DefaultActivityBehaviorFactory defaultActivityBehaviorFactory = new DefaultActivityBehaviorFactory();
            defaultActivityBehaviorFactory.setExpressionManager(expressionManager);
            activityBehaviorFactory = defaultActivityBehaviorFactory;
        } else if ((activityBehaviorFactory instanceof AbstractBehaviorFactory) && ((AbstractBehaviorFactory) activityBehaviorFactory).getExpressionManager() == null) {
            ((AbstractBehaviorFactory) activityBehaviorFactory).setExpressionManager(expressionManager);
        }
    }

    public void initBpmnParser() {
        if (bpmnParser == null) {
            bpmnParser = new BpmnParser();
        }

        if (bpmnParseFactory == null) {
            bpmnParseFactory = new DefaultBpmnParseFactory();
        }

        bpmnParser.setBpmnParseFactory(bpmnParseFactory);
        bpmnParser.setActivityBehaviorFactory(activityBehaviorFactory);
        bpmnParser.setListenerFactory(listenerFactory);

        List<BpmnParseHandler> parseHandlers = new ArrayList<>();
        if (getPreBpmnParseHandlers() != null) {
            parseHandlers.addAll(getPreBpmnParseHandlers());
        }
        parseHandlers.addAll(getDefaultBpmnParseHandlers());
        if (getPostBpmnParseHandlers() != null) {
            parseHandlers.addAll(getPostBpmnParseHandlers());
        }

        BpmnParseHandlers bpmnParseHandlers = new BpmnParseHandlers();
        bpmnParseHandlers.addHandlers(parseHandlers);
        bpmnParser.setBpmnParserHandlers(bpmnParseHandlers);
    }

    public List<BpmnParseHandler> getDefaultBpmnParseHandlers() {

        // Alphabetic list of default parse handler classes
        List<BpmnParseHandler> bpmnParserHandlers = new ArrayList<>();
        bpmnParserHandlers.add(new BoundaryEventParseHandler());
        bpmnParserHandlers.add(new BusinessRuleParseHandler());
        bpmnParserHandlers.add(new CallActivityParseHandler());
        bpmnParserHandlers.add(new CaseServiceTaskParseHandler());
        bpmnParserHandlers.add(new CancelEventDefinitionParseHandler());
        bpmnParserHandlers.add(new CompensateEventDefinitionParseHandler());
        bpmnParserHandlers.add(new ConditionalEventDefinitionParseHandler());
        bpmnParserHandlers.add(new EndEventParseHandler());
        bpmnParserHandlers.add(new ErrorEventDefinitionParseHandler());
        bpmnParserHandlers.add(new EscalationEventDefinitionParseHandler());
        bpmnParserHandlers.add(new EventBasedGatewayParseHandler());
        bpmnParserHandlers.add(new ExclusiveGatewayParseHandler());
        bpmnParserHandlers.add(new InclusiveGatewayParseHandler());
        bpmnParserHandlers.add(new IntermediateCatchEventParseHandler());
        bpmnParserHandlers.add(new IntermediateThrowEventParseHandler());
        bpmnParserHandlers.add(new ManualTaskParseHandler());
        bpmnParserHandlers.add(new MessageEventDefinitionParseHandler());
        bpmnParserHandlers.add(new ParallelGatewayParseHandler());
        bpmnParserHandlers.add(new ProcessParseHandler());
        bpmnParserHandlers.add(new ReceiveTaskParseHandler());
        bpmnParserHandlers.add(new ScriptTaskParseHandler());
        bpmnParserHandlers.add(new SendEventServiceTaskParseHandler());
        bpmnParserHandlers.add(new ExternalWorkerServiceTaskParseHandler());
        bpmnParserHandlers.add(new SendTaskParseHandler());
        bpmnParserHandlers.add(new SequenceFlowParseHandler());
        bpmnParserHandlers.add(new ServiceTaskParseHandler());
        bpmnParserHandlers.add(new HttpServiceTaskParseHandler());
        bpmnParserHandlers.add(new SignalEventDefinitionParseHandler());
        bpmnParserHandlers.add(new StartEventParseHandler());
        bpmnParserHandlers.add(new SubProcessParseHandler());
        bpmnParserHandlers.add(new EventSubProcessParseHandler());
        bpmnParserHandlers.add(new AdhocSubProcessParseHandler());
        bpmnParserHandlers.add(new TaskParseHandler());
        bpmnParserHandlers.add(new TimerEventDefinitionParseHandler());
        bpmnParserHandlers.add(new TransactionParseHandler());
        bpmnParserHandlers.add(new UserTaskParseHandler());
        bpmnParserHandlers.add(new VariableListenerEventDefinitionParseHandler());

        // Replace any default handler if the user wants to replace them
        if (customDefaultBpmnParseHandlers != null) {

            Map<Class<?>, BpmnParseHandler> customParseHandlerMap = new HashMap<>();
            for (BpmnParseHandler bpmnParseHandler : customDefaultBpmnParseHandlers) {
                for (Class<?> handledType : bpmnParseHandler.getHandledTypes()) {
                    customParseHandlerMap.put(handledType, bpmnParseHandler);
                }
            }

            for (int i = 0; i < bpmnParserHandlers.size(); i++) {
                // All the default handlers support only one type
                BpmnParseHandler defaultBpmnParseHandler = bpmnParserHandlers.get(i);
                if (defaultBpmnParseHandler.getHandledTypes().size() != 1) {
                    StringBuilder supportedTypes = new StringBuilder();
                    for (Class<?> type : defaultBpmnParseHandler.getHandledTypes()) {
                        supportedTypes.append(" ").append(type.getCanonicalName()).append(" ");
                    }
                    throw new FlowableException("The default BPMN parse handlers should only support one type, but " + defaultBpmnParseHandler.getClass() + " supports " + supportedTypes
                        + ". This is likely a programmatic error");
                } else {
                    Class<?> handledType = defaultBpmnParseHandler.getHandledTypes().iterator().next();
                    if (customParseHandlerMap.containsKey(handledType)) {
                        BpmnParseHandler newBpmnParseHandler = customParseHandlerMap.get(handledType);
                        logger.info("Replacing default BpmnParseHandler {} with {}", defaultBpmnParseHandler.getClass().getName(), newBpmnParseHandler.getClass().getName());
                        bpmnParserHandlers.set(i, newBpmnParseHandler);
                    }
                }
            }
        }

        return bpmnParserHandlers;
    }

    public void initProcessDiagramGenerator() {
        if (processDiagramGenerator == null) {
            processDiagramGenerator = new DefaultProcessDiagramGenerator();
        }
    }

    public void initJobHandlers() {
        jobHandlers = new HashMap<>();

        AsyncContinuationJobHandler asyncContinuationJobHandler = new AsyncContinuationJobHandler();
        jobHandlers.put(asyncContinuationJobHandler.getType(), asyncContinuationJobHandler);

        AsyncTriggerJobHandler asyncTriggerJobHandler = new AsyncTriggerJobHandler();
        jobHandlers.put(asyncTriggerJobHandler.getType(), asyncTriggerJobHandler);

        TriggerTimerEventJobHandler triggerTimerEventJobHandler = new TriggerTimerEventJobHandler();
        jobHandlers.put(triggerTimerEventJobHandler.getType(), triggerTimerEventJobHandler);

        TimerStartEventJobHandler timerStartEvent = new TimerStartEventJobHandler();
        jobHandlers.put(timerStartEvent.getType(), timerStartEvent);

        TimerSuspendProcessDefinitionHandler suspendProcessDefinitionHandler = new TimerSuspendProcessDefinitionHandler();
        jobHandlers.put(suspendProcessDefinitionHandler.getType(), suspendProcessDefinitionHandler);

        TimerActivateProcessDefinitionHandler activateProcessDefinitionHandler = new TimerActivateProcessDefinitionHandler();
        jobHandlers.put(activateProcessDefinitionHandler.getType(), activateProcessDefinitionHandler);

        ProcessEventJobHandler processEventJobHandler = new ProcessEventJobHandler();
        jobHandlers.put(processEventJobHandler.getType(), processEventJobHandler);

        AsyncCompleteCallActivityJobHandler asyncCompleteCallActivityJobHandler = new AsyncCompleteCallActivityJobHandler();
        jobHandlers.put(asyncCompleteCallActivityJobHandler.getType(), asyncCompleteCallActivityJobHandler);
        
        AsyncSendEventJobHandler asyncSendEventJobHandler = new AsyncSendEventJobHandler();
        jobHandlers.put(asyncSendEventJobHandler.getType(), asyncSendEventJobHandler);
        
        BpmnHistoryCleanupJobHandler bpmnHistoryCleanupJobHandler = new BpmnHistoryCleanupJobHandler();
        jobHandlers.put(bpmnHistoryCleanupJobHandler.getType(), bpmnHistoryCleanupJobHandler);

        ProcessInstanceMigrationJobHandler processInstanceMigrationJobHandler = new ProcessInstanceMigrationJobHandler();
        jobHandlers.put(processInstanceMigrationJobHandler.getType(), processInstanceMigrationJobHandler);
        
        ProcessInstanceMigrationStatusJobHandler processInstanceMigrationStatusJobHandler = new ProcessInstanceMigrationStatusJobHandler();
        jobHandlers.put(processInstanceMigrationStatusJobHandler.getType(), processInstanceMigrationStatusJobHandler);

        ExternalWorkerTaskCompleteJobHandler externalWorkerTaskCompleteJobHandler = new ExternalWorkerTaskCompleteJobHandler();
        jobHandlers.put(externalWorkerTaskCompleteJobHandler.getType(), externalWorkerTaskCompleteJobHandler);

        ParallelMultiInstanceActivityCompletionJobHandler parallelMultiInstanceActivityCompletionJobHandler = new ParallelMultiInstanceActivityCompletionJobHandler();
        jobHandlers.put(parallelMultiInstanceActivityCompletionJobHandler.getType(), parallelMultiInstanceActivityCompletionJobHandler);

        ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler parallelMultiInstanceWithNoWaitStateCompletionJobHandler = new ParallelMultiInstanceWithNoWaitStatesAsyncLeaveJobHandler();
        jobHandlers.put(parallelMultiInstanceWithNoWaitStateCompletionJobHandler.getType(), parallelMultiInstanceWithNoWaitStateCompletionJobHandler);

        ComputeDeleteHistoricProcessInstanceIdsJobHandler computeDeleteHistoricProcessInstanceIdsJobHandler = new ComputeDeleteHistoricProcessInstanceIdsJobHandler();
        jobHandlers.put(computeDeleteHistoricProcessInstanceIdsJobHandler.getType(), computeDeleteHistoricProcessInstanceIdsJobHandler);

        DeleteHistoricProcessInstanceIdsJobHandler deleteHistoricProcessInstanceBatchJobHandler = new DeleteHistoricProcessInstanceIdsJobHandler();
        jobHandlers.put(deleteHistoricProcessInstanceBatchJobHandler.getType(), deleteHistoricProcessInstanceBatchJobHandler);

        ComputeDeleteHistoricProcessInstanceStatusJobHandler computeDeleteHistoricProcessInstanceStatusJobHandler = new ComputeDeleteHistoricProcessInstanceStatusJobHandler();
        jobHandlers.put(computeDeleteHistoricProcessInstanceStatusJobHandler.getType(), computeDeleteHistoricProcessInstanceStatusJobHandler);

        DeleteHistoricProcessInstanceIdsStatusJobHandler deleteHistoricProcessInstanceStatusJobHandler = new DeleteHistoricProcessInstanceIdsStatusJobHandler();
        jobHandlers.put(deleteHistoricProcessInstanceStatusJobHandler.getType(), deleteHistoricProcessInstanceStatusJobHandler);

        // if we have custom job handlers, register them
        if (getCustomJobHandlers() != null) {
            for (JobHandler customJobHandler : getCustomJobHandlers()) {
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

            AsyncHistoryJobHandler asyncHistoryJobHandler = new AsyncHistoryJobHandler(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY);
            allHistoryJsonTransformers.forEach(asyncHistoryJobHandler::addHistoryJsonTransformer);
            asyncHistoryJobHandler.setAsyncHistoryJsonGroupingEnabled(isAsyncHistoryJsonGroupingEnabled);
            historyJobHandlers.put(asyncHistoryJobHandler.getType(), asyncHistoryJobHandler);

            AsyncHistoryJobZippedHandler asyncHistoryJobZippedHandler = new AsyncHistoryJobZippedHandler(HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED);
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
        historyJsonTransformers.add(new ProcessInstanceStartHistoryJsonTransformer(this));
        historyJsonTransformers.add(new ProcessInstanceEndHistoryJsonTransformer(this));
        historyJsonTransformers.add(new ProcessInstanceDeleteHistoryJsonTransformer(this));
        historyJsonTransformers.add(new ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer(this));
        historyJsonTransformers.add(new ProcessInstancePropertyChangedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new SubProcessInstanceStartHistoryJsonTransformer(this));
        historyJsonTransformers.add(new SetProcessDefinitionHistoryJsonTransformer(this));
        historyJsonTransformers.add(new UpdateProcessDefinitionCascadeHistoryJsonTransformer(this));

        historyJsonTransformers.add(new ActivityStartHistoryJsonTransformer(this));
        historyJsonTransformers.add(new ActivityEndHistoryJsonTransformer(this));
        historyJsonTransformers.add(new ActivityFullHistoryJsonTransformer(this));
        historyJsonTransformers.add(new ActivityUpdateHistoryJsonTransformer(this));

        historyJsonTransformers.add(new TaskCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new TaskEndedHistoryJsonTransformer(this));

        historyJsonTransformers.add(new TaskPropertyChangedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new TaskAssigneeChangedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new TaskOwnerChangedHistoryJsonTransformer(this));

        historyJsonTransformers.add(new IdentityLinkCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new IdentityLinkDeletedHistoryJsonTransformer(this));

        historyJsonTransformers.add(new EntityLinkCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new EntityLinkDeletedHistoryJsonTransformer(this));

        historyJsonTransformers.add(new VariableCreatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new VariableUpdatedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new VariableRemovedHistoryJsonTransformer(this));
        historyJsonTransformers.add(new HistoricDetailVariableUpdateHistoryJsonTransformer(this));
        historyJsonTransformers.add(new FormPropertiesSubmittedHistoryJsonTransformer(this));

        historyJsonTransformers.add(new HistoricUserTaskLogRecordJsonTransformer(this));
        historyJsonTransformers.add(new HistoricUserTaskLogDeleteJsonTransformer(this));
        return historyJsonTransformers;
    }

    // async executor
    // /////////////////////////////////////////////////////////////

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
            this.asyncTaskInvoker = new DefaultAsyncTaskInvoker(this.asyncTaskExecutor);
        }
    }

    public void initAsyncExecutor() {
        initAsyncTaskExecutor();
        if (asyncExecutor == null) {
            DefaultAsyncJobExecutor defaultAsyncExecutor = new DefaultAsyncJobExecutor(asyncExecutorConfiguration);
            if (asyncExecutorExecuteAsyncRunnableFactory != null) {
                defaultAsyncExecutor.setExecuteAsyncRunnableFactory(asyncExecutorExecuteAsyncRunnableFactory);
            }

            // Message queue mode
            defaultAsyncExecutor.setMessageQueueMode(asyncExecutorMessageQueueMode);

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
                DefaultAsyncHistoryJobExecutor defaultAsyncHistoryExecutor = new DefaultAsyncHistoryJobExecutor(getOrCreateAsyncHistoryExecutorConfiguration());

                // Message queue mode
                defaultAsyncHistoryExecutor.setMessageQueueMode(asyncHistoryExecutorMessageQueueMode);

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
                    historyJobHandlers.forEach((type, handler) -> {
                        asyncHistoryExecutor.getJobServiceConfiguration().mergeHistoryJobHandler(handler);
                    });
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

    protected AsyncJobExecutorConfiguration getOrCreateAsyncHistoryExecutorConfiguration() {
        if (asyncHistoryExecutorConfiguration == null) {
            asyncHistoryExecutorConfiguration = new AsyncJobExecutorConfiguration();
        }

        return asyncHistoryExecutorConfiguration;
    }

    // history
    // //////////////////////////////////////////////////////////////////

    public void initHistoryLevel() {
        if (historyLevel == null) {
            historyLevel = HistoryLevel.getHistoryLevelForKey(getHistory());
        }
    }

    // id generator
    // /////////////////////////////////////////////////////////////

    @Override
    public void initIdGenerator() {
        if (idGenerator == null) {
            DbIdGenerator dbIdGenerator = new DbIdGenerator();
            dbIdGenerator.setIdBlockSize(idBlockSize);
            idGenerator = dbIdGenerator;
        }

        if (idGenerator instanceof DbIdGenerator) {
            DbIdGenerator dbIdGenerator = (DbIdGenerator) idGenerator;
            if (dbIdGenerator.getIdBlockSize() == 0) {
                dbIdGenerator.setIdBlockSize(idBlockSize);
            }
            if (dbIdGenerator.getCommandExecutor() == null) {
                dbIdGenerator.setCommandExecutor(getCommandExecutor());
            }
            if (dbIdGenerator.getCommandConfig() == null) {
                dbIdGenerator.setCommandConfig(getDefaultCommandConfig().transactionRequiresNew());
            }
        }
    }

    // OTHER
    // ////////////////////////////////////////////////////////////////////

    @Override
    public void initTransactionFactory() {
        if (transactionFactory == null) {
            if (transactionsExternallyManaged) {
                transactionFactory = new ManagedTransactionFactory();
            } else {
                transactionFactory = new JdbcTransactionFactory();
            }
        }
    }

    public void initHelpers() {
        if (processInstanceHelper == null) {
            processInstanceHelper = new ProcessInstanceHelper();
        }
        if (listenerNotificationHelper == null) {
            listenerNotificationHelper = new ListenerNotificationHelper();
        }
        if (formHandlerHelper == null) {
            formHandlerHelper = new FormHandlerHelper();
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
            variableTypes.addType(new ParallelMultiInstanceLoopVariableType(this));
            variableTypes.addType(new BpmnAggregatedVariableType(this));
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

            if (variableTypes.getVariableType(BpmnAggregatedVariableType.TYPE_NAME) == null) {
                variableTypes.addTypeBefore(new BpmnAggregatedVariableType(this), SerializableType.TYPE_NAME);
            }

            if (variableTypes.getVariableType(ParallelMultiInstanceLoopVariableType.TYPE_NAME) == null) {
                variableTypes.addTypeBefore(new ParallelMultiInstanceLoopVariableType(this), SerializableType.TYPE_NAME);
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

    public void initFormEngines() {
        if (formEngines == null) {
            formEngines = new HashMap<>();
            FormEngine defaultFormEngine = new JuelFormEngine();
            formEngines.put(null, defaultFormEngine); // default form engine is
            // looked up with null
            formEngines.put(defaultFormEngine.getName(), defaultFormEngine);
        }
        if (customFormEngines != null) {
            for (FormEngine formEngine : customFormEngines) {
                formEngines.put(formEngine.getName(), formEngine);
            }
        }
    }

    public void initFormTypes() {
        if (formTypes == null) {
            formTypes = new FormTypes();
            formTypes.addFormType(new StringFormType());
            formTypes.addFormType(new LongFormType());
            formTypes.addFormType(new DateFormType("dd/MM/yyyy"));
            formTypes.addFormType(new BooleanFormType());
            formTypes.addFormType(new DoubleFormType());
        }
        if (customFormTypes != null) {
            for (AbstractFormType customFormType : customFormTypes) {
                formTypes.addFormType(customFormType);
            }
        }
    }

    public void initScriptingEngines() {
        if (resolverFactories == null) {
            resolverFactories = new ArrayList<>();
            resolverFactories.add(new VariableScopeResolverFactory());
            resolverFactories.add(new BeansResolverFactory());
        }
        if (scriptingEngines == null) {
            scriptingEngines = new ScriptingEngines(new ScriptBindingsFactory(this, resolverFactories));
        }
    }

    public void initExpressionManager() {
        if (expressionManager == null) {
            ProcessExpressionManager processExpressionManager = new ProcessExpressionManager(delegateInterceptor, beans);

            if (isExpressionCacheEnabled) {
                processExpressionManager.setExpressionCache(new DefaultDeploymentCache<>(expressionCacheSize));
                processExpressionManager.setExpressionTextLengthCacheLimit(expressionTextLengthCacheLimit);
            }

            if (preDefaultELResolvers != null) {
                preDefaultELResolvers.forEach(processExpressionManager::addPreDefaultResolver);
            }

            if (preBeanELResolvers != null) {
                preBeanELResolvers.forEach(processExpressionManager::addPreBeanResolver);
            }

            if (postDefaultELResolvers != null) {
                postDefaultELResolvers.forEach(processExpressionManager::addPostDefaultResolver);
            }

            expressionManager = processExpressionManager;
        }
        expressionManager.setFunctionDelegates(flowableFunctionDelegates);
        expressionManager.setAstFunctionCreators(astFunctionCreators);
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

    public void initAgendaFactory() {
        if (this.agendaFactory == null) {
            this.agendaFactory = new DefaultFlowableEngineAgendaFactory();
        }
    }

    public void initDelegateInterceptor() {
        if (delegateInterceptor == null) {
            delegateInterceptor = new DefaultDelegateInterceptor();
        }
    }

    public void initEventHandlers() {
        if (eventHandlers == null) {
            eventHandlers = new HashMap<>();

            SignalEventHandler signalEventHandler = new SignalEventHandler();
            eventHandlers.put(signalEventHandler.getEventHandlerType(), signalEventHandler);

            CompensationEventHandler compensationEventHandler = new CompensationEventHandler();
            eventHandlers.put(compensationEventHandler.getEventHandlerType(), compensationEventHandler);

            MessageEventHandler messageEventHandler = new MessageEventHandler();
            eventHandlers.put(messageEventHandler.getEventHandlerType(), messageEventHandler);

        }
        if (customEventHandlers != null) {
            for (EventHandler eventHandler : customEventHandlers) {
                eventHandlers.put(eventHandler.getEventHandlerType(), eventHandler);
            }
        }
    }

    // JPA
    // //////////////////////////////////////////////////////////////////////

    public void initJpa() {
        if (jpaPersistenceUnitName != null) {
            jpaEntityManagerFactory = JpaHelper.createEntityManagerFactory(jpaPersistenceUnitName);
        }
        if (jpaEntityManagerFactory != null) {
            sessionFactories.put(EntityManagerSession.class, new EntityManagerSessionFactory(jpaEntityManagerFactory, jpaHandleTransaction, jpaCloseEntityManager));
            VariableType jpaType = variableTypes.getVariableType(JPAEntityVariableType.TYPE_NAME);
            // Add JPA-type
            if (jpaType == null) {
                // We try adding the variable right before SerializableType, if
                // available
                int serializableIndex = variableTypes.getTypeIndex(SerializableType.TYPE_NAME);
                if (serializableIndex > -1) {
                    variableTypes.addType(new JPAEntityVariableType(), serializableIndex);
                } else {
                    variableTypes.addType(new JPAEntityVariableType());
                }
            }

            jpaType = variableTypes.getVariableType(JPAEntityListVariableType.TYPE_NAME);

            // Add JPA-list type after regular JPA type if not already present
            if (jpaType == null) {
                variableTypes.addType(new JPAEntityListVariableType(), variableTypes.getTypeIndex(JPAEntityVariableType.TYPE_NAME));
            }
        }
    }

    public void initProcessValidator() {
        if (this.processValidator == null) {
            if (customServiceTaskValidator == null) {
                this.processValidator = new ProcessValidatorFactory().createDefaultProcessValidator();
            } else {
                ProcessValidatorFactory processValidatorFactory = new ProcessValidatorFactory();
                processValidatorFactory.setCustomServiceTaskValidator(customServiceTaskValidator);
                this.processValidator = processValidatorFactory.createDefaultProcessValidator();
            }
        }
    }

    @Override
    protected void initAdditionalEventDispatchActions() {
        if (this.additionalEventDispatchActions == null) {
            this.additionalEventDispatchActions = new ArrayList<>();
            this.additionalEventDispatchActions.add(new BpmnModelEventDispatchAction());
        }
    }

    public void initFormFieldHandler() {
        if (this.formFieldHandler == null) {
            this.formFieldHandler = new DefaultFormFieldHandler();
        }
    }

    public void initFunctionDelegates() {
        if (this.flowableFunctionDelegates == null) {
            this.flowableFunctionDelegates = new ArrayList<>();
            this.flowableFunctionDelegates.add(new FlowableDateFunctionDelegate());

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

            flowableFunctionDelegates.add(new TaskGetFunctionDelegate());
        }

        if (this.customFlowableFunctionDelegates != null) {
            this.flowableFunctionDelegates.addAll(this.customFlowableFunctionDelegates);
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

    public void initDatabaseEventLogging() {
        if (enableDatabaseEventLogging) {
            // Database event logging uses the default logging mechanism and adds
            // a specific event listener to the list of event listeners
            getEventDispatcher().addEventListener(new EventLogger(clock, objectMapper));
        }
    }

    public void initFlowable5CompatibilityHandler() {

        // If Flowable 5 compatibility is disabled, no need to do anything
        // If handler is injected, no need to do anything
        if (flowable5CompatibilityEnabled && flowable5CompatibilityHandler == null) {

            // Create default factory if nothing set
            if (flowable5CompatibilityHandlerFactory == null) {
                flowable5CompatibilityHandlerFactory = new DefaultFlowable5CompatibilityHandlerFactory();
            }

            // Create handler instance
            flowable5CompatibilityHandler = flowable5CompatibilityHandlerFactory.createFlowable5CompatibilityHandler();

            if (flowable5CompatibilityHandler != null) {
                logger.info("Found compatibility handler instance : {}", flowable5CompatibilityHandler.getClass());

                flowable5CompatibilityHandler.setFlowable6ProcessEngineConfiguration(this);
            }
        }

    }

    /**
     * Called when the {@link ProcessEngine} is initialized, but before it is returned
     */
    protected void postProcessEngineInitialisation() {
        if (validateFlowable5EntitiesEnabled) {
            commandExecutor.execute(new ValidateV5EntitiesCmd());
        }

        if (redeployFlowable5ProcessDefinitions) {
            commandExecutor.execute(new RedeployV5ProcessDefinitionsCmd());
        }

        if (performanceSettings.isValidateExecutionRelationshipCountConfigOnBoot()) {
            commandExecutor.execute(new ValidateExecutionRelatedEntityCountCfgCmd());
        }

        if (performanceSettings.isValidateTaskRelationshipCountConfigOnBoot()) {
            commandExecutor.execute(new ValidateTaskRelatedEntityCountCfgCmd());
        }

        // if Flowable 5 support is needed configure the Flowable 5 job processors via the compatibility handler
        if (flowable5CompatibilityEnabled) {
            flowable5CompatibilityHandler.setJobProcessor(this.flowable5JobProcessors);
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


    public Runnable getProcessEngineCloseRunnable() {
        return () -> {

            // Async executor will have cleared the jobs lock owner/times, but not yet the process instance lock time/owner
            if (asyncExecutor != null) {
                commandExecutor.execute(new ClearProcessInstanceLockTimesCmd(asyncExecutor.getLockOwner()));
            }

            commandExecutor.execute(getSchemaCommandConfig(), new SchemaOperationProcessEngineClose());
        };
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
    public ProcessEngineConfigurationImpl addConfigurator(EngineConfigurator configurator) {
        super.addConfigurator(configurator);
        return this;
    }

    public void initLocalizationManagers() {
        if (this.internalProcessLocalizationManager == null) {
            this.setInternalProcessLocalizationManager(new DefaultProcessLocalizationManager(this));
        }

        if (this.internalProcessDefinitionLocalizationManager == null) {
            this.setInternalProcessDefinitionLocalizationManager(new DefaultProcessDefinitionLocalizationManager(this));
        }

    }

    // getters and setters
    // //////////////////////////////////////////////////////

    @Override
    public ProcessEngineConfigurationImpl setEngineName(String processEngineName) {
        this.processEngineName = processEngineName;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setDatabaseSchemaUpdate(String databaseSchemaUpdate) {
        this.databaseSchemaUpdate = databaseSchemaUpdate;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setDefaultCommandConfig(CommandConfig defaultCommandConfig) {
        this.defaultCommandConfig = defaultCommandConfig;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setSchemaCommandConfig(CommandConfig schemaCommandConfig) {
        this.schemaCommandConfig = schemaCommandConfig;
        return this;
    }

    @Override
    public List<CommandInterceptor> getCustomPreCommandInterceptors() {
        return customPreCommandInterceptors;
    }

    @Override
    public ProcessEngineConfigurationImpl setCustomPreCommandInterceptors(List<CommandInterceptor> customPreCommandInterceptors) {
        this.customPreCommandInterceptors = customPreCommandInterceptors;
        return this;
    }

    @Override
    public List<CommandInterceptor> getCustomPostCommandInterceptors() {
        return customPostCommandInterceptors;
    }

    @Override
    public ProcessEngineConfigurationImpl setCustomPostCommandInterceptors(List<CommandInterceptor> customPostCommandInterceptors) {
        this.customPostCommandInterceptors = customPostCommandInterceptors;
        return this;
    }

    @Override
    public List<CommandInterceptor> getCommandInterceptors() {
        return commandInterceptors;
    }

    @Override
    public ProcessEngineConfigurationImpl setCommandInterceptors(List<CommandInterceptor> commandInterceptors) {
        this.commandInterceptors = commandInterceptors;
        return this;
    }

    @Override
    public RepositoryService getRepositoryService() {
        return repositoryService;
    }

    public ProcessEngineConfigurationImpl setRepositoryService(RepositoryService repositoryService) {
        this.repositoryService = repositoryService;
        return this;
    }

    @Override
    public RuntimeService getRuntimeService() {
        return runtimeService;
    }

    public ProcessEngineConfigurationImpl setRuntimeService(RuntimeService runtimeService) {
        this.runtimeService = runtimeService;
        return this;
    }

    @Override
    public HistoryService getHistoryService() {
        return historyService;
    }

    public ProcessEngineConfigurationImpl setHistoryService(HistoryService historyService) {
        this.historyService = historyService;
        return this;
    }

    @Override
    public IdentityService getIdentityService() {
        return identityService;
    }

    public ProcessEngineConfigurationImpl setIdentityService(IdentityService identityService) {
        this.identityService = identityService;
        return this;
    }

    @Override
    public TaskService getTaskService() {
        return taskService;
    }

    public ProcessEngineConfigurationImpl setTaskService(TaskService taskService) {
        this.taskService = taskService;
        return this;
    }

    @Override
    public FormService getFormService() {
        return formService;
    }

    public ProcessEngineConfigurationImpl setFormService(FormService formService) {
        this.formService = formService;
        return this;
    }

    @Override
    public ManagementService getManagementService() {
        return managementService;
    }

    public ProcessEngineConfigurationImpl setManagementService(ManagementService managementService) {
        this.managementService = managementService;
        return this;
    }

    public DynamicBpmnService getDynamicBpmnService() {
        return dynamicBpmnService;
    }

    public ProcessEngineConfigurationImpl setDynamicBpmnService(DynamicBpmnService dynamicBpmnService) {
        this.dynamicBpmnService = dynamicBpmnService;
        return this;
    }

    public ProcessMigrationService getProcessMigrationService() {
        return processInstanceMigrationService;
    }

    public void setProcessInstanceMigrationService(ProcessMigrationService processInstanceMigrationService) {
        this.processInstanceMigrationService = processInstanceMigrationService;
    }

    @Override
    public ProcessEngineConfigurationImpl getProcessEngineConfiguration() {
        return this;
    }

    public boolean isDisableIdmEngine() {
        return disableIdmEngine;
    }

    public ProcessEngineConfigurationImpl setDisableIdmEngine(boolean disableIdmEngine) {
        this.disableIdmEngine = disableIdmEngine;
        return this;
    }
    
    public boolean isDisableEventRegistry() {
        return disableEventRegistry;
    }

    public ProcessEngineConfigurationImpl setDisableEventRegistry(boolean disableEventRegistry) {
        this.disableEventRegistry = disableEventRegistry;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
        this.sessionFactories = sessionFactories;
        return this;
    }

    public BpmnDeployer getBpmnDeployer() {
        return bpmnDeployer;
    }

    public ProcessEngineConfigurationImpl setBpmnDeployer(BpmnDeployer bpmnDeployer) {
        this.bpmnDeployer = bpmnDeployer;
        return this;
    }

    public BpmnParser getBpmnParser() {
        return bpmnParser;
    }

    public ProcessEngineConfigurationImpl setBpmnParser(BpmnParser bpmnParser) {
        this.bpmnParser = bpmnParser;
        return this;
    }

    public ParsedDeploymentBuilderFactory getParsedDeploymentBuilderFactory() {
        return parsedDeploymentBuilderFactory;
    }

    public ProcessEngineConfigurationImpl setParsedDeploymentBuilderFactory(ParsedDeploymentBuilderFactory parsedDeploymentBuilderFactory) {
        this.parsedDeploymentBuilderFactory = parsedDeploymentBuilderFactory;
        return this;
    }

    public TimerManager getTimerManager() {
        return timerManager;
    }

    public void setTimerManager(TimerManager timerManager) {
        this.timerManager = timerManager;
    }

    public EventSubscriptionManager getEventSubscriptionManager() {
        return eventSubscriptionManager;
    }

    public void setEventSubscriptionManager(EventSubscriptionManager eventSubscriptionManager) {
        this.eventSubscriptionManager = eventSubscriptionManager;
    }

    public BpmnDeploymentHelper getBpmnDeploymentHelper() {
        return bpmnDeploymentHelper;
    }

    public ProcessEngineConfigurationImpl setBpmnDeploymentHelper(BpmnDeploymentHelper bpmnDeploymentHelper) {
        this.bpmnDeploymentHelper = bpmnDeploymentHelper;
        return this;
    }

    public CachingAndArtifactsManager getCachingAndArtifactsManager() {
        return cachingAndArtifactsManager;
    }

    public void setCachingAndArtifactsManager(CachingAndArtifactsManager cachingAndArtifactsManager) {
        this.cachingAndArtifactsManager = cachingAndArtifactsManager;
    }

    public ProcessDefinitionDiagramHelper getProcessDefinitionDiagramHelper() {
        return processDefinitionDiagramHelper;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionDiagramHelper(ProcessDefinitionDiagramHelper processDefinitionDiagramHelper) {
        this.processDefinitionDiagramHelper = processDefinitionDiagramHelper;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setIdGenerator(IdGenerator idGenerator) {
        this.idGenerator = idGenerator;
        return this;
    }

    public String getWsSyncFactoryClassName() {
        return wsSyncFactoryClassName;
    }

    public ProcessEngineConfigurationImpl setWsSyncFactoryClassName(String wsSyncFactoryClassName) {
        this.wsSyncFactoryClassName = wsSyncFactoryClassName;
        return this;
    }

    public XMLImporterFactory getWsdlImporterFactory() {
        return wsWsdlImporterFactory;
    }

    public ProcessEngineConfigurationImpl setWsdlImporterFactory(XMLImporterFactory wsWsdlImporterFactory) {
        this.wsWsdlImporterFactory = wsWsdlImporterFactory;
        return this;
    }

    /**
     * Add or replace the address of the given web-service endpoint with the given value
     *
     * @param endpointName
     *     The endpoint name for which a new address must be set
     * @param address
     *     The new address of the endpoint
     */
    public ProcessEngineConfiguration addWsEndpointAddress(QName endpointName, URL address) {
        this.wsOverridenEndpointAddresses.put(endpointName, address);
        return this;
    }

    /**
     * Remove the address definition of the given web-service endpoint
     *
     * @param endpointName
     *     The endpoint name for which the address definition must be removed
     */
    public ProcessEngineConfiguration removeWsEndpointAddress(QName endpointName) {
        this.wsOverridenEndpointAddresses.remove(endpointName);
        return this;
    }

    public ConcurrentMap<QName, URL> getWsOverridenEndpointAddresses() {
        return this.wsOverridenEndpointAddresses;
    }

    public ProcessEngineConfiguration setWsOverridenEndpointAddresses(final ConcurrentMap<QName, URL> wsOverridenEndpointAddress) {
        this.wsOverridenEndpointAddresses.putAll(wsOverridenEndpointAddress);
        return this;
    }

    public Map<String, FormEngine> getFormEngines() {
        return formEngines;
    }

    public ProcessEngineConfigurationImpl setFormEngines(Map<String, FormEngine> formEngines) {
        this.formEngines = formEngines;
        return this;
    }

    public FormTypes getFormTypes() {
        return formTypes;
    }

    public ProcessEngineConfigurationImpl setFormTypes(FormTypes formTypes) {
        this.formTypes = formTypes;
        return this;
    }

    @Override
    public ScriptingEngines getScriptingEngines() {
        return scriptingEngines;
    }

    @Override
    public ProcessEngineConfigurationImpl setScriptingEngines(ScriptingEngines scriptingEngines) {
        this.scriptingEngines = scriptingEngines;
        return this;
    }

    @Override
    public VariableTypes getVariableTypes() {
        return variableTypes;
    }

    @Override
    public ProcessEngineConfigurationImpl setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
        return this;
    }

    public IdentityLinkServiceConfiguration getIdentityLinkServiceConfiguration() {
        return identityLinkServiceConfiguration;
    }

    public ProcessEngineConfigurationImpl setIdentityLinkServiceConfiguration(IdentityLinkServiceConfiguration identityLinkServiceConfiguration) {
        this.identityLinkServiceConfiguration = identityLinkServiceConfiguration;
        return this;
    }

    public EntityLinkServiceConfiguration getEntityLinkServiceConfiguration() {
        return entityLinkServiceConfiguration;
    }

    public ProcessEngineConfigurationImpl setEntityLinkServiceConfiguration(EntityLinkServiceConfiguration entityLinkServiceConfiguration) {
        this.entityLinkServiceConfiguration = entityLinkServiceConfiguration;
        return this;
    }

    public TaskServiceConfiguration getTaskServiceConfiguration() {
        return taskServiceConfiguration;
    }

    public ProcessEngineConfigurationImpl setTaskServiceConfiguration(TaskServiceConfiguration taskServiceConfiguration) {
        this.taskServiceConfiguration = taskServiceConfiguration;
        return this;
    }

    public ProcessEngineConfigurationImpl setVariableServiceConfiguration(VariableServiceConfiguration variableServiceConfiguration) {
        this.variableServiceConfiguration = variableServiceConfiguration;
        return this;
    }

    public InternalHistoryVariableManager getInternalHistoryVariableManager() {
        return internalHistoryVariableManager;
    }

    public ProcessEngineConfigurationImpl setInternalHistoryVariableManager(InternalHistoryVariableManager internalHistoryVariableManager) {
        this.internalHistoryVariableManager = internalHistoryVariableManager;
        return this;
    }

    public InternalTaskVariableScopeResolver getInternalTaskVariableScopeResolver() {
        return internalTaskVariableScopeResolver;
    }

    public ProcessEngineConfigurationImpl setInternalTaskVariableScopeResolver(InternalTaskVariableScopeResolver internalTaskVariableScopeResolver) {
        this.internalTaskVariableScopeResolver = internalTaskVariableScopeResolver;
        return this;
    }

    public InternalHistoryTaskManager getInternalHistoryTaskManager() {
        return internalHistoryTaskManager;
    }

    public ProcessEngineConfigurationImpl setInternalHistoryTaskManager(InternalHistoryTaskManager internalHistoryTaskManager) {
        this.internalHistoryTaskManager = internalHistoryTaskManager;
        return this;
    }

    public InternalTaskAssignmentManager getInternalTaskAssignmentManager() {
        return internalTaskAssignmentManager;
    }

    public ProcessEngineConfigurationImpl setInternalTaskAssignmentManager(InternalTaskAssignmentManager internalTaskAssignmentManager) {
        this.internalTaskAssignmentManager = internalTaskAssignmentManager;
        return this;
    }

    public IdentityLinkEventHandler getIdentityLinkEventHandler() {
        return identityLinkEventHandler;
    }

    public ProcessEngineConfigurationImpl setIdentityLinkEventHandler(IdentityLinkEventHandler identityLinkEventHandler) {
        this.identityLinkEventHandler = identityLinkEventHandler;
        return this;
    }

    public InternalTaskLocalizationManager getInternalTaskLocalizationManager() {
        return internalTaskLocalizationManager;
    }

    public ProcessEngineConfigurationImpl setInternalTaskLocalizationManager(InternalTaskLocalizationManager internalTaskLocalizationManager) {
        this.internalTaskLocalizationManager = internalTaskLocalizationManager;
        return this;
    }

    public InternalProcessLocalizationManager getInternalProcessLocalizationManager() {
        return internalProcessLocalizationManager;
    }

    public ProcessEngineConfigurationImpl setInternalProcessLocalizationManager(InternalProcessLocalizationManager internalProcessLocalizationManager) {
        this.internalProcessLocalizationManager = internalProcessLocalizationManager;
        return this;
    }

    public InternalProcessDefinitionLocalizationManager getInternalProcessDefinitionLocalizationManager() {
        return internalProcessDefinitionLocalizationManager;
    }

    public ProcessEngineConfigurationImpl setInternalProcessDefinitionLocalizationManager(InternalProcessDefinitionLocalizationManager internalProcessDefinitionLocalizationManager) {
        this.internalProcessDefinitionLocalizationManager = internalProcessDefinitionLocalizationManager;
        return this;
    }

    public InternalJobManager getInternalJobManager() {
        return internalJobManager;
    }

    public ProcessEngineConfigurationImpl setInternalJobManager(InternalJobManager internalJobManager) {
        this.internalJobManager = internalJobManager;
        return this;
    }

    public InternalJobCompatibilityManager getInternalJobCompatibilityManager() {
        return internalJobCompatibilityManager;
    }

    public ProcessEngineConfigurationImpl setInternalJobCompatibilityManager(InternalJobCompatibilityManager internalJobCompatibilityManager) {
        this.internalJobCompatibilityManager = internalJobCompatibilityManager;
        return this;
    }

    public boolean isSerializableVariableTypeTrackDeserializedObjects() {
        return serializableVariableTypeTrackDeserializedObjects;
    }

    public void setSerializableVariableTypeTrackDeserializedObjects(boolean serializableVariableTypeTrackDeserializedObjects) {
        this.serializableVariableTypeTrackDeserializedObjects = serializableVariableTypeTrackDeserializedObjects;
    }

    public boolean isJsonVariableTypeTrackObjects() {
        return jsonVariableTypeTrackObjects;
    }

    public ProcessEngineConfigurationImpl setJsonVariableTypeTrackObjects(boolean jsonVariableTypeTrackObjects) {
        this.jsonVariableTypeTrackObjects = jsonVariableTypeTrackObjects;
        return this;
    }

    public boolean isParallelMultiInstanceAsyncLeave() {
        return parallelMultiInstanceAsyncLeave;
    }

    public ProcessEngineConfigurationImpl setParallelMultiInstanceAsyncLeave(boolean parallelMultiInstanceAsyncLeave) {
        this.parallelMultiInstanceAsyncLeave = parallelMultiInstanceAsyncLeave;
        return this;
    }

    public Collection<ELResolver> getPreDefaultELResolvers() {
        return preDefaultELResolvers;
    }

    public ProcessEngineConfigurationImpl setPreDefaultELResolvers(Collection<ELResolver> preDefaultELResolvers) {
        this.preDefaultELResolvers = preDefaultELResolvers;
        return this;
    }

    public ProcessEngineConfigurationImpl addPreDefaultELResolver(ELResolver elResolver) {
        if (preDefaultELResolvers == null) {
            preDefaultELResolvers = new ArrayList<>();
        }

        preDefaultELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPreBeanELResolvers() {
        return preBeanELResolvers;
    }

    public ProcessEngineConfigurationImpl setPreBeanELResolvers(Collection<ELResolver> preBeanELResolvers) {
        this.preBeanELResolvers = preBeanELResolvers;
        return this;
    }

    public ProcessEngineConfigurationImpl addPreBeanELResolver(ELResolver elResolver) {
        if (this.preBeanELResolvers == null) {
            this.preBeanELResolvers = new ArrayList<>();
        }

        this.preBeanELResolvers.add(elResolver);
        return this;
    }

    public Collection<ELResolver> getPostDefaultELResolvers() {
        return postDefaultELResolvers;
    }

    public ProcessEngineConfigurationImpl setPostDefaultELResolvers(Collection<ELResolver> postDefaultELResolvers) {
        this.postDefaultELResolvers = postDefaultELResolvers;
        return this;
    }

    public ProcessEngineConfigurationImpl addPostDefaultELResolver(ELResolver elResolver) {
        if (this.postDefaultELResolvers == null) {
            this.postDefaultELResolvers = new ArrayList<>();
        }

        this.postDefaultELResolvers.add(elResolver);
        return this;
    }

    @Override
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    @Override
    public ProcessEngineConfigurationImpl setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public boolean isExpressionCacheEnabled() {
        return isExpressionCacheEnabled;
    }

    public ProcessEngineConfigurationImpl setExpressionCacheEnabled(boolean isExpressionCacheEnabled) {
        this.isExpressionCacheEnabled = isExpressionCacheEnabled;
        return this;
    }

    public int getExpressionCacheSize() {
        return expressionCacheSize;
    }

    public ProcessEngineConfigurationImpl setExpressionCacheSize(int expressionCacheSize) {
        this.expressionCacheSize = expressionCacheSize;
        return this;
    }

    public int getExpressionTextLengthCacheLimit() {
        return expressionTextLengthCacheLimit;
    }

    public ProcessEngineConfigurationImpl setExpressionTextLengthCacheLimit(int expressionTextLengthCacheLimit) {
        this.expressionTextLengthCacheLimit = expressionTextLengthCacheLimit;
        return this;
    }

    public BusinessCalendarManager getBusinessCalendarManager() {
        return businessCalendarManager;
    }

    public ProcessEngineConfigurationImpl setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
        this.businessCalendarManager = businessCalendarManager;
        return this;
    }

    public StartProcessInstanceInterceptor getStartProcessInstanceInterceptor() {
        return startProcessInstanceInterceptor;
    }

    public ProcessEngineConfigurationImpl setStartProcessInstanceInterceptor(StartProcessInstanceInterceptor startProcessInstanceInterceptor) {
        this.startProcessInstanceInterceptor = startProcessInstanceInterceptor;
        return this;
    }
    
    public CreateUserTaskInterceptor getCreateUserTaskInterceptor() {
        return createUserTaskInterceptor;
    }

    public ProcessEngineConfigurationImpl setCreateUserTaskInterceptor(CreateUserTaskInterceptor createUserTaskInterceptor) {
        this.createUserTaskInterceptor = createUserTaskInterceptor;
        return this;
    }

    public CreateExternalWorkerJobInterceptor getCreateExternalWorkerJobInterceptor() {
        return createExternalWorkerJobInterceptor;
    }

    public ProcessEngineConfigurationImpl setCreateExternalWorkerJobInterceptor(CreateExternalWorkerJobInterceptor createExternalWorkerJobInterceptor) {
        this.createExternalWorkerJobInterceptor = createExternalWorkerJobInterceptor;
        return this;
    }

    public ProcessInstanceQueryInterceptor getProcessInstanceQueryInterceptor() {
        return processInstanceQueryInterceptor;
    }

    public ProcessEngineConfigurationImpl setProcessInstanceQueryInterceptor(ProcessInstanceQueryInterceptor processInstanceQueryInterceptor) {
        this.processInstanceQueryInterceptor = processInstanceQueryInterceptor;
        return this;
    }

    public ExecutionQueryInterceptor getExecutionQueryInterceptor() {
        return executionQueryInterceptor;
    }

    public ProcessEngineConfigurationImpl setExecutionQueryInterceptor(ExecutionQueryInterceptor executionQueryInterceptor) {
        this.executionQueryInterceptor = executionQueryInterceptor;
        return this;
    }

    public HistoricProcessInstanceQueryInterceptor getHistoricProcessInstanceQueryInterceptor() {
        return historicProcessInstanceQueryInterceptor;
    }

    public ProcessEngineConfigurationImpl setHistoricProcessInstanceQueryInterceptor(HistoricProcessInstanceQueryInterceptor historicProcessInstanceQueryInterceptor) {
        this.historicProcessInstanceQueryInterceptor = historicProcessInstanceQueryInterceptor;
        return this;
    }

    public TaskQueryInterceptor getTaskQueryInterceptor() {
        return taskQueryInterceptor;
    }

    public ProcessEngineConfigurationImpl setTaskQueryInterceptor(TaskQueryInterceptor taskQueryInterceptor) {
        this.taskQueryInterceptor = taskQueryInterceptor;
        return this;
    }

    public HistoricTaskQueryInterceptor getHistoricTaskQueryInterceptor() {
        return historicTaskQueryInterceptor;
    }

    public ProcessEngineConfigurationImpl setHistoricTaskQueryInterceptor(HistoricTaskQueryInterceptor historicTaskQueryInterceptor) {
        this.historicTaskQueryInterceptor = historicTaskQueryInterceptor;
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setExecutionQueryLimit(int executionQueryLimit) {
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setTaskQueryLimit(int taskQueryLimit) {
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
        return this;
    }

    /**
     * @deprecated no longer needed, this is a noop
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setHistoricProcessInstancesQueryLimit(int historicProcessInstancesQueryLimit) {
        return this;
    }

    public FlowableEngineAgendaFactory getAgendaFactory() {
        return agendaFactory;
    }

    public ProcessEngineConfigurationImpl setAgendaFactory(FlowableEngineAgendaFactory agendaFactory) {
        this.agendaFactory = agendaFactory;
        return this;
    }

    public Map<String, JobHandler> getJobHandlers() {
        return jobHandlers;
    }

    public ProcessEngineConfigurationImpl setJobHandlers(Map<String, JobHandler> jobHandlers) {
        this.jobHandlers = jobHandlers;
        return this;
    }

    public Map<String, HistoryJobHandler> getHistoryJobHandlers() {
        return historyJobHandlers;
    }

    public ProcessEngineConfigurationImpl setHistoryJobHandlers(Map<String, HistoryJobHandler> historyJobHandlers) {
        this.historyJobHandlers = historyJobHandlers;
        return this;
    }

    public ProcessInstanceHelper getProcessInstanceHelper() {
        return processInstanceHelper;
    }

    public ProcessEngineConfigurationImpl setProcessInstanceHelper(ProcessInstanceHelper processInstanceHelper) {
        this.processInstanceHelper = processInstanceHelper;
        return this;
    }

    public ListenerNotificationHelper getListenerNotificationHelper() {
        return listenerNotificationHelper;
    }

    public ProcessEngineConfigurationImpl setListenerNotificationHelper(ListenerNotificationHelper listenerNotificationHelper) {
        this.listenerNotificationHelper = listenerNotificationHelper;
        return this;
    }

    public FormHandlerHelper getFormHandlerHelper() {
        return formHandlerHelper;
    }

    public ProcessEngineConfigurationImpl setFormHandlerHelper(FormHandlerHelper formHandlerHelper) {
        this.formHandlerHelper = formHandlerHelper;
        return this;
    }

    public CaseInstanceService getCaseInstanceService() {
        return caseInstanceService;
    }

    public ProcessEngineConfigurationImpl setCaseInstanceService(CaseInstanceService caseInstanceService) {
        this.caseInstanceService = caseInstanceService;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setSqlSessionFactory(SqlSessionFactory sqlSessionFactory) {
        this.sqlSessionFactory = sqlSessionFactory;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setTransactionFactory(TransactionFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl addCustomSessionFactory(SessionFactory sessionFactory) {
        super.addCustomSessionFactory(sessionFactory);
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setCustomSessionFactories(List<SessionFactory> customSessionFactories) {
        this.customSessionFactories = customSessionFactories;
        return this;
    }

    public List<JobHandler> getCustomJobHandlers() {
        return customJobHandlers;
    }

    public ProcessEngineConfigurationImpl setCustomJobHandlers(List<JobHandler> customJobHandlers) {
        this.customJobHandlers = customJobHandlers;
        return this;
    }

    public ProcessEngineConfigurationImpl addCustomJobHandler(JobHandler customJobHandler) {
        if (this.customJobHandlers == null) {
            this.customJobHandlers = new ArrayList<>();
        }
        this.customJobHandlers.add(customJobHandler);
        return this;
    }

    public List<HistoryJobHandler> getCustomHistoryJobHandlers() {
        return customHistoryJobHandlers;
    }

    public ProcessEngineConfigurationImpl setCustomHistoryJobHandlers(List<HistoryJobHandler> customHistoryJobHandlers) {
        this.customHistoryJobHandlers = customHistoryJobHandlers;
        return this;
    }

    public List<HistoryJsonTransformer> getCustomHistoryJsonTransformers() {
        return customHistoryJsonTransformers;
    }

    public ProcessEngineConfigurationImpl setCustomHistoryJsonTransformers(List<HistoryJsonTransformer> customHistoryJsonTransformers) {
        this.customHistoryJsonTransformers = customHistoryJsonTransformers;
        return this;
    }

    public List<FormEngine> getCustomFormEngines() {
        return customFormEngines;
    }

    public ProcessEngineConfigurationImpl setCustomFormEngines(List<FormEngine> customFormEngines) {
        this.customFormEngines = customFormEngines;
        return this;
    }

    public List<AbstractFormType> getCustomFormTypes() {
        return customFormTypes;
    }

    public ProcessEngineConfigurationImpl setCustomFormTypes(List<AbstractFormType> customFormTypes) {
        this.customFormTypes = customFormTypes;
        return this;
    }

    public List<String> getCustomScriptingEngineClasses() {
        return customScriptingEngineClasses;
    }

    public ProcessEngineConfigurationImpl setCustomScriptingEngineClasses(List<String> customScriptingEngineClasses) {
        this.customScriptingEngineClasses = customScriptingEngineClasses;
        return this;
    }

    public List<VariableType> getCustomPreVariableTypes() {
        return customPreVariableTypes;
    }

    public ProcessEngineConfigurationImpl setCustomPreVariableTypes(List<VariableType> customPreVariableTypes) {
        this.customPreVariableTypes = customPreVariableTypes;
        return this;
    }

    public List<VariableType> getCustomPostVariableTypes() {
        return customPostVariableTypes;
    }

    public ProcessEngineConfigurationImpl setCustomPostVariableTypes(List<VariableType> customPostVariableTypes) {
        this.customPostVariableTypes = customPostVariableTypes;
        return this;
    }

    public List<BpmnParseHandler> getPreBpmnParseHandlers() {
        return preBpmnParseHandlers;
    }

    public ProcessEngineConfigurationImpl setPreBpmnParseHandlers(List<BpmnParseHandler> preBpmnParseHandlers) {
        this.preBpmnParseHandlers = preBpmnParseHandlers;
        return this;
    }

    public List<BpmnParseHandler> getCustomDefaultBpmnParseHandlers() {
        return customDefaultBpmnParseHandlers;
    }

    public ProcessEngineConfigurationImpl setCustomDefaultBpmnParseHandlers(List<BpmnParseHandler> customDefaultBpmnParseHandlers) {
        this.customDefaultBpmnParseHandlers = customDefaultBpmnParseHandlers;
        return this;
    }

    public List<BpmnParseHandler> getPostBpmnParseHandlers() {
        return postBpmnParseHandlers;
    }

    public ProcessEngineConfigurationImpl setPostBpmnParseHandlers(List<BpmnParseHandler> postBpmnParseHandlers) {
        this.postBpmnParseHandlers = postBpmnParseHandlers;
        return this;
    }

    public ActivityBehaviorFactory getActivityBehaviorFactory() {
        return activityBehaviorFactory;
    }

    public ProcessEngineConfigurationImpl setActivityBehaviorFactory(ActivityBehaviorFactory activityBehaviorFactory) {
        this.activityBehaviorFactory = activityBehaviorFactory;
        return this;
    }

    public ListenerFactory getListenerFactory() {
        return listenerFactory;
    }

    public ProcessEngineConfigurationImpl setListenerFactory(ListenerFactory listenerFactory) {
        this.listenerFactory = listenerFactory;
        return this;
    }

    public BpmnParseFactory getBpmnParseFactory() {
        return bpmnParseFactory;
    }

    public ProcessEngineConfigurationImpl setBpmnParseFactory(BpmnParseFactory bpmnParseFactory) {
        this.bpmnParseFactory = bpmnParseFactory;
        return this;
    }

    public List<ResolverFactory> getResolverFactories() {
        return resolverFactories;
    }

    public ProcessEngineConfigurationImpl setResolverFactories(List<ResolverFactory> resolverFactories) {
        this.resolverFactories = resolverFactories;
        return this;
    }

    public DeploymentManager getDeploymentManager() {
        return deploymentManager;
    }

    public ProcessEngineConfigurationImpl setDeploymentManager(DeploymentManager deploymentManager) {
        this.deploymentManager = deploymentManager;
        return this;
    }

    public ProcessEngineConfigurationImpl setDelegateInterceptor(DelegateInterceptor delegateInterceptor) {
        this.delegateInterceptor = delegateInterceptor;
        return this;
    }

    public DelegateInterceptor getDelegateInterceptor() {
        return delegateInterceptor;
    }

    public EventHandler getEventHandler(String eventType) {
        return eventHandlers.get(eventType);
    }

    public ProcessEngineConfigurationImpl setEventHandlers(Map<String, EventHandler> eventHandlers) {
        this.eventHandlers = eventHandlers;
        return this;
    }

    public Map<String, EventHandler> getEventHandlers() {
        return eventHandlers;
    }

    public List<EventHandler> getCustomEventHandlers() {
        return customEventHandlers;
    }

    public ProcessEngineConfigurationImpl setCustomEventHandlers(List<EventHandler> customEventHandlers) {
        this.customEventHandlers = customEventHandlers;
        return this;
    }

    public FailedJobCommandFactory getFailedJobCommandFactory() {
        return failedJobCommandFactory;
    }

    public ProcessEngineConfigurationImpl setFailedJobCommandFactory(FailedJobCommandFactory failedJobCommandFactory) {
        this.failedJobCommandFactory = failedJobCommandFactory;
        return this;
    }

    public int getBatchSizeProcessInstances() {
        return batchSizeProcessInstances;
    }

    public ProcessEngineConfigurationImpl setBatchSizeProcessInstances(int batchSizeProcessInstances) {
        this.batchSizeProcessInstances = batchSizeProcessInstances;
        return this;
    }

    public int getBatchSizeTasks() {
        return batchSizeTasks;
    }

    public ProcessEngineConfigurationImpl setBatchSizeTasks(int batchSizeTasks) {
        this.batchSizeTasks = batchSizeTasks;
        return this;
    }

    public int getProcessDefinitionCacheLimit() {
        return processDefinitionCacheLimit;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionCacheLimit(int processDefinitionCacheLimit) {
        this.processDefinitionCacheLimit = processDefinitionCacheLimit;
        return this;
    }

    public DeploymentCache<ProcessDefinitionCacheEntry> getProcessDefinitionCache() {
        return processDefinitionCache;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionCache(DeploymentCache<ProcessDefinitionCacheEntry> processDefinitionCache) {
        this.processDefinitionCache = processDefinitionCache;
        return this;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionInfoCache(DeploymentCache<ProcessDefinitionInfoCacheObject> processDefinitionInfoCache){
        this.processDefinitionInfoCache = processDefinitionInfoCache;
        return this;
    }

    public DeploymentCache<ProcessDefinitionInfoCacheObject> getProcessDefinitionInfoCache() {
        return processDefinitionInfoCache;
    }

    public int getKnowledgeBaseCacheLimit() {
        return knowledgeBaseCacheLimit;
    }

    public ProcessEngineConfigurationImpl setKnowledgeBaseCacheLimit(int knowledgeBaseCacheLimit) {
        this.knowledgeBaseCacheLimit = knowledgeBaseCacheLimit;
        return this;
    }

    public DeploymentCache<Object> getKnowledgeBaseCache() {
        return knowledgeBaseCache;
    }

    public ProcessEngineConfigurationImpl setKnowledgeBaseCache(DeploymentCache<Object> knowledgeBaseCache) {
        this.knowledgeBaseCache = knowledgeBaseCache;
        return this;
    }

    public DeploymentCache<Object> getAppResourceCache() {
        return appResourceCache;
    }

    public ProcessEngineConfigurationImpl setAppResourceCache(DeploymentCache<Object> appResourceCache) {
        this.appResourceCache = appResourceCache;
        return this;
    }

    public int getAppResourceCacheLimit() {
        return appResourceCacheLimit;
    }

    public ProcessEngineConfigurationImpl setAppResourceCacheLimit(int appResourceCacheLimit) {
        this.appResourceCacheLimit = appResourceCacheLimit;
        return this;
    }

    public AppResourceConverter getAppResourceConverter() {
        return appResourceConverter;
    }

    public ProcessEngineConfigurationImpl setAppResourceConverter(AppResourceConverter appResourceConverter) {
        this.appResourceConverter = appResourceConverter;
        return this;
    }

    public boolean isEnableSafeBpmnXml() {
        return enableSafeBpmnXml;
    }

    public ProcessEngineConfigurationImpl setEnableSafeBpmnXml(boolean enableSafeBpmnXml) {
        this.enableSafeBpmnXml = enableSafeBpmnXml;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setEventDispatcher(FlowableEventDispatcher eventDispatcher) {
        this.eventDispatcher = eventDispatcher;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setEnableEventDispatcher(boolean enableEventDispatcher) {
        this.enableEventDispatcher = enableEventDispatcher;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setTypedEventListeners(Map<String, List<FlowableEventListener>> typedListeners) {
        this.typedEventListeners = typedListeners;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setEventListeners(List<FlowableEventListener> eventListeners) {
        this.eventListeners = eventListeners;
        return this;
    }

    public ProcessValidator getProcessValidator() {
        return processValidator;
    }

    public ProcessEngineConfigurationImpl setProcessValidator(ProcessValidator processValidator) {
        this.processValidator = processValidator;
        return this;
    }

    public ServiceTaskValidator getCustomServiceTaskValidator() {
        return customServiceTaskValidator;
    }

    public ProcessEngineConfigurationImpl setCustomServiceTaskValidator(ServiceTaskValidator customServiceTaskValidator) {
        this.customServiceTaskValidator = customServiceTaskValidator;
        return this;
    }

    public FormFieldHandler getFormFieldHandler() {
        return formFieldHandler;
    }

    public ProcessEngineConfigurationImpl setFormFieldHandler(FormFieldHandler formFieldHandler) {
        this.formFieldHandler = formFieldHandler;
        return this;
    }

    public boolean isFormFieldValidationEnabled() {
        return this.isFormFieldValidationEnabled;
    }

    public ProcessEngineConfigurationImpl setFormFieldValidationEnabled(boolean flag) {
        this.isFormFieldValidationEnabled = flag;
        return this;
    }

    public EventRegistryEventConsumer getEventRegistryEventConsumer() {
        return eventRegistryEventConsumer;
    }

    public ProcessEngineConfigurationImpl setEventRegistryEventConsumer(EventRegistryEventConsumer eventRegistryEventConsumer) {
        this.eventRegistryEventConsumer = eventRegistryEventConsumer;
        return this;
    }

    public boolean isEventRegistryStartProcessInstanceAsync() {
        return eventRegistryStartProcessInstanceAsync;
    }

    public ProcessEngineConfigurationImpl setEventRegistryStartProcessInstanceAsync(boolean eventRegistryStartProcessInstanceAsync) {
        this.eventRegistryStartProcessInstanceAsync = eventRegistryStartProcessInstanceAsync;
        return this;
    }

    public List<FlowableFunctionDelegate> getFlowableFunctionDelegates() {
        return flowableFunctionDelegates;
    }

    public ProcessEngineConfigurationImpl setFlowableFunctionDelegates(List<FlowableFunctionDelegate> flowableFunctionDelegates) {
        this.flowableFunctionDelegates = flowableFunctionDelegates;
        return this;
    }

    public List<FlowableFunctionDelegate> getCustomFlowableFunctionDelegates() {
        return customFlowableFunctionDelegates;
    }

    public ProcessEngineConfigurationImpl setCustomFlowableFunctionDelegates(List<FlowableFunctionDelegate> customFlowableFunctionDelegates) {
        this.customFlowableFunctionDelegates = customFlowableFunctionDelegates;
        return this;
    }

    public List<FlowableAstFunctionCreator> getAstFunctionCreators() {
        return astFunctionCreators;
    }

    public void setAstFunctionCreators(List<FlowableAstFunctionCreator> astFunctionCreators) {
        this.astFunctionCreators = astFunctionCreators;
    }

    public boolean isEnableDatabaseEventLogging() {
        return enableDatabaseEventLogging;
    }

    public ProcessEngineConfigurationImpl setEnableDatabaseEventLogging(boolean enableDatabaseEventLogging) {
        this.enableDatabaseEventLogging = enableDatabaseEventLogging;
        return this;
    }

    public boolean isEnableHistoricTaskLogging() {
        return enableHistoricTaskLogging;
    }

    public ProcessEngineConfigurationImpl setEnableHistoricTaskLogging(boolean enableHistoricTaskLogging) {
        this.enableHistoricTaskLogging = enableHistoricTaskLogging;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setUsingRelationalDatabase(boolean usingRelationalDatabase) {
        this.usingRelationalDatabase = usingRelationalDatabase;
        return this;
    }

    public boolean isEnableVerboseExecutionTreeLogging() {
        return enableVerboseExecutionTreeLogging;
    }

    public ProcessEngineConfigurationImpl setEnableVerboseExecutionTreeLogging(boolean enableVerboseExecutionTreeLogging) {
        this.enableVerboseExecutionTreeLogging = enableVerboseExecutionTreeLogging;
        return this;
    }

    public ProcessEngineConfigurationImpl setEnableEagerExecutionTreeFetching(boolean enableEagerExecutionTreeFetching) {
        this.performanceSettings.setEnableEagerExecutionTreeFetching(enableEagerExecutionTreeFetching);
        return this;
    }

    public ProcessEngineConfigurationImpl setEnableExecutionRelationshipCounts(boolean enableExecutionRelationshipCounts) {
        this.performanceSettings.setEnableExecutionRelationshipCounts(enableExecutionRelationshipCounts);
        return this;
    }

    public ProcessEngineConfigurationImpl setEnableTaskRelationshipCounts(boolean enableTaskRelationshipCounts) {
        this.performanceSettings.setEnableTaskRelationshipCounts(enableTaskRelationshipCounts);
        return this;
    }

    public PerformanceSettings getPerformanceSettings() {
        return performanceSettings;
    }

    public void setPerformanceSettings(PerformanceSettings performanceSettings) {
        this.performanceSettings = performanceSettings;
    }

    public ProcessEngineConfigurationImpl setEnableLocalization(boolean enableLocalization) {
        this.performanceSettings.setEnableLocalization(enableLocalization);
        return this;
    }

    public AttachmentDataManager getAttachmentDataManager() {
        return attachmentDataManager;
    }

    public ProcessEngineConfigurationImpl setAttachmentDataManager(AttachmentDataManager attachmentDataManager) {
        this.attachmentDataManager = attachmentDataManager;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setByteArrayDataManager(ByteArrayDataManager byteArrayDataManager) {
        this.byteArrayDataManager = byteArrayDataManager;
        return this;
    }

    public CommentDataManager getCommentDataManager() {
        return commentDataManager;
    }

    public ProcessEngineConfigurationImpl setCommentDataManager(CommentDataManager commentDataManager) {
        this.commentDataManager = commentDataManager;
        return this;
    }

    public DeploymentDataManager getDeploymentDataManager() {
        return deploymentDataManager;
    }

    public ProcessEngineConfigurationImpl setDeploymentDataManager(DeploymentDataManager deploymentDataManager) {
        this.deploymentDataManager = deploymentDataManager;
        return this;
    }

    public EventLogEntryDataManager getEventLogEntryDataManager() {
        return eventLogEntryDataManager;
    }

    public ProcessEngineConfigurationImpl setEventLogEntryDataManager(EventLogEntryDataManager eventLogEntryDataManager) {
        this.eventLogEntryDataManager = eventLogEntryDataManager;
        return this;
    }

    public ExecutionDataManager getExecutionDataManager() {
        return executionDataManager;
    }

    public ProcessEngineConfigurationImpl setExecutionDataManager(ExecutionDataManager executionDataManager) {
        this.executionDataManager = executionDataManager;
        return this;
    }

    public ActivityInstanceDataManager getActivityInstanceDataManager() {
        return activityInstanceDataManager;
    }

    public ProcessEngineConfigurationImpl setActivityInstanceDataManager(ActivityInstanceDataManager activityInstanceDataManager) {
        this.activityInstanceDataManager = activityInstanceDataManager;
        return this;
    }

    public HistoricActivityInstanceDataManager getHistoricActivityInstanceDataManager() {
        return historicActivityInstanceDataManager;
    }

    public ProcessEngineConfigurationImpl setHistoricActivityInstanceDataManager(HistoricActivityInstanceDataManager historicActivityInstanceDataManager) {
        this.historicActivityInstanceDataManager = historicActivityInstanceDataManager;
        return this;
    }

    public HistoricDetailDataManager getHistoricDetailDataManager() {
        return historicDetailDataManager;
    }

    public ProcessEngineConfigurationImpl setHistoricDetailDataManager(HistoricDetailDataManager historicDetailDataManager) {
        this.historicDetailDataManager = historicDetailDataManager;
        return this;
    }

    public HistoricProcessInstanceDataManager getHistoricProcessInstanceDataManager() {
        return historicProcessInstanceDataManager;
    }

    public ProcessEngineConfigurationImpl setHistoricProcessInstanceDataManager(HistoricProcessInstanceDataManager historicProcessInstanceDataManager) {
        this.historicProcessInstanceDataManager = historicProcessInstanceDataManager;
        return this;
    }

    public ModelDataManager getModelDataManager() {
        return modelDataManager;
    }

    public ProcessEngineConfigurationImpl setModelDataManager(ModelDataManager modelDataManager) {
        this.modelDataManager = modelDataManager;
        return this;
    }

    public ProcessDefinitionDataManager getProcessDefinitionDataManager() {
        return processDefinitionDataManager;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionDataManager(ProcessDefinitionDataManager processDefinitionDataManager) {
        this.processDefinitionDataManager = processDefinitionDataManager;
        return this;
    }

    public ProcessDefinitionInfoDataManager getProcessDefinitionInfoDataManager() {
        return processDefinitionInfoDataManager;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionInfoDataManager(ProcessDefinitionInfoDataManager processDefinitionInfoDataManager) {
        this.processDefinitionInfoDataManager = processDefinitionInfoDataManager;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setPropertyDataManager(PropertyDataManager propertyDataManager) {
        this.propertyDataManager = propertyDataManager;
        return this;
    }

    public ResourceDataManager getResourceDataManager() {
        return resourceDataManager;
    }

    public ProcessEngineConfigurationImpl setResourceDataManager(ResourceDataManager resourceDataManager) {
        this.resourceDataManager = resourceDataManager;
        return this;
    }

    public AttachmentEntityManager getAttachmentEntityManager() {
        return attachmentEntityManager;
    }

    public ProcessEngineConfigurationImpl setAttachmentEntityManager(AttachmentEntityManager attachmentEntityManager) {
        this.attachmentEntityManager = attachmentEntityManager;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setByteArrayEntityManager(ByteArrayEntityManager byteArrayEntityManager) {
        this.byteArrayEntityManager = byteArrayEntityManager;
        return this;
    }

    public CommentEntityManager getCommentEntityManager() {
        return commentEntityManager;
    }

    public ProcessEngineConfigurationImpl setCommentEntityManager(CommentEntityManager commentEntityManager) {
        this.commentEntityManager = commentEntityManager;
        return this;
    }

    public DeploymentEntityManager getDeploymentEntityManager() {
        return deploymentEntityManager;
    }

    public ProcessEngineConfigurationImpl setDeploymentEntityManager(DeploymentEntityManager deploymentEntityManager) {
        this.deploymentEntityManager = deploymentEntityManager;
        return this;
    }

    public EventLogEntryEntityManager getEventLogEntryEntityManager() {
        return eventLogEntryEntityManager;
    }

    public ProcessEngineConfigurationImpl setEventLogEntryEntityManager(EventLogEntryEntityManager eventLogEntryEntityManager) {
        this.eventLogEntryEntityManager = eventLogEntryEntityManager;
        return this;
    }

    public ExecutionEntityManager getExecutionEntityManager() {
        return executionEntityManager;
    }

    public ProcessEngineConfigurationImpl setExecutionEntityManager(ExecutionEntityManager executionEntityManager) {
        this.executionEntityManager = executionEntityManager;
        return this;
    }

    public ActivityInstanceEntityManager getActivityInstanceEntityManager() {
        return activityInstanceEntityManager;
    }

    public ProcessEngineConfigurationImpl setActivityInstanceEntityManager(ActivityInstanceEntityManager activityInstanceEntityManager) {
        this.activityInstanceEntityManager = activityInstanceEntityManager;
        return this;
    }

    public HistoricActivityInstanceEntityManager getHistoricActivityInstanceEntityManager() {
        return historicActivityInstanceEntityManager;
    }

    public ProcessEngineConfigurationImpl setHistoricActivityInstanceEntityManager(HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager) {
        this.historicActivityInstanceEntityManager = historicActivityInstanceEntityManager;
        return this;
    }

    public HistoricDetailEntityManager getHistoricDetailEntityManager() {
        return historicDetailEntityManager;
    }

    public ProcessEngineConfigurationImpl setHistoricDetailEntityManager(HistoricDetailEntityManager historicDetailEntityManager) {
        this.historicDetailEntityManager = historicDetailEntityManager;
        return this;
    }

    public HistoricProcessInstanceEntityManager getHistoricProcessInstanceEntityManager() {
        return historicProcessInstanceEntityManager;
    }

    public ProcessEngineConfigurationImpl setHistoricProcessInstanceEntityManager(HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager) {
        this.historicProcessInstanceEntityManager = historicProcessInstanceEntityManager;
        return this;
    }

    public ModelEntityManager getModelEntityManager() {
        return modelEntityManager;
    }

    public ProcessEngineConfigurationImpl setModelEntityManager(ModelEntityManager modelEntityManager) {
        this.modelEntityManager = modelEntityManager;
        return this;
    }

    public ProcessDefinitionEntityManager getProcessDefinitionEntityManager() {
        return processDefinitionEntityManager;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionEntityManager(ProcessDefinitionEntityManager processDefinitionEntityManager) {
        this.processDefinitionEntityManager = processDefinitionEntityManager;
        return this;
    }

    public ProcessDefinitionInfoEntityManager getProcessDefinitionInfoEntityManager() {
        return processDefinitionInfoEntityManager;
    }

    public ProcessEngineConfigurationImpl setProcessDefinitionInfoEntityManager(ProcessDefinitionInfoEntityManager processDefinitionInfoEntityManager) {
        this.processDefinitionInfoEntityManager = processDefinitionInfoEntityManager;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setPropertyEntityManager(PropertyEntityManager propertyEntityManager) {
        this.propertyEntityManager = propertyEntityManager;
        return this;
    }

    public ResourceEntityManager getResourceEntityManager() {
        return resourceEntityManager;
    }

    public ProcessEngineConfigurationImpl setResourceEntityManager(ResourceEntityManager resourceEntityManager) {
        this.resourceEntityManager = resourceEntityManager;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setTableDataManager(TableDataManager tableDataManager) {
        this.tableDataManager = tableDataManager;
        return this;
    }

    public CandidateManager getCandidateManager() {
        return candidateManager;
    }

    public void setCandidateManager(CandidateManager candidateManager) {
        this.candidateManager = candidateManager;
    }

    public List<AsyncRunnableExecutionExceptionHandler> getCustomAsyncRunnableExecutionExceptionHandlers() {
        return customAsyncRunnableExecutionExceptionHandlers;
    }

    public ProcessEngineConfigurationImpl setCustomAsyncRunnableExecutionExceptionHandlers(
        List<AsyncRunnableExecutionExceptionHandler> customAsyncRunnableExecutionExceptionHandlers) {

        this.customAsyncRunnableExecutionExceptionHandlers = customAsyncRunnableExecutionExceptionHandlers;
        return this;
    }

    public boolean isAddDefaultExceptionHandler() {
        return addDefaultExceptionHandler;
    }

    public ProcessEngineConfigurationImpl setAddDefaultExceptionHandler(boolean addDefaultExceptionHandler) {
        this.addDefaultExceptionHandler = addDefaultExceptionHandler;
        return this;
    }

    public HistoryManager getHistoryManager() {
        return historyManager;
    }

    public ProcessEngineConfigurationImpl setHistoryManager(HistoryManager historyManager) {
        this.historyManager = historyManager;
        return this;
    }

    public boolean isAsyncHistoryEnabled() {
        return isAsyncHistoryEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryEnabled(boolean isAsyncHistoryEnabled) {
        this.isAsyncHistoryEnabled = isAsyncHistoryEnabled;
        return this;
    }

    public boolean isAsyncHistoryJsonGzipCompressionEnabled() {
        return isAsyncHistoryJsonGzipCompressionEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryJsonGzipCompressionEnabled(boolean isAsyncHistoryJsonGzipCompressionEnabled) {
        this.isAsyncHistoryJsonGzipCompressionEnabled = isAsyncHistoryJsonGzipCompressionEnabled;
        return this;
    }

    public boolean isAsyncHistoryJsonGroupingEnabled() {
        return isAsyncHistoryJsonGroupingEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryJsonGroupingEnabled(boolean isAsyncHistoryJsonGroupingEnabled) {
        this.isAsyncHistoryJsonGroupingEnabled = isAsyncHistoryJsonGroupingEnabled;
        return this;
    }

    public int getAsyncHistoryJsonGroupingThreshold() {
        return asyncHistoryJsonGroupingThreshold;
    }

    public void setAsyncHistoryJsonGroupingThreshold(int asyncHistoryJsonGroupingThreshold) {
        this.asyncHistoryJsonGroupingThreshold = asyncHistoryJsonGroupingThreshold;
    }

    public AsyncHistoryListener getAsyncHistoryListener() {
        if (asyncHistoryListener == null) {
            asyncHistoryListener = new DefaultAsyncHistoryJobProducer();
        }
        return asyncHistoryListener;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryListener(AsyncHistoryListener asyncHistoryListener) {
        this.asyncHistoryListener = asyncHistoryListener;
        return this;
    }

    public JobManager getJobManager() {
        return jobManager;
    }

    public ProcessEngineConfigurationImpl setJobManager(JobManager jobManager) {
        this.jobManager = jobManager;
        return this;
    }

    public ChangeTenantIdManager getChangeTenantIdManager() {
        return changeTenantIdManager;
    }

    public ProcessEngineConfigurationImpl setChangeTenantIdManager(ChangeTenantIdManager changeTenantIdManager) {
        this.changeTenantIdManager = changeTenantIdManager;
        return this;
    }

    public Set<String> getChangeTenantEntityTypes() {
        return changeTenantEntityTypes;
    }

    public ProcessEngineConfigurationImpl setChangeTenantEntityTypes(Set<String> changeTenantEntityTypes) {
        this.changeTenantEntityTypes = changeTenantEntityTypes;
        return this;
    }

    public DynamicStateManager getDynamicStateManager() {
        return dynamicStateManager;
    }

    public ProcessEngineConfigurationImpl setDynamicStateManager(DynamicStateManager dynamicStateManager) {
        this.dynamicStateManager = dynamicStateManager;
        return this;
    }

    public ProcessInstanceMigrationManager getProcessInstanceMigrationManager() {
        return processInstanceMigrationManager;
    }

    public ProcessEngineConfigurationImpl setProcessInstanceMigrationManager(ProcessInstanceMigrationManager processInstanceMigrationValidationMananger) {
        this.processInstanceMigrationManager = processInstanceMigrationValidationMananger;
        return this;
    }
    
    public DecisionTableVariableManager getDecisionTableVariableManager() {
        return decisionTableVariableManager;
    }

    public ProcessEngineConfigurationImpl setDecisionTableVariableManager(DecisionTableVariableManager decisionTableVariableManager) {
        this.decisionTableVariableManager = decisionTableVariableManager;
        return this;
    }

    public IdentityLinkInterceptor getIdentityLinkInterceptor() {
        return identityLinkInterceptor;
    }

    public ProcessEngineConfigurationImpl setIdentityLinkInterceptor(IdentityLinkInterceptor identityLinkInterceptor) {
        this.identityLinkInterceptor = identityLinkInterceptor;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setClock(Clock clock) {
        if (this.clock == null) {
            this.clock = clock;
        } else {
            this.clock.setCurrentCalendar(clock.getCurrentCalendar());
        }

        if (flowable5CompatibilityEnabled && flowable5CompatibilityHandler != null) {
            getFlowable5CompatibilityHandler().setClock(clock);
        }
        return this;
    }

    public void resetClock() {
        if (this.clock != null) {
            clock.reset();
            if (flowable5CompatibilityEnabled && flowable5CompatibilityHandler != null) {
                getFlowable5CompatibilityHandler().resetClock();
            }
        }
    }

    public DelegateExpressionFieldInjectionMode getDelegateExpressionFieldInjectionMode() {
        return delegateExpressionFieldInjectionMode;
    }

    public ProcessEngineConfigurationImpl setDelegateExpressionFieldInjectionMode(DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode) {
        this.delegateExpressionFieldInjectionMode = delegateExpressionFieldInjectionMode;
        return this;
    }

    public List<Object> getFlowable5JobProcessors() {
        return flowable5JobProcessors;
    }

    public ProcessEngineConfigurationImpl setFlowable5JobProcessors(List<Object> jobProcessors) {
        this.flowable5JobProcessors = jobProcessors;
        return this;
    }

    public List<JobProcessor> getJobProcessors() {
        return jobProcessors;
    }

    public ProcessEngineConfigurationImpl setJobProcessors(List<JobProcessor> jobProcessors) {
        this.jobProcessors = jobProcessors;
        return this;
    }

    public List<HistoryJobProcessor> getHistoryJobProcessors() {
        return historyJobProcessors;
    }

    public ProcessEngineConfigurationImpl setHistoryJobProcessors(List<HistoryJobProcessor> historyJobProcessors) {
        this.historyJobProcessors = historyJobProcessors;
        return this;
    }

    public Map<String, List<RuntimeInstanceStateChangeCallback>> getProcessInstanceStateChangedCallbacks() {
        return processInstanceStateChangedCallbacks;
    }

    public ProcessEngineConfigurationImpl setProcessInstanceStateChangedCallbacks(Map<String, List<RuntimeInstanceStateChangeCallback>> processInstanceStateChangedCallbacks) {
        this.processInstanceStateChangedCallbacks = processInstanceStateChangedCallbacks;
        return this;
    }
    
    public List<ProcessInstanceMigrationCallback> getProcessInstanceMigrationCallbacks() {
        return processInstanceMigrationCallbacks;
    }

    public ProcessEngineConfigurationImpl setProcessInstanceMigrationCallbacks(List<ProcessInstanceMigrationCallback> processInstanceMigrationCallbacks) {
        this.processInstanceMigrationCallbacks = processInstanceMigrationCallbacks;
        return this;
    }

    public SchemaManager getVariableSchemaManager() {
        return variableSchemaManager;
    }

    public ProcessEngineConfigurationImpl setVariableSchemaManager(SchemaManager variableSchemaManager) {
        this.variableSchemaManager = variableSchemaManager;
        return this;
    }

    public SchemaManager getTaskSchemaManager() {
        return taskSchemaManager;
    }

    public ProcessEngineConfigurationImpl setTaskSchemaManager(SchemaManager taskSchemaManager) {
        this.taskSchemaManager = taskSchemaManager;
        return this;
    }

    public SchemaManager getIdentityLinkSchemaManager() {
        return identityLinkSchemaManager;
    }

    public ProcessEngineConfigurationImpl setIdentityLinkSchemaManager(SchemaManager identityLinkSchemaManager) {
        this.identityLinkSchemaManager = identityLinkSchemaManager;
        return this;
    }

    public SchemaManager getEntityLinkSchemaManager() {
        return entityLinkSchemaManager;
    }

    public ProcessEngineConfigurationImpl setEntityLinkSchemaManager(SchemaManager entityLinkSchemaManager) {
        this.entityLinkSchemaManager = entityLinkSchemaManager;
        return this;
    }

    public SchemaManager getEventSubscriptionSchemaManager() {
        return eventSubscriptionSchemaManager;
    }

    public ProcessEngineConfigurationImpl setEventSubscriptionSchemaManager(SchemaManager eventSubscriptionSchemaManager) {
        this.eventSubscriptionSchemaManager = eventSubscriptionSchemaManager;
        return this;
    }

    public SchemaManager getJobSchemaManager() {
        return jobSchemaManager;
    }

    public ProcessEngineConfigurationImpl setJobSchemaManager(SchemaManager jobSchemaManager) {
        this.jobSchemaManager = jobSchemaManager;
        return this;
    }
    
    public SchemaManager getBatchSchemaManager() {
        return batchSchemaManager;
    }

    public ProcessEngineConfigurationImpl setBatchSchemaManager(SchemaManager batchSchemaManager) {
        this.batchSchemaManager = batchSchemaManager;
        return this;
    }

    public boolean isEnableEntityLinks() {
        return enableEntityLinks;
    }

    public ProcessEngineConfigurationImpl setEnableEntityLinks(boolean enableEntityLinks) {
        this.enableEntityLinks = enableEntityLinks;
        return this;
    }

    public VariableAggregator getVariableAggregator() {
        return variableAggregator;
    }

    public ProcessEngineConfigurationImpl setVariableAggregator(VariableAggregator variableAggregator) {
        this.variableAggregator = variableAggregator;
        return this;
    }

    public boolean isHandleProcessEngineExecutorsAfterEngineCreate() {
        return handleProcessEngineExecutorsAfterEngineCreate;
    }

    public void setHandleProcessEngineExecutorsAfterEngineCreate(boolean handleProcessEngineExecutorsAfterEngineCreate) {
        this.handleProcessEngineExecutorsAfterEngineCreate = handleProcessEngineExecutorsAfterEngineCreate;
    }

    // Flowable 5

    public boolean isFlowable5CompatibilityEnabled() {
        return flowable5CompatibilityEnabled;
    }

    public ProcessEngineConfigurationImpl setFlowable5CompatibilityEnabled(boolean flowable5CompatibilityEnabled) {
        this.flowable5CompatibilityEnabled = flowable5CompatibilityEnabled;
        return this;
    }

    public boolean isValidateFlowable5EntitiesEnabled() {
        return validateFlowable5EntitiesEnabled;
    }

    public ProcessEngineConfigurationImpl setValidateFlowable5EntitiesEnabled(boolean validateFlowable5EntitiesEnabled) {
        this.validateFlowable5EntitiesEnabled = validateFlowable5EntitiesEnabled;
        return this;
    }

    public boolean isRedeployFlowable5ProcessDefinitions() {
        return redeployFlowable5ProcessDefinitions;
    }

    public ProcessEngineConfigurationImpl setRedeployFlowable5ProcessDefinitions(boolean redeployFlowable5ProcessDefinitions) {
        this.redeployFlowable5ProcessDefinitions = redeployFlowable5ProcessDefinitions;
        return this;
    }

    public Flowable5CompatibilityHandlerFactory getFlowable5CompatibilityHandlerFactory() {
        return flowable5CompatibilityHandlerFactory;
    }

    public ProcessEngineConfigurationImpl setFlowable5CompatibilityHandlerFactory(Flowable5CompatibilityHandlerFactory flowable5CompatibilityHandlerFactory) {
        this.flowable5CompatibilityHandlerFactory = flowable5CompatibilityHandlerFactory;
        return this;
    }

    public Flowable5CompatibilityHandler getFlowable5CompatibilityHandler() {
        return flowable5CompatibilityHandler;
    }

    public ProcessEngineConfigurationImpl setFlowable5CompatibilityHandler(Flowable5CompatibilityHandler flowable5CompatibilityHandler) {
        this.flowable5CompatibilityHandler = flowable5CompatibilityHandler;
        return this;
    }

    public Object getFlowable5ActivityBehaviorFactory() {
        return flowable5ActivityBehaviorFactory;
    }

    public ProcessEngineConfigurationImpl setFlowable5ActivityBehaviorFactory(Object flowable5ActivityBehaviorFactory) {
        this.flowable5ActivityBehaviorFactory = flowable5ActivityBehaviorFactory;
        return this;
    }

    public Object getFlowable5ExpressionManager() {
        return flowable5ExpressionManager;
    }

    public ProcessEngineConfigurationImpl setFlowable5ExpressionManager(Object flowable5ExpressionManager) {
        this.flowable5ExpressionManager = flowable5ExpressionManager;
        return this;
    }

    public Object getFlowable5ListenerFactory() {
        return flowable5ListenerFactory;
    }

    public ProcessEngineConfigurationImpl setFlowable5ListenerFactory(Object flowable5ListenerFactory) {
        this.flowable5ListenerFactory = flowable5ListenerFactory;
        return this;
    }

    public List<Object> getFlowable5PreBpmnParseHandlers() {
        return flowable5PreBpmnParseHandlers;
    }

    public ProcessEngineConfigurationImpl setFlowable5PreBpmnParseHandlers(List<Object> flowable5PreBpmnParseHandlers) {
        this.flowable5PreBpmnParseHandlers = flowable5PreBpmnParseHandlers;
        return this;
    }

    public List<Object> getFlowable5PostBpmnParseHandlers() {
        return flowable5PostBpmnParseHandlers;
    }

    public ProcessEngineConfigurationImpl setFlowable5PostBpmnParseHandlers(List<Object> flowable5PostBpmnParseHandlers) {
        this.flowable5PostBpmnParseHandlers = flowable5PostBpmnParseHandlers;
        return this;
    }

    public List<Object> getFlowable5CustomDefaultBpmnParseHandlers() {
        return flowable5CustomDefaultBpmnParseHandlers;
    }

    public ProcessEngineConfigurationImpl setFlowable5CustomDefaultBpmnParseHandlers(List<Object> flowable5CustomDefaultBpmnParseHandlers) {
        this.flowable5CustomDefaultBpmnParseHandlers = flowable5CustomDefaultBpmnParseHandlers;
        return this;
    }

    public Set<Class<?>> getFlowable5CustomMybatisMappers() {
        return flowable5CustomMybatisMappers;
    }

    public ProcessEngineConfigurationImpl setFlowable5CustomMybatisMappers(Set<Class<?>> flowable5CustomMybatisMappers) {
        this.flowable5CustomMybatisMappers = flowable5CustomMybatisMappers;
        return this;
    }

    public Set<String> getFlowable5CustomMybatisXMLMappers() {
        return flowable5CustomMybatisXMLMappers;
    }

    public ProcessEngineConfigurationImpl setFlowable5CustomMybatisXMLMappers(Set<String> flowable5CustomMybatisXMLMappers) {
        this.flowable5CustomMybatisXMLMappers = flowable5CustomMybatisXMLMappers;
        return this;
    }

    @Override
    public ProcessEngineConfigurationImpl setAsyncExecutorActivate(boolean asyncExecutorActivate) {
        this.asyncExecutorActivate = asyncExecutorActivate;
        return this;
    }

    public int getAsyncExecutorCorePoolSize() {
        return asyncExecutorCorePoolSize;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorCorePoolSize(int asyncExecutorCorePoolSize) {
        this.asyncExecutorCorePoolSize = asyncExecutorCorePoolSize;
        return this;
    }

    public int getAsyncExecutorNumberOfRetries() {
        return asyncExecutorNumberOfRetries;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorNumberOfRetries(int asyncExecutorNumberOfRetries) {
        this.asyncExecutorNumberOfRetries = asyncExecutorNumberOfRetries;
        return this;
    }

    public int getAsyncHistoryExecutorNumberOfRetries() {
        return asyncHistoryExecutorNumberOfRetries;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorNumberOfRetries(int asyncHistoryExecutorNumberOfRetries) {
        this.asyncHistoryExecutorNumberOfRetries = asyncHistoryExecutorNumberOfRetries;
        return this;
    }

    public int getAsyncExecutorMaxPoolSize() {
        return asyncExecutorMaxPoolSize;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorMaxPoolSize(int asyncExecutorMaxPoolSize) {
        this.asyncExecutorMaxPoolSize = asyncExecutorMaxPoolSize;
        return this;
    }

    public long getAsyncExecutorThreadKeepAliveTime() {
        return asyncExecutorThreadKeepAliveTime;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorThreadKeepAliveTime(long asyncExecutorThreadKeepAliveTime) {
        this.asyncExecutorThreadKeepAliveTime = asyncExecutorThreadKeepAliveTime;
        return this;
    }

    public int getAsyncExecutorThreadPoolQueueSize() {
        return asyncExecutorThreadPoolQueueSize;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorThreadPoolQueueSize(int asyncExecutorThreadPoolQueueSize) {
        this.asyncExecutorThreadPoolQueueSize = asyncExecutorThreadPoolQueueSize;
        return this;
    }

    public BlockingQueue<Runnable> getAsyncExecutorThreadPoolQueue() {
        return asyncExecutorThreadPoolQueue;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorThreadPoolQueue(BlockingQueue<Runnable> asyncExecutorThreadPoolQueue) {
        this.asyncExecutorThreadPoolQueue = asyncExecutorThreadPoolQueue;
        return this;
    }

    public long getAsyncExecutorSecondsToWaitOnShutdown() {
        return asyncExecutorSecondsToWaitOnShutdown;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorSecondsToWaitOnShutdown(long asyncExecutorSecondsToWaitOnShutdown) {
        this.asyncExecutorSecondsToWaitOnShutdown = asyncExecutorSecondsToWaitOnShutdown;
        return this;
    }

    public boolean isAsyncExecutorAllowCoreThreadTimeout() {
        return asyncExecutorAllowCoreThreadTimeout;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorAllowCoreThreadTimeout(boolean asyncExecutorAllowCoreThreadTimeout) {
        this.asyncExecutorAllowCoreThreadTimeout = asyncExecutorAllowCoreThreadTimeout;
        return this;
    }

    public ThreadFactory getAsyncExecutorThreadFactory() {
        return asyncExecutorThreadFactory;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorThreadFactory(ThreadFactory asyncExecutorThreadFactory) {
        this.asyncExecutorThreadFactory = asyncExecutorThreadFactory;
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getMaxTimerJobsPerAcquisition()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorMaxTimerJobsPerAcquisition() {
        return asyncExecutorConfiguration.getMaxTimerJobsPerAcquisition();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setMaxTimerJobsPerAcquisition(int)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorMaxTimerJobsPerAcquisition(int asyncExecutorMaxTimerJobsPerAcquisition) {
        asyncExecutorConfiguration.setMaxTimerJobsPerAcquisition(asyncExecutorMaxTimerJobsPerAcquisition);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getMaxAsyncJobsDuePerAcquisition()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorMaxAsyncJobsDuePerAcquisition() {
        return asyncExecutorConfiguration.getMaxAsyncJobsDuePerAcquisition();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setMaxAsyncJobsDuePerAcquisition(int)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorMaxAsyncJobsDuePerAcquisition(int asyncExecutorMaxAsyncJobsDuePerAcquisition) {
        asyncExecutorConfiguration.setMaxAsyncJobsDuePerAcquisition(asyncExecutorMaxAsyncJobsDuePerAcquisition);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getDefaultTimerJobAcquireWaitTime()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorDefaultTimerJobAcquireWaitTime() {
        return (int) asyncExecutorConfiguration.getDefaultTimerJobAcquireWaitTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setDefaultTimerJobAcquireWaitTime(Duration)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorDefaultTimerJobAcquireWaitTime(int asyncExecutorDefaultTimerJobAcquireWaitTime) {
        asyncExecutorConfiguration.setDefaultTimerJobAcquireWaitTime(Duration.ofMillis(asyncExecutorDefaultTimerJobAcquireWaitTime));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getDefaultAsyncJobAcquireWaitTime()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorDefaultAsyncJobAcquireWaitTime() {
        return (int) asyncExecutorConfiguration.getDefaultAsyncJobAcquireWaitTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setDefaultAsyncJobAcquireWaitTime(Duration)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorDefaultAsyncJobAcquireWaitTime(int asyncExecutorDefaultAsyncJobAcquireWaitTime) {
        asyncExecutorConfiguration.setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(asyncExecutorDefaultAsyncJobAcquireWaitTime));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getDefaultQueueSizeFullWaitTime()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorDefaultQueueSizeFullWaitTime() {
        return (int) asyncExecutorConfiguration.getDefaultQueueSizeFullWaitTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setDefaultQueueSizeFullWaitTime(Duration)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorDefaultQueueSizeFullWaitTime(int asyncExecutorDefaultQueueSizeFullWaitTime) {
        asyncExecutorConfiguration.setDefaultQueueSizeFullWaitTime(Duration.ofMillis(asyncExecutorDefaultQueueSizeFullWaitTime));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getLockOwner()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public String getAsyncExecutorLockOwner() {
        return asyncExecutorConfiguration.getLockOwner();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setLockOwner(String)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorLockOwner(String asyncExecutorLockOwner) {
        asyncExecutorConfiguration.setLockOwner(asyncExecutorLockOwner);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#isUnlockOwnedJobs()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public boolean isAsyncExecutorUnlockOwnedJobs() {
        return asyncExecutorConfiguration.isUnlockOwnedJobs();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setUnlockOwnedJobs(boolean)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorUnlockOwnedJobs(boolean asyncExecutorUnlockOwnedJobs) {
        asyncExecutorConfiguration.setUnlockOwnedJobs(asyncExecutorUnlockOwnedJobs);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getTimerLockTime()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorTimerLockTimeInMillis() {
        return (int) asyncExecutorConfiguration.getTimerLockTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setTimerLockTime(Duration)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorTimerLockTimeInMillis(int asyncExecutorTimerLockTimeInMillis) {
        asyncExecutorConfiguration.setTimerLockTime(Duration.ofMillis(asyncExecutorTimerLockTimeInMillis));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getAsyncJobLockTime()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorAsyncJobLockTimeInMillis() {
        return (int) asyncExecutorConfiguration.getAsyncJobLockTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setAsyncJobLockTime(Duration)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorAsyncJobLockTimeInMillis(int asyncExecutorAsyncJobLockTimeInMillis) {
        asyncExecutorConfiguration.setAsyncJobLockTime(Duration.ofMillis(asyncExecutorAsyncJobLockTimeInMillis));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getResetExpiredJobsInterval()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorResetExpiredJobsInterval() {
        return (int) asyncExecutorConfiguration.getResetExpiredJobsInterval().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setResetExpiredJobsInterval(Duration)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsInterval(int asyncExecutorResetExpiredJobsInterval) {
        asyncExecutorConfiguration.setResetExpiredJobsInterval(Duration.ofMillis(asyncExecutorResetExpiredJobsInterval));
        return this;
    }

    public int getAsyncExecutorResetExpiredJobsMaxTimeout() {
        return asyncExecutorResetExpiredJobsMaxTimeout;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsMaxTimeout(int asyncExecutorResetExpiredJobsMaxTimeout) {
        this.asyncExecutorResetExpiredJobsMaxTimeout = asyncExecutorResetExpiredJobsMaxTimeout;
        return this;
    }

    public ExecuteAsyncRunnableFactory getAsyncExecutorExecuteAsyncRunnableFactory() {
        return asyncExecutorExecuteAsyncRunnableFactory;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorExecuteAsyncRunnableFactory(ExecuteAsyncRunnableFactory asyncExecutorExecuteAsyncRunnableFactory) {
        this.asyncExecutorExecuteAsyncRunnableFactory = asyncExecutorExecuteAsyncRunnableFactory;
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getResetExpiredJobsPageSize()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncExecutorResetExpiredJobsPageSize() {
        return asyncExecutorConfiguration.getResetExpiredJobsPageSize();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setResetExpiredJobsPageSize(int)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsPageSize(int asyncExecutorResetExpiredJobsPageSize) {
        asyncExecutorConfiguration.setResetExpiredJobsPageSize(asyncExecutorResetExpiredJobsPageSize);
        return this;
    }

    public boolean isAsyncExecutorIsMessageQueueMode() {
        return asyncExecutorMessageQueueMode;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorMessageQueueMode(boolean asyncExecutorMessageQueueMode) {
        this.asyncExecutorMessageQueueMode = asyncExecutorMessageQueueMode;
        return this;
    }

    public AsyncJobExecutorConfiguration getAsyncExecutorConfiguration() {
        return asyncExecutorConfiguration;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorConfiguration(AsyncJobExecutorConfiguration asyncExecutorConfiguration) {
        this.asyncExecutorConfiguration = asyncExecutorConfiguration;
        return this;
    }

    public AsyncJobExecutorConfiguration getAsyncHistoryExecutorConfiguration() {
        return asyncHistoryExecutorConfiguration;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorConfiguration(AsyncJobExecutorConfiguration asyncHistoryExecutorConfiguration) {
        this.asyncHistoryExecutorConfiguration = asyncHistoryExecutorConfiguration;
        return this;
    }

    public boolean isAsyncHistoryExecutorIsMessageQueueMode() {
        return asyncHistoryExecutorMessageQueueMode;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorMessageQueueMode(boolean asyncHistoryExecutorMessageQueueMode) {
        this.asyncHistoryExecutorMessageQueueMode = asyncHistoryExecutorMessageQueueMode;
        return this;
    }
    
    public List<String> getEnabledJobCategories() {
        return enabledJobCategories;
    }

    public ProcessEngineConfigurationImpl setEnabledJobCategories(List<String> enabledJobCategories) {
        this.enabledJobCategories = enabledJobCategories;
        return this;
    }
    
    public ProcessEngineConfigurationImpl addEnabledJobCategory(String jobCategory) {
        if (enabledJobCategories == null) {
            enabledJobCategories = new ArrayList<>();
        }
        
        enabledJobCategories.add(jobCategory);
        return this;
    }

    public String getJobExecutionScope() {
        return jobExecutionScope;
    }

    public ProcessEngineConfigurationImpl setJobExecutionScope(String jobExecutionScope) {
        this.jobExecutionScope = jobExecutionScope;
        return this;
    }

    public String getHistoryJobExecutionScope() {
        return historyJobExecutionScope;
    }

    public ProcessEngineConfigurationImpl setHistoryJobExecutionScope(String historyJobExecutionScope) {
        this.historyJobExecutionScope = historyJobExecutionScope;
        return this;
    }

    public int getAsyncHistoryExecutorCorePoolSize() {
        return asyncHistoryExecutorCorePoolSize;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorCorePoolSize(int asyncHistoryExecutorCorePoolSize) {
        this.asyncHistoryExecutorCorePoolSize = asyncHistoryExecutorCorePoolSize;
        return this;
    }

    public int getAsyncHistoryExecutorMaxPoolSize() {
        return asyncHistoryExecutorMaxPoolSize;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorMaxPoolSize(int asyncHistoryExecutorMaxPoolSize) {
        this.asyncHistoryExecutorMaxPoolSize = asyncHistoryExecutorMaxPoolSize;
        return this;
    }

    public long getAsyncHistoryExecutorThreadKeepAliveTime() {
        return asyncHistoryExecutorThreadKeepAliveTime;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorThreadKeepAliveTime(long asyncHistoryExecutorThreadKeepAliveTime) {
        this.asyncHistoryExecutorThreadKeepAliveTime = asyncHistoryExecutorThreadKeepAliveTime;
        return this;
    }

    public int getAsyncHistoryExecutorThreadPoolQueueSize() {
        return asyncHistoryExecutorThreadPoolQueueSize;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorThreadPoolQueueSize(int asyncHistoryExecutorThreadPoolQueueSize) {
        this.asyncHistoryExecutorThreadPoolQueueSize = asyncHistoryExecutorThreadPoolQueueSize;
        return this;
    }

    public BlockingQueue<Runnable> getAsyncHistoryExecutorThreadPoolQueue() {
        return asyncHistoryExecutorThreadPoolQueue;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorThreadPoolQueue(BlockingQueue<Runnable> asyncHistoryExecutorThreadPoolQueue) {
        this.asyncHistoryExecutorThreadPoolQueue = asyncHistoryExecutorThreadPoolQueue;
        return this;
    }

    public long getAsyncHistoryExecutorSecondsToWaitOnShutdown() {
        return asyncHistoryExecutorSecondsToWaitOnShutdown;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorSecondsToWaitOnShutdown(long asyncHistoryExecutorSecondsToWaitOnShutdown) {
        this.asyncHistoryExecutorSecondsToWaitOnShutdown = asyncHistoryExecutorSecondsToWaitOnShutdown;
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getDefaultAsyncJobAcquireWaitTime()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime() {
        return (int) getOrCreateAsyncHistoryExecutorConfiguration().getDefaultAsyncJobAcquireWaitTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setDefaultAsyncJobAcquireWaitTime(Duration)}} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(int asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime) {
        getOrCreateAsyncHistoryExecutorConfiguration().setDefaultAsyncJobAcquireWaitTime(Duration.ofMillis(asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getDefaultQueueSizeFullWaitTime()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncHistoryExecutorDefaultQueueSizeFullWaitTime() {
        return (int) getOrCreateAsyncHistoryExecutorConfiguration().getDefaultQueueSizeFullWaitTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setDefaultQueueSizeFullWaitTime(Duration)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorDefaultQueueSizeFullWaitTime(int asyncHistoryExecutorDefaultQueueSizeFullWaitTime) {
        getOrCreateAsyncHistoryExecutorConfiguration().setDefaultQueueSizeFullWaitTime(Duration.ofMillis(asyncHistoryExecutorDefaultQueueSizeFullWaitTime));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getMaxAsyncJobsDuePerAcquisition()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncHistoryExecutorMaxJobsDuePerAcquisition() {
        return getOrCreateAsyncHistoryExecutorConfiguration().getMaxAsyncJobsDuePerAcquisition();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setMaxAsyncJobsDuePerAcquisition(int)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorMaxJobsDuePerAcquisition(int asyncHistoryExecutorMaxJobsDuePerAcquisition) {
        getOrCreateAsyncHistoryExecutorConfiguration().setMaxAsyncJobsDuePerAcquisition(asyncHistoryExecutorMaxJobsDuePerAcquisition);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getLockOwner()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public String getAsyncHistoryExecutorLockOwner() {
        return getOrCreateAsyncHistoryExecutorConfiguration().getLockOwner();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setLockOwner(String)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorLockOwner(String asyncHistoryExecutorLockOwner) {
        getOrCreateAsyncHistoryExecutorConfiguration().setLockOwner(asyncHistoryExecutorLockOwner);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getAsyncJobLockTime()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncHistoryExecutorAsyncJobLockTimeInMillis() {
        return (int) getOrCreateAsyncHistoryExecutorConfiguration().getAsyncJobLockTime().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setAsyncJobLockTime(Duration)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorAsyncJobLockTimeInMillis(int asyncHistoryExecutorAsyncJobLockTimeInMillis) {
        getOrCreateAsyncHistoryExecutorConfiguration().setAsyncJobLockTime(Duration.ofMillis(asyncExecutorSecondsToWaitOnShutdown));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getResetExpiredJobsInterval()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncHistoryExecutorResetExpiredJobsInterval() {
        return (int) getOrCreateAsyncHistoryExecutorConfiguration().getResetExpiredJobsInterval().toMillis();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setResetExpiredJobsInterval(Duration)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorResetExpiredJobsInterval(int asyncHistoryExecutorResetExpiredJobsInterval) {
        getOrCreateAsyncHistoryExecutorConfiguration().setResetExpiredJobsInterval(Duration.ofMillis(asyncHistoryExecutorResetExpiredJobsInterval));
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getResetExpiredJobsPageSize()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public int getAsyncHistoryExecutorResetExpiredJobsPageSize() {
        return getOrCreateAsyncHistoryExecutorConfiguration().getResetExpiredJobsPageSize();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setResetExpiredJobsPageSize(int)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorResetExpiredJobsPageSize(int asyncHistoryExecutorResetExpiredJobsPageSize) {
        getOrCreateAsyncHistoryExecutorConfiguration().setResetExpiredJobsPageSize(asyncHistoryExecutorResetExpiredJobsPageSize);
        return this;
    }

    public boolean isAsyncExecutorMessageQueueMode() {
        return asyncExecutorMessageQueueMode;
    }

    public boolean isAsyncHistoryExecutorMessageQueueMode() {
        return asyncHistoryExecutorMessageQueueMode;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#isAsyncJobAcquisitionEnabled()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public boolean isAsyncExecutorAsyncJobAcquisitionEnabled() {
        return asyncExecutorConfiguration.isAsyncJobAcquisitionEnabled();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setAsyncJobAcquisitionEnabled(boolean)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorAsyncJobAcquisitionEnabled(boolean isAsyncExecutorAsyncJobAcquisitionEnabled) {
        asyncExecutorConfiguration.setAsyncJobAcquisitionEnabled(isAsyncExecutorAsyncJobAcquisitionEnabled);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#isTimerJobAcquisitionEnabled()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public boolean isAsyncExecutorTimerJobAcquisitionEnabled() {
        return asyncExecutorConfiguration.isTimerJobAcquisitionEnabled();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setTimerJobAcquisitionEnabled(boolean)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorTimerJobAcquisitionEnabled(boolean isAsyncExecutorTimerJobAcquisitionEnabled) {
        asyncExecutorConfiguration.setTimerJobAcquisitionEnabled(isAsyncExecutorTimerJobAcquisitionEnabled);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#isResetExpiredJobEnabled()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public boolean isAsyncExecutorResetExpiredJobsEnabled() {
        return asyncExecutorConfiguration.isResetExpiredJobEnabled();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setResetExpiredJobEnabled(boolean)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsEnabled(boolean isAsyncExecutorResetExpiredJobsEnabled) {
        asyncExecutorConfiguration.setResetExpiredJobEnabled(isAsyncExecutorResetExpiredJobsEnabled);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#isAsyncJobAcquisitionEnabled()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled() {
        return getOrCreateAsyncHistoryExecutorConfiguration().isAsyncJobAcquisitionEnabled();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setAsyncJobAcquisitionEnabled(boolean)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorAsyncJobAcquisitionEnabled(boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled) {
        getOrCreateAsyncHistoryExecutorConfiguration().setAsyncJobAcquisitionEnabled(isAsyncHistoryExecutorAsyncJobAcquisitionEnabled);
        return this;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#isResetExpiredJobEnabled()} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public boolean isAsyncHistoryExecutorResetExpiredJobsEnabled() {
        return getOrCreateAsyncHistoryExecutorConfiguration().isResetExpiredJobEnabled();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setResetExpiredJobEnabled(boolean)} via {@link #getAsyncHistoryExecutorConfiguration()}
     */
    @Deprecated
    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorResetExpiredJobsEnabled(boolean isAsyncHistoryExecutorResetExpiredJobsEnabled) {
        getOrCreateAsyncHistoryExecutorConfiguration().setResetExpiredJobEnabled(isAsyncHistoryExecutorResetExpiredJobsEnabled);
        return this;
    }

    public JobServiceConfiguration getJobServiceConfiguration() {
        return jobServiceConfiguration;
    }

    public ProcessEngineConfigurationImpl setJobServiceConfiguration(JobServiceConfiguration jobServiceConfiguration) {
        this.jobServiceConfiguration = jobServiceConfiguration;
        return this;
    }
    
    public BatchServiceConfiguration getBatchServiceConfiguration() {
        return batchServiceConfiguration;
    }

    public ProcessEngineConfigurationImpl setBatchServiceConfiguration(BatchServiceConfiguration batchServiceConfiguration) {
        this.batchServiceConfiguration = batchServiceConfiguration;
        return this;
    }

    public EventSubscriptionServiceConfiguration getEventSubscriptionServiceConfiguration() {
        return eventSubscriptionServiceConfiguration;
    }

    public ProcessEngineConfigurationImpl setEventSubscriptionServiceConfiguration(EventSubscriptionServiceConfiguration eventSubscriptionServiceConfiguration) {
        this.eventSubscriptionServiceConfiguration = eventSubscriptionServiceConfiguration;
        return this;
    }

    @Override
    public VariableServiceConfiguration getVariableServiceConfiguration() {
        return variableServiceConfiguration;
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#getTenantId()} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public String getAsyncExecutorTenantId() {
        return asyncExecutorConfiguration.getTenantId();
    }

    /**
     * @deprecated use {@link AsyncJobExecutorConfiguration#setTenantId(String)} via {@link #getAsyncExecutorConfiguration()}
     */
    @Deprecated
    public void setAsyncExecutorTenantId(String asyncExecutorTenantId) {
        asyncExecutorConfiguration.setTenantId(asyncExecutorTenantId);
    }

    public String getBatchStatusTimeCycleConfig() {
        return batchStatusTimeCycleConfig;
    }

    public void setBatchStatusTimeCycleConfig(String batchStatusTimeCycleConfig) {
        this.batchStatusTimeCycleConfig = batchStatusTimeCycleConfig;
    }

}
