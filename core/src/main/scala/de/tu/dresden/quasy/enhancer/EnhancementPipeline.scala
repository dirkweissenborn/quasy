package de.tu.dresden.quasy.enhancer

import java.io.{FileWriter, File}
import de.tu.dresden.quasy.model.AnnotatedText
import actors.Future
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.io.AnnotatedTextSource
import collection.mutable._
import org.apache.commons.logging.LogFactory

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 10:49 AM
 */
class EnhancementPipeline(val enhancers:List[TextEnhancer]) {

    private val LOG = LogFactory.getLog(getClass)

    private var next = Map[TextEnhancer,TextEnhancer]()

    {
        var current:TextEnhancer = null
        val it = enhancers.iterator
        if(it.hasNext)
            current = it.next()
        else
            throw new IllegalArgumentException("Enhancement pipeline must consist of at least one enhancer")
        while(it.hasNext) {
            val nxt = it.next()
            next += (current -> nxt)
            current = nxt
        }
    }

    def process(texts:AnnotatedTextSource,outputDir:File = null, append:Boolean = true) {
        val writeOut = (outputDir != null)
        if (writeOut)
            outputDir.mkdirs()
        val assignedTexts = enhancers.foldLeft(Map[TextEnhancer,AnnotatedText]())( (acc,enhancer) => acc + (enhancer -> null))
        val futures = enhancers.foldLeft(Map[TextEnhancer,Future[AnnotatedText]]())( (acc,enhancer) => acc + (enhancer -> null))

        var first:AnnotatedText = null

        if (texts.hasNext)
            first = texts.next()

        if (append && outputDir!=null) {
            val files = outputDir.list()
            while(texts.hasNext && files.contains(first.id+".xml") )
                first = texts.next()
        }

        enhancers.foreach(_.start())

        if (first!=null) {
            assignedTexts(enhancers.head) = first

            while(assignedTexts.exists(_._2 != null)) {
                assignedTexts.foreach {
                    case (enhancer,text) => {
                        if (text != null) {
                            futures(enhancer) = (enhancer !! text).asInstanceOf[Future[AnnotatedText]]
                            assignedTexts(enhancer) = null
                        }
                    }
                }

                futures.foreach {
                    case (enhancer,future) => {
                        if (future != null) {
                            val text = future.apply()
                            futures(enhancer) = null
                            next.get(enhancer) match {
                                case Some(nextEnhancer) => assignedTexts(nextEnhancer) = text
                                case None => {
                                    if (writeOut) {
                                        val fw = new FileWriter(new File(outputDir,text.id+".xml"))
                                        fw.write(Xmlizer.toXml(text))
                                        fw.close()
                                        LOG.info("Written result for input text: "+text.id)
                                    }
                                }
                            }
                        }
                    }
                }

                if (texts.hasNext)
                    assignedTexts(enhancers.head) = texts.next()
            }
        }
    }

    def processBatches(texts:AnnotatedTextSource, batchsize:Int, outputDir:File = null, append:Boolean = true) {
        val writeOut = (outputDir != null)
        if (writeOut)
            outputDir.mkdirs()
        val assignedTexts = enhancers.foldLeft(Map[TextEnhancer,List[AnnotatedText]]())( (acc,enhancer) => acc + (enhancer -> null))
        val futures = enhancers.foldLeft(Map[TextEnhancer,Future[List[AnnotatedText]]]())( (acc,enhancer) => acc + (enhancer -> null))

        var first = texts.take(batchsize).toList

        if (append && outputDir!=null) {
            val files = outputDir.list()
            first = first.dropWhile(t => files.contains(t.id+".xml"))
            while(texts.hasNext && first.isEmpty )
                first = texts.take(batchsize).dropWhile(t => files.contains(t.id+".xml")).toList
        }

        enhancers.foreach(_.start())

        if (!first.isEmpty) {
            assignedTexts(enhancers.head) = first

            while(assignedTexts.exists(_._2 != null)) {
                assignedTexts.foreach {
                    case (enhancer,text) => {
                        if (text != null) {
                            futures(enhancer) = (enhancer !! text).asInstanceOf[Future[List[AnnotatedText]]]
                            assignedTexts(enhancer) = null
                        }
                    }
                }

                futures.foreach {
                    case (enhancer,future) => {
                        if (future != null) {
                            val texts = future.apply()
                            futures(enhancer) = null
                            next.get(enhancer) match {
                                case Some(nextEnhancer) => assignedTexts(nextEnhancer) = texts
                                case None => {
                                    if (writeOut) {
                                        texts.foreach(text => {
                                            val fw = new FileWriter(new File(outputDir,text.id+".xml"))
                                            fw.write(Xmlizer.toXml(text))
                                            fw.close()
                                            LOG.info("Written result for input text: "+text.id)
                                        })
                                    }
                                }
                            }
                        }
                    }
                }

                if (texts.hasNext)
                    assignedTexts(enhancers.head) = texts.take(batchsize).toList
            }
        }
    }

}
