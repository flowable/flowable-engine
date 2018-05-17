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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.xml.namespace.QName;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.TransactionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.apache.ibatis.transaction.managed.ManagedTransactionFactory;
import org.apache.ibatis.type.JdbcType;
import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.delegate.FlowableFunctionDelegate;
import org.flowable.common.engine.api.delegate.event.FlowableEngineEventType;
import org.flowable.common.engine.api.delegate.event.FlowableEventDispatcher;
import org.flowable.common.engine.api.delegate.event.FlowableEventListener;
import org.flowable.common.engine.impl.EngineConfigurator;
import org.flowable.common.engine.impl.EngineDeployer;
import org.flowable.common.engine.impl.HasExpressionManagerEngineConfiguration;
import org.flowable.common.engine.impl.ScriptingEngineAwareEngineConfiguration;
import org.flowable.common.engine.impl.calendar.BusinessCalendarManager;
import org.flowable.common.engine.impl.calendar.CycleBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DueDateBusinessCalendar;
import org.flowable.common.engine.impl.calendar.DurationBusinessCalendar;
import org.flowable.common.engine.impl.calendar.MapBusinessCalendarManager;
import org.flowable.common.engine.impl.callback.RuntimeInstanceStateChangeCallback;
import org.flowable.common.engine.impl.cfg.IdGenerator;
import org.flowable.common.engine.impl.db.AbstractDataManager;
import org.flowable.common.engine.impl.db.DbSchemaManager;
import org.flowable.common.engine.impl.el.ExpressionManager;
import org.flowable.common.engine.impl.event.FlowableEventDispatcherImpl;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandConfig;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.CommandInterceptor;
import org.flowable.common.engine.impl.interceptor.EngineConfigurationConstants;
import org.flowable.common.engine.impl.interceptor.SessionFactory;
import org.flowable.common.engine.impl.persistence.GenericManagerFactory;
import org.flowable.common.engine.impl.persistence.cache.EntityCache;
import org.flowable.common.engine.impl.persistence.cache.EntityCacheImpl;
import org.flowable.common.engine.impl.persistence.deploy.DefaultDeploymentCache;
import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.common.engine.impl.scripting.BeansResolverFactory;
import org.flowable.common.engine.impl.scripting.ResolverFactory;
import org.flowable.common.engine.impl.scripting.ScriptBindingsFactory;
import org.flowable.common.engine.impl.scripting.ScriptingEngines;
import org.flowable.common.engine.impl.util.ReflectUtil;
import org.flowable.engine.CandidateManager;
import org.flowable.engine.DefaultCandidateManager;
import org.flowable.engine.DynamicBpmnService;
import org.flowable.engine.FlowableEngineAgenda;
import org.flowable.engine.FlowableEngineAgendaFactory;
import org.flowable.engine.FormService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.IdentityService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.ProcessEngine;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.app.AppResourceConverter;
import org.flowable.engine.compatibility.DefaultFlowable5CompatibilityHandlerFactory;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandlerFactory;
import org.flowable.engine.delegate.event.impl.BpmnModelEventDispatchAction;
import org.flowable.engine.dynamic.DynamicStateManager;
import org.flowable.engine.form.AbstractFormType;
import org.flowable.engine.impl.DefaultProcessJobParentStateResolver;
import org.flowable.engine.impl.DynamicBpmnServiceImpl;
import org.flowable.engine.impl.FormServiceImpl;
import org.flowable.engine.impl.HistoryServiceImpl;
import org.flowable.engine.impl.IdentityServiceImpl;
import org.flowable.engine.impl.ManagementServiceImpl;
import org.flowable.engine.impl.ProcessEngineImpl;
import org.flowable.engine.impl.RepositoryServiceImpl;
import org.flowable.engine.impl.RuntimeServiceImpl;
import org.flowable.engine.impl.SchemaOperationProcessEngineClose;
import org.flowable.engine.impl.TaskServiceImpl;
import org.flowable.engine.impl.agenda.AgendaSessionFactory;
import org.flowable.engine.impl.agenda.DefaultFlowableEngineAgendaFactory;
import org.flowable.engine.impl.app.AppDeployer;
import org.flowable.engine.impl.app.AppResourceConverterImpl;
import org.flowable.engine.impl.bpmn.data.ItemInstance;
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
import org.flowable.engine.impl.bpmn.parser.handler.CompensateEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EndEventParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ErrorEventDefinitionParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EventBasedGatewayParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.EventSubProcessParseHandler;
import org.flowable.engine.impl.bpmn.parser.handler.ExclusiveGatewayParseHandler;
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
import org.flowable.engine.impl.bpmn.webservice.MessageInstance;
import org.flowable.engine.impl.cmd.RedeployV5ProcessDefinitionsCmd;
import org.flowable.engine.impl.cmd.ValidateExecutionRelatedEntityCountCfgCmd;
import org.flowable.engine.impl.cmd.ValidateTaskRelatedEntityCountCfgCmd;
import org.flowable.engine.impl.cmd.ValidateV5EntitiesCmd;
import org.flowable.engine.impl.db.DbIdGenerator;
import org.flowable.engine.impl.db.EntityDependencyOrder;
import org.flowable.engine.impl.db.ProcessDbSchemaManager;
import org.flowable.engine.impl.delegate.invocation.DefaultDelegateInterceptor;
import org.flowable.engine.impl.dynamic.DefaultDynamicStateManager;
import org.flowable.engine.impl.el.FlowableDateFunctionDelegate;
import org.flowable.engine.impl.el.ProcessExpressionManager;
import org.flowable.engine.impl.event.CompensationEventHandler;
import org.flowable.engine.impl.event.EventHandler;
import org.flowable.engine.impl.event.MessageEventHandler;
import org.flowable.engine.impl.event.SignalEventHandler;
import org.flowable.engine.impl.event.logger.EventLogger;
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
import org.flowable.engine.impl.history.DefaultHistoryManager;
import org.flowable.engine.impl.history.DefaultHistoryTaskManager;
import org.flowable.engine.impl.history.DefaultHistoryVariableManager;
import org.flowable.engine.impl.history.HistoryManager;
import org.flowable.engine.impl.history.async.AsyncHistoryManager;
import org.flowable.engine.impl.history.async.HistoryJsonConstants;
import org.flowable.engine.impl.history.async.json.transformer.ActivityEndHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityFullHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.ActivityStartHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.FormPropertiesSubmittedHistoryJsonTransformer;
import org.flowable.engine.impl.history.async.json.transformer.HistoricDetailVariableUpdateHistoryJsonTransformer;
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
import org.flowable.engine.impl.interceptor.DelegateInterceptor;
import org.flowable.engine.impl.interceptor.LoggingExecutionTreeCommandInvoker;
import org.flowable.engine.impl.jobexecutor.AsyncContinuationJobHandler;
import org.flowable.engine.impl.jobexecutor.AsyncTriggerJobHandler;
import org.flowable.engine.impl.jobexecutor.DefaultFailedJobCommandFactory;
import org.flowable.engine.impl.jobexecutor.ProcessEventJobHandler;
import org.flowable.engine.impl.jobexecutor.TimerActivateProcessDefinitionHandler;
import org.flowable.engine.impl.jobexecutor.TimerStartEventJobHandler;
import org.flowable.engine.impl.jobexecutor.TimerSuspendProcessDefinitionHandler;
import org.flowable.engine.impl.jobexecutor.TriggerTimerEventJobHandler;
import org.flowable.engine.impl.persistence.deploy.DeploymentManager;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionInfoCache;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManager;
import org.flowable.engine.impl.persistence.entity.AttachmentEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ByteArrayEntityManager;
import org.flowable.engine.impl.persistence.entity.ByteArrayEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.CommentEntityManager;
import org.flowable.engine.impl.persistence.entity.CommentEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManager;
import org.flowable.engine.impl.persistence.entity.DeploymentEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityImpl;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManager;
import org.flowable.engine.impl.persistence.entity.EventLogEntryEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManager;
import org.flowable.engine.impl.persistence.entity.EventSubscriptionEntityManagerImpl;
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
import org.flowable.engine.impl.persistence.entity.PropertyEntityManager;
import org.flowable.engine.impl.persistence.entity.PropertyEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManager;
import org.flowable.engine.impl.persistence.entity.ResourceEntityManagerImpl;
import org.flowable.engine.impl.persistence.entity.TableDataManager;
import org.flowable.engine.impl.persistence.entity.TableDataManagerImpl;
import org.flowable.engine.impl.persistence.entity.data.AttachmentDataManager;
import org.flowable.engine.impl.persistence.entity.data.ByteArrayDataManager;
import org.flowable.engine.impl.persistence.entity.data.CommentDataManager;
import org.flowable.engine.impl.persistence.entity.data.DeploymentDataManager;
import org.flowable.engine.impl.persistence.entity.data.EventLogEntryDataManager;
import org.flowable.engine.impl.persistence.entity.data.EventSubscriptionDataManager;
import org.flowable.engine.impl.persistence.entity.data.ExecutionDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricDetailDataManager;
import org.flowable.engine.impl.persistence.entity.data.HistoricProcessInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.ModelDataManager;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionDataManager;
import org.flowable.engine.impl.persistence.entity.data.ProcessDefinitionInfoDataManager;
import org.flowable.engine.impl.persistence.entity.data.PropertyDataManager;
import org.flowable.engine.impl.persistence.entity.data.ResourceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisAttachmentDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisByteArrayDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisCommentDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisDeploymentDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisEventLogEntryDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisEventSubscriptionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisExecutionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisHistoricActivityInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisHistoricDetailDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisHistoricProcessInstanceDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisModelDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisProcessDefinitionDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisProcessDefinitionInfoDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisPropertyDataManager;
import org.flowable.engine.impl.persistence.entity.data.impl.MybatisResourceDataManager;
import org.flowable.engine.impl.scripting.VariableScopeResolverFactory;
import org.flowable.engine.impl.util.ProcessInstanceHelper;
import org.flowable.engine.parse.BpmnParseHandler;
import org.flowable.form.api.FormFieldHandler;
import org.flowable.identitylink.service.IdentityLinkServiceConfiguration;
import org.flowable.identitylink.service.impl.db.IdentityLinkDbSchemaManager;
import org.flowable.idm.engine.IdmEngineConfiguration;
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
import org.flowable.task.service.InternalTaskLocalizationManager;
import org.flowable.task.service.InternalTaskVariableScopeResolver;
import org.flowable.task.service.TaskServiceConfiguration;
import org.flowable.task.service.history.InternalHistoryTaskManager;
import org.flowable.task.service.impl.DefaultTaskPostProcessor;
import org.flowable.task.service.impl.db.TaskDbSchemaManager;
import org.flowable.validation.ProcessValidator;
import org.flowable.validation.ProcessValidatorFactory;
import org.flowable.variable.api.types.VariableType;
import org.flowable.variable.api.types.VariableTypes;
import org.flowable.variable.service.VariableServiceConfiguration;
import org.flowable.variable.service.history.InternalHistoryVariableManager;
import org.flowable.variable.service.impl.db.IbatisVariableTypeHandler;
import org.flowable.variable.service.impl.db.VariableDbSchemaManager;
import org.flowable.variable.service.impl.types.BooleanType;
import org.flowable.variable.service.impl.types.ByteArrayType;
import org.flowable.variable.service.impl.types.CustomObjectType;
import org.flowable.variable.service.impl.types.DateType;
import org.flowable.variable.service.impl.types.DefaultVariableTypes;
import org.flowable.variable.service.impl.types.DoubleType;
import org.flowable.variable.service.impl.types.EntityManagerSession;
import org.flowable.variable.service.impl.types.EntityManagerSessionFactory;
import org.flowable.variable.service.impl.types.IntegerType;
import org.flowable.variable.service.impl.types.JPAEntityListVariableType;
import org.flowable.variable.service.impl.types.JPAEntityVariableType;
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

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public abstract class ProcessEngineConfigurationImpl extends ProcessEngineConfiguration implements
        ScriptingEngineAwareEngineConfiguration, HasExpressionManagerEngineConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessEngineConfigurationImpl.class);

    public static final String DEFAULT_WS_SYNC_FACTORY = "org.flowable.engine.impl.webservice.CxfWebServiceClientFactory";

    public static final String DEFAULT_WS_IMPORTER = "org.flowable.engine.impl.webservice.CxfWSDLImporter";

    public static final String DEFAULT_MYBATIS_MAPPING_FILE = "org/flowable/db/mapping/mappings.xml";

    // SERVICES /////////////////////////////////////////////////////////////////

    protected RepositoryService repositoryService = new RepositoryServiceImpl();
    protected RuntimeService runtimeService = new RuntimeServiceImpl();
    protected HistoryService historyService = new HistoryServiceImpl(this);
    protected IdentityService identityService = new IdentityServiceImpl(this);
    protected TaskService taskService = new TaskServiceImpl(this);
    protected FormService formService = new FormServiceImpl();
    protected ManagementService managementService = new ManagementServiceImpl();
    protected DynamicBpmnService dynamicBpmnService = new DynamicBpmnServiceImpl(this);

    // IDM ENGINE /////////////////////////////////////////////////////
    protected boolean disableIdmEngine;

    // DATA MANAGERS /////////////////////////////////////////////////////////////

    protected AttachmentDataManager attachmentDataManager;
    protected ByteArrayDataManager byteArrayDataManager;
    protected CommentDataManager commentDataManager;
    protected DeploymentDataManager deploymentDataManager;
    protected EventLogEntryDataManager eventLogEntryDataManager;
    protected EventSubscriptionDataManager eventSubscriptionDataManager;
    protected ExecutionDataManager executionDataManager;
    protected HistoricActivityInstanceDataManager historicActivityInstanceDataManager;
    protected HistoricDetailDataManager historicDetailDataManager;
    protected HistoricProcessInstanceDataManager historicProcessInstanceDataManager;
    protected ModelDataManager modelDataManager;
    protected ProcessDefinitionDataManager processDefinitionDataManager;
    protected ProcessDefinitionInfoDataManager processDefinitionInfoDataManager;
    protected PropertyDataManager propertyDataManager;
    protected ResourceDataManager resourceDataManager;

    // ENTITY MANAGERS ///////////////////////////////////////////////////////////

    protected AttachmentEntityManager attachmentEntityManager;
    protected ByteArrayEntityManager byteArrayEntityManager;
    protected CommentEntityManager commentEntityManager;
    protected DeploymentEntityManager deploymentEntityManager;
    protected EventLogEntryEntityManager eventLogEntryEntityManager;
    protected EventSubscriptionEntityManager eventSubscriptionEntityManager;
    protected ExecutionEntityManager executionEntityManager;
    protected HistoricActivityInstanceEntityManager historicActivityInstanceEntityManager;
    protected HistoricDetailEntityManager historicDetailEntityManager;
    protected HistoricProcessInstanceEntityManager historicProcessInstanceEntityManager;
    protected ModelEntityManager modelEntityManager;
    protected ProcessDefinitionEntityManager processDefinitionEntityManager;
    protected ProcessDefinitionInfoEntityManager processDefinitionInfoEntityManager;
    protected PropertyEntityManager propertyEntityManager;
    protected ResourceEntityManager resourceEntityManager;
    protected TableDataManager tableDataManager;

    // Candidate Manager

    protected CandidateManager candidateManager;

    // History Manager

    protected HistoryManager historyManager;

    protected boolean isAsyncHistoryEnabled;
    protected boolean isAsyncHistoryJsonGzipCompressionEnabled;
    protected boolean isAsyncHistoryJsonGroupingEnabled;
    protected int asyncHistoryJsonGroupingThreshold = 10;
    protected AsyncHistoryListener asyncHistoryListener;

    // Job Manager

    protected JobManager jobManager;
    
    // Dynamic state manager
    
    protected DynamicStateManager dynamicStateManager;
    
    // CONFIGURATORS ////////////////////////////////////////////////////////////

    protected boolean enableConfiguratorServiceLoader = true; // Enabled by default. In certain environments this should be set to false (eg osgi)
    protected List<EngineConfigurator> configurators; // The injected configurators
    protected List<EngineConfigurator> allConfigurators; // Including auto-discovered configurators

    protected VariableServiceConfiguration variableServiceConfiguration;
    protected IdentityLinkServiceConfiguration identityLinkServiceConfiguration;
    protected TaskServiceConfiguration taskServiceConfiguration;
    protected JobServiceConfiguration jobServiceConfiguration;
    protected EngineConfigurator idmEngineConfigurator;

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
    protected ProcessDefinitionInfoCache processDefinitionInfoCache;

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

    // ASYNC EXECUTOR ///////////////////////////////////////////////////////////

    /**
     * The number of retries for a job.
     */
    protected int asyncExecutorNumberOfRetries = 3;

    /**
     * The minimal number of threads that are kept alive in the threadpool for job execution. Default value = 2. (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorCorePoolSize = 2;

    /**
     * The maximum number of threads that are created in the threadpool for job execution. Default value = 10. (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorMaxPoolSize = 10;

    /**
     * The time (in milliseconds) a thread used for job execution must be kept alive before it is destroyed. Default setting is 5 seconds. Having a setting > 0 takes resources, but in the case of many
     * job executions it avoids creating new threads all the time. If 0, threads will be destroyed after they've been used for job execution.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected long asyncExecutorThreadKeepAliveTime = 5000L;

    /**
     * The size of the queue on which jobs to be executed are placed, before they are actually executed. Default value = 100. (This property is only applicable when using the
     * {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorThreadPoolQueueSize = 100;

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
     * The number of timer jobs that are acquired during one query (before a job is executed, an acquirement thread fetches jobs from the database and puts them on the queue).
     * <p>
     * Default value = 1, as this lowers the potential on optimistic locking exceptions. Change this value if you know what you are doing.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorMaxTimerJobsPerAcquisition = 1;

    /**
     * The number of async jobs that are acquired during one query (before a job is executed, an acquirement thread fetches jobs from the database and puts them on the queue).
     * <p>
     * Default value = 1, as this lowers the potential on optimistic locking exceptions. Change this value if you know what you are doing.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorMaxAsyncJobsDuePerAcquisition = 1;

    /**
     * The time (in milliseconds) the timer acquisition thread will wait to execute the next acquirement query. This happens when no new timer jobs were found or when less timer jobs have been fetched
     * than set in {@link #asyncExecutorMaxTimerJobsPerAcquisition}. Default value = 10 seconds.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorDefaultTimerJobAcquireWaitTime = 10 * 1000;

    /**
     * The time (in milliseconds) the async job acquisition thread will wait to execute the next acquirement query. This happens when no new async jobs were found or when less async jobs have been
     * fetched than set in {@link #asyncExecutorMaxAsyncJobsDuePerAcquisition}. Default value = 10 seconds.
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorDefaultAsyncJobAcquireWaitTime = 10 * 1000;

    /**
     * The time (in milliseconds) the async job (both timer and async continuations) acquisition thread will wait when the queue is full to execute the next query. By default set to 0 (for backwards
     * compatibility)
     */
    protected int asyncExecutorDefaultQueueSizeFullWaitTime;

    /**
     * When a job is acquired, it is locked so other async executors can't lock and execute it. While doing this, the 'name' of the lock owner is written into a column of the job.
     * <p>
     * By default, a random UUID will be generated when the executor is created.
     * <p>
     * It is important that each async executor instance in a cluster of Flowable engines has a different name!
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected String asyncExecutorLockOwner;

    /**
     * The amount of time (in milliseconds) a timer job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.
     * <p>
     * Default value = 5 minutes;
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorTimerLockTimeInMillis = 5 * 60 * 1000;

    /**
     * The amount of time (in milliseconds) an async job is locked when acquired by the async executor. During this period of time, no other async executor will try to acquire and lock this job.
     * <p>
     * Default value = 5 minutes;
     * <p>
     * (This property is only applicable when using the {@link DefaultAsyncJobExecutor}).
     */
    protected int asyncExecutorAsyncJobLockTimeInMillis = 5 * 60 * 1000;

    /**
     * The amount of time (in milliseconds) that is between two consecutive checks of 'expired jobs'. Expired jobs are jobs that were locked (a lock owner + time was written by some executor, but the
     * job was never completed).
     * <p>
     * During such a check, jobs that are expired are again made available, meaning the lock owner and lock time will be removed. Other executors will now be able to pick it up.
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
     * The {@link AsyncExecutor} has a 'cleanup' thread that resets expired jobs so they can be re-acquired by other executors. This setting defines the size of the page being used when fetching these
     * expired jobs.
     */
    protected int asyncExecutorResetExpiredJobsPageSize = 3;
    
    /**
     * Flags to control which threads (when using the default threadpool-based async executor) are started.
     * This can be used to boot up engine instances that still execute jobs originating from this instance itself,
     * but don't fetch new jobs themselves.
     */
    protected boolean isAsyncExecutorAsyncJobAcquisitionEnabled = true;
    protected boolean isAsyncExecutorTimerJobAcquisitionEnabled = true;
    protected boolean isAsyncExecutorResetExpiredJobsEnabled = true;
    

    /**
     * Experimental!
     * <p>
     * Set this to true when using the message queue based job executor.
     */
    protected boolean asyncExecutorMessageQueueMode;
    
    // More info: see similar async executor properties.
    protected boolean asyncHistoryExecutorMessageQueueMode;
    protected int asyncHistoryExecutorNumberOfRetries = 10;
    protected int asyncHistoryExecutorCorePoolSize = 2;
    protected int asyncHistoryExecutorMaxPoolSize = 10;
    protected long asyncHistoryExecutorThreadKeepAliveTime = 5000L;
    protected int asyncHistoryExecutorThreadPoolQueueSize = 100;
    protected BlockingQueue<Runnable> asyncHistoryExecutorThreadPoolQueue;
    protected long asyncHistoryExecutorSecondsToWaitOnShutdown = 60L;
    protected int asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime = 10 * 1000;
    protected int asyncHistoryExecutorDefaultQueueSizeFullWaitTime;
    protected String asyncHistoryExecutorLockOwner;
    protected int asyncHistoryExecutorAsyncJobLockTimeInMillis = 5 * 60 * 1000;
    protected int asyncHistoryExecutorResetExpiredJobsInterval = 60 * 1000;
    protected int asyncHistoryExecutorResetExpiredJobsPageSize = 3;
    protected boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled = true;
    protected boolean isAsyncHistoryExecutorTimerJobAcquisitionEnabled = true;
    protected boolean isAsyncHistoryExecutorResetExpiredJobsEnabled = true;
    
    protected String jobExecutionScope;

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

    // BPMN PARSER //////////////////////////////////////////////////////////////

    protected List<BpmnParseHandler> preBpmnParseHandlers;
    protected List<BpmnParseHandler> postBpmnParseHandlers;
    protected List<BpmnParseHandler> customDefaultBpmnParseHandlers;
    protected ActivityBehaviorFactory activityBehaviorFactory;
    protected ListenerFactory listenerFactory;
    protected BpmnParseFactory bpmnParseFactory;

    // PROCESS VALIDATION ///////////////////////////////////////////////////////

    protected ProcessValidator processValidator;

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
    protected InternalTaskLocalizationManager internalTaskLocalizationManager;
    protected InternalJobManager internalJobManager;
    protected InternalJobCompatibilityManager internalJobCompatibilityManager;

    protected Map<String, List<RuntimeInstanceStateChangeCallback>> processInstanceStateChangedCallbacks;

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

    protected ExpressionManager expressionManager;
    protected List<String> customScriptingEngineClasses;
    protected ScriptingEngines scriptingEngines;
    protected List<ResolverFactory> resolverFactories;

    protected BusinessCalendarManager businessCalendarManager;

    protected int executionQueryLimit = 20000;
    protected int taskQueryLimit = 20000;
    protected int historicTaskQueryLimit = 20000;
    protected int historicProcessInstancesQueryLimit = 20000;

    protected String wsSyncFactoryClassName = DEFAULT_WS_SYNC_FACTORY;
    protected XMLImporterFactory wsWsdlImporterFactory;
    protected ConcurrentMap<QName, URL> wsOverridenEndpointAddresses = new ConcurrentHashMap<>();

    protected DelegateInterceptor delegateInterceptor;

    protected Map<String, EventHandler> eventHandlers;
    protected List<EventHandler> customEventHandlers;

    protected FailedJobCommandFactory failedJobCommandFactory;
    
    protected FormFieldHandler formFieldHandler;

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

    /**
     * Using field injection together with a delegate expression for a service task / execution listener / task listener is not thread-sade , see user guide section 'Field Injection' for more
     * information.
     * <p>
     * Set this flag to false to throw an exception at runtime when a field is injected and a delegateExpression is used.
     *
     * @since 5.21
     */
    protected DelegateExpressionFieldInjectionMode delegateExpressionFieldInjectionMode = DelegateExpressionFieldInjectionMode.MIXED;

    protected ObjectMapper objectMapper = new ObjectMapper();

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

    protected DbSchemaManager identityLinkDbSchemaManager;
    protected DbSchemaManager variableDbSchemaManager;
    protected DbSchemaManager taskDbSchemaManager;
    protected DbSchemaManager jobDbSchemaManager;

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

    // buildProcessEngine
    // ///////////////////////////////////////////////////////

    @Override
    public ProcessEngine buildProcessEngine() {
        init();
        ProcessEngineImpl processEngine = new ProcessEngineImpl(this);

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
        initConfigurators();
        configuratorsBeforeInit();
        initProcessDiagramGenerator();
        initHistoryLevel();
        initFunctionDelegates();
        initDelegateInterceptor();
        initExpressionManager();
        initAgendaFactory();

        if (usingRelationalDatabase) {
            initDataSource();
            initDbSchemaManagers();
        }

        initHelpers();
        initVariableTypes();
        initBeans();
        initFormEngines();
        initFormTypes();
        initScriptingEngines();
        initClock();
        initBusinessCalendarManager();
        initCommandContextFactory();
        initTransactionContextFactory();
        initCommandExecutors();
        initServices();
        initIdGenerator();
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
        initHistoryManager();
        initDynamicStateManager();
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
        initTaskServiceConfiguration();
        initJobServiceConfiguration();
        initAsyncExecutor();
        initAsyncHistoryExecutor();
        configuratorsAfterInit();
        afterInitTaskServiceConfiguration();
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
                commandInvoker = new LoggingExecutionTreeCommandInvoker();
            } else {
                commandInvoker = new CommandInvoker();
            }
        }
    }

    @Override
    public String getEngineCfgKey() {
        return EngineConfigurationConstants.KEY_PROCESS_ENGINE_CONFIG;
    }

    @Override
    public List<CommandInterceptor> getAdditionalDefaultCommandInterceptors() {
        return Collections.<CommandInterceptor>singletonList(new BpmnOverrideContextInterceptor());
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
    }

    public void initDbSchemaManagers() {
        super.initDbSchemaManager();
        initProcessDbSchemaManager();
        initIdentityLinkDbSchemaManager();
        initVariableDbSchemaManager();
        initTaskDbSchemaManager();
        initJobDbSchemaManager();
    }

    protected void initProcessDbSchemaManager() {
        if (this.dbSchemaManager == null) {
            this.dbSchemaManager = new ProcessDbSchemaManager();
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

    @Override
    public InputStream getMyBatisXmlConfigurationStream() {
        return getResourceAsStream(DEFAULT_MYBATIS_MAPPING_FILE);
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

    @SuppressWarnings("rawtypes")
    public void initDataManagers() {
        if (attachmentDataManager == null) {
            attachmentDataManager = new MybatisAttachmentDataManager(this);
        }
        if (byteArrayDataManager == null) {
            byteArrayDataManager = new MybatisByteArrayDataManager(this);
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
        if (eventSubscriptionDataManager == null) {
            eventSubscriptionDataManager = new MybatisEventSubscriptionDataManager(this);
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
        if (propertyDataManager == null) {
            propertyDataManager = new MybatisPropertyDataManager(this);
        }
        if (resourceDataManager == null) {
            resourceDataManager = new MybatisResourceDataManager(this);
        }
    }

    // Entity managers //////////////////////////////////////////////////////////

    public void initEntityManagers() {
        if (attachmentEntityManager == null) {
            attachmentEntityManager = new AttachmentEntityManagerImpl(this, attachmentDataManager);
        }
        if (byteArrayEntityManager == null) {
            byteArrayEntityManager = new ByteArrayEntityManagerImpl(this, byteArrayDataManager);
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
        if (eventSubscriptionEntityManager == null) {
            eventSubscriptionEntityManager = new EventSubscriptionEntityManagerImpl(this, eventSubscriptionDataManager);
        }
        if (executionEntityManager == null) {
            executionEntityManager = new ExecutionEntityManagerImpl(this, executionDataManager);
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
        if (propertyEntityManager == null) {
            propertyEntityManager = new PropertyEntityManagerImpl(this, propertyDataManager);
        }
        if (resourceEntityManager == null) {
            resourceEntityManager = new ResourceEntityManagerImpl(this, resourceDataManager);
        }
        if (tableDataManager == null) {
            tableDataManager = new TableDataManagerImpl(this);
        }
    }

    // CandidateManager //////////////////////////////

    public void initCandidateManager() {
        if (candidateManager == null) {
            candidateManager = new DefaultCandidateManager(this);
        }
    }

    // History manager ///////////////////////////////////////////////////////////

    public void initHistoryManager() {
        if (historyManager == null) {
            if (isAsyncHistoryEnabled) {
                historyManager = new AsyncHistoryManager(this, historyLevel);
            } else {
                historyManager = new DefaultHistoryManager(this, historyLevel);
            }
        }
    }
    
    // Dynamic state manager ////////////////////////////////////////////////////
    
    public void initDynamicStateManager() {
        if (dynamicStateManager == null) {
            dynamicStateManager = new DefaultDynamicStateManager();
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

        if (customSessionFactories != null) {
            for (SessionFactory sessionFactory : customSessionFactories) {
                addSessionFactory(sessionFactory);
            }
        }

    }

    @Override
    protected void initDbSqlSessionFactoryEntitySettings() {
        defaultInitDbSqlSessionFactoryEntitySettings(EntityDependencyOrder.INSERT_ORDER, EntityDependencyOrder.DELETE_ORDER);

        // Oracle doesn't support bulk inserting for event log entries
        if (isBulkInsertEnabled && "oracle".equals(databaseType)) {
            dbSqlSessionFactory.getBulkInserteableEntityClasses().remove(EventLogEntryEntityImpl.class);
        }
    }

    public void initAsyncHistorySessionFactory() {
        AsyncHistorySessionFactory asyncHistorySessionFactory = new AsyncHistorySessionFactory();
        if (asyncHistoryListener == null) {
            initDefaultAsyncHistoryListener();
        }
        asyncHistorySessionFactory.setAsyncHistoryListener(asyncHistoryListener);
        asyncHistorySessionFactory.registerJobDataTypes(HistoryJsonConstants.ORDERED_TYPES);
        sessionFactories.put(AsyncHistorySession.class, asyncHistorySessionFactory);
    }

    protected void initDefaultAsyncHistoryListener() {
        DefaultAsyncHistoryJobProducer asyncHistoryJobProducer = new DefaultAsyncHistoryJobProducer(
                HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY, HistoryJsonConstants.JOB_HANDLER_TYPE_DEFAULT_ASYNC_HISTORY_ZIPPED);
        asyncHistoryJobProducer.setJsonGzipCompressionEnabled(isAsyncHistoryJsonGzipCompressionEnabled);
        asyncHistoryJobProducer.setAsyncHistoryJsonGroupingEnabled(isAsyncHistoryJsonGroupingEnabled);
        asyncHistoryListener = asyncHistoryJobProducer;
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
            this.variableServiceConfiguration.setInternalHistoryVariableManager(new DefaultHistoryVariableManager(this));
        }

        this.variableServiceConfiguration.setMaxLengthString(this.getMaxLengthString());
        this.variableServiceConfiguration.setSerializableVariableTypeTrackDeserializedObjects(this.isSerializableVariableTypeTrackDeserializedObjects());

        this.variableServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_VARIABLE_SERVICE_CONFIG, this.variableServiceConfiguration);
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

    public void initTaskServiceConfiguration() {
        this.taskServiceConfiguration = new TaskServiceConfiguration();
        this.taskServiceConfiguration.setHistoryLevel(this.historyLevel);
        this.taskServiceConfiguration.setClock(this.clock);
        this.taskServiceConfiguration.setObjectMapper(this.objectMapper);
        this.taskServiceConfiguration.setEventDispatcher(this.eventDispatcher);
        this.taskServiceConfiguration.setIdGenerator(this.taskIdGenerator);

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
            this.taskServiceConfiguration.setInternalTaskAssignmentManager(new DefaultTaskAssignmentManager(this));
        }

        if (this.internalTaskLocalizationManager != null) {
            this.taskServiceConfiguration.setInternalTaskLocalizationManager(this.internalTaskLocalizationManager);
        } else {
            this.taskServiceConfiguration.setInternalTaskLocalizationManager(new DefaultTaskLocalizationManager(this));
        }

        this.taskServiceConfiguration.setEnableTaskRelationshipCounts(this.performanceSettings.isEnableTaskRelationshipCounts());
        this.taskServiceConfiguration.setEnableLocalization(this.performanceSettings.isEnableLocalization());
        this.taskServiceConfiguration.setTaskQueryLimit(this.taskQueryLimit);
        this.taskServiceConfiguration.setHistoricTaskQueryLimit(this.historicTaskQueryLimit);

        this.taskServiceConfiguration.init();
        
        if (dbSqlSessionFactory != null && taskServiceConfiguration.getTaskDataManager() instanceof AbstractDataManager) {
            dbSqlSessionFactory.addLogicalEntityClassMapping("task", ((AbstractDataManager) taskServiceConfiguration.getTaskDataManager()).getManagedEntityClass());
        }

        addServiceConfiguration(EngineConfigurationConstants.KEY_TASK_SERVICE_CONFIG, this.taskServiceConfiguration);
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
        this.jobServiceConfiguration.setHistoryJobHandlers(this.historyJobHandlers);
        this.jobServiceConfiguration.setFailedJobCommandFactory(this.failedJobCommandFactory);

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

        // set the job processors
        this.jobServiceConfiguration.setJobProcessors(this.jobProcessors);
        this.jobServiceConfiguration.setHistoryJobProcessors(this.historyJobProcessors);
        
        this.jobServiceConfiguration.setJobExecutionScope(this.jobExecutionScope);

        this.jobServiceConfiguration.init();

        addServiceConfiguration(EngineConfigurationConstants.KEY_JOB_SERVICE_CONFIG, this.jobServiceConfiguration);
    }

    public void configuratorsAfterInit() {
        for (EngineConfigurator configurator : allConfigurators) {
            LOGGER.info("Executing configure() of {} (priority:{})", configurator.getClass(), configurator.getPriority());
            configurator.configure(this);
        }
    }

    public void afterInitTaskServiceConfiguration() {
        if (engineConfigurations.containsKey(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG)) {
            IdmEngineConfiguration idmEngineConfiguration = (IdmEngineConfiguration) engineConfigurations.get(EngineConfigurationConstants.KEY_IDM_ENGINE_CONFIG);
            this.taskServiceConfiguration.setIdmIdentityService(idmEngineConfiguration.getIdmIdentityService());
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
        bpmnParserHandlers.add(new CancelEventDefinitionParseHandler());
        bpmnParserHandlers.add(new CompensateEventDefinitionParseHandler());
        bpmnParserHandlers.add(new EndEventParseHandler());
        bpmnParserHandlers.add(new ErrorEventDefinitionParseHandler());
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
                        LOGGER.info("Replacing default BpmnParseHandler {} with {}", defaultBpmnParseHandler.getClass().getName(), newBpmnParseHandler.getClass().getName());
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
        historyJsonTransformers.add(new ProcessInstanceStartHistoryJsonTransformer());
        historyJsonTransformers.add(new ProcessInstanceEndHistoryJsonTransformer());
        historyJsonTransformers.add(new ProcessInstanceDeleteHistoryJsonTransformer());
        historyJsonTransformers.add(new ProcessInstanceDeleteHistoryByProcessDefinitionIdJsonTransformer());
        historyJsonTransformers.add(new ProcessInstancePropertyChangedHistoryJsonTransformer());
        historyJsonTransformers.add(new SubProcessInstanceStartHistoryJsonTransformer());
        historyJsonTransformers.add(new SetProcessDefinitionHistoryJsonTransformer());
        historyJsonTransformers.add(new UpdateProcessDefinitionCascadeHistoryJsonTransformer());

        historyJsonTransformers.add(new ActivityStartHistoryJsonTransformer());
        historyJsonTransformers.add(new ActivityEndHistoryJsonTransformer());
        historyJsonTransformers.add(new ActivityFullHistoryJsonTransformer());

        historyJsonTransformers.add(new TaskCreatedHistoryJsonTransformer());
        historyJsonTransformers.add(new TaskEndedHistoryJsonTransformer());

        historyJsonTransformers.add(new TaskPropertyChangedHistoryJsonTransformer());
        historyJsonTransformers.add(new TaskAssigneeChangedHistoryJsonTransformer());
        historyJsonTransformers.add(new TaskOwnerChangedHistoryJsonTransformer());
        
        historyJsonTransformers.add(new IdentityLinkCreatedHistoryJsonTransformer());
        historyJsonTransformers.add(new IdentityLinkDeletedHistoryJsonTransformer());
        
        historyJsonTransformers.add(new VariableCreatedHistoryJsonTransformer());
        historyJsonTransformers.add(new VariableUpdatedHistoryJsonTransformer());
        historyJsonTransformers.add(new VariableRemovedHistoryJsonTransformer());
        historyJsonTransformers.add(new HistoricDetailVariableUpdateHistoryJsonTransformer());
        historyJsonTransformers.add(new FormPropertiesSubmittedHistoryJsonTransformer());
        return historyJsonTransformers;
    }

    // async executor
    // /////////////////////////////////////////////////////////////

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

    public void initAsyncHistoryExecutor() {
        if (isAsyncHistoryEnabled && asyncHistoryExecutor == null) {
            DefaultAsyncHistoryJobExecutor defaultAsyncHistoryExecutor = new DefaultAsyncHistoryJobExecutor();

            // Message queue mode
            defaultAsyncHistoryExecutor.setMessageQueueMode(asyncHistoryExecutorMessageQueueMode);

            // Thread pool config
            defaultAsyncHistoryExecutor.setCorePoolSize(asyncHistoryExecutorCorePoolSize);
            defaultAsyncHistoryExecutor.setMaxPoolSize(asyncHistoryExecutorMaxPoolSize);
            defaultAsyncHistoryExecutor.setKeepAliveTime(asyncHistoryExecutorThreadKeepAliveTime);

            // Threadpool queue
            if (asyncHistoryExecutorThreadPoolQueue != null) {
                defaultAsyncHistoryExecutor.setThreadPoolQueue(asyncHistoryExecutorThreadPoolQueue);
            }
            defaultAsyncHistoryExecutor.setQueueSize(asyncHistoryExecutorThreadPoolQueueSize);
            
            // Thread flags
            defaultAsyncHistoryExecutor.setAsyncJobAcquisitionEnabled(isAsyncHistoryExecutorAsyncJobAcquisitionEnabled);
            defaultAsyncHistoryExecutor.setTimerJobAcquisitionEnabled(isAsyncHistoryExecutorTimerJobAcquisitionEnabled);
            defaultAsyncHistoryExecutor.setResetExpiredJobEnabled(isAsyncHistoryExecutorResetExpiredJobsEnabled);

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

            // Shutdown
            defaultAsyncHistoryExecutor.setSecondsToWaitOnShutdown(asyncHistoryExecutorSecondsToWaitOnShutdown);

            asyncHistoryExecutor = defaultAsyncHistoryExecutor;
        }

        if (asyncHistoryExecutor != null) {
            asyncHistoryExecutor.setJobServiceConfiguration(jobServiceConfiguration);
            asyncHistoryExecutor.setAutoActivate(asyncHistoryExecutorActivate);
        }
        jobServiceConfiguration.setAsyncHistoryExecutor(asyncHistoryExecutor);
        jobServiceConfiguration.setAsyncHistoryExecutorNumberOfRetries(asyncHistoryExecutorNumberOfRetries);
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
        if (taskIdGenerator == null) {
            taskIdGenerator = idGenerator;
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
            variableTypes.addType(new JodaDateType());
            variableTypes.addType(new JodaDateTimeType());
            variableTypes.addType(new DoubleType());
            variableTypes.addType(new UUIDType());
            variableTypes.addType(new JsonType(getMaxLengthString(), objectMapper));
            variableTypes.addType(new LongJsonType(getMaxLengthString() + 1, objectMapper));
            variableTypes.addType(new ByteArrayType());
            variableTypes.addType(new SerializableType(serializableVariableTypeTrackDeserializedObjects));
            variableTypes.addType(new CustomObjectType("item", ItemInstance.class));
            variableTypes.addType(new CustomObjectType("message", MessageInstance.class));
            if (customPostVariableTypes != null) {
                for (VariableType customVariableType : customPostVariableTypes) {
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
            expressionManager = new ProcessExpressionManager(delegateInterceptor, beans);
        }
        expressionManager.setFunctionDelegates(flowableFunctionDelegates);
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

    public void initEventDispatcher() {
        if (this.eventDispatcher == null) {
            this.eventDispatcher = new FlowableEventDispatcherImpl();
        }

        if (this.additionalEventDispatchActions == null) {
            this.additionalEventDispatchActions = new ArrayList<>();
            this.additionalEventDispatchActions.add(new BpmnModelEventDispatchAction());
        }

        this.eventDispatcher.setEnabled(enableEventDispatcher);

        if (eventListeners != null) {
            for (FlowableEventListener listenerToAdd : eventListeners) {
                this.eventDispatcher.addEventListener(listenerToAdd);
            }
        }

        if (typedEventListeners != null) {
            for (Entry<String, List<FlowableEventListener>> listenersToAdd : typedEventListeners.entrySet()) {
                // Extract types from the given string
                FlowableEngineEventType[] types = FlowableEngineEventType.getTypesFromString(listenersToAdd.getKey());

                for (FlowableEventListener listenerToAdd : listenersToAdd.getValue()) {
                    this.eventDispatcher.addEventListener(listenerToAdd, types);
                }
            }
        }
    }

    public void initProcessValidator() {
        if (this.processValidator == null) {
            this.processValidator = new ProcessValidatorFactory().createDefaultProcessValidator();
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
        }

        if (this.customFlowableFunctionDelegates != null) {
            this.flowableFunctionDelegates.addAll(this.customFlowableFunctionDelegates);
        }
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
                LOGGER.info("Found compatibility handler instance : {}", flowable5CompatibilityHandler.getClass());

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

    public Runnable getProcessEngineCloseRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                commandExecutor.execute(getSchemaCommandConfig(), new SchemaOperationProcessEngineClose());
            }
        };
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

    @Override
    public ProcessEngineConfigurationImpl setSessionFactories(Map<Class<?>, SessionFactory> sessionFactories) {
        this.sessionFactories = sessionFactories;
        return this;
    }

    public List<EngineConfigurator> getConfigurators() {
        return configurators;
    }

    public ProcessEngineConfigurationImpl addConfigurator(EngineConfigurator configurator) {
        if (this.configurators == null) {
            this.configurators = new ArrayList<>();
        }
        this.configurators.add(configurator);
        return this;
    }

    public ProcessEngineConfigurationImpl setConfigurators(List<EngineConfigurator> configurators) {
        this.configurators = configurators;
        return this;
    }

    public void setEnableConfiguratorServiceLoader(boolean enableConfiguratorServiceLoader) {
        this.enableConfiguratorServiceLoader = enableConfiguratorServiceLoader;
    }

    public List<EngineConfigurator> getAllConfigurators() {
        return allConfigurators;
    }

    public EngineConfigurator getIdmEngineConfigurator() {
        return idmEngineConfigurator;
    }

    public ProcessEngineConfigurationImpl setIdmEngineConfigurator(EngineConfigurator idmEngineConfigurator) {
        this.idmEngineConfigurator = idmEngineConfigurator;
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
     * @param endpointName The endpoint name for which a new address must be set
     * @param address      The new address of the endpoint
     */
    public ProcessEngineConfiguration addWsEndpointAddress(QName endpointName, URL address) {
        this.wsOverridenEndpointAddresses.put(endpointName, address);
        return this;
    }

    /**
     * Remove the address definition of the given web-service endpoint
     *
     * @param endpointName The endpoint name for which the address definition must be removed
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

    public VariableTypes getVariableTypes() {
        return variableTypes;
    }

    public ProcessEngineConfigurationImpl setVariableTypes(VariableTypes variableTypes) {
        this.variableTypes = variableTypes;
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

    public InternalTaskLocalizationManager getInternalTaskLocalizationManager() {
        return internalTaskLocalizationManager;
    }

    public ProcessEngineConfigurationImpl setInternalTaskLocalizationManager(InternalTaskLocalizationManager internalTaskLocalizationManager) {
        this.internalTaskLocalizationManager = internalTaskLocalizationManager;
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

    @Override
    public ExpressionManager getExpressionManager() {
        return expressionManager;
    }

    @Override
    public ProcessEngineConfigurationImpl setExpressionManager(ExpressionManager expressionManager) {
        this.expressionManager = expressionManager;
        return this;
    }

    public BusinessCalendarManager getBusinessCalendarManager() {
        return businessCalendarManager;
    }

    public ProcessEngineConfigurationImpl setBusinessCalendarManager(BusinessCalendarManager businessCalendarManager) {
        this.businessCalendarManager = businessCalendarManager;
        return this;
    }

    public int getExecutionQueryLimit() {
        return executionQueryLimit;
    }

    public ProcessEngineConfigurationImpl setExecutionQueryLimit(int executionQueryLimit) {
        this.executionQueryLimit = executionQueryLimit;
        return this;
    }

    public int getTaskQueryLimit() {
        return taskQueryLimit;
    }

    public ProcessEngineConfigurationImpl setTaskQueryLimit(int taskQueryLimit) {
        this.taskQueryLimit = taskQueryLimit;
        return this;
    }

    public int getHistoricTaskQueryLimit() {
        return historicTaskQueryLimit;
    }

    public ProcessEngineConfigurationImpl setHistoricTaskQueryLimit(int historicTaskQueryLimit) {
        this.historicTaskQueryLimit = historicTaskQueryLimit;
        return this;
    }

    public int getHistoricProcessInstancesQueryLimit() {
        return historicProcessInstancesQueryLimit;
    }

    public ProcessEngineConfigurationImpl setHistoricProcessInstancesQueryLimit(int historicProcessInstancesQueryLimit) {
        this.historicProcessInstancesQueryLimit = historicProcessInstancesQueryLimit;
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

    public FormFieldHandler getFormFieldHandler() {
        return formFieldHandler;
    }

    public ProcessEngineConfigurationImpl setFormFieldHandler(FormFieldHandler formFieldHandler) {
        this.formFieldHandler = formFieldHandler;
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

    public boolean isEnableDatabaseEventLogging() {
        return enableDatabaseEventLogging;
    }

    public ProcessEngineConfigurationImpl setEnableDatabaseEventLogging(boolean enableDatabaseEventLogging) {
        this.enableDatabaseEventLogging = enableDatabaseEventLogging;
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

    public ByteArrayDataManager getByteArrayDataManager() {
        return byteArrayDataManager;
    }

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

    public EventSubscriptionDataManager getEventSubscriptionDataManager() {
        return eventSubscriptionDataManager;
    }

    public ProcessEngineConfigurationImpl setEventSubscriptionDataManager(EventSubscriptionDataManager eventSubscriptionDataManager) {
        this.eventSubscriptionDataManager = eventSubscriptionDataManager;
        return this;
    }

    public ExecutionDataManager getExecutionDataManager() {
        return executionDataManager;
    }

    public ProcessEngineConfigurationImpl setExecutionDataManager(ExecutionDataManager executionDataManager) {
        this.executionDataManager = executionDataManager;
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

    public PropertyDataManager getPropertyDataManager() {
        return propertyDataManager;
    }

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

    public boolean isEnableConfiguratorServiceLoader() {
        return enableConfiguratorServiceLoader;
    }

    public AttachmentEntityManager getAttachmentEntityManager() {
        return attachmentEntityManager;
    }

    public ProcessEngineConfigurationImpl setAttachmentEntityManager(AttachmentEntityManager attachmentEntityManager) {
        this.attachmentEntityManager = attachmentEntityManager;
        return this;
    }

    public ByteArrayEntityManager getByteArrayEntityManager() {
        return byteArrayEntityManager;
    }

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

    public EventSubscriptionEntityManager getEventSubscriptionEntityManager() {
        return eventSubscriptionEntityManager;
    }

    public ProcessEngineConfigurationImpl setEventSubscriptionEntityManager(EventSubscriptionEntityManager eventSubscriptionEntityManager) {
        this.eventSubscriptionEntityManager = eventSubscriptionEntityManager;
        return this;
    }

    public ExecutionEntityManager getExecutionEntityManager() {
        return executionEntityManager;
    }

    public ProcessEngineConfigurationImpl setExecutionEntityManager(ExecutionEntityManager executionEntityManager) {
        this.executionEntityManager = executionEntityManager;
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

    public PropertyEntityManager getPropertyEntityManager() {
        return propertyEntityManager;
    }

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

    public TableDataManager getTableDataManager() {
        return tableDataManager;
    }

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

    public DynamicStateManager getDynamicStateManager() {
        return dynamicStateManager;
    }

    public ProcessEngineConfigurationImpl setDynamicStateManager(DynamicStateManager dynamicStateManager) {
        this.dynamicStateManager = dynamicStateManager;
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

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    public ProcessEngineConfigurationImpl setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
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

    public DbSchemaManager getVariableDbSchemaManager() {
        return variableDbSchemaManager;
    }

    public ProcessEngineConfigurationImpl setVariableDbSchemaManager(DbSchemaManager variableDbSchemaManager) {
        this.variableDbSchemaManager = variableDbSchemaManager;
        return this;
    }

    public DbSchemaManager getTaskDbSchemaManager() {
        return taskDbSchemaManager;
    }

    public ProcessEngineConfigurationImpl setTaskDbSchemaManager(DbSchemaManager taskDbSchemaManager) {
        this.taskDbSchemaManager = taskDbSchemaManager;
        return this;
    }

    public DbSchemaManager getIdentityLinkDbSchemaManager() {
        return identityLinkDbSchemaManager;
    }

    public ProcessEngineConfigurationImpl setIdentityLinkDbSchemaManager(DbSchemaManager identityLinkDbSchemaManager) {
        this.identityLinkDbSchemaManager = identityLinkDbSchemaManager;
        return this;
    }

    public DbSchemaManager getJobDbSchemaManager() {
        return jobDbSchemaManager;
    }

    public ProcessEngineConfigurationImpl setJobDbSchemaManager(DbSchemaManager jobDbSchemaManager) {
        this.jobDbSchemaManager = jobDbSchemaManager;
        return this;
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

    public int getAsyncExecutorMaxTimerJobsPerAcquisition() {
        return asyncExecutorMaxTimerJobsPerAcquisition;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorMaxTimerJobsPerAcquisition(int asyncExecutorMaxTimerJobsPerAcquisition) {
        this.asyncExecutorMaxTimerJobsPerAcquisition = asyncExecutorMaxTimerJobsPerAcquisition;
        return this;
    }

    public int getAsyncExecutorMaxAsyncJobsDuePerAcquisition() {
        return asyncExecutorMaxAsyncJobsDuePerAcquisition;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorMaxAsyncJobsDuePerAcquisition(int asyncExecutorMaxAsyncJobsDuePerAcquisition) {
        this.asyncExecutorMaxAsyncJobsDuePerAcquisition = asyncExecutorMaxAsyncJobsDuePerAcquisition;
        return this;
    }

    public int getAsyncExecutorDefaultTimerJobAcquireWaitTime() {
        return asyncExecutorDefaultTimerJobAcquireWaitTime;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorDefaultTimerJobAcquireWaitTime(int asyncExecutorDefaultTimerJobAcquireWaitTime) {
        this.asyncExecutorDefaultTimerJobAcquireWaitTime = asyncExecutorDefaultTimerJobAcquireWaitTime;
        return this;
    }

    public int getAsyncExecutorDefaultAsyncJobAcquireWaitTime() {
        return asyncExecutorDefaultAsyncJobAcquireWaitTime;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorDefaultAsyncJobAcquireWaitTime(int asyncExecutorDefaultAsyncJobAcquireWaitTime) {
        this.asyncExecutorDefaultAsyncJobAcquireWaitTime = asyncExecutorDefaultAsyncJobAcquireWaitTime;
        return this;
    }

    public int getAsyncExecutorDefaultQueueSizeFullWaitTime() {
        return asyncExecutorDefaultQueueSizeFullWaitTime;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorDefaultQueueSizeFullWaitTime(int asyncExecutorDefaultQueueSizeFullWaitTime) {
        this.asyncExecutorDefaultQueueSizeFullWaitTime = asyncExecutorDefaultQueueSizeFullWaitTime;
        return this;
    }

    public String getAsyncExecutorLockOwner() {
        return asyncExecutorLockOwner;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorLockOwner(String asyncExecutorLockOwner) {
        this.asyncExecutorLockOwner = asyncExecutorLockOwner;
        return this;
    }

    public int getAsyncExecutorTimerLockTimeInMillis() {
        return asyncExecutorTimerLockTimeInMillis;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorTimerLockTimeInMillis(int asyncExecutorTimerLockTimeInMillis) {
        this.asyncExecutorTimerLockTimeInMillis = asyncExecutorTimerLockTimeInMillis;
        return this;
    }

    public int getAsyncExecutorAsyncJobLockTimeInMillis() {
        return asyncExecutorAsyncJobLockTimeInMillis;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorAsyncJobLockTimeInMillis(int asyncExecutorAsyncJobLockTimeInMillis) {
        this.asyncExecutorAsyncJobLockTimeInMillis = asyncExecutorAsyncJobLockTimeInMillis;
        return this;
    }

    public int getAsyncExecutorResetExpiredJobsInterval() {
        return asyncExecutorResetExpiredJobsInterval;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsInterval(int asyncExecutorResetExpiredJobsInterval) {
        this.asyncExecutorResetExpiredJobsInterval = asyncExecutorResetExpiredJobsInterval;
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

    public int getAsyncExecutorResetExpiredJobsPageSize() {
        return asyncExecutorResetExpiredJobsPageSize;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsPageSize(int asyncExecutorResetExpiredJobsPageSize) {
        this.asyncExecutorResetExpiredJobsPageSize = asyncExecutorResetExpiredJobsPageSize;
        return this;
    }

    public boolean isAsyncExecutorIsMessageQueueMode() {
        return asyncExecutorMessageQueueMode;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorMessageQueueMode(boolean asyncExecutorMessageQueueMode) {
        this.asyncExecutorMessageQueueMode = asyncExecutorMessageQueueMode;
        return this;
    }

    public boolean isAsyncHistoryExecutorIsMessageQueueMode() {
        return asyncHistoryExecutorMessageQueueMode;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorMessageQueueMode(boolean asyncHistoryExecutorMessageQueueMode) {
        this.asyncHistoryExecutorMessageQueueMode = asyncHistoryExecutorMessageQueueMode;
        return this;
    }

    public String getJobExecutionScope() {
        return jobExecutionScope;
    }

    public ProcessEngineConfigurationImpl setJobExecutionScope(String jobExecutionScope) {
        this.jobExecutionScope = jobExecutionScope;
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

    public int getAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime() {
        return asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorDefaultAsyncJobAcquireWaitTime(int asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime) {
        this.asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime = asyncHistoryExecutorDefaultAsyncJobAcquireWaitTime;
        return this;
    }

    public int getAsyncHistoryExecutorDefaultQueueSizeFullWaitTime() {
        return asyncHistoryExecutorDefaultQueueSizeFullWaitTime;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorDefaultQueueSizeFullWaitTime(int asyncHistoryExecutorDefaultQueueSizeFullWaitTime) {
        this.asyncHistoryExecutorDefaultQueueSizeFullWaitTime = asyncHistoryExecutorDefaultQueueSizeFullWaitTime;
        return this;
    }

    public String getAsyncHistoryExecutorLockOwner() {
        return asyncHistoryExecutorLockOwner;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorLockOwner(String asyncHistoryExecutorLockOwner) {
        this.asyncHistoryExecutorLockOwner = asyncHistoryExecutorLockOwner;
        return this;
    }

    public int getAsyncHistoryExecutorAsyncJobLockTimeInMillis() {
        return asyncHistoryExecutorAsyncJobLockTimeInMillis;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorAsyncJobLockTimeInMillis(int asyncHistoryExecutorAsyncJobLockTimeInMillis) {
        this.asyncHistoryExecutorAsyncJobLockTimeInMillis = asyncHistoryExecutorAsyncJobLockTimeInMillis;
        return this;
    }

    public int getAsyncHistoryExecutorResetExpiredJobsInterval() {
        return asyncHistoryExecutorResetExpiredJobsInterval;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorResetExpiredJobsInterval(int asyncHistoryExecutorResetExpiredJobsInterval) {
        this.asyncHistoryExecutorResetExpiredJobsInterval = asyncHistoryExecutorResetExpiredJobsInterval;
        return this;
    }

    public int getAsyncHistoryExecutorResetExpiredJobsPageSize() {
        return asyncHistoryExecutorResetExpiredJobsPageSize;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorResetExpiredJobsPageSize(int asyncHistoryExecutorResetExpiredJobsPageSize) {
        this.asyncHistoryExecutorResetExpiredJobsPageSize = asyncHistoryExecutorResetExpiredJobsPageSize;
        return this;
    }

    public boolean isAsyncExecutorMessageQueueMode() {
        return asyncExecutorMessageQueueMode;
    }

    public boolean isAsyncHistoryExecutorMessageQueueMode() {
        return asyncHistoryExecutorMessageQueueMode;
    }

    public boolean isAsyncExecutorAsyncJobAcquisitionEnabled() {
        return isAsyncExecutorAsyncJobAcquisitionEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorAsyncJobAcquisitionEnabled(boolean isAsyncExecutorAsyncJobAcquisitionEnabled) {
        this.isAsyncExecutorAsyncJobAcquisitionEnabled = isAsyncExecutorAsyncJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncExecutorTimerJobAcquisitionEnabled() {
        return isAsyncExecutorTimerJobAcquisitionEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorTimerJobAcquisitionEnabled(boolean isAsyncExecutorTimerJobAcquisitionEnabled) {
        this.isAsyncExecutorTimerJobAcquisitionEnabled = isAsyncExecutorTimerJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncExecutorResetExpiredJobsEnabled() {
        return isAsyncExecutorResetExpiredJobsEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncExecutorResetExpiredJobsEnabled(boolean isAsyncExecutorResetExpiredJobsEnabled) {
        this.isAsyncExecutorResetExpiredJobsEnabled = isAsyncExecutorResetExpiredJobsEnabled;
        return this;
    }

    public boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled() {
        return isAsyncHistoryExecutorAsyncJobAcquisitionEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorAsyncJobAcquisitionEnabled(boolean isAsyncHistoryExecutorAsyncJobAcquisitionEnabled) {
        this.isAsyncHistoryExecutorAsyncJobAcquisitionEnabled = isAsyncHistoryExecutorAsyncJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncHistoryExecutorTimerJobAcquisitionEnabled() {
        return isAsyncHistoryExecutorTimerJobAcquisitionEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorTimerJobAcquisitionEnabled(boolean isAsyncHistoryExecutorTimerJobAcquisitionEnabled) {
        this.isAsyncHistoryExecutorTimerJobAcquisitionEnabled = isAsyncHistoryExecutorTimerJobAcquisitionEnabled;
        return this;
    }

    public boolean isAsyncHistoryExecutorResetExpiredJobsEnabled() {
        return isAsyncHistoryExecutorResetExpiredJobsEnabled;
    }

    public ProcessEngineConfigurationImpl setAsyncHistoryExecutorResetExpiredJobsEnabled(boolean isAsyncHistoryExecutorResetExpiredJobsEnabled) {
        this.isAsyncHistoryExecutorResetExpiredJobsEnabled = isAsyncHistoryExecutorResetExpiredJobsEnabled;
        return this;
    }
    
}
