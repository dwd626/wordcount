package wordcount;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WordCount {
	/** map to hold the predefined words and their counts */
	private Map<String, Integer> predefinedWords;
	
	/** set to collect characters that appear in predefined words (other than alpha letters) */
	private Set<Character> predefinedChars;
	
	public WordCount() {
		predefinedWords = new HashMap<>();
		predefinedChars = new HashSet<>();
	}
	
	/**
	 *  In order to handle potential predefined words such as "chat-GPT",
	 *  some non-alpha characters are considered part of a word.
	 *  Since this information is not known until after scanning the predefined words,
	 *  it's not optimal to define delimiting characters beforehand.
	 *  
	 *  When processing the text file, Scan each character to get the words.
	 *  An alternative is to use String.split().
	 *  ascii: A-Z 65-90, a-z 97-122
	 *  
	 *  Testing with a 25M word file showed scanning chars takes 60% less time than String.split().
	 *  
	 * @param textFile
	 * @param listFile
	 * @return
	 */
	public Map<String, Integer> countWords(String textFile, String listFile) {
		readPredefinedWords(listFile);
		if (predefinedWords.size() <= 0) return predefinedWords;

		BufferedReader r = null;
		String line;
		long startTime;
		try {
			StringBuilder buf = new StringBuilder(256);
			int charCounts = 0;
			
			startTime = System.currentTimeMillis();
			r = new BufferedReader(new FileReader(textFile));
			while ((line = r.readLine()) != null) {
				charCounts += line.length();
				for (char c: line.toCharArray()) {
					if (c > 64 && c < 91) buf.append(c);
					else if (c > 96 && c < 123) buf.append(Character.toUpperCase(c));
					else if (predefinedChars.contains(c)) buf.append(c);
					else if (buf.length() > 0) {
						addCountFor(buf.toString());
						buf = new StringBuilder(256);						
					}
				}
				if (buf.length() > 0) {
					addCountFor(buf.toString());
					buf = new StringBuilder(256);
				}
			}
			r.close();
			System.out.println("predefined characters: " + predefinedChars);
			System.out.println("Word file size: " + charCounts);
			System.out.println("Total time (ms): " + (System.currentTimeMillis() - startTime));

		} catch (IOException e) {
			System.out.println("exception: " + e);
			System.exit(1);
		} finally {
			try {
				r.close();
			} catch (Exception e) {
				System.out.println("exception closing file: " + e);
				System.exit(1);
			}
		}
		return predefinedWords;
	}
	
	/**
	 *  Read predefined words and put them into a Map. Up to 10K entries.
	 *  Take into considerations the words with a non-alpha character, such as "chat-GPT".
	 *  ascii: A-Z 65-90, a-z 97-122
	 * @param listFile
	 */
	public void readPredefinedWords(String listFile) {
		BufferedReader r = null;
		String line;
		long startTime;

		try {
			r = new BufferedReader(new FileReader(listFile));
			while ((line = r.readLine()) != null) {
				// the line adds the word to the map of predefined words
				predefinedWords.put(line.toUpperCase(), 0);
				
				// In the predefined word file, each line is one word.
				// This block collects special characters that may be part of a predefined word
				// such as "Levi's", "chat-GPT"
				for (char c: line.toCharArray()) {
					if (c < 65 || (c > 90 && c < 97) || c > 122) {
						predefinedChars.add(c);
					}
				}
			}
		} catch (IOException e) {
			System.out.println("exception: " + e);
			System.exit(1);
		} finally {
			try {
				r.close();
			} catch (Exception e) {
				System.out.println("exception closing reader: " + e);
				System.exit(1);
			}
		}
	}
	
	public void addCountFor(String s) {
		Integer n = predefinedWords.get(s);
		if (n != null) {
			predefinedWords.put(s, n+1);
		}
	}
}
