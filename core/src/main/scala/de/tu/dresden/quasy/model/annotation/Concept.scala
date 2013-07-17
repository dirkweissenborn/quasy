package de.tu.dresden.quasy.model.annotation

import de.tu.dresden.quasy.model.feature.Nominal
import de.tu.dresden.quasy.model.{Span, AnnotatedText}

/**
 * @author dirk
 *         Date: 4/8/13
 *         Time: 2:09 PM
 */
class Concept(spans:Array[Span], context:AnnotatedText, val conceptType:String) extends Annotation(spans,context) {
    def this(begin:Int,end:Int,context:AnnotatedText, conceptType:String) = this(Array(new Span(begin,end)),context,conceptType)
}