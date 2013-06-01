package de.tu.dresden.quasy.io

import de.tu.dresden.quasy.model.AnnotatedText
import io.Source
import java.io.File

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 11:50 AM
 */
object AnnotatedTextSource {
    type AnnotatedTextSource = Iterator[AnnotatedText]

    def apply(texts: AnnotatedText*):AnnotatedTextSource = texts.toIterator

    def fromFile(file:File):AnnotatedTextSource = new AnnotatedTextSource {
        private var lineIt = Source.fromFile(file).getLines()

        private var ct = -1

        def hasNext = {
            lineIt.hasNext
        }

        def next() = {
            var result = ""
            var currentLine = ""
            ct += 1

            while (lineIt.hasNext && {currentLine = lineIt.next(); !currentLine.isEmpty})
                result += currentLine + "\n"

            new AnnotatedText(ct.toString, result.trim)
        }

        def reset {
            lineIt = Source.fromFile(file).getLines()
        }
    }
}