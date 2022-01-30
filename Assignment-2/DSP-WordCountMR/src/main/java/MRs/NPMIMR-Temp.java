//package MRs;
//
//import java.io.IOException;
//
//import org.apache.hadoop.conf.Configuration;
//import org.apache.hadoop.fs.Path;
//import org.apache.hadoop.io.IntWritable;
//import org.apache.hadoop.io.LongWritable;
//import org.apache.hadoop.io.Text;
//import org.apache.hadoop.mapreduce.Job;
//import org.apache.hadoop.mapreduce.Mapper;
//import org.apache.hadoop.mapreduce.Partitioner;
//import org.apache.hadoop.mapreduce.Reducer;
//import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
//import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
//import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
//
//public class NPMIMR {
//
//    public static class MapperClass extends Mapper<LongWritable, Text, Text, IntWritable> {
//        private Text word = new Text();
//
//        @Override
//        public void map(LongWritable key, Text value, Context context) throws IOException,  InterruptedException {
//            String[] tokens = value.toString().split("\t");
//            if (tokens.length >= 5){
//                String decade = tokens[0];
//                String first = tokens[2];
//                String second = tokens[3];
//                int biGramAmount = Integer.parseInt(tokens[4]);
//                int n;
//                int firstAmount;
//                int secondAmount;
//
//                int npmi = Math.log(biGramAmount) + Math.log(n) - Math.log(firstAmount) - Math.log(secondAmount);
//
//                context.write(word, amount);
//                word.set(decade + " 3 " + first + " " + second);
//            }
//        }
//    }
//
//    public static class ReducerClass extends Reducer<Text,IntWritable,Text,IntWritable> {
//        @Override
//        public void reduce(Text key, Iterable<IntWritable> values, Context context) throws IOException,  InterruptedException {
//            int sum = 0;
//            for (IntWritable value : values) {
//                sum += value.get();
//            }
//            context.write(key, new IntWritable(sum));
//        }
//    }
//
//    public static class PartitionerClass extends Partitioner<Text, IntWritable> {
//        @Override
//        public int getPartition(Text key, IntWritable value, int numPartitions) {
//            return key.hashCode() % numPartitions;
//        }
//    }
//
//    public static void main(String[] args) throws Exception {
//        Configuration conf = new Configuration();
//        Job job = Job.getInstance(conf, args[0]);
//        job.setJarByClass(BiGramCount1MR.class);
//        job.setMapperClass(MapperClass.class);
//        job.setPartitionerClass(PartitionerClass.class);
//        job.setCombinerClass(ReducerClass.class);
//        job.setReducerClass(ReducerClass.class);
//        job.setMapOutputKeyClass(Text.class);
//        job.setMapOutputValueClass(IntWritable.class);
//        job.setInputFormatClass(SequenceFileInputFormat.class);
//        job.setOutputKeyClass(Text.class);
//        job.setOutputValueClass(IntWritable.class);
//        FileInputFormat.addInputPath(job, new Path(args[1]));
//        FileOutputFormat.setOutputPath(job, new Path(args[2]));
//        System.exit(job.waitForCompletion(true) ? 0 : 1);
//    }
//
//}