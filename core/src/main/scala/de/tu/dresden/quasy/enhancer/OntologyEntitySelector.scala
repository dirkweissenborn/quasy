package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.OntologyEntityMention

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 10:41 AM
 */
class OntologyEntitySelector(threshold:Double, source:String = "") extends TextEnhancer{
    def enhance(text: AnnotatedText) {
        var oes = text.getAnnotations[OntologyEntityMention]

        oes.foreach(oe => oe.ontologyConcepts = oe.ontologyConcepts.filter( oc =>
            if(source.isEmpty)
                oc.score > threshold
            else
                !source.equals(oc.source) || oc.score > threshold))

        oes = oes.filterNot(_.ontologyConcepts.isEmpty)

        val groupedOes = oes.groupBy(_.spans.size > 1)

        text.removeAllAnnotations[OntologyEntityMention]
        //Just keep entity mentions with one span for now
        //TODO also use OEs over more than one span
        if (groupedOes.contains(false)) {
            val selectedOes = groupedOes(false).sortBy(-_.coveredText.length).foldLeft(List[OntologyEntityMention]())((accOes,oe) => {
                // check if previous OE mentions has overlaps with the current OE mention
                if (!accOes.exists(accOe => oe.spans.foldLeft(false)( (contains, span) => contains || accOe.contains(span.begin,span.end) )  ))
                    oe :: accOes
                else
                    accOes
            })

            selectedOes.foreach(oe => text.addAnnotation(oe))
        }
    }
}
