/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2006, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.remoteclassloading;

import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.logging.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test the remote classloading facility of the WebServer listening on post 8083
 * 
 * @author Dimitris.Andreadis@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class RemoteClassloadingServiceUnitTestCase {

    private static Logger log = Logger.getLogger(RemoteClassloadingServiceUnitTestCase.class);

    // Can't use @ArquillianResource here. No resource is deployed to container.
    protected String baseURL = "http://localhost:8080/";

    /**
     * JBAS-4540, don't leak installation directory info through the classloading service.
     */
    @Test
    public void testHttpRequestRevealInstallationDirectory() throws Exception {
        HttpGet httpget = new HttpGet(new URL(baseURL + "/org.jboss.web.WebServer.class").toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_NOT_FOUND);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);

        String statusText = response.getStatusLine().getReasonPhrase();

        assertTrue("Status text reveals installation directory information: " + statusText, statusText.indexOf(".jar") < 0);
    }
}
