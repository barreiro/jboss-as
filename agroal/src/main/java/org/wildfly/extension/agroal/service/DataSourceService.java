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
import io.agroal.api.AgroalDataSourceListener;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
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

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Defines an extension to provide DataSources based on the Agroal project
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class DataSourceService implements Service<AgroalDataSource> {

    private final String dataSourceName;
    private final String jndiName;
    private final AgroalDataSourceConfigurationSupplier dataSourceConfiguration;
    private final InjectedValue<DriverService.DriverClass> driverService = new InjectedValue<>();
    private AgroalDataSource agroalDataSource;

    public DataSourceService(String dataSourceName, String jndiName, AgroalDataSourceConfigurationSupplier dataSourceConfiguration) {
        this.dataSourceName = dataSourceName;
        this.jndiName = jndiName;
        this.dataSourceConfiguration = dataSourceConfiguration;
    }

    @Override
    public void start(StartContext context) throws StartException {
        dataSourceConfiguration.connectionPoolConfiguration( cp -> cp
                .connectionFactoryConfiguration( cf -> cf
                        .driverClassName( driverService.getValue().driverClassName() )
                        .classLoaderProvider( driverService.getValue().driverClassloaderProvider() )
                )
        );

        try {
            agroalDataSource = AgroalDataSource.from( dataSourceConfiguration );

            ContextNames.BindInfo bindInfo = ContextNames.bindInfoFor( jndiName );
            BinderService binderService = new BinderService( bindInfo.getBindName() );
            ImmediateManagedReferenceFactory managedReferenceFactory = new ImmediateManagedReferenceFactory( agroalDataSource );
            context.getChildTarget().addService( bindInfo.getBinderServiceName(), binderService )
                    .addInjectionValue( binderService.getManagedObjectInjector(), new ImmediateValue<ManagedReferenceFactory>( managedReferenceFactory ) )
                    .addDependency( bindInfo.getParentContextServiceName(), ServiceBasedNamingStore.class, binderService.getNamingStoreInjector() )
                    .install();

            AgroalLogger.SERVICE_LOGGER.infof( "Started datasource '%s' bound to [%s]", dataSourceName, jndiName );
        } catch ( SQLException e ) {
            throw new StartException( "Exception starting datasource " + dataSourceName, e );
        }

        agroalDataSource.addListener( new AgroalDataSourceListener() {

            @Override
            public void beforeConnectionLeak(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.debugf( "Leak test on connection " + connection );
            }

            @Override
            public void beforeConnectionReap(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.debugf( "Reap test on connection " + connection );
            }

            @Override
            public void beforeConnectionValidation(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.debugf( "Validation test on connection " + connection );
            }

            @Override
            public void onConnectionAcquire(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.debugf( "Acquire connection " + connection );
            }

            @Override
            public void onConnectionCreation(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.infof( "Created connection " + connection );
            }

            @Override
            public void onConnectionReap(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.debugf( "Closing idle connection " + connection );
            }

            @Override
            public void onConnectionReturn(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.debugf( "Returning connection " + connection );
            }

            @Override
            public void onConnectionDestroy(Connection connection) {
                AgroalLogger.DATASOURCE_LOGGER.infof( "Destroyed connection " + connection );
            }

            @Override
            public void onWarning(Throwable throwable) {
                AgroalLogger.DATASOURCE_LOGGER.warnf( throwable.getMessage(), throwable );
            }
        } );
    }

    @Override
    public void stop(StopContext context) {
        agroalDataSource.close();
    }

    @Override
    public AgroalDataSource getValue() throws IllegalStateException, IllegalArgumentException {
        return agroalDataSource;
    }

    public Injector<DriverService.DriverClass> getDriverServiceInjector() {
        return driverService;
    }
}
