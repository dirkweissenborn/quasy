package de.tu.dresden.quasy.run

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

/**
 * @author dirk
 *          Date: 8/7/13
 *          Time: 10:38 AM
 */
object ExtractCuiSemtypeConnection {

    def main(args:Array[String]) {
        ForkJoinTasks.defaultForkJoinPool.setParallelism(args(4).toInt)
        val util = new ExtractionUtil(new File(args(0)))
        val cui = args(1)
        val semtype = args(2)
        val outputDir = new File(args(3),cui+"-"+semtype)
        outputDir.mkdirs()
        outputDir.listFiles().foreach(_.delete())

        println("Querying...")
        val hits = query(cui,semtype,util)

        println("Writing results...")
        var maxFreq = 0
        val uttFreqs= hits.keys.map(toCui => {
            val frequency = util.utteranceCuiFrequency(toCui)
            maxFreq = math.max(maxFreq,frequency)
            toCui -> frequency
        }).toMap

        val analyzer = new StandardAnalyzer(Version.LUCENE_40)
        val config = new IndexWriterConfig(Version.LUCENE_40,analyzer)
        config.setRAMBufferSizeMB(256)

        val fSDirectory = FSDirectory.open(outputDir)
        val indexWriter = new IndexWriter(fSDirectory, config)

        val frequencyWriter = new PrintWriter(new FileWriter(new File(outputDir,"frequencies.tsv")))
        val semtypeFreq = util.utteranceSemTypeFrequency(semtype)
        frequencyWriter.println("total\t"+semtypeFreq)

        hits.foreach {
            case (toCui,utterances) => {
                val cuiFreq = uttFreqs(toCui)
                //exclude max frequency, because it will be the main concept of that class like "protein" or "gene"
                if(cuiFreq < maxFreq) {
                    frequencyWriter.println(toCui+"\t"+cuiFreq)
                    utterances.foreach {
                        case (parent,annotations) => {
                            val docs = createDocs(annotations, parent, cui, toCui)
                            indexWriter.addDocuments(docs)
                        }
                    }
                }
            }
        }

        frequencyWriter.close()
        indexWriter.close()
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

    def query(cui: String,semType: String,  util:ExtractionUtil):
        // CUI -> Array(parent,bestAnnotations)
        Map[String,Array[(Document,Map[(Int,Int),Document])]] = {

        val parser = new QueryParser(Version.LUCENE_40, "", new KeywordAnalyzer)
        val parentQuery = parser.parse("uttCui:"+cui+" AND uttSemtype:" +semType)

        val hits =
            util.query[(String,Document,Array[Document])](parentQuery,
                (topGroups,index) => {
                    topGroups.groups.flatMap(g => {
                        val parent = index.searcher.doc(g.groupValue)
                        var indDocs = List[Document]()
                        val candidates = g.scoreDocs.map(ind => {
                            val doc = index.searcher.doc(ind.doc)
                            if(doc.get(ExtractionUtil.SEMTYPE) == semType && doc.get(ExtractionUtil.CUI) != cui)
                                indDocs ::= doc
                            doc
                        })
                        indDocs.toArray.map(indDoc => (indDoc.get(ExtractionUtil.CUI),parent,candidates))
                    })
                }).groupBy(_._1)


        //take best annotations foreach hit and filter out hits where at least 1 of the required cuis does not occur
        hits.mapValues(vs => vs.map {
            case (toCui,parent,annotations) => {
                val bestAnnotations = util.selectAnnotations(parent, annotations, cui, toCui)

                if( bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == cui) &&
                    bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == toCui) )
                    Some((parent,bestAnnotations))
                else
                    None
            }
        }.flatten.toArray).filterNot(_._2.isEmpty)
    }
}
