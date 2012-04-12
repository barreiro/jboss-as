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
package org.jboss.as.test.integration.web.webintegration;

import static org.jboss.as.test.integration.web.webintegration.WebIntegrationBase.accessURL;

import java.net.URL;

import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.arquillian.api.ServerSetup;
import org.jboss.as.test.integration.web.webintegration.WebIntegrationBase.WebIntegrationOptimizedSetup;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of servlet container integration into the JBoss server.
 * 
 * This test require optimization (pass by reference in EJB3 container)
 * 
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
@ServerSetup(WebIntegrationOptimizedSetup.class)
public class WebIntegrationOptimizedUnitTestCase {
    
    @Deployment(name = "jbosstest-web.ear", testable = false)
    public static EnterpriseArchive deployment() {
        return WebIntegrationUnitTestCase.deployment();
    }

    /**
     * Access the /jbosstest/EJBOnStartupServlet
     */
    @Test
    public void testEJBOnStartupServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/EJBOnStartupServlet"));
    }

    /**
     * Access the /jbosstest/EJBServlet
     */
    @Test
    public void testEJBServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/EJBServlet"));
    }

    /**
     * Access the /jbosstest/SpeedServlet
     */
    @Test
    public void testSpeedServlet(@ArquillianResource URL base) throws Exception {
        accessURL(new URL(base + "/jbosstest/SpeedServlet?optimized"));
    }

}
