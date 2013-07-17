package de.tu.dresden.quasy.model.db

import java.io.{FileWriter, PrintWriter, File}
import io.Source
import se.sics.prologbeans._
import gov.nih.nlm.nls.metamap.{ResultImpl, Result}
import de.tu.dresden.quasy.util.{StylaToPBTerm, PrologParser}

/**
 * @author dirk
 * Date: 6/13/13
 * Time: 12:52 PM
 */
object MetaMapCache {

    private var cacheFile:File = null
    private final val cacheSep = "\n"+("#"*30)+"\n"
    private var cache = Map[String,List[Result]]()
    private val itemSeparator = "\n#########E########\n"
    private final val parser = new PrologParser()


    def loadCache(file:File) {
        cacheFile = file
        file.getParentFile.mkdirs()
        if (!file.createNewFile()) {
            val cacheStr = Source.fromFile(cacheFile).getLines().mkString("\n")
            val entries = cacheStr.split(cacheSep)
            cache =
                (0 until (entries.size / 2)).map(i => {
                    val results = entries(i*2+1).split(itemSeparator)

                    (entries(i*2),results.map(r => {
                        val parsed = parser.parse(r).head
                        val t = StylaToPBTerm.transform(parsed)
                        new ResultImpl(t).asInstanceOf[Result]
                    }).toList)
                }).toMap
        }
    }

    def getCachedUmlsResponse(request:String) = cache.getOrElse(request,List[Result]())

    //def addToCache(request:String,response:String) = cache += (request -> response)
    def addToCache(request:String,response:List[Result]) = cache += (request -> response)

    def storeCache {
        cacheFile.getParentFile.mkdirs()
        val pw = new PrintWriter(new FileWriter(cacheFile))
        cache.dropRight(1).foreach(q => {
            pw.print(q._1)
            pw.print(cacheSep)
            pw.print(q._2.map(_.getMMOPBlist.toString).mkString(itemSeparator))
            pw.print(cacheSep)
        })
        val q = cache.last
        pw.print(q._1)
        pw.print(cacheSep)
        pw.print(q._2.map(_.getMMOPBlist.toString).mkString(itemSeparator))

        pw.close()
    }

}
