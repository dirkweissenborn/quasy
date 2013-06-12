package de.tu.dresden.quasy.answer.score.factoid.tycor

import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.model.annotation.DecisionAnswerType

/**
 * @author dirk
 * Date: 6/6/13
 * Time: 4:06 PM
 */
trait TycorScorer extends FactoidScorer{
    override protected def scorable(factoidAnswer: FactoidAnswer) =
        factoidAnswer.question.answerType != null &&
        !factoidAnswer.question.answerType.isInstanceOf[DecisionAnswerType]
}
