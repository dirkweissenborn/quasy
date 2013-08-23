package de.tu.dresden.quasy.model.db

import io.Source
import scala.Predef._

/**
 * @author dirk
 * Date: 6/6/13
 * Time: 2:38 PM
 */
object UmlsSemanticNetwork {

    private val abbreviations =
        Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("umls/SemanticTypeMappings_2011AA.txt")).
            getLines().map(line => line.split("\\|")).map(a => (a(1),a(0))).toMap

    private val semanticTypes = abbreviations.map(e => (e._2,e._1)).toMap


    private val relations =
        Source.fromInputStream(getClass.getClassLoader.getResourceAsStream("umls/SRSTRE2.txt")).
        getLines().map(line => {
            val Array(subj,pred,obj,"") = line.split("\\|",4)
            (getAbbreviation(subj),pred,getAbbreviation(obj))
        }).toList


    def getAbbreviation(semanticType:String) = abbreviations.getOrElse(semanticType,semanticType)
    def getFullname(abbreviation:String) = semanticTypes.getOrElse(abbreviation,abbreviation)

    def ?(subj:String,pred:String,obj:String) = relations.filter{
        case(relSubj,relPred,relObj) => {
            (subj == "?" || subj == "" || relSubj.matches(subj)) &&
            (obj  == "?" || obj  == "" || relObj.matches(obj))  &&
            (pred == "?" || pred == "" || relPred.matches(pred))
        }
    }

    def isa(subj:String,obj:String):Boolean = {
        subj == obj ||
        ?(subj,"isa",obj).size > 0
    }

    def main(args:Array[String]) {
        println("\""+getFullname("bpoc")+"\"")
        println(?("?","isa",getAbbreviation("Pathologic Function")).map(b => "\""+b._1+"\"").mkString(","))
        //println(?("?","isa",getAbbreviation("Physical Object")).map(b => "\""+b._1+"\"").mkString(","))

        //println(?("?", "part_of", getAbbreviation("Cell")).map(b => getFullname(b._1)).mkString(","))
    }
}
