package com.arshadshah.nimaz.helperClasses.utils


class DynamicQueries {
    //init the class so that it checks if the word is one or multiple words
    //if its is a single word then it will return the word itself
    //if it is a multiple word then it will run create a query for each word and return the queries in an arrayList
    companion object {
        fun getDynamicQuery(word: String): ArrayList<String> {
            //Array list of words like is, are e.t.c
            val wordsToRemove = arrayOf(
                "is",
                "are",
                "was",
                "were",
                "am",
                "be",
                "been",
                "being",
                "have",
                "has",
                "had",
                "having",
                "do",
                "does",
                "did",
                "doing",
                "a",
                "an",
                "the",
                "and",
                "but",
                "if",
                "or",
                "because",
                "as",
                "until",
                "while",
                "of",
                "at",
                "by",
                "for",
                "with",
                "about",
                "against",
                "between",
                "into",
                "before",
                "after",
                "above",
                "below",
                "to",
                "from",
                "up",
                "down",
                "in",
                "out",
                "on",
                "off",
                "over",
                "under",
                "then",
                "once",
                "here",
                "there",
                "when",
                "where",
                "why",
                "how",
                "all",
                "any",
                "both",
                "each",
                "few",
                "more",
                "most",
                "other",
                "some",
                "such",
                "no",
                "nor",
                "not",
                "only",
                "own",
                "so",
                "than",
                "too",
                "very",
                "can",
                "will",
                "just",
                "should",
                "now"
            )
            val dynamicQuery = ArrayList<String>()
            if (word.contains(" ")) {
                val words = word.split(" ")

                for (i in words) {
                    //check if the i exist in the array of words to remove than dont add it to the dynamic query
                    if (!wordsToRemove.contains(i)) {
                        dynamicQuery.add(i)
                    }
                }
            } else {
                dynamicQuery.add(word)
            }
            return dynamicQuery
        }
    }
}
