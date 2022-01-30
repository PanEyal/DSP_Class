package MRs;

import Misc.StopWords;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;

public class BiGramCount1MR {

    public enum counters{
        D150,D151,D152,D153,D154,D155,D156,D157,D158,D159,
        D160,D161,D162,D163,D164,D165,D166,D167,D168,D169,
        D170,D171,D172,D173,D174,D175,D176,D177,D178,D179,
        D180,D181,D182,D183,D184,D185,D186,D187,D188,D189,
        D190,D191,D192,D193,D194,D195,D196,D197,D198,D199,
        D200,D201,D202,D203,D204,D205,D206,D207,D208,D209
    }

    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {
        private Text word = new Text();
        private Text data = new Text();

        @Override
        public void map(LongWritable key, Text value, Context context) throws IOException,  InterruptedException {
            String[] tokens = value.toString().split("\t"); // [[2gram] , year, occurrences, pages, books]
            if (tokens.length >= 4){

                String[] nGrams = tokens[0].split(" ");
                if(nGrams.length < 2)
                    return;
                String first = nGrams[0];
                String second = nGrams[1];

                // check if the token is not a stop word
                if (!StopWords.stopWords.contains(first) && !StopWords.stopWords.contains(second)) {
                    String decade = tokens[1].substring(0, 3);
                    context.getCounter(counters.valueOf("D"+decade)).increment(1);
                    IntWritable amount = new IntWritable(Integer.parseInt(tokens[2]));
                    System.out.println();
                    word.set(decade + " 1 " + first + " ");
                    data.set(amount + " " + second);
                    context.write(word, data);

                    word.set(decade + " 2 " + second + " ");
                    data.set(amount + " " + first);
                    context.write(word, data);
                }
            }
        }
    }

    public static class ReducerClass extends Reducer<Text,Text,Text, MapWritable> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {

            // creates map for every bi-gram with the key word
            MapWritable map = new MapWritable();

            // sum value for the number of times the key word appeared
            Text sum = new Text("sum");
            map.put(sum, new IntWritable(0));

            for (Text data : values) {
                String[] tokens = data.toString().split(" "); // [amount, first] or [amount, second]
                // sum it
                int amount = Integer.parseInt(tokens[0]);
                map.put(sum, new IntWritable(((IntWritable) map.get(sum)).get() + amount));

                Text otherGram = new Text(tokens[1]);
                IntWritable value = (IntWritable) map.get(otherGram);
                if(value == null)
                    map.put(otherGram, new IntWritable(amount));
                else
                    map.put(otherGram, new IntWritable(value.get() + amount));
            }
        }
    }
}