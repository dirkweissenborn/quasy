package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.annotation.{Question, Token}
import de.tu.dresden.quasy.model.{DepRel, PosTag}
import scala.collection.mutable._

/**
 * @author dirk
 *         Date: 4/15/13
 *         Time: 1:35 PM
 */
object QuestionEnhancer {

    def inferAnswerClassByPattern(tokens: List[Token]) = {
        val map = Map[String, Double](
            Question.QC_ENUMERATION -> 0,
            Question.QC_EXPLANATION -> 0,
            Question.QC_FACTOID -> 0,
            Question.QC_YESNO -> 0,
            Question.QC_UNKNOWN -> .5)

        tokens.head.posTag match {
            case postag if (postag.startsWith("WP")) => {
                val depDepthSorted = tokens.sortBy(_.depDepth)
                depDepthSorted.find(_.posTag.matches(PosTag.ANYVERB_PATTERN)) match {
                    case Some(token) =>
                        if (token.posTag.equals(PosTag.Verb_3rd_person_singular_present))
                            map(Question.QC_FACTOID) += 1
                        else
                            map(Question.QC_ENUMERATION) += 1
                    case None => //do nothing
                }

                depDepthSorted.find(_.depTag.tag.equals(DepRel.Predicative_complement)) match {
                    case Some(token) =>
                        if (token.posTag.matches(PosTag.PLURALNOUN_PATTERN))
                            map(Question.QC_ENUMERATION) += 1
                        else
                            map(Question.QC_FACTOID) += 1
                }
            }

            case postag if (postag.equals(PosTag.Wh_adverb)) => map(Question.QC_EXPLANATION) += 1

            case postag if postag.matches("(" + PosTag.Modal + "|(" + PosTag.ANYVERB_PATTERN + ")).+") => map(Question.QC_YESNO) += 1
            case _ =>
        }

        map.maxBy(_._2)._1
    }

}
