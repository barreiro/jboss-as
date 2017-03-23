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
package org.wildfly.extension.agroal.definition;

import org.jboss.as.controller.AttributeDefinition;
import org.jboss.as.controller.AttributeMarshaller;
import org.jboss.as.controller.AttributeParser;
import org.jboss.as.controller.ObjectTypeAttributeDefinition;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.dmr.ModelNode;
import org.jboss.staxmapper.XMLExtendedStreamReader;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Custom Parser and marshaller for ConnectionPool and ConnectionFactory object.
 * Differs from the parsers for ObjectTypeAttributeDefinition in the way it handles attribute group (as a child element per group)
 *
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
public class AgroalObjectAttribute {

    public static final AttributeMarshaller MARSHALLER = new AgroalObjectMarshaller();

    public static final AttributeParser PARSER = new AgroalObjectParser();

    // -- //

    private static class AgroalObjectMarshaller extends AttributeMarshaller {

        @Override
        public boolean isMarshallableAsElement() {
            return true;
        }

        @Override
        public void marshallAsElement(AttributeDefinition attribute, ModelNode resourceModel, boolean marshallDefault, XMLStreamWriter writer) throws XMLStreamException {
            assert attribute instanceof ObjectTypeAttributeDefinition;
            ModelNode handler = resourceModel.get( attribute.getName() );

            List<AttributeDefinition> childElements = new ArrayList<>();
            Map<String, List<AttributeDefinition>> attributesByGroup = new HashMap<>();
            writer.writeStartElement( attribute.getXmlName() );

            for ( AttributeDefinition valueType : ( (ObjectTypeAttributeDefinition) attribute ).getValueTypes() ) {
                if ( !isMarshallable( valueType, handler, marshallDefault ) ) {
                    continue;
                }

                if ( valueType.getParser().isParseAsElement() ) {
                    childElements.add( valueType );
                } else if ( valueType.getAttributeGroup() != null && !valueType.getAttributeGroup().isEmpty() ) {
                        // If an attribute has a group, add to the map so that it's marshaled afterwards as element
                        List<AttributeDefinition> groupElements = attributesByGroup.getOrDefault( valueType.getAttributeGroup(), new ArrayList<>() );
                        groupElements.add( valueType );
                        attributesByGroup.put( valueType.getAttributeGroup(), groupElements );
                } else {
                    valueType.getMarshaller().marshallAsAttribute( valueType, handler, marshallDefault, writer );
                }
            }

            // Write child elements
            for ( AttributeDefinition valueType : childElements ) {
                valueType.marshallAsElement( handler, marshallDefault, writer );
            }

            // Write attribute groups
            for ( Map.Entry<String, List<AttributeDefinition>> entry : attributesByGroup.entrySet() ) {
                writer.writeStartElement( entry.getKey() );
                for ( AttributeDefinition valueType : entry.getValue() ) {
                    valueType.getMarshaller().marshallAsAttribute( valueType, handler, marshallDefault, writer );
                }
                writer.writeEndElement();
            }

            writer.writeEndElement();
        }
    }

    // --- //

    private static class AgroalObjectParser extends AttributeParser {

        private static void parseEmbeddedElement(ObjectTypeAttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode op, String... additionalExpectedAttributes) throws XMLStreamException {
            AttributeDefinition[] valueTypes = attribute.getValueTypes();

            Map<String, AttributeDefinition> attributes = new HashMap<>();
            Map<String, AttributeDefinition> childElements = new HashMap<>();
            Map<String, Map<String, AttributeDefinition>> groupElements = new HashMap<>();

            // Prepare attribute group data structures
            for ( AttributeDefinition valueType : valueTypes ) {
                if ( valueType.getParser().isParseAsElement() ) {
                    childElements.put( valueType.getParser().getXmlName( valueType ), valueType );
                } else if ( valueType.getAttributeGroup() != null && !valueType.getAttributeGroup().isEmpty() ) {
                    Map<String, AttributeDefinition> group = groupElements.getOrDefault( valueType.getAttributeGroup(), new HashMap<>() );
                    group.put( valueType.getXmlName(), valueType );
                    groupElements.put( valueType.getAttributeGroup(), group );
                } else {
                    attributes.put( valueType.getXmlName(), valueType );
                }
            }

            parseAttributes( reader, op, attributes, additionalExpectedAttributes );
            parseChildElements( reader, op, childElements, groupElements, additionalExpectedAttributes );
        }

        private static void parseAttributes(XMLExtendedStreamReader reader, ModelNode op, Map<String, AttributeDefinition> attributes, String[] additionalExpectedAttributes) throws XMLStreamException {
            for ( int i = 0; i < reader.getAttributeCount(); i++ ) {
                String attributeName = reader.getAttributeLocalName( i );
                String value = reader.getAttributeValue( i );
                if ( attributes.containsKey( attributeName ) ) {
                    AttributeDefinition def = attributes.get( attributeName );
                    AttributeParser parser = def.getParser();
                    assert parser != null;
                    parser.parseAndSetParameter( def, value, op, reader );
                } else if ( Arrays.binarySearch( additionalExpectedAttributes, attributeName ) < 0 ) {
                    throw ParseUtils.unexpectedAttribute( reader, i, attributes.keySet() );
                }
            }
        }

        private static void parseChildElements(XMLExtendedStreamReader reader, ModelNode op, Map<String, AttributeDefinition> childElements, Map<String, Map<String, AttributeDefinition>> groupElements, String[] additionalExpectedAttributes) throws XMLStreamException {
            if ( reader.isStartElement() ) {
                String originalStartElement = reader.getLocalName();
                if ( reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT ) {
                    do {
                        String attrName = reader.getLocalName();
                        if ( childElements.containsKey( attrName ) ) {
                            AttributeDefinition ad = childElements.get( reader.getLocalName() );
                            ad.getParser().parseElement( ad, reader, op );
                        } else if ( groupElements.containsKey( attrName ) ) {
                            Map<String, AttributeDefinition> group = groupElements.get( attrName );
                            for ( int i = 0; i < reader.getAttributeCount(); i++ ) {
                                String attributeName = reader.getAttributeLocalName( i );
                                String value = reader.getAttributeValue( i );
                                if ( group.containsKey( attributeName ) ) {
                                    AttributeDefinition def = group.get( attributeName );
                                    AttributeParser parser = def.getParser();
                                    assert parser != null;
                                    parser.parseAndSetParameter( def, value, op, reader );
                                } else if ( Arrays.binarySearch( additionalExpectedAttributes, attributeName ) < 0 ) {
                                    throw ParseUtils.unexpectedAttribute( reader, i, group.keySet() );
                                }
                            }
                            ParseUtils.requireNoContent( reader );
                        } else {
                            throw ParseUtils.unexpectedElement( reader );
                        }
                    }
                    while ( !reader.getLocalName().equals( originalStartElement ) && reader.hasNext() && reader.nextTag() != XMLStreamConstants.END_ELEMENT );
                }
            }
        }

        @Override
        public boolean isParseAsElement() {
            return true;
        }

        @Override
        public void parseElement(AttributeDefinition attribute, XMLExtendedStreamReader reader, ModelNode operation) throws XMLStreamException {
            assert attribute instanceof ObjectTypeAttributeDefinition;

            if ( operation.hasDefined( attribute.getName() ) ) {
                throw ParseUtils.unexpectedElement( reader );
            }
            if ( attribute.getXmlName().equals( reader.getLocalName() ) ) {
                ObjectTypeAttributeDefinition objectType = ( (ObjectTypeAttributeDefinition) attribute );
                ModelNode op = operation.get( attribute.getName() );
                op.setEmptyObject();
                parseEmbeddedElement( objectType, reader, op );
            } else {
                throw ParseUtils.unexpectedElement( reader, Collections.singleton( attribute.getXmlName() ) );
            }
            if ( !reader.isEndElement() ) {
                ParseUtils.requireNoContent( reader );
            }
        }
    }
}
