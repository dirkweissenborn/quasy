package de.tu.dresden.quasy.run

import de.tu.dresden.quasy.enhancer.clearnlp.FullClearNlpPipeline
import java.util.Properties
import de.tu.dresden.quasy.dependency.DependencyTree
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, Question, Chunk, Sentence}
import java.io.{FileInputStream, File}
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import de.tu.dresden.quasy.enhancer.QuestionEnhancer
import de.tu.dresden.quasy.model.db.LuceneIndex
import de.tu.dresden.quasy.enhancer.umls.UmlsEnhancer

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
          val chunker = OpenNlpChunkEnhancer.fromConfiguration(props)
          val qE =  new QuestionEnhancer(LuceneIndex.fromConfiguration(props))

          var sentence = ""
          while(sentence != "a") {
              println("Write your sentence:")
              sentence = readLine()
              val text = new AnnotatedText(sentence)
              pipeline.enhance(text)
              chunker.enhance(text)
              //UmlsEnhancer.enhance(text)
              qE.enhance(text)


              text.getAnnotations[Sentence].foreach(s => {
                  println(s.getTokens.map(t => t.coveredText+"_"+t.posTag).mkString(" "))
                  println(s.dependencyTree.prettyPrint+"\n")
                  println(s.printRoleLabels)
                  //println(s.getAnnotationsWithin[OntologyEntityMention].map(_.toString).mkString("\t"))
                  println(s.getAnnotationsWithin[Chunk].map(_.toString).mkString("\t"))
                  if (s.isInstanceOf[Question])
                      println(s.asInstanceOf[Question].answerType)
                  println()
              })
          }

      }

}
