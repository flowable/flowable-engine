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
package org.flowable.engine.impl.util;

import org.flowable.common.engine.api.FlowableException;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.impl.interceptor.Command;
import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.engine.compatibility.Flowable5CompatibilityHandler;
import org.flowable.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.flowable.engine.impl.context.Flowable5CompatibilityContext;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.repository.Deployment;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.job.api.Job;
import org.flowable.job.api.JobInfo;

/**
 * @author Joram Barrez
 * @author Tijs Rademakers
 */
public class Flowable5Util {

    public static final String V5_ENGINE_TAG = "v5";
    
    public static boolean isJobHandledByV5Engine(JobInfo jobInfo) {
        if (!(jobInfo instanceof Job)) { // v5 only knew one type of jobs
            return false;
        }
        
        final Job job = (Job) jobInfo;
        ProcessEngineConfigurationImpl processEngineConfiguration = CommandContextUtil.getProcessEngineConfiguration();
        boolean isFlowable5ProcessDefinition = Flowable5Util.isFlowable5ProcessDefinitionId(processEngineConfiguration, job.getProcessDefinitionId());
        if (isFlowable5ProcessDefinition) {
            return processEngineConfiguration.getCommandExecutor().execute(new Command<Boolean>() {
                @Override
                public Boolean execute(CommandContext commandContext) {
                    CommandContextUtil.getProcessEngineConfiguration(commandContext).getFlowable5CompatibilityHandler().executeJobWithLockAndRetry(job);
                    return true;
                }
            });
        }
        return false;
    }

    public static boolean isFlowable5ProcessDefinitionId(CommandContext commandContext, final String processDefinitionId) {

        if (processDefinitionId == null) {
            return false;
        }

        try {
            ProcessDefinition processDefinition = ProcessDefinitionUtil.getProcessDefinition(processDefinitionId);
            if (processDefinition == null) {
                return false;
            }
            return isFlowable5ProcessDefinition(processDefinition, commandContext);

        } catch (FlowableObjectNotFoundException e) {
            return false;
        }
    }

    /**
     * Use this method when running outside a {@link CommandContext}. It will check the cache first and only start a new {@link CommandContext} when no result is found in the cache.
     */
    public static boolean isFlowable5ProcessDefinitionId(final ProcessEngineConfigurationImpl processEngineConfiguration, final String processDefinitionId) {

        if (processDefinitionId == null) {
            return false;
        }

        ProcessDefinitionCacheEntry cacheEntry = processEngineConfiguration.getProcessDefinitionCache().get(processDefinitionId);
        if (cacheEntry != null) {
            ProcessDefinition processDefinition = cacheEntry.getProcessDefinition();
            return isFlowable5ProcessDefinition(processDefinition, processEngineConfiguration);

        } else {
            return processEngineConfiguration.getCommandExecutor().execute(new Command<Boolean>() {

                @Override
                public Boolean execute(CommandContext commandContext) {
                    return isFlowable5ProcessDefinitionId(commandContext, processDefinitionId);
                }

            });

        }
    }

    public static boolean isFlowable5Deployment(Deployment deployment, CommandContext commandContext) {
        return isFlowable5Deployment(deployment, CommandContextUtil.getProcessEngineConfiguration(commandContext));
    }

    public static boolean isFlowable5Deployment(Deployment deployment, ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (isV5Entity(deployment.getEngineVersion(), deployment.getId(), "deployment", processEngineConfiguration)) {
            return true;
        }

        return false;
    }

    public static boolean isFlowable5ProcessDefinition(ProcessDefinition processDefinition, CommandContext commandContext) {
        return isFlowable5ProcessDefinition(processDefinition, CommandContextUtil.getProcessEngineConfiguration(commandContext));
    }

    public static boolean isFlowable5ProcessDefinition(ProcessDefinition processDefinition, ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (isV5Entity(processDefinition.getEngineVersion(), processDefinition.getId(), "process definition", processEngineConfiguration)) {
            return true;
        }

        return false;
    }

    public static boolean isV5Entity(String tag, String id, String entityType, ProcessEngineConfigurationImpl processEngineConfiguration) {
        if (isVersion5Tag(tag)) {
            if (!processEngineConfiguration.isFlowable5CompatibilityEnabled() || processEngineConfiguration.getFlowable5CompatibilityHandler() == null) {
                throw new FlowableException(entityType + " with id " + id + " has a v5 tag and flowable 5 compatibility is not enabled");
            }

            return true;

        } else {
            if (tag != null) {
                throw new FlowableException("Invalid 'engine' for " + entityType + " with id " + id + " : " + tag);
            }
            return false;
        }
    }

    public static boolean isVersion5Tag(String tag) {
        return V5_ENGINE_TAG.equals(tag) || "activiti-5".equals(tag);
    }

    public static Flowable5CompatibilityHandler getFlowable5CompatibilityHandler() {
        Flowable5CompatibilityHandler flowable5CompatibilityHandler = CommandContextUtil.getProcessEngineConfiguration().getFlowable5CompatibilityHandler(); 
        if (flowable5CompatibilityHandler == null) {
            flowable5CompatibilityHandler = Flowable5CompatibilityContext.getFallbackFlowable5CompatibilityHandler();
        }

        if (flowable5CompatibilityHandler == null) {
            throw new FlowableException("Found Flowable 5 process definition, but no compatibility handler on the classpath");
        }
        return flowable5CompatibilityHandler;
    }

}
