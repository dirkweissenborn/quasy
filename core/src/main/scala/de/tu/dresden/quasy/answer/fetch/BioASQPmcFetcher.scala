package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.webservices.bioasq.BioASQServiceCall
import de.tu.dresden.quasy.answer.model.AnswerCandidate
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline}
import de.tu.dresden.quasy.io.AnnotatedTextIteratorSource
import de.tu.dresden.quasy.model.annotation.Sentence

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:01 PM
 */
class BioASQPmcFetcher(pipeline:EnhancementPipeline) extends CandidateFetcher{
    private val service = new BioASQServiceCall

    def fetch(question: AnnotatedText, docCount:Int) = {
        val documents = service.getPmcDocuments(question.text,docCount)

        //TODO just single sentences are candidates
        val texts = documents.documents.flatMap(document => {
            new AnnotatedText(document.documentAbstract) :: document.sections.map(section => new AnnotatedText(section)).toList
        })

        pipeline.process(new AnnotatedTextIteratorSource(texts.iterator))

        texts.flatMap(_.getAnnotations[Sentence].map(sentence => {
            val candText = sentence.toAnnotatedText
            new AnswerCandidate(candText,question)
        })).toList
    }
}
