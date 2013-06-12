package de.tu.dresden.quasy.run

import java.io.{InputStream, FileInputStream, File}
import io.Source
import java.util.zip.GZIPInputStream
import xml.pull._
import xml.pull.EvElemStart
import org.apache.lucene.document.{Field, Document}
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.FSDirectory
import org.apache.commons.logging.LogFactory
import org.apache.lucene.util.Version
import xml.{Elem, XML, MetaData}
import actors.Actor
import org.apache.lucene.analysis.en.EnglishAnalyzer

/**
 * @author dirk
 * Date: 5/6/13
 * Time: 3:31 PM
 */
object IndexPubmed {

    private val LOG = LogFactory.getLog(getClass)

    var INDEX_DIR:File = null
    var INDEX_WRITER:IndexWriter = null

    def main(args:Array[String]) {
        val pubmedDump = new File(args(0))
        INDEX_DIR = new File(args(1))
        val _override = false

        if(!INDEX_DIR.mkdirs() && _override){
            INDEX_DIR.listFiles().foreach(_.delete())
        }

        val config = new IndexWriterConfig(Version.LUCENE_36,new EnglishAnalyzer(Version.LUCENE_36))
        config.setRAMBufferSizeMB(64)
        INDEX_WRITER = new IndexWriter(FSDirectory.open(INDEX_DIR), config)

        var future:writingActor.Future[Any] = null

        writingActor.start()

        pubmedDump.listFiles().foreach(dump=> {
            LOG.info("writing dump "+dump.getName+" to index!")

            val input:InputStream = if(dump.getName.endsWith(".gz")) new GZIPInputStream(new FileInputStream(dump)) else new FileInputStream(dump)
            val xml = new XMLEventReader(Source.fromInputStream(input))

            var currentEntry = ""
            xml.next();xml.next()
            while(xml.hasNext) {
                val next = xml.next()
                next match {
                    case EvElemEnd(_,"MedlineCitation")  => {
                        currentEntry += "</MedlineCitation>"
                        if (currentEntry.contains("<Abstract>")) {
                            try {
                                val xml = XML.loadString(currentEntry)

                                if ((xml \\ "Language").exists(_.text.equals("eng"))) {
                                    val doc = toDocument(xml)
                                    if (future != null)
                                        future.apply()
                                    future = writingActor !!  Some(doc)
                                }
                            } catch {
                                case e => LOG.error(currentEntry);e.printStackTrace()
                            }
                        }
                        currentEntry = ""
                    }
                    case EvElemStart(_,elem,attrs,_) if elem != "\n"  =>
                        currentEntry += "<"+elem+attrsToString(attrs)+">"
                    case EvElemEnd(_,elem) =>
                        currentEntry += "</"+elem+">\n"
                    case EvText(text) =>  currentEntry += text
                    case EvEntityRef(entity) => {
                        entity
                    }
                }
            }
            INDEX_WRITER.commit()
        })

        writingActor ! None
        INDEX_WRITER.close()
    }

    private def attrsToString(attrs:MetaData) = {
        attrs.length match {
            case 0 => ""
            case _ => attrs.map( (m:MetaData) => " " + m.key + "='" + m.value.text.replaceAll("'","´").replaceAll("&","and") +"'" ).reduceLeft(_+_)
        }
    }

    val writingActor = new Actor {
        def act() {
            loop {
                receive {
                    case Some(doc:Document)=> {
                        INDEX_WRITER.addDocument(doc)
                        sender ! true //give something back
                    }
                    case None => exit()
                }
            }
        }
    }

    def toDocument(xml:Elem):Document = {
        val doc = new Document()

        val created = xml \ "DateCreated"
        val article = xml \ "Article"
        val journal = article \ "Journal"
        val chemicals = xml \\ "Chemical"
        val meshList = xml \\ "MeshHeading"

        doc.add(new Field("year", (created \ "Year").text, Field.Store.YES, Field.Index.NOT_ANALYZED))
        doc.add(new Field("journal", (journal \ "Title").text, Field.Store.YES, Field.Index.NOT_ANALYZED))
        doc.add(new Field("pmid", (xml \ "PMID").text , Field.Store.YES, Field.Index.NOT_ANALYZED))

        doc.add(new Field("title", (article \ "ArticleTitle").text, Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS))
        doc.add(new Field("contents", (article \\ "AbstractText").map(node => {
            val label = (node \ "@Label")
            if(!label.isEmpty)
                label.text +": "+node.text
            else
                node.text
        }).mkString("\n"),Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS))

        if (meshList != null)
            meshList.foreach(heading => {
                val majorTopic = (heading \\ "@MajorTopicYN").head
                var prefix = "m"
                if(majorTopic.text.equals("N"))
                    prefix = "nonM"
                doc.add(new Field(prefix+"ajorTopic", (heading \ "DescriptorName").text, Field.Store.YES, Field.Index.NOT_ANALYZED))
            })

        if (meshList != null)
            chemicals.foreach(chemical => {
                doc.add(new Field("chemical", (chemical \ "NameOfSubstance").text, Field.Store.YES, Field.Index.NOT_ANALYZED))
            })

        doc
    }

}
