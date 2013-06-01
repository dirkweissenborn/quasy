import de.tu.dresden.quasy.answer.model.AnswerContext
import de.tu.dresden.quasy.enhancer.stanford.CorefStanfordEnhancer
import de.tu.dresden.quasy.io.{AnnotatedTextSource, LoadGoldStandards}
import de.tu.dresden.quasy.model.AnnotatedText
import de.tu.dresden.quasy.model.annotation.{Chunk, OntologyEntityMention, OntologyConcept, Sentence}
import de.tu.dresden.quasy.run.RunFullPipeline
import java.io.{FileInputStream, File}
import java.util.Properties
import pitt.search.semanticvectors._

/**
 * @author dirk
 *          Date: 5/14/13
 *          Time: 2:24 PM
 */

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