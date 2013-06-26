package de.tu.dresden.quasy.webservices.nlm

import org.apache.commons.logging.LogFactory
import org.apache.commons.httpclient.{NameValuePair, HttpClient}
import org.apache.commons.httpclient.methods.GetMethod
import org.apache.commons.io.IOUtils
import xml.XML
import de.tu.dresden.quasy.webservices.model.{Document, DocumentsResult}
import org.apache.commons.httpclient.cookie.CookiePolicy
import org.apache.http.params.{CoreConnectionPNames, BasicHttpParams, HttpConnectionParams}
import org.apache.http.impl.client.DefaultHttpClient
import org.apache.commons.httpclient.params.DefaultHttpParams
import org.apache.http.client.params.HttpClientParams

/**
 * @author dirk
 * Date: 6/11/13
 * Time: 3:01 PM
 */
class NlmWebservice {

    private final val queryUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/esearch.fcgi"  //db=pubmed&term=
    private final val articleUrl = "http://eutils.ncbi.nlm.nih.gov/entrez/eutils/efetch.fcgi" //db=pubmed&id=17849242&rettype=xml&retmode=text

    private final val LOG = LogFactory.getLog(getClass)

    private final var httpClient = new HttpClient()
    httpClient.getParams.setParameter(CoreConnectionPNames.SO_TIMEOUT,10000)

    def getPubmedDocuments(query:String,nr:Int) = {
        val pmids = fetchPmids(query).take(nr)

        if (pmids.size > 0)
            fetchArticles(pmids)
        else
            new DocumentsResult(Array())
    }

    def fetchPmids(query:String) = {
        val method = new GetMethod(queryUrl)
        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES)
        method.setQueryString(Array(
            new NameValuePair("db","pubmed"),
            new NameValuePair("term",query)))

        var pmids:List[Int] = null
        while(pmids == null) {
            try {
                httpClient.executeMethod(method)
                val resultBody = IOUtils.toString(method.getResponseBodyAsStream)
                val xml = XML.loadString(resultBody)

                pmids = (xml \\ "Id").map(_.text.toInt).toList
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

        pmids
    }

    def fetchArticles(pmids:List[Int]) = {
        val method = new GetMethod(articleUrl)
        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES)
        val pmidStr = pmids.map(_.toString).mkString(",")
        method.setQueryString(Array(
            new NameValuePair("db","pubmed"),
            new NameValuePair("rettype","xml"),
            new NameValuePair("retmode","text"),
            new NameValuePair("id",pmidStr)))

        var result: DocumentsResult = null
        while(result == null) {
            try {
                httpClient.executeMethod(method)
                val resultBody = IOUtils.toString(method.getResponseBodyAsStream)
                val xml = XML.loadString(resultBody)
                val xTitle = (xml \\ "title")
                if(!xTitle.isEmpty && xTitle.head.text == "Bad Gateway!")
                    throw new RuntimeException(resultBody)

                val docs = xml.child.toArray.
                    map(c => (c \ "MedlineCitation")).
                    filter(_.size > 0).map(_.head).map(article => {
                        val abstr = (article \\ "AbstractText").toList.map(_.text).mkString("\n")
                        val pmid = (article \ "PMID").last.text
                        val title = (article \\ "ArticleTitle").last.text
                        val pmcId = (article \\ "OtherID").toList.map(_.text).find(_.startsWith("PMC")).getOrElse("")
                        if (!pmcId.isEmpty) {
                            val text = fetchPmc(pmcId)
                            if (text.isEmpty)
                                new Document(pmid,abstr,title,Array[String]())
                            else
                                new Document(pmid,abstr,title,Array(text))
                        }
                        else
                            new Document(pmid,abstr,title,Array[String]())
                    })

                result = new DocumentsResult(docs)
            }
            catch {
                case exception: Exception => {
                    exception.printStackTrace()
                    LOG.error("Servicecall error, trying again!")
                    httpClient = new HttpClient()
                    httpClient.getParams.setParameter(CoreConnectionPNames.SO_TIMEOUT,10000)
                }

            }
            finally {
                method.releaseConnection
            }
        }

        result
    }

    def fetchPmc(pmcId:String) = {
        val method = new GetMethod(articleUrl)
        method.getParams().setCookiePolicy(CookiePolicy.IGNORE_COOKIES)

        method.setQueryString(Array(
            new NameValuePair("db","pmc"),
            new NameValuePair("rettype","xml"),
            new NameValuePair("retmode","text"),
            new NameValuePair("id",pmcId)))

        var result: String = null
        while(result == null) {
            try {
                httpClient.executeMethod(method)
                val resultBody = IOUtils.toString(method.getResponseBodyAsStream)

                val start = resultBody.indexOf("<body>")
                if (start > -1) {
                    val end = resultBody.indexOf("</body>")
                    result = resultBody.substring(start+6,end)
                    result = result.replaceAll("(</title>)",".\n").replaceAll("(</p>|</td>)","\n").replaceAll("<[^>]+>","")
                }
                else
                    result = ""
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

        result
    }

}

object NlmWebservice {
    def main(args:Array[String]) {
        val service = new NlmWebservice

        service.getPubmedDocuments("23435440[uid]",2)
    }
}
