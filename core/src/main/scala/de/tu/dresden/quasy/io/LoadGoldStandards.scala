package de.tu.dresden.quasy.io

import java.io.File
import io.Source
import com.google.gson.Gson

/**
 * @author dirk
 * Date: 5/14/13
 * Time: 2:31 PM
 */
object LoadGoldStandards {

    def load(fromFile:File) = {
        var json =  Source.fromFile(fromFile).mkString("")
        json = json.replaceAll(""""exact_answer":\s*("[^"]+")""","\"exact_answer\": [[$1]]").replaceAll(""""exact_answer":\s*(\[[^\]\[]+\])""","\"exact_answer\": [$1]")
        val gson = new Gson()
        val qs = gson.fromJson(json, classOf[Questions])
        qs.questions
    }

    def loadAnswers(fromFile:File) = {
        var json =  Source.fromFile(fromFile).mkString("").trim()
        json = json.replaceAll(""""exact_answer":\s*("[^"]+")""","\"exact_answer\": [[$1]]").replaceAll(""""exact_answer":\s*(\[[^\]\[]+\])""","\"exact_answer\": [$1]")
        if(json.isEmpty)
            Array[IdAnswer]()
        else {
            if(json.endsWith(","))
                json = json.substring(0,json.length-1) + "]}"
            if(!json.endsWith("]}"))
                json += "]}"
            val gson = new Gson()
            val qs = gson.fromJson(json, classOf[Result])
            qs.questions
        }
    }

    case class Result(system:String,username:String,password:String,questions:Array[IdAnswer])

    case class IdAnswer(id:String,exact_answer:Array[Array[String]])

    case class Questions(questions: Array[QuestionAnswer])

    case class QuestionAnswer(id:String, `type`:String, body:String ,documents:Array[String], exact_answer:Array[Array[String]] )

    //case class Answer(ideal:String, exact_answer:Array[String], annotations:Array[Annot])

    //case class Annot(`type`:String,title:String, uri:String, text:String, beginIndex:Int, endIndex:Int, fieldName:String)

}
