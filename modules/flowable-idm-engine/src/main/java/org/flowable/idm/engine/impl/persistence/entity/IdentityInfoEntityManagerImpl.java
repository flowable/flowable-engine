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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.flowable.idm.engine.IdmEngineConfiguration;
import org.flowable.idm.engine.impl.persistence.entity.data.IdentityInfoDataManager;

/**
 * @author Tom Baeyens
 * @author Joram Barrez
 */
public class IdentityInfoEntityManagerImpl
    extends AbstractIdmEngineEntityManager<IdentityInfoEntity, IdentityInfoDataManager>
    implements IdentityInfoEntityManager {

    public IdentityInfoEntityManagerImpl(IdmEngineConfiguration idmEngineConfiguration, IdentityInfoDataManager identityInfoDataManager) {
        super(idmEngineConfiguration, identityInfoDataManager);
    }

    @Override
    public void deleteUserInfoByUserIdAndKey(String userId, String key) {
        IdentityInfoEntity identityInfoEntity = findUserInfoByUserIdAndKey(userId, key);
        if (identityInfoEntity != null) {
            delete(identityInfoEntity);
        }
    }

    @Override
    public void updateUserInfo(String userId, String userPassword, String type, String key, String value, String accountPassword, Map<String, String> accountDetails) {
        byte[] storedPassword = null;
        if (accountPassword != null) {
            storedPassword = encryptPassword(accountPassword, userPassword);
        }

        IdentityInfoEntity identityInfoEntity = findUserInfoByUserIdAndKey(userId, key);
        if (identityInfoEntity != null) {
            // update
            identityInfoEntity.setValue(value);
            identityInfoEntity.setPasswordBytes(storedPassword);
            dataManager.update(identityInfoEntity);

            if (accountDetails == null) {
                accountDetails = new HashMap<>();
            }

            Set<String> newKeys = new HashSet<>(accountDetails.keySet());
            List<IdentityInfoEntity> identityInfoDetails = dataManager.findIdentityInfoDetails(identityInfoEntity.getId());
            for (IdentityInfoEntity identityInfoDetail : identityInfoDetails) {
                String detailKey = identityInfoDetail.getKey();
                newKeys.remove(detailKey);
                String newDetailValue = accountDetails.get(detailKey);
                if (newDetailValue == null) {
                    delete(identityInfoDetail);
                } else {
                    // update detail
                    identityInfoDetail.setValue(newDetailValue);
                }
            }
            insertAccountDetails(identityInfoEntity, accountDetails, newKeys);

        } else {
            // insert
            identityInfoEntity = dataManager.create();
            identityInfoEntity.setUserId(userId);
            identityInfoEntity.setType(type);
            identityInfoEntity.setKey(key);
            identityInfoEntity.setValue(value);
            identityInfoEntity.setPasswordBytes(storedPassword);
            insert(identityInfoEntity, false);
            if (accountDetails != null) {
                insertAccountDetails(identityInfoEntity, accountDetails, accountDetails.keySet());
            }
        }
    }

    protected void insertAccountDetails(IdentityInfoEntity identityInfoEntity, Map<String, String> accountDetails, Set<String> keys) {
        for (String newKey : keys) {
            // insert detail
            IdentityInfoEntity identityInfoDetail = dataManager.create();
            identityInfoDetail.setParentId(identityInfoEntity.getId());
            identityInfoDetail.setKey(newKey);
            identityInfoDetail.setValue(accountDetails.get(newKey));
            insert(identityInfoDetail, false);
        }
    }

    protected byte[] encryptPassword(String accountPassword, String userPassword) {
        return accountPassword.getBytes();
    }

    protected String decryptPassword(byte[] storedPassword, String userPassword) {
        return new String(storedPassword);
    }

    @Override
    public IdentityInfoEntity findUserInfoByUserIdAndKey(String userId, String key) {
        return dataManager.findUserInfoByUserIdAndKey(userId, key);
    }

    @Override
    public List<IdentityInfoEntity> findIdentityInfoByUserId(String userId) {
        return dataManager.findIdentityInfoByUserId(userId);
    }

    @Override
    public List<String> findUserInfoKeysByUserIdAndType(String userId, String type) {
        return dataManager.findUserInfoKeysByUserIdAndType(userId, type);
    }

}
