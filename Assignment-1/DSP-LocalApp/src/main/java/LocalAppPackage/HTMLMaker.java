package LocalAppPackage;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class HTMLMaker {

    public static void make(String output, BufferedReader reader) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(output));

        writer.write("<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "    <style>\n" +
                "       table, th, td {\n" +
                "       border: 1px solid black;\n" +
                "       border-collapse: collapse;\n" +
                "       }\n" +
                "    </style>\n" +
                "    <meta charset=\"UTF-8\">\n" +
                "    <title>Sarcastic Review Output</title>\n" +
                "    <div class=\"header\">\n" +
                "        <h1>Sarcastic Review Output</h1>\n" +
                "    </div>\n" +
                "</head>\n" +
                "<body>\n" +
                "\n" +
                "  <table>\n" +
                "      <tr>\n" +
                "          <th>Colored Link</th>\n" +
                "          <th>Entities</th>\n" +
                "          <th>Sarcasm Detection</th>\n" +
                "      </tr>\n");

        String line;
        while ((line = reader.readLine()) != null) {
            writer.write(line);
            writer.newLine();
        }

        writer.write(
                "  </table>\n" +
                "</body>\n" +
                "</html>");

        writer.close();
    }
}
