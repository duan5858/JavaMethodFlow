/*
 * � 2009-2015 T-Systems International GmbH. All rights reserved
 * _______________UTF-8 checked via this umlaut: �
 */
package com.sample;


import com.sample2.Rest2;

/**
 * TODO Small and simple description of the type
 *
 * @copyright � 2009-2015 T-Systems International GmbH. All rights reserved
 * @author 122305
 * 
 * @changes 
 *    May 4, 2015: Created
 *
 */
public class Rest {

	public void printBye() {
		System.out.println("Bi");		
		printDone();
		Rest2 rest2 = new Rest2();
		rest2.printBye2();
		
	}		
	
	public void printDone() {
		System.out.println("Done");
	}	
}
