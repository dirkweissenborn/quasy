package de.tu.dresden.quasy.run

import java.io.{FileInputStream, File}
import java.util.Properties
import de.tu.dresden.quasy.io.{AnnotatedTextSource, PlainTextSource}
import de.tu.dresden.quasy.enhancer.clearnlp.{FullClearNlpPipeline}
import de.tu.dresden.quasy.enhancer.opennlp.OpenNlpChunkEnhancer
import de.tu.dresden.quasy.enhancer.{TextEnhancer, EnhancementPipeline}
import de.tu.dresden.quasy.enhancer.bioasq._
import de.tu.dresden.quasy.enhancer.stanford.FullStanfordNlpEnhancer
import de.tu.dresden.quasy.model.AnnotatedText

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 11:45 AM
 */
object RunFullPipeline {

    main(Array("corpus/question_corpus1000.txt","annotations/question_corpus1000","conf/configuration.properties"))

    def main(args:Array[String]) {
        if (args.size < 3) {
            throw new IllegalArgumentException("Input-file (1st argument), output-directory (2nd) and the configuration-file (3rd) must be specified!")
        }
        val inputFile = new File(args(0))
        val outputDir = new File(args(1))
        val config = new File(args(2))

        val props = new Properties()
        props.load(new FileInputStream(config))

       /* val texts = new AnnotatedTextSource {
            val it = List(new AnnotatedText("What is the role of thyroid hormones administration in the treatmet of heart failure?"),
                new AnnotatedText("What is the role of thyroid hormones administration in the treatmet of heart failure?"),
                new AnnotatedText("What is the role of thyroid hormones administration in the treatmet of heart failure?"),
                new AnnotatedText("What is the role of thyroid hormones administration in the treatmet of heart failure?")).iterator
            def next() = it.next()

            def hasNext = it.hasNext

            def reset {}
        }  */

        run(PlainTextSource.fromFile(inputFile),outputDir,props)
    }


    def run(texts:AnnotatedTextSource, outputDir:File, configuration:Properties, stanford:Boolean = false) {
        getFullPipeline(configuration, stanford).process(texts,outputDir)
    }

    def getFullPipeline(configuration: Properties, stanford: Boolean = false) = {
        var lexicalAnnotation: TextEnhancer = null
        if (stanford)
            lexicalAnnotation = new FullStanfordNlpEnhancer
        else
            lexicalAnnotation = FullClearNlpPipeline.fromConfiguration(configuration)

        val chunker = OpenNlpChunkEnhancer.fromConfiguration(configuration)

        new EnhancementPipeline(List(lexicalAnnotation, chunker, new MeshEnhancer)) //, new UniprotEnhancer, new DoidEnhancer, new GoEnhancer))//, new JochemEnhancer))
    }
}
