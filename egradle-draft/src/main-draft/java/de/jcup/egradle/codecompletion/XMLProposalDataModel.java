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
 package de.jcup.egradle.codecompletion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "model")
public class XMLProposalDataModel {

	private static final int DIVE_MAXIMUM = 1000;

	@XmlAttribute(name = "id")
	String id;
	
	private boolean prepared;

	public String getId() {
		return id;
	}

	/**
	 * Ensure preparation is done. Means calculation of container per path is done. This is only necessary when model is initial loaded and containers are relevant!
	 * @throws PreparationException
	 */
	void ensurePrepared() throws PreparationException{
		if (!prepared){
			prepare();
		}
	}
	
	private Map<String, Set<XMLProposalContainer>> containerMap = new TreeMap<>();
	
	/**
	 * Returns a set containing all suitable {@link XMLProposalContainer} instances
	 * @param path
	 * @return set of proposal elements, never <code>null</code>
	 * @throws PreparationException 
	 */
	public Set<XMLProposalContainer> getContainersByPath(String path) throws PreparationException{
		ensurePrepared();
		Set<XMLProposalContainer> set = containerMap.get(path);
		if (set==null){
			return Collections.emptySet();
		}
		return set;
	}

	private void prepare() throws PreparationException {
		prepared=true;
		/* calculate IDs */
		for (XMLProposalData data: proposals){
			List<XMLProposalRootPathEntry> rootPathEntries = data.getContext().getRootPathEntries();
			if (rootPathEntries.isEmpty()){
				/* create as fall back standard root path */
				XMLProposalRootPathEntry rootPathEntry = new XMLProposalRootPathEntry();
				rootPathEntry.path="";
				rootPathEntries.add(rootPathEntry);
			}
			calculateAndAddPathesToMap(data, null, rootPathEntries, data.getElements(),data.getValues(), 0);
		}
	}

	private void calculateAndAddPathesToMap(XMLProposalData data ,StringBuilder parentSB, List<XMLProposalRootPathEntry> rootPathEntries, List<XMLProposalElement> elements, List<XMLProposalValue> values, int dive) throws PreparationException {
		if (dive==0){
			/* first call so add root parts as synthetic containers for given elements!*/
			for (XMLProposalRootPathEntry entry: rootPathEntries){
				SyntheticXMLProposalContainer container = new SyntheticXMLProposalContainer();
				container.path=entry.path;
				container.elements=elements;
				container.values=values;
				String uid = entry.path;
				registerContainer(container, uid);
			}
			
		}
		dive++;
		if (dive>DIVE_MAXIMUM){
			/* to avoid infinite loops or stack overflow exceptions we hardly prevent this:*/
			throw new PreparationException("Dive greater than maximum:"+DIVE_MAXIMUM);
		}
		for (XMLProposalElement element:elements){
			StringBuilder sb = new StringBuilder();
			if (parentSB!=null){
				sb.append(parentSB);
				sb.append('.');
			}
			sb.append(element.getName());
			calculateAndAddPathesToMap(data, sb, rootPathEntries, element.getElements(), element.getValues(), dive);
			for (XMLProposalRootPathEntry rootPath : rootPathEntries){
				StringBuilder uidSb = new StringBuilder();
				uidSb.append(rootPath.path);
				uidSb.append(sb);
				String uid = uidSb.toString();
				
				registerContainer(element, uid);
			}
		}
	}

	private void registerContainer(XMLProposalContainer element, String uid) {
		Set<XMLProposalContainer> set = containerMap.get(uid);
		if (set == null){
			set = new LinkedHashSet<>();
			containerMap.put(uid, set);
		}
		set.add(element);
	}

	@XmlElement(name = "proposal")
	private List<XMLProposalData> proposals = new ArrayList<>();

	public List<XMLProposalData> getProposals() {
		return proposals;
	}

	public static class XMLProposalData {
	
		@XmlElement(name = "context")
		private XMLProposalContext context = new XMLProposalContext();
	
		public XMLProposalContext getContext() {
			return context;
		}
	
		@XmlElement(name = "element")
		private List<XMLProposalElement> elements = new ArrayList<>();
	
		public List<XMLProposalElement> getElements() {
			return elements;
		}
	
		@XmlElement(name = "value")
		private List<XMLProposalValue> values = new ArrayList<>();
	
		public List<XMLProposalValue> getValues() {
			return values;
		}
	
	}

	public static class XMLProposalContext {

		@XmlElement(name = "rootPathEntry")
		private List<XMLProposalRootPathEntry> rootPathEntries = new ArrayList<>();

		/**
		 * @return root path entries, never <code>null</code>
		 */
		public List<XMLProposalRootPathEntry> getRootPathEntries() {
			return rootPathEntries;
		}

	}

	public static class XMLProposalRootPathEntry {
		@XmlValue
		String path;

		public String createRootPathEntrygetPath() {
			return path;
		}
	}

	/**
	 * Represents an item of outline view inside xml
	 * 
	 * @author albert
	 *
	 */
	public static class XMLProposalElement implements XMLProposal,XMLProposalContainer {
		
		@XmlAttribute(name = "name", required=true)
		String name;

		public String getName() {
			return name;
		}

		@XmlElement(name = "element")
		private List<XMLProposalElement> elements = new ArrayList<>();
		
		public List<XMLProposalElement> getElements() {
			return elements;
		}

		@XmlElement(name = "value")
		private List<XMLProposalValue> values = new ArrayList<>();

		@XmlAttribute(name = "code")
		String code;

		public String getCode() {
			if (code==null) {
				/* If code is not explicit set in XML we lazily create a fall back code to name */
				code= name +" {\n\t$cursor\n}\n";
				/* FIXME albert,08.01.2017: recreate outline model as soon as possible when code completion triggered -worng place here but necessary marker */
			}
			return code;
		}
		
		public List<XMLProposalValue> getValues() {
			return values;
		}


		@XmlElement(name = "description")
		String description;
		

		@Override
		public String getDescription() {
			return description;
		}

	}

	/**
	 * Represents a child entry (a leave)
	 * 
	 * @author albert
	 *
	 */
	public static class XMLProposalValue implements XMLProposal {

		@XmlAttribute(name = "name")
		String name;

		
		@Override
		public String getName() {
			return name;
		}
		
		@XmlAttribute(name = "code")
		String code;

		public String getCode() {
			return code;
		}

		@XmlAttribute(name = "maxOccurrence")
		Integer maxOccurrence ;

		public Integer getMaxOccurrence() {
			return maxOccurrence;
		}

		@XmlElement(name = "description")
		String description;

		@Override
		public String getDescription() {
			return description;
		}

	}
	
	/**
	 * Synthetic xml prosal container - e.g. for root path entries not available in model but necessary to find...
	 * @author albert
	 *
	 */
	public static class SyntheticXMLProposalContainer implements XMLProposalContainer{

		private String path;
		private List<XMLProposalElement> elements = new ArrayList<>();
		private List<XMLProposalValue> values = new ArrayList<>();

		@Override
		public List<XMLProposalElement> getElements() {
			return elements;
		}

		@Override
		public List<XMLProposalValue> getValues() {
			return values;
		}

		@Override
		public String toString() {
			return "SyntheticXMLProposalContainer [path=" + path + ", values=" + values + ", elements=" + elements
					+ "]";
		}
		
	}

	public static interface XMLProposal {
		
		/**
		 * Returns display name (and also the name used for UID creation!)
		 * @return name
		 */
		public String getName();
		
		/**
		 * Returns code of proposal
		 * @return code
		 */
		public String getCode();
		
		/**
		 * Returns description of proposal
		 * @return description
		 */
		public String getDescription();
	}
	
	public static interface XMLProposalContainer {
		
		/**
		 * Return elements, never <code>null</code>
		 * @return lements, never <code>null</code>
		 */
		public List<XMLProposalElement> getElements();
		
		/**
		 * Return values, never <code>null</code>
		 * @return values, never <code>null</code>
		 */
		public List<XMLProposalValue> getValues();
	}
	
	public static class PreparationException extends Exception{

		private static final long serialVersionUID = 1L;

		public PreparationException(String message) {
			super(message);
		}

		
	}

}
