package de.jcup.egradle.core.parser;

public enum TokenType {

	UNKNOWN,
	
	COMMENT__SINGLE_LINE,
	
	COMMENT__MULTI_LINE,
	
	TASK, 
	
	GSTRING, 
	
	STRING,
	
	BRACE_OPENING, 
	
	BRACE_CLOSING
	
}