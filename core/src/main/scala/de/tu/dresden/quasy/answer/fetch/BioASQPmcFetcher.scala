package de.tu.dresden.quasy.answer.fetch

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.webservices.bioasq.BioASQService
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.enhancer.{EnhancementPipeline}
import de.tu.dresden.quasy.model.annotation._
import de.tu.dresden.quasy.webservices.bioasq.model.DocumentsResult
import de.tu.dresden.quasy.io.AnnotatedTextSource
import de.tu.dresden.quasy.QuasyFactory

/**
 * @author dirk
 * Date: 4/25/13
 * Time: 4:01 PM
 */
class BioASQFetcher(serviceCall:(String,Int)=>DocumentsResult) extends CandidateFetcher{

    def fetch(question: Question, docCount:Int, pipeline:EnhancementPipeline = null) = {
        val query: String =
            question.context.getAnnotationsBetween[OntologyEntityMention](question.begin,question.end).
            map(oem => {
                val meshConcepts = oem.ontologyConcepts.filter(_.source.equals(OntologyConcept.SOURCE_MESH))
                if(!meshConcepts.isEmpty)
                    "("+meshConcepts.map("\""+_.preferredLabel+"\"[MESH]").mkString(" OR ") +")"
                else
                "\""+oem.coveredText+"\""
            }).mkString(" ")
        val documents = serviceCall(query,docCount)

        //TODO just single sentences are candidates
        val texts = documents.documents.flatMap(document => {
            var result=List[AnnotatedText]()
            if (document.documentAbstract!=null)
                result ::= new AnnotatedText(document.documentAbstract.replace("\n"," "))

            if (document.sections!=null)
                result ++= document.sections.map(section => new AnnotatedText(section.replace("\n"," ")))

            result
        })

        extractAnswerCandidates(texts, question, pipeline)
    }
}

class BioASQPmcFetcher extends BioASQFetcher({ val service = new BioASQService; service.getPmcDocuments })
class BioASQPubMedFetcher extends BioASQFetcher({ val service = new BioASQService; service.getPubmedDocuments })
