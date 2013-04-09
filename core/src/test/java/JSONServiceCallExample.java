import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;


public class JSONServiceCallExample {


    private static final Log logger = LogFactory.getLog(JSONServiceCallExample.class);

    private static final String sessionFetcherUrl = "http://www.gopubmed.org/web/bioasq/mesh/json";

    private static final JsonParser jsonParser = new JsonParser();

    public static HttpClient httpClient;



    public static void main(String []args)
    {
        httpClient = new HttpClient();
        httpClient.getParams().setAuthenticationPreemptive(true);

        //Logger httpLogger = Logger.getLogger("org.apache.commons.httpclient");
        //httpLogger.setLevel(Level.ERROR);

        JSONServiceCallExample.getMeSHConcepts("cancer");

        System.exit(0);
    }


    public static ArrayList<String> getMeSHConcepts (String query) {


        if (query == null)
            throw new NullPointerException("Query must not be null");

        // Retrieve session URL
        String targetUrl = getTargetUrl(httpClient, sessionFetcherUrl);


        String queryString =  query;

        //DEBUG
        //System.out.println(queryString);

        String searchJsonQuery = toJsonQueryForTerms("findEntities", queryString);
        //DEBUG
        System.out.println("Query becomes:\n"+searchJsonQuery);

        JsonObject jsonObject = executeJsonQuery(httpClient, targetUrl, searchJsonQuery);

        return null;
    }


    private static String getTargetUrl(HttpClient httpClient, String url) {
        GetMethod basicGet = new GetMethod(url);
        try {
            int responseCode = httpClient.executeMethod(basicGet);
            if (responseCode != HttpURLConnection.HTTP_OK)
                throw new RuntimeException("Invalid response code " + responseCode + ".");
            String response = basicGet.getResponseBodyAsString();
            return response;
        } catch (HttpException exception) {
            throw new RuntimeException(exception);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            basicGet.releaseConnection();
        }
    }



    private static String toJsonQueryForTerms(String key, String aQuery) {
        //"{'findEntities':['gene',0,10]}"
        StringBuilder builder = new StringBuilder();
        builder.append("{'");
        builder.append(key);
        builder.append("':['"+aQuery+"']}");
        return builder.toString();
    }

    private static JsonObject executeJsonQuery(HttpClient httpClient, String uri, String query) {
        PostMethod method = new PostMethod(uri);
        method.setDoAuthentication(true);
        method.addRequestHeader("User-Agent", "JSONServiceCallExample");
        method.addRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        method.addRequestHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3");
        method.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
        method.addRequestHeader("Keep-Alive", "300");
        method.addRequestHeader("Connection", "keep-alive");

        StringPart fetchStringPart = new StringPart("json", query);


        fetchStringPart.setCharSet("utf-8");
        fetchStringPart.setContentType("application/json");
        method.setRequestEntity(new MultipartRequestEntity(new Part[] { fetchStringPart }, method.getParams()));

        try {
            httpClient.executeMethod(method);
            String resultBody = method.getResponseBodyAsString();

            //DEBUG
            System.out.println("Result:"+resultBody);


            JsonObject jsonObject = jsonParser.parse(resultBody).getAsJsonObject();
            return jsonObject;
        } catch (HttpException exception) {
            throw new RuntimeException(exception);
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        } finally {
            method.releaseConnection();
        }
    }



}
