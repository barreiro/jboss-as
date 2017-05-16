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
package org.wildfly.extension.agroal.logging;

import io.agroal.api.AgroalDataSourceListener;

import java.sql.Connection;

/**
 * Provides log for important DataSource actions
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class LoggingDataSourceListener implements AgroalDataSourceListener {

    @Override
    public void beforeConnectionLeak(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.debugv( "Leak test on connection {0}", connection );
    }

    @Override
    public void beforeConnectionReap(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.debugv( "Reap test on connection {0}", connection );
    }

    @Override
    public void beforeConnectionValidation(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.debugv( "Validation test on connection {0}", connection );
    }

    @Override
    public void onConnectionAcquire(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.debugv( "Acquire connection {0}", connection );
    }

    @Override
    public void onConnectionCreation(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.infov( "Created connection {0}", connection );
    }

    @Override
    public void onConnectionReap(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.debugv( "Closing idle connection {0}", connection );
    }

    @Override
    public void onConnectionReturn(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.debugv( "Returning connection {0}", connection );
    }

    @Override
    public void onConnectionDestroy(Connection connection) {
        AgroalLogger.DATASOURCE_LOGGER.infov( "Destroyed connection {0}", connection );
    }

    @Override
    public void onWarning(Throwable throwable) {
        AgroalLogger.DATASOURCE_LOGGER.warnv( throwable.getMessage(), throwable );
    }
}
