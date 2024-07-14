# Usage

wordscount.jar is a runnable jar. It can be run in two ways.

- Run it with the absolute path of the two files: predefined words text, and the text file to be processed.
```
java -jar wordscount.jar C:\Users\anfun\ar\do\words\predefinedWords.txt C:\Users\anfun\ar\do\words\words.txt
```
- Run it without command line parameters. The user will be required to enter the file names at the command prompt.
```
java -jar wordscount.jar
Please provide two file names. Or enter the predefined word file name (full path):
C:\Users\anfun\ar\do\words\predefinedWords2.txt
Enter the name of the file to be processed (full path):
C:\Users\anfun\ar\do\words\words2.txt
predefined characters: [']
Word file size: 110
Total time (ms): 1

Predefined word         Match count
STARTS  0
AI      2
UNARY   0
EXPRESSION      0
DETECT  0
I'M     1
NAME    3
PLUS    0
```

# Test cases

- The command line contains the file names. Expected: the file names are used to load predefined words and process the word data file content.
- The command line does not have the file names. User is prompted to enter the file names. Exptected: user entered file names are read to process the data.
- When prompted to enter a file name, an invalid path is entered. Expected: the application exits with exception. 
- Data file up to 25M size.
