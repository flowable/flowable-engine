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

import org.flowable.ldap.LDAPGroupCache;
import org.springframework.stereotype.Component;

/**
 * @author Filip Hrisafov
 */
@Component
public class SampleLdapGroupCacheListener implements LDAPGroupCache.LDAPGroupCacheListener, SampleCacheListenerAccessor {

    protected String lastCacheMiss;
    protected String lastCacheHit;
    protected String lastCacheEviction;
    protected String lastCacheExpiration;

    @Override
    public void cacheHit(String userId) {
        this.lastCacheHit = userId;
    }

    @Override
    public void cacheMiss(String userId) {
        this.lastCacheMiss = userId;
    }

    @Override
    public void cacheEviction(String userId) {
        this.lastCacheEviction = userId;
    }

    @Override
    public void cacheExpired(String userId) {
        this.lastCacheExpiration = userId;
    }

    @Override
    public String getLastCacheMiss() {
        return lastCacheMiss;
    }

    @Override
    public String getLastCacheHit() {
        return lastCacheHit;
    }

    @Override
    public String getLastCacheEviction() {
        return lastCacheEviction;
    }

    @Override
    public String getLastCacheExpiration() {
        return lastCacheExpiration;
    }
}
