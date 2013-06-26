package de.tu.dresden.quasy.answer.score.factoid.tycor


import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.{AnswerContext, FactoidAnswer}
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.enhancer.EnhancementPipeline
import de.tu.dresden.quasy.io.AnnotatedTextSource
import de.tu.dresden.quasy.model.annotation.{SimpleAnswerTypeLike, DecisionAnswerType, Section}
import java.text.Normalizer
import de.tu.dresden.quasy.model.db.{AnnotationCache, LuceneIndex}

/**
 * @author dirk
 * Date: 5/21/13
 * Time: 1:29 PM
 */
case class SupportingEvidenceTycor(luceneIndex:LuceneIndex, nrDocs:Int, atLeastDependencyTreeAnnotator:EnhancementPipeline) extends TycorScorer {

    def scoreInternal(factoid: FactoidAnswer) = {
        val aType = factoid.question.answerType.asInstanceOf[SimpleAnswerTypeLike]

        if (!aType.coveredTokens.isEmpty) {
            val aTypeTxt = aType.coveredTokens.map(_.coveredText).mkString(" ")
            val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).
            parse( factoid.answer.synonyms.map(syn => "\" "+syn+" "+aTypeTxt+" \"~2").mkString(" OR "))

            val scoredDocs = luceneIndex.searcher.search(q,null,nrDocs*2) //duplicates :(

            var asciiTexts = List[String]()

            scoredDocs.scoreDocs.foldLeft(Set[Int]())( (acc,scoreDoc) => {
                val doc = luceneIndex.reader.document(scoreDoc.doc)
                val pmid: Int = doc.get("pmid").toInt
                if(!acc.contains(pmid) && acc.size < nrDocs){
                    val t = doc.get("title") + "\n" + doc.get("contents")
                    asciiTexts ::= Normalizer.normalize(t, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                }
                acc + pmid
            })

            val texts = asciiTexts.map(asciiText => AnnotationCache.getCachedAnnotatedText(asciiText))
            atLeastDependencyTreeAnnotator.process(AnnotatedTextSource(texts:_*))
            val tycor = new ParagraphTycor(texts.flatMap(text => text.getAnnotations[Section].map(section => AnswerContext(section,factoid.question))).toList)

            tycor.scoreInternal(factoid)
        }
        else 0.0
    }
}
