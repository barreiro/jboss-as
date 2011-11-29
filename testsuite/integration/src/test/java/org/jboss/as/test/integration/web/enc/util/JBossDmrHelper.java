/*
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Inc., and individual contributors as indicated
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
package org.jboss.as.test.integration.web.enc.util;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.REMOVE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.BINDING_TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.LOOKUP;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.SIMPLE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.TYPE;
import static org.jboss.as.naming.subsystem.NamingSubsystemModel.VALUE;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.as.controller.client.ModelControllerClient;
import org.jboss.as.controller.client.OperationBuilder;
import org.jboss.dmr.ModelNode;
import org.jboss.logging.Logger;

/**
 * Helper classe that uses jboss-dmr to setup the proper test environment
 * 
 * @author barreiro
 */
public class JBossDmrHelper {

    private static Logger log = Logger.getLogger(JBossDmrHelper.class);

    public static void setupNaming() {
        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            final List<ModelNode> updates = new ArrayList<ModelNode>();
            
            updates.addAll(addJndiLookup(mcc, "java:/MockDS", "java:jboss/datasources/ExampleDS"));
            updates.addAll(addJndiBinding(mcc, "java:global/0", 0));
            updates.addAll(addJndiBinding(mcc, "java:global/1", 1));
            updates.addAll(addJndiBinding(mcc, "java:global/0.0", 0.0f));
            updates.addAll(addJndiBinding(mcc, "java:global/1.1", 1.1f));
            updates.addAll(addJndiBinding(mcc, "java:global/String0", "String0"));
            updates.addAll(addJndiBinding(mcc, "java:global/String1", "String1"));
            
            applyUpdates(updates, mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    public static void removeNaming() {
        try {
            ModelControllerClient mcc = ModelControllerClient.Factory.create(InetAddress.getByName("localhost"), 9999);
            final List<ModelNode> updates = new ArrayList<ModelNode>();
            
            updates.addAll(removeJndi(mcc, "java:/MockDS"));
            updates.addAll(removeJndi(mcc, "java:global/0"));
            updates.addAll(removeJndi(mcc, "java:global/1"));
            updates.addAll(removeJndi(mcc, "java:global/0.0"));
            updates.addAll(removeJndi(mcc, "java:global/1.1"));
            updates.addAll(removeJndi(mcc, "java:global/String0"));
            updates.addAll(removeJndi(mcc, "java:global/String1"));
            
            applyUpdates(updates, mcc);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }
    }

    /**
     * Adds a JNDI binding to an int
     */
    private static List<ModelNode> addJndiBinding(ModelControllerClient client, String name, int value) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(BINDING_TYPE).set(SIMPLE);
        op.get(VALUE).set(value);
        op.get(TYPE).set("int");

        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "naming");
        address.add(BINDING, name);

        return Arrays.asList(op);
    }
    
    /**
     * Adds a JNDI binding to a float
     */
    private static List<ModelNode> addJndiBinding(ModelControllerClient client, String name, float value) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(BINDING_TYPE).set(SIMPLE);
        op.get(VALUE).set(value);
        op.get(TYPE).set("float");

        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "naming");
        address.add(BINDING, name);

        return Arrays.asList(op);
    }
    
    /**
     * Adds a JNDI binsing to a string
     */
    private static List<ModelNode> addJndiBinding(ModelControllerClient client, String name, String value) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(BINDING_TYPE).set(SIMPLE);
        op.get(VALUE).set(value);
        op.get(TYPE).set(String.class.getName());

        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "naming");
        address.add(BINDING, name);

        return Arrays.asList(op);
    }
    
    /**
     * Adds a JNDI lookup to another name (some sort of alias)
     */
    private static List<ModelNode> addJndiLookup(ModelControllerClient client, String name, String target) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(ADD);
        op.get(BINDING_TYPE).set(LOOKUP);
        op.get(LOOKUP).set(target);

        ModelNode address = op.get(OP_ADDR);
        address.add(SUBSYSTEM, "naming");
        address.add(BINDING, name);

        return Arrays.asList(op);
    }

    /**
     * Remove a JNDI binding
     */
    private static List<ModelNode> removeJndi(ModelControllerClient client, String name) throws Exception {
        ModelNode op = new ModelNode();
        op.get(OP).set(REMOVE);
        op.get(OP_ADDR).add(SUBSYSTEM, "naming");
        op.get(OP_ADDR).add(BINDING, name);

        return Arrays.asList(op);
    }

    private static void applyUpdates(final List<ModelNode> updates, final ModelControllerClient client) throws Exception {
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
}
