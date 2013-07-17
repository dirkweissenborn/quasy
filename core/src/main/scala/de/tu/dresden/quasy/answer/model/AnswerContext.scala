package de.tu.dresden.quasy.answer.model

import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{OntologyConcept, Question, Annotation}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:44 PM
 */
case class AnswerContext(answerContext:Annotation, question:Question) {
    var scores = Map[Manifest[_],Double]()
}


