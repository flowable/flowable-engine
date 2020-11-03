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

package org.flowable.management.jmx.mbeans;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBean;

import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.job.service.impl.asyncexecutor.AsyncExecutor;
import org.flowable.management.jmx.DefaultManagementMBeanAssembler;
import org.flowable.management.jmx.ManagementMBeanAssembler;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * @author Saeid Mirzaei
 */

public class JobExecutorMBeanTest {

    protected JobExecutorMBean jobExecutorMbean;

    @Mock
    protected ProcessEngineConfiguration processEngineConfiguration;

    @Mock
    protected AsyncExecutor jobExecutor;

    @Before
    public void initMocks() throws MalformedObjectNameException {
        MockitoAnnotations.initMocks(this);
        when(processEngineConfiguration.getAsyncExecutor()).thenReturn(jobExecutor);
        jobExecutorMbean = new JobExecutorMBean(processEngineConfiguration);
    }

    @Test
    public void TestIsJobExecutorActivatedFalse() {
        when(jobExecutor.isActive()).thenReturn(false);

        boolean result = jobExecutorMbean.isJobExecutorActivated();
        verify(jobExecutor).isActive();
        assertThat(result).isFalse();

    }

    @Test
    public void TestIsJobExecutorActivatedTrue() {
        when(jobExecutor.isActive()).thenReturn(true);
        boolean result = jobExecutorMbean.isJobExecutorActivated();
        verify(jobExecutor).isActive();
        assertThat(result).isTrue();
    }

    @Test
    public void setJobExecutorActivateTrue() {
        jobExecutorMbean.setJobExecutorActivate(true);
        verify(jobExecutor).start();

        jobExecutorMbean.setJobExecutorActivate(false);
        verify(jobExecutor).shutdown();

    }

    ManagementMBeanAssembler assembler = new DefaultManagementMBeanAssembler();

    @Test
    public void testAnnotations() throws MalformedObjectNameException, JMException {

        ModelMBean modelBean = assembler.assemble(jobExecutorMbean, new ObjectName("domain", "key", "value"));
        assertThat(modelBean).isNotNull();
        MBeanInfo beanInfo = modelBean.getMBeanInfo();
        assertThat(beanInfo).isNotNull();
        assertThat(beanInfo.getOperations()).hasSize(2);
        int counter = 0;

        for (MBeanOperationInfo op : beanInfo.getOperations()) {
            if ("setJobExecutorActivate".equals(op.getName())) {
                counter++;
                assertThat(op.getDescription()).isEqualTo("set job executor activate");
                assertThat(op.getReturnType()).isEqualTo("void");
                assertThat(op.getSignature()).hasSize(1);
                assertThat(op.getSignature()[0].getType()).isEqualTo("java.lang.Boolean");
            }
        }
        assertThat(counter).isEqualTo(1);

        // check attributes
        assertThat(beanInfo.getAttributes()).hasSize(1);

        counter = 0;

        for (MBeanAttributeInfo attr : beanInfo.getAttributes()) {
            if ("JobExecutorActivated".equals(attr.getName())) {
                counter++;
                assertThat(attr.getDescription()).isEqualTo("check if the job executor is activated");
                assertThat(attr.getType()).isEqualTo("boolean");
            }
        }
        assertThat(counter).isEqualTo(1);

    }

}
