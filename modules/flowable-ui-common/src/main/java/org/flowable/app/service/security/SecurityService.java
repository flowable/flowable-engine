package org.flowable.app.service.security;

import org.flowable.app.security.FlowableAppUser;

public interface SecurityService  {
    FlowableAppUser getCurrentFlowableAppUser();
}
