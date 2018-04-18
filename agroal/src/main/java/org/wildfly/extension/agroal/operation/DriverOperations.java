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
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.modules.Module;
import org.jboss.modules.ModuleLoadException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.agroal.definition.DriverDefinition;
import org.wildfly.extension.agroal.logging.AgroalLogger;
import org.wildfly.extension.agroal.service.DriverService;

import java.lang.reflect.Method;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.agroal.definition.DriverDefinition.CLASS_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.DriverDefinition.MODULE_ATTRIBUTE;

/**
 * Handler responsible for adding a driver resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DriverOperations {

    public static final ServiceName DRIVER_SERVICE_PREFIX = ServiceName.of( "wildfly", "agroal", "jdbc-driver" );

    // --- //

    public static final OperationStepHandler ADD_OPERATION = new DriverAdd();

    public static final OperationStepHandler REMOVE_OPERATION = new DriverRemove();

    public static final OperationStepHandler INFO_OPERATION = new DriverInfo();

    // --- //

    private static class DriverAdd extends AbstractAddStepHandler {

        private static Class<?> loadClass(String driverName, String moduleName, String className) throws OperationFailedException {
            try {
                Module module = Module.getCallerModuleLoader().loadModule( moduleName );
                Class<?> providerClass = module.getClassLoader().loadClass( className );
                AgroalLogger.DRIVER_LOGGER.debugf( "loaded module '%s' for driver: %s", moduleName, driverName );
                return providerClass;
            } catch ( ModuleLoadException e ) {
                throw new OperationFailedException( "failed to load module '" + moduleName + "'", e );
            } catch ( ClassNotFoundException e ) {
                throw new OperationFailedException( "failed to load class '" + className + "'", e );
            }
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
            ServiceName driverServiceName = ServiceName.of( DRIVER_SERVICE_PREFIX, driverName );
            DriverService driverService = DriverService.DRIVER_WITH_NO_PROVIDER;

            if ( CLASS_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
                String moduleName = MODULE_ATTRIBUTE.resolveModelAttribute( context, model ).asString();
                String className = CLASS_ATTRIBUTE.resolveModelAttribute( context, model ).asString();

                driverService = new DriverService( loadClass( driverName, moduleName, className ) );
            }
            context.getServiceTarget().addService( driverServiceName, driverService ).install();
        }
    }

    // --- //

    public static class DriverRemove extends AbstractRemoveStepHandler {
        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String driverName = PathAddress.pathAddress( operation.require( OP_ADDR ) ).getLastElement().getValue();

            ServiceName driverServiceName = ServiceName.of( DRIVER_SERVICE_PREFIX, driverName );
            context.removeService( driverServiceName );

            AgroalLogger.DRIVER_LOGGER.debugf( "unloaded driver: %s", driverName );
        }
    }

    // --- //

    private static class DriverInfo implements OperationStepHandler {

        @Override
        public void execute(OperationContext context, ModelNode operation) throws OperationFailedException {
            if ( context.isNormalServer() ) {

                String driverName = context.getCurrentAddressValue();
                Service<?> driverService = context.getServiceRegistry( false ).getService( DRIVER_SERVICE_PREFIX.append( driverName ) ).getService();
                Class<?> providerClass = ( (DriverService.ProviderClass) driverService.getValue() ).providerClass();

                if ( providerClass == null ) {
                    context.getResult().set( new ModelNode() );
                    return;
                }

                ModelNode result = new ModelNode();

                // consistent with io.agroal.pool.util.PropertyInjector
                if ( javax.sql.XADataSource.class.isAssignableFrom( providerClass ) || javax.sql.DataSource.class.isAssignableFrom( providerClass ) ) {
                    for ( Method method : providerClass.getMethods() ) {
                        String name = method.getName();
                        if ( method.getParameterCount() == 1 && name.startsWith( "set" ) ) {
                            result.get( name.substring( 3 ) ).set( method.getParameterTypes()[0].getName() );
                        }
                    }
                }

                // from java.sql.Driver maybe use Driver.getPropertyInfo

                context.getResult().set( result );
            }
        }
    }
}
