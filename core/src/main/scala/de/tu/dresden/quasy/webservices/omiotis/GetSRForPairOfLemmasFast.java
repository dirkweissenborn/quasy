
package de.tu.dresden.quasy.webservices.omiotis;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getSRForPairOfLemmasFast complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getSRForPairOfLemmasFast">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="lemma1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="lemma2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getSRForPairOfLemmasFast", propOrder = {
    "lemma1",
    "lemma2"
})
public class GetSRForPairOfLemmasFast {

    protected String lemma1;
    protected String lemma2;

    /**
     * Gets the value of the lemma1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLemma1() {
        return lemma1;
    }

    /**
     * Sets the value of the lemma1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLemma1(String value) {
        this.lemma1 = value;
    }

    /**
     * Gets the value of the lemma2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getLemma2() {
        return lemma2;
    }

    /**
     * Sets the value of the lemma2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setLemma2(String value) {
        this.lemma2 = value;
    }

}
