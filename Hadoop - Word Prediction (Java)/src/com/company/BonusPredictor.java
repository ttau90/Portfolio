import java.io.IOException;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

public class BonusPredictor {

    public static class PredictionMapper
            extends Mapper<Object, Text, Text, IntWritable>{
        private final static IntWritable ONE = new IntWritable(1);

        public void map(Object inKey, Text inValue, Context context)
                throws IOException, InterruptedException{

            String line = inValue.toString();

            //Replace URLs
            line = line.toLowerCase().replaceAll("(https?|ftp):\\/\\/[^\\s/$.?#][^\\s]*", " ");

            //Remove <ref> tags
            line = line.replaceAll("<((ref.*?)|(\\/ref))>", " ");

            //Remove 2+ ''
            line = line.replaceAll("'{2,}", " ");

            //Remove all punctuation
            line = line.replaceAll("[^a-z' ]|'(?=[^a-z])|(?<![a-z])'", " ");

            //Replace multiple spaces with 1 space
            line = line.replaceAll("\\s+", " ").trim();

            if(line.equals("")){
                return;
            }
            String words[] = line.split(" ");

            for(String s : words){
                //Don't do anything if it's a 1 letter word
                if(s.length() == 1){
                    continue;
                }
                for(int i = 1; i < s.length(); i++){
                    context.write(new Text(s.substring(0, i)), ONE);
                }
                //Underscore indicates that it's a word
                //To be used in probability calculator
                context.write(new Text(s+"_"), ONE);
            }
        }
    }

    public static class PredictionReducer
            extends Reducer<Text, IntWritable, Text, IntWritable>{

        private IntWritable result = new IntWritable();

        public void reduce(Text key, Iterable<IntWritable> values, Context context)
                throws IOException, InterruptedException{
            int sum = 0;
            for(IntWritable val : values){
                sum += val.get();
            }

            result.set(sum);

            context.write(key, result);
        }
    }


    public static void main(String[] args) throws Exception {

        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "word count");
        job.setJarByClass(BonusPredictor.class);
        job.setMapperClass(PredictionMapper.class);
        //job.setCombinerClass(PredictionReducer.class);
        job.setReducerClass(PredictionReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        FileInputFormat.addInputPath(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
}