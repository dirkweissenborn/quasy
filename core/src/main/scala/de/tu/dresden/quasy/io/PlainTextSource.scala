package de.tu.dresden.quasy.io

import java.io.File
import io.Source
import de.tu.dresden.quasy.model.AnnotatedText
import scala.Array

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 11:54 AM
 */

object PlainTextSource {
    private class PlainTextSource(file:File) extends AnnotatedTextSource {
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

    def fromFile(file:File):AnnotatedTextSource = new PlainTextSource(file)
}
