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
package org.flowable.cmmn.engine.impl.agenda;

import org.flowable.common.engine.impl.interceptor.CommandContext;
import org.flowable.common.engine.impl.interceptor.Session;
import org.flowable.common.engine.impl.interceptor.SessionFactory;

public class CmmnEngineAgendaSessionFactory implements SessionFactory {
    
    protected CmmnEngineAgendaFactory cmmnEngineAgendaFactory;
    
    public CmmnEngineAgendaSessionFactory(CmmnEngineAgendaFactory cmmnEngineAgendaFactory) {
        this.cmmnEngineAgendaFactory = cmmnEngineAgendaFactory;
    }

    @Override
    public Class<?> getSessionType() {
        return CmmnEngineAgenda.class;
    }

    @Override
    public Session openSession(CommandContext commandContext) {
        return cmmnEngineAgendaFactory.createAgenda(commandContext);
    }

}
