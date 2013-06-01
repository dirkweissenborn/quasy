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
        var json =  "[" + Source.fromFile(fromFile).mkString("") + "]"
        json = json.replaceAll("""\n\{ id: ""","\n,{ id: ")
        json = json.replaceAll("""exact: '(.+)',""","exact: ['$1'],")

        val gson = new Gson()
        gson.fromJson(json, classOf[Array[QuestionAnswer]])
    }


    case class QuestionAnswer(val id:String, val `type`:String, val body:String, val answer:Answer )

    case class Answer(val ideal:String, val exact:Array[String], val annotations:Array[Annot])

    case class Annot(val `type`:String,val title:String, val uri:String, val text:String, val beginIndex:Int, val endIndex:Int, val fieldName:String)

}
