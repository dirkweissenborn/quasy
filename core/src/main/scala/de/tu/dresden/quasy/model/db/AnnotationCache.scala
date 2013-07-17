package de.tu.dresden.quasy.model.db

import java.io.{FileWriter, PrintWriter, File}
import gov.nih.nlm.nls.metamap.{ResultImpl, Result}
import de.tu.dresden.quasy.util.{Xmlizer, StylaToPBTerm, PrologParser}
import scala.io.Source
import de.tu.dresden.quasy.model.AnnotatedText

/**
 * @author dirk
 *          Date: 6/25/13
 *          Time: 1:16 PM
 */
object AnnotationCache {

    private var cacheFile:File = null
    private final val cacheSep = "\n"+("#"*30)
    private var cache = Map[String,AnnotatedText]()

    def loadCache(file:File) {
        cacheFile = file
        file.getParentFile.mkdirs()
        if (!file.createNewFile()) {
            val cacheStr = Source.fromFile(cacheFile).getLines().mkString("\n")
            val entries = cacheStr.split(cacheSep)
            cache =
                entries.filterNot(_.trim.isEmpty).map(entry => {
                    var t:AnnotatedText = null
                    try {
                        t = Xmlizer.fromXml[AnnotatedText](entry)
                    }
                    catch {
                        case e:Exception =>
                    }
                    if(t == null)
                        None
                    else
                        Some((t.text,t))
                }).flatten.toMap
        }
    }

    def getCachedAnnotatedTextOrElse(text:String, default: AnnotatedText) = cache.getOrElse(AnnotatedText.cleanText(text),default)
    def getCachedAnnotatedText(text:String) = cache.getOrElse(AnnotatedText.cleanText(text),new AnnotatedText(text))

    //def addToCache(request:String,response:String) = cache += (request -> response)
    def addToCache(text:AnnotatedText, update:Boolean = false) =
        if(update || !cache.contains(text.text))
            cache += (text.text -> text)

    def storeCache {
        cacheFile.getParentFile.mkdirs()
        val pw = new PrintWriter(new FileWriter(cacheFile))
        cache.foreach(q => {
            pw.print(Xmlizer.toXml(q._2))
            pw.print(cacheSep)
        })
        pw.close()
    }


}
