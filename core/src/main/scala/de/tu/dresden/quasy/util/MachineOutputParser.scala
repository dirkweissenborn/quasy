package de.tu.dresden.quasy.util

import scala.util.parsing.combinator._
import prolog.io.IO


class MachineOutputParser extends RegexParsers {

    val singleQuote:Parser[String] = "'" ~> """(\\'|[^']|''|'(?![,\[\]()]))*""".r <~ "'"
    val doubleQuote:Parser[String] = "\"" ~> """(\\"|[^"]|""|"(?![,\[\]()]))*""".r <~ "\""
    val symbol:Parser[String] = """[^,\n()\[\]]*""".r
    val number:Parser[String] = """-?\d+""".r
    val symbolOrQuote:Parser[String] = singleQuote | symbol

    def stringList:Parser[List[String]] = "[" ~> repsep(symbol,",") <~ "]"

    def list[T](elements:Parser[T]):Parser[List[T]] = "[" ~> repsep(elements,",") <~ "]"

    def utterance:Parser[MMUtterance] =
        rep("\n") ~> ("args" ~> """[^\n]+\n""".r).? ~>
        ("aas" ~> """[^\n]+\n""".r).? ~>
        ("neg_list" ~> """[^\n]+\n""".r).? ~>
        "utterance(" ~> singleQuote ~ "," ~ doubleQuote ~ "," ~ symbol ~ "," ~ stringList ~ ").\n" ~
        phrases <~ "'EOU'." <~ rep("\n") ^^ {
            case id ~ "," ~ text ~ "," ~ positionalInfo ~ "," ~ _ ~ _ ~ mmPhrases => {
                val Array(start,length) = positionalInfo.split("/",2)
                val Array(pmid,sec,num) = id.split("""\.""",3)
                MMUtterance(pmid,sec,num.toInt,text,start.toInt,length.toInt,mmPhrases)
            }
        }

    def phrases:Parser[List[MMPhrase]] = rep(phrase)

    def phrase:Parser[MMPhrase] =
        "phrase(" ~> symbolOrQuote ~> "," ~> """.*\],""".r ~> symbol ~ """,[^\n]+\n""".r ~ candidates ~ mappings ^^ {
            case positionalInfo ~ _ ~ cands ~ maps => {
                val Array(start,length) = positionalInfo.split("/",2)
                MMPhrase(start.toInt,length.toInt,cands,maps)
            }
        }

    def candidates:Parser[List[MMCandidate]] =
        "candidates(" ~> number ~> "," ~> number ~> "," ~> number ~> "," ~> number ~> "," ~>
        "[" ~> repsep(candidate,",") <~ "])."

    //ev(-856,'C0183413','SPECULA, OPHTHALMIC','SPECULA, OPHTHALMIC',[ophthalmic,specula],[medd],[[[2,2],[1,1],2],[[3,3],[2,2],1]],yes,no,['SPN'],[168/12],0)
    def candidate:Parser[MMCandidate] =
        "ev(" ~> number ~ ","  ~ singleQuote ~ "," ~ symbolOrQuote ~ "," ~ symbolOrQuote ~ "," ~ stringList ~ "," ~
        stringList ~ "," ~ list(list(list(number) | number)) ~ "," ~ """(yes|no),(yes|no)""".r ~ "," ~
        list(singleQuote) ~ "," ~ stringList <~ "," <~ number <~ ")" ^^ {
            case score ~ "," ~ cui ~ "," ~ umlsName ~ "," ~ preferredName ~ "," ~ _ ~ "," ~
                 semtypes ~ "," ~ _ ~ "," ~ _ ~ "," ~
                 sources ~ "," ~ positions => {
                val rPositions = positions.map(positionalInfo => {
                    val Array(start,length) = positionalInfo.split("/",2)
                    (start.toInt,length.toInt)
                })

                MMCandidate(score.toInt,cui,semtypes,rPositions)
            }

        }

    def mappings:Parser[List[MMMapping]] =  "mappings([" ~> repsep(mapping,",") <~ "])."

    //map(-871,[ev(-660,'C0184511','Improved','Improved',[improved],[qlco],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','CHV'],[159/8],0),ev(-856,'C0183413','SPECULA, OPHTHALMIC','SPECULA, OPHTHALMIC',[ophthalmic,specula],[medd],[[[2,2],[1,1],2],[[3,3],[2,2],1]],yes,no,['SPN'],[168/12],0)])
    def mapping:Parser[MMMapping] =
        "map(" ~> number ~ "," ~ list(candidate) <~ ")" ^^ {
            case score ~_~ candidates => {
                MMMapping(score.toInt,candidates.map(_.cui))
            }
        }

    def parse(s: String) = {
        val utt = parseAll(utterance, s) match {
            case Success(xss, _) => xss
            case other => {
                IO.warnmes("syntax error: " + other)
                null
            }
        }

        utt
    }


    def main(args:Array[String]) {
        val input = """args('MetaMap11v2.BINARY.Linux -L 2011 -Z 2011AB -qE -Q 4 -E /tmp/text_000N_345 /tmp/text_000N.out_345',[lexicon_year-'2011',mm_data_year-'2011AB',machine_output-[],composite_phrases-4,indicate_citation_end-[],infile-'/tmp/text_000N_345',outfile-'/tmp/text_000N.out_345']).
                      |aas([]).
                      |neg_list([]).
                      |utterance('16691646.ti.1',"Statement of ""Cases of Gonorrhoeal and Purulent"", Ophthalmia treated in the       Desmarres (U. S. Army) Eye and Ear Hospital, Chicago, Illinois, with       Special Report of Treatment Employed.",156/191,[228,303]).
                      |phrase('Statement of Cases of Gonorrhoeal',[head([lexmatch([statement]),inputmatch(['Statement']),tag(noun),tokens([statement])]),prep([lexmatch([of]),inputmatch([of]),tag(prep),tokens([of])]),mod([lexmatch([cases]),inputmatch(['Cases']),tag(noun),tokens([cases])]),prep([lexmatch([of]),inputmatch([of]),tag(prep),tokens([of])]),mod([lexmatch([gonorrhoeal]),inputmatch(['Gonorrhoeal']),tag(noun),tokens([gonorrhoeal])])],156/33,[]).
                      |candidates(6,2,0,4,[ev(-760,'C1710187','Statement','Statement',[statement],[idcn],[[[1,1],[1,1],0]],yes,no,['NCI'],[156/9],0),ev(-593,'C0868928','Cases','Case (situation)',[cases],[ftcn],[[[3,3],[1,1],0]],no,no,['MTH','SNOMEDCT','CHV','LCH'],[169/5],0),ev(-593,'C1533148','Cases','Case unit dose',[cases],[qnco],[[[3,3],[1,1],0]],no,no,['MTH','SNOMEDCT','NCI'],[169/5],0),ev(-560,'C1706255','CASE','Packaging Case',[case],[medd],[[[3,3],[1,1],1]],no,no,['MTH','NCI'],[169/5],1),ev(-560,'C1706256','Case','Clinical Study Case',[case],[cnce],[[[3,3],[1,1],1]],no,no,['MTH','NCI'],[169/5],1),ev(-521,'C0018081','Gonorrhea','Gonorrhea',[gonorrhea],[dsyn],[[[5,5],[1,1],3]],no,no,['LCH','MEDLINEPLUS','MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','COSTAR','DXP','SNMI','AOD','CHV','CSP','MTHICD9','ICD10CM','ICD9CM'],[178/11],0)]).
                      |mappings([map(-663,[ev(-760,'C1710187','Statement','Statement',[statement],[idcn],[[[1,1],[1,1],0]],yes,no,['NCI'],[156/9],0),ev(-593,'C0868928','Cases','Case (situation)',[cases],[ftcn],[[[3,3],[1,1],0]],no,no,['MTH','SNOMEDCT','CHV','LCH'],[169/5],0),ev(-521,'C0018081','Gonorrhea','Gonorrhea',[gonorrhea],[dsyn],[[[5,5],[1,1],3]],no,no,['LCH','MEDLINEPLUS','MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','COSTAR','DXP','SNMI','AOD','CHV','CSP','MTHICD9','ICD10CM','ICD9CM'],[178/11],0)]),map(-663,[ev(-760,'C1710187','Statement','Statement',[statement],[idcn],[[[1,1],[1,1],0]],yes,no,['NCI'],[156/9],0),ev(-593,'C1533148','Cases','Case unit dose',[cases],[qnco],[[[3,3],[1,1],0]],no,no,['MTH','SNOMEDCT','NCI'],[169/5],0),ev(-521,'C0018081','Gonorrhea','Gonorrhea',[gonorrhea],[dsyn],[[[5,5],[1,1],3]],no,no,['LCH','MEDLINEPLUS','MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','COSTAR','DXP','SNMI','AOD','CHV','CSP','MTHICD9','ICD10CM','ICD9CM'],[178/11],0)])]).
                      |phrase(and,[conj([lexmatch([and]),inputmatch([and]),tag(conj),tokens([and])])],190/3,[]).
                      |candidates(0,0,0,0,[]).
                      |mappings([]).
                      |phrase('Purulent Ophthalmia',[mod([lexmatch([purulent]),inputmatch(['Purulent']),tag(adj),tokens([purulent])]),head([lexmatch([ophthalmia]),inputmatch(['Ophthalmia']),tag(noun),tokens([ophthalmia])])],194/19,[]).
                      |candidates(6,4,0,2,[ev(-861,'C0014236','Ophthalmia','Endophthalmitis',[ophthalmia],[dsyn],[[[2,2],[1,1],0]],yes,no,['MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','CHV','CSP','SNMI','CST'],[203/10],0),ev(-768,'C0015392','Eye','Eye',[eye],[bpoc],[[[2,2],[1,1],5]],yes,no,['COSTAR','FMA','HL7V2.5','LCH','LNC','MSH','MTH','NCI','SNM','SNOMEDCT','UWDA','AOD','CHV','CSP','ICPC','OMIM','SNMI','ICF-CY','ICF'],[203/10],1),ev(-768,'C1280202','Eye','Entire eye',[eye],[bpoc],[[[2,2],[1,1],5]],yes,no,['MTH','SNOMEDCT'],[203/10],1),ev(-755,'C0042789','Ocular','Vision',[ocular],[orgf],[[[2,2],[1,1],7]],yes,no,['LCH','LNC','MSH','MTH','NCI','SNM','SNOMEDCT','SNMI','AOD','CHV','CSP','GO','ICF-CY','ICF'],[203/10],1),ev(-755,'C1299003','Ocular','Ocular (qualifier)',[ocular],[spco],[[[2,2],[1,1],7]],yes,no,['MTH','NCI','SNMI','SNOMEDCT'],[203/10],1),ev(-694,'C0439665','Purulent','Purulent',[purulent],[qlco],[[[1,1],[1,1],0]],no,no,['SNOMEDCT','CHV'],[194/8],0)]).
                      |mappings([map(-888,[ev(-694,'C0439665','Purulent','Purulent',[purulent],[qlco],[[[1,1],[1,1],0]],no,no,['SNOMEDCT','CHV'],[194/8],0),ev(-861,'C0014236','Ophthalmia','Endophthalmitis',[ophthalmia],[dsyn],[[[2,2],[1,1],0]],yes,no,['MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','CHV','CSP','SNMI','CST'],[203/10],0)])]).
                      |phrase('treated in the       Desmarres',[verb([lexmatch([treated]),inputmatch([treated]),tag(verb),tokens([treated])]),prep([lexmatch([in]),inputmatch([in]),tag(prep),tokens([in])]),det([lexmatch([the]),inputmatch([the]),tag(det),tokens([the])]),mod([inputmatch(['Desmarres']),tag(noun),tokens([desmarres])])],214/30,[228]).
                      |candidates(3,1,0,2,[ev(-770,'C0332293',treated,'Treated with',[treated],[topp],[[[1,1],[1,1],0]],yes,no,['MTH','SNMI','SNOMEDCT','CHV'],[214/7],0),ev(-770,'C1522326','Treated','Treating',[treated],[ftcn],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[214/7],0),ev(-737,'C1292734','TREAT','Treatment intent',[treat],[ftcn],[[[1,1],[1,1],1]],yes,no,['MTH','SNOMEDCT','NCI','CHV'],[214/7],1)]).
                      |mappings([map(-770,[ev(-770,'C0332293',treated,'Treated with',[treated],[topp],[[[1,1],[1,1],0]],yes,no,['MTH','SNMI','SNOMEDCT','CHV'],[214/7],0)]),map(-770,[ev(-770,'C1522326','Treated','Treating',[treated],[ftcn],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[214/7],0)])]).
                      |phrase('(U. S. Army',[punc([inputmatch(['(']),tokens([])]),mod([lexmatch(['U']),inputmatch(['U']),tag(noun),tokens([u])]),punc([inputmatch(['.']),tokens([])]),mod([lexmatch([s]),inputmatch(['S']),tag(noun),tokens([s])]),punc([inputmatch(['.']),tokens([])]),head([lexmatch([army]),inputmatch(['Army']),tag(noun),tokens([army])])],245/11,[]).
                      |candidates(3,0,0,3,[ev(-827,'C0680778',army,army,[army],[orgt],[[[3,3],[1,1],0]],yes,no,['AOD','CHV','LCH'],[252/4],0),ev(-734,'C0041703','U S','United States',[u,s],[geoa],[[[1,1],[1,1],0],[[2,2],[2,2],0]],no,no,['AOD','CHV','CSP','HL7V3.0','LCH','MSH','MTH','NCI','SNOMEDCT'],[246/1,249/1],0),ev(-660,'C1553035','[u]','Unit Of Measure Prefix - micro',[u],[qnco],[[[1,1],[1,1],0]],no,no,['MTH','NCI','HL7V3.0'],[246/1],0)]).
                      |mappings([map(-901,[ev(-734,'C0041703','U S','United States',[u,s],[geoa],[[[1,1],[1,1],0],[[2,2],[2,2],0]],no,no,['AOD','CHV','CSP','HL7V3.0','LCH','MSH','MTH','NCI','SNOMEDCT'],[246/1,249/1],0),ev(-827,'C0680778',army,army,[army],[orgt],[[[3,3],[1,1],0]],yes,no,['AOD','CHV','LCH'],[252/4],0)])]).
                      |phrase(')',[punc([inputmatch([')']),tokens([])])],256/1,[]).
                      |candidates(0,0,0,0,[]).
                      |mappings([]).
                      |phrase('Eye',[head([lexmatch([eye]),inputmatch(['Eye']),tag(noun),tokens([eye])])],258/3,[]).
                      |candidates(6,4,0,2,[ev(-1000,'C0015392','Eye','Eye',[eye],[bpoc],[[[1,1],[1,1],0]],yes,no,['COSTAR','FMA','HL7V2.5','LCH','LNC','MSH','MTH','NCI','SNM','SNOMEDCT','UWDA','AOD','CHV','CSP','ICPC','OMIM','SNMI','ICF-CY','ICF'],[258/3],0),ev(-1000,'C1280202','Eye','Entire eye',[eye],[bpoc],[[[1,1],[1,1],0]],yes,no,['MTH','SNOMEDCT'],[258/3],0),ev(-944,'C0042789','Ocular','Vision',[ocular],[orgf],[[[1,1],[1,1],2]],yes,no,['LCH','LNC','MSH','MTH','NCI','SNM','SNOMEDCT','SNMI','AOD','CHV','CSP','GO','ICF-CY','ICF'],[258/3],1),ev(-944,'C1299003','Ocular','Ocular (qualifier)',[ocular],[spco],[[[1,1],[1,1],2]],yes,no,['MTH','NCI','SNMI','SNOMEDCT'],[258/3],1),ev(-907,'C0014236','Ophthalmia','Endophthalmitis',[ophthalmia],[dsyn],[[[1,1],[1,1],5]],yes,no,['MSH','MTH','NCI','NDFRT','SNM','SNOMEDCT','CHV','CSP','SNMI','CST'],[258/3],1),ev(-907,'C0700042','Oculus','Orbital region',[oculus],[blor],[[[1,1],[1,1],5]],yes,no,['FMA','MTH','SNMI','SNM','SNOMEDCT','UWDA'],[258/3],1)]).
                      |mappings([map(-1000,[ev(-1000,'C0015392','Eye','Eye',[eye],[bpoc],[[[1,1],[1,1],0]],yes,no,['COSTAR','FMA','HL7V2.5','LCH','LNC','MSH','MTH','NCI','SNM','SNOMEDCT','UWDA','AOD','CHV','CSP','ICPC','OMIM','SNMI','ICF-CY','ICF'],[258/3],0)]),map(-1000,[ev(-1000,'C1280202','Eye','Entire eye',[eye],[bpoc],[[[1,1],[1,1],0]],yes,no,['MTH','SNOMEDCT'],[258/3],0)])]).
                      |phrase(and,[conj([lexmatch([and]),inputmatch([and]),tag(conj),tokens([and])])],262/3,[]).
                      |candidates(0,0,0,0,[]).
                      |mappings([]).
                      |phrase('Ear Hospital,',[mod([lexmatch([ear]),inputmatch(['Ear']),tag(noun),tokens([ear])]),head([lexmatch([hospital]),inputmatch(['Hospital']),tag(noun),tokens([hospital])]),punc([inputmatch([',']),tokens([])])],266/13,[]).
                      |candidates(10,6,0,4,[ev(-861,'C0019994','Hospital, NOS','Hospitals',[hospital],[hcro,mnob],[[[2,2],[1,1],0]],yes,no,['LCH','MEDLINEPLUS','MSH','MTH','CHV','LNC','NCI','SNOMEDCT','AOD','CSP','SNMI'],[270/8],0),ev(-861,'C1510665','Hospital','Hospital environment',[hospital],[qlco],[[[2,2],[1,1],0]],yes,no,['MTH','SNOMEDCT'],[270/8],0),ev(-827,'C1609061','Hospitals','Hospitals - NUCCProviderCodes',[hospitals],[inpr],[[[2,2],[1,1],1]],yes,no,['MTH','HL7V3.0'],[270/8],1),ev(-694,'C0013443','Ear','Ear structure',[ear],[bpoc],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','CHV','FMA','HL7V2.5','LCH','LNC','MSH','NCI','SNM','UWDA','AOD','CSP','ICPC','OMIM','SNMI'],[266/3],0),ev(-694,'C0521421','Ear','Entire ear',[ear],[bpoc],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','SNMI'],[266/3],0),ev(-661,'C1414437','EARS','EPRS gene',[ears],[gngm],[[[1,1],[1,1],1]],no,no,['HUGO','MTH','OMIM'],[266/3],1),ev(-638,'C0521422','Otic','Otic',[otic],[spco],[[[1,1],[1,1],2]],no,no,['MTH','SNMI','CHV'],[266/3],1),ev(-638,'C1272926','Otic','Otic dosage form',[otic],[bodm],[[[1,1],[1,1],2]],no,no,['MTH','SNOMEDCT','NCI'],[266/3],1),ev(-638,'C1418197','OTOR','OTOR gene',[otor],[gngm],[[[1,1],[1,1],2]],no,no,['HUGO','MTH','OMIM'],[266/3],1),ev(-638,'C1522191','Otic','Auricular Route of Drug Administration',[otic],[ftcn],[[[1,1],[1,1],2]],no,no,['MTH','HL7V2.5','NCI','SNOMEDCT','SNMI','CHV'],[266/3],1)]).
                      |mappings([map(-888,[ev(-694,'C0013443','Ear','Ear structure',[ear],[bpoc],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','CHV','FMA','HL7V2.5','LCH','LNC','MSH','NCI','SNM','UWDA','AOD','CSP','ICPC','OMIM','SNMI'],[266/3],0),ev(-861,'C0019994','Hospital, NOS','Hospitals',[hospital],[hcro,mnob],[[[2,2],[1,1],0]],yes,no,['LCH','MEDLINEPLUS','MSH','MTH','CHV','LNC','NCI','SNOMEDCT','AOD','CSP','SNMI'],[270/8],0)]),map(-888,[ev(-694,'C0013443','Ear','Ear structure',[ear],[bpoc],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','CHV','FMA','HL7V2.5','LCH','LNC','MSH','NCI','SNM','UWDA','AOD','CSP','ICPC','OMIM','SNMI'],[266/3],0),ev(-861,'C1510665','Hospital','Hospital environment',[hospital],[qlco],[[[2,2],[1,1],0]],yes,no,['MTH','SNOMEDCT'],[270/8],0)]),map(-888,[ev(-694,'C0521421','Ear','Entire ear',[ear],[bpoc],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','SNMI'],[266/3],0),ev(-861,'C0019994','Hospital, NOS','Hospitals',[hospital],[hcro,mnob],[[[2,2],[1,1],0]],yes,no,['LCH','MEDLINEPLUS','MSH','MTH','CHV','LNC','NCI','SNOMEDCT','AOD','CSP','SNMI'],[270/8],0)]),map(-888,[ev(-694,'C0521421','Ear','Entire ear',[ear],[bpoc],[[[1,1],[1,1],0]],no,no,['MTH','SNOMEDCT','SNMI'],[266/3],0),ev(-861,'C1510665','Hospital','Hospital environment',[hospital],[qlco],[[[2,2],[1,1],0]],yes,no,['MTH','SNOMEDCT'],[270/8],0)])]).
                      |phrase('Chicago,',[head([lexmatch(['Chicago']),inputmatch(['Chicago']),tag(noun),tokens([chicago])]),punc([inputmatch([',']),tokens([])])],280/8,[]).
                      |candidates(1,0,0,1,[ev(-1000,'C0008044','Chicago','Chicago',[chicago],[geoa],[[[1,1],[1,1],0]],yes,no,['MSH','CHV'],[280/7],0)]).
                      |mappings([map(-1000,[ev(-1000,'C0008044','Chicago','Chicago',[chicago],[geoa],[[[1,1],[1,1],0]],yes,no,['MSH','CHV'],[280/7],0)])]).
                      |phrase('Illinois,',[head([lexmatch(['Illinois']),inputmatch(['Illinois']),tag(noun),tokens([illinois])]),punc([inputmatch([',']),tokens([])])],289/9,[]).
                      |candidates(1,0,0,1,[ev(-1000,'C0020898','Illinois','Illinois (geographic location)',[illinois],[geoa],[[[1,1],[1,1],0]],yes,no,['MTH','SNOMEDCT','AOD','LCH','MSH','NCI','CHV'],[289/8],0)]).
                      |mappings([map(-1000,[ev(-1000,'C0020898','Illinois','Illinois (geographic location)',[illinois],[geoa],[[[1,1],[1,1],0]],yes,no,['MTH','SNOMEDCT','AOD','LCH','MSH','NCI','CHV'],[289/8],0)])]).
                      |phrase('with       Special Report',[prep([lexmatch([with]),inputmatch([with]),tag(prep),tokens([with])]),mod([lexmatch([special]),inputmatch(['Special']),tag(adj),tokens([special])]),head([lexmatch([report]),inputmatch(['Report']),tag(noun),tokens([report])])],299/25,[303]).
                      |candidates(3,0,0,3,[ev(-861,'C0684224','Report','Report (document)',[report],[inpr],[[[2,2],[1,1],0]],yes,no,['MTH','CHV','MSH','NCI','SNOMEDCT','AOD'],[318/6],0),ev(-861,'C0700287','Report','Reporting',[report],[hlca],[[[2,2],[1,1],0]],yes,no,['MTH','NCI','SNOMEDCT','AOD','CHV','LNC'],[318/6],0),ev(-694,'C0205555','Special','Special (qualifier)',[special],[qlco],[[[1,1],[1,1],0]],no,no,['MTH','NCI','SNMI','SNOMEDCT','CHV'],[310/7],0)]).
                      |mappings([map(-888,[ev(-694,'C0205555','Special','Special (qualifier)',[special],[qlco],[[[1,1],[1,1],0]],no,no,['MTH','NCI','SNMI','SNOMEDCT','CHV'],[310/7],0),ev(-861,'C0684224','Report','Report (document)',[report],[inpr],[[[2,2],[1,1],0]],yes,no,['MTH','CHV','MSH','NCI','SNOMEDCT','AOD'],[318/6],0)]),map(-888,[ev(-694,'C0205555','Special','Special (qualifier)',[special],[qlco],[[[1,1],[1,1],0]],no,no,['MTH','NCI','SNMI','SNOMEDCT','CHV'],[310/7],0),ev(-861,'C0700287','Report','Reporting',[report],[hlca],[[[2,2],[1,1],0]],yes,no,['MTH','NCI','SNOMEDCT','AOD','CHV','LNC'],[318/6],0)])]).
                      |phrase('of Treatment',[prep([lexmatch([of]),inputmatch([of]),tag(prep),tokens([of])]),head([lexmatch([treatment]),inputmatch(['Treatment']),tag(noun),tokens([treatment])])],325/12,[]).
                      |candidates(6,0,0,6,[ev(-1000,'C0039798',treatment,'therapeutic aspects',[treatment],[ftcn],[[[1,1],[1,1],0]],yes,no,['MTH','MSH'],[328/9],0),ev(-1000,'C0087111','Treatment','Therapeutic procedure',[treatment],[topp],[[[1,1],[1,1],0]],yes,no,['MTHMST','MTH','SNOMEDCT','NCI','MTHHH','SNMI','CHV','CSP','AOD','LNC','HL7V2.5','MSH'],[328/9],0),ev(-1000,'C1522326','Treatment','Treating',[treatment],[ftcn],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[328/9],0),ev(-1000,'C1533734','Treatment','Administration procedure',[treatment],[topp],[[[1,1],[1,1],0]],yes,no,['MTH','ICD10PCS','SNOMEDCT','NCI'],[328/9],0),ev(-1000,'C1705169','Treatment','Biomaterial Treatment',[treatment],[cnce],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[328/9],0),ev(-1000,'C3161471','TREATMENT','Treatment Study',[treatment],[resa],[[[1,1],[1,1],0]],yes,no,['NCI'],[328/9],0)]).
                      |mappings([map(-1000,[ev(-1000,'C0039798',treatment,'therapeutic aspects',[treatment],[ftcn],[[[1,1],[1,1],0]],yes,no,['MTH','MSH'],[328/9],0)]),map(-1000,[ev(-1000,'C0087111','Treatment','Therapeutic procedure',[treatment],[topp],[[[1,1],[1,1],0]],yes,no,['MTHMST','MTH','SNOMEDCT','NCI','MTHHH','SNMI','CHV','CSP','AOD','LNC','HL7V2.5','MSH'],[328/9],0)]),map(-1000,[ev(-1000,'C1522326','Treatment','Treating',[treatment],[ftcn],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[328/9],0)]),map(-1000,[ev(-1000,'C1533734','Treatment','Administration procedure',[treatment],[topp],[[[1,1],[1,1],0]],yes,no,['MTH','ICD10PCS','SNOMEDCT','NCI'],[328/9],0)]),map(-1000,[ev(-1000,'C1705169','Treatment','Biomaterial Treatment',[treatment],[cnce],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[328/9],0)]),map(-1000,[ev(-1000,'C3161471','TREATMENT','Treatment Study',[treatment],[resa],[[[1,1],[1,1],0]],yes,no,['NCI'],[328/9],0)])]).
                      |phrase('Employed.',[verb([lexmatch([employed]),inputmatch(['Employed']),tag(verb),tokens([employed])]),punc([inputmatch(['.']),tokens([])])],338/9,[]).
                      |candidates(2,1,0,1,[ev(-1000,'C0557351','Employed','Employed',[employed],[fndg],[[[1,1],[1,1],0]],yes,no,['MTH','SNOMEDCT','AOD','CHV'],[338/8],0),ev(-966,'C0457083','Employ','Usage',[employ],[ftcn],[[[1,1],[1,1],1]],yes,no,['MTH','NCI','SNOMEDCT','CHV'],[338/8],1)]).
                      |mappings([map(-1000,[ev(-1000,'C0557351','Employed','Employed',[employed],[fndg],[[[1,1],[1,1],0]],yes,no,['MTH','SNOMEDCT','AOD','CHV'],[338/8],0)])]).
                      |'EOU'.
                      |""".stripMargin

        val cands = parseAll(candidates,
            "candidates(4,2,0,2,[ev(-1000,'C0005775','Circulation','Blood Circulation',[circulation],[phsf],[[[1,1],[1,1],0]],yes,no,['MSH','MTH','NCI','AOD','CHV','CSP','GO','LCH'],[1133/11],0),ev(-1000,'C1516559','Circulation','Circulatory Process',[circulation],[orgf],[[[1,1],[1,1],0]],yes,no,['MTH','NCI'],[1133/11],0),ev(-928,'C0497231','Circulatory','circulatory system',[circulatory],[ftcn],[[[1,1],[1,1],3]],yes,no,['MTH','LNC','ICPC','CHV'],[1133/11],1),ev(-900,'C1531633','Circulator','Circulating nurse',[circulator],[prog],[[[1,1],[1,1],6]],yes,no,['SNOMEDCT'],[1133/11],1)]).")
        val utt = parse(input)
        utt
    }
}

/*
case class MMArgs(args:String, options:List[String])

case class MMNegList(negs:List[String]) */

case class MMUtterance(pmid:String,section:String,num:Int,text:String,startPos:Int,length:Int,phrases:List[MMPhrase])

case class MMPhrase(start:Int,length:Int, candidates:List[MMCandidate], mappings:List[MMMapping])

case class MMCandidate(score:Int,cui:String, semtypes:List[String], positions:List[(Int,Int)])

case class MMMapping(score:Int,cuis:List[String])
