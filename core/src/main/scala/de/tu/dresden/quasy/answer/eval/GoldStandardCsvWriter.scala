package de.tu.dresden.quasy.answer.eval

import de.tu.dresden.quasy.answer.model.FactoidAnswer
import de.tu.dresden.quasy.model.annotation.Question
import java.io.{FileWriter, PrintWriter, File}
import cc.mallet.types.StringKernel

/**
 * @author dirk
 * Date: 6/10/13
 * Time: 1:57 PM
 */
class GoldStandardCsvWriter(goldStandards:Map[Question,Set[String]], out:File) extends EvalWriter{
    private val pw = new PrintWriter(new FileWriter(out))
    private val sk = new StringKernel()
    private var scoreTypes = List[(FactoidAnswer) => Double]()

    def writeFactoidEval(factoidAnswer: FactoidAnswer) {
        if (scoreTypes.isEmpty) {
            writeFirstLine(factoidAnswer)
        }

        pw.println("#"+factoidAnswer.question.coveredText+" -> "+factoidAnswer.answerText)

        pw.println(scoreTypes.map(_(factoidAnswer).toString).mkString(","))
    }


    def writeFirstLine(factoidAnswer: FactoidAnswer) {
        //first: simple scores
        val scorerManifests = factoidAnswer.getScores.keys.toList
        scoreTypes = scorerManifests.map(m => {
            pw.print(m.toString + ",")
            (fa: FactoidAnswer) => fa.getScores(m)
        }).toList
        //second: product scores
        scoreTypes ++= scorerManifests.flatMap(m1 => scorerManifests.map(m2 => {
            if (m1.equals(m2))
                None
            else {
                pw.print(m1.toString + "*" + m2.toString + ",")
                Some((fa: FactoidAnswer) => fa.getScores(m1) * fa.getScores(m2))
            }
        })).flatten.toList

        scoreTypes ++= List((fa: FactoidAnswer) => {
            if (goldStandards(fa.question).map(st => sk.K(fa.answerText.toLowerCase, st.toLowerCase)).max > 0.6)
                1.0
            else
                0.0
        })
        pw.println("correct")
    }

    def close = pw.close()
}
