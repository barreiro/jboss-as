/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.as.test.integration.web.webintegration.interfaces;

import javax.ejb.EntityBean;
import javax.ejb.CreateException;

public abstract class InternalEntityBean implements EntityBean {

    private static final long serialVersionUID = -8883778049256183985L;

    public abstract Integer getId();

    public abstract void setId(Integer key);

    public abstract int getValue();

    public abstract void setValue(int value);

    public Integer ejbCreate(int the_key, int the_value) throws CreateException {
        setId(new Integer(the_key));
        setValue(the_value);
        return null;
    }

    public void ejbPostCreate(int the_key, int the_value) {
    }

    public void ejbRemove() {
    }

    public void setEntityContext(javax.ejb.EntityContext context) {
    }

    public void unsetEntityContext() {
    }

    public void ejbActivate() {
    }

    public void ejbPassivate() {
    }

    public void ejbLoad() {
    }

    public void ejbStore() {
    }

}
