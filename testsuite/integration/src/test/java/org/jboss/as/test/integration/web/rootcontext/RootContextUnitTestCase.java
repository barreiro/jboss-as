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
package org.jboss.as.test.integration.web.rootcontext;

import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.OperateOnDeployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This class tests a root context deployed as an EAR or a WAR.
 * 
 * @author Stan.Silvert@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RootContextUnitTestCase {

    private static Logger log = Logger.getLogger(RootContextUnitTestCase.class);

    // @ArquillianResource
    // protected URL baseURL;

    @BeforeClass
    public static void deployment() {
        // We need to suspend the default root context when running these tests
        //ObjectName root = new ObjectName("jboss.web.deployment:war=/ROOT");
        //MBeanServerConnection connection = getServer();
        //connection.invoke(root, "stop", null, null);
    }

    @Deployment(name = "root-context.war", testable = false)
    public static WebArchive warDeployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/rootcontext/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "root-context.war");

        war.setWebXML(tccl.getResource(resourcesLocation + "root-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");

        log.info(war.toString(true));
        return war;
    }

    @Deployment(name = "root-web.ear", testable = false)
    public static EnterpriseArchive earDeployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/rootcontext/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "root-web.war");

        war.setWebXML(tccl.getResource(resourcesLocation + "root-web.xml"));
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");
        
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "root-web.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application-root.xml"));
        ear.addAsModule(war);

        log.info(ear.toString(true));
        log.info(war.toString(true));
        return ear;
    }
    
    @AfterClass
    public static void undeployment() {
        // We need to resume the default root context after running these tests
        //ObjectName root = new ObjectName("jboss.web.deployment:war=/ROOT");
        //MBeanServerConnection connection = getServer();
        //connection.invoke(root, "start", null, null);
    }

    @Test
    @OperateOnDeployment("root-context.war")
    public void testRootContextWAR(@ArquillianResource URL url) throws Exception {
        String response = hitRootContext(url);
        assertTrue(response.contains("A Root Context Page"));
    }

    @Test
    @OperateOnDeployment("root-web.ear")
    public void testRootContextEAR(@ArquillianResource URL url) throws Exception {
        String response = hitRootContext(url);
        assertTrue(response.contains("A Root Context Page"));
    }

    /**
     * Access http://localhost/
     */
    private String hitRootContext(URL url) throws Exception {
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
        
        return EntityUtils.toString(response.getEntity());
    }
}
