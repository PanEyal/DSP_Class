package MRs;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;

import java.io.IOException;

public class BiGramCount {
    private static long[] nArray = new long[60]; // The number of words in each decade
    private static String minPmi;
    private static String relMinPmi;

    public static void main(String[] args) throws Exception {
        minPmi = args[0];
        relMinPmi = args[1];

        System.exit(runJob1() ? (runJob2() ? (runJob3() ? 0 : 1) : 1) : 1);
    }

    private static boolean runJob1() throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "BiGramCount1MR");
        job.setJarByClass(BiGramCount1MR.class);
        job.setMapperClass(BiGramCount1MR.MapperClass.class);
        job.setReducerClass(BiGramCount1MR.ReducerClass.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setInputFormatClass(SequenceFileInputFormat.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(MapWritable.class);
        FileInputFormat.addInputPath(job, new Path("s3://datasets.elasticmapreduce/ngrams/books/20090715/heb-all/2gram/data"));
        FileOutputFormat.setOutputPath(job, new Path("s3n://buckettestefipan123/output1"));

        boolean complete = job.waitForCompletion(true);

        for(int i = 150; i<210; i++){
            nArray[i-150] = job.getCounters().findCounter(BiGramCount1MR.counters.valueOf("D"+i)).getValue();
        }
        return (complete);
    }

    private static boolean runJob2() throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        Job job = Job.getInstance(conf, "BiGramCount2MR");
        job.setJarByClass(BiGramCount2MR.class);
        job.setMapperClass(BiGramCount2MR.MapperClass.class);
        job.setReducerClass(BiGramCount2MR.ReducerClass.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path("s3n://buckettestefipan123/output1"));
        FileOutputFormat.setOutputPath(job, new Path("s3n://buckettestefipan123/output2"));
        return (job.waitForCompletion(true));
    }

    private static boolean runJob3() throws IOException, InterruptedException, ClassNotFoundException {
        Configuration conf = new Configuration();
        for(int i = 150; i<210; i++){
            conf.set("D"+i, String.valueOf(nArray[i-150]));
        }
        conf.set("minPmi", minPmi);
        conf.set("relMinPmi", relMinPmi);

        Job job = Job.getInstance(conf, "BiGramCount3MR");
        job.setJarByClass(BiGramCount3MR.class);
        job.setMapperClass(BiGramCount3MR.MapperClass.class);
        job.setReducerClass(BiGramCount3MR.ReducerClass.class);
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(Text.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);
        FileInputFormat.addInputPath(job, new Path("s3n://buckettestefipan123/output2"));
        FileOutputFormat.setOutputPath(job, new Path("s3n://buckettestefipan123/output3"));
        return (job.waitForCompletion(true));
    }
}
