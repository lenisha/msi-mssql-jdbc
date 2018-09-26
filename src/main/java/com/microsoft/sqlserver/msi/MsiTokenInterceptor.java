package com.microsoft.sqlserver.msi;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tomcat.jdbc.pool.ConnectionPool;
import org.apache.tomcat.jdbc.pool.JdbcInterceptor;
import org.apache.tomcat.jdbc.pool.PooledConnection;
import java.lang.reflect.Method;

public class MsiTokenInterceptor extends JdbcInterceptor {
    protected static final Logger logger = LogManager.getLogger(MsiTokenInterceptor.class);
    protected static long SKEW = 1;

    @Override
    public void reset(ConnectionPool parent, PooledConnection con) {
        logger.debug("Reset connection pool started");
        if ( parent == null || con == null ) return;

        try {
            if ( !MsiAuthToken.isMsiEnabled(parent.getPoolProperties().getUrl()) )
                return;

            long now = System.currentTimeMillis() / 1000;

            logger.debug("MSI Token validation time now:" + now + " token expiration at:" + MsiTokenCache.getExpiration());

            if ( MsiTokenCache.getExpiration() > now  + SKEW) {
                // Token is still valid
                logger.debug("Token is still valid");
            }
            else {
                // token expired or was not obtained yet
                logger.debug("Getting new token");
                String accessToken = MsiAuthToken.aquireMsiToken("https://database.windows.net/");
                parent.getPoolProperties().getDbProperties().setProperty("accessToken", accessToken);
                MsiAuthToken.cacheToken(accessToken);
                // need to refresh connection with new token
                con.connect();
            }
        } catch (Throwable e) {
            logger.error("Exception caught on reconnect:" + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }

    @Override
    public void poolStarted(ConnectionPool pool) {
        logger.info("Init connection pool start");
        super.poolStarted(pool);

        try {
            if ( pool != null &&  MsiAuthToken.isMsiEnabled(pool.getPoolProperties().getUrl()) ) {
                String accessToken = MsiAuthToken.aquireMsiToken("https://database.windows.net/");
                pool.getPoolProperties().getDbProperties().setProperty("accessToken", accessToken);
                MsiAuthToken.cacheToken(accessToken);
            }
        } catch (Throwable e) {
            logger.error("Exception caught during initialization:" + e.getMessage());
            throw new RuntimeException(e.getCause());
        }
    }
}