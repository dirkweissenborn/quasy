package de.tu.dresden.quasy.enhancer.bioasq

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{OntologyConcept, NamedEntityMention}
import collection.mutable._

/**
 * @author dirk
 * Date: 4/10/13
 * Time: 10:42 AM
 */
abstract class BioASQNEEnhancer(serviceCall: String => FindEntityResult, source:String) extends TextEnhancer {

    def enhance(text: AnnotatedText) {
        val result = serviceCall(text.text)
        val ontologyConcepts = Map[List[Span],List[OntologyConcept]]()

        result.findings.foreach( finding => {
            val spanSet = finding.ranges.map(range => new Span(range.begin, range.end)).toSet

            val spans = spanSet.toList.sortBy(_.begin).foldLeft(List[Span]())((acc,span) => {
                if (acc.isEmpty)
                    acc ++ List(span)
                else if (acc.last.end +1 == span.begin){
                    val begin = acc.last.begin
                    acc.dropRight(1) ++ List(new Span(begin,span.end))
                }
                else
                    acc ++ List(span)
            }).toList

            val concept = new OntologyConcept(source, finding.concept.termId, finding.score, finding.concept.uri)
            var concepts = ontologyConcepts.getOrElse(spans,List[OntologyConcept]())
            concepts ::= concept
            ontologyConcepts += (spans -> concepts)
        })

        ontologyConcepts.foreach {
            case(spans, concepts) => {
                new NamedEntityMention(spans.toArray,text,concepts.reverse)
            }
        }
    }
}

class MeshEnhancer extends BioASQNEEnhancer(BioASQServiceCall.getMeSHConcepts, "MESH")

class JochemEnhancer extends BioASQNEEnhancer(BioASQServiceCall.getJochemConcepts, "JOCHEM")

class UniprotEnhancer extends BioASQNEEnhancer(BioASQServiceCall.getUniprotConcepts, "UNIPROT")

class DoidEnhancer extends BioASQNEEnhancer(BioASQServiceCall.getDoidConcepts, "DOID")

class GoEnhancer extends BioASQNEEnhancer(BioASQServiceCall.getGoConcepts, "GO")



