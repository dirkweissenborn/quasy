import cc.mallet.types.StringKernel
import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.enhancer.stanford.CorefStanfordEnhancer
import de.tu.dresden.quasy.io.{AnnotatedTextSource, LoadGoldStandards}
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Chunk, OntologyEntityMention, OntologyConcept, Sentence}
import de.tu.dresden.quasy.model.db.LuceneIndex
import de.tu.dresden.quasy.run.RunFullPipeline
import io.Source
import java.io.{FileWriter, PrintWriter, FileInputStream, File}
import java.text.Normalizer
import java.util.Properties
import org.apache.lucene.queryParser.QueryParser
import org.apache.lucene.util.Version
import pitt.search.semanticvectors._

/**
 * @author dirk
 *          Date: 5/14/13
 *          Time: 2:24 PM
 */


val original = Source.fromFile("QA_1370963959453.csv").getLines().take(1).toList.head.split(",")
val newOne = Source.fromFile("QA_1371025678291.csv").getLines().take(1).toList.head.split(",")
val map = newOne.map(s => (s,original.indexOf(s))).toMap


Source.fromFile("QA_1371025678291.csv").getLines().drop(1).foreach(line => {
    if (line.startsWith("#"))
        println(line)
    else {
        val scores = line.split(",")
        println(scores.zip(newOne).map {
            case (s,l) => (s,map(l))
        }.sortBy(_._2).map(_._1).mkString(","))
    }
})


/*val s1 = "fibrotic tissue"
val s2 = "fibrocytes"

val sk = new StringKernel()

val s = sk.K(s1,s2)
s  */

val props = new Properties()
props.load(new FileInputStream("conf/configuration.properties"))

val l = LuceneIndex.fromConfiguration(props)


val q = new QueryParser(Version.LUCENE_36, "contents", l.analyzer).
    parse("\"rheumatoid arthritis\" AND \"less common in men\"")

val top = l.searcher.search(q,1000)

top

/*val goldAnswers = LoadGoldStandards.load(new File("corpus/questions.pretty.json"))
val props = new Properties()
props.load(new FileInputStream("conf/configuration.properties"))

//val answer = new AnnotatedText("Disease patterns in RA vary between the sexes; the condition is more commonly seen in women, who exhibit a more aggressive disease and a poorer long-term outcome.")
//val answer = new AnnotatedText("Are there any DNMT3 proteins present in plants?")
//CorefStanfordEnhancer.enhance(answer)

val pipeline = RunFullPipeline.getFullPipeline(props)

val goldCandidates = goldAnswers.map(a => {
    val q = new AnnotatedText(a.id,a.body)
    val answer = new AnnotatedText(a.answer.ideal)
    (q,answer)
})

pipeline.process(AnnotatedTextSource(goldCandidates.map(_._1):_*))
pipeline.process(AnnotatedTextSource(goldCandidates.map(_._2):_*))

goldCandidates.foreach{
    case(question,answer) => {
        println("#### QUESTION ####")
        question.getAnnotations[Sentence].foreach(sentence => {
            //NounalRelationEnhancer.enhance(text)

            println(question.id + "\t" + sentence.coveredText)

            println()
            println(question.getAnnotationsBetween[OntologyEntityMention](sentence.begin,sentence.end).map(_.toString).mkString("\t"))
            println(question.getAnnotationsBetween[Chunk](sentence.begin,sentence.end).map(_.toString).mkString("\t"))
            println()

            println(sentence.printRoleLabels)
            println(sentence.getDependencyTree.prettyPrint)
            println()
        })

        println("#### ANSWER ####")
        answer.getAnnotations[Sentence].foreach(sentence => {
            println(sentence.coveredText)


            println()
            println(answer.getAnnotationsBetween[OntologyEntityMention](sentence.begin,sentence.end).map(_.toString).mkString("\t"))
            println(answer.getAnnotationsBetween[Chunk](sentence.begin,sentence.end).map(_.toString).mkString("\t"))
            println()

            println(sentence.printRoleLabels)
            println(sentence.getDependencyTree.prettyPrint)
            println()
        })
    }
} */


//Search.main("-searchtype permutation -queryvectorfile elementalvectors.bin -searchvectorfile permtermvectors.bin sequenc consensu ?".split(" "))

lazy val flagConfig = FlagConfig.getFlagConfig(Array("-queryvectorfile","elementalvectors.bin", "-searchvectorfile", "permtermvectors.bin"))
lazy val vecReader = new VectorStoreReaderLucene("elementalvectors.bin",flagConfig)
lazy val searchVecReader = new VectorStoreReaderLucene("permtermvectors.bin",flagConfig)

val vec2 = CompoundVectorBuilder.getQueryVectorFromString(
    searchVecReader, null, flagConfig,  "aataaa")

val vec3 = CompoundVectorBuilder.getQueryVectorFromString(
    searchVecReader, null, flagConfig,  "gene")

val vec1 = CompoundVectorBuilder.getPermutedQueryVector(
    vecReader, null, flagConfig,  "consensu sequenc ?".split(" "))

println(vec1.measureOverlap(vec2))
println(vec1.measureOverlap(vec3))