package com.Assignment.sample;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
  *   Main class.
  */
public class FairBilling  {
	
	public static final String START_ACTION = "Start";
	public static final String END_ACTION = "End";
	
	protected List<String> loadFileToList(String fileName) {
		
		List<String> lines = Collections.emptyList(); 
		try {
			lines = Files.readAllLines(Paths.get(fileName), StandardCharsets.UTF_8);
		} catch (NoSuchFileException n) {
			throw new FairBillingException("No such file " + fileName, n);
		} catch (IOException e) {
			throw new FairBillingException("File error " + fileName, e);
		} 
		return lines; 
	}

	protected List<LineInPieces> breakUpAllTheLines(List<String> lines) {
		
		List<LineInPieces> lineInPiecesList = new ArrayList<>();
		
		
		for (String line : lines) {
			
			LineInPieces lineInPieces = new LineInPieces();
			lineInPieces = breakUpLine(line);			

	    	if (!START_ACTION.equals(lineInPieces.getAction()) && !END_ACTION.equals(lineInPieces.getAction())) {
	    		System.err.println("Invalid action: action not set to either Start or End in line " + line + " - skipped");
	    		lineInPieces.setValid(false);
	    	}

			// Ignore invalid lines
			if (lineInPieces.isValid()) {
				lineInPiecesList.add(lineInPieces);
			}
		}
		
		return lineInPiecesList;
	}

	
	
	protected Map<String, List<CustSession>> processLines(List<LineInPieces> lines) {

		Map<String, List<CustSession>> userSessionMap = new LinkedHashMap<>();
		
		for (LineInPieces line : lines) {			
			List<CustSession> userSessionList = userSessionMap.get(line.getUserid());				
			
			userSessionList = processLine(line, userSessionList);
			userSessionMap.put(line.getUserid(), userSessionList);
		}
		
		return userSessionMap;
	}
	
	
	protected List<CustSession> processLine(LineInPieces line, List<CustSession> userSessionList) {
		
		if (userSessionList == null) {
			userSessionList = new ArrayList<CustSession>();
		}

		LocalTime lineTime = LocalTime.parse(line.getHours() + ":" 
				+ line.getMinutes() + ":" 
				+ line.getSeconds());
		
		if (START_ACTION.equals(line.getAction()) ) {
			CustSession userSession = new CustSession(line.getUserid());
			userSession.setStartTime(lineTime);
			userSessionList.add(userSession);
			return userSessionList;
		}

		
		for (CustSession userSession: userSessionList) {
			
			if (userSession.getEndTime() == null) {
				userSession.setEndTime(lineTime);
				return userSessionList;
			}
		}
		
		// Otherwise just add a new End record 
		CustSession userSession = new CustSession(line.getUserid());
		userSession.setEndTime(lineTime);
		userSessionList.add(userSession);
		return userSessionList;
	}
	
	
	protected LineInPieces breakUpLine(String line) {
		
		LineInPieces lineInPieces = new LineInPieces();
		lineInPieces.setValid(false);
		
		if (line == null || line.isEmpty()) {
			return lineInPieces;
		}

		String patternString = "^(\\d\\d):(\\d\\d):(\\d\\d) (.*) (.*)$";
        Pattern pattern = Pattern.compile(patternString);		
        Matcher matcher = pattern.matcher(line);
        
    	while(matcher.find()) {
            lineInPieces.setHours( matcher.group(1) );
            lineInPieces.setMinutes( matcher.group(2) );
            lineInPieces.setSeconds( matcher.group(3) );
            lineInPieces.setUserid( matcher.group(4) );
            lineInPieces.setAction( matcher.group(5) );
        }
    	
    	lineInPieces.setValid(matcher.matches());
    	
        return lineInPieces;
	}
	
	
	protected List<FinalResult> processFileAsList(List<String> lines) {
		
		if (lines == null || lines.isEmpty()) {
			return new ArrayList<>();
		}
		
    	List<LineInPieces> piecesList = breakUpAllTheLines(lines);
    	
    	// Get first and last times in file.
    	LocalTime firstTimeInFile = null;
    	LocalTime lastTimeInFile = null;
    	if (piecesList.size() > 0) {
    		firstTimeInFile = 
    				LocalTime.parse(piecesList.get(0).getHours() + ":" 
    						+ piecesList.get(0).getMinutes() + ":" 
    						+ piecesList.get(0).getSeconds());
    		lastTimeInFile = 
    				LocalTime.parse(piecesList.get(piecesList.size()-1).getHours() + ":" 
    						+ piecesList.get(piecesList.size()-1).getMinutes() + ":" 
    						+ piecesList.get(piecesList.size()-1).getSeconds());
    				
    	}
    	
    	Map<String, List<CustSession>> map = processLines(piecesList);
    	
    	List<FinalResult> results = new ArrayList<>();
    	
    	for (String userid : map.keySet()) {
    		int total = 0;
    		int numberOfSessions = 0;
    		for (CustSession us : map.get(userid)) {
    			numberOfSessions++;
    			if (us.getStartTime() == null) { 
    				us.setStartTime(firstTimeInFile);
    			}
    			if (us.getEndTime() == null) { 
    				us.setEndTime(lastTimeInFile);
    			}
    			total += + Duration.between(us.getStartTime(),us.getEndTime()).getSeconds();
    		}
    		results.add( new FinalResult(userid, numberOfSessions, total) );
    	}
    	
    	return results;
	}
	
    public static void main( String[] args ) {

    	try {
    		
	    	if (args.length < 1) {
	    		System.err.println("Wrong number of arguments " + args.length + ": syntax is \nFairBilling <path to file>");
	    	}
	    
	    	String fileName = args[args.length - 1];
	    	
	    	FairBilling fairBilling = new FairBilling();
	    	List<String> lines = fairBilling.loadFileToList(fileName);

	    	List<FinalResult> results = fairBilling.processFileAsList(lines);
	    	
	    	for (FinalResult result : results) {
	    		System.out.println(result.getUserId() + " " + result.getNumberOfSessions() + " " + result.getBillableTimeInSeconds());
	    	}
	    	
    	} catch(FairBillingException fb) {
    		System.err.println("Unexpected error: " + fb.getMessage());
    	}
    }
}
