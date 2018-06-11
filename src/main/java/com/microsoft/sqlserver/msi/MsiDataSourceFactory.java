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
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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

        if (ref.get("msiEnable") != null) {
            logger.info("Enabling MSI  Datasource");
            addAccessToken(ref);
        }
        for (RefAddr refaddr: Collections.list( ((Reference)obj).getAll() ) ) {
            logger.info("refernce proprerties" + refaddr.toString());
        }
        // Return the customized instance
        return super.getObjectInstance(obj, name, nameCtx, environment);

    }

    private void addAccessToken(Reference ref) throws Exception {

        String accessToken = MsiAuthToken.aquireMsiToken("https://database.windows.net/");
        //String accessToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6ImlCakwxUmNxemhpeTRmcHhJeGRacW9oTTJZayIsImtpZCI6ImlCakwxUmNxemhpeTRmcHhJeGRacW9oTTJZayJ9.eyJhdWQiOiJodHRwczovL2RhdGFiYXNlLndpbmRvd3MubmV0LyIsImlzcyI6Imh0dHBzOi8vc3RzLndpbmRvd3MubmV0LzcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0Ny8iLCJpYXQiOjE1Mjg3MzQyNDIsIm5iZiI6MTUyODczNDI0MiwiZXhwIjoxNTI4NzYzMzQyLCJhaW8iOiJZMmRnWU5qUzJHQzR1MkNQc1g5VDlRUzlIdW5iQUE9PSIsImFwcGlkIjoiNzJjMGJmNzItODM3MS00MDQ1LWI5YjAtNzNjZjVkM2QwZGZkIiwiYXBwaWRhY3IiOiIyIiwiZV9leHAiOjI4ODAwMCwiaWRwIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3LyIsIm9pZCI6IjgyY2M1Zjk2LTIyNmEtNDcyMS05MDJjLTc0NTFjYzY4ZmQ4MCIsInN1YiI6IjgyY2M1Zjk2LTIyNmEtNDcyMS05MDJjLTc0NTFjYzY4ZmQ4MCIsInRpZCI6IjcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0NyIsInV0aSI6ImhfUUlXSGRTSFVLc0ZHY2hJZkllQUEiLCJ2ZXIiOiIxLjAifQ.TlB6h6IeF6g5PS76GwVgvHgoVoPKZeN09pNoH0xaN8yValHgMwRev_eploq5HmmdvdYhIiTDKBAFtN5uYYlMmZDdIo92UCq7YizCY-dGgf5D3GQc7FFbVHPU-adxeHSI7n5k5iWrTk_S5dr0P7pP-AjPrpg6wEFM7ZzQg0y8jIuRorYEca3ltkTld5ld6BA5I855qxjukJIVHN6EYCyiLORUbevLEmsLExeQZFfka1VAfm-OF5s_qXx85QikfGqJxMOiQwwlHra1uMhWdpiItt6VNVtqH8RpEiSwo8sARHJqI5nJ_LXocYEdJO4tUoAWZRf5483qjXGrz9vo6gIQ_w";
        logger.info("adding connectionProperties with the MSI token");
        StringRefAddr ra = new StringRefAddr("connectionProperties","accessToken=" + accessToken + ";");
        ref.add(ra);

    }
}