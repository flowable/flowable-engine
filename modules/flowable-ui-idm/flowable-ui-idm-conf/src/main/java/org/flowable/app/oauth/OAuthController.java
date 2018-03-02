/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.flowable.app.oauth;

import java.util.Collection;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.flowable.app.security.FlowableAppUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.flowable.app.security.OAuthAuthorizationHandler;
import org.flowable.app.security.OAuthAuthorizationRequestor;

/**
 *
 * @author ahmedghonim
 */
@Controller
public class OAuthController {

    private static final Logger LOG = LoggerFactory.getLogger(OAuthController.class);

    @Autowired(required = false)
    protected Collection<OAuthAuthorizationRequestor> oAuth2Requestors;

    @Autowired(required = false)
    protected Collection<OAuthAuthorizationHandler> oAuth2Handlers;

    @Autowired
    protected RememberMeServices customPersistentRememberMeServices;

    @RequestMapping("/authorize")
    public String authorize(@RequestParam(value = "config", required = false) String config, Model model,
            HttpServletRequest request) {
        if (oAuth2Requestors == null) {
            String e = "No OAuth2 Requestors found on Classpath!";
            LOG.error(e);
            throw new RuntimeException(e);
        }
        OAuthAuthorizationRequestor selectedRequestor = null;
        for (OAuthAuthorizationRequestor c : oAuth2Requestors) {
            if (config == null || config.isEmpty()) {
                LOG.debug("No specific Requestor selected. Defaulting to the first one!");
                selectedRequestor = c;
                break;
            }
            if (c.canAuthorize(config)) {
                selectedRequestor = c;
                break;
            }
        }
        if (selectedRequestor == null) {
            String e = String.format("Did not find default OAuth Requestors or one matching name '%1$s'!", config);
            LOG.error(e);
            throw new RuntimeException(e);
        }
        LOG.debug(String.format("Using OAuth Requestor '%1$s'!", selectedRequestor.getClass().getCanonicalName()));
        String redirectUrl = selectedRequestor.buildAuthorizationURL();
        if (redirectUrl != null) {
            return "redirect:" + redirectUrl;
        }
        LOG.error("Requestor could not build an authorization URL");
        return "redirect:/";
    }

    @RequestMapping("/handle")
    public String handle(HttpServletRequest request,
            @RequestParam(value = "state", required = false) String state,
            @RequestParam(value = "code", required = true) String code,
            HttpServletResponse response) {
        if (oAuth2Handlers == null) {
            String e = "No OAuth2 Handlers found on Classpath!";
            LOG.error(e);
            throw new RuntimeException(e);
        }
        OAuthAuthorizationHandler selectedHandler = null;
        for (OAuthAuthorizationHandler c : oAuth2Handlers) {
            if (c.canHandle(state)) {
                selectedHandler = c;
                break;
            }
        }
        if (selectedHandler == null) {
            String e = String.format("Did not find OAuth Handler that can handle the state!");
            LOG.error(e);
            throw new RuntimeException(e);
        }
        LOG.debug(String.format("Using OAuth Handler '%1$s'!", selectedHandler.getClass().getCanonicalName()));
        FlowableAppUser user = selectedHandler.handle(state, code);
        if (user != null) {
            Authentication auth2 = new UsernamePasswordAuthenticationToken(user, "", user.getAuthorities());
            customPersistentRememberMeServices.loginSuccess(request, response, auth2);
            return selectedHandler.successRedirectUrl(state);
        }
        return "redirect:/";
    }
}
