package de.tu.dresden.quasy.clearnlp

import java.io.File
import org.scalatest.FunSuite
import de.tu.dresden.quasy.enhancer.clearnlp.{ClearNlpDepAndSrlEnhancer, ClearNlpPosAndLemmaEnhancer, ClearNlpSegmentationEnhancer}
import de.tu.dresden.quasy.model.Text
import de.tu.dresden.quasy.model.annotation.{Token, Sentence, Section}


class TestClearNLP  extends FunSuite{

    /*@Test
    def testDEPParser {
        val parser = new DemoDEPParser("/home/dirk/workspace/bioasq/model/dictionary-1.3.1.zip",
            "/home/dirk/workspace/bioasq/model/medical-en-pos-1.1.0g.jar",
            "/home/dirk/workspace/bioasq/model/medical-en-dep-1.1.0b3.jar")

        val parse = parser.parse(question)

        parse.foreach( tree => System.out.println(tree.toStringDEP()+"\n"))
    } */

    test("bla") {
        val segmenter = new ClearNlpSegmentationEnhancer(new File("de/tu/dresden/bioasq/model/dictionary-1.3.1.zip"))
        val text = new Text("This is! a test? \n about segmentation.")

        segmenter.enhance(text)
        val sections = text.getAnnotations[Section]

        assert(sections.length === 2)
        assert(sections.head.coveredText === "This is! a test? ")

        val sentences = text.getAnnotations[Sentence]
        assert(sentences.size === 2)
        assert(sentences.last.coveredText === "a test?")
        assert(sentences.last.start === 9)
        assert(sentences.last.end === 16)

        val tokens = text.getAnnotations[Token]
        assert(tokens.size === 9)
        assert(tokens.head.coveredText === "This")
        assert(tokens.last.start === 37)
        assert(tokens.last.end === 38)

        val postagger = new ClearNlpPosAndLemmaEnhancer(new File("de/tu/dresden/bioasq/model/medical-en-pos-1.1.0g.jar"),new File("de/tu/dresden/bioasq/model/dictionary-1.3.1.zip"))
        postagger.enhance(text)


        val question = new Text("What is the role of thyroid hormones administration in the treatment of heart failure?\nbla bla bla!")
        segmenter.enhance(question)
        postagger.enhance(question)

        val depParser = new ClearNlpDepAndSrlEnhancer(new File("/home/dirk/workspace/bioasq/model/medical-en-dep-1.1.0b3.jar"))
        depParser.enhance(question)
    }


}
