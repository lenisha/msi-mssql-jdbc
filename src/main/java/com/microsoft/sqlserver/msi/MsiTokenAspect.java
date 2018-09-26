package com.microsoft.sqlserver.msi;

import com.microsoft.sqlserver.jdbc.StringUtils;
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

    public static boolean isMsiEnabled(String jdbcUrl) {

        // Environment variable overrides any context setting or url set
        String msiEnableEnv = System.getenv("JDBC_MSI_ENABLE");
        if (!StringUtils.isEmpty(msiEnableEnv) && msiEnableEnv.compareToIgnoreCase("true") == 0) {
            MsiAuthToken.logger.debug("MSI Enabled in Environement");
            return true;
        }

        // Application Setting variable overrides any context setting or url set
        msiEnableEnv = System.getenv("APPSETTING_JDBC_MSI_ENABLE");
        if ( !StringUtils.isEmpty(msiEnableEnv) && msiEnableEnv.compareToIgnoreCase("true") == 0) {
            MsiAuthToken.logger.debug("MSI Enabled in AppSetting Environement");
            return true;
        }

        // URL Setting variable overrides any context setting
        if ( jdbcUrl != null ) {
            jdbcUrl = jdbcUrl.replaceAll("\\s+", "");

            if ( jdbcUrl.indexOf("msiEnable=true") > 0) {
                MsiAuthToken.logger.debug("MSI Enabled in Url reference");
                return true;
            }
        }
        return  false;
    }
    private static void cacheToken(String accessToken) throws Exception {
        logger.debug("Caching Token and expiration");
        MsiTokenCache.saveExpiration(MsiAuthToken.getTokenExpiration(accessToken));
        MsiTokenCache.saveToken(accessToken);
    }

    @Before(value = "execution (* org.apache.tomcat.dbcp.dbcp.BasicDataSource.getConnection())")
    public void onNewConnection(final JoinPoint pjp) throws Throwable {

        Object target = pjp.getTarget();
        if ( !(target instanceof BasicDataSource) )
            return;

        BasicDataSource ds = (BasicDataSource)target;
        if (!isMsiEnabled(ds.getUrl()))
            return;

        long now = System.currentTimeMillis() / 1000;

        logger.debug("MSI Token validation time now:" + now + " token expiration at:" + MsiTokenCache.getExpiration());

        if ( MsiTokenCache.getExpiration() > now  + SKEW) {
            // Token is still valid
            logger.debug("Token is still valid");
            ds.addConnectionProperty("accessToken", MsiTokenCache.getToken() );
            return;
        }
        else {
            // token expired or was not obtained yet
            logger.debug("Getting new token");
            String accessToken = MsiAuthToken.aquireMsiToken("https://database.windows.net/");
            ds.addConnectionProperty("accessToken", accessToken);
            cacheToken(accessToken);
        }
    }



}