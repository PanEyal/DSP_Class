package LocalAppPackage;

import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.util.List;
import java.util.concurrent.TimeUnit;


public class LocalApp {

    public static void main(String[] args) throws IOException, InterruptedException {
        boolean shouldTerminate = false;
        int numOfFiles;
        String numberOfWorkersPerNMessages;

        long startTimeMillis = System.currentTimeMillis();
        long startTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(startTimeMillis);

        // get argument
        if(args[args.length-1].equals("terminate")){
            shouldTerminate = true;
            if(args.length %2 == 1 || args.length < 4){
                System.out.println("Invalid number of Arguments");
                System.exit(1);
            }
            numOfFiles = (args.length - 2) / 2;
            numberOfWorkersPerNMessages = args[args.length - 2];
        }
        else{
            if(args.length %2 == 0 || args.length < 3){
                System.out.println("Invalid number of Arguments");
                System.exit(1);
            }
            numOfFiles = (args.length - 1) / 2;
            numberOfWorkersPerNMessages = args[args.length - 1];
        }

        String bucket = "bucket" + System.currentTimeMillis();
        String Manager2LocalAppQueue = "Manager2LocalApp" + System.currentTimeMillis();

        //Initialize a bucket in EC2
        S3Handler.createBucket(bucket);

        //Initialize the Queues in SQS
        SQSHandler.createQueue(Manager2LocalAppQueue);

        //create manager
        EC2Handler.validateManagerRunning();


        System.out.println("\nStarted uploading the files...\n");
        //uploads the files
        int curr = 0;
        while (curr < numOfFiles) {
            String inputFileKey = "inputKey" + curr;
            S3Handler.UploadFile(bucket, inputFileKey, args[curr]);
            System.out.println("Uploaded file: " + args[curr]);
            try {
                SQSHandler.sendMessage("LocalApp2Manager", Manager2LocalAppQueue + "," + bucket + "," + inputFileKey + "," + numberOfWorkersPerNMessages);
            }
            catch (Exception e){
                //Manager has received a terminate message and LocalApp2Manager queue is no longer exist
                S3Handler.deleteObject(bucket, inputFileKey);
                break;
            }
            curr++;
        }

        System.out.println("\nWaiting for completion...\n");
        //wait for completion
        curr = 0;
        boolean managerCrushed = false;
        while (curr < numOfFiles) {
            List<Message> messageList = SQSHandler.receiveMessages(Manager2LocalAppQueue);

            for(Message message : messageList){
                String outputFileKey = message.body();
                int fileNum = Integer.parseInt(outputFileKey.substring(9));

                //download final files
                HTMLMaker.make(args[fileNum + numOfFiles], S3Handler.downloadFile(bucket, outputFileKey));
                System.out.println("Downloaded file: " + args[fileNum + numOfFiles] + " successfully");
                //delete files in S3
                S3Handler.deleteObject(bucket, outputFileKey);

            }
            //delete message from Manager2LocalApp Queue
            SQSHandler.deleteMessages(Manager2LocalAppQueue, messageList);

            curr = curr + messageList.size();


            if(!EC2Handler.isManagerRunning()){
                //check if the manager crashed
                if(SQSHandler.isQueueAvailable("LocalApp2Manager")) {
                    managerCrushed = true;
                }
                break;
            }
        }

        if(!managerCrushed){
            while (curr < numOfFiles){
                curr++;
                System.out.println("File " + curr + " failed due to manger termination");
                try{
                    S3Handler.deleteObject(bucket, "inputKey"+curr);
                }
                catch (Exception e){

                }
            }

            if(shouldTerminate){
                SQSHandler.sendMessage("LocalApp2Manager", "terminate");
                System.out.println("\nManager Instance terminated");
            }

            //delete Queues
            SQSHandler.deleteQueue(Manager2LocalAppQueue);

            //delete Bucket
            S3Handler.deleteBucket(bucket);

            long endTimeMillis = System.currentTimeMillis();
            long endTimeMinutes = TimeUnit.MILLISECONDS.toMinutes(endTimeMillis);

            System.out.println("\nIt took the program: " + (endTimeMinutes - startTimeMinutes) + " minutes to finish");
        }

        else{
            EC2Handler.terminateAllInstances();
            S3Handler.CleanAndRemoveBucket(bucket);
            SQSHandler.deleteQueue(Manager2LocalAppQueue);
            try {
                SQSHandler.deleteManagerQueues();
            }
            catch (Exception e){
                //already deleted by another localApp instance
            }


            System.out.println("\nManager Instance crushed, closed all AWS services and waiting 60 sec...");

            Thread.sleep(60000);

            System.out.println("\nExiting. Run the program again");

            System.exit(1);
        }

    }
}
