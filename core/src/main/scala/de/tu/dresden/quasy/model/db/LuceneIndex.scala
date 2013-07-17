package de.tu.dresden.quasy.model.db

import java.io.File
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.index.{DirectoryReader}
import org.apache.lucene.search.IndexSearcher
import org.apache.lucene.analysis.en.EnglishAnalyzer
import org.apache.lucene.util.Version
import java.util.Properties
import javax.naming.ConfigurationException
import org.apache.lucene.analysis.Analyzer

/**
 * @author dirk
 * Date: 5/22/13
 * Time: 11:06 AM
 */
case class LuceneIndex(luceneIndex:File, var analyzer: Analyzer = new EnglishAnalyzer(Version.LUCENE_40)) {

    var index = FSDirectory.open(luceneIndex)
    var reader = DirectoryReader.open(index)
    var searcher = new IndexSearcher(reader)

    def close {
        reader.close()
    }

    def open {
        reader.close()

        index = FSDirectory.open(luceneIndex)
        reader = DirectoryReader.open(index)
        searcher = new IndexSearcher(reader)
    }
}

object LuceneIndex{
    var index:LuceneIndex = null
    def fromConfiguration(config:Properties, version:Version):LuceneIndex = {
        if (index == null)
            index = LuceneIndex({
                val indexPath: String = config.getProperty("lucene.index")
                if(indexPath == null)
                    throw new ConfigurationException("lucene.index not set in properties")

                val indexFile = new File(indexPath)
                if(!indexFile.isDirectory)
                    throw new ConfigurationException("Property lucene.index points to not existing directory")

                indexFile
            } , new EnglishAnalyzer(version))

        index
    }
}
