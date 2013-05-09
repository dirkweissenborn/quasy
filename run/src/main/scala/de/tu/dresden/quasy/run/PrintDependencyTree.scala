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

      var sentences = List("What activities do patients with patellar instability perceive makes their patella unstable?")

      def main(args:Array[String]) {
          val props = new Properties()
          props.load(new FileInputStream("conf/configuration.properties"))

          val pipeline = FullClearNlpPipeline.fromConfiguration(props)
          val texts = sentences.map(sentence => new AnnotatedText(sentence))

          texts.foreach(text => pipeline.enhance(text) )

          texts.foreach(text => {
              val depTree = text.getAnnotations[Sentence].head.getDependencyTree
              println(depTree.graph.toString())
              println
          })

      }

}
