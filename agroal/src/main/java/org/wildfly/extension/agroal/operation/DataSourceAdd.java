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

import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration.TransactionIsolation;
import io.agroal.api.configuration.ConnectionValidator;
import io.agroal.api.configuration.InterruptProtection;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.PathAddress;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.agroal.definition.DataSourceDefinition;
import org.wildfly.extension.agroal.service.DataSourceService;
import org.wildfly.extension.agroal.service.DriverService;

import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition.*;

/**
 * Handler responsible for adding a datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceAdd extends AbstractAddStepHandler {

    public static final DataSourceAdd INSTANCE = new DataSourceAdd();

    public static final ServiceName DATASOURCE_SERVICE_PREFIX = ServiceName.of( "wildfly", "agroal", "datasource" );

    private DataSourceAdd() {
    }

    @Override
    protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
        for ( AttributeDefinition attributeDefinition : DataSourceDefinition.INSTANCE.getAttributes() ) {
            attributeDefinition.validateAndSet( operation, model );
        }
    }

    private static AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        AgroalConnectionFactoryConfigurationSupplier configuration = new AgroalConnectionFactoryConfigurationSupplier();

        configuration.jdbcUrl( URL_ATTRIBUTE.resolveModelAttribute( context, model ).asString() );
        configuration.initialSql( NEW_CONNECTION_SQL_ATTRIBUTE.resolveModelAttribute( context, model ).asString() );

        if ( INTERRUPT_PROTECTION_ATTRIBUTE.resolveModelAttribute( context, model ).asBoolean( false ) ) {
            // TODO: JBoss threads interrupt protection
            configuration.interruptHandlingMode( InterruptProtection.none() );
        }

        if ( TRANSACTION_ISOLATION_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
            TransactionIsolation transactionIsolation = TransactionIsolation.valueOf( TRANSACTION_ISOLATION_ATTRIBUTE.resolveModelAttribute( context, model ).asString() );
            configuration.jdbcTransactionIsolation( transactionIsolation );
        }

        if ( CONNECTION_PROPERTIES_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
            for ( Property jdbcProperty : CONNECTION_PROPERTIES_ATTRIBUTE.resolveModelAttribute( context, model ).asPropertyList() ) {
                configuration.jdbcProperty( jdbcProperty.getName(), jdbcProperty.getValue().asString() );
            }
        }

        if ( SECURITY_USERNAME_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
            configuration.principal( new NamePrincipal( SECURITY_USERNAME_ATTRIBUTE.resolveModelAttribute( context, model ).asString() ) );
        }
        if ( SECURITY_PASSWORD_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
            configuration.credential( new SimplePassword( SECURITY_PASSWORD_ATTRIBUTE.resolveModelAttribute( context, model ).asString() ) );
        }

        return configuration;
    }

    private static AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
        AgroalConnectionPoolConfigurationSupplier configuration = new AgroalConnectionPoolConfigurationSupplier();

        configuration.maxSize( MAX_SIZE_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() );
        configuration.minSize( MIN_SIZE_ATTRIBUTE.resolveModelAttribute( context, model ).asInt( 0 ) );

        configuration.leakTimeout( ofMillis( LEAK_DETECTION_ATTRIBUTE.resolveModelAttribute( context, model ).asInt( 0 ) ) );
        configuration.acquisitionTimeout( ofMillis( BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.resolveModelAttribute( context, model ).asInt( 0 ) ) );
        configuration.validationTimeout( ofMillis( BACKGROUND_VALIDATION_ATTRIBUTE.resolveModelAttribute( context, model ).asInt( 0 ) ) );
        configuration.reapTimeout( ofMinutes( IDLE_REMOVAL_ATTRIBUTE.resolveModelAttribute( context, model ).asInt( 0 ) ) );

        configuration.connectionValidator( ConnectionValidator.defaultValidator() );

        // TODO: remove
        //configuration.preFillMode( AgroalConnectionPoolConfiguration.PreFillMode.MAX );

        return configuration;
    }

    @Override
    protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {

        String datasourceName = PathAddress.pathAddress( operation.require( OP_ADDR ) ).getLastElement().getValue();

        ModelNode factoryModel = CONNECTION_FACTORY_ATTRIBUTE.resolveModelAttribute( context, model );
        AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = connectionFactoryConfiguration( context, factoryModel );

        ModelNode poolModel = CONNECTION_POOL_ATTRIBUTE.resolveModelAttribute( context, model );
        AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration = connectionPoolConfiguration( context, poolModel );
        connectionPoolConfiguration.connectionFactoryConfiguration( connectionFactoryConfiguration );

        AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
        dataSourceConfiguration.connectionPoolConfiguration( connectionPoolConfiguration );

        String jndiName = JNDI_NAME_ATTRIBUTE.resolveModelAttribute( context, model ).asString();

        String driverName = DRIVER_ATTRIBUTE.resolveModelAttribute( context, factoryModel ).asString();
        ServiceName driverServiceName = ServiceName.of( DriverAdd.DRIVER_SERVICE_PREFIX, driverName );

        DataSourceService dataSourceService = new DataSourceService( datasourceName, jndiName, dataSourceConfiguration );
        ServiceName dataSourceServiceName = DATASOURCE_SERVICE_PREFIX.append( datasourceName );

        context.getServiceTarget().addService( dataSourceServiceName, dataSourceService )
                .addDependency( driverServiceName, DriverService.DriverClass.class, dataSourceService.getDriverServiceInjector() )
                .install();
    }
}
