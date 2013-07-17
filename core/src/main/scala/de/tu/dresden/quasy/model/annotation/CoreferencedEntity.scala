package de.tu.dresden.quasy.model.annotation

import de.tu.dresden.quasy.model.{AnnotatedText, Span}

/**
 * @author dirk
 * Date: 5/2/13
 * Time: 2:59 PM
 */
class CoreferencedEntity(spans:Array[Span],
                         context:AnnotatedText, val mentionType:String, val reference:CoreferencedEntity) extends Annotation(spans,context) {
    def this(begin:Int, end:Int, context:AnnotatedText,mentionType:String,reference:CoreferencedEntity) = this(Array(new Span(begin,end)), context,mentionType,reference)
}

object CoreferencedEntity {
    final object MentionType {
        final val PRONOMINAL = "PRONOMINAL"
        final val NOMINAL = "NOMINAL"
        final val PROPER = "PROPER"
    }
}
