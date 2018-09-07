package org.flowable.identitylink.service.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractIdentityLinkServiceNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return IdentityLinkServiceEntityConstants.IDENTITY_LINK_SERVICE_ID_PREFIX;
    }
}
