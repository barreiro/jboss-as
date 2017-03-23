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
import io.agroal.api.configuration.supplier.AgroalConnectionFactoryConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalConnectionPoolConfigurationSupplier;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import org.jboss.as.controller.AbstractAddStepHandler;
import org.jboss.as.controller.AbstractRemoveStepHandler;
import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.OperationContext;
import org.jboss.as.controller.OperationFailedException;
import org.jboss.as.controller.OperationStepHandler;
import org.jboss.as.controller.PathAddress;
import org.jboss.as.txn.service.TxnServices;
import org.jboss.dmr.ModelNode;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.wildfly.extension.agroal.definition.DataSourceDefinition;
import org.wildfly.extension.agroal.service.DataSourceService;
import org.wildfly.extension.agroal.service.DriverService;

import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition.CONNECTION_FACTORY_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition.CONNECTION_POOL_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition.DRIVER_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition.JNDI_NAME_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.AbstractDataSourceDefinition.STATISTICS_ENABLED_ATTRIBUTE;
import static org.wildfly.extension.agroal.definition.DataSourceDefinition.CONNECTABLE_ATTRIBUTE;

/**
 * Operations for adding and removing a datasource resource to the model
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceOperations {

    public static final String DATASOURCE_SERVICE_NAME = "datasource";

    public static final ServiceName DATASOURCE_SERVICE_PREFIX = ServiceName.of( "wildfly", "agroal", DATASOURCE_SERVICE_NAME );

    // --- //

    public static final OperationStepHandler ADD_OPERATION = new DataSourceAdd();

    public static final OperationStepHandler REMOVE_OPERATION = new DataSourceRemove();

    // --- //

    private static class DataSourceAdd extends AbstractAddStepHandler {

        @Override
        protected void populateModel(ModelNode operation, ModelNode model) throws OperationFailedException {
            for ( AttributeDefinition attributeDefinition : DataSourceDefinition.INSTANCE.getAttributes() ) {
                attributeDefinition.validateAndSet( operation, model );
            }
        }

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress( operation.require( OP_ADDR ) ).getLastElement().getValue();

            ModelNode factoryModel = CONNECTION_FACTORY_ATTRIBUTE.resolveModelAttribute( context, model );
            AgroalConnectionFactoryConfigurationSupplier connectionFactoryConfiguration = AbstractDataSourceOperations.connectionFactoryConfiguration( context, factoryModel );

            ModelNode poolModel = CONNECTION_POOL_ATTRIBUTE.resolveModelAttribute( context, model );
            AgroalConnectionPoolConfigurationSupplier connectionPoolConfiguration = AbstractDataSourceOperations.connectionPoolConfiguration( context, poolModel );
            connectionPoolConfiguration.connectionFactoryConfiguration( connectionFactoryConfiguration );

            AgroalDataSourceConfigurationSupplier dataSourceConfiguration = new AgroalDataSourceConfigurationSupplier();
            dataSourceConfiguration.connectionPoolConfiguration( connectionPoolConfiguration );
            dataSourceConfiguration.metricsEnabled( STATISTICS_ENABLED_ATTRIBUTE.resolveModelAttribute( context, model ).asBoolean() );

            String jndiName = JNDI_NAME_ATTRIBUTE.resolveModelAttribute( context, model ).asString();
            boolean connectable = CONNECTABLE_ATTRIBUTE.resolveModelAttribute( context, model ).asBoolean();

            String driverName = DRIVER_ATTRIBUTE.resolveModelAttribute( context, factoryModel ).asString();
            ServiceName driverServiceName = ServiceName.of( DriverOperations.DRIVER_SERVICE_PREFIX, driverName );

            DataSourceService dataSourceService = new DataSourceService( datasourceName, jndiName, connectable, false, dataSourceConfiguration );
            ServiceName dataSourceServiceName = DATASOURCE_SERVICE_PREFIX.append( datasourceName );

            ServiceBuilder<AgroalDataSource> serviceBuilder = context.getServiceTarget().addService( dataSourceServiceName, dataSourceService );
            serviceBuilder.addDependency( driverServiceName, DriverService.ProviderClass.class, dataSourceService.getDriverServiceInjector() );

            // Define dependencies for JTA DataSources
            if ( DataSourceDefinition.JTA_ATTRIBUTE.resolveModelAttribute( context, model ).asBoolean() ) {
                serviceBuilder.addDependency( TxnServices.JBOSS_TXN_TRANSACTION_MANAGER, TransactionManager.class, dataSourceService.getTransactionManagerInjector() );
                serviceBuilder.addDependency( TxnServices.JBOSS_TXN_SYNCHRONIZATION_REGISTRY, TransactionSynchronizationRegistry.class, dataSourceService.getTransactionSynchronizationRegistryInjector() );
            }

            serviceBuilder.install();
        }
    }

    // --- //

    private static class DataSourceRemove extends AbstractRemoveStepHandler {

        @Override
        protected void performRuntime(OperationContext context, ModelNode operation, ModelNode model) throws OperationFailedException {
            String datasourceName = PathAddress.pathAddress( operation.require( OP_ADDR ) ).getLastElement().getValue();
            ServiceName datasourceServiceName = ServiceName.of( DATASOURCE_SERVICE_PREFIX, datasourceName );
            context.removeService( datasourceServiceName );
        }
    }
}
