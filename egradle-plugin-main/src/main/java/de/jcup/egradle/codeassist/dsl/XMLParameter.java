package de.jcup.egradle.codeassist.dsl;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "parameter")
public class XMLParameter implements Parameter, ModifiableParameter {


	@XmlAttribute(name = "type")
	private String typeAsString;
	
	@XmlAttribute(name = "name")
	private String name;

	@XmlElement(name = "description")
	private String description;

	private Type type;

	public String getDescription() {
		return description;
	}

	@Override
	public String getName() {
		return name;
	}

	/* (non-Javadoc)
	 * @see de.jcup.egradle.codeassist.dsl.ModifiableParameter#setType(de.jcup.egradle.codeassist.dsl.Type)
	 */
	@Override
	public void setType(Type type) {
		this.type = type;
	}

	@Override
	public Type getType() {
		return type;
	}
	
	public String getTypeAsString() {
		return typeAsString;
	}
	
	@Override
	public String toString() {
		return "XMLType [name=" + name + ", type="+typeAsString+" ]";
	}
}
