package de.tu.dresden.quasy.run

import java.io.{FileWriter, PrintWriter, File}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.KeywordAnalyzer
import scala.collection.parallel.{ForkJoinTasks}
import org.apache.lucene.document.Document


/**
 * @author dirk
 *          Date: 7/30/13
 *          Time: 1:01 PM
 */
object ExtractConnections {

    def main(args:Array[String]) {
        ForkJoinTasks.defaultForkJoinPool.setParallelism(args(5).toInt)

        val util = new ExtractionUtil(new File(args(0)))

        val cui1 = args(1)
        val cui2 = args(2)

        val indirection = args(3)

        val outputDir = new File(args(4),cui1+"-"+indirection+"-"+cui2)
        outputDir.mkdirs()

        println("Querying indexes...")
        val (hits1,hits2) = query(cui1, cui2, indirection,util)

        println("Scoring candidates...")
        val total = util.utteranceSemTypeFrequency(indirection)
        val icfs = hits1.keys.map(cui => (cui,
            total.toDouble /
                math.max(1,util.utteranceCuiFrequency(cui)))
        ).toMap

                                               // cf*icf
        var scores = hits1.map(hit =>
               (hit._1,
                   math.log(math.min(hit._2.size,hits2(hit._1).size)+1) * math.log(icfs(hit._1)),
                   icfs(hit._1),
                   hit._2.size,
                   hits2(hit._1).size)).toSeq

        scores = scores.sortBy(-_._2)

        println("Writing results...")
        val scoreWriter = new PrintWriter(new FileWriter(new File(outputDir,"scores.tsv")))
        scoreWriter.println("CUI\tlog(cf+1)*log(icf)\ticf\tcf1\tcf2")
        scores.foreach(score => scoreWriter.println(score._1+"\t"+score._2+"\t"+score._3+"\t"+score._4+"\t"+score._5))
        scoreWriter.close()

        def writeToFile(cui1:String, cui: String, docs: Array[(Document, Map[(Int, Int), Document])]) {
            val pw = new PrintWriter(new FileWriter(new File(outputDir, cui1 + "-" + cui + ".tsv")))
            docs.foreach(doc => {
                val utterance = doc._1.get(ExtractionUtil.TEXT)
                pw.println(doc._1.get(ExtractionUtil.PMID) + "." +
                    doc._1.get(ExtractionUtil.SECTION) + "." +
                    doc._1.get(ExtractionUtil.NUMBER) +
                    "\t" + utterance)
            })
            pw.close()
        }

        hits1.foreach{
            case (cui,docs) => {
                writeToFile(cui1,cui, docs)
            }
        }
        hits2.foreach{
            case (cui,docs) => {
                writeToFile(cui2,cui, docs)
            }
        }
    }


    def query(cui1: String, cui2: String, indirection: String,  util:ExtractionUtil):
    // CUI -> Array(parent,bestAnnotations)
    (Map[String,Array[(Document,Map[(Int,Int),Document])]],
     Map[String,Array[(Document,Map[(Int,Int),Document])]]) = {

        val parser = new QueryParser(Version.LUCENE_40, "", new KeywordAnalyzer)
        val parentQuery1 = parser.parse("uttCui:"+cui1+" AND uttSemtype:" +indirection)
        val parentQuery2 = parser.parse("uttCui:"+cui2+" AND uttSemtype:" +indirection)

        var hits1 =
                util.query[(String,Document,Array[Document])](parentQuery1,
                    (topGroups,index) => {
                        topGroups.groups.flatMap(g => {
                            val parent = index.searcher.doc(g.groupValue)
                            var indDocs = List[Document]()
                            val candidates = g.scoreDocs.map(ind => {
                                val doc = index.searcher.doc(ind.doc)
                                if(doc.get(ExtractionUtil.SEMTYPE) == indirection && doc.get(ExtractionUtil.CUI) != cui1 && doc.get(ExtractionUtil.CUI) != cui2)
                                    indDocs ::= doc
                                doc
                            })
                            indDocs.toArray.map(indDoc => (indDoc.get(ExtractionUtil.CUI),parent,candidates))
                        })
                    }).groupBy(_._1)


        var hits2 = util.query[(String,Document,Array[Document])](parentQuery2,
            (topGroups,index) => {
                topGroups.groups.flatMap(g => {
                    val parent = index.searcher.doc(g.groupValue)
                    var indDocs = List[Document]()
                    val candidates = g.scoreDocs.map(ind => {
                        val doc = index.searcher.doc(ind.doc)
                        if(doc.get(ExtractionUtil.SEMTYPE) == indirection && hits1.contains(doc.get(ExtractionUtil.CUI)) )
                            indDocs ::= doc
                        doc
                    })
                    indDocs.toArray.map(indDoc => (indDoc.get(ExtractionUtil.CUI),parent,candidates))
                })
        }).groupBy(_._1)

        //just consider connecting cuis between cui1 and cui2
        hits1 = hits1.filter(cui => hits2.contains(cui._1))

        //take best annotations foreach hit and filter out hits where at least 1 of the required cuis does not occur
        var finalHits1 = hits1.mapValues(vs => vs.map {
            case (indCui,parent,annotations) => {
                val bestAnnotations = util.selectAnnotations(parent, annotations, cui1, indCui)

                if( bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == cui1) &&
                    !bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == cui2) &&
                    bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == indCui) )
                    Some((parent,bestAnnotations))
                else
                    None
            }
        }.flatten.toArray).filterNot(_._2.isEmpty)

        //just consider overlap again, because of new filtering before
        hits2 = hits2.filter(cui => finalHits1.contains(cui._1))
        val finalHits2 = hits2.mapValues(vs => vs.map {
            case (indCui,parent,annotations) => {
                val bestAnnotations = util.selectAnnotations(parent, annotations, cui2, indCui)

                if( bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == cui2) &&
                    !bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == cui1) &&
                    bestAnnotations.exists(_._2.get(ExtractionUtil.CUI) == indCui) )
                    Some((parent,bestAnnotations))
                else
                    None
            }
        }.flatten.toArray).filterNot(_._2.isEmpty)

        //just consider overlap again, because of new filtering before
        finalHits1 = finalHits1.filter(cui => finalHits2.contains(cui._1))

        (finalHits1,finalHits2)
    }

}
