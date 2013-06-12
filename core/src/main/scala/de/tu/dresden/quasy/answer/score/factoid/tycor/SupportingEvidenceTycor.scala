package de.tu.dresden.quasy.answer.score.factoid.tycor


import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.{AnswerContext, FactoidAnswer}
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.enhancer.EnhancementPipeline
import de.tu.dresden.quasy.io.AnnotatedTextSource
import de.tu.dresden.quasy.model.annotation.{DecisionAnswerType, Section}
import java.text.Normalizer
import de.tu.dresden.quasy.model.db.LuceneIndex

/**
 * @author dirk
 * Date: 5/21/13
 * Time: 1:29 PM
 */
case class SupportingEvidenceTycor(luceneIndex:LuceneIndex, nrDocs:Int, atLeastDependencyTreeAnnotator:EnhancementPipeline) extends TycorScorer {

    def scoreInternal(factoid: FactoidAnswer) = {
        val aType = factoid.question.answerType

        if (!aType.coveredText.isEmpty) {
            val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).
            parse( factoid.answer.synonyms.map(syn => "\" "+syn+" "+aType.coveredText+" \"~2").mkString(" OR "))

            val scoredDocs = luceneIndex.searcher.search(q,null,nrDocs*2) //duplicates :(

            var asciiText = ""

            scoredDocs.scoreDocs.foldLeft(Set[Int]())( (acc,scoreDoc) => {
                val doc = luceneIndex.reader.document(scoreDoc.doc)
                val pmid: Int = doc.get("pmid").toInt
                if(!acc.contains(pmid) && acc.size < nrDocs){
                    val t = doc.get("title") + "\n" + doc.get("contents")
                    asciiText += Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                }
                acc + pmid
            })

            val text = new AnnotatedText("SupportingEvidence["+q.toString()+"]",asciiText)
            atLeastDependencyTreeAnnotator.process(AnnotatedTextSource(text))
            val tycor = new ParagraphTycor(text.getAnnotations[Section].map(section => AnswerContext(section,factoid.question)).toList)

            tycor.scoreInternal(factoid)
        }
        else 0.0
    }
}
