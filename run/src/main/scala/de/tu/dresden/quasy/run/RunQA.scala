package de.tu.dresden.quasy.run

import java.io.{FileInputStream, File}
import java.util.Properties
import de.tu.dresden.quasy.io.PlainTextSource

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:52 PM
 */
object RunQA {

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

        RunFullPipeline.run(PlainTextSource.fromFile(inputFile),outputDir,props)
    }


}
