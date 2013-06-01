package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.model.annotation.OntologyConcept
import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 5/29/13
 * Time: 10:55 AM
 */
trait FactoidScorer {

    def score(factoid:FactoidAnswer):Double = {
        val score = scoreInternal(factoid)
        factoid.scores += (Manifest.classType(this.getClass) -> score)
        score
    }

    protected[factoid] def scoreInternal(factoid:FactoidAnswer):Double

}
