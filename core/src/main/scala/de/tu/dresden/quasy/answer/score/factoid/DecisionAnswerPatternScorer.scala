package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, DecisionAnswerType}
import de.tu.dresden.quasy.model.db.LuceneIndex
import org.apache.lucene.util.Version
import org.apache.lucene.queryparser.classic.QueryParser

/**
 * @author dirk
 * Date: 6/7/13
 * Time: 1:52 PM
 */
class DecisionAnswerPatternScorer(luceneIndex:LuceneIndex) extends FactoidScorer {
    def scoreInternal(factoid: FactoidAnswer) = {
        factoid.question.answerType match {
            case aType:DecisionAnswerType => {
                //TODO make this better
                val questionContext =
                    factoid.question.getAnnotationsWithin[OntologyEntityMention].
                        filterNot(oem => aType.answerOptions.exists(opt => oem.coveredText.contains(opt.answerText) || opt.answerText.contains(oem.coveredText)))

                val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).
                    parse(questionContext.foldLeft("\"")(_+_.coveredText+"\" AND \"") + aType.criterion+" "+factoid.answerText+"\"")
                luceneIndex.searcher.search(q,Int.MaxValue).totalHits.toDouble
            }
            case _ => 0.0
        }
    }
}
