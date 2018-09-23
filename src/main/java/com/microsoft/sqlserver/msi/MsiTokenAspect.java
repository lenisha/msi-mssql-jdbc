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

        if ( MsiTokenCache.getExpiration() > now ) {
            return;
        }

        // token expired or was not obtained yet
        //String accessToken="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Imk2bEdrM0ZaenhSY1ViMkMzbkVRN3N5SEpsWSIsImtpZCI6Imk2bEdrM0ZaenhSY1ViMkMzbkVRN3N5SEpsWSJ9.eyJhdWQiOiJodHRwczovL2RhdGFiYXNlLndpbmRvd3MubmV0LyIsImlzcyI6Imh0dHBzOi8vc3RzLndpbmRvd3MubmV0LzcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0Ny8iLCJpYXQiOjE1Mzc2NzU2ODIsIm5iZiI6MTUzNzY3NTY4MiwiZXhwIjoxNTM3NzA0NzgyLCJhaW8iOiI0MkJnWUJEYmFKRzNKK20rcHJ2TngyMXAzTDROQUE9PSIsImFwcGlkIjoiNTdhNjI0YTItMjIzYy00MzI5LWJlMDgtMzA2YTFkM2Y0N2U4IiwiYXBwaWRhY3IiOiIyIiwiZV9leHAiOjI4ODAwMCwiaWRwIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3LyIsIm9pZCI6IjdjN2EyMGFiLThiNjItNDQ3OC1hNDc4LWRmYTMzN2U2YjRlZiIsInN1YiI6IjdjN2EyMGFiLThiNjItNDQ3OC1hNDc4LWRmYTMzN2U2YjRlZiIsInRpZCI6IjcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0NyIsInV0aSI6Ik42VHpLdWtUXzBXc1ZEbjI2a2dkQUEiLCJ2ZXIiOiIxLjAifQ.K4pIPMjyuckgWwe261CzvLHlXXenfybEvixMoWbMk_fROg5C4h09KXJQHL1avQBW_sqstOy8kpBRMT4YEWv0099oOdmyHgmb7awNcTST1z0EpmJ3Ad_zIMooT35S1zZzF-gD6xBrFEifzgOc4DJdLbEm_stTEthKoW-Zzim1Ec8b1v3h64MPE2mgvLp1lMuQct9taz1eA8flXmBndut8SyPuUDhkaeKSlQxtLTntnxrQzjqC8AeBCVI8djpwUWYdaz4cGYxOfIGg9Zr_DzhTBACvLYv39Re3tHa_8BmzuzmPnD-BdGMz0BKxbORfbce-qSFSGcfy433_D98OX-2t0Q";
        String accessToken = MsiAuthToken.aquireAndCacheMsiToken("https://database.windows.net/");

        ds.addConnectionProperty("accessToken", accessToken );
    }



}