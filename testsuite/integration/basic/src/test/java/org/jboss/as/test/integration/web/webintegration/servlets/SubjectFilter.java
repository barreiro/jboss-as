/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.as.test.integration.web.webintegration.servlets;

import java.io.IOException;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.security.auth.Subject;
import javax.security.jacc.PolicyContext;
import javax.security.jacc.PolicyContextException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import org.jboss.security.AuthenticationManager;

/**
 * @author Scott.Stark@jboss.org
 */
public class SubjectFilter implements Filter {

    public void init(FilterConfig filterConfig) throws ServletException {
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException,
            ServletException {
        try {
            Subject userSubject = getActiveSubject((HttpServletResponse) response);
            if (userSubject == null)
                throw new ServletException("Active subject was null");
        } catch (NamingException e) {
            throw new ServletException("Failed to lookup active subject", e);
        } catch (PolicyContextException e) {
            throw new ServletException("Failed to lookup active subject", e);
        }
        filterChain.doFilter(request, response);
    }

    public void destroy() {
    }

    protected Subject getActiveSubject(HttpServletResponse httpResponse) throws NamingException, PolicyContextException {
        InitialContext ctx = new InitialContext();
        
        // ctx.lookup("java:comp/env/security/securityMgr");
        AuthenticationManager mgr = (AuthenticationManager) ctx.lookup("java:jboss/jaas/jbosstest-context");

        @SuppressWarnings("deprecation")
        Subject s0 = mgr.getActiveSubject();
        httpResponse.addHeader("X-SubjectFilter-SubjectSecurityManager", s0.toString());
        
        //Subject s1 = (Subject) ctx.lookup("java:comp/env/security/subject");
        Subject s1 = (Subject) PolicyContext.getContext("javax.security.auth.Subject.container");
        httpResponse.addHeader("X-SubjectFilter-ENC", s1.toString());

        return s1;
    }
}
