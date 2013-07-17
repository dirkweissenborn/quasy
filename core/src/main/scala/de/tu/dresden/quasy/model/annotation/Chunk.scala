package de.tu.dresden.quasy.model.annotation

import de.tu.dresden.quasy.model.{Span, AnnotatedText}

/**
 * @author dirk
 *         Date: 4/8/13
 *         Time: 2:09 PM
 */
class Chunk(spans:Array[Span], context:AnnotatedText, val chunkType:String) extends Annotation(spans,context) {
    def this(begin:Int,end:Int,context:AnnotatedText, chunkType:String) = this(Array(new Span(begin,end)),context,chunkType)

    override def toString = getClass.getSimpleName+"_"+chunkType+ "["+coveredText+"]"

    if (spans.size > 1) {
        throw new IllegalArgumentException("Chunk can just span over one span range!")
    }
}