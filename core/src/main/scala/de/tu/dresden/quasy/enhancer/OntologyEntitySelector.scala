package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.OntologyEntityMention

/**
 * @author dirk
 * Date: 5/8/13
 * Time: 10:41 AM
 */
class OntologyEntitySelector(scoreThreshold:Double, source:String = "") extends TextEnhancer{
    protected def pEnhance(text: AnnotatedText) {
        var oes = text.getAnnotations[OntologyEntityMention]

        oes.foreach(oe => oe.ontologyConcepts = oe.ontologyConcepts.filter( oc =>
            if(source.isEmpty)
                oc.score > scoreThreshold
            else
                !source.equals(oc.source) || oc.score > scoreThreshold))

        oes = oes.filterNot(_.ontologyConcepts.isEmpty)

        val groupedOes = oes.groupBy(_.spans.size > 1)

        text.removeAllAnnotationsOfType[OntologyEntityMention]
        //Just keep entity mentions with one span for now
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

        /*val annotations = text.getAnnotations[OntologyEntityMention]

        if (groupedOes.contains(true)) {
            val selectedOes = groupedOes(true).sortBy(-_.coveredText.length).foldLeft(List[OntologyEntityMention]())((accOes,oe) => {
                // check if previous OE mentions has overlaps with current OE mention
                if (!accOes.exists(accOe => oe.spans.foldLeft(false)( (contains, span) => contains || accOe.contains(span.begin,span.end) )) &&
                    !annotations.exists(annotation => oe.spans.foldLeft(false)( (contains, span) => contains || annotation.contains(span.begin,span.end) ))
                )
                    oe :: accOes
                else
                    accOes
            })

            selectedOes.foreach(oe =>
                if(oe.getTokens.exists(_.posTag.matches(PosTag.ANYNOUN_PATTERN)))
                    text.addAnnotation(oe)
            )
        }*/
    }
}
