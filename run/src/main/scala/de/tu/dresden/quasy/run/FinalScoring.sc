import cc.mallet.types.StringKernel
import de.tu.dresden.quasy.io.LoadGoldStandards
import de.tu.dresden.quasy.io.LoadGoldStandards.QuestionAnswer
import de.tu.dresden.quasy.model.db.ScoreCache
import io.Source
import java.io.File

/**
 * @author dirk
 *          Date: 6/12/13
 *          Time: 12:37 PM
 */
/*val impScores =Set("de.tu.dresden.quasy.answer.score.factoid.tycor.UmlsTycor$"
    ,"de.tu.dresden.quasy.answer.score.factoid.tycor.ParagraphTycor"
    ,"de.tu.dresden.quasy.answer.score.factoid.IdfScorer"
    ,"de.tu.dresden.quasy.answer.score.factoid.WeightedContextScorer[de.tu.dresden.quasy.model.annotation.Sentence]"
    ,"de.tu.dresden.quasy.answer.score.factoid.ConceptProminenceScorer"
    ,"de.tu.dresden.quasy.answer.score.factoid.tycor.SupportingEvidenceTycor")   */

val goldAnswers = LoadGoldStandards.load(new File("corpus/questions.0.json"))
ScoreCache.loadCache(new File("cache/questions.0.json.scores"))

val answers = ScoreCache.cache.map(q =>
    (q._1,
     q._2.map(answerScores => {
        val answer = answerScores._1
        val scores = answerScores._2

        val score =
           scores("de.tu.dresden.quasy.answer.score.factoid.tycor.UmlsTycor$") *                                                          2.9192+
           scores("de.tu.dresden.quasy.answer.score.factoid.tycor.ParagraphTycor") *                                                      3.0907+
           scores("de.tu.dresden.quasy.answer.score.factoid.IdfScorer") *                                                                 2.5066+
           scores("de.tu.dresden.quasy.answer.score.factoid.WeightedContextScorer[de.tu.dresden.quasy.model.annotation.Sentence]") *      0.5348+
           scores("de.tu.dresden.quasy.answer.score.factoid.ConceptProminenceScorer") *                                                   3.1232+
           scores("de.tu.dresden.quasy.answer.score.factoid.tycor.SupportingEvidenceTycor") *                                             1.3281-
               6.1447

        (answer,score)
}))).toMap

var totalMeanRank = 0.0
var totalRecall = 0.0
var totalRecallNoZeros3 = 0.0
var totalRecallNoZeros = 0.0
val sk = new StringKernel()


val filteredQAs: Array[QuestionAnswer] = goldAnswers.filter(_.`type`.matches("factoid|list"))
var nrZeros = 0.0

filteredQAs.drop(1).foreach(qa => {
    var meanRank = 0.0

    var tp = 0.0;var tpNoZero3 = 0.0;var tpNoZero = 0.0
    var fn = 0.0;var fnNoZero3 = 0.0;var fnNoZero = 0.0
    var fp = 0.0;var fpNoZero3 = 0.0;var fpNoZero = 0.0

    println(qa.body)
    println("Gold answer(s): ["+qa.answer.exact.mkString(",")+"]")
    val sortedAnswers = answers(qa.body).toSeq.sortBy(-_._2).zipWithIndex
    sortedAnswers.take(10).foreach(a => println(a._1._2+" -> "+a._1._1))
    qa.answer.exact.foreach(goldAnswer => {
        sortedAnswers.find(a => sk.K(a._1._1,goldAnswer)>0.6) match {
            case Some(((_,score),rank)) => {
                if(rank <  qa.answer.exact.size) {
                    tp += 1.0
                    tpNoZero += 1.0
                } else {
                    fn += 1.0
                    fnNoZero += 1.0
                }
                if (rank < 3*qa.answer.exact.size) tpNoZero3 += 1.0 else fnNoZero3 += 1.0
                meanRank += rank
            }
            case None => {fn += 1;nrZeros+=1.0/qa.answer.exact.size}
        }
    })

    meanRank /= qa.answer.exact.size
    totalMeanRank += meanRank
    totalRecall += tp/(tp+fn)
    if (tpNoZero > 0) {
        totalRecallNoZeros3 += tpNoZero3/(tpNoZero3+fnNoZero3)
        totalRecallNoZeros += tpNoZero/(tpNoZero+fnNoZero)
    }
})

println("#######################")
println("Mean rank: "+ totalMeanRank/(filteredQAs.size-nrZeros))
println("Total accuracy: "+ totalRecall/filteredQAs.size)
println("Number of unanswerable questions: "+nrZeros)
println("Total accuracy no zeros: "+ totalRecallNoZeros/(filteredQAs.size-nrZeros))
println("Total accuracy no zeros (within first 3): "+ totalRecallNoZeros3/(filteredQAs.size-nrZeros))
println("#######################")