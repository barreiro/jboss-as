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
package org.jboss.as.test.integration.web.virtualhost;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.NAME;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.VALUE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.WRITE_ATTRIBUTE_OPERATION;
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
import org.jboss.arquillian.test.api.ArquillianResource;
import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;
import org.jboss.shrinkwrap.api.ShrinkWrap;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test virtual hosts.
 * 
 * @author <a href="mailto:emuckenh@redhat.com">Emanuel Muckenhuber</a>
 * @author lbarreiro@redhat.com
 */
@RunWith(Arquillian.class)
@RunAsClient
public class VirtualHostTestCase {

    private static Logger log = Logger.getLogger(VirtualHostTestCase.class);

    @ArquillianResource
    private URL baseURL;

    // FIXME Duplicated from org.jboss.as.web.Constants.Constants;
    private static String ALIAS = "alias";
    private static String VIRTUAL_SERVER = "virtual-server";

    /** The web ctx url. */
    private static final String webURL = "index.html";

    @Deployment(name = "jbossweb-virtual-host.war", testable = false)
    public static WebArchive deployment() {
        ClassLoader tccl = Thread.currentThread().getContextClassLoader();
        String resourcesLocation = "org/jboss/as/test/integration/web/virtualhost/resources/";

        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            createVirtualServer(mcc, "virtual.host", "virtual.alias");
            dumpWebSubsystem(mcc);
            Thread.sleep(5 * 1000l);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        WebArchive war = ShrinkWrap.create(WebArchive.class, "jbossweb-virtual-host.war");
        war.setWebXML(tccl.getResource(resourcesLocation + "web.xml"));
        war.addAsWebInfResource(tccl.getResource(resourcesLocation + "jboss-web.xml"), "jboss-web.xml");
        war.addAsWebResource(tccl.getResource(resourcesLocation + "index.html"), "index.html");

        log.info(war.toString());
        return war;
    }

    @AfterClass
    public static void undeployment() {
        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            removeVirtualServer(mcc, "virtual.host");
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    private static void createVirtualServer(ModelControllerClient client, String serverName, String... alias)
            throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add(VIRTUAL_SERVER, serverName);
        updates.add(op);
        
        if (alias.length > 0) {
            final List<ModelNode> aliasNodes = new ArrayList<ModelNode>();
            for (String a : alias) {
                aliasNodes.add(new ModelNode(a));
            }
            
            ModelNode aliasOp = new ModelNode();
            aliasOp.get(OP).set(WRITE_ATTRIBUTE_OPERATION);
            aliasOp.get(OP_ADDR).add(SUBSYSTEM, "web");
            aliasOp.get(OP_ADDR).add(VIRTUAL_SERVER, serverName);
            aliasOp.get(NAME).set(ALIAS);
            aliasOp.get(VALUE).set(aliasNodes);
            updates.add(aliasOp);
        }  

        applyUpdates(updates, client);
    }

    public static void removeVirtualServer(final ModelControllerClient client, String serverName) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get(OP_ADDR).add(VIRTUAL_SERVER, serverName);
        updates.add(op);

        applyUpdates(updates, client);
    }

    public static void dumpWebSubsystem(final ModelControllerClient client) throws Exception {
        final List<ModelNode> updates = new ArrayList<ModelNode>();

        ModelNode op = new ModelNode();
        op.get(OP).set("read-resource");
        op.get(OP_ADDR).add(SUBSYSTEM, "web");
        op.get("recursive").set(true);
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

    /**
     * Test the virtual.host
     */
    @Test
    public void testVirtualHost() throws Exception {
        URL url = new URL(baseURL + webURL);
        testHost(url, "virtual.host");
    }

    /**
     * Test the virtual.alias
     */
    @Test
    public void testVirtualAlias() throws Exception {
        URL url = new URL(baseURL + webURL);
        testHost(url, "virtual.alias");
    }

    /**
     * Check the response of the server based on a virtual host.
     * 
     * @param url the url
     * @param virtualHost the virtual host
     * @throws Exception
     */
    private void testHost(URL url, String virtualHost) throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        HttpGet httpget = new HttpGet(url.toString());
        httpget.setHeader("Host", virtualHost);

        log.info("Executing request " + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        Header[] errorHeaders = response.getHeaders("X-NoJException");
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_OK);
        assertTrue("X-NoJException(" + Arrays.toString(errorHeaders) + ") is null", errorHeaders.length == 0);
    }

    /**
     * Test the non existing web app.
     */
    @Test
    public void testNormalHost() throws Exception {
        DefaultHttpClient httpclient = new DefaultHttpClient();

        HttpGet httpget = new HttpGet(baseURL + webURL);

        log.info("Executing request " + httpget.getRequestLine());
        HttpResponse response = httpclient.execute(httpget);

        int statusCode = response.getStatusLine().getStatusCode();
        assertTrue("Wrong response code: " + statusCode, statusCode == HttpURLConnection.HTTP_NOT_FOUND);
    }
}
