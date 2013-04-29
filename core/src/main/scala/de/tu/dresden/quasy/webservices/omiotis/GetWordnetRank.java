
package de.tu.dresden.quasy.webservices.omiotis;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for getWordnetRank complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="getWordnetRank">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="t1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="p1" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="o1" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "getWordnetRank", propOrder = {
    "t1",
    "p1",
    "o1"
})
public class GetWordnetRank {

    protected String t1;
    protected int p1;
    protected long o1;

    /**
     * Gets the value of the t1 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getT1() {
        return t1;
    }

    /**
     * Sets the value of the t1 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setT1(String value) {
        this.t1 = value;
    }

    /**
     * Gets the value of the p1 property.
     * 
     */
    public int getP1() {
        return p1;
    }

    /**
     * Sets the value of the p1 property.
     * 
     */
    public void setP1(int value) {
        this.p1 = value;
    }

    /**
     * Gets the value of the o1 property.
     * 
     */
    public long getO1() {
        return o1;
    }

    /**
     * Sets the value of the o1 property.
     * 
     */
    public void setO1(long value) {
        this.o1 = value;
    }

}
