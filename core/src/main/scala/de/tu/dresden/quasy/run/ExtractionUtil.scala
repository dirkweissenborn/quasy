package de.tu.dresden.quasy.run

import java.io.File
import de.tu.dresden.quasy.model.db.LuceneIndex
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.util.Version
import org.apache.lucene.search.grouping.TopGroups
import org.apache.lucene.search.join.{ToParentBlockJoinCollector, ScoreMode, ToParentBlockJoinQuery}
import org.apache.lucene.search._
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.document.Document
import org.apache.lucene.index.Term
import scala.Some
import scala.collection.JavaConversions._

/**
 * @author dirk
 *          Date: 8/7/13
 *          Time: 10:40 AM
 */
class ExtractionUtil(val indexDir:File) {

    println("Opening indexes...")
    private val indexes = indexDir.listFiles().toSeq.par.map(i => new LuceneIndex(i,new StandardAnalyzer(Version.LUCENE_40)))

    private val parentFilter = new CachingWrapperFilter(
        new QueryWrapperFilter(
            new TermQuery(new Term(ExtractionUtil.TYP, "utterance"))))

    private val joinQueryCand = new ToParentBlockJoinQuery(
        new TermQuery(new Term(ExtractionUtil.TYP, "candidate")),
        parentFilter,
        ScoreMode.Total)

    def query[R](utteranceQuery:Query,postProcess: (TopGroups[java.lang.Integer],LuceneIndex) => Array[R])(implicit m:Manifest[R]):Array[R] = {
        val query = new BooleanQuery()
        query.add(utteranceQuery, Occur.MUST)
        query.add(joinQueryCand, Occur.MUST)

        indexes.flatMap(index => {
            val c = new ToParentBlockJoinCollector(
                Sort.INDEXORDER, // sort
                1000, // numHits
                true, // trackScores
                false // trackMaxScore
            )

            index.searcher.search(query, c)

            val topGroups = c.getTopGroups(
                joinQueryCand,
                Sort.INDEXORDER,
                0, // offset
                100, // maxDocsPerGroup
                0, // withinGroupOffset
                false // fillSortFields
            )
            if (topGroups != null) {
                Some(postProcess(topGroups,index))
            }
            else
                None
        }).seq.flatten.toArray
    }

    //Start,End -> best annotation
    def selectAnnotations(parent:Document, annotations:Array[Document],fromCui:String,toCui:String): Map[(Int,Int),Document] = {
        val offset = parent.get(ExtractionUtil.START).toInt

        var groupedAnnotations = annotations.groupBy(doc => {
            val Array(start,length) = doc.get(ExtractionUtil.POS).split(":",2).map(_.toInt)
            (start,length)
        })

        //filter out overlapping annotations
        val filteredKeys = groupedAnnotations.keys.toList.sortBy(_._1).foldLeft(List[(Int,Int)]())((keys,key) => {
            if(keys.isEmpty)
                List(key)
            else {
                val last = keys.head
                if(last._1+last._2 < key._1)
                    key :: keys
                // if overlapping, take the one with the biggest length
                else if(key._2 <= last._2)
                    keys
                else
                    key :: keys.tail
            }
        })

        val preferredCuis = List(fromCui,toCui)

        groupedAnnotations = groupedAnnotations.filter(ann => filteredKeys.contains(ann._1))

        // select annotations with highest (negative scores so actually lowest) scores with preference for preferred cuis
        groupedAnnotations.map {
            case ((start,length),annots) => {
                ( (start-offset,start-offset+length),
                    annots.reduceLeft((maxDoc,doc) => {
                        val max = maxDoc.get(ExtractionUtil.SCORE).toInt
                        val current = doc.get(ExtractionUtil.SCORE).toInt
                        if(current < max)
                            doc
                        else if(current > max)
                            maxDoc
                        else if(preferredCuis.contains(doc.get(ExtractionUtil.CUI)))
                            doc
                        else
                            maxDoc
                    }) )
            }
        }
    }

    def utteranceSemTypeFrequency(semtype:String) = {
        indexes.map(index => index.reader.docFreq(new Term("uttSemtype",semtype))).sum
    }

    def utteranceCuiFrequency(cui:String) = {
        indexes.map(index => index.reader.docFreq(new Term("uttCui",cui))).sum
    }

}

object ExtractionUtil {

    final val START = "start"
    final val END = "end"
    final val POS = "pos"
    final val TYP = "type"
    final val CUI = "cui"
    final val PMID = "pmid"
    final val TEXT = "text"
    final val SEMTYPE = "semtype"
    final val SCORE = "score"
    final val SECTION = "section"
    final val NUMBER = "number"
    final val FROMCUI = "fromCui"
    final val TOCUI = "toCui"
}
