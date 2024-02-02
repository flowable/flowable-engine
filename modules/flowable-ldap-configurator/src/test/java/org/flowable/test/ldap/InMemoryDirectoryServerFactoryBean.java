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

import java.io.InputStream;

import org.flowable.common.engine.api.FlowableException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.io.Resource;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;

/**
 * @author Filip Hrisafov
 */
public class InMemoryDirectoryServerFactoryBean implements FactoryBean<InMemoryDirectoryServer>, InitializingBean, SmartLifecycle {

    protected final Object lifeCycleMonitor = new Object();
    protected boolean running;

    protected String baseDn;
    protected String user;
    protected String password;
    protected int port;
    protected Resource ldif;
    protected InMemoryDirectoryServer inMemoryDirectoryServer;

    @Override
    public InMemoryDirectoryServer getObject() throws Exception {
        return inMemoryDirectoryServer;
    }

    @Override
    public Class<?> getObjectType() {
        return InMemoryDirectoryServer.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(baseDn);
        config.addAdditionalBindCredentials(user, password);
        config.setSchema(null);
        InMemoryListenerConfig listenerConfig = InMemoryListenerConfig.createLDAPConfig("LDAP", port);
        config.setListenerConfigs(listenerConfig);
        inMemoryDirectoryServer = new InMemoryDirectoryServer(config);
        try (InputStream stream = ldif.getInputStream()) {
            inMemoryDirectoryServer.importFromLDIF(true, new LDIFReader(stream));
        }
    }

    @Override
    public void start() {
        synchronized (lifeCycleMonitor) {
            if (!isRunning()) {
                try {
                    inMemoryDirectoryServer.startListening();
                    running = true;
                } catch (LDAPException e) {
                    throw new FlowableException("failed to start listening", e);
                }
            }
        }
    }

    @Override
    public void stop() {
        synchronized (lifeCycleMonitor) {
            if (isRunning()) {
                inMemoryDirectoryServer.shutDown(true);
                running = false;
            }
        }
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    // Getters and setters
    public String getBaseDn() {
        return baseDn;
    }

    public void setBaseDn(String baseDn) {
        this.baseDn = baseDn;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public Resource getLdif() {
        return ldif;
    }

    public void setLdif(Resource ldif) {
        this.ldif = ldif;
    }
}
