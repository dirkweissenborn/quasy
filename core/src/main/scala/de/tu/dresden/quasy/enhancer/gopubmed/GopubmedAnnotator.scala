package de.tu.dresden.quasy.enhancer.gopubmed

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{Token, OntologyEntityMention, OntologyConcept}
import de.tu.dresden.quasy.webservices.gopubmed.GoPubMedService
import collection.mutable._
import collection.immutable

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 2:13 PM
 */
class GopubmedAnnotator extends TextEnhancer{
    private val service = new GoPubMedService

    def enhance(text: AnnotatedText) {
        /*val tokens = text.getAnnotations[Token].sortBy(_.begin)
        val newTokenBegins = Map[Int,Token]()
        val newTokenEnds = Map[Int,Token]()
        val newText = tokens.foldLeft("")((accText,token) => {
            newTokenBegins += ((accText.length + 1) -> token)
            newTokenEnds += ((accText.length+token.coveredText.length + 1) -> token)
            accText +" "+ token.coveredText
        }) */

        val result =  service.getEntityConcepts(text.text)

        if (result != null) {
            val ontologyConcepts = Map[List[Span],List[OntologyConcept]]()

            val uriLabelMap = result.terms.map(term => (term.uri,term.label)).toMap

            result.annotations.groupBy(_.conceptUri).values.foreach(annotations =>
                annotations.foreach(annotation => {
                    val source = annotation.conceptUri.getPath.replace("http://namespaces.transinsight.com/","").substring(1).replaceAll("[#/].+","").toUpperCase

                    val concept = new OntologyConcept(source, uriLabelMap(annotation.conceptUri), uriLabelMap(annotation.conceptUri), immutable.Set[String](), 1.0, annotation.conceptUri)
                    val spans = annotation.ranges.sortBy(_.begin).foldLeft(List[Span]())((acc,span) => {
                        try {
                           /* var begin = newTokenBegins.get(span.begin)
                            var end = newTokenEnds.get(span.end)

                            if(begin.equals(None)) {
                                //Fallback
                                begin = Some(newTokenBegins.minBy(el => math.abs(el._1 - span.begin))._2)
                            }
                            if(end.equals(None)) {
                                end = Some(newTokenEnds.minBy(el => math.abs(el._1 - span.end))._2)
                            }  */
                            if(text.text.substring(span.end-1,span.end).matches("""[.!?\-:,;]"""))
                                span.end -= 1
                            val newSpan = new Span(span.begin,span.end)
                            if (acc.isEmpty)
                                acc ++ List(newSpan)
                            else {
                                val last = acc.last
                                if (last.end + 2 >= newSpan.begin){
                                    acc.dropRight(1) ++ List(new Span(last.begin,math.max(newSpan.end,last.end)))
                                }
                                else if(newSpan !=acc.last)
                                    acc ++ List(newSpan)
                                else
                                    acc
                            }
                        }
                        catch {
                            case e => e.printStackTrace(); acc
                        }
                    }).toList
                    var concepts = ontologyConcepts.getOrElse(spans,List[OntologyConcept]())
                    if (!concepts.exists(_.uri == concept.uri))
                        concepts ::= concept
                    ontologyConcepts += (spans -> concepts)
                }))

            ontologyConcepts.foreach {
                case(spans, concepts) => {
                    new OntologyEntityMention(spans.toArray,text,concepts.reverse)
                }
            }
        }
    }

}

object GopubmedAnnotator{

    def main(args:Array[String]) {
        val result = (new GoPubMedService).getEntityConcepts(text)
        result
    }

    val text = """Eukaryotic DNA cytosine methylation can be used to transcriptionally silence repetitive sequences, including transposons and retroviruses. This silencing is stable between cell generations as cytosine methylation is maintained epigenetically through DNA replication. The Arabidopsis thaliana Dnmt3 cytosine methyltransferase ortholog DOMAINS rearranged methyltransferase2 (DRM2) is required for establishment of small interfering RNA (siRNA) directed DNA methylation. In mammals PIWI proteins and piRNA act in a convergently evolved RNA-directed DNA methylation system that is required to repress transposon expression in the germ line. De novo methylation may also be independent of RNA interference and small RNAs, as in Neurospora crassa. Here we identify a clade of catalytically mutated DRM2 paralogs in flowering plant genomes, which in A.thaliana we term domains rearranged methyltransferase3 (DRM3). Despite being catalytically mutated, DRM3 is required for normal maintenance of non-CG DNA methylation, establishment of RNA-directed DNA methylation triggered by repeat sequences and accumulation of repeat-associated small RNAs. Although the mammalian catalytically inactive Dnmt3L paralogs act in an analogous manner, phylogenetic analysis indicates that the DRM and Dnmt3 protein families diverged independently in plants and animals. We also show by site-directed mutagenesis that both the DRM2 N-terminal UBA domains and C-terminal methyltransferase domain are required for normal RNA-directed DNA methylation, supporting an essential targeting function for the UBA domains. These results suggest that plant and mammalian RNA-directed DNA methylation systems consist of a combination of ancestral and convergent features."""
}
