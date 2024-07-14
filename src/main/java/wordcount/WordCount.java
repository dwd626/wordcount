package wordcount;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Write a program that reads a file and finds matches against a predefined set of words. There can be up to 10K entries in the list of predefined words. The output of the program should look something like this: 

Predefined word           Match count 
FirstName                           3500 
LastName                           2700 
Zipcode                                1601 

Design:

There are two steps:
1. read the predefined words
2. match the input file against the predefined words

Considerations:
- the size of the predefined words of 10K (each word max 256), and input file of 20M, in memory HashMap and StringBuilder suffice.
- the predefined words may contain non-alpha characters such as "'-", but this is not known before reading the file.
- run this as a standalone. for i/o, use simple System.in and System.out. All this may change in a different context.
- to parse the input file, I compared String.split() and character scanning and chose the faster one.
 
 */
public class WordCount {
	/** map to hold the original word (maintain original case) to the uppercase. */
	private Map<String, String> originalWords;
	
	/** map to hold the predefined words and their counts */
	private Map<String, Integer> predefinedWords;
	
	/** set to collect characters that appear in predefined words (other than alpha letters) */
	private Set<Character> predefinedChars;
	
	public WordCount() {
		originalWords = new HashMap<>();
		predefinedWords = new HashMap<>();
		predefinedChars = new HashSet<>();
	}
	
	public static void main(String[] args) {
		String wordFile, textFile;
		
		if (args.length == 2) {
			wordFile = args[0];
			textFile = args[1];
		} else {
			Scanner reader = new Scanner(System.in);
			System.out.println("Please provide two file names. Or enter the predefined word file name (full path):");
			wordFile = reader.nextLine();
			System.out.println("Enter the name of the file to be processed (full path):");
			textFile = reader.nextLine();
			reader.close();
		}
		
		WordCount app = new WordCount();
		Map<String, Integer> countedWords = app.countWords(textFile, wordFile);
		
		// output the results
		StringBuilder buf = new StringBuilder("\nPredefined word\t\tMatch count\n");
		for (Map.Entry<String, Integer> en: countedWords.entrySet()) {
			buf.append(en.getKey()).append("\t").append(en.getValue()).append("\n");
		}
		System.out.println(buf.toString());
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
		Map<String, Integer> countWords = new HashMap<>();
		for (Map.Entry<String, Integer> en: predefinedWords.entrySet()) {
			countWords.put(originalWords.get(en.getKey()), en.getValue());
		}
		return countWords;
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
				// keep the original words for output purpose
				String lineUpper = line.toUpperCase();
				originalWords.put(lineUpper, line);
				
				// adds the word to the map of predefined words
				predefinedWords.put(lineUpper, 0);
				
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
