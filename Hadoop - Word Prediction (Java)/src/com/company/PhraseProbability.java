import java.io.IOException;
import java.util.*;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.io.ImmutableBytesWritable;
import org.apache.hadoop.hbase.mapreduce.TableMapReduceUtil;
import org.apache.hadoop.hbase.mapreduce.TableReducer;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;

public class PhraseProbability {

    public static class ProbabilityMapper
            extends Mapper<Object, Text, Text, Text>{

        public void map(Object inKey, Text inValue, Context context)
                throws IOException, InterruptedException{

            if(inValue.toString().equals("")){
                return;
            }

            //Split the line into the phrase/count pair
            String pair[] = inValue.toString().split("\t");

            //Check to see if minimum threshhold is met.
            Configuration config = context.getConfiguration();
            int min = Integer.parseInt(config.get("min"));
            if(Integer.parseInt(pair[1]) <= min){
                return;
            }

            //Split the phrase into words
            String words[] = pair[0].split(" ");

            //Send the whole phrase as a key only if the phrase has less than 5 words in it
            //If the input value is: hello world 10
            //Then output <hello world, hello world:10>
            if(words.length < 5){
                context.write(new Text(pair[0]), new Text(inValue.toString().replaceAll("\t", ":")));
            }

            //Send the whole phrase, excluding last word, as the key if the phrase has more than 1 word in it
            //if the input value is: hello world 10
            //Then output <hello, world:10>
            if(words.length > 1){
                String key = "";

                //Create the key for the key/value pair
                //The key is composed of all but the last word in the phrase
                for(int i = 0; i < words.length - 1; i++){
                    key += words[i] + " ";
                }
                context.write(new Text(key.trim()), new Text(inValue.toString().replaceAll("\t", ":")));
            }
        }
    }

    public static class ProbabilityReducer
            extends TableReducer<Text, Text, ImmutableBytesWritable> {

        public void reduce(Text key, Iterable<Text> values, Context context)
                throws IOException, InterruptedException{

            int sum = 0;
            double basePhraseCount = 0;
            ArrayList<WordPair> wpal = new ArrayList<WordPair>();
            String predictionList = "";
            byte[] cFamily = Bytes.toBytes("probs");

            //Iterate through all the values for a key
            //If it's the "base" value (denominator) then store it as the base
            //Otherwise store the word/count pair into the WordPair object
            for(Text val : values){
                String splitValue[] = val.toString().split(":");
                if(key.toString().equals(splitValue[0])){
                    basePhraseCount = Double.parseDouble(splitValue[1]);
                }else {
                    String[] words = splitValue[0].split(" ");
                    wpal.add(new WordPair(words[words.length-1], splitValue[1]));
                }
            }

            //If there's no word pairs (numerators) then just quit
            if(wpal.size() == 0){
                return;
            }

            //Calculate the probabilities wp.getProbability() initially contains the count
            //of how many times that word appeared after the base.
            for(WordPair wp : wpal){
                wp.setProbability(wp.getProbability()/basePhraseCount);
            }

            Collections.sort(wpal);

            Put put = new Put(Bytes.toBytes(key.toString()));
            WordPair wp;

            Configuration config = context.getConfiguration();
            int count = Integer.parseInt(config.get("count"));

            //Loop through all the words for the key and add it to the Put object
            //Finally write it to the HBase database
            for (int i = 0; i < wpal.size(); i++) {
                if (i > count-1) {
                    break;
                }

                wp = wpal.get(i);
                put.add(cFamily, Bytes.toBytes(wp.getWord()), Bytes.toBytes(wp.getStringProb()));

            }
            context.write(null, put);
        }

        //Class that keeps track of a word and its probability
        //Mostly used for sorting to pick the 5 words with the highest probability
        public class WordPair implements Comparable<WordPair>{
            private String word = "";
            private double probability = 0.0;

            public WordPair(String word, String count){
                this.word = word;
                this.probability = Double.parseDouble(count);
            }

            public void setProbability(double probability){
                this.probability = probability;
            }

            public String getWord(){
                return this.word;
            }

            public double getProbability(){
                return this.probability;
            }

            public String getStringProb(){
                return Double.toString(this.probability);
            }

            //Sort on probability in descending order.
            //In case of tie, then sort on word ascending order.
            public int compareTo(WordPair w){
                if(this.getProbability() > w.getProbability()){
                    return -1;
                }else if(this.getProbability() < w.getProbability()){
                    return 1;
                }else{
                    return this.getWord().compareTo(w.getWord());
                }
            }
        }
    }

    public static void main(String[] args) throws Exception {
        String hbaseTableName = "ProbTable";
        Configuration conf = HBaseConfiguration.create();
        conf.set("min", args[2]);
        conf.set("count", args[3]);
        Job job = Job.getInstance(conf, "Word Probability");
        job.setJarByClass(PhraseProbability.class);
        job.setMapperClass(ProbabilityMapper.class);
        //job.setCombinerClass(ProbabilityReducer.class);
        job.setReducerClass(ProbabilityReducer.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        //FileOutputFormat.setOutputPath(job, new Path(args[1]));

        TableMapReduceUtil.initTableReducerJob(hbaseTableName,
                ProbabilityReducer.class,
                job);

        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}