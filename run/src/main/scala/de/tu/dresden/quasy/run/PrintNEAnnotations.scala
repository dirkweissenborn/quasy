package de.tu.dresden.quasy.run

import java.io.File
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Chunk, OntologyEntityMention, Sentence}

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 9:42 AM
 */
object PrintNEAnnotations {

    main(Array("/home/dirk/workspace/bioasq/annotations/question_corpus1000"))

    def main(args:Array[String]) {
        val corpusDir = new File(args(0))

        corpusDir.listFiles.take(10).foreach(file => {
            val text= Xmlizer.fromFile[AnnotatedText](file)
            println(text.id +"\t"+text.text)
            text.getAnnotations[OntologyEntityMention].foreach(ne => {
                print(ne.coveredText)
                print("["+ne.ontologyConcepts.map(c => {
                    c.source
                }).mkString(",")+"]\t")
            })

            println()
            println()
        })

    }

}
