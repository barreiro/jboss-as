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
package org.jboss.as.test.integration.web.webprogramlogin;

import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.test.integration.web.sso.SSOBaseCase;
import org.jboss.logging.Logger;
import org.junit.Test;

/**
 * JBAS-4077: Web Programmatic Login
 * 
 * @author Anil.Saldhana@redhat.com
 * @author lbarreiro@redhat.com
 */
public class WebProgrammaticLoginTestCase {

    private static Logger log = Logger.getLogger(WebProgrammaticLoginTestCase.class);

    @ArquillianResource
    private URL baseURL;

    // "programmaticweblogin.ear"

    /**
     * Test unsuccessful login
     */
    @Test
    public void testUnsuccessfulLogin() throws Exception {
        HttpClient httpclient = new DefaultHttpClient();

        // try to perform programmatic auth without supplying login information.
        String path = "war1/TestServlet";
        HttpGet indexGet = new HttpGet(baseURL + path + "?operation=login");
        HttpResponse response = httpclient.execute(indexGet);

        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Get Error(" + statusCode + ")", statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR);

        // assert access to the restricted area of the first app is denied.
        SSOBaseCase.checkAccessDenied(httpclient, baseURL + "war1/restricted/restricted.html");

        // assert access to the second app is not granted, as no successful
        // login was performed (and therefore no ssoid has been set).
        SSOBaseCase.checkAccessDenied(httpclient, baseURL + "war2/index.html");

        // try to perform programmatic auth with no valid username/password.
        path = path + "?operation=login&username=dummy&pass=dummy";
        indexGet = new HttpGet(baseURL + path);
        response = httpclient.execute(indexGet);

        statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Get Error(" + statusCode + ")", statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR);

        // assert access to the restricted applications remains denied.
        SSOBaseCase.checkAccessDenied(httpclient, baseURL + "war1/restricted/restricted.html");
        SSOBaseCase.checkAccessDenied(httpclient, baseURL + "war2/index.html");
    }

    /**
     * Test Successful programmatic login in a servlet
     */
    @Test
    public void testSuccessfulLogin() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        String path1 = "war1/TestServlet?operation=login&username=jduke&pass=theduke";
        HttpGet indexGet = new HttpGet(baseURL + path1);
        HttpResponse response = httpclient.execute(indexGet);

        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Get OK(" + statusCode + ")", statusCode == HttpURLConnection.HTTP_OK);

        // assert access to the restricted are of the first app is now allowed.
        SSOBaseCase.checkAccessAllowed(httpclient, baseURL + "war1/restricted/restricted.html");
        // assert the sso cookie has been created.
        SSOBaseCase.processSSOCookie(httpclient.getCookieStore(), baseURL.toString(), baseURL.toString());
        // assert access to the second application is allowed.
        SSOBaseCase.checkAccessAllowed(httpclient, baseURL + "war2/index.html");

        // perform a logout and assert access is not allowed anymore.
        HttpGet indexGet2 = new HttpGet(baseURL + "war1/TestServlet?operation=logout");
        response = httpclient.execute(indexGet2);

        statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Get OK(" + statusCode + ")", statusCode == HttpURLConnection.HTTP_OK);

        SSOBaseCase.checkAccessDenied(httpclient, baseURL + "war1/restricted/restricted.html");
        SSOBaseCase.checkAccessDenied(httpclient, baseURL + "war2/index.html");
    }
}
