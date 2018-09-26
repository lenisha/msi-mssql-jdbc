package com.microsoft.sqlserver.msi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.microsoft.sqlserver.jdbc.StringUtils;
import com.nimbusds.jose.JWSObject;
import net.minidev.json.JSONObject;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import javax.naming.Reference;
import javax.naming.StringRefAddr;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MsiAuthToken {
    public static final String API_VERSION="2017-09-01";
    public static final String MSI_ENDPOINT="MSI_ENDPOINT";
    public static final String MSI_SECRET="MSI_SECRET";

    protected static final Logger logger = LogManager.getLogger(MsiAuthToken.class);


    public static String aquireMsiToken(String resourceURI) throws Exception  {

        String endpoint = System.getenv("MSI_ENDPOINT");
        String secret = System.getenv("MSI_SECRET");

        if (endpoint == null || endpoint.isEmpty()) {
            logger.error("NO MSI_ENDPOINT FOUND");
            throw new NoMSIFoundException("NO MSI_ENDPOINT FOUND");
        }
        if (secret == null || secret.isEmpty()) {
            logger.error("NO MSI_SECRET FOUND");
            throw new NoMSIFoundException("NO MSI_SECRET FOUND");
        }
        String tokenUrl = endpoint + "?resource="+resourceURI+"&api-version=" + API_VERSION;

        Map<String, String> headers = new HashMap<>();
        headers.put("Secret", secret);
        headers.put("Accept", "application/json, text/javascript, */*");


        MsiAuthResponse response = null;
        try {
            logger.debug("Invoking endpoint to get token: " + tokenUrl);
            final String json = HttpHelper.executeHttpGet(tokenUrl,headers,null);

            response = convertJsonToObject(json, MsiAuthResponse.class);
            logger.debug("MSI Access Token Expiration: " + response.getExpiresOn());

        } catch (Exception ex) {
            logger.error("Error Getting MSI token",ex);
            throw ex;
        }

        return response.getAccessToken();
    }



    public static <T> T convertJsonToObject(final String json, final Class<T> clazz) throws  JsonSyntaxException, JsonIOException {
        final Reader reader = new StringReader(json);
        final Gson gson = new GsonBuilder().create();
        return gson.fromJson(reader, clazz);
    }

    public static long getTokenExpiration(String token) throws Exception {
        JWSObject jwsObject;
        try {
            jwsObject = JWSObject.parse(token);
            JSONObject json = jwsObject.getPayload().toJSONObject();
            boolean hasExpiration = json.containsKey("exp");
            if ( hasExpiration )
            {
                Long ltime = (Long)json.get("exp");
                logger.debug("Parsed access token, expiration:" + ltime);
                return ltime.longValue();
            }
        } catch (java.text.ParseException e) {
            // Invalid JWS object encoding
            logger.error("Error parsing access token:" + e.getMessage());
            throw e;
        }
        return 0;
    }

    public static boolean isMsiEnabled(String jdbcUrl) {

        // Environment variable overrides any context setting or url set
        String msiEnableEnv = System.getenv("JDBC_MSI_ENABLE");
        if (!StringUtils.isEmpty(msiEnableEnv) && msiEnableEnv.compareToIgnoreCase("true") == 0) {
            logger.debug("MSI Enabled in Environement");
            return true;
        }

        // Application Setting variable overrides any context setting or url set
        msiEnableEnv = System.getenv("APPSETTING_JDBC_MSI_ENABLE");
        if ( !StringUtils.isEmpty(msiEnableEnv) && msiEnableEnv.compareToIgnoreCase("true") == 0) {
            logger.debug("MSI Enabled in AppSetting Environement");
            return true;
        }

        // URL Setting variable overrides any context setting
        if ( jdbcUrl != null ) {
            jdbcUrl = jdbcUrl.replaceAll("\\s+", "");

            if ( jdbcUrl.indexOf("msiEnable=true") > 0) {
                logger.debug("MSI Enabled in Url reference");
                return true;
            }
        }
        return  false;
    }

    static void cacheToken(String accessToken) throws Exception {
        logger.debug("Caching Token and expiration");
        MsiTokenCache.saveExpiration(getTokenExpiration(accessToken));
        MsiTokenCache.saveToken(accessToken);
    }
}
