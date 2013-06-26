/**
 * @author: dirk
 *          Date: 6/25/13
 *          Time: 6:02 PM
 */

import cc.mallet.types.StringKernel
import de.tu.dresden.quasy.io.LoadGoldStandards
import de.tu.dresden.quasy.io.LoadGoldStandards.QuestionAnswer
import de.tu.dresden.quasy.model.db.ScoreCache
import io.Source
import java.io.File

/**
 * @author dirk
 *         Date: 6/12/13
 *         Time: 12:37 PM
 */
/*val impScores =Set("de.tu.dresden.quasy.answer.score.factoid.tycor.UmlsTycor$"
    ,"de.tu.dresden.quasy.answer.score.factoid.tycor.ParagraphTycor"
    ,"de.tu.dresden.quasy.answer.score.factoid.IdfScorer"
    ,"de.tu.dresden.quasy.answer.score.factoid.WeightedContextScorer[de.tu.dresden.quasy.model.annotation.Sentence]"
    ,"de.tu.dresden.quasy.answer.score.factoid.ConceptProminenceScorer"
    ,"de.tu.dresden.quasy.answer.score.factoid.tycor.SupportingEvidenceTycor")   */
object Appppp extends App {
    val goldAnswers = LoadGoldStandards.load(new File("corpus/questions.0.json"))
    ScoreCache.loadCache(new File("cache/questions.0.json.scores"))

    val answers = ScoreCache.cache.map(q =>
        (q._1,
            q._2.map(answerScores => {
                val answer = answerScores._1
                val scores = answerScores._2

                val score =
                    scores("de.tu.dresden.quasy.answer.score.factoid.tycor.UmlsTycor$") * 2.9192 +
                        scores("de.tu.dresden.quasy.answer.score.factoid.tycor.ParagraphTycor") * 3.0907 +
                        scores("de.tu.dresden.quasy.answer.score.factoid.IdfScorer") * 2.5066 +
                        scores("de.tu.dresden.quasy.answer.score.factoid.WeightedContextScorer[de.tu.dresden.quasy.model.annotation.Sentence]") * 0.5348 +
                        scores("de.tu.dresden.quasy.answer.score.factoid.ConceptProminenceScorer") * 3.1232 +
                        scores("de.tu.dresden.quasy.answer.score.factoid.tycor.SupportingEvidenceTycor") * 1.3281 -
                        6.1447

                (answer, score)
            }))).toMap


    /*println("""{"system": "BioASQ_Baseline_1bB", "username": "gbt", "password": "gr1979",
                  "questions": [{ "id": "5118dd1305c10fae75000001",
                  "exact_answer": [["women"]]}""")*/

    /*answers.foreach(as => {
        val goldAnswer: QuestionAnswer = goldAnswers.find(_.body.trim == as._1.trim).get
        val id = goldAnswer.id
        val sorted: List[(String, Double)] = as._2.toList.sortBy(-_._2)
        println(goldAnswer.body)
        sorted.take(10).foreach(a => println(a._1+"->"+a._2))
        println()

        /* val aString = if(goldAnswer.`type` == "factoid")
             sorted.take(5).map("[\"" + _._1 + "\"]").mkString(",")
         else
             sorted.takeWhile(_._2 >= -2.5).map("[\""+_._1+"\"]").mkString(",")
     println(""",{ "id": "%s",
                            "exact_answer": [%s]}""".format(id,aString))     */
    }) */
    //println("]}")


    var totalMeanRank = 0.0
    var totalRecall = 0.0
    var totalRecallNoZeros3 = 0.0
    var totalRecallNoZeros = 0.0
    val sk = new StringKernel()
    val filteredQAs: Array[QuestionAnswer] = goldAnswers.filter(_.`type`.matches("factoid|list"))
    var nrZeros = 0.0
    //threshold -> (total precision, total recall)
    var thresholds = (-4.0 until 0.0 by 0.1).map(threshold => threshold ->(0.0, 0.0)).toMap
    var ranks = (1 until 10).map(_ -> (0.0,0.0)).toMap

    var ranksAndThresholds = {
        val ts = (-4.0 until 0.0 by 0.1)
        val rs = (1 until 10)
        (for (t <- ts; r <- rs) yield (r,t)).map(_ -> (0.0,0.0)).toMap
    }
    val nrList = goldAnswers.count(_.`type` == "list")
    var nrListNoZeros = nrList.toDouble
    filteredQAs.drop(1).foreach(qa => {
        val nrAnswers = qa.exact_answer.size
        var meanRank = 0.0
        var tp = 0.0
        var tpNoZero3 = 0.0
        var tpNoZero = 0.0
        var fn = 0.0
        var fnNoZero3 = 0.0
        var fnNoZero = 0.0

        println(qa.body)
        //println("Gold answer(s): [" + qa.exact_answer.mkString(",") + "]")
        val sortedAnswers = answers(qa.body).toSeq.sortBy(-_._2).zipWithIndex
        sortedAnswers.take(10).foreach(a => println(a._1._2 + " -> " + a._1._1))

        qa.exact_answer.foreach(goldAnswer => {
            sortedAnswers.find(a => goldAnswer.exists(ga => sk.K(a._1._1, ga) > 0.6)) match {
                case Some(((_, score), rank)) => {
                    if (rank < nrAnswers) {
                        tp += 1.0
                        tpNoZero += 1.0
                    } else {
                        fn += 1.0
                        fnNoZero += 1.0
                    }
                    if (rank < 3 * nrAnswers) tpNoZero3 += 1.0 else fnNoZero3 += 1.0
                    meanRank += rank

                }
                case None => {
                    fn += 1; nrZeros += 1.0 / nrAnswers
                }
            }
        })

        if (qa.`type` == "list") {
            nrListNoZeros -= (fn - fnNoZero) / nrAnswers

            thresholds.keys.foreach(threshold => {
                val answerList = sortedAnswers.takeWhile(_._1._2 >= threshold)
                val fpPlusTp = answerList.size

                val tp = answerList.count(a => qa.exact_answer.flatten.exists(goldAnswer => sk.K(a._1._1, goldAnswer) > 0.6))
                val fn = nrAnswers - tp

                if (tp > 0)
                    thresholds += (threshold ->
                        (thresholds(threshold)._1 + tp.toDouble / fpPlusTp,
                            thresholds(threshold)._2 + tp.toDouble / (tp + fn)))
            })

            ranksAndThresholds.keys.foreach(rkTh => {
                val answerList = sortedAnswers.take(rkTh._1) ++ sortedAnswers.drop(rkTh._1).takeWhile(_._1._2 >= rkTh._2)
                val fpPlusTp = answerList.size

                val tp = answerList.count(a => qa.exact_answer.flatten.exists(goldAnswer => sk.K(a._1._1, goldAnswer) > 0.6))
                val fn = nrAnswers - tp

                if (tp > 0)
                    ranksAndThresholds += (rkTh ->
                        (ranksAndThresholds(rkTh)._1 + tp.toDouble / fpPlusTp,
                            ranksAndThresholds(rkTh)._2 + tp.toDouble / (tp + fn)))
            })
        }


        meanRank /= nrAnswers
        totalMeanRank += meanRank
        totalRecall += tp / (tp + fn)
        if (tpNoZero > 0) {
            totalRecallNoZeros3 += tpNoZero3 / (tpNoZero3 + fnNoZero3)
            totalRecallNoZeros += tpNoZero / (tpNoZero + fnNoZero)
        }
    })

    println("#######################")
    println("Mean rank: " + totalMeanRank / (filteredQAs.size - nrZeros))
    println("Total accuracy: " + totalRecall / filteredQAs.size)
    println("Number of unanswerable questions: " + nrZeros)
    println("Total accuracy no zeros: " + totalRecallNoZeros / (filteredQAs.size - nrZeros))
    println("Total accuracy no zeros (within first 3): " + totalRecallNoZeros3 / (filteredQAs.size - nrZeros))
    println("#######################")
    println()


    val f1Thresholds = thresholds.map {
        case (threshold, (precision, recall)) => {
            (threshold, 2 * precision * recall / (precision + recall) / nrList, 2 * precision * recall / (precision + recall) / nrListNoZeros)
        }
    }.toList
    val f1Ranks = ranksAndThresholds.map {
        case (rank, (precision, recall)) => {
            (rank, 2 * precision * recall / (precision + recall) / nrList, 2 * precision * recall / (precision + recall) / nrListNoZeros)
        }
    }.toList
    println("#######List answer thresholds with zeros########")
    f1Thresholds.sortBy(-_._2).take(5).foreach(t => println(t._1 + " F1: " + t._2))
    println("#######List answer ranks and thresholds with zeros########")
    f1Ranks.sortBy(-_._2).take(5).foreach(t => println(t._1 + " F1: " + t._2))

    println("#######List answer thresholds without zeros########")
    f1Thresholds.sortBy(-_._3).take(5).foreach(t => println(t._1 + " F1: " + t._3))
    println("#######List answer ranks and thresholds without zeros########")
    f1Ranks.sortBy(-_._3).take(5).foreach(t => println(t._1 + " F1: " + t._3))

}
