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
package org.jboss.as.test.integration.web.enc;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.junit.Assert.assertTrue;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jboss.arquillian.container.test.api.Deployment;
import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.as.test.integration.web.enc.servlet.StandaloneENCAnnotationsServlet;
import org.jboss.as.test.integration.web.enc.util.ENCServiceActivator;
import org.jboss.as.test.integration.web.enc.util.ENCTesterFirst;
import org.jboss.as.test.integration.web.enc.util.JBossDmrHelper;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.msc.service.ServiceActivator;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.asset.StringAsset;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Tests of a web app java:comp/env jndi settings specified via javaee5
 * annotations
 * 
 * @author Scott.Stark@jboss.org
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class EncAnnotationsUnitTestCase {
    
    private static Logger log = Logger.getLogger(EncAnnotationsUnitTestCase.class);

    private String baseURL = "http://localhost:8080/";
    
    @Deployment(name = "simple-annonly.war", testable = false)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/enc/resources/";
        String mocksLocation = "org/jboss/as/test/integration/web/enc/mock";
        
        JBossDmrHelper.setupNaming();
        
    // return getDeploySetup(EncAnnotationsUnitTestCase.class,
    // "simple-mock.beans,simple-annonly.war");
        
//        <jar destfile="${build.lib}/simple-mock.beans">
//        <zipfileset dir="${build.resources}/web/simple-xmlonly"
//           fullpath="META-INF/jboss-beans.xml">
//           <include name="encbinding-jboss-beans.xml"/>            
//        </zipfileset>
//        <fileset dir="${build.classes}">
//           <include name="org/jboss/test/web/mock/**"/>
//        </fileset>         
//     </jar>
        
//      try {
//      ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
//      addJNDIbinding(mcc, "java:/MockDS", "java:jboss/datasources/ExampleDS");
//  } catch (Exception e) {
//      log.error(e.getMessage(), e);
//  }
    
//        <war destfile="${build.lib}/simple-annonly.war"
//            webxml="${build.resources}/web/simple-xmlonly/annotated-web.xml">
//            <classes dir="${build.classes}">
//               <include name="org/jboss/test/web/servlets/StandaloneENCAnnotationsServlet.class"/>
//               <include name="org/jboss/test/web/servlets/ENCTesterFirst.class"/>
//            </classes>
//           <webinf dir="${build.resources}/web/simple-xmlonly">
//               <include name="jboss-service.xml"/>
//           </webinf>
//         </war>
         
        WebArchive war = ShrinkWrap.create(WebArchive.class, "simple-annonly.war");
        
        war.setWebXML(tccl.getResource(resourcesLocation + "annotated-web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-service.xml"), "jboss-service.xml");
        war.addClass(ENCTesterFirst.class);
        war.addClass(StandaloneENCAnnotationsServlet.class);
        war.addPackage(mocksLocation);
        
        log.info(war.toString(true));
        return war;
    }

    @AfterClass
    public static void undeployment() {
        JBossDmrHelper.removeNaming();
    }
    
    /**
     * Access the http://{host}/simple-annonly/ENCServlet
     */
    @Test
    public void testAltRequestInfoServlet() throws Exception {
        testURL(new URL(baseURL + "simple-annonly/ENCServlet"));
    }

    private void testURL(URL url) throws Exception {
        HttpGet httpget = new HttpGet(url.toURI());
        DefaultHttpClient httpclient = new DefaultHttpClient();

        log.info("Executing request" + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-Exception");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-Exception(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
    }
}
