/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
package org.wildfly.extension.agroal.operation;

import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.agroal.definition.DriverDefinition;
import org.wildfly.extension.agroal.logging.AgroalLogger;
import org.wildfly.extension.agroal.service.DriverService;

import java.sql.Driver;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.agroal.definition.DriverDefinition.DRIVER_CLASS_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.DriverDefinition.MODULE_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.DriverDefinition.SLOT_ATTRIBUTE;

/**
 * Handler responsible for adding a driver resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DriverAdd extends AbstractAddStepHandler {

    public static final DriverAdd INSTANCE = new DriverAdd();

    public static final ServiceName DRIVER_SERVICE_PREFIX = ServiceName.of( "wildfly", "agroal", "jdbc-driver" );

    private DriverAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for ( AttributeDefinition attributeDefinition : DriverDefinition.INSTANCE.getAttributes() ) {
            attributeDefinition.validateAndSet( operation, model );
        }
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
        String driverName = PathAddress.pathAddress( operation.require( OP_ADDR ) ).getLastElement().getValue();

        String driverClassName = DRIVER_CLASS_ATTRIBUTE.resolveModelAttribute( context, model ).asString();
        String moduleName = MODULE_ATTRIBUTE.resolveModelAttribute( context, model ).asString();
        String slotName = SLOT_ATTRIBUTE.resolveModelAttribute( context, model ).asString( "main" );

        Class<? extends Driver> driverClass;
        try {
            Module module = Module.getCallerModuleLoader().loadModule( moduleName + ":" + slotName );
            driverClass = module.getClassLoader().loadClass( driverClassName ).asSubclass( Driver.class );
            AgroalLogger.DRIVER_LOGGER.debugf( "loaded module '%s:%s' for driver: %s", moduleName, slotName, driverName );
        } catch ( ModuleLoadException e ) {
            throw new OperationFailedException( "failed to load module '" + moduleName + ":" + slotName + "'", e );
        } catch ( ClassNotFoundException e ) {
            throw new OperationFailedException( "failed to load class '" + driverClassName + "'", e );
        } catch ( ClassCastException e ) {
            throw new OperationFailedException( "class '" + driverClassName + "' is not a JDBC driver", e );
        }

        ServiceName driverServiceName = ServiceName.of( DRIVER_SERVICE_PREFIX, driverName );
        DriverService driverService = new DriverService( driverClass );
        context.getServiceTarget().addService( driverServiceName, driverService ).install();
    }
}
