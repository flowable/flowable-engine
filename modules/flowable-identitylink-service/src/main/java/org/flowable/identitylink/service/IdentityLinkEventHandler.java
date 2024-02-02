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
package org.flowable.identitylink.service;

import org.flowable.identitylink.api.IdentityLink;

/**
 * An interface to handle identity link event
 *
 * @author martin.grofcik
 */
public interface IdentityLinkEventHandler {

    /**
     * Handle an event of the {@link IdentityLink} addition
     *
     * @param identityLink identityLink which was recently added
     */
    void handleIdentityLinkAddition(IdentityLink identityLink);

    /**
     * Handle an event of {@link IdentityLink} deletion
     *
      * @param identityLink identityLink which is going to be deleted
     */
    void handleIdentityLinkDeletion(IdentityLink identityLink);
}
