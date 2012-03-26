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
import java.io.PrintWriter;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jboss.as.test.integration.web.webintegration.interfaces.Entity;
import org.jboss.as.test.integration.web.webintegration.interfaces.EntityHome;
import org.jboss.as.test.integration.web.webintegration.interfaces.EntityPK;
import org.jboss.as.test.integration.web.webintegration.util.Util;

/**
 * A servlet that accesses an entity EJB
 * 
 * @author Scott.Stark@jboss.org
 */
public class EntityServlet extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = 7912796412250168574L;

    protected void processRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException,
            IOException {
        try {
            InitialContext ctx = new InitialContext();
            Context enc = (Context) ctx.lookup("java:comp/env");
            EntityHome home = (EntityHome) enc.lookup("ejb/Entity");
            try {
                // Remove old entity beans from previous test runs
                Entity existing = home.findByPrimaryKey(new EntityPK(12345));
                if (existing != null) {
                    existing.remove();
                }
            } catch (Exception e) {
                // ignore
            }
            Entity bean = home.create(12345, 6789);
            bean.write(7890);
            bean.read();
            bean.remove();
        } catch (Exception e) {
            throw new ServletException("Failed to call Entity through remote interfaces", e);
        }
        response.setContentType("text/html");
        PrintWriter out = response.getWriter();
        out.println("<html>");
        out.println("<head><title>EntityServlet</title></head>");
        out.println("<body>Tests passed<br>Time:" + Util.getTime() + "</body>");
        out.println("</html>");
        out.close();
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        processRequest(request, response);
    }
}
