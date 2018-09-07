package org.flowable.content.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractContentEngineNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return ContentEngineEntityConstants.CONTENT_ENGINE_ID_PREFIX;
    }
}
