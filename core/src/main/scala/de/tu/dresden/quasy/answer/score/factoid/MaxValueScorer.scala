package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 6/11/13
 * Time: 11:23 AM
 */
class MaxValueScorer[S <: FactoidScorer](factoids: Seq[FactoidAnswer])(implicit m:Manifest[S]) extends FactoidScorer {

    val maxValue = {
        val scores = factoids.map(_.score()(m))
        scores.max
    }

    def scoreInternal(factoid: FactoidAnswer) = {
       maxValue
    }

    protected override val man = manifest[AverageValueScorer[S]]

}

class AverageValueScorer[S <: FactoidScorer](factoids: Seq[FactoidAnswer])(implicit m:Manifest[S]) extends FactoidScorer {

    val averageValue = {
            val scores = factoids.map(_.score()(m))
            scores.sum / scores.size
    }

    def scoreInternal(factoid: FactoidAnswer) = {
       averageValue
    }

    protected override val man = manifest[AverageValueScorer[S]]

}
