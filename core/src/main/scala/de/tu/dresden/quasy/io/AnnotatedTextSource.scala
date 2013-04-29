package de.tu.dresden.quasy.io

import de.tu.dresden.quasy.model.AnnotatedText

/**
 * @author dirk
 * Date: 4/18/13
 * Time: 11:50 AM
 */
trait AnnotatedTextSource extends Iterator[AnnotatedText]

class AnnotatedTextIteratorSource(iterator:Iterator[AnnotatedText]) extends AnnotatedTextSource {
    def hasNext = iterator.hasNext

    def next = iterator.next()
}