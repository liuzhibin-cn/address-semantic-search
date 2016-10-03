package com.rrs.rd.address.similarity;

public class NoHistoryDataException extends RuntimeException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	public NoHistoryDataException(String message){
		super(message.startsWith("No history data") ? message : "No history data for: " + message);
	}
}