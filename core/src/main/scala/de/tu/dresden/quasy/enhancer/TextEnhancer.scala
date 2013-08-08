package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.AnnotatedText
import actors.Actor
import de.tu.dresden.quasy.model.annotation.Annotation
import org.apache.commons.logging.LogFactory

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 12:46 PM
 */
abstract class TextEnhancer extends Actor {
    private val id = this.getClass.getSimpleName

    protected def pEnhance(text: AnnotatedText)

    def enhance(text: AnnotatedText) {
        if(!text.enhancedBy.contains(id)) {
            pEnhance(text)
            text.enhancedBy += id
        }
    }

    def enhanceBatch(texts:Seq[AnnotatedText]) {
        val batchText = new AnnotatedText(texts.map(_.text).mkString("\n"))
        var offset = 0
        texts.foreach(text => {
            val sortedAnnotations = text.getAllAnnotations.toSet[Annotation].toSeq.sortBy(_.begin)
            sortedAnnotations.foreach(an =>{
                //TODO CAREFUL with annotations which share spans
                an.spans.foreach(span => {
                    span.begin += offset
                    span.end += offset
                })
                an.context = batchText
                batchText.addAnnotation(an)
            })
            text.removeAllAnnotations
            offset += text.text.length+1
        } )

        enhance(batchText)

        offset = 0
        val iterator = texts.toIterator
        var text = iterator.next()
        val sortedAnnotations = batchText.getAllAnnotations.toSet[Annotation].toSeq.sortBy(_.end)
        sortedAnnotations.foreach(annotation => {
            while(offset + text.text.length < annotation.end) {
                offset += text.text.length+1
                text = iterator.next()
            }
            if (offset <= annotation.begin) {
                annotation.spans.foreach(span => {
                    span.begin -= offset
                    span.end -= offset
                })
                annotation.context = text

                text.addAnnotation(annotation)
            }
        })
    }

    def act() {
        loop {
            receive {
                case text:AnnotatedText => enhance(text); reply(text)
                case texts:List[AnnotatedText] => enhanceBatch(texts); reply(texts)
                case _ => null;exit()
            }
        }
    }
}
