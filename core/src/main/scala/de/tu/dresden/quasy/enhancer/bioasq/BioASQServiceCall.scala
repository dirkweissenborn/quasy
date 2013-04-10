package de.tu.dresden.quasy.enhancer.bioasq

import org.apache.commons.logging.{LogFactory}
import com.google.gson.{Gson, JsonObject, JsonParser}
import org.apache.commons.httpclient.{HttpException, HttpClient}
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import java.net.HttpURLConnection
import scala.RuntimeException
import scala.StringBuilder
import org.apache.commons.httpclient.methods.multipart.{Part, MultipartRequestEntity, StringPart}
import java.io.IOException

/**
 * @author dirk
 * Date: 4/10/13
 * Time: 10:09 AM
 */
object BioASQServiceCall {

    final val JOCHEM_URL = "http://www.gopubmed.org/web/bioasq/jochem/json"
    final val UNIPROT_URL = "http://www.gopubmed.org/web/bioasq/uniprot/json"
    final val GO_URL = "http://www.gopubmed.org/web/bioasq/go/json"
    final val MESH_URL = "http://www.gopubmed.org/web/bioasq/mesh/json"
    final val DOID_URL = "http://www.gopubmed.org/web/bioasq/doid/json"
    final val LINKEDLIFE_URL = "http://www.gopubmed.org/web/bioasq/linkedlifedata/triples"

    private final val LOG = LogFactory.getLog(getClass)
    private final val jsonParser = new JsonParser
    private final val httpClient: HttpClient = new HttpClient
    private final val gson = new Gson()

    def main(args: Array[String]) {
        //httpClient.getParams.setAuthenticationPreemptive(true)
        /*getMeSHConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getGoConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getUniprotConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getJochemConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getDoidConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")*/
        getLinkedLifeTriples("What is the role of thyroid hormones administration in the treatment of heart failure?")
        System.exit(0)
    }

    def getEntityConcepts(query: String, url:String): FindEntityResult = {
        if (query == null) throw new NullPointerException("Query must not be null")
        LOG.info("Query-url: "+url+"  - findEntities=\""+query+"\"")
        val targetUrl: String = getTargetUrl(httpClient, url)
        val queryString: String = query
        val searchJsonQuery: String = toJsonQueryForTerms("findEntities", queryString)
        //System.out.println("Query becomes:\n" + searchJsonQuery)
        val jsonObject: JsonObject = executeJsonQuery(httpClient, targetUrl, searchJsonQuery)
        gson.fromJson(jsonObject.get("result"), classOf[FindEntityResult])
    }

    def getMeSHConcepts(query: String) = {
        getEntityConcepts(query,MESH_URL)
    }

    def getUniprotConcepts(query: String) = {
        getEntityConcepts(query, UNIPROT_URL)
    }

    def getGoConcepts(query: String) = {
        getEntityConcepts(query,GO_URL)
    }

    def getJochemConcepts(query: String) = {
        getEntityConcepts(query,JOCHEM_URL)
    }

    def getDoidConcepts(query: String) = {
        getEntityConcepts(query,DOID_URL)
    }

    def getLinkedLifeTriples(query: String): LinkedLifeTriples = {
        if (query == null) throw new NullPointerException("Query must not be null")
        LOG.info("Query-url: "+LINKEDLIFE_URL+"  - findTriples=\""+query+"\"")
        val targetUrl: String = getTargetUrl(httpClient, LINKEDLIFE_URL)
        val queryString: String = query
        val searchJsonQuery: String = toJsonQueryForTerms("findTriples", queryString)
        System.out.println("Query becomes:\n" + searchJsonQuery)
        val jsonObject: JsonObject = executeJsonQuery(httpClient, targetUrl, searchJsonQuery)
        //gson.fromJson(jsonObject.get("result"), classOf[FindEntityResult])
        null
    }

    private def getTargetUrl(httpClient: HttpClient, url: String): String = {
        val basicGet: GetMethod = new GetMethod(url)
        try {
            val responseCode: Int = httpClient.executeMethod(basicGet)
            if (responseCode != HttpURLConnection.HTTP_OK) throw new RuntimeException("Invalid response code " + responseCode + ".")
            val response: String = basicGet.getResponseBodyAsString
            response
        }
        catch {
            case exception: HttpException => {
                throw new RuntimeException(exception)
            }
            case exception: IOException => {
                throw new RuntimeException(exception)
            }
        }
        finally {
            basicGet.releaseConnection
        }
    }

    private def toJsonQueryForTerms(key: String, aQuery: String): String = {
        val builder: StringBuilder = new StringBuilder
        builder.append("{'")
        builder.append(key)
        builder.append("':['" + aQuery + "']}")
        builder.toString
    }

    private def executeJsonQuery(httpClient: HttpClient, uri: String, query: String): JsonObject = {
        val method: PostMethod = new PostMethod(uri)
        method.setDoAuthentication(true)
        method.addRequestHeader("User-Agent", "JSONServiceCallExample")
        method.addRequestHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        method.addRequestHeader("Accept-Language", "de-de,de;q=0.8,en-us;q=0.5,en;q=0.3")
        method.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7")
        method.addRequestHeader("Keep-Alive", "300")
        method.addRequestHeader("Connection", "keep-alive")
        val fetchStringPart: StringPart = new StringPart("json", query)
        fetchStringPart.setCharSet("utf-8")
        fetchStringPart.setContentType("application/json")
        method.setRequestEntity(new MultipartRequestEntity(Array[Part](fetchStringPart), method.getParams))
        try {
            httpClient.executeMethod(method)
            val resultBody: String = method.getResponseBodyAsString
            LOG.info("Result:" + resultBody)
            val jsonObject: JsonObject = jsonParser.parse(resultBody).getAsJsonObject
            return jsonObject
        }
        catch {
            case exception: HttpException => {
                throw new RuntimeException(exception)
            }
            case exception: IOException => {
                throw new RuntimeException(exception)
            }
        }
        finally {
            method.releaseConnection
        }
    }
}
