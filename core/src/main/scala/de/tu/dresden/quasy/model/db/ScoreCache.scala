package de.tu.dresden.quasy.model.db

import java.io.{FileWriter, PrintWriter, File}
import io.Source
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.FactoidAnswer
import collection.mutable.Map

/**
 * @author dirk
 * Date: 6/12/13
 * Time: 2:06 PM
 */
object ScoreCache {
    //question -> Map(answer string -> Map(manifest string -> score))
    val cache = Map[String,Map[String,Map[String,Double]]]()

    private var cacheFile:File = null

    def loadCache(file:File) {
        cacheFile = file
        file.getParentFile.mkdirs()
        if (!file.createNewFile()) {
            Source.fromFile(file).getLines().foreach(line => {
                if(!line.isEmpty) {
                    val Array(question, answer, scoresStr) = line.split(";", 3)
                    val qmap = cache.getOrElseUpdate(question, Map[String, Map[String, Double]]())
                    val map = qmap.getOrElseUpdate(answer, Map[String, Double]())

                    scoresStr.split(";").map(scoreStr => {
                        val Array(scorer, score) = scoreStr.split("#", 2)
                        map += (scorer -> score.toDouble)
                    })
                }
            })
        }
    }

    def clean = cache.clear()

    def addScore[T <: FactoidScorer](s:Double, fa:FactoidAnswer)(implicit m:Manifest[T]) {
        val qmap = cache.getOrElseUpdate(fa.question.coveredText,Map[String,Map[String,Double]]())
        val map = qmap.getOrElseUpdate(fa.answerText,Map[String,Double]())

        map += (m.toString -> s)
    }

    def score[T <: FactoidScorer](fa:FactoidAnswer)(implicit m:Manifest[T]) = {
        val map = cache.getOrElse(fa.question.coveredText,null)
        var s = Double.MinValue
        if(map != null) {
            val aMap = map.getOrElse(fa.answerText,null)
            if(aMap ne null)
                s = aMap.getOrElse(m.toString,Double.MinValue)
        }

        if(s == Double.MinValue)
           None
        else
           Some(s)
    }

    def storeCache {
        cacheFile.getParentFile.mkdirs()
        val pw = new PrintWriter(new FileWriter(cacheFile))
        cache.foreach(q => {
            q._2.foreach(answer => {
                pw.println(q._1+";"+answer._1+";"+answer._2.map(e => e._1+"#"+e._2).mkString(";"))
            })
        })
        pw.close()
    }

}
