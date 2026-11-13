/*
 * Copyright (c) 2004-2022, University of Oslo
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors 
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.hisp.dhis.security;

import static org.apache.commons.lang3.StringUtils.firstNonEmpty;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.hisp.dhis.security.apikey.ApiTokenAuthenticationToken;
import org.hisp.dhis.security.oidc.DhisOidcUser;
import org.hisp.dhis.user.UserDetails;
import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AbstractAuthenticationEvent;
import org.springframework.security.authentication.event.AbstractAuthenticationFailureEvent;
import org.springframework.security.authentication.event.InteractiveAuthenticationSuccessEvent;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.authentication.OAuth2LoginAuthenticationToken;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.session.SessionFixationProtectionEvent;
import org.springframework.security.web.authentication.switchuser.AuthenticationSwitchUserEvent;
import org.springframework.util.ClassUtils;

/**
 * @author Morten Svanæs <msvanaes@dhis2.org>
 */
@Slf4j
public class AuthenticationLoggerListener
    implements ApplicationListener<AbstractAuthenticationEvent> {

  @Override
  public void onApplicationEvent(AbstractAuthenticationEvent event) {
    if (!log.isInfoEnabled()) {
      return;
    }

    if (event instanceof SessionFixationProtectionEvent
        || event instanceof InteractiveAuthenticationSuccessEvent) {
      return;
    }

    if (event instanceof AuthenticationSwitchUserEvent switchUserEvent) {
      log.info(
          "Authentication event: AuthenticationSwitchUserEvent; username: {}; targetUser: {}",
          escapeLogField(switchUserEvent.getAuthentication().getName()),
          escapeLogField(switchUserEvent.getTargetUser().getUsername()));
    }

    logAuthenticationEvent(event);
  }

  private void logAuthenticationEvent(AbstractAuthenticationEvent event) {
    Authentication authentication = event.getAuthentication();

    String authName = firstNonEmpty(authentication.getName(), "");
    String ipAddress = "";
    String exceptionMessage = "";

    // Resolve identity independently of details and outcome. Never log token keys
    // or serialize an authentication/principal object to obtain audit fields.
    if (authentication instanceof OAuth2LoginAuthenticationToken authenticationToken) {
      authName =
          firstNonEmpty(getUsernameFromPrincipal(authenticationToken.getPrincipal()), authName);
    } else if (authentication instanceof OAuth2AuthenticationToken authenticationToken) {
      authName =
          firstNonEmpty(getUsernameFromPrincipal(authenticationToken.getPrincipal()), authName);
    } else if (authentication instanceof ApiTokenAuthenticationToken authenticationToken) {
      UserDetails principal = authenticationToken.getPrincipal();
      authName = principal == null ? "" : firstNonEmpty(principal.getUsername(), "");
    }

    if (authentication.getDetails() instanceof ForwardedIpAwareWebAuthenticationDetails details) {
      ipAddress = formatIpAddress(details.getIp());
    } else if (authentication.getDetails() instanceof WebAuthenticationDetails details) {
      ipAddress = formatIpAddress(details.getRemoteAddress());
    }
    if (event instanceof AbstractAuthenticationFailureEvent failureEvent) {
      exceptionMessage = "exception: " + escapeLogField(failureEvent.getException().getMessage());
    }

    logMessage(event, authName, ipAddress, exceptionMessage);
  }

  private void logMessage(
      AbstractAuthenticationEvent event,
      String authName,
      String ipAddress,
      String exceptionMessage) {
    String eventClassName =
        String.format("Authentication event: %s; ", ClassUtils.getShortName(event.getClass()));
    String usernamePrefix =
        StringUtils.isEmpty(authName)
            ? ""
            : String.format("username: %s; ", escapeLogField(authName));
    String msg = eventClassName + usernamePrefix + ipAddress + exceptionMessage;

    log.info(Strings.CS.removeEnd(msg.stripTrailing(), ";"));
  }

  private static String formatIpAddress(String ip) {
    return String.format("ip: %s; ", escapeLogField(ip));
  }

  // Preserve international identities while keeping each value inside one line
  // and one semicolon-delimited field. Escape backslashes to avoid ambiguity.
  private static String escapeLogField(String value) {
    if (value == null) return "";
    StringBuilder safe = new StringBuilder();
    value
        .codePoints()
        .forEach(
            c -> {
              int type = Character.getType(c);
              if (c == '\\'
                  || c == ';'
                  || c == '<'
                  || c == '>'
                  || c == '&'
                  || c == '"'
                  || Character.isISOControl(c)
                  || type == Character.FORMAT
                  || type == Character.LINE_SEPARATOR
                  || type == Character.PARAGRAPH_SEPARATOR
                  || type == Character.SURROGATE) {
                String hex = Integer.toHexString(c);
                int width = c > 0xffff ? 8 : 4;
                safe.append(c > 0xffff ? "\\U" : "\\u");
                for (int i = hex.length(); i < width; i++) safe.append('0');
                safe.append(hex);
              } else {
                safe.appendCodePoint(c);
              }
            });
    return safe.toString();
  }

  private static String getUsernameFromPrincipal(Object principal) {
    if (principal instanceof DhisOidcUser dhisOidcUser) {
      return dhisOidcUser.getUsername();
    }
    return "";
  }
}
