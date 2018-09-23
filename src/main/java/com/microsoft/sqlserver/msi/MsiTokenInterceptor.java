package com.microsoft.sqlserver.msi;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import org.apache.tomcat.jdbc.pool.PooledConnection;

import java.lang.reflect.Method;

public class MsiTokenInterceptor extends JdbcInterceptor {
    protected static final Logger logger = LogManager.getLogger(MsiTokenInterceptor.class);

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {

    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        if (compare(GETCONNECTION_VAL,method)){
            logger.debug("MSI INJECT in GET Connection");
        }
        return super.invoke(proxy,method,args);
    }
}
