package org.flowable.form.engine.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntityNoRevision;

public abstract class AbstractFormEngineNoRevisionEntity extends AbstractEntityNoRevision {

    public String getIdPrefix() {
        return FormEngineEntityConstants.FORM_ENGINE_ID_PREFIX;
    }
}
