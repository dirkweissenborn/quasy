package de.tu.dresden.quasy.answer.postprocess

import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.model.db.ScoreCache

/**
 * @author dirk
 * Date: 6/12/13
 * Time: 2:58 PM
 */
object CacheUpdater extends AnswerPostProcessor {
    def processFactoid(factoidAnswer: FactoidAnswer) {
        factoidAnswer.getScores.foreach(score => ScoreCache.addScore(score._2,factoidAnswer)(score._1))
    }
}
