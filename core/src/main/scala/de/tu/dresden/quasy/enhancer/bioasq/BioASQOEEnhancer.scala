package de.tu.dresden.quasy.enhancer.bioasq

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{OntologyConcept, OntologyEntityMention}
import collection.mutable._
import org.apache.commons.logging.LogFactory
import de.tu.dresden.quasy.webservices.bioasq.{BioASQService}
import de.tu.dresden.quasy.webservices.bioasq.model.BioASQServiceResult
import collection.immutable

/**
 * @author dirk
 * Date: 4/10/13
 * Time: 10:42 AM
 */
abstract class BioASQOEEnhancer(serviceCall: String => BioASQServiceResult, source:String, minScore:Double=0.0) extends TextEnhancer {
    private val LOG = LogFactory.getLog(getClass)

    def enhance(text: AnnotatedText) {
        val result =  serviceCall(text.text)

        if (result != null) {
            val ontologyConcepts = Map[List[Span],List[OntologyConcept]]()

            result.findings.foreach( finding => {
                if (finding.score >= minScore) {
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

                    val concept = new OntologyConcept(source, finding.concept.label,finding.concept.label,immutable.Set[String](), finding.score, finding.concept.uri)
                    var concepts = ontologyConcepts.getOrElse(spans,List[OntologyConcept]())
                    concepts ::= concept
                    ontologyConcepts += (spans -> concepts)
                }
            })

            ontologyConcepts.foreach {
                case(spans, concepts) => {
                    new OntologyEntityMention(spans.toArray,text,concepts.reverse)
                }
            }

        }
    }
}

class MeshEnhancer extends BioASQOEEnhancer((new BioASQService).getMeSHConcepts, OntologyConcept.SOURCE_MESH)

class JochemEnhancer extends BioASQOEEnhancer((new BioASQService).getJochemConcepts, OntologyConcept.SOURCE_JOCHEM)

class UniprotEnhancer extends BioASQOEEnhancer((new BioASQService).getUniprotConcepts, OntologyConcept.SOURCE_UNIPROT)

class DoidEnhancer extends BioASQOEEnhancer((new BioASQService).getDoidConcepts, OntologyConcept.SOURCE_DOID)

class GoEnhancer extends BioASQOEEnhancer((new BioASQService).getGoConcepts, OntologyConcept.SOURCE_GO)



