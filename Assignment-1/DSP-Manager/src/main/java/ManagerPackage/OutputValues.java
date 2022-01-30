package ManagerPackage;

import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedPart;

import java.util.Collection;
import java.util.LinkedList;

public class OutputValues {
    private static final int MIN_PART_LENGTH = 5 * 1024 * 1024;

    private int messagesNumber;
    private boolean[] jobWatcher;
    private int finishedNumber;
    private Collection<CompletedPart> parts;
    private StringBuilder currentPart;
    private String bucket;
    private String key;
    private String uploadId;

    public OutputValues(int partsNumber, String bucket, String key) {
        this.messagesNumber = partsNumber;
        this.jobWatcher = new boolean[messagesNumber];
        this.finishedNumber = 0;
        this.parts = new LinkedList<>();
        this.currentPart = new StringBuilder();
        this.bucket = bucket;
        this.key = key;
        this.uploadId = S3Handler.startMultipartUpload(bucket, key);
    }

    public CompleteMultipartUploadRequest addPart(int jobNumber, String message) {
        if(jobWatcher[jobNumber] == false){
            jobWatcher[jobNumber] = true;
            currentPart.append(message);
            finishedNumber++;
        }

        if(currentPart.length() >= MIN_PART_LENGTH || finishedNumber == messagesNumber){
            parts.add(S3Handler.addPartToMultipartUpload(bucket, key, uploadId, parts.size() + 1, currentPart.toString()));
            currentPart = new StringBuilder();
        }

        if (finishedNumber == messagesNumber){
            return S3Handler.getCompleteMultipartUploadRequest(bucket, key, uploadId, parts);
        }

        else
            return null;
    }
}
