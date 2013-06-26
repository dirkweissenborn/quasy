package de.tu.dresden.quasy.webservices.bioasq

import de.tu.dresden.quasy.webservices.bioasq.model.{FindPubMedCitationsRequest, BioASQServiceResult, FindEntityRequest, LinkedLifeTriples}
import org.apache.commons.logging.LogFactory
import com.google.gson.{Gson, JsonObject, JsonParser}
import org.apache.commons.httpclient.{HttpException, HttpClient}
import org.apache.commons.httpclient.methods.{PostMethod, GetMethod}
import java.net.{HttpURLConnection}
import scala.RuntimeException
import org.apache.commons.httpclient.methods.multipart.{Part, MultipartRequestEntity, StringPart}
import java.io.{IOException}
import org.apache.commons.io.IOUtils
import de.tu.dresden.quasy.webservices.model.DocumentsResult

/**
 * @author dirk
 * Date: 4/10/13
 * Time: 10:09 AM
 */
class BioASQService {

    final val JOCHEM_URL = "http://www.gopubmed.org/web/bioasq/jochem/json"
    final val UNIPROT_URL = "http://www.gopubmed.org/web/bioasq/uniprot/json"
    final val GO_URL = "http://www.gopubmed.org/web/bioasq/go/json"
    final val MESH_URL = "http://www.gopubmed.org/web/bioasq/mesh/json"
    final val DOID_URL = "http://www.gopubmed.org/web/bioasq/doid/json"
    final val LINKEDLIFE_URL = "http://www.gopubmed.org/web/bioasq/linkedlifedata2/triples"

    final val PMC_URL = "http://www.gopubmed.org/web/bioasq/pmc/json"
    final val PUBMED_URL = "http://gopubmed.org/web/gopubmedbeta/bioasq/pubmed"

    private final val LOG = LogFactory.getLog(getClass)
    private final val jsonParser = new JsonParser
    private final var httpClient: HttpClient = new HttpClient
    private final val gson = new Gson()

    def getEntityConcepts[T](query: String, url:String, nr:Int = -1)(implicit m:Manifest[T]): T = {
        if (query == null) throw new NullPointerException("Query must not be null")
        val targetUrl: String = getTargetUrl(url)
        var requestObjects:Array[Any] = Array(query)
        if (nr > 0)
            requestObjects ++= Array(0,math.min(nr,1000))


        //LOG.info("Query-url: "+url+"  - findEntities=\""+queryString+"\"")

        val request =
            if (url.equals(PUBMED_URL))
                FindPubMedCitationsRequest(requestObjects)
            else
                FindEntityRequest(requestObjects)

        val searchJsonQuery: String = gson.toJson(request)
        //System.out.println("Query becomes:\n" + searchJsonQuery)
        val jsonObject: JsonObject = executeJsonQuery(targetUrl, searchJsonQuery)
        var result:Any = null
        if (jsonObject!=null)
            result = gson.fromJson(jsonObject.get("result"), m.erasure)
        result.asInstanceOf[T]
    }

    def getMeSHConcepts(query: String) = {
        getEntityConcepts[BioASQServiceResult](query,MESH_URL)
    }

    def getUniprotConcepts(query: String) = {
        getEntityConcepts[BioASQServiceResult](query, UNIPROT_URL)
    }

    def getGoConcepts(query: String) = {
        getEntityConcepts[BioASQServiceResult](query,GO_URL)
    }

    def getJochemConcepts(query: String) = {
        getEntityConcepts[BioASQServiceResult](query,JOCHEM_URL)
    }

    def getDoidConcepts(query: String) = {
        getEntityConcepts[BioASQServiceResult](query,DOID_URL)
    }

    def getLinkedLifeTriples(query: String): LinkedLifeTriples = {
        getEntityConcepts[LinkedLifeTriples](query,LINKEDLIFE_URL)
    }

    def getPmcDocuments(query: String, nr:Int) = {
        getEntityConcepts[DocumentsResult](query,PMC_URL,nr)
    }

    def getPubmedDocuments(query: String, nr:Int) = {
        getEntityConcepts[DocumentsResult](query,PUBMED_URL,nr)
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
                //LOG.info("Result:" + resultBody)
                jsonObject = jsonParser.parse(resultBody).getAsJsonObject
            }
            catch {
                case exception: Exception => {
                    exception.printStackTrace()
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

object BioASQService {

    def main(args: Array[String]) {
        val service = new BioASQService
        //httpClient.getParams.setAuthenticationPreemptive(true)
        //service.getMeSHConcepts("Drugs that bind to but do not activate SEROTONIN 5-HT1 RECEPTORS, thereby blocking the actions of SEROTONIN 5-HT1 RECEPTOR AGONISTS. Included under this heading are antagonists for one or more of the specific 5-HT1 receptor subtypes.")
        /*getGoConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getUniprotConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getJochemConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")
        getDoidConcepts("What is the role of thyroid hormones administration in the treatment of heart failure?")*/
        //service.getLinkedLifeTriples("Dextrose")
        val res = service.getPubmedDocuments("1234321",1)
        assert(res.documents.head.pmid == "1234321")
        System.exit(0)
    }
}

