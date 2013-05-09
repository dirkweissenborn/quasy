package de.tu.dresden.quasy.webservices.transinsight

import org.apache.commons.logging.LogFactory
import com.google.gson.{JsonObject, Gson, JsonParser}
import org.apache.commons.httpclient.{HttpClient}
import org.apache.commons.httpclient.methods.{PostMethod}
import java.net.{URL}
import org.apache.commons.httpclient.methods.multipart.{Part, MultipartRequestEntity, StringPart}
import org.apache.commons.io.IOUtils
import de.tu.dresden.quasy.model.Span

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 2:03 PM
 */
class TransinsightService {

    private final val url = "http://www.gopubmed.org/web/annotate/webservice/annotate"

    private final val LOG = LogFactory.getLog(getClass)
    private final val jsonParser = new JsonParser
    private final var httpClient: HttpClient = new HttpClient
    private final val gson = new Gson()


    def getEntityConcepts(query: String):ServiceResponse = {
        if (query == null) throw new NullPointerException("Query must not be null")

        //System.out.println("Query becomes:\n" + searchJsonQuery)
        val jsonObject: JsonObject = executeJsonQuery(url, query)
        var result:ServiceResponse = null
        try {
            if (jsonObject!=null)
                result = gson.fromJson(jsonObject.get("result"), classOf[ServiceResponse])
            }
        catch {
            case e => LOG.error("Error parsing result of query: "+query);e.printStackTrace()//..
        }

        result
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

        method.setParameter("text", query)

        var jsonObject: JsonObject = null

        while(jsonObject == null) {
            try {
                httpClient.executeMethod(method)
                val resultBody = IOUtils.toString(method.getResponseBodyAsStream).replaceAll("\"uri:","\"http:")
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

    case class ServiceResponse(val annotations:Array[Annot], val terms:Array[Term], val time_ms:Int)

    case class Annot(val conceptUri:URL, val ranges:Array[Span])

    case class Term(val label:String,val uri:URL)

    case class ServiceRequest(val text:String)
}

object TransinsightService {
    def main(args:Array[String]) {
        val service = new TransinsightService
        val result = service.getEntityConcepts("Does change in blood pressure predict heart disease")
        result
    }
}
