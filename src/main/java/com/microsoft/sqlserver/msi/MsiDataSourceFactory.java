package com.microsoft.sqlserver.msi;


import javax.naming.*;

import com.microsoft.sqlserver.jdbc.SQLServerDataSource;
import org.apache.tomcat.dbcp.dbcp.BasicDataSourceFactory;

import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import java.sql.Connection;
import java.sql.ResultSet;

import java.util.Collections;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * DataSource factory calls to be used in Tomcat context.xml
 */
public class MsiDataSourceFactory extends BasicDataSourceFactory implements ObjectFactory {

    protected static final Logger logger = LogManager.getLogger(MsiDataSourceFactory.class);

    //http://tomcat.apache.org/tomcat-6.0-doc/jndi-resources-howto.html#Adding_Custom_Resource_Factories
    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable environment) throws Exception {
       // We only know how to deal with <code>javax.naming.Reference</code>s
        // that specify a class name of "javax.sql.DataSource"
        if (obj == null || !(obj instanceof Reference)) {
            return null;
        }
        Reference ref = (Reference) obj;
        if (!"javax.sql.DataSource".equals(ref.getClassName())) {
            return null;
        }
        if ( isMsiEnabled(ref) ) {
            logger.info("Enabling MSI  Datasource");
            addAccessToken(ref);
        }
        // Return the customized instance
        return super.getObjectInstance(obj, name, nameCtx, environment);
    }

    private boolean isMsiEnabled(Reference ref) {

        // Environment variable overrides any context setting or url set
        String msiEnableEnv = System.getenv("JDBC_MSI_ENABLE");
        if ( !"".equals(msiEnableEnv) && msiEnableEnv.compareToIgnoreCase("true") == 0) {
            logger.debug("MSI Enabled in Environement");
            return true;
        }

        // Application Setting variable overrides any context setting or url set
        msiEnableEnv = System.getenv("APPSETTING_JDBC_MSI_ENABLE");
        if ( !"".equals(msiEnableEnv) && msiEnableEnv.compareToIgnoreCase("true") == 0) {
            logger.debug("MSI Enabled in AppSetting Environement");
            return true;
        }

        // URL Setting variable overrides any context setting
        StringRefAddr msiEnableUrl = (StringRefAddr)ref.get("url");
        if ( msiEnableUrl != null ) {
            String msiEnableUrlValue = (String)msiEnableUrl.getContent();
            msiEnableUrlValue = msiEnableUrlValue.replaceAll("\\s+", "");

            if ( msiEnableUrlValue.indexOf("msiEnable=true") > 0) {
                logger.debug("MSI Enabled in Url reference");
                return true;
            }
        }

        // Application Setting variable overrides any context setting or url set
        StringRefAddr msiEnableCtx = (StringRefAddr)ref.get("msiEnable");
        if ( msiEnableCtx != null ) {
            String msiEnableCtxValue = (String)msiEnableCtx.getContent();
            if ( msiEnableCtxValue.compareToIgnoreCase("true") == 0 ) {
                logger.debug("MSI Enabled in context reference");
                return true;
            }
        }
        return  false;
    }

    private void addAccessToken(Reference ref) throws Exception {

        String accessToken = MsiAuthToken.aquireMsiToken("https://database.windows.net/");
        logger.info("adding connectionProperties with the MSI token");
        StringRefAddr ra = new StringRefAddr("connectionProperties","accessToken=" + accessToken + ";");
        ref.add(ra);

    }


    private void printDebug() {
        if ( logger.isDebugEnabled() ) {
            Map<String, String> env = System.getenv();
            for (String envName : env.keySet()) {
                logger.debug("MSI System env:" + envName + " " + env.get(envName));
            }
            Properties props = System.getProperties();
            for (String propName : props.stringPropertyNames()) {
                logger.debug("MSI System prop:" + propName + " " + props.getProperty(propName));
            }
        }
    }

    private void pringDebugConnection(Reference obj) {

        if ( logger.isDebugEnabled() ) {
            for (RefAddr refaddr : Collections.list(((Reference) obj).getAll())) {
                logger.debug("reference properties" + refaddr.toString());
            }
        }
    }
}