
package de.tu.dresden.quasy.webservices.omiotis;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getSRForPairOfLemmasPOSFast complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getSRForPairOfLemmasPOSFast">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="lemma1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="lemma2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="pos1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="pos2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getSRForPairOfLemmasPOSFast", propOrder = {
    "lemma1",
    "lemma2",
    "pos1",
    "pos2"
})
public class GetSRForPairOfLemmasPOSFast {

    protected String lemma1;
    protected String lemma2;
    protected String pos1;
    protected String pos2;

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

    /**
     * Gets the value of the pos1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPos1() {
        return pos1;
    }

    /**
     * Sets the value of the pos1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPos1(String value) {
        this.pos1 = value;
    }

    /**
     * Gets the value of the pos2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getPos2() {
        return pos2;
    }

    /**
     * Sets the value of the pos2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setPos2(String value) {
        this.pos2 = value;
    }

}
