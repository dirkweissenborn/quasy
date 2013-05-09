package de.tu.dresden.quasy

import scala.io.Source

/**
 * @author dirk
 * Date: 4/30/13
 * Time: 5:36 PM
 */
object QuasyFactory {
    val stopWords =
        Source.fromFile(getClass.getClassLoader.getResource("stopwords.txt").getPath,"ISO8859_1").getLines().map(_.trim).toSet
}
