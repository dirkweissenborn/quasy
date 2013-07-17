package de.tu.dresden.quasy.enhancer.umls

import de.tu.dresden.quasy.enhancer.TextEnhancer
import de.tu.dresden.quasy.model.{Span, AnnotatedText}
import de.tu.dresden.quasy.webservices.metamap.MetaMapService
import scala.collection.JavaConversions._
import de.tu.dresden.quasy.model.annotation.{OntologyEntityMention, UmlsConcept}
import java.net.URL
import de.tu.dresden.quasy.model.db.MetaMapCache
import de.tu.dresden.quasy.util.Xmlizer
import java.io.File
import gov.nih.nlm.nls.metamap.Ev


/**
 * @author dirk
 * Date: 4/15/13
 * Time: 10:41 AM
 */
object UmlsEnhancer2 extends TextEnhancer {
    private final val allowedSTypes = "acab,amas,aapp,anab,anst,antb,biof,bacs,blor,bpoc,carb,crbs,cell,celc,celf,comd,chem,chvf,chvs,clnd,cgab,diap,dsyn,drdd,eico,elii,emst,enzy,food,ffas,fngs,gngp,gngm,genf,hops,horm,inpo,inch,lipd,mobd,moft,mosq,neop,nsba,nnon,nusq,ortf,orch,opco,phsu,rcpt,sosy,strd,sbst,virs,vita".split(",")


    protected def pEnhance(text: AnnotatedText) {
        /*var results = MetaMapCache.getCachedUmlsResponse(text.text)

        if(results.isEmpty) */
        val results = MetaMapService.process(text.text)

        var mappings = results.flatMap(_.getUtteranceList).flatMap(_.getPCMList).flatMap(_.getMappingList).flatMap(_.getEvList)
        mappings = mappings.groupBy(m => m.getTerm.toString).map(_._2.head).toList
        mappings.foreach(mapping => {
            val spans = mapping.getPositionalInfo.map(p => new Span(p.getX,p.getX+p.getY)).toArray.sortBy(_.begin)
            val tuis:Set[String] = mapping.getSemanticTypes.filter(allowedSTypes.contains).toSet
            if(tuis.size > 0) {
                val concept =
                    new UmlsConcept(mapping.getConceptId,
                                    mapping.getPreferredName,
                                    Set[String](),
                                    tuis,
                                    Set[String](),
                                    -mapping.getScore)

                new OntologyEntityMention(spans,text,List(concept))
            }
        })

        //MetaMapCache.addToCache(text.text, results)
    }

    def main(args:Array[String]) {
        MetaMapCache.loadCache(new File("./test.mm"))
        val text = new AnnotatedText("Is Rheumatoid Arthritis more common in (men or)- women?")
        pEnhance(text)
        MetaMapCache.storeCache

    }

}




