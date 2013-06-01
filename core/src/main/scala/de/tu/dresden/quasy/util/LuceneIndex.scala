package de.tu.dresden.quasy.util

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.index.IndexReader
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import pitt.search.lucene.PorterAnalyzer
import java.util.Properties
import javax.naming.ConfigurationException

/**
 * @author dirk
 * Date: 5/22/13
 * Time: 11:06 AM
 */
case class LuceneIndex(val luceneIndex:File) {

    var index = FSDirectory.open(luceneIndex)
    var reader = IndexReader.open(index)
    var searcher = new IndexSearcher(reader)
    var analyzer=  new PorterAnalyzer()

    def close {
        reader.close()
    }

    def open {
        reader.close()

        index = FSDirectory.open(luceneIndex)
        reader = IndexReader.open(index)
        searcher = new IndexSearcher(reader)
        analyzer=  new PorterAnalyzer()
    }
}

object LuceneIndex{
    var index:LuceneIndex = null
    def fromConfiguration(config:Properties):LuceneIndex = {
        if (index == null)
            index = LuceneIndex({
                val indexPath: String = config.getProperty("lucene.index")
                if(indexPath == null)
                    throw new ConfigurationException("lucene.index not set in properties")

                val indexFile = new File(indexPath)
                if(!indexFile.isDirectory)
                    throw new ConfigurationException("Property lucene.index points to not existing directory")

                indexFile
            } )

        index
    }
}
