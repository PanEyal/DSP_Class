package ManagerPackage;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Base64;
import java.util.LinkedList;
import java.util.List;

public class EC2Handler {

    private static final Region REGION = Region.US_EAST_1;
    public static Ec2Client ec2c = Ec2Client.builder().region(REGION).build();
    private static String workerAmiName = "WorkerAmi";
    private static String workerAmiId = "ami-0192d6b6e43e650ea";
    private static String arn = "arn:aws:iam::871952098085:instance-profile/ManagerAndWorker";

    public static String createEC2WorkerInstance() {
        RunInstancesRequest runRequest = RunInstancesRequest.builder()
                .imageId(workerAmiId)
                .instanceType(InstanceType.T2_MEDIUM)
                .maxCount(1)
                .minCount(1)
                .keyName("DSP rock")
                .iamInstanceProfile(IamInstanceProfileSpecification.builder().arn(arn).build())
                .userData(Base64.getEncoder().encodeToString(("#!/bin/bash\n"+
                                                                "java -jar /home/ec2-user/worker.jar\n").getBytes()))
                .build();
        RunInstancesResponse response = ec2c.runInstances(runRequest);
        String instanceId = response.instances().get(0).instanceId();
        Tag tag = Tag.builder()
                .key("worker")
                .value(workerAmiName)
                .build();
        CreateTagsRequest tagRequest = CreateTagsRequest.builder()
                .resources(instanceId)
                .tags(tag)
                .build();
        ec2c.createTags(tagRequest);
        return instanceId;
    }

    private static String getManagerRunningId() {
        String nextToken = null;

        do {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
            DescribeInstancesResponse response = ec2c.describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    for (Tag tag : instance.tags()) {
                        if (tag.key().equals("manager")) {
                            return instance.instanceId();
                        }
                    }
                }
            }
            nextToken = response.nextToken();
        } while (nextToken != null);

        return null;
    }

    public static void terminateManager(){
        String managerId = getManagerRunningId();
        if(managerId != null){
            Tag tag = Tag.builder()
                    .key("manager")
                    .build();

            DeleteTagsRequest deleteTagsRequest = DeleteTagsRequest.builder().resources(EC2Handler.getManagerRunningId()).tags(tag).build();
            EC2Handler.ec2c.deleteTags(deleteTagsRequest);
            terminateInstance(managerId);
        }
    }

    public static void terminateAllWorkers() {
        List<String> workersInstances = new LinkedList<>();

        String nextToken = null;
        do {
            DescribeInstancesRequest request = DescribeInstancesRequest.builder().maxResults(6).nextToken(nextToken).build();
            DescribeInstancesResponse response = ec2c.describeInstances(request);

            for (Reservation reservation : response.reservations()) {
                for (Instance instance : reservation.instances()) {
                    for (Tag tag : instance.tags()) {
                        if (tag.key().equals("worker")) {
                            workersInstances.add(instance.instanceId());
                        }
                    }
                }
            }
            nextToken = response.nextToken();
        } while (nextToken != null);

        terminateInstances(workersInstances);
    }

    private static void terminateInstances(List<String> instanceId){
        TerminateInstancesRequest terminateInstancesRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build();
        ec2c.terminateInstances(terminateInstancesRequest);
    }

    private static void terminateInstance(String instanceId){
        TerminateInstancesRequest terminateInstancesRequest = TerminateInstancesRequest.builder().instanceIds(instanceId).build();
        ec2c.terminateInstances(terminateInstancesRequest);
    }
}
