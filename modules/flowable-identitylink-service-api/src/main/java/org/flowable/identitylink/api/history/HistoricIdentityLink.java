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

package org.flowable.identitylink.api.history;

import java.util.Date;

import org.flowable.identitylink.api.IdentityLink;
import org.flowable.identitylink.api.IdentityLinkInfo;

/**
 * Historic counterpart of {@link IdentityLink} that represents the current state if any runtime link. Will be preserved when the runtime process instance or task is finished.
 * 
 * @author Frederik Heremans
 */
public interface HistoricIdentityLink extends IdentityLinkInfo {
    /**
     * Returns the time when the identity link was created.
     */
    Date getCreateTime();
}
