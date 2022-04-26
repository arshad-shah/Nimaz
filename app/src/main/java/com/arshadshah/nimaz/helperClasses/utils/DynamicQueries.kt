package com.arshadshah.nimaz.helperClasses.utils


class DynamicQueries {
    //init the class so that it checks if the word is one or multiple words
    //if its is a single word then it will return the word itself
    //if it is a multiple word then it will run create a query for each word and return the queries in an arrayList

    companion object {
        fun getDynamicQuery(word: String): ArrayList<String> {
            val dynamicQuery = ArrayList<String>()
            if (word.contains(" ")) {
                val words = word.split(" ")
                
                for (i in words) {
                    dynamicQuery.add(i)
                }
            } else {
                dynamicQuery.add(word)
            }
            return dynamicQuery
        }
    }

}
