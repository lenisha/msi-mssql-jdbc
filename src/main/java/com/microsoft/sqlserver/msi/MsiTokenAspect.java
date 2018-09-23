package com.microsoft.sqlserver.msi;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.tomcat.dbcp.dbcp.BasicDataSource;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;



@Aspect
public class MsiTokenAspect {
    protected static final Logger logger = LogManager.getLogger(MsiTokenAspect.class);
    protected static long SKEW = 1;

    @Before(value = "execution (* org.apache.tomcat.dbcp.dbcp.BasicDataSource.getConnection())")
    public void onNewConnection(final JoinPoint pjp) throws Throwable {

        Object target = pjp.getTarget();
        if ( !(target instanceof BasicDataSource) )
            return;

        BasicDataSource ds = (BasicDataSource)target;
        if (!MsiAuthToken.isMsiEnabled(ds.getUrl()))
            return;

        long now = System.currentTimeMillis() / 1000;

        logger.debug("MSI Token validation now:" + now + " and it will expire at:" + MsiTokenCache.getExpiration());

        if ( MsiTokenCache.getExpiration() > now  + SKEW) {
            // Token is still valid
            ds.addConnectionProperty("accessToken", MsiTokenCache.getToken() );
            return;
        }
        else {
            // token expired or was not obtained yet
            String accessToken = MsiAuthToken.aquireAndCacheMsiToken("https://database.windows.net/");
            ds.addConnectionProperty("accessToken", accessToken);
        }
    }



}