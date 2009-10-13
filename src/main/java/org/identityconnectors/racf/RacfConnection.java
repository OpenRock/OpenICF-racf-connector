/*
 * ====================
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright 2008-2009 Sun Microsystems, Inc. All rights reserved.     
 * 
 * The contents of this file are subject to the terms of the Common Development 
 * and Distribution License("CDDL") (the "License").  You may not use this file 
 * except in compliance with the License.
 * 
 * You can obtain a copy of the License at 
 * http://IdentityConnectors.dev.java.net/legal/license.txt
 * See the License for the specific language governing permissions and limitations 
 * under the License. 
 * 
 * When distributing the Covered Code, include this CDDL Header Notice in each file
 * and include the License file at identityconnectors/legal/license.txt.
 * If applicable, add the following below this CDDL Header, with the fields 
 * enclosed by brackets [] replaced by your own identifying information: 
 * "Portions Copyrighted [year] [name of copyright owner]"
 * ====================
 */
package org.identityconnectors.racf;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;

import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.rw3270.RW3270BaseConnection;
import org.identityconnectors.rw3270.RW3270Connection;
import org.identityconnectors.rw3270.RW3270ConnectionFactory;

public class RacfConnection {
    private InitialContext            _context;
    private DirContext                _dirContext;
    private RacfConfiguration         _configuration;
    private RW3270Connection          _racfConnection;

    public RacfConnection(RacfConfiguration configuration) throws NamingException {
        _configuration = configuration;
        if (_configuration.getLdapUserName()!=null) {
            _context = new InitialContext(createCommonContextProperties());
            _dirContext = new InitialDirContext(createCommonContextProperties());
        }
        if (_configuration.getUserName()!=null) {
            try {
                System.out.println("initializing command line");
                _racfConnection = new RW3270ConnectionFactory(configuration).newConnection();
                ((RW3270BaseConnection)_racfConnection).loginUser();
            } catch (Exception e) {
                throw ConnectorException.wrap(e);
            }
        }
    }

    private Hashtable<Object,Object> createCommonContextProperties()
    {
        Hashtable<Object,Object> env = new Hashtable<Object,Object>();

        String url = "ldap://" + _configuration.getHostNameOrIpAddr() + ":" +
        _configuration.getHostPortNumber();

        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, url);
        env.put(Context.SECURITY_PRINCIPAL, _configuration.getLdapUserName());
        GuardedStringAccessor accessor = new GuardedStringAccessor();
        _configuration.getLdapPassword().access(accessor);
        env.put(Context.SECURITY_CREDENTIALS, new String(accessor.getArray()));
        if (_configuration.getUseSsl()) {
            env.put(Context.SECURITY_PROTOCOL, "ssl");
        }
        env.put("java.naming.ldap.attributes.binary", "racfPasswordEnvelope");

        return env;
    }

    public InitialContext getContext() {
        return _context;
    }

    public DirContext getDirContext() {
        return _dirContext;
    }

    public void dispose() {
        try {
            _context.close();
            _dirContext.close();
        } catch (NamingException ne) {
            throw ConnectorException.wrap(ne);
        }
        if (_racfConnection!=null)
            _racfConnection.dispose();
    }

    public RW3270Connection getRacfConnection() {
        return _racfConnection;
    }

}
