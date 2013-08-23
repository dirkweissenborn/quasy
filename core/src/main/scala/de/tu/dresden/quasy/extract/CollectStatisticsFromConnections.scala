package de.tu.dresden.quasy.extract

import java.io.{FileWriter, PrintWriter, File}
import de.tu.dresden.quasy.model.db.LuceneIndex
import org.apache.lucene.analysis.core.KeywordAnalyzer
import org.apache.lucene.index.{Term}
import scala.io.Source

/**
 * @author dirk
 *          Date: 8/19/13
 *          Time: 5:33 PM
 */
object CollectStatisticsFromConnections {

    def main(args:Array[String]) {
        val indexesDir = new File(args(0))

        var map = Map[String,Int]()
        val indexes = indexesDir.listFiles().filter(_.isDirectory)
        var total = 0

        indexes.foreach(indexDir => {
            val cuis = Source.fromFile(new File(indexDir, ExtractionUtil.FREQUENCIES_FILENAME)).getLines().map(_.split("\t",2)(0)).filter(_.startsWith("C"))
            if(!cuis.isEmpty)
                total += 1
            cuis.foreach(cui => {
                map += (cui -> (1 + map.getOrElse(cui , 0)))
            })
        })

        indexes.foreach(indexDir => {
            val cuis = Source.fromFile(new File(indexDir, ExtractionUtil.FREQUENCIES_FILENAME)).getLines().map(_.split("\t",2)(0)).toSet
            val freqWriter =  new PrintWriter(new FileWriter(new File(indexDir, ExtractionUtil.FREQUENCIES_FILENAME)))
            freqWriter.println("total\t"+total)
            cuis.foreach(cui => {
                freqWriter.println(cui +"\t"+map(cui))
            })
            freqWriter.close()
        })
    }

}
