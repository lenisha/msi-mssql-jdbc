package com.microsoft.sqlserver.msi;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.Reader;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

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
            //logger.debug("Token Response: " + json);

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
}
