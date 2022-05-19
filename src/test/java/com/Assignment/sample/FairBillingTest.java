package com.Assignment.sample;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.Assignment.sample.FairBilling;
import com.Assignment.sample.FairBillingException;
import com.Assignment.sample.LineInPieces;
import com.Assignment.sample.FinalResult;
import com.Assignment.sample.CustSession;

public class FairBillingTest {

	private FairBilling fairBilling;
	
	@Before
	public void setup() {
		fairBilling = new FairBilling();
	}

	@Test
    public void testLoadFile() {	
		List<String> file = fairBilling.loadFileToList("test.log");		
        assertFalse( file.isEmpty() );
        assertEquals("Test file loaded correctly", 11, file.size());
    }
	
	@Test
	public void testProcessLineStartingWithNothing() {
		List<CustSession> userList = null;
		userList = fairBilling.processLine(new LineInPieces("14","02","11","FRED","Start"), userList);
		assertNotNull(userList);
		assertEquals(1, userList.size());
		assertEquals("UserSession [userId=FRED, startTime=14:02:11, endTime=null]", userList.get(0).toString());
	}

	// Start with one, where start date is already set, action is end
	@Test
	public void testProcessLineOneRowStartThisIsEnd() {
		
		List<CustSession> userList = new ArrayList<>();
		CustSession userSession = new CustSession("FRED", LocalTime.of(14, 01, 56), null);
		userList.add(userSession);
		
		userList = fairBilling.processLine(new LineInPieces("14","02","11","FRED","End"), userList);
		assertNotNull(userList);
		assertEquals(1, userList.size());
		assertEquals("UserSession [userId=FRED, startTime=14:01:56, endTime=14:02:11]", userList.get(0).toString());
	}

	// Start with one, where end date is already set, action is start
	@Test
	public void testProcessLineOneRowEndThisIsStart() {
		
		List<CustSession> userList = new ArrayList<>();
		CustSession userSession = new CustSession("FRED", LocalTime.of(14, 01, 56), LocalTime.of(14, 02, 01));
		userList.add(userSession);
		
		userList = fairBilling.processLine(new LineInPieces("14","02","11","FRED","Start"), userList);
		assertNotNull(userList);
		assertEquals(2, userList.size());
		assertEquals("UserSession [userId=FRED, startTime=14:02:11, endTime=null]", userList.get(1).toString());
	}

	@Test
	public void testProcessLineCheckEndMatchesFirstStart() {
		
		List<CustSession> userList = new ArrayList<>();
		CustSession existingEnd = new CustSession("FRED", LocalTime.of(14, 02, 01), null);
		userList.add(existingEnd);
		CustSession existingEnd2 = new CustSession("FRED", LocalTime.of(14, 03, 01), null);
		userList.add(existingEnd2);
		
		userList = fairBilling.processLine(new LineInPieces("14","02","11","FRED","End"), userList);
		assertNotNull(userList);
		assertEquals(2, userList.size());
		assertEquals("UserSession [userId=FRED, startTime=14:02:01, endTime=14:02:11]", userList.get(0).toString());
		assertEquals("UserSession [userId=FRED, startTime=14:03:01, endTime=null]", userList.get(1).toString());
	}

	//---------------------------------------------
		
	@Test
	public void testProcessFileOneEndOneRow() {
		List<LineInPieces> pList = Arrays.asList(
		       new LineInPieces("14","02","11","FRED","End"));
		
		Map<String, List<CustSession>> map = fairBilling.processLines(pList);
		assertNotNull(map);
		assertEquals(1, map.keySet().size());
		assertEquals(1, map.get("FRED").size());
		assertEquals("[UserSession [userId=FRED, startTime=null, endTime=14:02:11]]", map.get("FRED").toString());
	}
	
	@Test
	public void testProcessFileTwoEndsTwoRows() {
		List<LineInPieces> pList = Arrays.asList(
		       new LineInPieces("14","02","11","FRED","End"),
		       new LineInPieces("14","02","12","FRED","End"));
		
		Map<String, List<CustSession>> map = fairBilling.processLines(pList);
		assertEquals(2, map.get("FRED").size());
	}
	
	@Test
	public void testProcessFileTwoStartsTwoRows() {
		List<LineInPieces> pList = Arrays.asList(
		       new LineInPieces("14","02","11","FRED","Start"),
		       new LineInPieces("14","02","12","FRED","Start"));
		
		Map<String, List<CustSession>> map = fairBilling.processLines(pList);
		assertEquals(2, map.get("FRED").size());
	}
	
	//---------------------------------------------
	
	@Test
	public void testProcessNullFileAsList() {
		List<FinalResult> results = fairBilling.processFileAsList(null);
		assertEquals(0, results.size());
	}

	@Test
	public void testProcessEmptyFileAsList() {
		List<FinalResult> results = fairBilling.processFileAsList(new ArrayList<>());
		assertEquals(0, results.size());
	}
	
	@Test
	public void testPFALWithOneRow() {
		List<FinalResult> results = fairBilling.processFileAsList(Arrays.asList("14:00:00 FRED Start"));
		assertEquals(1, results.size());
		assertEquals("UserResult [userId=FRED, numberOfSessions=1, billableTimeInSeconds=0]", results.get(0).toString());
	}
	
	@Test
	public void testPFALWithTwoRows() {
		List<FinalResult> results = fairBilling.processFileAsList(
				Arrays.asList("14:00:00 FRED Start",
						      "14:00:01 FRED End"));
		assertEquals(1, results.size());
		assertEquals("UserResult [userId=FRED, numberOfSessions=1, billableTimeInSeconds=1]", results.get(0).toString());
	}
	
	@Test
	public void testPFALWithThreeRows() {
		List<FinalResult> results = fairBilling.processFileAsList(
				Arrays.asList("14:00:00 FRED Start",
						      "14:00:01 FRED End",
						      "14:00:02 FRED End"));
		assertEquals(1, results.size());
		assertEquals("UserResult [userId=FRED, numberOfSessions=2, billableTimeInSeconds=3]", results.get(0).toString());
	}
	
	/** This verifies that the first Start is matched with an end, not the last */
	@Test
	public void testPFALGotchaWithAliceFile() {
		List<FinalResult> results = fairBilling.processFileAsList(
				Arrays.asList("14:02:03 ALICE99 Start",
						      "14:02:34 ALICE99 End",
						      "14:02:58 ALICE99 Start",
						      "14:03:33 ALICE99 Start",
						      "14:03:35 ALICE99 End",
						      "14:04:05 ALICE99 End",
						      "14:04:23 ALICE99 End",
						      "14:04:41 CHARLIE Start"));
		assertEquals(2, results.size());
		assertEquals("UserResult [userId=ALICE99, numberOfSessions=4, billableTimeInSeconds=240]", results.get(0).toString());
	}

	@Test
	public void testPFALWithAllLinesInvalid() {
		List<FinalResult> results = fairBilling.processFileAsList(
				Arrays.asList("XXXX",
						      "XXX",
						      "SSS"));
		assertEquals(0, results.size());
	}

}
