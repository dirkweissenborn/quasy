package de.tu.dresden.quasy.run

import de.tu.dresden.quasy.model.db.LuceneIndex
import java.io.File
import org.apache.lucene.search._
import org.apache.lucene.index.Term
import org.apache.lucene.search.BooleanClause.Occur
import org.apache.lucene.search.join.{ToParentBlockJoinCollector, ScoreMode, ToParentBlockJoinQuery}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.KeywordAnalyzer

/**
 * @author dirk
 *          Date: 7/17/13
 *          Time: 3:33 PM
 */
object Test extends App{

    val index = new LuceneIndex(new File("/media/dirk/mra_backup/metamap_index"))

    val parentFilter = new CachingWrapperFilter(
        new QueryWrapperFilter(
            new TermQuery(new Term("type", "utterance"))))

    val skuQuery = new QueryParser(Version.LUCENE_40,"",new KeywordAnalyzer).parse("(cui:C0868928 AND type:candidate) OR (cui:C1710187 AND type:candidate)")

    val skuJoinQuery = new ToParentBlockJoinQuery(
        skuQuery,
        parentFilter,
        ScoreMode.Total)

    val c = new ToParentBlockJoinCollector(
        Sort.INDEXORDER, // sort
        1000,             // numHits
        true,           // trackScores
        false           // trackMaxScore
    )
    index.searcher.search(skuJoinQuery, c)

    val hits = c.getTopGroups(
        skuJoinQuery,
        Sort.INDEXORDER,
        0,   // offset
        1000,  // maxDocsPerGroup
        0,   // withinGroupOffset
        true // fillSortFields
    )

    hits
}
