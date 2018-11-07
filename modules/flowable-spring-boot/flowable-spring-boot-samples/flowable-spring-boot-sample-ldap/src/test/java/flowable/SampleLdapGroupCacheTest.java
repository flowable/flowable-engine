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
package flowable;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import org.flowable.common.engine.impl.runtime.Clock;
import org.flowable.engine.ProcessEngineConfiguration;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.ldap.LDAPIdentityServiceImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Filip Hrisafov
 */
@SpringBootTest
@RunWith(SpringRunner.class)
public class SampleLdapGroupCacheTest extends AbstractSampleLdapTest {

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private ProcessEngineConfiguration processEngineConfiguration;

    @Autowired
    private SampleCacheListenerAccessor cacheListener;

    @Autowired
    private RepositoryService repositoryService;

    private Clock clock;

    private Collection<String> processes;

    @Before
    public void setUp() {
        clock = processEngineConfiguration.getClock();
        processes = new ArrayList<>();
        ((LDAPIdentityServiceImpl) idmIdentityService).getLdapGroupCache().clear();
    }

    @After
    public void tearDown() {
        clock.reset();
        processes.forEach(instanceId -> runtimeService.deleteProcessInstance(instanceId, "Test tear down"));
    }

    @Test
    public void ldapGroupCacheUsage() {
        repositoryService.createDeployment().addClasspathResource("processes/groupCache.bpmn20.xml").deploy();
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testLdapGroupCache");
        processes.add(processInstance.getId());

        // First task is for Kermit -> cache miss
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).hasSize(1);
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("kermit");

        // Second task is for Pepe -> cache miss
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().taskCandidateUser("pepe").list()).hasSize(1);
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("pepe");

        // Third task is again for kermit -> cache hit
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).hasSize(1);
        assertThat(cacheListener.getLastCacheHit()).isEqualTo("kermit");

        // Fourth task is for fozzie -> cache miss + cache eviction of pepe
        // (LRU)
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").list()).hasSize(1);
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheEviction()).isEqualTo("pepe");
    }

    @Test
    public void ldapGroupCacheExpiration() {
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).isEmpty();
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("kermit");

        assertThat(taskService.createTaskQuery().taskCandidateUser("pepe").list()).isEmpty();
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("pepe");

        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").list()).isEmpty();
        assertThat(cacheListener.getLastCacheHit()).isEqualTo("kermit");

        // Test the expiration time of the cache
        Date now = new Date();
        clock.setCurrentTime(now);

        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").list()).isEmpty();
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheEviction()).isEqualTo("pepe");

        // Moving the clock forward to 45 minutes should trigger cache eviction
        // (configured to 30 mins)
        processEngineConfiguration.getClock().setCurrentTime(new Date(now.getTime() + (45 * 60 * 1000)));
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").list()).isEmpty();
        assertThat(cacheListener.getLastCacheExpiration()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheEviction()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("fozzie");
    }
}
