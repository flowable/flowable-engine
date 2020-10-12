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
package org.flowable.test.ldap;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Date;

import org.flowable.engine.impl.util.EngineServiceUtil;
import org.flowable.engine.test.Deployment;
import org.flowable.ldap.LDAPGroupCache;
import org.flowable.ldap.LDAPGroupCache.LDAPGroupCacheListener;
import org.flowable.ldap.LDAPIdentityServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ContextConfiguration;

@ContextConfiguration("classpath:flowable-context-ldap-group-cache.xml")
public class LdapGroupCacheTest extends LDAPTestCase {

    protected TestLDAPGroupCacheListener cacheListener;

    @BeforeEach
    protected void setUp() throws Exception {

        // Set test cache listener
        LDAPGroupCache ldapGroupCache = ((LDAPIdentityServiceImpl) 
                        EngineServiceUtil.getIdmIdentityService(processEngineConfiguration)).getLdapGroupCache();
        ldapGroupCache.clear();

        cacheListener = new TestLDAPGroupCacheListener();
        ldapGroupCache.setLdapCacheListener(cacheListener);

    }

    @Test
    @Deployment
    public void testLdapGroupCacheUsage() {
        runtimeService.startProcessInstanceByKey("testLdapGroupCache");

        // First task is for Kermit -> cache miss
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("kermit");

        // Second task is for Pepe -> cache miss
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().taskCandidateUser("pepe").count()).isEqualTo(1);
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("pepe");

        // Third task is again for kermit -> cache hit
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isEqualTo(1);
        assertThat(cacheListener.getLastCacheHit()).isEqualTo("kermit");

        // Fourth task is for fozzie -> cache miss + cache eviction of pepe
        // (LRU)
        taskService.complete(taskService.createTaskQuery().singleResult().getId());
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isEqualTo(1);
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheEviction()).isEqualTo("pepe");
    }

    @Test
    public void testLdapGroupCacheExpiration() {
        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isZero();
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("kermit");

        assertThat(taskService.createTaskQuery().taskCandidateUser("pepe").count()).isZero();
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("pepe");

        assertThat(taskService.createTaskQuery().taskCandidateUser("kermit").count()).isZero();
        assertThat(cacheListener.getLastCacheHit()).isEqualTo("kermit");

        // Test the expiration time of the cache
        Date now = new Date();
        processEngineConfiguration.getClock().setCurrentTime(now);

        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isZero();
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheEviction()).isEqualTo("pepe");

        // Moving the clock forward two 45 minutes should trigger cache eviction
        // (configured to 30 mins)
        processEngineConfiguration.getClock().setCurrentTime(new Date(now.getTime() + (45 * 60 * 1000)));
        assertThat(taskService.createTaskQuery().taskCandidateUser("fozzie").count()).isZero();
        assertThat(cacheListener.getLastCacheExpiration()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheEviction()).isEqualTo("fozzie");
        assertThat(cacheListener.getLastCacheMiss()).isEqualTo("fozzie");
    }

    // Test cache listener
    static class TestLDAPGroupCacheListener implements LDAPGroupCacheListener {

        protected String lastCacheMiss;
        protected String lastCacheHit;
        protected String lastCacheEviction;
        protected String lastCacheExpiration;

        @Override
        public void cacheMiss(String userId) {
            this.lastCacheMiss = userId;
        }

        @Override
        public void cacheHit(String userId) {
            this.lastCacheHit = userId;
        }

        @Override
        public void cacheExpired(String userId) {
            this.lastCacheExpiration = userId;
        }

        @Override
        public void cacheEviction(String userId) {
            this.lastCacheEviction = userId;
        }

        public String getLastCacheMiss() {
            return lastCacheMiss;
        }

        public void setLastCacheMiss(String lastCacheMiss) {
            this.lastCacheMiss = lastCacheMiss;
        }

        public String getLastCacheHit() {
            return lastCacheHit;
        }

        public void setLastCacheHit(String lastCacheHit) {
            this.lastCacheHit = lastCacheHit;
        }

        public String getLastCacheExpiration() {
            return lastCacheExpiration;
        }

        public void setLastCacheExpiration(String lastCacheExpiration) {
            this.lastCacheExpiration = lastCacheExpiration;
        }

        public String getLastCacheEviction() {
            return lastCacheEviction;
        }

        public void setLastCacheEviction(String lastCacheEviction) {
            this.lastCacheEviction = lastCacheEviction;
        }

    }

}
