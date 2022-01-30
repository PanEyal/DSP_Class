## DSP - Assignment 2
	# Efi Ashrov - 208439000
	# Pan Eyal - 208722058


## Program Flow
	# Initiate
		The program will run with the following command:
		> java -jar LocalApp.jar 0.5 0
		when 0.5 is the desired minimal PMI value, and 0 is the desired relative minimal PMI value. 

		In the LocalApp application, the program will create a MapReduce Job Request from the AWS, it will be configured with 
		DSP2-MRs-BiGramCount.jar Jar file that was preloaded into the S3 relevant bucket.
		We ran the application with 6 instances with type M4Large
		We sent all credentials throw a specific roles: "EMR_DefaultRole" and "EMR_EC2_DefaultRole"

	# Main Class
		Our main class creates 3 jobs that will be performed one after another.

	# Job1
		Receive the Google 2Gram-heb dataset in the format:
		> [[2gram], year, occurrences, pages, books]
		The map function will extract each line from the dataset and will build from it 2 key and value pairs:
		> [decade flag first/second | amount second/first]
		During the map reduce, we will keep count on the Bigrams in each decade and increment a specified counter to keep track of how many occurrences this bigram had in that decade.

			Format explanation:
			-Decade: the decade of the specified year.
			-Flag: 1 if the first word in the key, and 2 if the second is in the key.
			-Amount: the amount of occurrences of the relevant word in this year.

		The reduce function will sum all of the occurrences of each word (first or second separately),
		then will build from it the following key and value pairs:
		> [decade, flag, first/second | {sum = _ , otherBigram1 = _ , ... , otherBigramK = _ }]

			Format explanation:
			-The value of each key will be a MapWritable that will contain the sum of the first/second word that in the key, and each occurrences of it with any other bigram word.

		Job1 Output Example:
			153 1 אומר 	{בי=1, לישראל=1, לקרב=1, מלאכי=1, דור=2, sum=11, אלא=1, טוב=1, יי=1, לעם=1, ח=1}
			153 1 אמרה 	{בראשית=1, sum=3, תורה=1, אתמול=1}
			153 1 אנוכי 	{שולח=2, sum=5, בן=1, אומר=1, אנוכי=1}
			153 1 באותה 	{שעת=2, sum=3, שעה=1}
			153 1 בימי 	{משיח=3, אברהם=1, המשיח=2, שפוט=1, אלישע=1, מלכי=1, חרבן=1, דויד=1, sum=13, למך=1, בית=1}

	# Job2
		Receive the output from Job1 in the format:
		> [decade, flag, first/second, {sum = _ , otherBigram1 = _ , ... , otherBigramK = _ }]
		The map function will extract the sum value from the map.
		for each entry in the map it will build from it the following key and value pairs:
		> [decade first second | BigramAmount, firstAmount, secondAmount]

			Format explanation:
			*For flag 1:
			-BigramAmount: the amount of occurrences of the Bigram in the decade
			-firstAmount: the amount of occurrences of the first word in the decade
			-secondAmount: 0

			*For flag 2:
			-BigramAmount: the amount of occurrences of the Bigram in the decade
			-firstAmount: 0
			-secondAmount: the amount of occurrences of the second word in the decade

		The reduce function will sum separately the BigramAmount, firstAmount, secondAmount for each bigram in the desired decade.
		then will build from it the following key and value pairs with the updated final values:
		> [decade first second | BigramAmount, firstAmount, secondAmount]

			Format explanation:
			-BigramAmount: the amount of occurrences of the Bigram in the decade
			-firstAmount: the amount of occurrences of the first word in the decade
			-secondAmount: the amount of occurrences of the second word in the decade

		Job2 Output Example:
			153 אומר לעם	1 11 2
			153 אור אלוהים	2 9 15
			153 אור הראשון	2 9 5
			153 אור ראשון	1 9 2
			153 אחדות רק	1 0 26
			153 אינו אומר	1 4 15


	# Job3
		Receive the output from Job2 in the format:
		> [decade first second, BigramAmount, firstAmount, secondAmount]
		The map function will send to the reduce with the following format:
		> [decade | first, second, BigramAmount, firstAmount, secondAmount]

			Format explanation:
			-The only necessary key will be the decade, to sum only the necessary bigram occurrences for it

		The reduce function will create a BiGramNPMI List for each bigram that will contain the bigram itself and the relevant npmi.
		then the reduce function will filter the List if it is a collocation according to the specified arguments that was sent from the LocalApp application.
		Then it will sort the list in descending order by their NPMI values and create a key value pair in the following format:
		> [decade | {bigram1 npmi, ..., bigramK npmi}]

			Format explanation:
			-decade: the result will be for this decade.
			-map: a list of the top-10 collocations ordered by their npmi.

		Job3 Output Example:
			------- DECADE: 153 ---------	
			שפכי כמים    1.0
			שעה נוטל    1.0
			שכינה כמשה    1.0
			שהמלך אוהבו    1.0
			שה תמים    1.0
			רומא הגדולה    1.0
			קמה באמה    1.0
			קורא אותן    1.0
			עדות אחרת    1.0
			נקראו עברים    1.0

## reports:
	# Top-10 collocations
		-We will add the top-10 collocation list in another text file next to this README file

	# Data from the Log File
		* Job1
		Map-Reduce Framework
			Map input records=252069581
			Map output records=237304292
			Reduce input records=237304292
			Reduce output records=3377007
		File System Counters
			FILE: Number of bytes read=3406768228
			FILE: Number of bytes written=5201418920
			HDFS: Number of bytes read=5304
			HDFS: Number of bytes written=0
			S3N: Number of bytes read=2581617934
			S3N: Number of bytes written=815695922

		* Job2
		Map-Reduce Framework
			Map input records=3377007
			Map output records=53641792
			Reduce input records=53641792
			Reduce output records=26820896
		File System Counters
			FILE: Number of bytes read=1288101312
			FILE: Number of bytes written=2079006586
			HDFS: Number of bytes read=1998
			HDFS: Number of bytes written=0
			S3N: Number of bytes read=815820111
			S3N: Number of bytes written=964197529

		* Job3
		Map-Reduce Framework
			Map input records=26820896
			Map output records=26820896
			Reduce input records=26820896
			Reduce output records=33
		File System Counters
			FILE: Number of bytes read=470333520
			FILE: Number of bytes written=946905352
			HDFS: Number of bytes read=1998
			HDFS: Number of bytes written=0
			S3N: Number of bytes read=964323781
			S3N: Number of bytes written=30220

	# Good/Bad Collocations
		5 Good:
			- שה תמים    1.0
			- שינוי טבע    1.0
			- נונ ומנונ    1.0
			- טמא שפתיים    1.0
			- תסמונת דאון    1.0
		Those pairs of words create a new meaning and can be expected to come together


		5 Bad:
			- מדלג ומקפץ    1.0
			- ואביו שמר    1.0
			- לפיסיקה ולכימיה    1.0
			- 328 329    1.0
			- תכתב ותחתם    1.0
		Those pairs of words can be connected to each other, but it doesn’t create a new meaning and not necessary always be connected.
