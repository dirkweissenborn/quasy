package de.tu.dresden.quasy.run

import java.io.{FileWriter, PrintWriter, File}
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Sentence, Token}
import de.tu.dresden.quasy.dep.DependencyTree

/**
 * @author dirk
 * Date: 4/24/13
 * Time: 3:17 PM
 */
object ExtractNounalRelationsFromWhatBeQuestions {
    main(Array("/home/dirk/workspace/bioasq/annotations/questions_what_be","corpus/nounal_relations_headwords.txt"))

    def main(args:Array[String]) {
        val corpusDir = new File(args(0))
        val output = new File(args(1))
        output.getParentFile.mkdirs()

        val pw = new PrintWriter(new FileWriter(output))

        corpusDir.listFiles.foreach(file => {
            val text= Xmlizer.fromFile[AnnotatedText](file)
            text.getAnnotations[Sentence].foreach(sentence => {
                /*val tree = new DependencyTree(sentence)
                tree.root.children.find(_.nodeHead.depTag.tag.equals("nsubj")) match {
                    case Some(depNode) => {
                        var tokens = depNode.tokens
                        var chs = depNode.children
                        if (chs.exists(_.nodeHead.depTag.tag.equals("prep"))) {
                          //  tokens ++= chs.flatMap(_.tokens).toList
                            pw.println(tokens.sortBy(_.position).map(_.coveredText).mkString(" "))
                        }
                    }
                    case None =>
                }  */
            })
        })

        pw.close()

    }


}
