/*
 * Copyright 2016 Albert Tregnaghi
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *		http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions
 * and limitations under the License.
 *
 */
package de.jcup.egradle.codeassist.dsl;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.commons.lang3.StringUtils;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "type")
public class XMLType implements ModifiableType {

	@XmlAttribute(name = "interface")
	Boolean isInterface;

	@XmlElement(name = "description")
	private String description;

	private Map<String, Type> extensionMap = new TreeMap<>();

	private Map<String, Reason> extensionReasonMap = new TreeMap<>();

	@XmlAttribute(name = "language")
	private String language;

	private Map<LanguageElement, Reason> elementReasons = new HashMap<>();

	@XmlElement(name = "method", type = XMLMethod.class)
	private Set<Method> methods = new TreeSet<>();

	@XmlElement(name = "interface", type = XMLTypeReference.class)
	Set<TypeReference> interfaces = new TreeSet<>();

	@XmlAttribute(name = "name")
	String name;

	@XmlElement(name = "property", type = XMLProperty.class)
	private Set<Property> properties = new TreeSet<>();

	@XmlAttribute(name = "version")
	private String version;

	@XmlAttribute(name = "superType")
	private String superTypeAsString;

	private Type superType;

	private Set<Type> superInterfaceTypes;

	private Set<Method> mergedMethods;
	private Set<Property> mergedProperties;

	private Set<TypeReference> mergedInterfaces;

	@XmlAttribute(name = "documented", required = false)
	private Boolean partOfGradleDSLDocumentation = null;

	@Override
	public void addExtension(String extensionId, Type extensionType, Reason reason) {
		if (extensionType == null) {
			return;
		}
		if (extensionId == null) {
			extensionId = "null";
		}
		extensionMap.put(extensionId, extensionType);
		if (reason != null) {
			extensionReasonMap.put(extensionId, reason);
		}
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public void setDescription(String description) {
		this.description = description;
	}

	public Map<String, Type> getExtensions() {
		return extensionMap;
	}

	public String getLanguage() {
		return language;
	}

	@Override
	public Set<Method> getMethods() {
		/*
		 * we use always merged variant - so it does not matter when inheritance
		 * is called - in point of mixins etc.
		 */
		if (mergedMethods == null) {
			mergeMethodsAndProperties(null);
		}
		return mergedMethods;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Set<Property> getProperties() {
		if (mergedProperties == null) {
			mergeMethodsAndProperties(null);
		}
		return mergedProperties;
	}

	public Reason getReasonForExtension(String extensionId) {
		if (extensionId == null) {
			return null;
		}
		return extensionReasonMap.get(extensionId);
	}

	public LanguageElement getElementForMixin(String mixinId) {
		if (mixinId == null) {
			return null;
		}
		for (LanguageElement element : elementReasons.keySet()) {
			Reason reason = elementReasons.get(element);
			if (reason == null) {
				continue;
			}
			String id = reason.getMixinId();
			if (id == null) {
				continue;
			}
			if (mixinId.equals(id)) {
				return element;
			}
		}
		return null;
	}

	public Reason getReasonFor(LanguageElement element) {
		if (element == null) {
			return null;
		}
		return elementReasons.get(element);
	}

	@Override
	public String getShortName() {
		return StringUtils.substringAfterLast(name, ".");
	}

	public String getVersion() {
		if (version == null || version.length() == 0) {
			return "current";
		}
		return version;
	}

	@Override
	public void mixin(Type mixinType, Reason reason) {
		if (mixinType == null) {
			return;
		}
		Set<Method> mixinMethods = mixinType.getMethods();
		for (Method method : mixinMethods) {
			getMethods().add(method);
			if (reason != null) {
				elementReasons.put(method, reason);
			}
		}

		for (Property property : mixinType.getProperties()) {
			getProperties().add(property);
			if (reason != null) {
				elementReasons.put(property, reason);
			}
		}
	}

	@Override
	public String toString() {
		return "XMLType [name=" + name + "]";
	}

	@Override
	public String getSuperTypeAsString() {
		return superTypeAsString;
	}

	public Type getSuperType() {
		return superType;
	}

	@Override
	public void extendFrom(Type superType) {
		if (superType == null) {
			return;
		}
		if (superType.isInterface()) {
			extendFromInterface(superType);
		} else {
			extendFromSuperClass(superType);
		}

	}

	private void extendFromInterface(Type interfaceType) {
		if (interfaceType == null) {
			return;
		}
		if (!interfaceType.isInterface()) {
			return;
		}
		if (superInterfaceTypes == null) {
			superInterfaceTypes = new LinkedHashSet<>();
		}
		superInterfaceTypes.add(interfaceType);
		mergeMethodsAndProperties(interfaceType);

		addSuperTypeAsInterfaceIfNecessary(interfaceType);

	}

	private void extendFromSuperClass(Type superType) {
		if (superType == null) {
			return;
		}
		if (superType.isInterface()) {
			return;
		}
		this.superType = superType;

		mergedInterfaces = new TreeSet<>(interfaces);
		Set<TypeReference> superInterfaceReferrences = superType.getInterfaces();

		mergedInterfaces.addAll(superInterfaceReferrences);
		mergeMethodsAndProperties(superType);
	}

	private void addSuperTypeAsInterfaceIfNecessary(Type superType) {
		if (superType.isInterface()) {
			XMLTypeReference reference = new XMLTypeReference();
			reference.setType(superType);
			if (mergedInterfaces == null) {
				mergedInterfaces = new TreeSet<>();
			}
			mergedInterfaces.add(reference);
		}
	}

	private void mergeMethodsAndProperties(Type superType) {
		if (mergedMethods == null) {
			this.mergedMethods = new TreeSet<>(methods);
		}
		if (mergedProperties == null) {
			this.mergedProperties = new TreeSet<>(properties);
		}
		if (superType == null) {
			return;
		}
		Set<Method> superMethods = superType.getMethods();
		this.mergedMethods.addAll(superMethods);
		/* register super methods and reason */
		ReasonImpl superTypeReason = new ReasonImpl();
		superTypeReason.setSuperType(superType);
		for (Method sm : superMethods) {
			elementReasons.put(sm, superTypeReason);
		}

		Set<Property> superProperties = superType.getProperties();
		this.mergedProperties.addAll(superProperties);
		/* register super properties and reason */
		for (Property sm : superProperties) {
			elementReasons.put(sm, superTypeReason);
		}
	}

	@Override
	public boolean isImplementingInterface(String type) {
		if (type == null) {
			return false;
		}
		Set<TypeReference> interfacesToInspect = getInterfaces();
		for (TypeReference ref : interfacesToInspect) {
			Type interfaceType = ref.getType();
			if (interfaceType == null) {
				/* should not happen, but... */
				continue;
			}
			String name = interfaceType.getName();
			if (type.equals(name)) {
				return true;
			}
			boolean subtypeDoesImplement = interfaceType.isImplementingInterface(type);
			if (subtypeDoesImplement) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isDescendantOf(String type) {
		if (type == null) {
			return false;
		}
		if (superType == null) {
			return false;
		}
		if (type.equals(superType.getName())) {
			return true;
		}
		return superType.isDescendantOf(type);
	}

	@Override
	public Set<Property> getDefinedProperties() {
		return properties;
	}

	@Override
	public Set<Method> getDefinedMethods() {
		return methods;
	}

	@Override
	public boolean isInterface() {
		/* jaxb hack to suppress xyz="false" attributes */
		if (isInterface == null) {
			return false;
		}
		return isInterface.booleanValue();
	}

	public Set<TypeReference> getInterfaces() {
		if (mergedInterfaces != null) {
			return mergedInterfaces;
		}
		return interfaces;
	}

	@Override
	public int compareTo(Type o) {
		if (o == null) {
			return 1;
		}
		String otherName = o.getName();
		if (otherName == null) {
			if (this.name == null) {
				return 0;
			}
			return 1;
		}
		if (this.name == null) {
			return 1;
		}
		int compare = this.name.compareTo(otherName);
		return compare;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((isInterface != null && isInterface.booleanValue()) ? 1231 : 1237);
		result = prime * result + ((interfaces == null) ? 0 : interfaces.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((properties == null) ? 0 : properties.hashCode());
		result = prime * result + ((superTypeAsString == null) ? 0 : superTypeAsString.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XMLType other = (XMLType) obj;
		if (isInterface != other.isInterface)
			return false;
		if (interfaces == null) {
			if (other.interfaces != null)
				return false;
		} else if (!interfaces.equals(other.interfaces))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (methods == null) {
			if (other.methods != null)
				return false;
		} else if (!methods.equals(other.methods))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (properties == null) {
			if (other.properties != null)
				return false;
		} else if (!properties.equals(other.properties))
			return false;
		if (superTypeAsString == null) {
			if (other.superTypeAsString != null)
				return false;
		} else if (!superTypeAsString.equals(other.superTypeAsString))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public boolean isDocumented() {
		/* workaround for JAXM preventing to: atribute="false" */
		return partOfGradleDSLDocumentation != null && partOfGradleDSLDocumentation.booleanValue();
	}

	public void setDocumented(boolean partOfGradleDSLDocumentation) {
		if (!partOfGradleDSLDocumentation) {
			this.partOfGradleDSLDocumentation = null;
		} else {
			this.partOfGradleDSLDocumentation = Boolean.TRUE;
		}
	}

}
