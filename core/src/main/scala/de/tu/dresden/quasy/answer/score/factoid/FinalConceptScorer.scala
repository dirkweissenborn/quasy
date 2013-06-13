package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.FactoidAnswer
import tycor.{UmlsTycor, SupportingEvidenceTycor, ParagraphTycor}
import de.tu.dresden.quasy.model.annotation.Sentence

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 2:59 PM
 */
object FinalConceptScorer extends FactoidScorer {
    def scoreInternal(factoid: FactoidAnswer) = {
        val prominence = factoid.score[ConceptProminenceScorer]
        val weightedProminence = factoid.score[WeightedContextScorer[Sentence]]
        val paragraphTypeScore = factoid.score[ParagraphTycor]
        val supportingEvidenceScore = factoid.score[SupportingEvidenceTycor]
        val idfScore = factoid.score[IdfScorer]
        val umlsTypeScore = factoid.score[UmlsTycor.type]
                                                                                                                       //smoothing
        weightedProminence * idfScore * (math.max(List(paragraphTypeScore,supportingEvidenceScore).max + umlsTypeScore,0.01))

        umlsTypeScore*            2.9192+
        paragraphTypeScore*       3.0907+
        idfScore *                2.5066+
        weightedProminence *      0.5348+
        prominence *              3.1232+
        supportingEvidenceScore * 1.3281-
                                  6.1447
    }
}
