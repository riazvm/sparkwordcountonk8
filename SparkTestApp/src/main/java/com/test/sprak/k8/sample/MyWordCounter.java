package com.test.sprak.k8.sample;

import java.util.Arrays;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

public class MyWordCounter {
	
	private static void wordCount() {

        SparkConf sparkConf = new SparkConf().setMaster("local").setAppName("My Word Counter");
        

    	// Create a Java version of the Spark Context from the configuration
    	JavaSparkContext sc = new JavaSparkContext(sparkConf);

    	JavaRDD<String> lines = sc.textFile("hdfs://192.168.100.113:9000/usr/local/hadoop/input");

    	JavaPairRDD<String, Integer> counts = lines.flatMap(line -> Arrays.asList(line.split(" ")).iterator())
                .mapToPair(word -> new Tuple2<String, Integer>(word, 1))
                .reduceByKey((x, y) ->  x +  y)
                .sortByKey();
    	
    	JavaPairRDD countData =lines.flatMap(line -> Arrays.asList(line.split(" ")).iterator())
                .mapToPair(word -> new Tuple2<String, Integer>(word, 1));
    	
    	counts.foreach(data -> {
            System.out.println(data._1()+"-"+data._2());
        });
       

        
       countData.saveAsTextFile("hdfs://192.168.100.113:9000/usr/local/hadoop/testk82");
    }
	

	public static void main(String[] args) {
		

	        wordCount();
	}

}
