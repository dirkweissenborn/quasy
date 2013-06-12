package de.tu.dresden.quasy.run

import de.tu.dresden.quasy.enhancer.clearnlp.FullClearNlpPipeline
import java.util.Properties
import de.tu.dresden.quasy.dependency.DependencyTree
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.Sentence
import java.io.{FileInputStream, File}

/**
 * @author dirk
 * Date: 4/22/13
 * Time: 4:06 PM
 */
object PrintDependencyTree {

      def main(args:Array[String]) {
          val props = new Properties()
          props.load(new FileInputStream("conf/configuration.properties"))

          val pipeline = FullClearNlpPipeline.fromConfiguration(props)

          var sentence = ""
          while(sentence != "a") {
              println("Write your sentence:")
              sentence = readLine()
              val text = new AnnotatedText(sentence)
              pipeline.enhance(text)
              text.getAnnotations[Sentence].foreach(s => {
                  println(s.getTokens.map(t => t.coveredText+"_"+t.posTag).mkString(" "))
                  println(s.getDependencyTree.prettyPrint+"\n")
                  println(s.printRoleLabels)
                  println()
              })
          }

      }

}
