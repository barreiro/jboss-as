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
package org.jboss.as.test.integration.web.activecachevalve;

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
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.Ignore;
import org.junit.runner.RunWith;

/**
 * JBAS-7311: Valve at Engine level caches active request/response
 * 
 * @author Anil.Saldhana@redhat.com
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class ActiveRequestResponseCacheValveUnitTestCase {

    private static Logger log = Logger.getLogger(ActiveRequestResponseCacheValveUnitTestCase.class);

    protected final String baseURL = "http://localhost:8080/" + getContextPath();

    public String getContextPath() {
        return "valve-requestcaching-web";
    }

    @Deployment(testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/activecachevalve/resources/";

        WebArchive war = ShrinkWrap.create(WebArchive.class, "valve-requestcaching-web.war");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "testCachedRequest.jsp"), "testCachedRequest.jsp");

        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "valve-requestcaching.ear");
        ear.addAsModule(war);

        System.out.println(ear.toString(true));
        System.out.println(war.toString(true));
        return ear;
    }

    /**
     * Test a jsp page that internally checks whether the
     * {@link ActiveRequestResponseCacheValve} is caching the active request
     * 
     * @throws Exception
     */
    @Ignore // lbarerreiro: SKIPPED !!! Valve not present in AS7
    @Test
    public void testActiveRequestCaching() throws Exception {
        URL url = new URL(baseURL + "/testCachedRequest.jsp");

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
