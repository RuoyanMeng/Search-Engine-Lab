
/*  
 *   This file is part of the computer assignment for the
 *   Information Retrieval course at KTH.
 * 
 *   Johan Boye, 2017
 */

package ir;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.StringTokenizer;
import java.util.Iterator;
import java.util.Map;
import java.nio.charset.*;
import java.io.*;

/**
 * A class for representing a query as a list of words, each of which has an
 * associated weight.
 */
public class Query {

    /**
     * Help class to represent one query term, with its associated weight.
     */
    class QueryTerm {
        String term;
        double weight;

        QueryTerm(String t, double w) {
            term = t;
            weight = w;
        }

        public void setWeight(double weight){
            this.weight = weight;
        }
    }

    /**
     * Representation of the query as a list of terms with associated weights. In
     * assignments 1 and 2, the weight of each term will always be 1.
     */
    public ArrayList<QueryTerm> queryterm = new ArrayList<QueryTerm>();

    /**
     * Relevance feedback constant alpha (= weight of original query terms). Should
     * be between 0 and 1. (only used in assignment 3).
     */
    double alpha = 0.2;

    /**
     * Relevance feedback constant beta (= weight of query terms obtained by
     * feedback from the user). (only used in assignment 3).
     */
    double beta = 1 - alpha;

    /**
     * Creates a new empty Query
     */
    public Query() {
    }

    /**
     * Creates a new Query from a string of words
     */
    public Query(String queryString) {
        StringTokenizer tok = new StringTokenizer(queryString);
        while (tok.hasMoreTokens()) {
            queryterm.add(new QueryTerm(tok.nextToken(), 1.0));
        }
    }

    public void addTerm(String term){
        queryterm.add(new QueryTerm(term, 1.0));
    }

    public void addQueries(Query query, Query queries){
        for (int i=0;i<queries.size();i++){
            query.addTerm(queries.queryterm.get(i).term);
        }
    }

    

    /**
     * Returns the number of terms
     */
    public int size() {
        return queryterm.size();
    }

    /**
     * Returns the Manhattan query length
     */
    public double length() {
        double len = 0;
        for (QueryTerm t : queryterm) {
            len += t.weight;
        }
        return len;
    }

    /**
     * Returns a copy of the Query
     */
    public Query copy() {
        Query queryCopy = new Query();
        for (QueryTerm t : queryterm) {
            queryCopy.queryterm.add(new QueryTerm(t.term, t.weight));
        }
        return queryCopy;
    }

    /**
     * Expands the Query using Relevance Feedback
     *
     * @param results       The results of the previous query.
     * @param docIsRelevant A boolean array representing which query results the
     *                      user deemed relevant.
     * @param engine        The search engine object
     */
    public void relevanceFeedback(PostingsList results, boolean[] docIsRelevant, Engine engine) {

        int numOfRelevantDoc = 0;
        for (int i = 0; i < docIsRelevant.length; i++) {
            if (docIsRelevant[i]) {
                numOfRelevantDoc++;
            }
        }
        //System.err.println("numOfRelevantDoc:" + numOfRelevantDoc);

        // Rocchio algorithm
        HashMap<String, Double> expWeight = new HashMap<String, Double>();
        int topNum = 10; // Only pick from top 10 results of tf_idf ranking
        for (int i = 0; i < topNum; i++) {
            if (docIsRelevant[i]) {
                int docID = results.get(i).docID;
                HashMap<String, Integer> doc_tfMap = engine.index.termFreq.get(docID);
                int docLength = engine.index.docLengths.get(docID);
                //System.err.println("docLength:" +docLength);
                for (Map.Entry<String, Integer> tf : doc_tfMap.entrySet()) {
                    //System.err.println("tf:"+tf.getValue());
                    String term = tf.getKey();
                    double weight;
                    double idf;
                    idf = Math.log10(engine.index.docNames.size()/engine.index.docFreq.get(term));
                    weight = beta * (1.0 / numOfRelevantDoc) * (Double.valueOf(tf.getValue()) / Double.valueOf(docLength));
                    //System.err.println("w:"+weight);
                    if (!expWeight.containsKey(term)) {
                        expWeight.put(term, weight);
                    } else {
                        weight = weight + expWeight.get(term);
                        expWeight.put(term, weight);
                    }
                }
            }
        }

        // update weight
        for (Map.Entry<String, Double> q : expWeight.entrySet()) {
            QueryTerm expTerm = new QueryTerm(q.getKey(),1.0);
            if (!queryterm.contains(expTerm)) {
                queryterm.add(new QueryTerm(q.getKey(), q.getValue()));
            }else{
                int n = queryterm.indexOf(expTerm);
                queryterm.get(n).setWeight(q.getValue()+alpha);
                System.err.println("+"+queryterm.get(n).weight);
            }

        }

    }
}
