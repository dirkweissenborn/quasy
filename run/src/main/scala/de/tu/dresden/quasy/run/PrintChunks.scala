package de.tu.dresden.quasy.run

import java.util.Properties
import java.io.FileInputStream
import de.tu.dresden.quasy.enhancer.clearnlp.FullClearNlpPipeline
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Chunk, Sentence}
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer

/**
 * @author dirk
 * Date: 6/6/13
 * Time: 4:12 PM
 */
object PrintChunks {

    var sentences = List("Is Rheumatoid Arthritis the result of men or women?","Is Rheumatoid Arthritis more common in men or women?")

    def main(args:Array[String]) {
        val props = new Properties()
        props.load(new FileInputStream("conf/configuration.properties"))

        val chunker = OpenNlpChunkEnhancer.fromConfiguration(props)
        val pipeline = FullClearNlpPipeline.fromConfiguration(props)
        val texts = sentences.map(sentence => new AnnotatedText(sentence))

        texts.foreach(text => {pipeline.enhance(text);chunker.enhance(text)} )

        texts.foreach(text => {
            print(text.getAnnotations[Chunk].map(_.toString).mkString("\t"))
            println
        })

    }

}
