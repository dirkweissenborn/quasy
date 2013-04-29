package de.tu.dresden.quasy.enhancer

import de.tu.dresden.quasy.model.AnnotatedText
import actors.Actor

/**
 * @author dirk
 * Date: 3/28/13
 * Time: 12:46 PM
 */
abstract class TextEnhancer extends Actor {
    def enhance(text: AnnotatedText)

    def enhanceBatch(texts:List[AnnotatedText]) {
        val batchText = new AnnotatedText(texts.map(_.text).mkString(" "))
        var offset = 0
        texts.foreach(text => {
            text.getAllAnnotations.foreach(an =>{
                an.spans.foreach(span => {
                    span.begin += offset
                    span.end += offset
                })
                an.context = batchText
                batchText.addAnnotation(an)
            })

            offset += text.text.length + 1
        } )

        enhance(batchText)

        batchText.getAllAnnotations.foreach(annotation => {
            var offset = 0
            texts.find(text => { offset += text.text.length+1; annotation.end < offset }) match {
                case Some(text) => {
                    annotation.spans.foreach(span => {
                        span.begin -= offset-text.text.length-1
                        span.end -= offset-text.text.length-1
                    })
                    annotation.context = text

                    text.addAnnotation(annotation)
                }
                case None =>
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
