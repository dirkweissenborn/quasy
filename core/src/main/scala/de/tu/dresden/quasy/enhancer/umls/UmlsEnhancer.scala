package de.tu.dresden.quasy.enhancer.umls

import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, Token, UmlsConcept}
import de.tu.dresden.quasy.util.Xmlizer
import de.tu.dresden.quasy.enhancer.TextEnhancer
import scala.sys.process._
import scala.actors.{Actor, Futures}
import org.apache.commons.logging.LogFactory
import java.io.{InputStreamReader, BufferedReader}

/**
 * @author dirk
 *          Date: 6/25/13
 *          Time: 3:00 PM
 */
object UmlsEnhancer extends TextEnhancer {
    private val LOG = LogFactory.getLog(getClass)

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
            val (first,newLast) = last.splitAt(math.max(last.indexOf(". ",maxLength)+1,maxLength))
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

    import scala.sys.process._

    def requestSemRep(text:String):String = {
        //val commands = Array("sh","-c","echo "+"\""+text+"\" | "+pathToMM+" -AlN")
        //var proc:java.lang.Process = null

        //Array("sh","-c","echo "+"\""+text+"\" | "+pathToMM+" -AlN")

        val result = (("echo "+text.replaceAll("\"","'").replaceAll("\n"," ")) #| (pathToMM + " -AlN")).!!

        result
    }

    def main(args:Array[String]) {
        var text = new AnnotatedText("Similarly to antiviral vaccines now used to prevent cervical cancer (anti-HPV vaccine) or hepatocellular carcinoma (anti-hepatitis B vaccine) preventive vaccines against usually non-expressed retroviral antigens may stimulate long lasting CD8+ T lymphocytic response in an otherwise vulnerable host that could then become able to eradicate early malignancies expressing these retroviral antigens [58]. Nearly 85% of malignant melanocytes express an antigen called HERV-K-MEL, a product of a pseudo-gene incorporated in the HERV-K env gene [25,80,81]. The HERV-K-MEL antigen, already previously defined as a marker of melanoma risk, is not present in normal tissues, but is significantly expressed in the majority of dysplastic and normal naevi, as well as other tumors like sarcoma, lymphoma, bladder, breast and ovarian cancer [25]. The FEBrile Infections and Melanoma (FEBIM) multicentre case&#x2013;control study provided evidence how the Bacillus of Calmette Guerin (BCG) and vaccinia virus vaccination given in early childhood or acute infectious diseases acquired later in life were associated with a lesser melanoma risk [81]. This evidence was further examined and confirmed in another multi-centre case-control study conducted on 603 incident cases of malignant melanoma and 627 population controls (Table 1) [82]. Table 1 Case-control study (FEBIM-1): Combined effect of infections and vaccinations on the risk of melanoma; Odds ratios (95% confidence interval) for melanoma risk, adjusted for study centre, gender, age, skin phenotype, freckling index, number of naevi and solar burns[82] &#xA0; Number of severe infections &#xA0; 0 &#x2265;1 No vaccine 1.0 0.37 (0.10-1.42) BCG or Vaccinia 0.57 (0.33-0-96) 0.29 (0.15-0.57) BCG and Vaccinia 0.40 (0.23-0.68) 0.33 (0.17-0.65) A protein bearing a high homology sequence of amino acids with the antigen HERV-K-MEL is expressed by BCG and vaccinia virus vaccine (Table 2). The yellow fever virus vaccine (YFV) was also found to express an antigen with a strict homology sequence of amino acids with HERV-K-MEL (Table 2) [38]. Table 2 Comparison between amino acid sequence of HERV-K-MEL and proteins from different viruses[38] &#xA0; HERV-K-Mel M L A V _ I S C A V BCG L * * * DV V P I * * Vaccinia virus S * * * V * A * * &#xA0; Yellow fever virus S * * * _ _ * S * * A = Alanine; L: Leucine; V = Valine; I = Isoleucine; S = Serine; M = Methionine; C = Cisteine; P=Proline; D=Aspartic Acid; G= Glycine; * = Identical amino acids; _ = Missing amino acid. Sera from four Rhesus macaques before and four weeks after being administered with YFV were incubated with melanoma cells from two randomly selected patients: immune reactivity was observed at indirect immune-fluorescence in most apes post vaccination [Hunsmann &amp; Krone 2005. Vaccination against malignant melanoma. European Patent EP1586330A1]. This suggests that YFV might confer a protection against melanoma, by molecular mimicry (Figure 2). Figure 2 Molecular mimicry and immunological response possibly triggered by the yellow fever virus vaccine (YFV), leading to cancer prevention. APC= Antigen presenting cells. To assess this protective effect, a cohort study (28,306 subjects vaccinated with YFV) and a case-control study nested in the cohort (37 melanoma cases vs. 151 tumours not expressing HERV-K-MEL) was recently performed in North-Eastern Italy [83]. The time elapsed since YFV up to end of follow up (TSV) was split into the following year intervals: 0-4; 5-9; 10+. In the case control study contrasting melanoma with tumors non-expressing HERV-K-MEL, the Odds Ratios (OR) for the above mentioned time bands adjusted for age and sex were 1.00, 0.96, (95% CI: 0.43-2.14) and 0.26 (95% CI: 0.07-0.96). The risk of melanoma was therefore reduced if YFV had been received at least 10 years before, as a result of prevention of tumor initiation rather than culling of already compromised melanoma cells [83]. Hodges Vasquez et al. [84] recently conducted a case-control study on 7,010 members of the US military to test the association between YFV and melanoma risk. Total cases of melanoma in this cohort were 638 diagnosed from 1999 to 2009 and each of them was contrasted with 10 healthy controls from active duty military service members. The study concluded that no significant association between YFV 17D and melanoma risk was found. However the maximum TSV was only 11.5 years and controls were presumably selected among healthy subjects. Selecting controls among individuals with malignancies other than melanoma from the same cohort of vaccinees (as done in the above Italian study) might influence the strength of the association, as study subjects would be a better choice. If the interaction between YFV and HERV-K-MEL prevents melanoma, healthy individuals could not be accepted as controls because some of them could be &#x201C;cases of melanoma prevented by YFV&#x201D; rather than simply subjects without disease. Prevention of melanoma could occur frequently because numerous infectious agents produce homologous epitopes capable of generating cross-reactive immunity.")

        enhance(text)

        val xml = Xmlizer.toXml(text)

        text = Xmlizer.fromXml(xml)

        println(xml)
    }

}
