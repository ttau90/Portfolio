# SVM Classifier

This script was developed as part of the Safe Cities Capstone Project

This script was used to create a classifier that would classify tweets as police or non-police related.
It reads in a json file, where each row is a single tweet that has been labeled, and trains a tf-idf weighted classifier.
The classifier is saved as pickles for future use.

To label tweets, the program reads a json file of unlabeled tweets, and uses the previously trained classifier to predict the label.

The script also can count the number of times each hashtag or twitter handle appears in either unlabeled, or labeled tweets, broken-down by category.