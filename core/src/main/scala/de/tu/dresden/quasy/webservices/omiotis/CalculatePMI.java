
package de.tu.dresden.quasy.webservices.omiotis;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for calculatePMI complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="calculatePMI">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="t1" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="p1" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="o1" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *         &lt;element name="t2" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *         &lt;element name="p2" type="{http://www.w3.org/2001/XMLSchema}int"/>
 *         &lt;element name="o2" type="{http://www.w3.org/2001/XMLSchema}long"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "calculatePMI", propOrder = {
    "t1",
    "p1",
    "o1",
    "t2",
    "p2",
    "o2"
})
public class CalculatePMI {

    protected String t1;
    protected int p1;
    protected long o1;
    protected String t2;
    protected int p2;
    protected long o2;

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

    /**
     * Gets the value of the t2 property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getT2() {
        return t2;
    }

    /**
     * Sets the value of the t2 property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setT2(String value) {
        this.t2 = value;
    }

    /**
     * Gets the value of the p2 property.
     * 
     */
    public int getP2() {
        return p2;
    }

    /**
     * Sets the value of the p2 property.
     * 
     */
    public void setP2(int value) {
        this.p2 = value;
    }

    /**
     * Gets the value of the o2 property.
     * 
     */
    public long getO2() {
        return o2;
    }

    /**
     * Sets the value of the o2 property.
     * 
     */
    public void setO2(long value) {
        this.o2 = value;
    }

}
