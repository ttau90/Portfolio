# Hadoop - Word Prediction

This program uses Hadoop to calculate the probability of a word appearing, given the previous n-words.
The probabilities were trained on Wikipedia articles

WordPrediction.java parses the Wikipedia articles and emits a key-value pair where the key is an n-gram.
PhraseProbability.java takes the generated n-grams and calculates the probabilities of a word appearing
given the prior n-words, and stores the probability into an hbase database.

BonusPredictor.java and BonusProbability.java are similar to the Word and Phrase programs, except instead
of predicting words given the previous n-words, the Bonus programs will predict the word given the previous
n-characters.

Word prediction is essentially a phrase completion, while Bonus prediction is word completion.