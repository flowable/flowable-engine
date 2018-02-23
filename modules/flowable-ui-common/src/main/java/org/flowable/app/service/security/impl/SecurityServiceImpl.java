package org.flowable.app.service.security.impl;

import org.flowable.app.security.FlowableAppUser;
import org.flowable.app.service.security.SecurityService;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class SecurityServiceImpl implements SecurityService {
    @Override
    public FlowableAppUser getCurrentFlowableAppUser() {
        FlowableAppUser user = null;
        SecurityContext securityContext = SecurityContextHolder.getContext();
        if (securityContext != null && securityContext.getAuthentication() != null) {
            Object principal = securityContext.getAuthentication().getPrincipal();
            if (principal instanceof FlowableAppUser) {
                user = (FlowableAppUser) principal;
            }
        }
        return user;
    }
}
