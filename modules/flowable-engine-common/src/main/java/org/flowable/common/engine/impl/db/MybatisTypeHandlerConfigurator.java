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
package org.flowable.common.engine.impl.db;

import org.apache.ibatis.type.TypeHandler;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.flowable.common.engine.impl.AbstractEngineConfiguration;

/**
 * This class configures {@link TypeHandler} in {@link TypeHandlerRegistry}
 *
 * @author martin.grofcik
 */
public interface MybatisTypeHandlerConfigurator {

    void configure(AbstractEngineConfiguration engineConfiguration, TypeHandlerRegistry typeHandlerRegistry);
}
