package de.jcup.egradle.codecompletion.dsl;

import java.util.Set;

/**
 * Represents a type. In java or groovy this can be a class, an interface , an enum etc.
 * @author Albert Tregnaghi
 *
 */
public interface Type extends LanguageElement{

	public Set<Property> getProperties();
	
	public Set<Method> getMethods();

	/**
	 * 
	 * @return a short/simple name
	 */
	public String getShortName();
	
	public void mixin(Type mixinType);

	public void addExtension(String extensionId, Type extensionType);

}
