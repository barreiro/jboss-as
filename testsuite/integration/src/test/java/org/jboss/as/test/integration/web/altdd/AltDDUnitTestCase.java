/*
 * JBoss, Home of Professional Open Source
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
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
package org.jboss.as.test.integration.web.altdd;

import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of using a war alt-dd web.xml
 *
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class AltDDUnitTestCase {
    
    private static Logger log = Logger.getLogger(AltDDUnitTestCase.class);

    @ArquillianResource
    protected URL baseURL;

    @Deployment(testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/altdd/resources/";
        
        WebArchive war = ShrinkWrap.create(WebArchive.class, "altdd.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "web.xml"));
        war.addClass(RequestInfoServlet.class);

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "altdd.ear");
        ear.setApplicationXML(tccl.getResource(resourcesLocation + "application.xml"));
        ear.addAsManifestResource(tccl.getResource(resourcesLocation + "altdd-web.xml"), "altdd-web.xml");
        ear.addAsModule(war);

        log.info(ear.toString(true));
        log.info(war.toString(true));
        return ear;
    }

    /**
     * Access the http://{host}/altdd-test/AltRequestInfoServlet to see
     * that the altenate uri for the RequestInfoServlet was used
     */
    @Test
    public void testAltRequestInfoServlet() throws Exception {
        testRequestInfoServlet(new URL(baseURL + "/AltRequestInfoServlet"));
    }

    /**
     * Access the http://{host}/altdd-test/AltRequestInfoServlet2 to see
     * that the RequestInfoServlet2 was added
     */
    @Test
    public void testAltRequestInfoServlet2() throws Exception {
        testRequestInfoServlet(new URL(baseURL + "/AltRequestInfoServlet2"));
    }

    private void testRequestInfoServlet(URL url) throws Exception {
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
    }
}
