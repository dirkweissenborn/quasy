package de.tu.dresden.quasy.enhancer.transinsight

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{Token, OntologyEntityMention, OntologyConcept}
import de.tu.dresden.quasy.webservices.transinsight.TransinsightService
import collection.mutable._

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 2:13 PM
 */
class TransinsightAnnotator extends TextEnhancer{
    private val service = new TransinsightService

    def enhance(text: AnnotatedText) {
        val tokens = text.getAnnotations[Token].sortBy(_.begin)
        val newTokenBegins = Map[Int,Token]()
        val newTokenEnds = Map[Int,Token]()
        val newText = tokens.foldLeft("")((accText,token) => {
            newTokenBegins += ((accText.length + 1) -> token)
            newTokenEnds += ((accText.length+token.coveredText.length + 1) -> token)
            accText +" "+ token.coveredText
        })

        val result =  service.getEntityConcepts(newText)

        if (result != null) {
            val ontologyConcepts = Map[List[Span],List[OntologyConcept]]()

            val uriLabelMap = result.terms.map(term => (term.uri,term.label)).toMap

            result.annotations.foreach(annotation => {
                val source = annotation.conceptUri.getPath.replace("http://namespaces.transinsight.com/","").replaceAll("#.+","").substring(1).toUpperCase

                val concept = new OntologyConcept(source, uriLabelMap(annotation.conceptUri), 1.0, annotation.conceptUri)
                val spans = annotation.ranges.sortBy(_.begin).foldLeft(List[Span]())((acc,span) => {
                    val newSpan = new Span(newTokenBegins(span.begin).begin,newTokenEnds(span.end).end)
                    if (acc.isEmpty)
                        acc ++ List(newSpan)
                    else {
                        val last = acc.last
                        if (last.end +1 >= newSpan.begin){
                            acc.dropRight(1) ++ List(new Span(last.begin,math.max(newSpan.end,last.end)))
                        }
                        else if(newSpan !=acc.last)
                            acc ++ List(newSpan)
                        else
                            acc
                    }
                }).toList
                var concepts = ontologyConcepts.getOrElse(spans,List[OntologyConcept]())
                if (!concepts.exists(_.uri == concept.uri))
                    concepts ::= concept
                ontologyConcepts += (spans -> concepts)
            })

            ontologyConcepts.foreach {
                case(spans, concepts) => {
                    new OntologyEntityMention(spans.toArray,text,concepts.reverse)
                }
            }
        }
    }

}
