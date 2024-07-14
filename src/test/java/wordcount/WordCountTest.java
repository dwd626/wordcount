package wordcount;

import java.util.Map;
import java.util.Scanner;

/**
 * When running the jar file, if two strings are provided in command line, use them as input.
 * Otherwise, prompt the user to input the file names (full path).
 */
public class WordCountTest {
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
}
