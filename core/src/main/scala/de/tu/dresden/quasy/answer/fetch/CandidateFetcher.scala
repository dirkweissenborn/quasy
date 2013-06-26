package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.model.annotation.{Section, Question}
import de.tu.dresden.quasy.enhancer.EnhancementPipeline
import de.tu.dresden.quasy.io.AnnotatedTextSource

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 3:59 PM
 */
trait CandidateFetcher {

    def fetch(question:Question, docCount:Int, pipeline:EnhancementPipeline) : List[AnswerContext]
    def fetchByPmid(question: Question, pmids:List[Int], pipeline:EnhancementPipeline = null) : List[AnswerContext]

    protected def extractAnswerCandidates(answerTexts: Array[AnnotatedText], question: Question, pipeline: EnhancementPipeline): List[AnswerContext] = {
        if (pipeline != null)
            pipeline.process(AnnotatedTextSource(answerTexts: _*))
        else {
            answerTexts.foreach(text => {
                if(text.getAnnotations[Section].isEmpty) {
                    var sectionOffset = 0
                    text.text.split("[\n\r]").foreach(sectionString => {
                        if (sectionString != "") {
                            sectionOffset = text.text.indexOf(sectionString, sectionOffset)
                            new Section(sectionOffset, sectionOffset + sectionString.length, text)
                        }
                        sectionOffset += sectionString.length
                    })
                }
            })
        }

        answerTexts.flatMap(_.getAnnotations[Section].map(section => {
            new AnswerContext(section, question)
        })).toList
    }

}
