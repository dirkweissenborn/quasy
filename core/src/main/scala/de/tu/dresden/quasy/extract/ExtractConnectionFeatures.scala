package de.tu.dresden.quasy.extract

import java.io.{PrintWriter, FileWriter, FileInputStream, File}
import org.apache.lucene.queryparser.classic.QueryParser
import org.apache.lucene.util.Version
import org.apache.lucene.analysis.core.KeywordAnalyzer
import de.tu.dresden.quasy.model.db.LuceneIndex
import org.apache.lucene.search._
import org.apache.lucene.index.Term
import org.apache.lucene.search.join.{ToParentBlockJoinCollector, ScoreMode, ToParentBlockJoinQuery}
import de.tu.dresden.quasy.model.{PosTag, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{DepTag, Token, UmlsConcept, OntologyEntityMention}
import scala.io.Source
import scala.Some
import org.apache.lucene.search.BooleanClause.Occur
import java.util.Properties
import de.tu.dresden.quasy.enhancer.clearnlp.FullClearNlpPipeline
import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.cluster.MedoidEM
import cc.mallet.types.StringKernel


/**
 * @author dirk
 *          Date: 7/30/13
 *          Time: 1:01 PM
 */
object ExtractConnectionFeatures {

    def main(args:Array[String]) {
        val cui1 = args(0)
        val cui2 = args(1)

        val indirection = args(2)

        val indexDir1 = new File(args(3))
        val index1 = new LuceneIndex(indexDir1, new KeywordAnalyzer)
        val indexDir2 = new File(args(4))
        val index2 = new LuceneIndex(indexDir2, new KeywordAnalyzer)

        val outputDir = new File(args(6),cui1+"-"+indirection+"-"+cui2)
        outputDir.mkdirs()
        val diseaseWriter = new PrintWriter(new FileWriter(new File(outputDir,"disease-"+indirection)))
        val drugWriter = new PrintWriter(new FileWriter(new File(outputDir,"drugs-"+indirection)))

        println("Scoring candidates...")
        val frequenciesFile1 = indexDir1.listFiles().find(_.getName == ExtractionUtil.FREQUENCIES_FILENAME).get
        val frequenciesFile2 = indexDir2.listFiles().find(_.getName == ExtractionUtil.FREQUENCIES_FILENAME).get

        val frequencyLines1 = Source.fromFile(frequenciesFile1).getLines()
        val frequencyLines2 = Source.fromFile(frequenciesFile2).getLines()

        val totalSemtype = frequencyLines1.next().split("\t")(1).toDouble
        var cuiUttFreqs = Map[String,Int]()

        //only consider common cuis
        frequencyLines1.foreach(entry => {
            val Array(cui,freq) = entry.split("\t",2)
            cuiUttFreqs += (cui -> freq.toInt)
        })
        frequencyLines2.next()
        cuiUttFreqs = frequencyLines2.map(entry => {
            val Array(cui,freq) = entry.split("\t",2)
            if(cuiUttFreqs.contains(cui))
                Some(cui -> freq.toInt)
            else
                None
        }).flatten.toMap


        if(cuiUttFreqs.size > 0) {
            var scores = cuiUttFreqs.map {
                case (cui,uttFreq) => {
                    val toCuiTerm = new Term(ExtractionUtil.TOCUI, cui)
                    val freq = math.min(index1.reader.docFreq(toCuiTerm),index2.reader.docFreq(toCuiTerm))+1
                    (cui, math.log(freq+1) * math.log(totalSemtype/cuiUttFreqs(cui)))
                }
            }.toArray

            //cut off at point with highest 2nd derivation
            scores = scores.sortBy(-_._2).take(100)

            //scores.foreach(s => println(s._1+"\t"+s._2))
            val cutOff =
                (0 until scores.length-1).map(idx => {
                    if(idx == 0)
                        (idx,scores(idx)._2 - scores(idx+1)._2)
                    else
                        (idx, 2*scores(idx)._2 - scores(idx+1)._2 -scores(idx-1)._2)
                }).maxBy(_._2)._1 +1


            val selectCuis = scores.take(cutOff).map(_._1).toSet

            println("Selected Cuis: "+selectCuis.mkString(", "))

            val conf = new Properties()
            conf.load(new FileInputStream(new File(args(5))))
            val depParser = FullClearNlpPipeline.fromConfiguration(conf)

            println("Querying indexes...")
            val hits1 = query(index1,selectCuis,depParser)
            val hits2 = query(index2, selectCuis,depParser)

            println("Writing results...")
            println("Writing "+cui1+"-"+indirection)
            var map1 = Map[String,Int]()
            hits1.foreach {
                case (cui,annotatedTexts) => {
                    annotatedTexts.foreach(text => {
                        if(text.getAnnotations[OntologyEntityMention].find(_.ontologyConcepts.head.conceptId == cui2).isEmpty) {
                            val paths = getDepPath(text,cui1,cui).map(cleanPath).filterNot(_.isEmpty)
                            if(!paths.isEmpty) {
                                val cleanedPath = paths.minBy(_.size)
                                if(cleanedPath.size > 2 && cleanedPath.size < 7) {
                                    var pathAsString = printPath(cleanedPath, cui1, cui)
                                    println(text.text)
                                    println(pathAsString)
                                    println()
                                    pathAsString = pathAsString.substring(pathAsString.indexOf(":"),pathAsString.lastIndexOf(cui))
                                    diseaseWriter.println(pathAsString)
                                    val count = map1.getOrElse(pathAsString,0)
                                    map1 += (pathAsString -> (count+1))
                                }
                            }
                        }
                    })
                }
            }
            map1.toSeq.sortBy(-_._2).takeWhile(_._2 > 1).foreach(path => println(path._2+"\t"+path._1))
            println()
            println("Writing "+cui2+"-"+indirection)
            var map2 = Map[String,Int]()
            hits2.foreach {
                case (cui,annotatedTexts) => {
                    annotatedTexts.foreach(text => {
                        if(text.getAnnotations[OntologyEntityMention].find(_.ontologyConcepts.head.conceptId == cui1).isEmpty) {
                            val paths = getDepPath(text,cui2,cui).map(cleanPath).filterNot(_.isEmpty)
                            if(!paths.isEmpty) {
                                val cleanedPath = paths.minBy(_.size)
                                if(cleanedPath.size > 2) {
                                    var pathAsString = printPath(cleanedPath, cui2, cui)
                                    println(text.text)
                                    println(pathAsString)
                                    println()
                                    pathAsString =pathAsString.substring(pathAsString.indexOf(":"),pathAsString.lastIndexOf(cui))
                                    drugWriter.println(pathAsString)
                                    val count = map2.getOrElse(pathAsString,0)

                                    map2 += (pathAsString -> (count+1))
                                }
                            }
                        }
                    })
                }
            }
            map2.toSeq.sortBy(-_._2).takeWhile(_._2 > 1).foreach(path => println(path._2+"\t"+path._1))
            drugWriter.close()
            diseaseWriter.close()
            println(cui1+"-"+indirection+"-"+cui2+" finished")

            print("Average path length: ")
            val jointMap = map1.toSeq ++ map2.toSeq
            var total = 0.0
            val p = """(->|<-)""".r
            val avLength = jointMap.map{
                case (path,count) => {
                    total += count
                    count * p.findAllIn(path).size+count
                }
            }.sum / total
            println(avLength)

            print("Below average: ")
            println(jointMap.map{
                case (path,count) => {
                    if(count * p.findAllIn(path).size+count <= avLength)
                        count
                    else 0
                }
            }.sum / total)

            println()

            val sk = new StringKernel()
            val clusteringAlg = new MedoidEM[String]((s,k) => 1- math.sqrt(sk.K(s,k)))

            println("Clusters "+cui1)
            val clusterAssignments = clusteringAlg.cluster(map1.keys.toList,20)
            clusterAssignments.foreach {
                case (cluster,assignments) => {
                    println("###########Cluster#############")
                    assignments.sortBy(-_._2).foreach(a => println(a._1))
            }}

            println("Clusters "+cui2)
            val clusterAssignments2 = clusteringAlg.cluster(map2.keys.toList,20)
            clusterAssignments2.foreach {
                case (cluster,assignments) => {
                    println("###########Cluster#############")
                    assignments.sortBy(-_._2).foreach(a => println(a._1))
                }}
        }

    }

    private final val replaceSemtypes = Set("spco","phob","amas","blor","bsoj","crbs","geoa","mosq","nusq","acab","aapp","amph","anab","anst","anim","antb","arch","bact","bacs","bodm","bird","bpoc","bdsu","carb","celc","cell","chvf","chvs","chem","clnd","cgab","drdd","eico","elii","emst","enzy","euka","fish","food","ffas","fngs","gngm","hops","horm","humn","imft","irda","inch","lipd","mamm","mnob","medd","nsba","nnon","orch","orgm","opco","phsu","plnt","rcpt","rept","resd","strd","sbst","tisu","vtbt","virs","vita")

    def cleanPath(path: List[Token]) = {
        var resultPath = path

        //just consider paths in which at least one verbal form occurs
        if(resultPath.tail.dropRight(1).exists(_.posTag.matches(PosTag.ANYVERB_PATTERN))) {
            //replace tokens of spatial or physical mentions with there respective semantic type
            path.head.context.getAnnotations[OntologyEntityMention].foreach(mention => {
                val concept = mention.ontologyConcepts.head.asInstanceOf[UmlsConcept]
                if(concept.semanticTypes.forall(replaceSemtypes.contains))
                    mention.getTokens.foreach(t => {
                        if(t.posTag.matches(PosTag.ANYNOUN_PATTERN))
                        t.lemma = concept.semanticTypes.head
                    })
            })

            //if passive, make it active
            if(resultPath.exists(_.depTag.tag == "nsubjpass")) {
                val passToken = resultPath.find(_.depTag.tag == "nsubjpass").get
                passToken.depTag = DepTag("dobj",passToken.depTag.dependsOn)
            }

            if(resultPath.exists(_.depTag.tag == "agent")) {
                val byToken = resultPath.find(_.depTag.tag == "agent").get
                val passObjToken = resultPath.find(_.depTag.dependsOn == byToken.position)
                val verbToken = resultPath.find(_.position == byToken.depTag.dependsOn)
                if(verbToken.isDefined) {
                    resultPath.find(t => t.depTag.dependsOn == verbToken.get.position && t.depTag.tag == "nsubj") match {
                        case Some(token) => token.depTag = DepTag("dobj",token.depTag.dependsOn)
                        case None =>
                    }
                }
                if(passObjToken.isDefined) {
                    if(verbToken.isDefined)
                    passObjToken.get.depTag = DepTag("nsubj",byToken.depTag.dependsOn)
                    resultPath = resultPath.filter(_ != byToken)
                }
            }

            //Clean conj and appos sequences
            val startToken = resultPath.head
            val endToken = resultPath.last

            def removeSequences(deptag:String,tmpPath:List[Token]): List[Token] = {
                tmpPath.tail.foldLeft(List[Token](tmpPath.head))((acc, token) => {
                    val last = acc.head
                    if (token.depTag.tag == deptag) {
                        if (token.depDepth > last.depDepth) {
                            //downwards in tree
                            token.depTag = last.depTag
                            token :: acc.tail
                        }
                        else //upwards
                        if (last.depTag.tag == deptag) //there is a deeper token of this depTag, so skip
                            acc
                        else //deepest of token of this depTag
                            token :: acc
                    } else if (last.depTag.tag == deptag) {
                        //can only happen upwards -> change depTag
                        last.depTag = token.depTag
                        acc
                    } else
                        token :: acc
                }).reverse
            }

            resultPath = removeSequences("conj",resultPath)
            resultPath = removeSequences("appos",resultPath)

            resultPath = resultPath.filterNot(_.depTag.tag.matches("""punct|hyph|num"""))

            if(resultPath.head != startToken || resultPath.last != endToken ||
               !resultPath.tail.dropRight(1).exists(_.posTag.matches(PosTag.ANYVERB_PATTERN)))
                List[Token]()
            else
                resultPath
        }
        else
            List[Token]()
    }

    def printPath(path: List[Token], fromCui:String, toCui:String) = {
        def printToken(token:Token) = {
            var result = token.lemma
            if(token.posTag.matches(PosTag.ANYVERB_PATTERN)) {
                result += ":VB"
                var auxTokens = token.sentence.getTokens.filter(t => t.depTag.dependsOn == token.position && t.depTag.tag.matches("neg|acomp"))
                auxTokens = auxTokens.filterNot(path.contains)
                if(auxTokens.size > 0)
                    result += "("+auxTokens.map(_.lemma).mkString(" ")+")"
            }
            if(token.posTag.matches(PosTag.ANYNOUN_PATTERN)) {
                result += ":" + token.depTag.tag
                var auxTokens = token.sentence.getTokens.filter(t => t.depTag.dependsOn == token.position && t.depTag.tag.matches("amod|nn|neg"))
                auxTokens = auxTokens.filterNot(path.contains)
                if(auxTokens.size > 0)
                    result += "("+auxTokens.map(_.lemma).mkString(" ")+")"
            }
            result
        }
        val startToken = path.head
        val startMention = startToken.context.getAnnotations[OntologyEntityMention].find(m => m.getTokens.contains(startToken) && m.ontologyConcepts.head.conceptId == fromCui).get

        val endToken = path.last
        val endMention = endToken.context.getAnnotations[OntologyEntityMention].find(m => m.getTokens.contains(endToken)&& m.ontologyConcepts.head.conceptId == toCui).get

        var result = startMention.ontologyConcepts.head.asInstanceOf[UmlsConcept].semanticTypes.head + "(" + startMention.coveredText + "):" + startToken.depTag.tag
        var last = startToken
        path.tail.dropRight(1).foreach(t => {
            if (last.depDepth < t.depDepth)
                result += " <- "
            else
                result += " -> "

            result += printToken(t)
            last = t
        })
        if (last.depDepth < endToken.depDepth)
            result += " <- "
        else
            result += " -> "
        result += toCui + "(" + endMention.coveredText + "):" + endToken.depTag.tag

        result
    }

    def getDepPath(text: AnnotatedText, fromCui: String, toCui: String):List[List[Token]] = {
        val startMentions = text.getAnnotations[OntologyEntityMention].filter(_.ontologyConcepts.head.conceptId == fromCui)
        val endMentions = text.getAnnotations[OntologyEntityMention].filter(_.ontologyConcepts.head.conceptId == toCui)

        startMentions.flatMap(startMention =>
            endMentions.map(endMention => {
                val startToken = startMention.getTokens.minBy(_.depDepth)

                val endToken = endMention.getTokens.minBy(_.depDepth)

                if (startToken.sentence == endToken.sentence) {
                    var token = startToken

                    var path1 = List[Token](startToken)
                    while (token.depDepth > 0 && token != endToken) {
                        token = token.sentence.getTokens(token.depTag.dependsOn - 1)
                        path1 ::= token
                    }
                    path1 = path1.reverse

                    token = endToken
                    var path2 = List[Token](endToken)
                    while (token.depDepth > 0 && !path1.contains(token)) {
                        token = token.sentence.getTokens(token.depTag.dependsOn - 1)
                        path2 ::= token
                    }

                    if(path1.contains(token)) {
                        //Combine paths and remove tokens which are part of the mentions
                        val path = path1.takeWhile(t => t != token && !endMention.getTokens.contains(t)) ++ List(path2.head) ++ path2.tail.dropWhile(t => startMention.getTokens.contains(t))
                        Some(path)
                    }
                    else
                        None
                }
                else
                    None
            }).flatten
        )
    }

    // CUI -> List(annotated utterances)
    def query(index:LuceneIndex, allowedCuis:Set[String], depParser:TextEnhancer):Map[String,List[AnnotatedText]] = {
        val parentFilter = new CachingWrapperFilter(
            new QueryWrapperFilter(
                new TermQuery(new Term(ExtractionUtil.TYP, "utterance"))))

        val joinQueryCand = new ToParentBlockJoinQuery(
            new TermQuery(new Term(ExtractionUtil.TYP, "candidate")),
            parentFilter,
            ScoreMode.None)

        val query = new BooleanQuery()
        if(allowedCuis != null) {
            val utteranceQuery =
                new QueryParser(Version.LUCENE_40,"",new KeywordAnalyzer).parse(allowedCuis.map(c => ExtractionUtil.TOCUI+":"+c).mkString(" "))
            query.add(utteranceQuery, Occur.MUST)
        }
        query.add(joinQueryCand, Occur.MUST)

        val c = new ToParentBlockJoinCollector(
            Sort.INDEXORDER, // sort
            100000, // numHits
            false, // trackScores
            false // trackMaxScore
        )

        index.searcher.search(query, c)

        val topGroups = c.getTopGroups(
            joinQueryCand,
            Sort.INDEXORDER,
            0, // offset
            100, // maxDocsPerGroup
            0, // withinGroupOffset
            false // fillSortFields
        )

        if (topGroups != null) {
            topGroups.groups.foldLeft(Map[String,List[AnnotatedText]]())( (acc,g) => {
                val parent = index.searcher.doc(g.groupValue)
                val text = parent.get(ExtractionUtil.TEXT).replace("\"\"","\"") + "."

                val id = parent.get(ExtractionUtil.PMID) + "." +
                    parent.get(ExtractionUtil.SECTION) + "." +
                    parent.get(ExtractionUtil.NUMBER)
                val toCui = parent.get(ExtractionUtil.TOCUI)

                val list = acc.getOrElse(toCui,List[AnnotatedText]())

                //Eliminate duplicates
                if(!list.exists(text => text.id == id)) {
                    val annotatedText = new AnnotatedText(id,text)
                    var offset = 0

                    val annotations = g.scoreDocs.map(ind => {
                        val doc = index.searcher.doc(ind.doc)
                        (doc.get(ExtractionUtil.START).toInt,doc.get(ExtractionUtil.END).toInt) ->
                            (doc.get(ExtractionUtil.CUI),doc.getValues(ExtractionUtil.SEMTYPE))
                    })
                    //Eliminate wrong annotation positions
                    if(!annotations.exists(s => s._1._1 < 0 || s._1._2 > text.length))  {
                        depParser.enhance(annotatedText)
                        annotations.sortBy(_._1._1).foreach {
                            case ((start,end),(cui,semtypes)) => {
                                val entity = AnnotatedText.cleanText(text.substring(start,end))
                                val newStart = annotatedText.text.indexOf(entity,offset)
                                offset = newStart+1
                                new OntologyEntityMention(
                                    newStart,
                                    newStart+entity.length,
                                    annotatedText,
                                    List(new UmlsConcept(cui,entity,Set[String](),semtypes.toSet)))
                            }
                        }
                        acc + ( toCui -> (annotatedText :: list))
                    } else
                        acc
                }
                else acc
            })
        }
        else {
            Map[String,List[AnnotatedText]]()
        }
    }

}
