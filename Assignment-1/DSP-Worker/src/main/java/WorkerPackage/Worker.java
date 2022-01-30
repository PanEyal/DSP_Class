package WorkerPackage;

import com.google.gson.Gson;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class Worker {

    private static Gson gson = new Gson();
    private static SentimentAnalysisHandler sah = new SentimentAnalysisHandler();
    private static namedEntityRecognitionHandler nerh = new namedEntityRecognitionHandler();

    public static void main(String[] args){
        while(true){
            //recieve 1 message from the Manager2Workers queue
            List<Message> messages = SQSHandler.receiveMessage("Manager2Workers");
            for(Message message : messages) {

                String[] messageArgs = message.body().split(",", 4);

                String Manager2LocalAppQueue = messageArgs[0];
                String outputFileKey = messageArgs[1];
                int jobNumber = Integer.parseInt(messageArgs[2]);
                String productReviewString = messageArgs[3];

                ProductReview productReview = gson.fromJson(productReviewString, ProductReview.class);
                StringBuilder subTableHTML = new StringBuilder();
                for (Review review : productReview.getReviews()) {
                    ReviewOutput reviewOutput = new ReviewOutput();

                    reviewOutput.setLink(review.getLink());
                    reviewOutput.setSentimentLvl(sah.findSentiment(review.getText()) + 1);
                    reviewOutput.setEntities(nerh.getRelevantEntities(review.getText()));
                    reviewOutput.setSarcasmLvl(java.lang.Math.abs(reviewOutput.getSarcasmLvl() - review.getRating()));

                    String hexColor = getColorBySentimentLvl(reviewOutput.getSentimentLvl());

                    subTableHTML.append(
                            "\n    <tr>\n" +
                            "          <th style=\"color: " + hexColor + "\">" + reviewOutput.getLink() + "</th>\n" +
                            "          <th>" + reviewOutput.getEntities() + "</th>\n" +
                            "          <th>" + reviewOutput.getSarcasmLvl() + "</th>\n" +
                            "      </tr>");
                }
                SQSHandler.sendMessage("Workers2Manager", Manager2LocalAppQueue + "," + outputFileKey + "," + jobNumber + "," + subTableHTML);
            }
            SQSHandler.deleteMessages("Manager2Workers", messages);
        }
    }

    private static String getColorBySentimentLvl(int sentimentLvl){
        switch (sentimentLvl) {
            case 1:
                return "#a10000"; //dark red
            case 2:
                return "#ff0000"; // red
            case 4:
                return "#45eb13"; // green
            case 5:
                return "#29910a"; // dark green
            default:
                return "#000000"; // black
        }
    }
}
