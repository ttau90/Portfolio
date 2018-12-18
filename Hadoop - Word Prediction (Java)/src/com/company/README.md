# Hadoop - Word Prediction

### Project Objectives
The objective for this project is to use Hadoop to process a large corpus of Wikipedia articles to create word prediction and word completion, given the previous n-words or previous n-characters.

Unfortunately I do not have the original project instructions so some details may be vague.

### WordPrediction.java
This class would read in a corpus of Wikipedia articles and output a key-value pair where the key is n-grams and the value is the count of how many times that n-gram appeared.  For example if the input is:

`The quick brown fox jumped over the quick brown dog.`

then a sample output of this program would be:

```
the quick brown fox	1
the quick brown dog	1
the quick brown	2
the quick	2
brown fox	1
brown dog	1
brown	2
fox	1
dog	1
the 2
```

This output is then the input of PhraseProbability.java.

#### PhraseProbability.java
This class would read the output of WordPrediction.java and calculate the probability of all words that would appear next, given a specific n-gram.

For example, if the n-gram is "the quick brown" then the probability of all words that appear after that phrase is "dog" with a 50% probability and "fox" with a 50% probability.

The n-gram and probability of all words appearing after the n-gram is stored in an HBase database.

#### BonusPredictor.java nd BonusProbability.java
These two classes worked in a similar manner to the WordPrediction and PhraseProbability classes.  The only difference is that instead of predicting the next words given an n-gram of words, these classes attempted to implement word completion, given an n-gram of characters

#### Grading Details
The autograder for this assignment most likely examined the contents of the HBase database and looked up the values for n-grams and compared it against the correct answer.