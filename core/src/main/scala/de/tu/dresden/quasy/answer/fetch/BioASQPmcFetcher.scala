package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.webservices.bioasq.BioASQService
import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline}
import de.tu.dresden.quasy.io.AnnotatedTextIteratorSource
import de.tu.dresden.quasy.model.annotation.{Section, Question, Sentence}

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:01 PM
 */
class BioASQPmcFetcher(pipeline:EnhancementPipeline) extends CandidateFetcher{
    private val service = new BioASQService

    def fetch(question: Question, docCount:Int) = {
        val documents = service.getPmcDocuments(question.coveredText,docCount)

        //TODO just single sentences are candidates
        val texts = documents.documents.flatMap(document => {
            var result=List[AnnotatedText]()
            if (document.documentAbstract!=null)
                result ::= new AnnotatedText(document.documentAbstract.replace("\n"," "))

            if (document.sections!=null)
                result ++= document.sections.map(section => new AnnotatedText(section.replace("\n"," "))).toList

            result
        })

        pipeline.process(new AnnotatedTextIteratorSource(texts.iterator))

        texts.flatMap(_.getAnnotations[Section].map(section => {
            new AnswerCandidate(section,question)
        })).toList
    }
}
