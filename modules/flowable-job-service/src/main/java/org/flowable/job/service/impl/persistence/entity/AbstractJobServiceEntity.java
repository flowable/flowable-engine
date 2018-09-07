package org.flowable.job.service.impl.persistence.entity;

import org.flowable.common.engine.impl.persistence.entity.AbstractEntity;

public abstract class AbstractJobServiceEntity extends AbstractEntity {

    public String getIdPrefix() {
        return JobServiceEntityConstants.JOB_SERVICE_ID_PREFIX;
    }
}
