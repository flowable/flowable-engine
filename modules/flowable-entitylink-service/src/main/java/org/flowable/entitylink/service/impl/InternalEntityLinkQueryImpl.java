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
package org.flowable.entitylink.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.flowable.common.engine.api.FlowableIllegalArgumentException;
import org.flowable.common.engine.impl.db.SingleCachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.cache.CachedEntity;
import org.flowable.common.engine.impl.persistence.cache.CachedEntityMatcher;
import org.flowable.common.engine.impl.persistence.entity.Entity;
import org.flowable.entitylink.api.EntityLinkInfo;
import org.flowable.entitylink.api.InternalEntityLinkQuery;

/**
 * @author Filip Hrisafov
 */
public class InternalEntityLinkQueryImpl<E extends Entity & EntityLinkInfo>
        implements InternalEntityLinkQuery<E>, CachedEntityMatcher<E>, SingleCachedEntityMatcher<E> {

    protected final Function<InternalEntityLinkQueryImpl<E>, List<E>> listProvider;
    protected final Function<InternalEntityLinkQueryImpl<E>, E> singleResultProvider;

    protected String scopeId;
    protected Collection<String> scopeIds;
    protected String scopeDefinitionId;
    protected String scopeType;
    protected String referenceScopeId;
    protected String referenceScopeDefinitionId;
    protected String referenceScopeType;
    protected String rootScopeId;
    protected String rootScopeType;
    protected String linkType;
    protected String hierarchyType;

    public InternalEntityLinkQueryImpl(Function<InternalEntityLinkQueryImpl<E>, List<E>> listProvider,
            Function<InternalEntityLinkQueryImpl<E>, E> singleResultProvider) {
        this.listProvider = listProvider;
        this.singleResultProvider = singleResultProvider;
    }

    @Override
    public InternalEntityLinkQuery<E> scopeId(String scopeId) {
        if (StringUtils.isEmpty(scopeId)) {
            throw new FlowableIllegalArgumentException("scopeId is empty");
        }
        this.scopeId = scopeId;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> scopeIds(Collection<String> scopeIds) {
        if (scopeIds == null || scopeIds.isEmpty()) {
            throw new FlowableIllegalArgumentException("scopeIds is empty");
        }
        this.scopeIds = scopeIds;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> scopeDefinitionId(String scopeDefinitionId) {
        if (StringUtils.isEmpty(scopeDefinitionId)) {
            throw new FlowableIllegalArgumentException("scopeDefinitionId is empty");
        }
        this.scopeDefinitionId = scopeDefinitionId;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> scopeType(String scopeType) {
        if (StringUtils.isEmpty(scopeType)) {
            throw new FlowableIllegalArgumentException("scopeType is empty");
        }
        this.scopeType = scopeType;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> referenceScopeId(String referenceScopeId) {
        if (StringUtils.isEmpty(referenceScopeId)) {
            throw new FlowableIllegalArgumentException("referenceScopeId is empty");
        }
        this.referenceScopeId = referenceScopeId;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> referenceScopeDefinitionId(String referenceScopeDefinitionId) {
        if (StringUtils.isEmpty(referenceScopeDefinitionId)) {
            throw new FlowableIllegalArgumentException("referenceScopeDefinitionId is empty");
        }
        this.referenceScopeDefinitionId = referenceScopeDefinitionId;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> referenceScopeType(String referenceScopeType) {
        if (StringUtils.isEmpty(referenceScopeType)) {
            throw new FlowableIllegalArgumentException("referenceScopeType is empty");
        }
        this.referenceScopeType = referenceScopeType;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> rootScopeId(String rootScopeId) {
        if (StringUtils.isEmpty(rootScopeId)) {
            throw new FlowableIllegalArgumentException("rootScopeId is empty");
        }
        this.rootScopeId = rootScopeId;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> rootScopeType(String rootScopeType) {
        if (StringUtils.isEmpty(rootScopeType)) {
            throw new FlowableIllegalArgumentException("rootScopeType is empty");
        }
        this.rootScopeType = rootScopeType;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> linkType(String linkType) {
        if (StringUtils.isEmpty(linkType)) {
            throw new FlowableIllegalArgumentException("linkType is empty");
        }
        this.linkType = linkType;
        return this;
    }

    @Override
    public InternalEntityLinkQuery<E> hierarchyType(String hierarchyType) {
        if (StringUtils.isEmpty(hierarchyType)) {
            throw new FlowableIllegalArgumentException("hierarchyType is empty");
        }
        this.hierarchyType = hierarchyType;
        return this;
    }

    @Override
    public List<E> list() {
        return listProvider.apply(this);
    }

    @Override
    public E singleResult() {
        return singleResultProvider.apply(this);
    }

    public Collection<String> getScopeIds() {
        return scopeIds;
    }

    public String getScopeId() {
        return scopeId;
    }

    public String getScopeDefinitionId() {
        return scopeDefinitionId;
    }

    public String getScopeType() {
        return scopeType;
    }

    public String getReferenceScopeId() {
        return referenceScopeId;
    }

    public String getReferenceScopeDefinitionId() {
        return referenceScopeDefinitionId;
    }

    public String getReferenceScopeType() {
        return referenceScopeType;
    }

    public String getRootScopeId() {
        return rootScopeId;
    }

    public String getRootScopeType() {
        return rootScopeType;
    }

    public String getLinkType() {
        return linkType;
    }

    public String getHierarchyType() {
        return hierarchyType;
    }

    // This method is needed because we have a different way of querying list and single objects via MyBatis.
    // Querying lists wraps the object in a ListQueryParameterObject
    public InternalEntityLinkQueryImpl<E> getParameter() {
        return this;
    }

    @Override
    public boolean isRetained(E entity, Object param) {
        return isRetained(entity, (InternalEntityLinkQueryImpl<?>) param);
    }

    @Override
    public boolean isRetained(Collection<E> databaseEntities, Collection<CachedEntity> cachedEntities, E entity, Object param) {
        return isRetained(entity, (InternalEntityLinkQueryImpl<?>) param);
    }

    public boolean isRetained(E entity, InternalEntityLinkQueryImpl<?> param) {
        if (param.scopeId != null && !param.scopeId.equals(entity.getScopeId())) {
            return false;
        }

        if (param.scopeIds != null && !param.scopeIds.contains(entity.getScopeId())) {
            return false;
        }

        if (param.scopeDefinitionId != null && !param.scopeDefinitionId.equals(entity.getScopeDefinitionId())) {
            return false;
        }

        if (param.scopeType != null && !param.scopeType.equals(entity.getScopeType())) {
            return false;
        }

        if (param.referenceScopeId != null && !param.referenceScopeId.equals(entity.getReferenceScopeId())) {
            return false;
        }

        if (param.referenceScopeDefinitionId != null && !param.referenceScopeDefinitionId.equals(entity.getReferenceScopeDefinitionId())) {
            return false;
        }

        if (param.referenceScopeType != null && !param.referenceScopeType.equals(entity.getReferenceScopeType())) {
            return false;
        }

        if (param.rootScopeId != null && !param.rootScopeId.equals(entity.getRootScopeId())) {
            return false;
        }

        if (param.rootScopeType != null && !param.rootScopeType.equals(entity.getRootScopeType())) {
            return false;
        }

        if (param.linkType != null && !param.linkType.equals(entity.getLinkType())) {
            return false;
        }

        if (param.hierarchyType != null && !param.hierarchyType.equals(entity.getHierarchyType())) {
            return false;
        }

        return true;
    }
}
