package de.tu.dresden.quasy.webservices.bioasq

import model.{DocumentsResult, LinkedLifeTriples, FindEntityResult}
import org.apache.commons.logging.LogFactory
import com.google.gson.{Gson, JsonObject, JsonParser}
import org.apache.commons.httpclient.{HttpException, HttpClient}
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import java.net.HttpURLConnection
import scala.RuntimeException
import scala.StringBuilder
import org.apache.commons.httpclient.methods.multipart.{Part, MultipartRequestEntity, StringPart}
import java.io.{StringWriter, IOException}
import org.apache.commons.io.IOUtils

/**
 * @author dirk
 * Date: 4/10/13
 * Time: 10:09 AM
 */
class BioASQServiceCall {

    final val JOCHEM_URL = "http://www.gopubmed.org/web/bioasq/jochem/json"
    final val UNIPROT_URL = "http://www.gopubmed.org/web/bioasq/uniprot/json"
    final val GO_URL = "http://www.gopubmed.org/web/bioasq/go/json"
    final val MESH_URL = "http://www.gopubmed.org/web/bioasq/mesh/json"
    final val DOID_URL = "http://www.gopubmed.org/web/bioasq/doid/json"
    final val LINKEDLIFE_URL = "http://www.gopubmed.org/web/bioasq/linkedlifedata2/triples"

    final val PMC_URL = "http://www.gopubmed.org/web/bioasq/pmc/json"

    private final val LOG = LogFactory.getLog(getClass)
    private final val jsonParser = new JsonParser
    private final var httpClient: HttpClient = new HttpClient
    private final val gson = new Gson()

    private val writer = new StringWriter()


    def getEntityConcepts[T](query: String, url:String, nr:Int = -1)(implicit m:Manifest[T]): T = {
        if (query == null) throw new NullPointerException("Query must not be null")
        val targetUrl: String = getTargetUrl(url)
        var queryString: String = "'"+query+"'"
        if (nr > 0)
            queryString += ",1,"+nr+""

        LOG.info("Query-url: "+url+"  - findEntities=\""+queryString+"\"")

        val searchJsonQuery: String = toJsonQueryForTerms("findEntities", queryString)
        //System.out.println("Query becomes:\n" + searchJsonQuery)
        val jsonObject: JsonObject = executeJsonQuery(targetUrl, searchJsonQuery)
        var result:Any = null
        if (jsonObject!=null)
            result = gson.fromJson(jsonObject.get("result"), m.erasure)
        result.asInstanceOf[T]
    }

    def getMeSHConcepts(query: String) = {
        getEntityConcepts[FindEntityResult](query,MESH_URL)
    }

    def getUniprotConcepts(query: String) = {
        getEntityConcepts[FindEntityResult](query, UNIPROT_URL)
    }

    def getGoConcepts(query: String) = {
        getEntityConcepts[FindEntityResult](query,GO_URL)
    }

    def getJochemConcepts(query: String) = {
        getEntityConcepts[FindEntityResult](query,JOCHEM_URL)
    }

    def getDoidConcepts(query: String) = {
        getEntityConcepts[FindEntityResult](query,DOID_URL)
    }

    def getLinkedLifeTriples(query: String): LinkedLifeTriples = {
        getEntityConcepts[LinkedLifeTriples](query,LINKEDLIFE_URL)
    }

    def getPmcDocuments(query: String, nr:Int) = {
        getEntityConcepts[DocumentsResult](query,PMC_URL,nr)
    }

    private def getTargetUrl(url: String): String = {
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

    private def toJsonQueryForTerms(key:String, query:String): String = {
        val builder: StringBuilder = new StringBuilder
        builder.append("{'")
        builder.append(key)
        builder.append("':[" + query + "]}")
        builder.toString
    }

    private def executeJsonQuery(uri: String, query: String): JsonObject = {

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

        var jsonObject: JsonObject = null

        while(jsonObject == null) {
            try {
                httpClient.executeMethod(method)
                val resultBody = IOUtils.toString(method.getResponseBodyAsStream)
                LOG.info("Result:" + resultBody)
                jsonObject = jsonParser.parse(resultBody).getAsJsonObject
            }
            catch {
                case exception: Exception => {
                    LOG.error("Servicecall error, trying again!")
                    httpClient = new HttpClient()
                }

            }
            finally {
                method.releaseConnection
            }
        }

        jsonObject
    }
}

object BioASQServiceCall {

    def main(args: Array[String]) {
        val service = new BioASQServiceCall
        //httpClient.getParams.setAuthenticationPreemptive(true)
        //service.getMeSHConcepts("6-(hydroxymethyl)oxane-2,3,4,5-tetrol")
        /*getGoConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getUniprotConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getJochemConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getDoidConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")*/
        //service.getLinkedLifeTriples("Dextrose")
        service.getPmcDocuments("What is the role of thyroid hormones administration in the treatment of heart failure?",10)
        System.exit(0)
    }
}
