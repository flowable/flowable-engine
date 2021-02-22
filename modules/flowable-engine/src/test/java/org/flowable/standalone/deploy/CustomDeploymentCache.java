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
package org.flowable.standalone.deploy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.flowable.common.engine.impl.persistence.deploy.DeploymentCache;
import org.flowable.engine.impl.persistence.deploy.ProcessDefinitionCacheEntry;
import org.flowable.engine.repository.ProcessDefinition;

/**
 * Very simplistic cache implementation that only caches one process definition.
 * 
 * @author Joram Barrez
 */
public class CustomDeploymentCache implements DeploymentCache<ProcessDefinitionCacheEntry> {

    protected String id;
    protected ProcessDefinitionCacheEntry entry;

    @Override
    public ProcessDefinitionCacheEntry get(String id) {
        if (id.equals(this.id)) {
            return entry;
        }
        return null;
    }

    @Override
    public void add(String id, ProcessDefinitionCacheEntry object) {
        this.id = id;
        this.entry = object;
    }

    @Override
    public void remove(String id) {
        if (id.equals(this.id)) {
            this.id = null;
            this.entry = null;
        }
    }

    @Override
    public void clear() {
        this.id = null;
        this.entry = null;
    }

    @Override
    public boolean contains(String id) {
        return id.equals(this.id);
    }

    @Override
    public Collection<ProcessDefinitionCacheEntry> getAll() {
        if (entry != null) {
            return Collections.singletonList(entry);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public int size() {
        if (entry != null) {
            return 1;
        } else {
            return 0;
        }
    }

    // For testing purposes only
    public ProcessDefinition getCachedProcessDefinition() {
        if (entry == null) {
            return null;
        }
        return entry.getProcessDefinition();
    }

}
