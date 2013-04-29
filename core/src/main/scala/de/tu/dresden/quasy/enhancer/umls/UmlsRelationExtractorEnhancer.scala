package de.tu.dresden.quasy.enhancer.umls

import gov.nih.nlm.nls.skr.GenericObject
import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{SemanticRelationAnnotation, NamedEntityMention, UmlsConcept}
import de.tu.dresden.quasy.util.Xmlizer
import java.io._
import org.apache.http.entity.mime.content.FileBody
import java.nio.charset.Charset

/**
 * @author dirk
 * Date: 4/15/13
 * Time: 10:41 AM
 */
object UmlsRelationExtractorEnhancer extends TextEnhancer {
    /*
    SE|00000000||tx|1|text|Is it true that aspirin is a good treatment for headache?
SE|00000000||tx|1|entity|C0205238|True|qlco|||true||||1000|7|10
SE|00000000||tx|1|entity|C0004057|Aspirin|orch,phsu|||aspirin||||1000|17|23
SE|00000000||tx|1|entity|C0205170|Good|qlco|||good||||888|30|33
SE|00000000||tx|1|entity|C0039798|therapeutic aspects|ftcn|||treatment||||888|35|43
SE|00000000||tx|1|entity|C0018681|Headache|sosy|||headache||||1000|49|56
SE|00000000||tx|1|relation|3|2|C0004057|Aspirin|orch,phsu|phsu|||aspirin||||1000|17|23|PREP|TREATS||45|47|1|1|C0018681|Headache|sosy|sosy|||headache||||1000|49|56
     */

    def enhance(text: AnnotatedText) {
        val resultStr = requestSemRep(text.text+"\n")
        var NEs = List[NamedEntityMention]()
        resultStr.split("""\n""").foreach( line => {
            val split = line.split("""\|""")

            if (split.size > 5 && split(5).equals("entity")) {
                val Array(_,_,_,_,_,"entity",cui,_,tuis,_,_,_,_,_,_,begin,end) = split
                val concept = new UmlsConcept(cui, tuis.split(","))

                NEs ::= new NamedEntityMention(Array(new Span(begin.toInt,end.toInt + 1)), text, List(concept))
            } else if (split.size > 5 && split(5).equals("relation"))  {
                val beginSubj = split(19).toInt
                val endSubj = split(20).toInt +1

                val beginObj = split(39).toInt
                val endObj = split(40).toInt +1

                val predicate = split(22)

                new SemanticRelationAnnotation(
                    predicate,
                    NEs.find(_.spans.head.equals(new Span(beginSubj,endSubj))).get,
                    NEs.find(_.spans.head.equals(new Span(beginObj,endObj))).get)
            }
        } )
    }

    def requestSemRep(text:String):String = {

        //myGenericObj.setField("Batch_Command", "semrep -D")
        //myGenericObj.setField("SilentEmail", true)
        //myGenericObj.setFileField("UpLoad_File", filename)

        val username = "dweissenborn"//System.getProperty("umlsUsername")
        val password = "horst84!"//System.getProperty("umlsPassword")
        val myGenericObj = new GenericObject(200,username, password)

        myGenericObj.setField("Email_Address", "dirk.weissenborn@gmail.com")
        myGenericObj.setField("APIText", text)
        myGenericObj.setField("COMMAND_ARGS", "-D")

        var results = ""
        // Submit the job request
        try {
            results = myGenericObj.handleSubmission
            println(results)
        }
        catch {
            case ex: RuntimeException => {
                ex.printStackTrace
            }
        }
        results
    }

    def main(args:Array[String]) {

        var text = new AnnotatedText("Is it true that aspirin is a good treatment for headache?")

        enhance(text)

        val xml = Xmlizer.toXml(text)

        text = Xmlizer.fromXml(xml)

        println(xml)
    }
}


