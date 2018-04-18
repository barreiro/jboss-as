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

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.AgroalConnectionFactoryConfiguration;
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.api.security.NamePrincipal;
import io.agroal.api.security.SimplePassword;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.agroal.definition.XADataSourceDefinition;
import org.wildfly.extension.agroal.service.DataSourceService;
import org.wildfly.extension.agroal.service.DriverService;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import static io.agroal.api.configuration.AgroalConnectionPoolConfiguration.ConnectionValidator.defaultValidator;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofMinutes;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.agroal.definition.XADataSourceDefinition.*;

/**
 * Operations for adding and removing an xa-datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class XADataSourceOperations extends AbstractAddStepHandler {

    public static final String XADATASOURCE_SERVICE_NAME = "xa-datasource";

    public static final ServiceName XADATASOURCE_SERVICE_PREFIX = ServiceName.of( "wildfly", "agroal", "xa-datasource" );

    // --- //

    public static final OperationStepHandler ADD_OPERATION = new XADataSourceAdd();

    public static final OperationStepHandler REMOVE_OPERATION = new XADataSourceRemove();

    // --- //

    private static class XADataSourceAdd extends AbstractAddStepHandler {

        private static AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration(OperationContext context, ModelNode model) throws OperationFailedException {
            AgroalConnectionFactoryConfigurationSupplier configuration = new AgroalConnectionFactoryConfigurationSupplier();

            if ( URL_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
                configuration.jdbcUrl( URL_ATTRIBUTE.resolveModelAttribute( context, model ).asString() );
            }

            if ( NEW_CONNECTION_SQL_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
                configuration.initialSql( NEW_CONNECTION_SQL_ATTRIBUTE.resolveModelAttribute( context, model ).asString() );
            }

            if ( TRANSACTION_ISOLATION_ATTRIBUTE.resolveModelAttribute( context, model ).isDefined() ) {
                AgroalConnectionFactoryConfiguration.TransactionIsolation transactionIsolation = AgroalConnectionFactoryConfiguration.TransactionIsolation.valueOf( TRANSACTION_ISOLATION_ATTRIBUTE.resolveModelAttribute( context, model ).asString() );
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
            configuration.minSize( MIN_SIZE_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() );
            configuration.initialSize( INITIAL_SIZE_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() );

            configuration.acquisitionTimeout( ofMillis( BLOCKING_TIMEOUT_MILLIS_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() ) );
            configuration.leakTimeout( ofMillis( LEAK_DETECTION_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() ) );
            configuration.validationTimeout( ofMillis( BACKGROUND_VALIDATION_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() ) );
            configuration.reapTimeout( ofMinutes( IDLE_REMOVAL_ATTRIBUTE.resolveModelAttribute( context, model ).asInt() ) );
            configuration.connectionValidator( defaultValidator() );

            return configuration;
        }

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for ( AttributeDefinition attributeDefinition : XADataSourceDefinition.INSTANCE.getAttributes() ) {
                attributeDefinition.validateAndSet( operation, model );
            }
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
            dataSourceConfiguration.metricsEnabled( STATISTICS_ENABLED_ATTRIBUTE.resolveModelAttribute( context, model ).asBoolean() );

            String jndiName = JNDI_NAME_ATTRIBUTE.resolveModelAttribute( context, model ).asString();

            String driverName = DRIVER_ATTRIBUTE.resolveModelAttribute( context, factoryModel ).asString();
            ServiceName driverServiceName = ServiceName.of( DriverOperations.DRIVER_SERVICE_PREFIX, driverName );

            DataSourceService dataSourceService = new DataSourceService( datasourceName, jndiName, false, true, dataSourceConfiguration );
            ServiceName dataSourceServiceName = XADATASOURCE_SERVICE_PREFIX.append( datasourceName );

            ServiceBuilder<AgroalDataSource> serviceBuilder = context.getServiceTarget().addService( dataSourceServiceName, dataSourceService );
            serviceBuilder.addDependency( driverServiceName, DriverService.ProviderClass.class, dataSourceService.getDriverServiceInjector() );

            // Define JTA dependencies (required for XA)
            serviceBuilder.addDependency( TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, dataSourceService.getTransactionManagerInjector() );
            serviceBuilder.addDependency( TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, dataSourceService.getTransactionSynchronizationRegistryInjector() );

            serviceBuilder.install();
        }
    }

    // --- //

    private static class XADataSourceRemove extends AbstractRemoveStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress( operation.require( OP_ADDR ) ).getLastElement().getValue();
            ServiceName datasourceServiceName = ServiceName.of( XADATASOURCE_SERVICE_PREFIX, datasourceName );
            context.removeService( datasourceServiceName );
        }
    }
}
