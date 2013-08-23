package de.tu.dresden.quasy.extract

import scala.collection.parallel.ForkJoinTasks
import java.io.{FileWriter, PrintWriter, File}
import org.apache.lucene.document._
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.index.{IndexWriterConfig, IndexWriter}
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.analysis.standard.StandardAnalyzer
import scala.Some
import scala.collection.JavaConversions._
import scala.actors.Actor
import de.tu.dresden.quasy.util.MMUtterance
import de.tu.dresden.quasy.index.IndexMetaMappedBaselineFromTar

/**
 * @author dirk
 *          Date: 8/7/13
 *          Time: 10:38 AM
 */
object ExtractCuiSemtypeConnection {

    private var INDEX_WRITER:IndexWriter = null

    def main(args:Array[String]) {
        ForkJoinTasks.defaultForkJoinPool.setParallelism(args(4).toInt)
        val cui = args(1)
        val semTypes = args(2).split(",")
        val outputDir = new File(args(3),cui+"-"+args(2))
        outputDir.mkdirs()

        if(outputDir.listFiles().exists(_.getName == ExtractionUtil.FREQUENCIES_FILENAME)) {
            println("Skip! Was already processed before.")
            System.exit(0)
        }

        outputDir.listFiles().foreach(_.delete())

        val util = new ExtractionUtil(new File(args(0)))

        println("Querying and Writing...")
        val parser = new QueryParser(Version.LUCENE_40, "", new KeywordAnalyzer)
        val parentQuery = parser.parse("uttCui:"+cui+" AND ("+semTypes.map(s => "uttSemtype:" +s).mkString(" OR ")+")")

        val analyzer = new StandardAnalyzer(Version.LUCENE_40)
        val config = new IndexWriterConfig(Version.LUCENE_40,analyzer)
        config.setRAMBufferSizeMB(350)
        config.setMaxBufferedDocs(10000000)

        val fSDirectory = FSDirectory.open(outputDir)
        INDEX_WRITER = new IndexWriter(fSDirectory, config)
        var hits = Set[String]()

        util.query[String](parentQuery,
            (topGroups,index) => {
                println("Processing "+index.luceneIndex.getName)
                topGroups.groups.flatMap(g => {
                    val parent = index.searcher.doc(g.groupValue)
                    var indDocs = List[Document]()
                    val candidates = g.scoreDocs.map(ind => {
                        val doc = index.searcher.doc(ind.doc)
                        if(semTypes.contains(doc.get(ExtractionUtil.SEMTYPE)) && doc.get(ExtractionUtil.CUI) != cui)
                            indDocs ::= doc
                        doc
                    })
                    val bestAnnotations = ExtractionUtil.selectAnnotations(parent, candidates)
                    indDocs.foreach(indDoc => {
                        val toCui = indDoc.get(ExtractionUtil.CUI)
                        //take best annotations foreach hit and filter out hits where at least 1 of the required cuis does not occur
                        val selected = bestAnnotations.map { case (pos,annots) =>
                            pos ->
                                (annots.find(doc => doc.get(ExtractionUtil.CUI) == toCui || doc.get(ExtractionUtil.CUI) == cui) match {
                                    //One of the preferred cuis (from and to cui)
                                    case Some(doc:Document) => {
                                        // post process stupid gngm annotations, because they are really bad
                                        var result = doc
                                        val coveredText = parent.get(ExtractionUtil.TEXT).replace("\"\"","\"").substring(pos._1,pos._2)

                                        if(coveredText.length < 2)
                                            result = null

                                        doc.get(ExtractionUtil.SEMTYPE) match {
                                            case "gngm" => {
                                                //Do rather not take gngm as annotation, if it could be of another type as well (e.g., disease)
                                                if(annots.exists(_.get(ExtractionUtil.SEMTYPE) != "gngm") ) {
                                                    //println("Excluded as ambiguous gene: "+coveredText)
                                                    result = null
                                                //some really common words are annotated as genes, other normal words are proteins.
                                                //Genes usually don't look like normal english words
                                                } else if(coveredText.matches("[A-Z]?[a-z]+") || coveredText.toLowerCase == "ii") {
                                                    //println("Excluded as gene: "+coveredText)
                                                    result = null
                                                }
                                                /*else {
                                                    println("Included as gene: "+coveredText)
                                                } */
                                            }
                                           /* case "aapp" => {
                                                if(coveredText)
                                                println("Included as protein: "+coveredText)
                                            }*/
                                            case _ => //do nothing
                                        }

                                        result
                                    }
                                    case None => annots.find(_.get(ExtractionUtil.SEMTYPE) != "gngm").getOrElse(annots.head)
                                })
                        }.filter(_._2 ne null)

                        if( selected.exists(_._2.get(ExtractionUtil.CUI) == cui) &&
                            selected.exists(_._2.get(ExtractionUtil.CUI) == toCui) ) {
                            hits += toCui
                            val docs = createDocs(selected, parent, cui, toCui)
                            INDEX_WRITER.addDocuments(docs)
                        }
                    })
                    Array[String]()
                })
            })

        val connectedCuiWriter = new PrintWriter(new FileWriter(new File(outputDir,ExtractionUtil.FREQUENCIES_FILENAME)))
        hits.foreach (connectedCuiWriter.println)
        connectedCuiWriter.close()
        INDEX_WRITER.close()
    }


    private def createDocs(annotations: Map[(Int, Int), Document], parent: Document, fromCui:String, toCui:String): List[Document] = {
        var docs = List[Document]()
        val doc = new Document()

        doc.add(new StringField(ExtractionUtil.PMID, parent.get(ExtractionUtil.PMID), Field.Store.YES))
        doc.add(new StringField(ExtractionUtil.SECTION, parent.get(ExtractionUtil.SECTION), Field.Store.YES))
        doc.add(new StringField(ExtractionUtil.NUMBER, parent.get(ExtractionUtil.NUMBER), Field.Store.YES))
        doc.add(new TextField(ExtractionUtil.TEXT, parent.get(ExtractionUtil.TEXT), Field.Store.YES))
        doc.add(new StringField(ExtractionUtil.TYP, "utterance", Field.Store.YES))
        doc.add(new StringField(ExtractionUtil.FROMCUI,fromCui, Field.Store.YES))
        doc.add(new StringField(ExtractionUtil.TOCUI,toCui, Field.Store.YES))
        docs ::= doc

        annotations.foreach {
            case ((start, end), candidate) => {
                val candDoc = new Document

                candDoc.add(new StringField(ExtractionUtil.CUI, candidate.get(ExtractionUtil.CUI), Field.Store.YES))
                candDoc.add(new IntField(ExtractionUtil.START, start, Field.Store.YES))
                candDoc.add(new IntField(ExtractionUtil.END, end, Field.Store.YES))
                candidate.getValues(ExtractionUtil.SEMTYPE).foreach(semType => candDoc.add(new StringField(ExtractionUtil.SEMTYPE, semType, Field.Store.YES)))
                candDoc.add(new StringField("type", "candidate", Field.Store.YES))

                docs ::= candDoc
            }
        }
        docs
    }
}
