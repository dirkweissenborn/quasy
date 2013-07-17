package de.tu.dresden.quasy.model.annotation

import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.util.Xmlizer

/**
 * @author dirk
 * Date: 4/5/13
 * Time: 12:56 PM
 */
abstract class Annotation(var spans:Array[Span], var context:AnnotatedText) {
    {
        var end = spans.head.end
        if (spans.tail.exists(span => {
            if(end > span.begin)
                true
            else {
                end = span.end
                false
            }
        }))
            throw new IllegalArgumentException("spans must be ordered and must not overlap!")
    }

    def this(begin:Int,end:Int,context:AnnotatedText) = this(Array(new Span(begin,end)),context)

    //Dirty fix for accessing this context in subclasses
    protected def getContext = context

    addToContext

    val coveredText = spans.map(span => context.text.substring(span.begin,span.end)).mkString(" ")
    def begin = spans.head.begin
    def end = spans.last.end

    override def toString = getClass.getSimpleName+ "["+coveredText+"]"

    def between(begin:Int,end:Int):Boolean = this.begin >= begin && this.end <= end

    def contains(begin:Int,end:Int):Boolean = this.spans.foldLeft(false)((acc, span) => acc || (span.begin <= begin && span.end >= end))

    def getTokens = {
        if (tokens == null)
            tokens = context.getAnnotations[Token].filter(token => this.contains(token.begin,token.end))
        if (tokens.isEmpty){
            tokens = context.getAnnotations[Token].filter(token => token.contains(begin,end))
        }
        tokens
    }

    def getAnnotationsWithin[T <: Annotation](implicit m:Manifest[T]) = context.getAnnotations[T].filter(a => a.spans.forall(s => this.contains(s.begin,s.end)))

    private var tokens:List[Token] = null

    def addToContext {
        context.addAnnotation(this)
    }

    override def equals(obj:Any) = obj match {
        case that:Annotation if that.getClass.equals(this.getClass) => this.begin.equals(that.begin) && this.end.equals(that.end) && this.context.equals(that.context)
        case _ => false
    }

    /**
     * Be careful, this will probably destroy the original annotation structure, clone the whole annotated context before
     * if you need the old structure still. Not really working
     */
    /*def toAnnotatedText = {
        val result = new AnnotatedText(coveredText)
        val begin = this.begin
        context.getAllAnnotationsBetween(begin,end).foreach(annot => {
            annot.spans.foreach(span => { span.end -= begin; span.begin -= begin})
            annot.context = result
            result.addAnnotation(annot)
        })

        result
    }*/

    override def clone =
        Xmlizer.fromXml[this.type](Xmlizer.toXml(this))
}

class Section(spans:Array[Span], context:AnnotatedText) extends Annotation(spans,context) {
    if (spans.size > 1) {
        throw new IllegalArgumentException("Section can just span over one span range!")
    }

    def this(begin:Int,end:Int,context:AnnotatedText) = this(Array(new Span(begin,end)),context)

    def getSentences = context.getAnnotations[Sentence].filter(sentence => this.contains(sentence.begin,sentence.end)).asInstanceOf[List[Sentence]]
}





