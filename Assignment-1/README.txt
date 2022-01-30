##DSP - Assignment 1
Efi Asherov - 208439000
Pan Eyal - 208722058

##Running Instructions:
-> open a terminal/console in the localApp.jar folder
-> run the following command:
	>java -jar localApp.jar inputFileName1... inputFileNameN outputFileName1... outputFileNameN n [terminate]
-> where:
	* "inputFileName1... inputFileNameN" - the locations of the input files
	* "outputFileName1... outputFileNameN" - the location of the output files
	* "n" - the number of desired workers per message per file
	* "terminate" - a flag for terminating the manager

##Program description:

--- >LocalAPP.jar ---

-> initiation:
	* checks if terminate message should be sent in the end of the procedure
	* create a bucket in S3 for uploading the input files, and receive the output files
	* create a SQS queue uniquely named "Manager2LocalApp" + System.currentTimeMillis()
	* validate that the manager is running, if not, create a SQS queue named "LocalApp2Manager" and create the EC2 instance of the manager with does settings:
		- using the AMI image "ManagerAmi" with ID "ami-0c5032b46b0d86577" of type "T2_MEDIUM", it installed with java and contains the manager.jar file necessary for running the manager application
		- using an arn role "arn:aws:iam::871952098085:instance-profile/ManagerAndWorker" that let it use the necessary AWS services.


-> uploads the input files to S3
-> send to a queue named "LocalApp2Manager" a message that states:
	* the name of the uniquely Queue relevant to this LocalApp
	* the name of the uniquely bucket relevant to this LocalApp
	* the name of the input file
	* the number of workers per n messages

-> in case of an error in the upload, it concludes that the manager received a terminated message by another LocalApp and therefore the "LocalApp2Manager" queue is no longer available. LocalApp stops sending other files.

-> read from the "Manager2LocalApp" queue messages about file completion for each file that was read successfully by the manager.

-> when a message is received, the message includes the output file name in S3 and therefore download the file. Every line in the file is in HTML format that fits a line in a table, HTMLMaker is a class that chains those lines together and makes the HTML output file with the name that was specified in the initial arguments.

-> delete the output file from S3.

-> while trying to receive messages from Manager, check if the manager has crashed, and if so exit from the while-loop and close all necessary AWS services. wait 60 seconds so there wouldn't be an SQS error if the user will try to run it again. 

-> if it wasn't crushed, but the manager was terminated through another LocalApp, then delete all received files from the S3 bucket.

-> if the manager is still running, all the files have downloaded successfully and a termination argument was received, send a termination message to the manager through the LocalApp2Manager queue.

-> delete "Manager2LocalAppQueue" from the SQS and the related bucket in S3.

--- /LocalAPP.jar ---


--- >Manager.jar ---

-> create "Manager2Workers" and "Workers2Manager" SQS queues

-> starts 2 Threads, one for handling messages from LocalApp and sending jobs to workers, and one for handling messages from Workers and sending completion messages to LocalApp

-> handleMessageFromLocalApp Thread:
	-> get messages from "LocalApp2Manager" queue

	-> check if the message is terminate, if so stop receiving any other messages from this queue, else continue:

	-> download the input file from S3. every line of the file is in a JSON format that represent a review, send it to the "Manager2Workers" as a message that states:
		* the name of the uniquely Queue relevant to this File
		* the name of the input file
		* the number of this job
		* the JSON string

	-> check if there are sufficient workers as defined by the number of workers per n messages, and if not initiate another worker instance with does settings:
		- using the AMI image "WorkerAmi" with ID "ami-0192d6b6e43e650ea" of type "T2_MEDIUM", it installed with java and contains the worker.jar file necessary for running the worker application
		- using an arn role "arn:aws:iam::871952098085:instance-profile/ManagerAndWorker" that let it use the necessary AWS services.

	-> add to a HashMap correlation between LocalApp to another HashMap that currelats between a File and all the reviews it contains, It will be represented by a class named OutputValues

-> handleMessageFromWorkers Thread:

	-> while the manager shouldn't terminate or there are still messages from worker it expect to receive continue:

	-> get messages from "Worker2Manager" queue

	-> check if the currelating file is not already completed (because duplication of jobs output may occur if two workers reads the same message from the SQS queue)

	-> add to the correlation OutputValue Class the output from the worker.

	-> if the OutputValue received all the files it expects:
		-> return a completeMultipartUploadRequest, that then will be used to upload the summary file to S3 relevant bucket
		-> remove the relevant key and value from the HashMaps

	-> when the manager changed flag to terminate and no messages left from worker to handle, then:
		-> terminate all the workers instances

		-> delete "Manager2Workers" and "Workers2Manager" SQS queues

		-> terminate the manager instance

--- /Manager.jar ---

--- Worker.jar ---
-> receive a message from "Manager2Workers" SQS queue

-> create a class from the JSON string that will represent the review

-> find the Sentiment level of the review by taking the distance from the Sarcasm level in the review and the ratings the user gave in his review. we find the sarcasm level through stanford libraries.

-> assign the right color to the review accordingly to the sentimental level in the following way:
	* 1 = dark red
	* 2 = red
	* 3 = black
	* 4 = green
	* 5 = dark green

-> find the Entities for PERSON, LOCATION or ORGANIZATION through stanford libraries

-> make a string representing a line in a table in an HTML format that will be colored by the assigned color we found and it will contain:
	* the link for the review
	* a list of entities found in the review
	* the sarcasm level of the review

-> send the string to the "Worker2Manager" queue

--- /Worker.jar ---

##Running Values
we used n = 10, every file has 50 reviews to cover, means 5 workers will be created and everyone would roughly have 50 reviews to process.
It took the program 13 minutes to finish.

##Other Marks
security: 
- we don't pass any credentials, we made a user role for the instances

scalability: 
- the program will create as many workers as needed, the only drawback is the RAM on the manager that will need to send and assemble all the jobs/results from the workers once finished.

persistence: 
- if a worker instance dies, another worker will get it's job because the message of the job didn't get deleted and after the visibility time of the message will pass, it will be available for the other workers. 
- if it takes a long time for a worker to finish processing the message another worker will take it, and by the way we implemented it, the manager will ignore duplicate messages.
- if a manager instance dies unexpectedly, we made sure all AWS services are shutting down and nothing keeps running, the user will need to run the program again.

thread:
- we decided to give the manager two threads, one for input files and one for output, those processes are independent with each other.
- giving a couple of threads to the same process that reads and sends SQS messages is generally bad, because you might send or receive duplication that way.

termination: we made sure all AWS services are shutting down and nothing keeps running when a termination message sent

system limitations: we choose the EC2 instances to be as small as needed

workers: 
- all of the workers are working hard and equally the jobs, if a worker is not working on a job he will try to recieve a message from the "Manager2Worker" queue

defined: 
- each part of our system has properly defined tasks as described above in the Program description.

distirbution: 
- we noticed when running our program that the "bottleneck" of it is when the manager is waiting to achive all the parts of the files he needs. therefore, the distributed work is distributed on all the workers equally, and that's was the goal.

