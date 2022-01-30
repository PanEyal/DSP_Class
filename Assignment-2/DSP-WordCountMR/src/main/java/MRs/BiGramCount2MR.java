package MRs;

import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class BiGramCount2MR {

    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {
        private Text word = new Text();
        private Text data = new Text();

        @Override // key is the line number, input is the line text
        public void map(LongWritable key, Text input, Context context) throws IOException,  InterruptedException {
            String[] tokens = input.toString().split("\\s+", 4); // [decade, flag, word, mapString]
            String decade = tokens[0];
            String flag = tokens[1];
            String value = tokens[2];
            String[] mapString = tokens[3].substring(1,tokens[3].length()-1).split(",\\s+");

            // creates a map from array
            Map<String,String> map = new HashMap<>();
            for(String entry : mapString) {
                String[] vals = entry.split("=");
                map.put(vals[0],vals[1]);
            }

            int sum = Integer.parseInt(map.remove("sum")); // this is c(w1) or c(w2)

            if(flag.equals("1")){
                for(Map.Entry entry : map.entrySet()) {
                    word.set(decade + " " + value + " " + entry.getKey()); // {decade, first word, second word)}
                    data.set(entry.getValue() + " " + sum + " 0"); // {bi-gram amount, c(w1), second amount place holder}
                    context.write(word, data);
                }
            }
            else{ // flag == 2
                for (Map.Entry entry : map.entrySet() ) {
                    word.set(decade + " " + entry.getKey() + " " + value); // {decade, first word, second word)}
                    data.set(entry.getValue() + " 0 " + sum); // {bi-gram amount, first amount place holder, c(w2)}
                    context.write(word, data);
                }
            }
        }
    }

    public static class ReducerClass extends Reducer<Text,Text,Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {
            int amount = 0;
            int firstAmount = 0;
            int secondAmount = 0;
            for (Text value : values) {
                String[] tokens = value.toString().split("\\s+");
                amount = Integer.parseInt(tokens[0]);
                firstAmount += Integer.parseInt(tokens[1]);
                secondAmount += Integer.parseInt(tokens[2]);
            }
            context.write(key, new Text(amount + " " + firstAmount + " " + secondAmount)); // [decade, first, second | c(w1,w2) , c(w1) ,c(w2)]
        }
    }
}