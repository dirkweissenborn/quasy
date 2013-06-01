import java.io.{IOException, File}
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.index.{CorruptIndexException, IndexWriter}
import org.apache.lucene.index.IndexWriter.MaxFieldLength
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.util.Version
import pitt.search.semanticvectors.{Search, BuildPositionalIndex}

/**
 * @author dirk
 *          Date: 5/9/13
 *          Time: 2:55 PM
 */

//BuildPositionalIndex.main("-luceneindexpath /data/pubmed/index -contentsfields contents,title -docidfield pmid -positionalmethod permutation".split(" "))
Search.main("-searchtype permutation -queryvectorfile elementalvectors.bin -searchvectorfile permtermvectors.bin aspirin treat ?".split(" "))