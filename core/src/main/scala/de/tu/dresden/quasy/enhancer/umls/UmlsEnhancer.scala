package de.tu.dresden.quasy.enhancer.umls

import gov.nih.nlm.nls.skr.GenericObject
import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{SemanticRelationAnnotation, OntologyEntityMention, UmlsConcept}
import de.tu.dresden.quasy.util.Xmlizer
import java.io._
import org.apache.http.entity.mime.content.FileBody
import java.nio.charset.Charset
import org.apache.commons.logging.LogFactory
import java.text.Normalizer

/**
 * @author dirk
 * Date: 4/15/13
 * Time: 10:41 AM
 */
object UmlsEnhancer extends TextEnhancer {
    private final val LOG = LogFactory.getLog(getClass)

    /*
00000000|MM|68.24|Opioids|C0242402|[hops,orch,phsu]|["Opioid"-tx-3-"opioid","Opioid"-tx-2-"opioid","Opioid"-tx-1-"opioid"]|TX|286:6|185:6|84:6|D27.505.696.663.850.014.520;D27.505.954.427.040.325;D27.505.954.427.210.049
00000000|MM|20.83|Patients|C0030705|[podg]|["Patients"-tx-3-"patients","Patients"-tx-2-"patients","Patients"-tx-1-"patients"]|TX|256:8|155:8|54:8|M01.643
00000000|MM|15.79|Having administered|C1521801|[ftcn]|["Administered"-tx-3-"administered","Administered"-tx-2-"administered","Administered"-tx-1-"administered"]|TX|229:12|128:12|27:12|
00000000|MM|15.79|Pharmaceutical Preparations|C0013227|[phsu]|["Medication, NOS"-tx-3-"medication","Medication, NOS"-tx-2-"medication","Medication, NOS"-tx-1-"medication"]|TX|208:10|107:10|6:10|D26
00000000|MM|15.79|medication - HL7 publishing domain|C3244316|[ftcn]|["medication"-tx-3-"medication","medication"-tx-2-"medication","medication"-tx-1-"medication"]|TX|208:10|107:10|6:10|
00000000|MM|11.29|Acute overdose|C1963951|[inpo]|["Acute overdose"-tx-3-"acute overdose","Acute overdose"-tx-2-"acute overdose","Acute overdose"-tx-1-"acute overdose"]|TX|280:5,293:8|179:5,192:8|78:5,91:8|
00000000|MM|10.67|Management procedure|C1273870|[ocac]|["Managing"-tx-3-"managing","Managing"-tx-2-"managing","Managing"-tx-1-"managing"]|TX|247:8|146:8|45:8|
00000000|MM|10.56|Suspected diagnosis|C0332147|[qlco]|["Suspected"-tx-3-"suspected","Suspected"-tx-2-"suspected","Suspected"-tx-1-"suspected"]|TX|270:9|169:9|68:9|
00000000|MM|10.56|Suspected qualifier|C0750491|[idcn]|["Suspected"-tx-3-"suspected","Suspected"-tx-2-"suspected","Suspected"-tx-1-"suspected"]|TX|270:9|169:9|68:9|
     */

    def enhance(text: AnnotatedText) {
        val asciiText =
            Normalizer.normalize(text.text, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")

        val maxLength = 9900
        var last = asciiText
        var first = ""
        var texts = List[String]()
        while (last.size > maxLength) {
            val (newfirst,newlast) = last.splitAt(asciiText.indexOf(" ",maxLength))
            last = newlast.trim
            first = newfirst.trim
            texts ::= first
        }
        texts ::= last
        texts = texts.reverse
        var offset = 0

        texts.foreach(asciiText => {
            val resultStr = requestSemRep(asciiText+"\n")
            var NEs = List[OntologyEntityMention]()
            resultStr.split("""\n""").foreach( line => {
                val split = line.split("""\|""")

                if (split.size > 8 && split(1)=="MM") {
                    val cui = split(4)
                    val tuis = split(5).substring(1, split(5).length-1)
                    val spans = split.drop(8).takeWhile(_.matches("([0-9]+:[0-9]+,?)+")).flatMap(_.split(",")).map(spanStr => {
                        val Array(begin,length) = spanStr.split(":",2)
                        (begin.toInt,length.toInt)
                    })

                    val treeNrs = if(split.last.matches("[A-Z][0-9.]*")) split.last.split(";") else Array[String]()
                        val concept = new UmlsConcept(cui, split(3),Set[String](),tuis.split(","),treeNrs,split(2).toDouble)
                        try {
                            spans.foreach {
                                case (begin,length) => NEs ::= new OntologyEntityMention(Array(new Span(offset+begin,offset+begin+length)), text, List(concept))
                            }
                        }
                        catch {
                            case e =>
                        }
                }
                /*if (split.size > 5 && split(5).equals("text")) {
                    offset = asciiText.indexOf(split(6),offset+1)
                }

                if (split.size > 5 && split(5).equals("entity")) {
                    val cui = split(6)
                    val tuis = split(8)
                    val begin = split(16)
                    val end = split(17)
                    val concept = new UmlsConcept(cui, tuis.split(","))
                    try {
                        NEs ::= new OntologyEntityMention(Array(new Span(begin.toInt-1+offset,end.toInt+offset)), text, List(concept))
                    }
                    catch {
                        case e =>
                    }
                } else if (split.size > 5 && split(5).equals("relation"))  {
                    val beginSubj = split(19).toInt
                    val endSubj = split(20).toInt +1

                    val beginObj = split(39).toInt
                    val endObj = split(40).toInt +1

                    val predicate = split(22)

                    new SemanticRelationAnnotation(
                        predicate,
                        NEs.find(_.spans.head.equals(new Span(beginSubj,endSubj))).get,
                        NEs.find(_.spans.head.equals(new Span(beginObj,endObj))).get)
                } */
            } )
            offset += asciiText.length+1
        })
    }

    def requestSemRep(text:String):String = {
        var results = ""

        //myGenericObj.setField("Batch_Command", "semrep -D")
        //myGenericObj.setField("SilentEmail", true)
        //myGenericObj.setFileField("UpLoad_File", filename)
        try {
            val username = System.getProperty("umlsUsername")
            val password = System.getProperty("umlsPassword")
            val email = System.getProperty("umlsEmail")
            //val myGenericObj = new GenericObject(200,username, password)
            val myGenericObj = if(text.size > 10000) new GenericObject(username, password) else new GenericObject(100,username, password)

            //TODO read from properties
            myGenericObj.setField("Email_Address", email)

            if(text.size > 10000) {
                val pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream("./batch.txt"),"US-ASCII"))
                pw.print(text)
                pw.close()
                myGenericObj.setField("SilentEmail", true)
                myGenericObj.setFileField("UpLoad_File", "./batch.txt")
                myGenericObj.setField("Batch_Command", "metamap -AlN -J acab,amas,aapp,anab,anst,antb,biof,bacs,blor,bpoc,carb,crbs,cell,celc,celf,comd,chem,chvf,chvs,clna,clnd,cgab,diap,dsyn,drdd,eico,elii,emst,enzy,food,ffas,fngs,gngp,gngm,genf,geoa,hops,horm,inpo,inch,lipd,menp,mobd,moft,mosq,neop,nsba,nnon,nusq,ortf,orch,opco,phsu,rcpt,sosy,strd,sbst,virs,vita --prune 10 -V Base")
            }
            else {
                myGenericObj.setField("APIText", text)
                myGenericObj.setField("KSOURCE", "1213")
                myGenericObj.setField("COMMAND_ARGS", """-AlN -J acab,amas,aapp,anab,anst,antb,biof,bacs,blor,bpoc,carb,crbs,cell,celc,celf,comd,chem,chvf,chvs,clna,clnd,cgab,diap,dsyn,drdd,eico,elii,emst,enzy,food,ffas,fngs,gngp,gngm,genf,geoa,hops,horm,inpo,inch,lipd,menp,mobd,moft,mosq,neop,nsba,nnon,nusq,ortf,orch,opco,phsu,rcpt,sosy,strd,sbst,virs,vita --prune 10 -V Base""")
            }

            // Submit the job request
            try {

                results = myGenericObj.handleSubmission
                //println(results)
            }
            catch {
                case ex: RuntimeException => {
                    ex.printStackTrace
                }
            }

            if(text.size > 10000)
                new File("./batch.txt").delete()
        }
        catch {
            case e:NullPointerException => LOG.error(
                """When using UmlsEnhancer you have to specify a umlsUsername and umlsPassword by
                  | using JVM arguments -DumlsUsername and -DumlsPassword
                """.stripMargin)
        }

        results
    }

    def main(args:Array[String]) {

        var text = new AnnotatedText("Screening for psychological risk factors is an important first step in safeguarding against nonadherence practices and identifying patients who may be vulnerable to the risks associated with opioid therapy.")

        enhance(text)

        val xml = Xmlizer.toXml(text)

        text = Xmlizer.fromXml(xml)

        println(xml)
    }
}


