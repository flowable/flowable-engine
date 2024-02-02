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

import org.flowable.spring.impl.test.SpringFlowableTestCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;

import com.unboundid.ldap.listener.InMemoryDirectoryServer;

/**
 * @author Joram Barrez
 */
@Tag("ldap")
public class LDAPTestCase extends SpringFlowableTestCase {


    @AfterAll
    static void closeInMemoryDirectoryServer(@Autowired InMemoryDirectoryServer inMemoryDirectoryServer) {
        // Need to do this 'manually', or otherwise the ldap server won't be
        // shut down properly
        // on the QA machine, failing the next tests
        if (inMemoryDirectoryServer != null) {
            inMemoryDirectoryServer.shutDown(true);
        }
    }

}
