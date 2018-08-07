/*
 * This software is distributed under following license based on modified BSD
 * style license.
 * ----------------------------------------------------------------------
 *
 * Copyright 2003 The Nimbus Project. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE NIMBUS PROJECT ``AS IS'' AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN
 * NO EVENT SHALL THE NIMBUS PROJECT OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are
 * those of the authors and should not be interpreted as representing official
 * policies, either expressed or implied, of the Nimbus Project.
 */
package jp.ossc.nimbus.service.websocket;

import java.sql.Connection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.ossc.nimbus.core.ServiceManagerFactory;
import jp.ossc.nimbus.core.ServiceName;
import jp.ossc.nimbus.service.connection.ConnectionFactory;
import jp.ossc.nimbus.service.connection.PersistentManager;

/**
 * DBを使用した認証サービス。<br>
 * 認証時にloginSelectSqlで指定したSQLで検索を実施し、結果レコード数が１件であれば認証OK<br>
 * ログアウト時にlogoutUpdateSqlで指定したSQLで更新を行う<br>
 * <p>
 *
 * @author M.Ishida
 */
public class DatabaseAuthenticatorService extends AbstractAuthenticatorService implements Authenticator, DatabaseAuthenticatorServiceMBean {

    private static final long serialVersionUID = 5687455356256140976L;
    
    protected ServiceName connectionFactoryServiceName;
    protected ConnectionFactory connectionFactory;

    protected ServiceName persistentManagerServiceName;
    protected PersistentManager persistentManager;
    
    protected String loginSelectSql;
    protected String logoutUpdateSql;
    
    public ServiceName getConnectionFactoryServiceName() {
        return connectionFactoryServiceName;
    }
    
    public void setConnectionFactoryServiceName(ServiceName serviceName) {
        connectionFactoryServiceName = serviceName;
    }
    
    public ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }
    
    public void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }
    
    public ServiceName getPersistentManagerServiceName() {
        return persistentManagerServiceName;
    }
    
    public void setPersistentManagerServiceName(ServiceName serviceName) {
        persistentManagerServiceName = serviceName;
    }
    
    public PersistentManager getPersistentManager() {
        return persistentManager;
    }
    
    public void setPersistentManager(PersistentManager manager) {
        persistentManager = manager;
    }
    
    public String getLoginSelectSql() {
        return loginSelectSql;
    }
    
    public void setLoginSelectSql(String sql) {
        loginSelectSql = sql;
    }

    public String getLogoutUpdateSql() {
        return logoutUpdateSql;
    }

    public void setLogoutUpdateSql(String sql) {
        logoutUpdateSql = sql;
    }

    /**
     * サービスの開始処理を行う。
     * <p>
     *
     * @exception Exception サービスの開始に失敗した場合
     */
    public void startService() throws Exception {
        super.startService();
        
        if (connectionFactoryServiceName != null) {
            connectionFactory = (ConnectionFactory) ServiceManagerFactory.getServiceObject(connectionFactoryServiceName);
        }
        if (connectionFactory == null) {
            throw new IllegalArgumentException("ConnectionFactory is null.");
        }
        if (persistentManagerServiceName != null) {
            persistentManager = (PersistentManager) ServiceManagerFactory.getServiceObject(persistentManagerServiceName);
        }
        if (persistentManager == null) {
            throw new IllegalArgumentException("PersistentManager is null.");
        }
    }
    
    
    protected boolean login(String id, String ticket) throws Exception {
        if(loginSelectSql == null) {
            return true;
        }
        Connection con = connectionFactory.getConnection();
        try {
            Map param = new HashMap();
            param.put(idKey, id);
            param.put(ticketKey, ticket);
            List list = (List) persistentManager.loadQuery(con, loginSelectSql, param, null);
            return list.size() == 1;
        } finally {
            if(con != null) {
                try {
                    con.close();
                } catch(Exception e) {}
            }
        }
    }

    protected void logout(String id, String ticket) throws Exception {
        if(logoutUpdateSql == null) {
            return;
        }
        Connection con = connectionFactory.getConnection();
        try {
            Map param = new HashMap();
            param.put(idKey, id);
            param.put(ticketKey, ticket);
            persistentManager.persistQuery(con, logoutUpdateSql, param);
        } finally {
            if(con != null) {
                try {
                    con.close();
                } catch(Exception e) {}
            }
        }
        
    }


}
