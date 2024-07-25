package wordcount;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Write a program that reads a file and finds matches against a predefined set of words. There can be up to 10K entries in the list of predefined words. The output of the program should look something like this: 

Predefined word           Match count 
FirstName                           3500 
LastName                           2700 
Zipcode                                1601 

AI	1536 [1234 5678 other words]
starts	1536
Detect	7168
Unary	1024
I'm	0
plus	1536
Name	22528

Design:

There are two steps:
1. read the predefined words
2. match the input file against the predefined words

Considerations:
- the size of the predefined words of 10K (each word max 256), and input file of 20M, in memory HashMap and StringBuilder suffice.
- the predefined words may contain non-alpha characters such as "'-", but this is not known before reading the file.
- run this as a standalone. for i/o, use simple System.in and System.out. All this may change in a different context.
- to parse the input file, I compared String.split() and character scanning and chose the faster one.

Additional changes:

For each predefined word, find matches for the second set of words (in a different input file).
The use case for this is to find match for a list of words in the preceding vicinity (50 characters) of the predefined words.

Considerations and design changes:
- Line breaks are counted in the 50 character boundary
- The entire content can fit into a String
- Since more info about the predefined words are collected, instead of a Map that contains only the count,
	I use a data structure to wrap all the info
- 
 */
public class WordCount {
	final int LOOKBACK_STEP = 50;
	
	/** map to hold the original word (maintain original case) to the upper case. */
	private Map<String, String> originalWords;
	
	/** map to hold the upper case word to the PredefinedWord object. */
	private Map<String, PredefinedWord> predefinedWords;
	
	/** map to hold the predefined words and their counts */
	private List<String> secondaryWords;

	/** set to collect characters that appear in predefined words (other than alpha letters) */
	private Set<Character> predefinedChars;
	
	public WordCount() {
		originalWords = new HashMap<>();
		predefinedWords = new HashMap<>();
		predefinedChars = new HashSet<>();
		secondaryWords = new ArrayList<>();
	}
	
	public static void main(String[] args) {
		String wordFile, wordFile2, textFile;
		
		if (args.length == 3) {
			wordFile = args[0];
			wordFile2 = args[1];
			textFile = args[2];
		} else {
			Scanner reader = new Scanner(System.in);
			System.out.println("Please provide two file names. Or enter the predefined word file name (full path):");
			wordFile = reader.nextLine();
			System.out.println("Enter the secondary word file name (full path):");
			wordFile2 = reader.nextLine();
			System.out.println("Enter the name of the file to be processed (full path):");
			textFile = reader.nextLine();
			reader.close();
		}
		
		WordCount app = new WordCount();
		app.countWords(textFile, wordFile, wordFile2);
		System.out.println(app.output());
	}
	
	public String output() {
		StringBuilder buf = new StringBuilder("\nPredefined word\t\tMatch count\tSecondary words\n");
		for (PredefinedWord wd: predefinedWords.values()) {
			buf.append(wd.toString()).append("\n");
		}
		return buf.toString();
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
	 * @param listFile2
	 * @return
	 */
	public void countWords(String textFile, String listFile, String listFile2) {
		readPredefinedWords(listFile);
		readSecondaryWords(listFile2);
		if (predefinedWords.size() <= 0) return;

		BufferedReader r = null;
		String line;
		long startTime;
		try {
			// read the input text file
			int inputFileSize = 0;			
			StringBuilder buf = new StringBuilder();
			r = new BufferedReader(new FileReader(textFile));
			while ((line = r.readLine()) != null) {
				inputFileSize += line.length();
				buf.append(line);
			}
			r.close();
			String fileContent = buf.toString();
			int fileLength = fileContent.length();
			System.out.println("input file length: " + fileLength);

			// parse the input file to get predefined word count, as well as lookback
			startTime = System.currentTimeMillis();
			buf = new StringBuilder(256);

			for (int i = 0; i < fileLength; i++) {
				char c = fileContent.charAt(i);
				
				if (c > 64 && c < 91) buf.append(c);
				else if (c > 96 && c < 123) buf.append(Character.toUpperCase(c));
				else if (predefinedChars.contains(c)) buf.append(c);
				else if (buf.length() > 0) {
					lookBackMatch(buf.toString(), fileContent, i);
					buf = new StringBuilder(256);
				}
			}
			if (buf.length() > 0) {
				lookBackMatch(buf.toString(), fileContent, fileLength - 1);
				buf = new StringBuilder(256);
			}
			
			System.out.println("predefined characters: " + predefinedChars);
			System.out.println("Word file size: " + inputFileSize);
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
	}
	
	/**
	 * Given the token, find match for the secondary words.
	 */
	public void lookBackMatch(String token, String fileContent, int i) {
		if (predefinedWords.containsKey(token)) {
			String lookbackString = fileContent.substring(Math.max(0, (i - LOOKBACK_STEP)), i);
			Set<String> matched = matchedSecondaryWords(lookbackString);
			predefinedWords.get(token).match(matched);
		}
	}
	
	/**
	 * Given the input string, find a match to the secondary words.
	 */
	public Set<String> matchedSecondaryWords(String tmp) {
		Set<String> ans = new HashSet<>();
		for (String s: secondaryWords) {
			if (tmp.indexOf(s) >= 0) {
				ans.add(s);
			}
		}
		return ans;
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
				predefinedWords.put(lineUpper, new PredefinedWord(lineUpper, 0));
				
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
	
	/**
	 * Read secondary words into a Set.
	 * @param listFile
	 */
	public void readSecondaryWords(String listFile) {
		BufferedReader r = null;
		String line;
		try {
			r = new BufferedReader(new FileReader(listFile));
			while ((line = r.readLine()) != null) {
				secondaryWords.add(line);
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

	
	// predefined word, count, and vicinity matched words
	class PredefinedWord {
		String text;
		int count;
		Set<String> secondaryWords;
		
		public PredefinedWord(String s, int c) {
			text = s;
			count = c;
			secondaryWords = new HashSet<>();
		}
		
		public void match(Set<String> strs) {
			count++;
			if (strs != null && !strs.isEmpty()) {
				secondaryWords.addAll(strs);
			}
		}
		
		public String toString() {
			StringBuffer buf = new StringBuffer();
			return buf.append(originalWords.get(text)).append("\t").append(count).append("\t").append(secondaryWords).toString();
		}
	}
}
