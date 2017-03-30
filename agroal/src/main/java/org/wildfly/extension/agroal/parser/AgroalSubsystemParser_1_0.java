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
package org.wildfly.extension.agroal.parser;

import org.jboss.as.controller.PersistentResourceXMLDescription;
import org.jboss.as.controller.PersistentResourceXMLParser;
import org.wildfly.extension.agroal.definition.AgroalSubsystemDefinition;
import org.wildfly.extension.agroal.definition.DatasourceDefinition;
import org.wildfly.extension.agroal.definition.DriverDefinition;
import org.wildfly.extension.agroal.definition.XaDatasourceDefinition;

import static org.jboss.as.controller.PersistentResourceXMLDescription.builder;

/**
 * The subsystem parser and marshaller, that reads the model to and from it's xml persistent representation
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalSubsystemParser_1_0 extends PersistentResourceXMLParser {

    public static final AgroalSubsystemParser_1_0 INSTANCE = new AgroalSubsystemParser_1_0();

    private static final PersistentResourceXMLDescription.PersistentResourceXMLBuilder XML_DESCRIPTION;

    static {
        XML_DESCRIPTION = builder( AgroalSubsystemDefinition.INSTANCE.getPathElement(), Namespace.AGROAL_1_0.getUriString() );

        PersistentResourceXMLDescription.PersistentResourceXMLBuilder datasourceXMLBuilder = builder( DatasourceDefinition.INSTANCE.getPathElement() );
        DatasourceDefinition.INSTANCE.getAttributes().forEach( datasourceXMLBuilder::addAttribute );
        XML_DESCRIPTION.addChild( datasourceXMLBuilder );

        PersistentResourceXMLDescription.PersistentResourceXMLBuilder xaDatasourceXMLBuilder = builder( XaDatasourceDefinition.INSTANCE.getPathElement() );
        XaDatasourceDefinition.INSTANCE.getAttributes().forEach( xaDatasourceXMLBuilder::addAttribute );
        XML_DESCRIPTION.addChild( xaDatasourceXMLBuilder );

        PersistentResourceXMLDescription.PersistentResourceXMLBuilder driverXMLBuilder = builder( DriverDefinition.INSTANCE.getPathElement() );
        driverXMLBuilder.setXmlWrapperElement( DriverDefinition.DRIVERS_ELEMENT_NAME );
        DriverDefinition.INSTANCE.getAttributes().forEach( driverXMLBuilder::addAttribute );
        XML_DESCRIPTION.addChild( driverXMLBuilder );
    }

    private AgroalSubsystemParser_1_0() {
    }

    @Override
    public PersistentResourceXMLDescription getParserDescription() {
        return XML_DESCRIPTION.build();
    }
}
