package de.tu.dresden.quasy.index

import org.apache.commons.logging.LogFactory
import java.io.{FileInputStream, File}
import org.apache.lucene.index.{DirectoryReader, IndexWriterConfig, IndexWriter}
import org.apache.commons.compress.archivers.tar.{TarArchiveEntry, TarArchiveInputStream}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.search.IndexSearcher
import java.util.zip.GZIPInputStream
import scala.io.Source
import org.apache.lucene.queryparser.classic.QueryParser
import de.tu.dresden.quasy.util.{MachineOutputParser}
import scala.actors.Actor
import de.tu.dresden.quasy.util.MMUtterance
import scala.Some
import scala.collection.JavaConversions._

/**
 * @author dirk
 *          Date: 7/18/13
 *          Time: 10:11 AM
 */
object IndexMetaMappedBaselineFromGz {

    private val LOG = LogFactory.getLog(getClass)

    var INDEX_DIR:File = null
    var INDEX_WRITER:IndexWriter = null
    var mmoToXml = ""

    def main(args:Array[String]) {

        val inputFile: String = args(0)
        val is = new GZIPInputStream(new FileInputStream(inputFile))
        INDEX_DIR = new File(args(1))
        val _override = false

        if(!INDEX_DIR.mkdirs() && _override){
            INDEX_DIR.listFiles().foreach(_.delete())
        }

        val analyzer = new StandardAnalyzer(Version.LUCENE_40)
        val config = new IndexWriterConfig(Version.LUCENE_40,analyzer)
        config.setRAMBufferSizeMB(256)

        val fSDirectory = FSDirectory.open(INDEX_DIR)
        INDEX_WRITER = new IndexWriter(fSDirectory, config)
        INDEX_WRITER.commit()

        var future:writingActor.Future[Any] = null

        writingActor.start()
        var currentUtterance = ""

        val reader:DirectoryReader = DirectoryReader.open(fSDirectory)
        val searcher = new IndexSearcher(reader)
        var analyze = _override || reader == null
        var counter = 0
        var skipCounter = 0
        val utteranceRegex = """utterance\('([^']+)',""".r
        val parser = new MachineOutputParser

        LOG.info(inputFile+": Processing: "+inputFile)

        Source.fromInputStream(is).getLines().foreach(line => {
            try {
                currentUtterance += line + "\n"
                if(line.equals("'EOU'.")) {
                    if(!analyze) {
                        try {
                            val utterance = utteranceRegex.findFirstIn(currentUtterance).getOrElse("0.0.0")
                            val id = utterance.substring(utterance.indexOf('\''),utterance.lastIndexOf('\''))
                            val Array(pmid,sec,num) = id.split("""\.""",3)

                            val query = new QueryParser(Version.LUCENE_40, "", analyzer ).
                                parse("pmid:"+pmid+" AND section:"+sec+" AND number:"+num)
                            if(searcher.search(query,1).totalHits == 0) {
                                analyze = true
                            } else {
                                skipCounter += 1
                                if(skipCounter % 10000 == 0) {
                                    LOG.info(inputFile+": "+skipCounter+" utterances skipped!")
                                }
                            }
                        }
                        catch {
                            case e:Exception =>  {
                                LOG.error(e.getMessage+ ", while trying to find utterance in index. Skipping it!")
                                skipCounter += 1
                                if(skipCounter % 10000 == 0) {
                                    LOG.info(inputFile+": "+skipCounter+" utterances skipped!")
                                }
                            }
                        }
                    }
                    if(analyze) {
                        try {
                            val mmUtterance = parser.parse(currentUtterance)
                            if(mmUtterance ne null) {
                                if (future != null)
                                    future.apply()
                                future = writingActor !! Some(mmUtterance)
                            } else
                                analyze = false  // This item was not indexed so it might be that the following items are already indexed
                            }
                        catch {
                            case e:Exception => {
                                LOG.error(e.getMessage+ ", while parsing. Skipping it!")
                                analyze = false
                            }
                        }
                    }

                    counter += 1
                    currentUtterance = ""

                    if(counter % 1000 == 0) {
                        LOG.info(inputFile+": "+counter+" utterances processed!")
                    }
                }
            }
            catch {
                case e:Exception => {
                    LOG.error(inputFile+": "+e.printStackTrace())
                }
            }
        })

        is.close()
        writingActor !! None
    }

    val writingActor = new Actor {
        def act() {
            loop {
                receive {
                    case Some(utterance:MMUtterance)=> {
                        val docs = IndexMetaMappedBaselineFromTar.toDocuments(utterance)
                        INDEX_WRITER.addDocuments(docs)
                        sender ! true //give something back
                    }
                    case None => {
                        INDEX_WRITER.close()
                        exit()
                    }
                    case "commit" => INDEX_WRITER.commit()
                }
            }
        }
    }

}
