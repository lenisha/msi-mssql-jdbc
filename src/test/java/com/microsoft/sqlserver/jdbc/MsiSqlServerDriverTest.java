
package com.microsoft.sqlserver.jdbc;


import com.microsoft.sqlserver.msi.MsiTokenCache;
import java.util.Date;

public class MsiSqlServerDriverTest   {

    public static void main(String[] args) {
        long unix = 1537593354;
        String gmtString = "9/22/2018 5:15:54 AM +00:00";

        Date date = null;
        try {

            long exp = MsiTokenCache.getExpiration();
            MsiTokenCache.saveExpiration(gmtString);
            exp = MsiTokenCache.getExpiration();
            MsiTokenCache.saveExpiration(gmtString);
            exp = MsiTokenCache.getExpiration();
        } catch (Exception e) {
            e.printStackTrace();
        }

        /**String at="eyJ0eXAiOiJKV1QiLCJhbGciOiJSUzI1NiIsIng1dCI6Imk2bEdrM0ZaenhSY1ViMkMzbkVRN3N5SEpsWSIsImtpZCI6Imk2bEdrM0ZaenhSY1ViMkMzbkVRN3N5SEpsWSJ9.eyJhdWQiOiJodHRwczovL2RhdGFiYXNlLndpbmRvd3MubmV0LyIsImlzcyI6Imh0dHBzOi8vc3RzLndpbmRvd3MubmV0LzcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0Ny8iLCJpYXQiOjE1Mzc1NjQyNTQsIm5iZiI6MTUzNzU2NDI1NCwiZXhwIjoxNTM3NTkzMzU0LCJhaW8iOiI0MkJnWU5DNjFQOC9ZM0hMOFZmemE2N0huazcvQVFBPSIsImFwcGlkIjoiNTdhNjI0YTItMjIzYy00MzI5LWJlMDgtMzA2YTFkM2Y0N2U4IiwiYXBwaWRhY3IiOiIyIiwiZV9leHAiOjI4ODAwMCwiaWRwIjoiaHR0cHM6Ly9zdHMud2luZG93cy5uZXQvNzJmOTg4YmYtODZmMS00MWFmLTkxYWItMmQ3Y2QwMTFkYjQ3LyIsIm9pZCI6IjdjN2EyMGFiLThiNjItNDQ3OC1hNDc4LWRmYTMzN2U2YjRlZiIsInN1YiI6IjdjN2EyMGFiLThiNjItNDQ3OC1hNDc4LWRmYTMzN2U2YjRlZiIsInRpZCI6IjcyZjk4OGJmLTg2ZjEtNDFhZi05MWFiLTJkN2NkMDExZGI0NyIsInV0aSI6InJHVDE5Tk82dTBLbGVJUC1BTE0yQUEiLCJ2ZXIiOiIxLjAifQ.hjW6IiVAvjkicNODJUSwePmsRu6gTyNlsLYwcguWkmQErCnvAn03xsFLnrqVDTU0Cxi5kPOYHehPa8qwx_7ENATiNBiY0Hef-cW7Em_6DY3ndYeIsYtMBkxXMfMavjuUkC2SLr5dwYcJOH3QkeJD1S0b42QZKS8C2cSN2fYnPObYCSmN46h9yZQ902wyBKTSzkl_9OsDWXYCM9jkXGjRe4aRngj_QwGrDBydh6YG_bVK0B0o5R-EIN3j7pxoNHiBP5m7NUK0sx0eZpYmgZU7sxG8ivPrH5r9-FwFunz49PrKGxTOvBpzq0-n7JaSjpPLP_eV5Y5tzR8M8dm55evFuA";

        JWSObject jwsObject;

        try {
            jwsObject = JWSObject.parse(at);
            JSONObject json = jwsObject.getPayload().toJSONObject();
            boolean has = json.containsKey("exp");
            if (  has )
            {
                Long ltime = (Long)json.get("exp");

                System.out.print("hooray:" + ltime.longValue());
            }
        } catch (java.text.ParseException e) {
            // Invalid JWS object encoding
        }**/

    }
}