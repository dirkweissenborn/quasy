package de.tu.dresden.quasy.model

import annotation.Annotation
import com.thoughtworks.xstream.annotations.XStreamConverter
import de.tu.dresden.quasy.util.AnnotationsXstreamConverter
import collection.mutable._

/**
 * @author dirk
 * Date: 4/24/13
 * Time: 11:17 AM
 */
trait Annotatable {
    @XStreamConverter(classOf[AnnotationsXstreamConverter])
    protected var annotations = Map[Manifest[_],List[Annotation]]()
    private var mightHaveDuplicates = false

    def addAnnotation[T <: Annotation](annotation:T) {
        var m = Manifest.classType(annotation.getClass)
        val annotationM = Manifest.classType(classOf[Annotation])

        mightHaveDuplicates = true

        //Also add annotations to lists of superclasses, like a question annotation should also be a sentence annotation
        while(m.<:<(annotationM) && !m.equals(annotationM)) {
            annotations += (m -> (annotations.getOrElse(m, List[Annotation]()) ++ List(annotation)))
            m = Manifest.classType(m.erasure.getSuperclass)
        }
    }

    def getAnnotations[T <: Annotation](implicit m:Manifest[T]):List[T] = {
        annotations.getOrElse(m, List[T]()).asInstanceOf[List[T]]
    }

    def getAllAnnotations = {
        annotations.flatMap(_._2).toList
    }

    def getAnnotationsBetween[T <: Annotation](start:Int,end:Int)(implicit m:Manifest[T]) = getAnnotations[T].filter(_.between(start, end))

    def getAllAnnotationsBetween(start:Int,end:Int) = getAllAnnotations.filter(_.between(start, end))

    def removeAllAnnotationsOfType[T <: Annotation](implicit m:Manifest[T]) { annotations -= m }
    def removeAllAnnotations { annotations.clear() }

    def deleteDuplicates {
        if (mightHaveDuplicates)
            annotations.foreach( entry => {
                    annotations(entry._1) = entry._2.reverse.foldLeft(List[Annotation]())((acc,annot) => {
                        if (acc.exists(_.equals(annot)))
                            acc
                        else
                            annot :: acc
                    })
            })

        mightHaveDuplicates = false
    }
}
