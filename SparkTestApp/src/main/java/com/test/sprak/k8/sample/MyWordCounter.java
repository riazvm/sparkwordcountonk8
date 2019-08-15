package com.test.sprak.k8.sample;

import java.util.Arrays;
import java.util.Calendar;

import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.JavaRDD;
import org.apache.spark.api.java.JavaSparkContext;

import scala.Tuple2;

public class MyWordCounter {
	
	private static void wordCount(String[] args) {

        SparkConf sparkConf = new SparkConf().setMaster("local").setAppName("My Word Counter");
        String input =args[0];
        String output = args[1]+Calendar.getInstance().getTimeInMillis();
        
        
        System.out.println("input--------"+input);
        System.out.println("output--------"+output);
    	// Create a Java version of the Spark Context from the configuration
    	JavaSparkContext sc = new JavaSparkContext(sparkConf);
    	

    	JavaRDD<String> lines = sc.textFile(input);

    	JavaPairRDD<String, Integer> counts = lines.flatMap(line -> Arrays.asList(line.split(" ")).iterator())
                .mapToPair(word -> new Tuple2<String, Integer>(word, 1))
                .reduceByKey((x, y) ->  x +  y)
                .sortByKey();
    	
    	JavaPairRDD countData =lines.flatMap(line -> Arrays.asList(line.split(" ")).iterator())
                .mapToPair(word -> new Tuple2<String, Integer>(word, 1));
    	
    	counts.foreach(data -> {
            System.out.println(data._1()+"-"+data._2());
        });
       

        
       countData.saveAsTextFile(output);
    		   
    		   //"hdfs://192.168.100.111:9000/usr/local/hadoop/outputk8-"+Calendar.getInstance().getTimeInMillis());
    }
	

	public static void main(String[] args) {
		

	        wordCount(args);
	}

}
