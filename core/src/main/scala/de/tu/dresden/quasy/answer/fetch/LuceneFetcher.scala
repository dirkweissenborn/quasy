package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, Question}
import org.apache.lucene.util.Version
import org.apache.lucene.queryParser.QueryParser
import de.tu.dresden.quasy.enhancer.EnhancementPipeline
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.util.LuceneIndex
import org.apache.commons.logging.LogFactory
import org.apache.lucene.analysis.KeywordAnalyzer

/**
 * @author dirk
 * Date: 5/22/13
 * Time: 10:45 AM
 */
class LuceneFetcher(luceneIndex:LuceneIndex) extends CandidateFetcher {

    private val LOG = LogFactory.getLog(this.getClass)

    def fetch(question: Question, docCount: Int, pipeline:EnhancementPipeline = null) = {
        val query: String =
            question.context.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).
                map(oem => "\""+oem.coveredText+"\"").mkString(" ")
        val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(query)
        LOG.info("Query becomes: "+q)
        val docs = luceneIndex.searcher.search(q,null,docCount*2)

        val texts = docs.scoreDocs.map(scoreDoc => {
            val doc = luceneIndex.searcher.doc(scoreDoc.doc)
            new AnnotatedText(doc.get("pmid"),doc.get("title") +"\n"+doc.get("contents"))
        }).groupBy(_.id).map(_._2.head).take(docCount).toArray

        extractAnswerCandidates(texts,question,pipeline)
    }

    def fetchByPmid(question: Question, pmids:List[Int], pipeline:EnhancementPipeline = null) = {
        val query: String = pmids.map(_.toString).mkString(" ")
        val q = new QueryParser(Version.LUCENE_36, "pmid", new KeywordAnalyzer).parse(query)
        LOG.info("Query becomes: "+q)
        val docs = luceneIndex.searcher.search(q,null,Int.MaxValue)

        val texts = docs.scoreDocs.map(scoreDoc => {
            val doc = luceneIndex.searcher.doc(scoreDoc.doc)
            new AnnotatedText(doc.get("pmid"),doc.get("title") +"\n"+doc.get("contents"))
        }).groupBy(_.id).map(_._2.head).toArray

        extractAnswerCandidates(texts,question,pipeline)
    }
}
