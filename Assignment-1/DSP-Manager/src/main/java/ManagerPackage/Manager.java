package ManagerPackage;

import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Manager {

    // For some reason amazon educate strict number of instances to be more no then 9,
    // therefore to avoid unnecessary creation and then automatic termination by amazon,
    // we restricted the max allowed workers here to be 8 (9 total with the manager itself)
    private static final int MAX_ALLOWED_WORKERS = 8;

    private static int activeWorkers = 0;
    private static boolean shouldTerminate = false;

    // Hashmap that hold a key for every LocalApp, correlating to its relevant hashmap that hold a key for every file,
    // correlating to its relevant outputValues class
    private static ConcurrentHashMap<String, ConcurrentHashMap<String, OutputValues>> LocalAppJob = new ConcurrentHashMap<>();

    public static void main(String[] args) {

        SQSHandler.createQueue("Manager2Workers");
        SQSHandler.createQueue("Workers2Manager");

        // create a new thread for handling local app messages
        Thread handleMessageFromLocalApp = new Thread(()-> {
            while (!shouldTerminate){
                try {
                    receiveAndHandleMessageFromLocalApp();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            SQSHandler.deleteQueue("LocalApp2Manager");
        });
        handleMessageFromLocalApp.start();

        // create a new thread for handling workers messages
        Thread handleMessageFromWorkers = new Thread(() -> {
            while (!shouldTerminate || !LocalAppJob.isEmpty()){
                receiveAndHandleMessageFromWorkers();

            }

            EC2Handler.terminateAllWorkers();

            SQSHandler.deleteQueue("Workers2Manager");
            SQSHandler.deleteQueue("Manager2Workers");

            EC2Handler.terminateManager();
        });
        handleMessageFromWorkers.start();

    }

    private static void receiveAndHandleMessageFromLocalApp() throws IOException {
        List<Message> LocalApp2ManagerMessages = SQSHandler.receiveMessages("LocalApp2Manager");
        for (Message message : LocalApp2ManagerMessages) {

            if(message.body().equals("terminate")){
                shouldTerminate = true;
            }
            else {
                String[] messageArgs = message.body().split(",", 4);

                String Manager2LocalAppQueue = messageArgs[0];
                String bucket = messageArgs[1];
                String inputFileKey = messageArgs[2];
                int numberOfWorkersPerNMessages = Integer.parseInt(messageArgs[3]);

                assignJobs(S3Handler.downloadFile(bucket, inputFileKey), Manager2LocalAppQueue, bucket, inputFileKey, numberOfWorkersPerNMessages);
                S3Handler.deleteFile(bucket, inputFileKey);
            }
            SQSHandler.deleteMessage("LocalApp2Manager", message);
        }
    }

    private static void assignJobs(BufferedReader reader, String Manager2LocalAppQueue, String bucket, String inputFileKey, int numberOfWorkersPerNMessages) throws IOException {
        String outputFileKey = "outputKey" + inputFileKey.substring(8);
        int jobNumber = 0;

        String line = reader.readLine();
        while (line != null) {
            if(!line.isEmpty()) {
                if (isNewWorkerNeeded(jobNumber, numberOfWorkersPerNMessages)) {
                    EC2Handler.createEC2WorkerInstance();
                    activeWorkers++;
                }
                SQSHandler.sendMessage("Manager2Workers", Manager2LocalAppQueue + "," + outputFileKey + "," + jobNumber + "," + line);

                line = reader.readLine();
                jobNumber++;
            }
        }
        LocalAppJob.putIfAbsent(Manager2LocalAppQueue, new ConcurrentHashMap<>());
        LocalAppJob.get(Manager2LocalAppQueue).put(outputFileKey, new OutputValues(jobNumber, bucket, outputFileKey));
    }

    // checks if activeWorkers are under the max value we specified and that enough workers are running according to the
    // necessary amount of workers per n messages
    private static boolean isNewWorkerNeeded(int count, int numberOfWorkersPerNMessages){
        return activeWorkers <= MAX_ALLOWED_WORKERS && count >= activeWorkers * numberOfWorkersPerNMessages;
    }

    private static void receiveAndHandleMessageFromWorkers(){
        List<Message> Workers2ManagerMessages = SQSHandler.receiveMessages("Workers2Manager");
        for (Message message : Workers2ManagerMessages) {
            String[] messageArgs = message.body().split(",", 4);

            String Manager2LocalAppQueue = messageArgs[0];
            String outputFileKey = messageArgs[1];
            int jobNumber = Integer.parseInt(messageArgs[2]);
            String subTableHTML = messageArgs[3];

            // check if we haven't finished working on the file/job
            if(LocalAppJob.containsKey(Manager2LocalAppQueue) && LocalAppJob.get(Manager2LocalAppQueue).containsKey(outputFileKey)) {

                // build the upload request
                CompleteMultipartUploadRequest completeMultipartUploadRequest = LocalAppJob.get(Manager2LocalAppQueue).get(outputFileKey).addPart(jobNumber, subTableHTML);

                // if completeMultipartUploadRequest isn't null, it means the completeMultipartUploadRequest
                // is completed and therefore we will send it
                if (completeMultipartUploadRequest != null) {
                    S3Handler.sendCompleteMultipartUpload(completeMultipartUploadRequest);

                    SQSHandler.sendMessage(Manager2LocalAppQueue, outputFileKey);

                    // remove the key and value of the file we sent from the relevant LocalApp hashmap
                    LocalAppJob.get(Manager2LocalAppQueue).remove(outputFileKey);
                    // if it was the last file of the task from the LocalApp, remove the LocalApp hashmap as well
                    if (LocalAppJob.get(Manager2LocalAppQueue).isEmpty()) {
                        LocalAppJob.remove(Manager2LocalAppQueue);
                    }
                }
            }

        }
        SQSHandler.deleteMessages("Workers2Manager", Workers2ManagerMessages);
    }

}
