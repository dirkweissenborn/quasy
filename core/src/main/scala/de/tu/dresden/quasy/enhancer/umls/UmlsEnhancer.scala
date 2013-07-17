package de.tu.dresden.quasy.enhancer.umls

import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, Token, UmlsConcept}
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.enhancer.TextEnhancer
import scala.sys.process._

/**
 * @author dirk
 *          Date: 6/25/13
 *          Time: 3:00 PM
 */
object UmlsEnhancer extends TextEnhancer {
    private final val pathToMM = "/home/dirk/workspace/public_mm/bin/metamap12"

    private final val allowedSTypes = "acab,amas,aapp,anab,anst,antb,biof,bacs,blor,bpoc,carb,crbs,cell,celc,celf,comd,chem,chvf,chvs,clnd,cgab,diap,dsyn,drdd,eico,elii,emst,enzy,food,ffas,fngs,gngp,gngm,genf,hops,horm,inpo,hlca,inch,imft,lipd,mobd,moft,mosq,neop,nsba,nnon,nusq,ortf,orch,opco,phsu,rcpt,sosy,strd,sbst,virs,vita".split(",")
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

    protected def pEnhance(text: AnnotatedText) {
        val asciiText = text.text

        val maxLength = 5000
        var last:String = asciiText
        var texts = List[String]()
        while (last.length > maxLength) {
            val (first,newLast) = last.splitAt(math.max(last.indexOf(". ",maxLength),maxLength))
            last = newLast
            texts ::= first
        }
        texts ::= last
        texts = texts.reverse
        var offset = 0

        texts.foreach(asciiText => {
            val whiteSpaces = asciiText.takeWhile(_.equals(' ')).size
            offset += whiteSpaces
            val resultStr = requestSemRep(asciiText.trim)
            //var NEs = List[OntologyEntityMention]()
            resultStr.split("""\n""").foreach( line => {
                val split = line.split("""\|""")

                if (split.size > 8 && split(1)=="MM") {
                    val cui = split(4)
                    val tuis = split(5).substring(1, split(5).length-1).split(",").filter(allowedSTypes.contains).toSet
                    if(tuis.size > 0) {
                        val treeNrs = if(split.last.matches("[A-Z][0-9.]*")) split.last.split(";").toSet else Set[String]()
                        val concept = new UmlsConcept(cui, split(3),Set[String](),tuis,treeNrs,split(2).toDouble)
                        try {
                            split.drop(8).takeWhile(_.matches("([0-9]+:[0-9]+,?)+")).map(spanStr => spanStr.split(",")).foreach(spanStrs =>{
                                val spans = spanStrs.map(spanStr => {
                                    val Array(beginS,lengthS) = spanStr.split(":",2)
                                    val begin = beginS.toInt
                                    val end = begin + lengthS.toInt
                                    val beginToken = text.getAnnotations[Token].find(_.contains(offset+begin,offset+begin)).get
                                    val endToken = text.getAnnotations[Token].find(_.contains(offset+end-1,offset+end-1)).get

                                    new Span(beginToken.begin,endToken.end)
                                })
                                //NEs ::=
                                new OntologyEntityMention(spans, text, List(concept))
                            })
                        }
                        catch {
                            case e =>
                        }
                    }
                }
            } )
            offset += asciiText.length - whiteSpaces
        })
    }

    def requestSemRep(text:String):String =
        (("echo \""+text+"\"") #| ("sh "+pathToMM+" -AlN")).!!

    def main(args:Array[String]) {
        var text = new AnnotatedText("Screening for psychological risk factors is an important first step in safeguarding against nonadherence practices and identifying patients who may be vulnerable to the risks associated with opioid therapy.")

        enhance(text)

        val xml = Xmlizer.toXml(text)

        text = Xmlizer.fromXml(xml)

        println(xml)
    }

}
