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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

import org.flowable.spring.boot.ldap.FlowableLdapProperties;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldif.LDIFReader;

/**
 * @author Filip Hrisafov
 */
public class InMemoryDirectoryServerCreator {

    public static InMemoryDirectoryServer create(FlowableLdapProperties properties) {
        try {
            InMemoryDirectoryServerConfig config = new InMemoryDirectoryServerConfig(
                properties.getBaseDn());
            config.addAdditionalBindCredentials(
                properties.getUser(),
                properties.getPassword());
            config.setSchema(null);
            InMemoryListenerConfig listenerConfig = InMemoryListenerConfig
                .createLDAPConfig("LDAP", properties.getPort());
            config.setListenerConfigs(listenerConfig);
            InMemoryDirectoryServer server = new InMemoryDirectoryServer(config);
            importLdif(server);
            return server;
        } catch (LDAPException e) {
            throw new RuntimeException(e);
        }

    }

    private static void importLdif(InMemoryDirectoryServer directoryServer) throws LDAPException {
        try (InputStream stream = InMemoryDirectoryServerCreator.class.getClassLoader().getResourceAsStream("users.ldif")) {
            directoryServer.importFromLDIF(true, new LDIFReader(stream));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
