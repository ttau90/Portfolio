#!/opt/python27/bin/python
# -*- coding: utf-8 -*-
#
"""
Created on Fri Feb 24 16:07:35 2017

@author: Tim
"""

import pandas as pd
import nltk, sklearn
import os, glob, sys, getopt
import re
from collections import Counter
import cPickle as pickle
import datetime as dt


class Classify():
    #Read tweets from a json file.  Accepts either a json file or a folder of jsons
    def readFiles(self, inputDir):
        dfList = []  # List of data frames to be concat into one later

        if os.path.isdir(inputDir):
            files = glob.glob(os.path.join(inputDir, "*"))
        else:
            files = [inputDir]

        for f in files:
            df = pd.read_json(f, lines=True)
            dfList.append(df)

        tweets = pd.concat(dfList)
        return tweets

    #Processes a single tweet to remove urls and punctuations, and then performs stemming
    def process(self, text, lemmatizer=nltk.stem.wordnet.WordNetLemmatizer()):
        text = text.lower()

        # Remove hashtagged words
        # text = re.sub(r"#\\S*", "", text)

        # Remove username handles
        # text = re.sub(r"@\\S*", "", text)

        # Remove URLS
        text = re.sub(r"^https?:\/\/.*[\r\n]*", "", text)

        # Remove punctuation
        for i in '!"$%&\'()*+,-./:;<=>?[\\]^_`{|}~':
            text = text.replace(i, "")

        wordList = nltk.word_tokenize(text)

        # Lemmatize the words
        for i in range(len(wordList)):
            try:
                wordList[i] = lemmatizer.lemmatize(wordList[i])
            except:
                wordList[i] == ""

        return filter(None, wordList)

    #Process all tweets in a dataframe
    def processAll(self, tweets, lemmatizer=nltk.stem.wordnet.WordNetLemmatizer()):
        size = float(len(tweets))
        counter = 0
        pTweets = tweets.copy()
        sourceIndex = list(pTweets.columns).index("_source")
        pList = list()
        for row in range(len(pTweets)):
            counter += 1
            if counter % 1000 == 0:
                print "Processing Text " + str(counter / size)
            pList.append(self.process(pTweets.iloc[row, sourceIndex]["text"]))

        pTweets["p_text"] = pList
        # pTweets["p_text"] = pTweets.apply(lambda row: self.process(row["_source"]["text"]), axis = 1)
        return pTweets

    # Removes words that appear only 1 time in all of the training data
    # Must call "processAll" before running getRareWords or results will not be correct
    def getRareWords(self, tweets):
        allWords = []
        [allWords.extend(row) for row in tweets["p_text"]]
        allWords = [word.encode("UTF-8") for word in allWords]

        wordCount = Counter(allWords)
        rareWords = [word for word, count in wordCount.iteritems() if count < 2]

        return sorted(rareWords)

    #Turns the processed tweets into a TFIDF feature matrix
    def createFeatures(self, tweets, rare_words):
        stopWords=nltk.corpus.stopwords.words('english')
        stopWords.extend(rare_words)
        docList = [" ".join(text) for text in tweets["p_text"]]
        tfidf = sklearn.feature_extraction.text.TfidfVectorizer(stop_words = stopWords)
        vectorizer = tfidf.fit_transform(docList)

        return (tfidf, vectorizer)
    
    #Trains an SVM classifier using the TFIDF matrix
    def learn_classifier(self, xTrain, yTrain, kernel="best"):
        if kernel == "best":
            kernel = "linear"

        clf = sklearn.svm.SVC(kernel=kernel)
        svc = clf.fit(xTrain, yTrain)

        return svc

    #Main training function that utilizes the above helper functions to fully train the classifier
    def train(self, tweets, lemmatizer=nltk.stem.wordnet.WordNetLemmatizer()):
        pTweets = self.processAll(tweets, lemmatizer)
        rareWords = self.getRareWords(pTweets)
        tfidf, vectorizer = self.createFeatures(pTweets, rareWords)
        classifier = self.learn_classifier(vectorizer, pTweets["_label"], "best")

        return tfidf, vectorizer, classifier

    #Takes a dataframe of unlabeled tweets, the trained models, and predicts labels
    def predict(self, tfidf, classifier, tweets):
        pTweets = self.processAll(tweets)
        docList = [" ".join(text) for text in pTweets["p_text"]]
        transformed = tfidf.transform(docList)
        predicted = classifier.predict(transformed)
        tweets["p_label"] = predicted

        return tweets

    #This function will count how many times each hashtag and twitter handle
    #appears in the labeled categories of police and non-police tweets
    def count(self, tweets, listSize):
        length = float(len(tweets))
        handleRegex = re.compile(r"@\w*")
        hashtagRegex = re.compile(r"#\w*")

        policeHashDict = dict()
        policeHandleDict = dict()
        noPoliceHashDict = dict()
        noPoliceHandleDict = dict()
        unlabeledHashDict = dict()
        unlabeledHandleDict = dict()

        counter = 0

        hasLabel = False

        if "p_label" in tweets.columns:
            hasLabel = True

        for row in tweets.iterrows():
            if counter % 1000 == 0:
                print "Hashtag counting " + str(counter / length) + " complete"

            text = row[1]["_source"]["text"]
            label = None
            if hasLabel:
                label = row[1]["p_label"]

            hashList = hashtagRegex.findall(text)
            handleList = handleRegex.findall(text)

            if label == 7:
                policeHashDict = self.__addToDict(policeHashDict, hashList)
                policeHandleDict = self.__addToDict(policeHandleDict, handleList)
            elif label == 8:
                noPoliceHashDict = self.__addToDict(noPoliceHashDict, hashList)
                noPoliceHandleDict = self.__addToDict(noPoliceHandleDict, handleList)
            else:
                unlabeledHashDict = self.__addToDict(unlabeledHashDict, hashList)
                unlabeledHandleDict = self.__addToDict(unlabeledHandleDict, handleList)

            counter += 1

        policeHashTop = self.__getTopN(policeHashDict, listSize)
        policeHandleTop = self.__getTopN(policeHandleDict, listSize)
        noPoliceHashTop = self.__getTopN(noPoliceHashDict, listSize)
        noPoliceHandleTop = self.__getTopN(noPoliceHandleDict, listSize)
        unlabeledHashTop = self.__getTopN(unlabeledHashDict, listSize)
        unlabeledHandleTop = self.__getTopN(unlabeledHandleDict, listSize)

        return policeHashTop, policeHandleTop, noPoliceHashTop, noPoliceHandleTop, unlabeledHashTop, unlabeledHandleTop

    def __addToDict(self, dictionary, tagList):
        for word in tagList:
            word = word.lower()
            updateIndex = None
            dictionary[word] = dictionary.setdefault(word, 0) + 1
        return dictionary

    def __getTopN(self, dictionary, listSize):
        topList = list()

        for key, value in dictionary.iteritems():
            insertIndex = None

            for i in range(len(topList)):
                key2, value2 = topList[i]

                if value > value2 and insertIndex == None:
                    insertIndex = i

            if insertIndex != None:
                newTop = topList[0:insertIndex]
                newTop.append((key, value))
                newTop.extend(topList[insertIndex:-1])
                topList = newTop
            elif len(topList) < listSize:
                topList.append((key, value))

        return topList


if __name__ == "__main__":
    # These 2 lines fix a bug with python 2.x not reading utf-8 correctly when
    # loading json into pandas
    reload(sys)
    sys.setdefaultencoding("utf8")

    inputDir = None
    ouputDir = None
    classDir = None
    tagListSize = None
    onlyCount = False

    try:
        opts, args = getopt.getopt(sys.argv[1:], "hi:o:c:t:s")
        print opts
    except getopt.GetoptError:
        print("Run with the following options:\n\t-i <inputTweetdirectory>\n\t" +
              "-o <outputdirectory>\n\t-c <classifierDirectory>\n\t" +
              "-t <Integer for number of top hashtag/handles>\n\t" +
              "-s No args - Add this flag if you only want to count tags from labeled data")
    for opt, arg in opts:
        if opt == "-h":
            print("Options:\n\t-i <inputdirectory>\n\t-o <outputdirectory>" +
                  "\n\t-c <classifierDirectory>\n\t" +
                  "-t <Integer for number of top hashtag/handles\n\t" +
                  "-s No args - Add this flag if you only want to count tags from labeled data")
            sys.exit()
        elif opt == "-i":
            inputDir = arg
        elif opt == "-o":
            outputDir = arg
        elif opt == "-c":
            classDir = arg
        elif opt == "-t":
            tagListSize = int(arg)
        elif opt == "-s":
            onlyCount = True

    if onlyCount and classDir != None:
        print("Cannot use -c and -t flags at the same time.")
        sys.exit()

    c = Classify()
    tweets = c.readFiles(inputDir)

    if not os.path.exists(outputDir):
        os.makedirs(outputDir)

    outputName = dt.datetime.today().strftime("%H%M%S-%m%d%Y")

    if classDir == None and not onlyCount:
        tfidf, vectorizer, classifier = c.train(tweets)

        pickle.dump(tfidf, open(os.path.join(outputDir, "tfidf"), "wb"))
        pickle.dump(vectorizer, open(os.path.join(outputDir, "vectorizer"), "wb"))
        pickle.dump(classifier, open(os.path.join(outputDir, "classifier"), "wb"))

    elif not onlyCount:
        tfidf = pickle.load(open(os.path.join(classDir, "tfidf"), "rb"))
        vectorizer = pickle.load(open(os.path.join(classDir, "vectorizer"), "rb"))
        classifier = pickle.load(open(os.path.join(classDir, "classifier"), "rb"))

        labeled = c.predict(tfidf, classifier, tweets)

        labeled.to_json(os.path.join(outputDir, outputName), orient="records", lines=True)

    if (tagListSize != None) and (tagListSize > 0):
        if not onlyCount:
            tweets = labeled

        policeHashTop, policeHandleTop, noPoliceHashTop, noPoliceHandleTop, unlabeledHashTop, unlabeledHandleTop = c.count(
            tweets, tagListSize)

        if len(policeHashTop) > 0:
            with open(os.path.join(outputDir, outputName + "_PoliceHashtags.txt"), "w+") as outFile:
                outFile.writelines("\n".join("%s\t%s" % x for x in policeHashTop))
            outFile.closed

        if len(policeHandleTop) > 0:
            with open(os.path.join(outputDir, outputName + "_PoliceHandles.txt"), "w+") as outFile:
                outFile.writelines("\n".join("%s\t%s" % x for x in policeHandleTop))
            outFile.closed

        if len(noPoliceHashTop) > 0:
            with open(os.path.join(outputDir, outputName + "_NotPoliceHashtags.txt"), "w+") as outFile:
                outFile.writelines("\n".join("%s\t%s" % x for x in noPoliceHashTop))
            outFile.closed

        if len(noPoliceHandleTop) > 0:
            with open(os.path.join(outputDir, outputName + "_NotPoliceHandles.txt"), "w+") as outFile:
                outFile.writelines("\n".join("%s\t%s" % x for x in noPoliceHandleTop))
            outFile.closed

        if len(unlabeledHashTop) > 0:
            with open(os.path.join(outputDir, outputName + "_unlabledHashtags.txt"), "w+") as outFile:
                outFile.writelines("\n".join("%s\t%s" % x for x in unlabeledHashTop))
            outFile.closed

        if len(unlabeledHandleTop) > 0:
            with open(os.path.join(outputDir, outputName + "_unlabledHandles.txt"), "w+") as outFile:
                outFile.writelines("\n".join("%s\t%s" % x for x in unlabeledHandleTop))
            outFile.closed
