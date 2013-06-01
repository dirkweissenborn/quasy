package de.tu.dresden.quasy.answer.tycor


import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version
import de.tu.dresden.quasy.util.LuceneIndex
import de.tu.dresden.quasy.answer.score.factoid.FactoidScorer
import de.tu.dresden.quasy.answer.model.FactoidAnswer

/**
 * @author dirk
 * Date: 5/21/13
 * Time: 1:29 PM
 */
case class LuceneTycor(luceneIndex:LuceneIndex) extends FactoidScorer {

    def scoreInternal(factoid: FactoidAnswer) = {
        val aType = factoid.question.targetType

        var q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).
            parse( factoid.answer.synonyms.map(syn => "\""+syn+"\"").mkString(" OR "))

        val totalDocsNr = luceneIndex.searcher.search(q,null,Int.MaxValue).totalHits

        if (totalDocsNr > 0) {
            val queryStr: String =
                aType.coveredText.split(" ").permutations.
                    flatMap(perm => factoid.answer.synonyms.map( syn => "\"" + perm.mkString(" ") + " " + syn + "\"")).toSet.
                    mkString(" OR ")

            q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(queryStr)
            q = new QueryParser(Version.LUCENE_36, "contents", luceneIndex.analyzer).parse(q.toString.split(" contents:").toSet.mkString(" "))
            val partialDocs = luceneIndex.searcher.search(q,null,Int.MaxValue)

            partialDocs.totalHits.toDouble / totalDocsNr
        }
        else
            0.0
    }
}
