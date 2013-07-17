package de.tu.dresden.quasy.run

import org.apache.commons.logging.LogFactory
import java.io._
import org.apache.lucene.index.{DirectoryReader, IndexWriterConfig, IndexWriter}
import org.apache.lucene.util.Version
import org.apache.lucene.store.FSDirectory
import java.util.zip.GZIPInputStream
import scala.io.Source
import scala.actors.Actor
import org.apache.lucene.document._
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.tar.{TarArchiveInputStream, TarArchiveEntry}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.analysis.standard.StandardAnalyzer
import scala.collection.JavaConversions._
import scala.Some
import de.tu.dresden.quasy.util.{MMUtterance, MachineOutputParser}

/**
 * @author dirk
 *          Date: 7/15/13
 *          Time: 1:31 PM
 */
object IndexMetaMappedBaseline {

    private val LOG = LogFactory.getLog(getClass)

    var INDEX_DIR:File = null
    var INDEX_WRITER:IndexWriter = null
    var mmoToXml = ""

    def main(args:Array[String]) {
        val metaMapDump = new File(args(0))

        val stream = new ArchiveStreamFactory().createArchiveInputStream("tar", new FileInputStream(metaMapDump)).asInstanceOf[TarArchiveInputStream]
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
        var entry:TarArchiveEntry = null
        var currentUtterance = ""

        val reader:DirectoryReader = DirectoryReader.open(fSDirectory)
        val searcher = new IndexSearcher(reader)
        var analyze = _override || reader == null
        var counter = 0
        var skipCounter = 0
        val utteranceRegex = """utterance\('([^']+)',""".r

        while({entry = stream.getNextTarEntry;entry != null}) {
            LOG.info("Processing: "+entry.getName)
            /* Get Size of the file and create a byte array for the size */
            val content = new Array[Byte](entry.getSize.toInt)
            val offset=0
            /* Read file from the archive into byte array */
            stream.read(content, offset, content.length - offset)
            /* Use IOUtiles to write content of byte array to physical file */
            val is = new GZIPInputStream(new ByteArrayInputStream(content))
            Source.fromInputStream(is).getLines().foreach(line => {
                try {
                    currentUtterance += line + "\n"
                    if(line.equals("'EOU'.")) {
                        if(!analyze) {
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
                                    LOG.info(skipCounter+" utterances skipped!")
                                }
                            }
                        }
                        if(analyze) {
                            val mmUtterance = MachineOutputParser.parse(currentUtterance)
                            if(mmUtterance ne null) {
                                if (future != null)
                                    future.apply()
                                future = writingActor !! Some(mmUtterance)
                            }
                        }

                        counter += 1
                        currentUtterance = ""

                        if(counter % 10000 == 0) {
                            LOG.info(counter+" utterances processed!")
                            writingActor !! "commit"
                        }
                    }
                }
                catch {
                    case e:Exception => {
                        LOG.error(e.printStackTrace())
                    }
                }
            })
            INDEX_WRITER.commit()
        }

        stream.close()
        writingActor ! None
        INDEX_WRITER.close()
    }

    val writingActor = new Actor {
        def act() {
            loop {
                receive {
                    case Some(utterance:MMUtterance)=> {
                        val docs = toDocuments(utterance)
                        INDEX_WRITER.addDocuments(docs)
                        sender ! true //give something back
                    }
                    case None => exit()
                    case "commit" => INDEX_WRITER.commit()
                }
            }
        }
    }

    def toDocuments(mmUtterance:MMUtterance):List[Document] = {
        var docs = List[Document]()

        val doc = new Document()
        doc.add(new StringField("pmid", mmUtterance.pmid , Field.Store.YES))
        doc.add(new StringField("section", mmUtterance.section , Field.Store.YES))
        doc.add(new StringField("number", mmUtterance.num.toString , Field.Store.YES))
        doc.add(new TextField("text", mmUtterance.text.replaceAll(" ( )+"," ").replaceAll("\"\"+","\"").replaceAll("''+","'").trim , Field.Store.YES))
        doc.add(new IntField("start", mmUtterance.startPos , Field.Store.YES))
        doc.add(new IntField("length", mmUtterance.length , Field.Store.YES))
        doc.add(new StringField("type", "utterance" , Field.Store.YES))
        docs ::= doc

        mmUtterance.phrases.foreach(phrase => {
            phrase.mappings.foreach(mapping => {
                val mapDoc = new Document

                mapDoc.add(new IntField("score", mapping.score , Field.Store.YES))
                mapping.cuis.foreach(cui =>  mapDoc.add(new StringField("cui", cui, Field.Store.YES)) )
                mapDoc.add(new StringField("type", "mapping" , Field.Store.YES))
                mapDoc.add(new IntField("phraseStart",phrase.start, Field.Store.YES))
                mapDoc.add(new IntField("phraseLength",phrase.length, Field.Store.YES))

                docs ::= mapDoc
            })

            phrase.candidates.foreach(candidate => {
                val candDoc = new Document

                candDoc.add(new IntField("score", candidate.score, Field.Store.YES))
                candDoc.add(new StringField("cui", candidate.cui , Field.Store.YES))
                candidate.semtypes.foreach(semType=>  candDoc.add(new StringField("semtype", semType, Field.Store.YES)) )
                candidate.positions.foreach(pos=>
                    candDoc.add(new StringField("pos",pos._1+":"+pos._2, Field.Store.YES)) )
                candDoc.add(new StringField("type", "candidate" , Field.Store.YES))
                candDoc.add(new IntField("phraseStart",phrase.start, Field.Store.YES))
                candDoc.add(new IntField("phraseLength",phrase.length, Field.Store.YES))

                docs ::= candDoc
            })
        })

        docs
    }

}
