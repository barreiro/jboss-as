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
package org.jboss.as.test.integration.web.ear;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.security.DeclareRoles;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import javax.servlet.ServletException;
import javax.servlet.annotation.HttpConstraint;
import javax.servlet.annotation.ServletSecurity;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.security.auth.callback.UsernamePasswordHandler;

/**
 * A servlet that is used to test login of an user.
 * 
 * @author lbarreiro@redhat.com
 */
@WebServlet("/LoginServlet")
@ServletSecurity(@HttpConstraint(rolesAllowed = { "AuthorizedUser" }))
@DeclareRoles("AuthorizedUser")
public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 8696327635287568661L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {

        LoginContext lc = null;
        try {
            lc = doLogin("jbosstest-domain", "user", "strangepass");
        } catch (LoginException e) {
            throw new ServletException("Failed to login to 'client-login' domain as 'user'", e);
        } finally {
            if (lc != null) {
                try {
                    lc.logout();
                } catch (LoginException e) {
                }
            }
        }
        Writer writer = response.getWriter();
        writer.write("GOOD");

    }

    private LoginContext doLogin(String realm, String username, String password) throws LoginException {
        UsernamePasswordHandler handler = new UsernamePasswordHandler(username, password.toCharArray());
        LoginContext lc = new LoginContext(realm, handler);
        lc.login();
        return lc;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
