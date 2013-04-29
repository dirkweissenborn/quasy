package de.tu.dresden.quasy.run

import java.io.File
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.model.annotation.{Token, Sentence}
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.dep.{DependencyNode, DependencyTree}
import de.tu.dresden.quasy.enhancer.NounalRelationEnhancer

/**
 * @author dirk
 * Date: 4/24/13
 * Time: 1:31 PM
 */
object PrintSemanticRoleLabels {

    main(Array("/home/dirk/workspace/bioasq/annotations/question_corpus1000"))

    def main(args:Array[String]) {
        val corpusDir = new File(args(0))

        corpusDir.listFiles.foreach(file => {
            val text= Xmlizer.fromFile[AnnotatedText](file)
            text.getAnnotations[Sentence].foreach(sentence => {
                val tree = sentence.getDependencyTree
                if(text.text.startsWith("What")){
                    NounalRelationEnhancer.enhance(text)

                    println(text.id + "\t" + sentence.coveredText)

                    println(sentence.getTokens.flatMap(token => {
                    token.srls.map(srl => {
                        var res = ""
                        try {
                            res = srl.label+"(" +
                            tree.graph.nodes.find( (node:DependencyNode) => node.nodeHead.position.equals(srl.head)).get.tokens.map(_.lemma).mkString(" ")+","+
                            tree.getSubtree(tree.graph.nodes.find( (node:DependencyNode) => node.nodeHead.equals(token)).get)
                                .nodes.flatMap( (node:DependencyNode) => node.tokens).toSeq.sortBy(_.position).map(_.lemma).mkString(" ")+")"
                        }
                        catch {
                            case e => e.printStackTrace()
                        }
                        res
                    })
                }).mkString("\t"))
                println()
                }
            })
        })

    }

}
