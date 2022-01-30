package MRs;

import Misc.BigramNPMI;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BiGramCount3MR {

    public static class MapperClass extends Mapper<LongWritable, Text, Text, Text> {
        private Text word = new Text();
        private Text data = new Text();

        @Override // key is the line number, input is the line text
        public void map(LongWritable key, Text input, Context context) throws IOException,  InterruptedException {
            String[] tokens = input.toString().split("\\s+", 6); // [decade, first, second , c(w1,w2) , c(w1) ,c(w2)]
            String decade = tokens[0];
            String first = tokens[1];
            String second = tokens[2];
            String c12 = tokens[3];
            String c1 = tokens[4];
            String c2 = tokens[5];

            word.set(decade);
            data.set(first + " " + second + " " + c12 + " " + c1 + " " + c2);
            context.write(word, data);
        }
    }

    public static class ReducerClass extends Reducer<Text,Text,Text, Text> {
        @Override
        public void reduce(Text key, Iterable<Text> values, Context context) throws IOException,  InterruptedException {
            List<BigramNPMI> output = new ArrayList<>();
            double minPmi = Double.parseDouble(context.getConfiguration().get("minPmi"));
            double relMinPmi = Double.parseDouble(context.getConfiguration().get("relMinPmi"));
            double sumMinPmi = (double) 0;
            String decade = key.toString();

            for (Text value : values) {
                String[] tokens = value.toString().split("\\s+", 5); // [first, second , c(w1,w2) , c(w1) ,c(w2)]
                String first = tokens[0];
                String second = tokens[1];
                double c12 = Double.parseDouble(tokens[2]);
                long c1 = Long.parseLong(tokens[3]);
                long c2 = Long.parseLong(tokens[4]);
                double n = Double.parseDouble(context.getConfiguration().get("D"+decade));

                double pmi = Math.log(c12) + Math.log(n) - Math.log(c1) - Math.log(c2);
                double npmi = (pmi / Math.log(c12/n)) * Double.parseDouble("-1");
                sumMinPmi += npmi;

                BigramNPMI valueBigram = new BigramNPMI(first, second, npmi);
                output.add(valueBigram);
            }

            for (BigramNPMI bigram : output){
                if(!bigram.isCollocation(minPmi,relMinPmi,sumMinPmi))
                    output.remove(bigram);
            }
            output.sort(new BigramNPMI.BigramComparator());

            StringBuilder outputTable = new StringBuilder("\n");
            int count = 0;
            for (BigramNPMI bigram : output){
                count++;
                outputTable.append(bigram.getFirst()).append(" ")
                        .append(bigram.getSecond()).append("    ")
                        .append(bigram.getNpmi()).append("\n");
                if(count == 10)
                    break;
            }
            outputTable.append("\n");

            context.write(new Text("------- DECADE: " + decade + " ---------"), new Text(outputTable.toString())); // [decade : [first, second, npmi]]
        }
    }
}