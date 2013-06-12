package de.tu.dresden.quasy.answer.score.factoid

import de.tu.dresden.quasy.answer.model.FactoidAnswer
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version
import de.tu.dresden.quasy.model.db.LuceneIndex

/**
 * @author dirk
 * Date: 6/4/13
 * Time: 2:31 PM
 */
class IdfScorer(val luceneIndex:LuceneIndex) extends FactoidScorer{

    private final val maxScore = math.log(luceneIndex.reader.maxDoc())

    def scoreInternal(factoid: FactoidAnswer) = {
        val q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).
            parse( factoid.answer.synonyms.map(syn => "\""+syn.replace("\"","")+"\"").mkString(" OR "))

        val nrOfDocs = luceneIndex.searcher.search(q,null,Int.MaxValue).totalHits

        if (nrOfDocs != 0)
            math.log(luceneIndex.reader.maxDoc()/nrOfDocs) / maxScore
        else
            1.0
    }
}
