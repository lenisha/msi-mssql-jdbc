package com.microsoft.sqlserver.msi;


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class MsiTokenCache {

    private static String KEY_EXPIRE = "expiresOn";

    private static ConcurrentHashMap<String,Long> cache = null;

    protected static ConcurrentHashMap<String,Long> getCache() {
        if (cache == null)
           cache = new ConcurrentHashMap<String, Long>();
        return cache;
    }

    public static long getExpiration() {
        cache = getCache();
        Long expiration = cache.get(KEY_EXPIRE);

        if ( expiration != null )
            return expiration.longValue();
        else
            return 0;
    }

    public static void saveExpiration(String expiration) throws Exception {
        try {
            Date dateFmt = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss a X").parse(expiration);
            long expLong= dateFmt.getTime() / 1000;

            saveExpiration(expLong);
        } catch (ParseException e) {
            e.printStackTrace();
            throw e;
        }
    }

    private static void saveExpiration(long expiration) {
        cache = getCache();
        cache.put(KEY_EXPIRE,new Long(expiration));
    }

}
