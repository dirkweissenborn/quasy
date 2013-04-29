
package de.tu.dresden.quasy.webservices.omiotis;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the de.tu.dresden.quasy.enhancer.omiotis package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _GetLemmaForWordPosOffset_QNAME = new QName("http://sr.hua.org/", "getLemmaForWordPosOffset");
    private final static QName _GetSRForPairOfLemmasFastResponse_QNAME = new QName("http://sr.hua.org/", "getSRForPairOfLemmasFastResponse");
    private final static QName _GetAllGlossPairsForTerms_QNAME = new QName("http://sr.hua.org/", "getAllGlossPairsForTerms");
    private final static QName _GetWordnetRank_QNAME = new QName("http://sr.hua.org/", "getWordnetRank");
    private final static QName _GetAllPairsForTermsPMIResponse_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsPMIResponse");
    private final static QName _CalculateSR_QNAME = new QName("http://sr.hua.org/", "calculateSR");
    private final static QName _GetAllPairsForTermsResponse_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsResponse");
    private final static QName _GetAllPairsForTermsIDFPMIResponse_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsIDFPMIResponse");
    private final static QName _GetExtendedSynsetForSenseResponse_QNAME = new QName("http://sr.hua.org/", "getExtendedSynsetForSenseResponse");
    private final static QName _GetSRForPairOfLemmasResponse_QNAME = new QName("http://sr.hua.org/", "getSRForPairOfLemmasResponse");
    private final static QName _GetGlossForSense_QNAME = new QName("http://sr.hua.org/", "getGlossForSense");
    private final static QName _GetAllPairsForTermsGPMI_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsGPMI");
    private final static QName _GetAllPairsForTermsIDFPMI_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsIDFPMI");
    private final static QName _Lemmatize_QNAME = new QName("http://sr.hua.org/", "lemmatize");
    private final static QName _GetSRForPairOfLemmasFast_QNAME = new QName("http://sr.hua.org/", "getSRForPairOfLemmasFast");
    private final static QName _GetLemmaForWordPosOffsetResponse_QNAME = new QName("http://sr.hua.org/", "getLemmaForWordPosOffsetResponse");
    private final static QName _CalculatePMI_QNAME = new QName("http://sr.hua.org/", "calculatePMI");
    private final static QName _GetSRForPairOfLemmasPOSFastResponse_QNAME = new QName("http://sr.hua.org/", "getSRForPairOfLemmasPOSFastResponse");
    private final static QName _GetAllPairsForTermsGPMIResponse_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsGPMIResponse");
    private final static QName _GetAllPairsForTerms_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTerms");
    private final static QName _GetAllGlossPairsForTermsResponse_QNAME = new QName("http://sr.hua.org/", "getAllGlossPairsForTermsResponse");
    private final static QName _GetAllSensesForTerm_QNAME = new QName("http://sr.hua.org/", "getAllSensesForTerm");
    private final static QName _LemmatizeResponse_QNAME = new QName("http://sr.hua.org/", "lemmatizeResponse");
    private final static QName _GetAllSensesForTermResponse_QNAME = new QName("http://sr.hua.org/", "getAllSensesForTermResponse");
    private final static QName _GetAllWordsForSenseResponse_QNAME = new QName("http://sr.hua.org/", "getAllWordsForSenseResponse");
    private final static QName _GetAllWordsForSense_QNAME = new QName("http://sr.hua.org/", "getAllWordsForSense");
    private final static QName _CalculateSRResponse_QNAME = new QName("http://sr.hua.org/", "calculateSRResponse");
    private final static QName _GetWordnetRankResponse_QNAME = new QName("http://sr.hua.org/", "getWordnetRankResponse");
    private final static QName _GetGlossForSenseResponse_QNAME = new QName("http://sr.hua.org/", "getGlossForSenseResponse");
    private final static QName _GetSRForPairOfLemmas_QNAME = new QName("http://sr.hua.org/", "getSRForPairOfLemmas");
    private final static QName _CalculatePMIResponse_QNAME = new QName("http://sr.hua.org/", "calculatePMIResponse");
    private final static QName _GetExtendedSynsetForSense_QNAME = new QName("http://sr.hua.org/", "getExtendedSynsetForSense");
    private final static QName _GetAllExtendedGlossPairsForTerms_QNAME = new QName("http://sr.hua.org/", "getAllExtendedGlossPairsForTerms");
    private final static QName _GetSRForPairOfLemmasPOSFast_QNAME = new QName("http://sr.hua.org/", "getSRForPairOfLemmasPOSFast");
    private final static QName _GetAllExtendedGlossPairsForTermsResponse_QNAME = new QName("http://sr.hua.org/", "getAllExtendedGlossPairsForTermsResponse");
    private final static QName _GetAllPairsForTermsPMI_QNAME = new QName("http://sr.hua.org/", "getAllPairsForTermsPMI");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: de.tu.dresden.quasy.enhancer.omiotis
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GetLemmaForWordPosOffset }
     * 
     */
    public GetLemmaForWordPosOffset createGetLemmaForWordPosOffset() {
        return new GetLemmaForWordPosOffset();
    }

    /**
     * Create an instance of {@link GetSRForPairOfLemmasFastResponse }
     * 
     */
    public GetSRForPairOfLemmasFastResponse createGetSRForPairOfLemmasFastResponse() {
        return new GetSRForPairOfLemmasFastResponse();
    }

    /**
     * Create an instance of {@link GetAllGlossPairsForTerms }
     * 
     */
    public GetAllGlossPairsForTerms createGetAllGlossPairsForTerms() {
        return new GetAllGlossPairsForTerms();
    }

    /**
     * Create an instance of {@link GetWordnetRank }
     * 
     */
    public GetWordnetRank createGetWordnetRank() {
        return new GetWordnetRank();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsPMIResponse }
     * 
     */
    public GetAllPairsForTermsPMIResponse createGetAllPairsForTermsPMIResponse() {
        return new GetAllPairsForTermsPMIResponse();
    }

    /**
     * Create an instance of {@link CalculateSR }
     * 
     */
    public CalculateSR createCalculateSR() {
        return new CalculateSR();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsResponse }
     * 
     */
    public GetAllPairsForTermsResponse createGetAllPairsForTermsResponse() {
        return new GetAllPairsForTermsResponse();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsIDFPMIResponse }
     * 
     */
    public GetAllPairsForTermsIDFPMIResponse createGetAllPairsForTermsIDFPMIResponse() {
        return new GetAllPairsForTermsIDFPMIResponse();
    }

    /**
     * Create an instance of {@link GetSRForPairOfLemmasResponse }
     * 
     */
    public GetSRForPairOfLemmasResponse createGetSRForPairOfLemmasResponse() {
        return new GetSRForPairOfLemmasResponse();
    }

    /**
     * Create an instance of {@link GetExtendedSynsetForSenseResponse }
     * 
     */
    public GetExtendedSynsetForSenseResponse createGetExtendedSynsetForSenseResponse() {
        return new GetExtendedSynsetForSenseResponse();
    }

    /**
     * Create an instance of {@link GetGlossForSense }
     * 
     */
    public GetGlossForSense createGetGlossForSense() {
        return new GetGlossForSense();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsGPMI }
     * 
     */
    public GetAllPairsForTermsGPMI createGetAllPairsForTermsGPMI() {
        return new GetAllPairsForTermsGPMI();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsIDFPMI }
     * 
     */
    public GetAllPairsForTermsIDFPMI createGetAllPairsForTermsIDFPMI() {
        return new GetAllPairsForTermsIDFPMI();
    }

    /**
     * Create an instance of {@link Lemmatize }
     * 
     */
    public Lemmatize createLemmatize() {
        return new Lemmatize();
    }

    /**
     * Create an instance of {@link GetSRForPairOfLemmasFast }
     * 
     */
    public GetSRForPairOfLemmasFast createGetSRForPairOfLemmasFast() {
        return new GetSRForPairOfLemmasFast();
    }

    /**
     * Create an instance of {@link GetLemmaForWordPosOffsetResponse }
     * 
     */
    public GetLemmaForWordPosOffsetResponse createGetLemmaForWordPosOffsetResponse() {
        return new GetLemmaForWordPosOffsetResponse();
    }

    /**
     * Create an instance of {@link CalculatePMI }
     * 
     */
    public CalculatePMI createCalculatePMI() {
        return new CalculatePMI();
    }

    /**
     * Create an instance of {@link GetSRForPairOfLemmasPOSFastResponse }
     * 
     */
    public GetSRForPairOfLemmasPOSFastResponse createGetSRForPairOfLemmasPOSFastResponse() {
        return new GetSRForPairOfLemmasPOSFastResponse();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsGPMIResponse }
     * 
     */
    public GetAllPairsForTermsGPMIResponse createGetAllPairsForTermsGPMIResponse() {
        return new GetAllPairsForTermsGPMIResponse();
    }

    /**
     * Create an instance of {@link GetAllPairsForTerms }
     * 
     */
    public GetAllPairsForTerms createGetAllPairsForTerms() {
        return new GetAllPairsForTerms();
    }

    /**
     * Create an instance of {@link GetAllGlossPairsForTermsResponse }
     * 
     */
    public GetAllGlossPairsForTermsResponse createGetAllGlossPairsForTermsResponse() {
        return new GetAllGlossPairsForTermsResponse();
    }

    /**
     * Create an instance of {@link GetAllSensesForTerm }
     * 
     */
    public GetAllSensesForTerm createGetAllSensesForTerm() {
        return new GetAllSensesForTerm();
    }

    /**
     * Create an instance of {@link LemmatizeResponse }
     * 
     */
    public LemmatizeResponse createLemmatizeResponse() {
        return new LemmatizeResponse();
    }

    /**
     * Create an instance of {@link GetAllSensesForTermResponse }
     * 
     */
    public GetAllSensesForTermResponse createGetAllSensesForTermResponse() {
        return new GetAllSensesForTermResponse();
    }

    /**
     * Create an instance of {@link GetAllWordsForSenseResponse }
     * 
     */
    public GetAllWordsForSenseResponse createGetAllWordsForSenseResponse() {
        return new GetAllWordsForSenseResponse();
    }

    /**
     * Create an instance of {@link GetAllWordsForSense }
     * 
     */
    public GetAllWordsForSense createGetAllWordsForSense() {
        return new GetAllWordsForSense();
    }

    /**
     * Create an instance of {@link CalculateSRResponse }
     * 
     */
    public CalculateSRResponse createCalculateSRResponse() {
        return new CalculateSRResponse();
    }

    /**
     * Create an instance of {@link GetWordnetRankResponse }
     * 
     */
    public GetWordnetRankResponse createGetWordnetRankResponse() {
        return new GetWordnetRankResponse();
    }

    /**
     * Create an instance of {@link GetSRForPairOfLemmas }
     * 
     */
    public GetSRForPairOfLemmas createGetSRForPairOfLemmas() {
        return new GetSRForPairOfLemmas();
    }

    /**
     * Create an instance of {@link GetGlossForSenseResponse }
     * 
     */
    public GetGlossForSenseResponse createGetGlossForSenseResponse() {
        return new GetGlossForSenseResponse();
    }

    /**
     * Create an instance of {@link CalculatePMIResponse }
     * 
     */
    public CalculatePMIResponse createCalculatePMIResponse() {
        return new CalculatePMIResponse();
    }

    /**
     * Create an instance of {@link GetExtendedSynsetForSense }
     * 
     */
    public GetExtendedSynsetForSense createGetExtendedSynsetForSense() {
        return new GetExtendedSynsetForSense();
    }

    /**
     * Create an instance of {@link GetSRForPairOfLemmasPOSFast }
     * 
     */
    public GetSRForPairOfLemmasPOSFast createGetSRForPairOfLemmasPOSFast() {
        return new GetSRForPairOfLemmasPOSFast();
    }

    /**
     * Create an instance of {@link GetAllExtendedGlossPairsForTerms }
     * 
     */
    public GetAllExtendedGlossPairsForTerms createGetAllExtendedGlossPairsForTerms() {
        return new GetAllExtendedGlossPairsForTerms();
    }

    /**
     * Create an instance of {@link GetAllExtendedGlossPairsForTermsResponse }
     * 
     */
    public GetAllExtendedGlossPairsForTermsResponse createGetAllExtendedGlossPairsForTermsResponse() {
        return new GetAllExtendedGlossPairsForTermsResponse();
    }

    /**
     * Create an instance of {@link GetAllPairsForTermsPMI }
     * 
     */
    public GetAllPairsForTermsPMI createGetAllPairsForTermsPMI() {
        return new GetAllPairsForTermsPMI();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLemmaForWordPosOffset }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getLemmaForWordPosOffset")
    public JAXBElement<GetLemmaForWordPosOffset> createGetLemmaForWordPosOffset(GetLemmaForWordPosOffset value) {
        return new JAXBElement<GetLemmaForWordPosOffset>(_GetLemmaForWordPosOffset_QNAME, GetLemmaForWordPosOffset.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSRForPairOfLemmasFastResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getSRForPairOfLemmasFastResponse")
    public JAXBElement<GetSRForPairOfLemmasFastResponse> createGetSRForPairOfLemmasFastResponse(GetSRForPairOfLemmasFastResponse value) {
        return new JAXBElement<GetSRForPairOfLemmasFastResponse>(_GetSRForPairOfLemmasFastResponse_QNAME, GetSRForPairOfLemmasFastResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllGlossPairsForTerms }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllGlossPairsForTerms")
    public JAXBElement<GetAllGlossPairsForTerms> createGetAllGlossPairsForTerms(GetAllGlossPairsForTerms value) {
        return new JAXBElement<GetAllGlossPairsForTerms>(_GetAllGlossPairsForTerms_QNAME, GetAllGlossPairsForTerms.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetWordnetRank }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getWordnetRank")
    public JAXBElement<GetWordnetRank> createGetWordnetRank(GetWordnetRank value) {
        return new JAXBElement<GetWordnetRank>(_GetWordnetRank_QNAME, GetWordnetRank.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsPMIResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsPMIResponse")
    public JAXBElement<GetAllPairsForTermsPMIResponse> createGetAllPairsForTermsPMIResponse(GetAllPairsForTermsPMIResponse value) {
        return new JAXBElement<GetAllPairsForTermsPMIResponse>(_GetAllPairsForTermsPMIResponse_QNAME, GetAllPairsForTermsPMIResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CalculateSR }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "calculateSR")
    public JAXBElement<CalculateSR> createCalculateSR(CalculateSR value) {
        return new JAXBElement<CalculateSR>(_CalculateSR_QNAME, CalculateSR.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsResponse")
    public JAXBElement<GetAllPairsForTermsResponse> createGetAllPairsForTermsResponse(GetAllPairsForTermsResponse value) {
        return new JAXBElement<GetAllPairsForTermsResponse>(_GetAllPairsForTermsResponse_QNAME, GetAllPairsForTermsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsIDFPMIResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsIDFPMIResponse")
    public JAXBElement<GetAllPairsForTermsIDFPMIResponse> createGetAllPairsForTermsIDFPMIResponse(GetAllPairsForTermsIDFPMIResponse value) {
        return new JAXBElement<GetAllPairsForTermsIDFPMIResponse>(_GetAllPairsForTermsIDFPMIResponse_QNAME, GetAllPairsForTermsIDFPMIResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetExtendedSynsetForSenseResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getExtendedSynsetForSenseResponse")
    public JAXBElement<GetExtendedSynsetForSenseResponse> createGetExtendedSynsetForSenseResponse(GetExtendedSynsetForSenseResponse value) {
        return new JAXBElement<GetExtendedSynsetForSenseResponse>(_GetExtendedSynsetForSenseResponse_QNAME, GetExtendedSynsetForSenseResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSRForPairOfLemmasResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getSRForPairOfLemmasResponse")
    public JAXBElement<GetSRForPairOfLemmasResponse> createGetSRForPairOfLemmasResponse(GetSRForPairOfLemmasResponse value) {
        return new JAXBElement<GetSRForPairOfLemmasResponse>(_GetSRForPairOfLemmasResponse_QNAME, GetSRForPairOfLemmasResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetGlossForSense }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getGlossForSense")
    public JAXBElement<GetGlossForSense> createGetGlossForSense(GetGlossForSense value) {
        return new JAXBElement<GetGlossForSense>(_GetGlossForSense_QNAME, GetGlossForSense.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsGPMI }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsGPMI")
    public JAXBElement<GetAllPairsForTermsGPMI> createGetAllPairsForTermsGPMI(GetAllPairsForTermsGPMI value) {
        return new JAXBElement<GetAllPairsForTermsGPMI>(_GetAllPairsForTermsGPMI_QNAME, GetAllPairsForTermsGPMI.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsIDFPMI }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsIDFPMI")
    public JAXBElement<GetAllPairsForTermsIDFPMI> createGetAllPairsForTermsIDFPMI(GetAllPairsForTermsIDFPMI value) {
        return new JAXBElement<GetAllPairsForTermsIDFPMI>(_GetAllPairsForTermsIDFPMI_QNAME, GetAllPairsForTermsIDFPMI.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link Lemmatize }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "lemmatize")
    public JAXBElement<Lemmatize> createLemmatize(Lemmatize value) {
        return new JAXBElement<Lemmatize>(_Lemmatize_QNAME, Lemmatize.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSRForPairOfLemmasFast }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getSRForPairOfLemmasFast")
    public JAXBElement<GetSRForPairOfLemmasFast> createGetSRForPairOfLemmasFast(GetSRForPairOfLemmasFast value) {
        return new JAXBElement<GetSRForPairOfLemmasFast>(_GetSRForPairOfLemmasFast_QNAME, GetSRForPairOfLemmasFast.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetLemmaForWordPosOffsetResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getLemmaForWordPosOffsetResponse")
    public JAXBElement<GetLemmaForWordPosOffsetResponse> createGetLemmaForWordPosOffsetResponse(GetLemmaForWordPosOffsetResponse value) {
        return new JAXBElement<GetLemmaForWordPosOffsetResponse>(_GetLemmaForWordPosOffsetResponse_QNAME, GetLemmaForWordPosOffsetResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CalculatePMI }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "calculatePMI")
    public JAXBElement<CalculatePMI> createCalculatePMI(CalculatePMI value) {
        return new JAXBElement<CalculatePMI>(_CalculatePMI_QNAME, CalculatePMI.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSRForPairOfLemmasPOSFastResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getSRForPairOfLemmasPOSFastResponse")
    public JAXBElement<GetSRForPairOfLemmasPOSFastResponse> createGetSRForPairOfLemmasPOSFastResponse(GetSRForPairOfLemmasPOSFastResponse value) {
        return new JAXBElement<GetSRForPairOfLemmasPOSFastResponse>(_GetSRForPairOfLemmasPOSFastResponse_QNAME, GetSRForPairOfLemmasPOSFastResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsGPMIResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsGPMIResponse")
    public JAXBElement<GetAllPairsForTermsGPMIResponse> createGetAllPairsForTermsGPMIResponse(GetAllPairsForTermsGPMIResponse value) {
        return new JAXBElement<GetAllPairsForTermsGPMIResponse>(_GetAllPairsForTermsGPMIResponse_QNAME, GetAllPairsForTermsGPMIResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTerms }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTerms")
    public JAXBElement<GetAllPairsForTerms> createGetAllPairsForTerms(GetAllPairsForTerms value) {
        return new JAXBElement<GetAllPairsForTerms>(_GetAllPairsForTerms_QNAME, GetAllPairsForTerms.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllGlossPairsForTermsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllGlossPairsForTermsResponse")
    public JAXBElement<GetAllGlossPairsForTermsResponse> createGetAllGlossPairsForTermsResponse(GetAllGlossPairsForTermsResponse value) {
        return new JAXBElement<GetAllGlossPairsForTermsResponse>(_GetAllGlossPairsForTermsResponse_QNAME, GetAllGlossPairsForTermsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllSensesForTerm }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllSensesForTerm")
    public JAXBElement<GetAllSensesForTerm> createGetAllSensesForTerm(GetAllSensesForTerm value) {
        return new JAXBElement<GetAllSensesForTerm>(_GetAllSensesForTerm_QNAME, GetAllSensesForTerm.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link LemmatizeResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "lemmatizeResponse")
    public JAXBElement<LemmatizeResponse> createLemmatizeResponse(LemmatizeResponse value) {
        return new JAXBElement<LemmatizeResponse>(_LemmatizeResponse_QNAME, LemmatizeResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllSensesForTermResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllSensesForTermResponse")
    public JAXBElement<GetAllSensesForTermResponse> createGetAllSensesForTermResponse(GetAllSensesForTermResponse value) {
        return new JAXBElement<GetAllSensesForTermResponse>(_GetAllSensesForTermResponse_QNAME, GetAllSensesForTermResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllWordsForSenseResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllWordsForSenseResponse")
    public JAXBElement<GetAllWordsForSenseResponse> createGetAllWordsForSenseResponse(GetAllWordsForSenseResponse value) {
        return new JAXBElement<GetAllWordsForSenseResponse>(_GetAllWordsForSenseResponse_QNAME, GetAllWordsForSenseResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllWordsForSense }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllWordsForSense")
    public JAXBElement<GetAllWordsForSense> createGetAllWordsForSense(GetAllWordsForSense value) {
        return new JAXBElement<GetAllWordsForSense>(_GetAllWordsForSense_QNAME, GetAllWordsForSense.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CalculateSRResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "calculateSRResponse")
    public JAXBElement<CalculateSRResponse> createCalculateSRResponse(CalculateSRResponse value) {
        return new JAXBElement<CalculateSRResponse>(_CalculateSRResponse_QNAME, CalculateSRResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetWordnetRankResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getWordnetRankResponse")
    public JAXBElement<GetWordnetRankResponse> createGetWordnetRankResponse(GetWordnetRankResponse value) {
        return new JAXBElement<GetWordnetRankResponse>(_GetWordnetRankResponse_QNAME, GetWordnetRankResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetGlossForSenseResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getGlossForSenseResponse")
    public JAXBElement<GetGlossForSenseResponse> createGetGlossForSenseResponse(GetGlossForSenseResponse value) {
        return new JAXBElement<GetGlossForSenseResponse>(_GetGlossForSenseResponse_QNAME, GetGlossForSenseResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSRForPairOfLemmas }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getSRForPairOfLemmas")
    public JAXBElement<GetSRForPairOfLemmas> createGetSRForPairOfLemmas(GetSRForPairOfLemmas value) {
        return new JAXBElement<GetSRForPairOfLemmas>(_GetSRForPairOfLemmas_QNAME, GetSRForPairOfLemmas.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link CalculatePMIResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "calculatePMIResponse")
    public JAXBElement<CalculatePMIResponse> createCalculatePMIResponse(CalculatePMIResponse value) {
        return new JAXBElement<CalculatePMIResponse>(_CalculatePMIResponse_QNAME, CalculatePMIResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetExtendedSynsetForSense }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getExtendedSynsetForSense")
    public JAXBElement<GetExtendedSynsetForSense> createGetExtendedSynsetForSense(GetExtendedSynsetForSense value) {
        return new JAXBElement<GetExtendedSynsetForSense>(_GetExtendedSynsetForSense_QNAME, GetExtendedSynsetForSense.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllExtendedGlossPairsForTerms }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllExtendedGlossPairsForTerms")
    public JAXBElement<GetAllExtendedGlossPairsForTerms> createGetAllExtendedGlossPairsForTerms(GetAllExtendedGlossPairsForTerms value) {
        return new JAXBElement<GetAllExtendedGlossPairsForTerms>(_GetAllExtendedGlossPairsForTerms_QNAME, GetAllExtendedGlossPairsForTerms.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetSRForPairOfLemmasPOSFast }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getSRForPairOfLemmasPOSFast")
    public JAXBElement<GetSRForPairOfLemmasPOSFast> createGetSRForPairOfLemmasPOSFast(GetSRForPairOfLemmasPOSFast value) {
        return new JAXBElement<GetSRForPairOfLemmasPOSFast>(_GetSRForPairOfLemmasPOSFast_QNAME, GetSRForPairOfLemmasPOSFast.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllExtendedGlossPairsForTermsResponse }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllExtendedGlossPairsForTermsResponse")
    public JAXBElement<GetAllExtendedGlossPairsForTermsResponse> createGetAllExtendedGlossPairsForTermsResponse(GetAllExtendedGlossPairsForTermsResponse value) {
        return new JAXBElement<GetAllExtendedGlossPairsForTermsResponse>(_GetAllExtendedGlossPairsForTermsResponse_QNAME, GetAllExtendedGlossPairsForTermsResponse.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link GetAllPairsForTermsPMI }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://sr.hua.org/", name = "getAllPairsForTermsPMI")
    public JAXBElement<GetAllPairsForTermsPMI> createGetAllPairsForTermsPMI(GetAllPairsForTermsPMI value) {
        return new JAXBElement<GetAllPairsForTermsPMI>(_GetAllPairsForTermsPMI_QNAME, GetAllPairsForTermsPMI.class, null, value);
    }

}
