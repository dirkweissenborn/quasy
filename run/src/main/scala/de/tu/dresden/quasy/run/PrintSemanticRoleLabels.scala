package de.tu.dresden.quasy.run

import java.io.File
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.model.annotation.Sentence
import de.tu.dresden.quasy.model.AnnotatedText

/**
 * @author dirk
 * Date: 4/24/13
 * Time: 1:31 PM
 */
object PrintSemanticRoleLabels {

    main(Array("/home/dirk/workspace/bioasq/annotations/question_corpus1000"))

    def main(args:Array[String]) {
        val corpusDir = new File(args(0))

        corpusDir.listFiles.take(200).foreach(file => {
            val text= Xmlizer.fromFile[AnnotatedText](file)
            text.getAnnotations[Sentence].foreach(sentence => {
                //NounalRelationEnhancer.enhance(text)

                println(text.id + "\t" + sentence.coveredText)

                println(sentence.printRoleLabels)
                println(sentence.getDependencyTree.prettyPrint)
                println()
            })
        })

    }
}
