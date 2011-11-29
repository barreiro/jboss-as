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
package org.jboss.as.test.integration.web.jsfintegration;

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
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of JSF integration into the JBoss server. This test requires than a web
 * container and JSF implementation be integrated into the JBoss server. The
 * tests currently do NOT use the java.net.HttpURLConnection and associated http
 * client and these do not return valid HTTP error codes so if a failure occurs
 * it is best to connect the webserver using a browser to look for additional
 * error info.
 * 
 * @author Stan.Silvert@jboss.org
 * @
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JSFIntegrationUnitTestCase {

    private static Logger log = Logger.getLogger(JSFIntegrationUnitTestCase.class);

    @ArquillianResource
    protected URL baseURL;

    @Deployment(name = "jbosstest-jsf.war", testable = false)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/jsfintegration/resources/";
        
//        <war destfile="${build.lib}/jbosstest-jsf.war"
//            webxml="${build.resources}/web/WEB-INF/jsftest-web.xml">
//            <fileset dir="${build.resources}/web/html/jsftest">
//              <include name="**/*.jsp"/>
//              <include name="**/*.xhtml"/>
//            </fileset>
//
//            <classes dir="${build.classes}">
//               <include name="org/jboss/test/jsf/webapp/**"/>
//            </classes>
//
//            <webinf dir="${build.resources}/web/html/jsftest/WEB-INF">
//              <include name="**/*"/>
//            </webinf>
//          </war>
          
        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-jsf.war");

        war.setWebXML(tccl.getResource(resourcesLocation + "jsftest-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jsftest/faces-config.xml"), "faces-config.xml");

        war.addAsWebResource(tccl.getResource(resourcesLocation + "jsftest/index.jsp"), "index.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "jsftest/beanvalidation.xhtml"), "beanvalidation.xhtml");
        
        war.addClass(InjectionBean.class);
        war.addClass(JBossColor.class);
        war.addClass(MySessionBean.class);
        war.addClass(ValidatedBean.class);
        
        log.info(war.toString(true));
        return war;
    }

    @Deployment(name = "bundled-myfaces-hellojsf.war", testable = false)
    public static WebArchive deploymentBundle() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/jsfintegration/resources/";
        
//        <!-- make WAR for testing legacy WARs that bundle MyFaces -->
//        <war destfile="${build.lib}/bundled-myfaces-hellojsf.war"
//          webxml="${build.resources}/web/WEB-INF/bundled-myfaces-jsf.xml">
//          <lib refid="apache-myfaces:myfaces-api:jar"/>
//          <lib refid="apache-myfaces:myfaces-impl:jar"/>
//          <!--<lib refid="javax.servlet:jstl:jar"/>-->
//          <lib refid="commons-beanutils:commons-beanutils:jar"/>
//          <lib refid="commons-codec:commons-codec:jar"/>
//          <lib refid="commons-collections:commons-collections:jar"/>
//          <lib refid="commons-digester:commons-digester:jar"/>
//          <lib refid="commons-lang:commons-lang:jar"/>
//          <lib refid="commons-el:commons-el:jar"/>
//
//          <fileset dir="${build.resources}/web/html/bundledmyfacesjsftest">
//            <include name="**/*.jsp"/>
//            <include name="**/*.html"/>
//          </fileset>
//
//          <webinf dir="${build.resources}/web/html/bundledmyfacesjsftest/WEB-INF">
//            <include name="**/*"/>
//          </webinf>
//     
//        </war>
        WebArchive war = ShrinkWrap.create(WebArchive.class, "bundled-myfaces-hellojsf.war");

        war.setWebXML(tccl.getResource(resourcesLocation + "bundled-myfaces-jsf.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "bundledmyfacesjsftest/faces-config.xml"), "faces-config.xml");

        war.addAsWebResource(tccl.getResource(resourcesLocation + "bundledmyfacesjsftest/index.html"), "index.html");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "bundledmyfacesjsftest/index.jsp"), "index.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "bundledmyfacesjsftest/finalgreeting.xhtml"), "finalgreeting.xhtml");
        
        war.addClass(InjectionBean.class);
        war.addClass(JBossColor.class);
        war.addClass(MySessionBean.class);
        war.addClass(ValidatedBean.class);

        log.info(war.toString(true));
        return war;
    }

    /**
     * Access the http://localhost/jbosstest-jsf/index.jsf.
     */
    @Test
    public void testJSFIntegrated() throws Exception {
        String responseBody = getResponseBody("jbosstest-jsf");

        assertTrue(responseBody.contains("@PostConstruct was called."));
        assertTrue(responseBody.contains("@PreDestroy was called."));
        assertTrue(responseBody.contains("Datasource was injected."));

        // Tests JSF/JSTL integration
        assertTrue(responseBody.contains("number one"));
        assertTrue(responseBody.contains("number two"));
        assertTrue(responseBody.contains("number three"));

        // Tests enum support
        assertTrue(responseBody.contains("JBoss Color selection is PURPLE"));
    }

    public void testJSFAppWithBundledMyFaces() throws Exception {
//        WebConversation webConversation = new WebConversation();
//
//        // Initial JSF request
//        WebRequest req = new GetMethodWebRequest(baseURL + "bundled-myfaces-hellojsf/index.faces");
//        WebResponse webResponse = webConversation.getResponse(req);
//        assertTrue(webResponse.getText().contains("Enter your name"));
//
//        // submit data
//        WebForm form = webResponse.getFormWithID("form1");
//        form.setParameter("form1:input_foo_text", "Stan");
//        SubmitButton submitButton = form.getSubmitButtonWithID("form1:submit_button");
//        webResponse = form.submit(submitButton);
//        assertTrue(webResponse.getText().contains("Hello Stan"));
    }

    public void testBeanValidationIntegratedWithJSF() throws Exception {
//        WebConversation wc = new WebConversation();
//        String url = makeRequestString("jbosstest-jsf", "/beanvalidation.jsf");
//        WebResponse response = wc.getResponse(url);
//        WebForm form = response.getFormWithID("form1");
//        form.setParameter("form1:input_name", "a");
//        SubmitButton submitButton = form.getSubmitButtonWithID("form1:submit_button");
//        response = form.submit(submitButton);
//        assertTrue(response.getText().contains("size must be between 2 and"));
    }

    private String getResponseBody(String warName) throws Exception {
        HttpGet httpget = makeRequest(warName);
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("Executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);

        return EntityUtils.toString(response.getEntity());
    }

    // makes default GetMethod request for index.jsf
    private HttpGet makeRequest(String warName) {
        return new HttpGet(makeRequestString(warName, "index.jsf"));
    }

    private String makeRequestString(String warName, String jsfPage) {
        return baseURL + warName + "/" + jsfPage;
    }

}
