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
package org.wildfly.extension.agroal.service;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import io.agroal.narayana.NarayanaTransactionIntegration;
import org.jboss.as.naming.ImmediateManagedReferenceFactory;
import org.jboss.as.naming.ManagedReferenceFactory;
import org.jboss.as.naming.ServiceBasedNamingStore;
import org.jboss.as.naming.deployment.ContextNames;
import org.jboss.as.naming.service.BinderService;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.InjectedValue;
import org.wildfly.extension.agroal.logging.AgroalLogger;
import org.wildfly.extension.agroal.logging.LoggingDataSourceListener;

import javax.sql.DataSource;
import javax.sql.XADataSource;
import javax.transaction.TransactionManager;
import javax.transaction.TransactionSynchronizationRegistry;
import java.sql.Driver;
import java.sql.SQLException;

/**
 * Defines an extension to provide DataSources based on the Agroal project
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceService implements Service<AgroalDataSource> {

    private final String dataSourceName;
    private final String jndiName;
    private final boolean connectable;
    private final boolean xa;
    private final AgroalDataSourceConfigurationSupplier dataSourceConfiguration;
    private final InjectedValue<DriverService.ProviderClass> driverService = new InjectedValue<>();
    private final InjectedValue<TransactionManager> transactionManager = new InjectedValue<>();
    private final InjectedValue<TransactionSynchronizationRegistry> transactionSynchronizationRegistry = new InjectedValue<>();
    private AgroalDataSource agroalDataSource;

    public DataSourceService(String dataSourceName, String jndiName, boolean connectable, boolean xa, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        this.dataSourceName = dataSourceName;
        this.jndiName = jndiName;
        this.connectable = connectable;
        this.xa = xa;
        this.dataSourceConfiguration = dataSourceConfiguration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        Class<?> providerClass = driverService.getValue().providerClass();
        if ( xa ) {
            if ( !XADataSource.class.isAssignableFrom( providerClass ) ) {
                throw new StartException( "An xa-datasource requires a javax.sqlXADataSource as connection provider. Fix the connection-provider for the driver" );
            }
        } else {
            if ( providerClass != null && !DataSource.class.isAssignableFrom( providerClass ) && !Driver.class.isAssignableFrom( providerClass ) ) {
                throw new StartException( "Invalid connection provider. Either a java.sql.Driver or javax.sql.DataSource implementation is required. Fix the connection-provider for the driver" );
            }
        }

        dataSourceConfiguration.connectionPoolConfiguration( cp -> cp.connectionFactoryConfiguration( cf -> cf.connectionProviderClass( providerClass ) ) );

        if ( transactionManager.getOptionalValue() != null && transactionSynchronizationRegistry.getOptionalValue() != null ) {
            NarayanaTransactionIntegration txIntegration = new NarayanaTransactionIntegration( transactionManager.getValue(), transactionSynchronizationRegistry.getValue(), jndiName, connectable );
            dataSourceConfiguration.connectionPoolConfiguration( cp -> cp.transactionIntegration( txIntegration ) );
        } else if ( xa ) {
            throw new StartException( "Could not start xa-datasource: transaction manager is missing" );
        }

        try {
            agroalDataSource = AgroalDataSource.from( dataSourceConfiguration, new LoggingDataSourceListener( dataSourceName ) );

            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor( jndiName );
            BinderService binderService = new BinderService( bindInfo.getBindName() );
            ImmediateManagedReferenceFactory managedReferenceFactory = new ImmediateManagedReferenceFactory( agroalDataSource );
            context.getChildTarget().addService( bindInfo.getBinderServiceName(), binderService )
                    .addInjectionValue( binderService.getManagedObjectInjector(), new ImmediateValue<ManagedReferenceFactory>( managedReferenceFactory ) )
                    .addDependency( bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector() )
                    .install();

            AgroalLogger.SERVICE_LOGGER.infof( "Started %sdatasource '%s' bound to [%s]", xa ? "xa-" : "", dataSourceName, jndiName );
        } catch ( SQLException e ) {
            agroalDataSource = null;
            throw new StartException( "Exception starting datasource " + dataSourceName, e );
        }
    }

    @Override
    public void stop(StopContext context) {
        agroalDataSource.close();
        AgroalLogger.SERVICE_LOGGER.infof( "Stopped %sdatasource '%s'", xa ? "xa-" : "", dataSourceName );
    }

    @Override
    public AgroalDataSource getValue() throws IllegalStateException, IllegalArgumentException {
        return agroalDataSource;
    }

    public Injector<DriverService.ProviderClass> getDriverServiceInjector() {
        return driverService;
    }

    public Injector<TransactionManager> getTransactionManagerInjector() {
        return transactionManager;
    }

    public Injector<TransactionSynchronizationRegistry> getTransactionSynchronizationRegistryInjector() {
        return transactionSynchronizationRegistry;
    }
}