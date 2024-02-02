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
package org.flowable.management.jmx;

import static org.assertj.core.api.Assertions.assertThat;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import org.flowable.engine.ProcessEngineConfiguration;
import org.junit.Test;

/**
 * @author Saeid Mirzaei
 */

public class JobExecutorJMXClientTest {

    @Test
    public void testJobExecutorJMXClient() throws Exception {
        String hostName = Utils.getHostName();
        JMXServiceURL url = new JMXServiceURL("service:jmx:rmi://" + hostName + ":10111/jndi/rmi://" + hostName + ":1099/jmxrmi/flowable");

        ProcessEngineConfiguration processEngineConfig = ProcessEngineConfiguration.createProcessEngineConfigurationFromResource("flowable.cfg.xml");
        processEngineConfig.buildProcessEngine();

        // wait for jmx server to come up
        Thread.sleep(500);
        JMXConnector jmxc = JMXConnectorFactory.connect(url, null);

        MBeanServerConnection mbsc = jmxc.getMBeanServerConnection();
        ObjectName jobExecutorBeanName = new ObjectName("org.flowable.jmx.Mbeans:type=JobExecutor");

        processEngineConfig.getAsyncExecutor().shutdown();

        // first check that job executor is not activated and correctly reported
        // as being inactive
        assertThat(processEngineConfig.isAsyncExecutorActivate()).isFalse();
        assertThat((Boolean) mbsc.getAttribute(jobExecutorBeanName, "JobExecutorActivated")).isFalse();
        // now activate it remotely
        mbsc.invoke(jobExecutorBeanName, "setJobExecutorActivate", new Boolean[] { true }, new String[] { Boolean.class.getName() });

        // check if it has the effect and correctly reported
        // assertTrue(processEngineConfig.getJobExecutor().isActive());
        assertThat((Boolean) mbsc.getAttribute(jobExecutorBeanName, "JobExecutorActivated")).isTrue();

        // again disable and check it
        mbsc.invoke(jobExecutorBeanName, "setJobExecutorActivate", new Boolean[] { false }, new String[] { Boolean.class.getName() });

        // check if it has the effect and correctly reported
        assertThat(processEngineConfig.isAsyncExecutorActivate()).isFalse();
        assertThat((Boolean) mbsc.getAttribute(jobExecutorBeanName, "JobExecutorActivated")).isFalse();
    }
}
