package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.answer.tycor.{UmlsTycor, LuceneTycor}
import tycor.ParagraphTycor

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 2:59 PM
 */
object FinalConceptScorer extends FactoidScorer {
    protected[factoid] def scoreInternal(factoid: FactoidAnswer) = {
        val prominence = factoid.scores.getOrElse(Manifest.classType(classOf[ConceptProminenceScorer]),0.0)
        val luceneTypeScore = factoid.scores.getOrElse(Manifest.classType(classOf[LuceneTycor]),0.0)
        val paragraphTypeScore = factoid.scores.getOrElse(Manifest.classType(classOf[ParagraphTycor]),0.0)
        val umlsTypeScore = factoid.scores.getOrElse(Manifest.classType(UmlsTycor.getClass),0.0)

        prominence * List(luceneTypeScore,paragraphTypeScore,umlsTypeScore).max
    }
}
