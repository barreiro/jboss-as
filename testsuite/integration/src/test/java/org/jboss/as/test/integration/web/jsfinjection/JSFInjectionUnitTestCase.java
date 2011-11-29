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
package org.jboss.as.test.integration.web.jsfinjection;

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
import java.net.InetAddress;
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
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.JavaArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * JBAS-4399. Test correct injection into a JSF managed beans.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class JSFInjectionUnitTestCase {
    
    private static Logger log = Logger.getLogger(JSFInjectionUnitTestCase.class);

    protected final String baseURL = "http://localhost:8080/";

    @Deployment(name = "jbosstest-jsf-injection.ear", testable = false)
    public static EnterpriseArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/jsfinjection/resources/";
        String ejbLocation = "org/jboss/as/test/integration/web/jsfinjection/ejb3";
        String mocksLocation = "org/jboss/as/test/integration/web/jsfinjection/mock";

        // return getDeploySetup(JSFInjectionUnitTestCase.class,
        // "simple-mock.beans,jbosstest-jsf-injection.ear");
        
        // <jar destfile="${build.lib}/simple-mock.beans">
        // <zipfileset dir="${build.resources}/web/simple-xmlonly"
        // fullpath="META-INF/jboss-beans.xml">
        // <include name="encbinding-jboss-beans.xml"/>
        // </zipfileset>
        // <fileset dir="${build.classes}">
        // <include name="org/jboss/test/web/mock/**"/>
        // </fileset>
        // </jar>

//        try {
//            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
//            addJNDIbinding(mcc, "java:/MockDS", "java:jboss/datasources/ExampleDS");
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
        
//        <war destfile="${build.lib}/jbosstest-jsf-injection.war"
//            webxml="${build.resources}/web/jsfinjection/WEB-INF/web.xml">
//            <fileset dir="${build.resources}/web/jsfinjection">
//              <include name="**/*.jsp"/>
//            </fileset>
//
//            <classes dir="${build.classes}">
//               <include name="org/jboss/test/jsf/managed/**"/>
//            </classes>
//
//            <webinf dir="${build.resources}/web/jsfinjection/WEB-INF">
//              <include name="faces-config.xml"/>
//              <include name="jboss-service.xml"/>          
//            </webinf>
//          </war>

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbosstest-jsf-injection.war");

        war.setWebXML(tccl.getResource(resourcesLocation + "web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "faces-config.xml"), "faces-config.xml");
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-service.xml"), "jboss-service.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "enc.jsp"), "enc.jsp");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "simple.jsp"), "simple.jsp");
        war.addClass(ENCAnnotationsManagedBean.class);
        war.addClass(ENCTester.class);
        war.addClass(SimpleManagedBean.class);

//        <war destfile="${build.lib}/jbosstest-alternate-jsf-injection.war"
//            webxml="${build.resources}/web/jsfinjection/WEB-INF/alternate-injection-provider.xml">
//            <fileset dir="${build.resources}/web/jsfinjection">
//              <include name="**/*.jsp"/>
//            </fileset>
//
//            <classes dir="${build.classes}">
//               <include name="org/jboss/test/jsf/managed/**"/>
//            </classes>
//
//            <webinf dir="${build.resources}/web/jsfinjection/WEB-INF">
//              <include name="faces-config.xml"/>
//            </webinf>
//          </war>

        WebArchive alternateWar = ShrinkWrap.create(WebArchive.class, "jbosstest-alternate-jsf-injection.war");
        
        alternateWar.setWebXML(tccl.getResource(resourcesLocation + "alternate-injection-provider.xml"));
        alternateWar.addAsWebInfResource(tccl.getResource(resourcesLocation + "faces-config.xml"), "faces-config.xml");
        alternateWar.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-service.xml"), "jboss-service.xml");
        alternateWar.addAsWebResource(tccl.getResource(resourcesLocation + "enc.jsp"), "enc.jsp");
        alternateWar.addAsWebResource(tccl.getResource(resourcesLocation + "simple.jsp"), "simple.jsp");
        alternateWar.addClass(ENCAnnotationsManagedBean.class);
        alternateWar.addClass(ENCTester.class);
        alternateWar.addClass(SimpleManagedBean.class);
        
//        <jar destfile="${build.lib}/jbosstest-jsf-ejbs.jar">
//        <fileset dir="${build.classes}">
//           <include name="org/jboss/test/web/ejb3/**"/>
//        </fileset>
//        </jar>
        
        JavaArchive ejbJar = ShrinkWrap.create(JavaArchive.class, "jbosstest-jsf-ejbs.jar");

        ejbJar.addPackage(ejbLocation);
        ejbJar.addPackage(mocksLocation);
//        
//      <ear earfile="${build.lib}/jbosstest-jsf-injection.ear"
//      appxml="${build.resources}/web/jsfinjection/META-INF/application.xml">
//      <fileset dir="${build.lib}">
//         <include name="jbosstest-jsf-ejbs.jar"/>
//         <include name="jbosstest-jsf-injection.war"/>
//         <include name="jbosstest-alternate-jsf-injection.war"/>
//      </fileset>
//   </ear>
        
        EnterpriseArchive ear = ShrinkWrap.create(EnterpriseArchive.class, "jbosstest-jsf-injection.ear");
        ear.addAsApplicationResource(tccl.getResource(resourcesLocation + "application.xml"), "application.xml");
        ear.addAsLibrary(ejbJar);
        ear.addAsModule(war);
        ear.addAsModule(alternateWar);
    
        log.info(ear.toString(true));
        log.info(ejbJar.toString(true));
        log.info(war.toString(true));
        log.info(alternateWar.toString(true));
        return ear;
    }

    @AfterClass
    public static void undeployment() {
//        try {
//            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
//            removeJNDIbinding(mcc, "java:/MockDS");
//        } catch (Exception e) {
//            log.error(e.getMessage(), e);
//        }
    }
    
    private static void addJNDIbinding(ModelControllerClient client, String name, String target) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(BINDING_TYPE).set(LOOKUP);
        op.get(LOOKUP).set(target);
        
        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "naming");
        address.add(BINDING, name);
        
        updates.add(op);

        applyUpdates(updates, client);
    }
    
    private static void removeJNDIbinding(ModelControllerClient client, String name) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "naming");
        op.get(OP_ADDR).add(BINDING, name);
        
        updates.add(op);

        applyUpdates(updates, client);
    }
    
    public static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
        for (ModelNode update : updates) {
            log.info("+++ Update on " + client + ":\n" + update.toString());
            ModelNode result = client.execute(new OperationBuilder(update).build());
            if (result.hasDefined("outcome") && "success".equals(result.get("outcome").asString())) {
                if (result.hasDefined("result"))
                    log.info(result.get("result"));
            } else if (result.hasDefined("failure-description")) {
                throw new RuntimeException(result.get("failure-description").toString());
            } else {
                throw new RuntimeException("Operation not successful; outcome = " + result.get("outcome"));
            }
        }
    }

    @Test
    public void testSimpleJSF() throws Exception {
        testURL(new URL(baseURL + "jsfinjection/simple.jsf"));
    }

    @Test
    public void testENCJSF() throws Exception {
        testURL(new URL(baseURL + "jsfinjection/enc.jsf"));
    }

    @Test
    public void testAlternateJSF() throws Exception {
        testURL(new URL(baseURL + "alternate-jsf-injection/simple.jsf"));
    }

    @Test
    public void testAlternameENCJSF() throws Exception {
        testURL(new URL(baseURL + "alternate-jsf-injection/enc.jsf"));
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
