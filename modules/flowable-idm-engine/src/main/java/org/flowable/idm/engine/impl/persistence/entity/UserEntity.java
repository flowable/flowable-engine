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
package org.flowable.idm.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.db.HasRevision;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.idm.api.Picture;
import org.flowable.idm.api.User;

/**
 * @author Tom Baeyens
 * @author Arkadiy Gornovoy
 */
public interface UserEntity extends User, Entity, HasRevision {

    Picture getPicture();

    void setPicture(Picture picture);

    @Override
    String getId();

    @Override
    void setId(String id);

    @Override
    String getFirstName();

    @Override
    void setFirstName(String firstName);

    @Override
    String getLastName();

    @Override
    void setLastName(String lastName);

    @Override
    String getEmail();

    @Override
    void setEmail(String email);

    @Override
    String getPassword();

    @Override
    void setPassword(String password);

    @Override
    boolean isPictureSet();

    ByteArrayRef getPictureByteArrayRef();

}
