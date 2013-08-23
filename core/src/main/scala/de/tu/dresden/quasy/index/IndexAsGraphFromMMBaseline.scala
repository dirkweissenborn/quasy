package de.tu.dresden.quasy.index

import org.apache.commons.logging.LogFactory
import java.io.{FileInputStream, File}
import java.util.zip.GZIPInputStream
import de.tu.dresden.quasy.util.{MMCandidate, MMUtterance, MachineOutputParser}
import scala.io.Source

import java.util.Properties
import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.enhancer.clearnlp.FullClearNlpPipeline
import com.thinkaurelius.titan.core.{TitanGraph, TitanFactory}
import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{DepTag, Token, UmlsConcept, OntologyEntityMention}
import com.tinkerpop.blueprints.{Edge, Direction, Vertex}
import scala.collection.JavaConversions._
import scala.actors.Future
import org.apache.commons.configuration.BaseConfiguration
import com.tinkerpop.blueprints.Query.Compare

/**
 * @author dirk
 *          Date: 8/21/13
 *          Time: 2:00 PM
 */
object IndexAsGraphFromMMBaseline {


    private val LOG = LogFactory.getLog(getClass)

    var mmoToXml = ""
    var depParser:TextEnhancer = null

    def main(args:Array[String]) {

        val inputFile: String = args(0)
        val is = new GZIPInputStream(new FileInputStream(inputFile))
        val indexDir = new File(args(1))
        val _override = if(args.size > 3)
            args(3) == "override"
        else
            false

        val newGraph = indexDir.mkdirs() || _override || indexDir.list().isEmpty

        if(_override){
            LOG.info("Overriding output directory!")
            def deleteDir(dir:File) {
                dir.listFiles().foreach(f => {
                    if (f.isDirectory) {
                        deleteDir(f)
                        f.delete()
                    }
                    else
                        f.delete()
                })
            }
            deleteDir(indexDir)
        }

        val conf = new Properties()
        conf.load(new FileInputStream(new File(args(2))))

        depParser = FullClearNlpPipeline.fromConfiguration(conf, true)

        var currentUtterance = ""

        val parser = new MachineOutputParser

        //TITAN init
        val titanConf = new BaseConfiguration()
        titanConf.setProperty("storage.directory",new File(indexDir,"standard").getAbsolutePath)
        titanConf.setProperty("storage.index.search.backend","lucene")
        titanConf.setProperty("storage.index.search.directory",new File(indexDir,"searchindex").getAbsolutePath)

        val graph = TitanFactory.open(titanConf)
        if(newGraph) {
            graph.makeType().name("cui").dataType(classOf[String]).indexed(classOf[Vertex]).unique(Direction.BOTH).makePropertyKey()
            graph.makeType().name("semtypes").dataType(classOf[String]).indexed("search",classOf[Vertex]).unique(Direction.OUT).makePropertyKey()
            graph.makeType().name("uttIds").dataType(classOf[String]).indexed("search",classOf[Edge]).unique(Direction.OUT).makePropertyKey()
            graph.makeType().name("count").dataType(classOf[java.lang.Integer]).indexed(classOf[Edge]).unique(Direction.OUT).makePropertyKey()
            graph.commit()
        } else
            processedUtts = graph.getEdges.flatMap(e => {
               e.getProperty[String]("uttIds").split(",")
            }).toSet

        LOG.info(inputFile+": Processing: "+inputFile)

        var future:Future[Unit] = null

        var counter = 0
        val utteranceRegex = """utterance\('([^']+)',""".r


        Source.fromInputStream(is).getLines().foreach(line => {
            try {
                currentUtterance += line + "\n"
                if(line.equals("'EOU'.")) {
                    try {
                        val utterance = utteranceRegex.findFirstIn(currentUtterance).getOrElse("0.0.0")
                        val id = utterance.substring(utterance.indexOf('\'')+1,utterance.lastIndexOf('\''))

                        if(!processedUtts.contains(id)) {
                            val mmUtterance = parser.parse(currentUtterance)
                            if(future ne null)
                                future.apply()
                            if(mmUtterance ne null) {
                                future = actors.Futures.future(extractConnections(mmUtterance,graph))
                            }
                        }
                    }
                    catch {
                        case e:Exception => {
                            LOG.error(e.getMessage+ ", while parsing. Skipping it!")
                        }
                    }

                    counter += 1
                    currentUtterance = ""

                    if(counter % 1000 == 0) {
                        LOG.info(inputFile+": "+counter+" utterances processed!")
                        future.apply()
                        future2.apply()
                        graph.commit()
                    }
                }
            }
            catch {
                case e:Exception => {
                    LOG.error(inputFile+": "+e.printStackTrace())
                }
            }
        })
        future.apply()
        future2.apply()
        graph.shutdown()
        is.close()
    }

    var processedUtts = Set[String]()
    var future2: Future[Unit] =null
    def extractConnections(utt:MMUtterance, graph:TitanGraph) {
        val text = utt.text.replace("\"\"","\"") + "."
        val id = utt.pmid+"."+utt.section+"."+utt.num
        if(!processedUtts.contains(id)) {
            processedUtts += id

            val annotations = selectAnnotations(utt)

            if(annotations.size > 1) {
                val annotatedText = new AnnotatedText(id,text)
                depParser.enhance(annotatedText)

                if(future2 ne null)
                    future2.apply()
                future2 = actors.Futures.future(writeToGraph(annotatedText, text, annotations, graph))
            }
        }
    }


    def writeToGraph(annotatedText: AnnotatedText, text: String, annotations: Map[(Int, Int), List[MMCandidate]], graph: TitanGraph) {
        val id = annotatedText.id
        var offset = 0
        try {
            annotations.toSeq.sortBy(_._1._1).foreach {
                case ((start, end), candidates) => {
                    val entity = AnnotatedText.cleanText(text.substring(start, end))
                    val newStart = annotatedText.text.indexOf(entity, offset)
                    val newEnd = newStart + entity.length
                    offset = newStart + 1
                    val cands = candidates.filter(c => allowCandidate(c, entity))
                    if (cands.size > 0 &&
                        annotatedText.getAnnotationsBetween[Token](newStart, newEnd).exists(_.posTag.matches(PosTag.ANYNOUN_PATTERN)))
                        new OntologyEntityMention(
                            newStart,
                            newEnd,
                            annotatedText,
                            candidates.map(c => new UmlsConcept(c.cui, entity, Set[String](), c.semtypes.toSet, Set[String](), c.score)))
                }
            }

            annotatedText.getAnnotations[OntologyEntityMention].foreach(m1 => {
                annotatedText.getAnnotations[OntologyEntityMention].foreach(m2 => {
                    if (m1.begin < m2.begin) {
                        cleanPath(getDepPath(annotatedText, m1, m2).getOrElse(List[Token]())) match {
                            case Some(path) => {
                                val rel = printPath(path)
                                m1.ontologyConcepts.foreach(c1 => {
                                    m2.ontologyConcepts.foreach(c2 => {
                                        val it1 = graph.query.has("cui", Compare.EQUAL, c1.conceptId).limit(1).vertices()
                                        val it2 = graph.query.has("cui", Compare.EQUAL, c2.conceptId).limit(1).vertices()

                                        val from =
                                            if (!it1.isEmpty)
                                                it1.head
                                            else {
                                                val f = graph.addVertex(null)
                                                f.setProperty("cui", c1.conceptId)
                                                f.setProperty("semtypes", c1.asInstanceOf[UmlsConcept].semanticTypes.filter(s => selectedSemtypes.contains(s)).mkString(","))
                                                f
                                            }

                                        val to = if (!it2.isEmpty)
                                            it2.head
                                        else {
                                            val t = graph.addVertex(null)
                                            t.setProperty("cui", c2.conceptId)
                                            t.setProperty("semtypes", c2.asInstanceOf[UmlsConcept].semanticTypes.filter(s => selectedSemtypes.contains(s)).mkString(","))
                                            t
                                        }

                                        from.getEdges(Direction.OUT, rel).iterator().find(_.getVertex(Direction.IN) == to) match {
                                            case Some(edge) => {
                                                val ids = edge.getProperty[String]("uttIds").split(",")
                                                if (!ids.contains(id)) {
                                                    edge.setProperty("count", edge.getProperty[java.lang.Integer]("count") + 1)
                                                    edge.setProperty("uttIds", ids.mkString(",") + "," + id)
                                                }
                                            }
                                            case None => {
                                                val edge = graph.addEdge(null, from, to, rel)
                                                edge.setProperty("uttIds", id)
                                                edge.setProperty("count", 1)
                                            }
                                        }
                                    })
                                })
                            }
                            case None =>
                        }

                    }
                })
            })
        }
        catch {
            case e: Exception => e.printStackTrace()
        }
    }

    private val selectedSemtypes =
        Set("amph","anim","arch","bact","bird","euka","fish","fngs","humn","mamm","plnt","rept","vtbt","virs", //Organisms
            "acab","anab","bpoc","celc","cell","cgab","emst","ffas","gngm","tisu",  //anatomical structures
            "clnd",  //clinical drug
            "sbst","aapp","antb","bacs","bodm","bdsu","carb","chvf","chvs","chem","eico","elii","enzy","food", //Substances
            "hops","horm","imft","irda","inch","lipd","nsba","nnon","orch","opco","phsu","rcpt","strd","vita", //Substances
            "sosy",// Sign or Symptom
            "amas","blor","bsoj","crbs","mosq","nusq", //molecular sequences, body parts
            "patf","comd","dsyn","emod","mobd","neop",     //pathologic functions
            "inpo") //injury or poisening

    private val stopSemTypes = Set("ftcn","qlco","qnco","idcn","inpr")

    def allowCandidate(cand : MMCandidate, coveredText:String):Boolean = {
        if(cand.semtypes.exists(s => selectedSemtypes.contains(s))) {
            if(coveredText.length < 2)
                false
            else if(cand.semtypes.contains("gngm")) {
                //some really common words are annotated as genes, other normal words are proteins.
                //Genes usually don't look like normal english words
                if(coveredText.matches("[A-Z]?[a-z]+") || coveredText.toLowerCase == "ii") {
                    false
                }
                else true
            } else if(coveredText.startsWith("level"))
                false
            else
                true
        }
        else false
    }

    //Start,End -> best annotation
    def selectAnnotations(utt:MMUtterance): Map[(Int,Int),List[MMCandidate]] = {
        val offset = utt.startPos
        val docLength = utt.length

        var groupedAnnotations = utt.phrases.flatMap(phrase => {
            if(phrase.mappings.isEmpty)
                List[((Int,Int),MMCandidate)]()
            else {
                val mapping = phrase.mappings.maxBy(-_.score)
                val mappings = phrase.mappings.filter(_.score == mapping.score)
                val allowedCuis = mappings.flatMap(_.cuis)
                phrase.candidates.filter(c => allowedCuis.contains(c.cui) && c.semtypes.exists(s => selectedSemtypes.contains(s))).flatMap(cand => {
                    cand.positions.map(pos => ((pos._1,pos._2), cand))
                })
            }
        }).groupBy(_._1).mapValues(_.map(_._2))

        //filter out overlapping annotations
        val filteredKeys = groupedAnnotations.keys.toList.sortBy(_._1).foldLeft(List[(Int,Int)]())((keys,key) => {
            if(keys.isEmpty)
                List(key)
            else {
                val last = keys.head
                if(last._1+last._2 < key._1)
                    key :: keys
                // if overlapping, take the one with the biggest length
                else if(key._2 <= last._2)
                    keys
                else
                    key :: keys.tail
            }
        })

        groupedAnnotations = groupedAnnotations.filter(ann => filteredKeys.contains(ann._1))

        // select annotations with highest (negative scores so actually lowest) scores with preference for preferred cuis
        groupedAnnotations.map {
            case ((start,length),annots) => {
                val newStart = start - offset
                val newEnd = newStart + length
                val bestAnnots =  annots.tail.foldLeft(List(annots.head))((bestPartAnnots,annot) => {
                    val max = bestPartAnnots.head.score
                    val current = annot.score
                    if(current < max)
                        List(annot)
                    else if(current > max)
                        bestPartAnnots
                    else
                        annot :: bestPartAnnots
                })

                if(newStart >= 0 && newEnd > newStart && newEnd < docLength && !bestAnnots.isEmpty)
                    ((newStart,newEnd), bestAnnots)
                else
                    ((newStart,newEnd), null)
            }
        }.filter(_._2 ne null)
    }

    def getDepPath(text: AnnotatedText, fromMention:OntologyEntityMention, toMention:OntologyEntityMention):Option[List[Token]] = {
        val startToken = fromMention.getTokens.minBy(_.depDepth)
        val endToken = toMention.getTokens.minBy(_.depDepth)

        if (startToken.sentence == endToken.sentence) {
            var token = startToken

            var path1 = List[Token](startToken)
            while (token.depDepth > 0 && token.depTag.dependsOn > 0 && token != endToken) {
                token = token.sentence.getTokens(token.depTag.dependsOn - 1)
                path1 ::= token
            }
            path1 = path1.reverse

            token = endToken
            var path2 = List[Token](endToken)
            while (token.depDepth > 0 && token.depTag.dependsOn > 0 && !path1.contains(token)) {
                token = token.sentence.getTokens(token.depTag.dependsOn - 1)
                path2 ::= token
            }

            if(path1.contains(token)) {
                //Combine paths and remove tokens which are part of the mentions
                val path = path1.takeWhile(t => t != token && !toMention.getTokens.contains(t)) ++ List(path2.head) ++ path2.tail.dropWhile(t => fromMention.getTokens.contains(t))
                Some(path)
            }
            else
                None
        }
        else
            None
    }

    private final val replaceSemtypes = Set("spco","phob","amas","blor","bsoj","crbs","geoa","mosq","nusq","acab","aapp","amph",
        "anab","anst","anim","antb","arch","bact","bacs","bodm","bird","bpoc","bdsu",
        "carb","celc","cell","chvf","chvs","chem","clnd","cgab","drdd","eico","elii",
        "emst","enzy","euka","fish","food","ffas","fngs","gngm","hops","horm","humn",
        "imft","irda","inch","lipd","mamm","mnob","medd","nsba","nnon","orch","orgm",
        "opco","phsu","plnt","rcpt","rept","resd","strd","sbst","tisu","vtbt","virs","vita","sosy","dsyn")

    def cleanPath(path: List[Token]):Option[List[(Token,DepTag)]] = {
        if(!path.isEmpty){
            var resultPath = path.map(t => (t,DepTag(t.depTag.tag,t.depTag.dependsOn)))

            //just consider paths in which at least one verbal form occurs
            if(resultPath.exists(_._1.posTag.matches(PosTag.ANYVERB_PATTERN))) {
                //replace tokens of spatial or physical mentions with there respective semantic type
               /* resultPath.head._1.context.getAnnotations[OntologyEntityMention].foreach(mention => {
                    val concept = mention.ontologyConcepts.head.asInstanceOf[UmlsConcept]
                    if(concept.semanticTypes.forall(selectedSemtypes.contains))
                        mention.getTokens.foreach(t => {
                            if(t.posTag.matches(PosTag.ANYNOUN_PATTERN))
                                t.lemma = concept.semanticTypes.head
                        })
                }) */

                //if passive, make it active
                if(resultPath.exists(_._2.tag == "nsubjpass")) {
                    val (_,depTag) = resultPath.find(_._2.tag == "nsubjpass").get
                    depTag.tag = "dobj"
                }

                if(resultPath.exists(_._2.tag == "agent")) {
                    val (byToken,byTag) = resultPath.find(_._2.tag == "agent").get
                    val passObjToken = resultPath.find(_._2.dependsOn == byToken.position)
                    val verbToken = resultPath.find(_._1.position == byTag.dependsOn)
                    if(verbToken.isDefined) {
                        resultPath.find{ case (t,depTag) => depTag.dependsOn == verbToken.get._1.position && depTag.tag == "nsubj" } match {
                            case Some((_,depTag)) => depTag.tag = "dobj"
                            case None =>
                        }
                    }
                    if(passObjToken.isDefined) {
                        val (_,passObjDepTag) = passObjToken.get
                        if(verbToken.isDefined) {
                            passObjDepTag.tag = "nsubj"
                            passObjDepTag.dependsOn = byTag.dependsOn
                        }
                        resultPath = resultPath.filter(_._1 != byToken)
                    }
                }

                //Clean conj and appos sequences
                val startToken = resultPath.head
                val endToken = resultPath.last

                def removeSequences(depLabel:String,tmpPath:List[(Token,DepTag)]): List[(Token,DepTag)] = {
                    tmpPath.tail.foldLeft(List[(Token,DepTag)](tmpPath.head)){
                        case (acc, (token,depTag)) => {
                            val (last,lastDepTag) = acc.head
                            if (depTag.tag == depLabel) {
                                if (token.depDepth > last.depDepth) {
                                    //downwards in tree
                                    depTag.tag = lastDepTag.tag
                                    depTag.dependsOn = lastDepTag.dependsOn
                                    (token,depTag) :: acc.tail
                                }
                                else //upwards
                                if (lastDepTag.tag == depLabel) //there is a deeper token of this depTag, so skip
                                    acc
                                else //deepest of token of this depTag
                                    (token,depTag) :: acc
                            } else if (lastDepTag.tag == depLabel) {
                                //can only happen upwards -> change depTag
                                lastDepTag.tag = depTag.tag
                                lastDepTag.dependsOn = depTag.dependsOn
                                acc
                            } else
                                (token,depTag) :: acc
                        }
                    }.reverse
                }

                resultPath = removeSequences("conj",resultPath)
                resultPath = removeSequences("appos",resultPath)

                //filter
                resultPath = resultPath.filterNot(_._2.tag.matches("""punct|hyph"""))

                if(resultPath.isEmpty || resultPath.head != startToken || resultPath.last != endToken ||
                    !resultPath.exists(_._1.posTag.matches(PosTag.ANYVERB_PATTERN)) || resultPath.size > 6)
                    None
                else
                    Some(resultPath)
            }
            else
                None
        }
        else None
    }

    def printPath(path: List[(Token,DepTag)]) = {
        def printToken(token:Token, deptag:DepTag) = {
            var result = token.lemma
            if(token.posTag.matches(PosTag.ANYVERB_PATTERN)) {
                result += ":VB"
                var auxTokens = token.sentence.getTokens.filter(t => t.depTag.dependsOn == token.position && t.depTag.tag.matches("neg|acomp")&& !path.exists(_._1 == t))
                auxTokens = auxTokens.filterNot(path.contains).filter(_.posTag.matches(PosTag.ANYADJECTIVE_PATTERN+"|"+PosTag.ANYVERB_PATTERN+"|"+PosTag.ANYNOUN_PATTERN))
                if(auxTokens.size > 0)
                    result += "("+auxTokens.map(_.lemma).mkString(" ")+")"
            }
            if(token.posTag.matches(PosTag.ANYNOUN_PATTERN)) {
                result += ":" + deptag.tag
                val auxTokens = token.sentence.getTokens
                    .filter(t => t.depTag.dependsOn == token.position && !replaceSemtypes.contains(t.lemma) && t.depTag.tag.matches("amod|nn|neg") && !path.exists(_._1 == t))
                    .filter(_.posTag.matches(PosTag.ANYADJECTIVE_PATTERN+"|"+PosTag.ANYVERB_PATTERN+"|"+PosTag.ANYNOUN_PATTERN))
                if(auxTokens.size > 0)
                    result += "("+auxTokens.map(_.lemma).mkString(" ")+")"
            }
            result
        }
        val startToken = path.head

        val endToken = path.last

        var result = startToken._2.tag
        var last = startToken._1
        path.tail.dropRight(1).foreach(t => {
            if (last.depDepth < t._1.depDepth)
                result += " <- "
            else
                result += " -> "

            result += printToken(t._1,t._2)
            last = t._1
        })
        if (last.depDepth < endToken._1.depDepth)
            result += " <- "
        else
            result += " -> "
        result += endToken._2.tag

        result
    }
}
