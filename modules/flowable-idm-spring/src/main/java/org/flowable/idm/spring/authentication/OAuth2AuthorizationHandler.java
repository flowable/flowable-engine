/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.idm.spring.authentication;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;

/**
 *
 * @author ahmedghonim
 */
public interface OAuth2AuthorizationHandler {

    public boolean canHandle(HttpServletRequest request, String state);

    public Authentication handle(HttpServletRequest request, String state);

    public String successRedirectUrl(HttpServletRequest request, String state);
    
}
