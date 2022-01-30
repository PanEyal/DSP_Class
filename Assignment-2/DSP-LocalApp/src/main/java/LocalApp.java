import com.amazonaws.auth.*;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduce;
import com.amazonaws.services.elasticmapreduce.AmazonElasticMapReduceClient;
import com.amazonaws.services.elasticmapreduce.model.*;
import java.io.*;

public class LocalApp {
    public static void main(String[] args) throws IOException {
        // get argument
        if (args.length != 2) {
            System.out.println("Invalid number of Arguments");
            System.exit(1);
        }

        AWSCredentialsProvider credentials = DefaultAWSCredentialsProviderChain.getInstance();
        AmazonElasticMapReduce mapReduce = new AmazonElasticMapReduceClient(credentials.getCredentials());

        //------------------- config for BiGramCountMR
        HadoopJarStepConfig hadoopJarStep = new HadoopJarStepConfig()
                .withJar("s3://buckettestefipan123/DSP2-MRs-BiGramCount.jar") // This should be a full map reduce application.
                .withMainClass("MRs.BiGramCount")
                .withArgs(args);

        StepConfig stepConfig = new StepConfig()
                .withName("BiGramCount")
                .withHadoopJarStep(hadoopJarStep)
                .withActionOnFailure("TERMINATE_JOB_FLOW");

        JobFlowInstancesConfig instances = new JobFlowInstancesConfig()
                .withInstanceCount(6)
                .withMasterInstanceType(InstanceType.M4Large.toString())
                .withSlaveInstanceType(InstanceType.M4Large.toString())
                .withHadoopVersion("3.3.0")
                .withKeepJobFlowAliveWhenNoSteps(false)
                .withPlacement(new PlacementType("us-east-1a"))
                .withEc2KeyName("efiSP");

        RunJobFlowRequest runFlowRequest = new RunJobFlowRequest()
                .withName("BiGramCount")
                .withInstances(instances)
                .withSteps(stepConfig)
                .withReleaseLabel("emr-5.33.0")
                .withLogUri("s3n://buckettestefipan123/logs");

            runFlowRequest.setServiceRole("EMR_DefaultRole");
            runFlowRequest.setJobFlowRole("EMR_EC2_DefaultRole");
            System.out.println("starting job...");
            RunJobFlowResult runJobFlowResult = mapReduce.runJobFlow(runFlowRequest);
            String jobFlowId = runJobFlowResult.getJobFlowId();
            System.out.println("Ran job flow with id: " + jobFlowId);
    }
}
