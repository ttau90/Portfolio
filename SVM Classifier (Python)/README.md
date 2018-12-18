# SVM Classifier

### Project Background
This script was developed as part of the Safe Cities Capstone Project.  The objective of this project was to explore whether Twitter could be utilized to understand how much trust the community had in their local police department.

Tweets were collected from three sources, Twitter's Gardenhose, Firehose, and using Twitter's streaming API.  Only a subset of cities were selected to be in scope of this project.

In order measure trust, we decided use sentiment as a proxy.  The (reasonable but untested) theory was that people generally will trust others if they have a positive sentiment regarding the other party.

My main contribution for this project was creating a classifier that would classify tweets as either police related or non-police related.  In addition, hashtags and twitter handles were extracted from each tweet 


### Script Details
This script was used to create a classifier that would classify tweets as police or non-police related.
It reads in a json file, where each row is a single tweet that has been labeled, and trains a tf-idf weighted classifier.
The classifier is saved as pickles for future use.

To label tweets, the trained classifier reads a json file of unlabeled tweets, and uses the previously trained classifier to predict the label.

The script also can count the number of times each hashtag or twitter handle appears in either unlabeled, or labeled tweets, broken-down by category of police and non-police related.  The hashtag and twitter handle counts for each kind of tweet was used for additional data analysis to understand if there are particular influencers or hashtag themes.