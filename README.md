BT Fair Billing Technical Assessment
====================================

Compile using: 

	mvn clean package
	
Run from the project root using:

    java -jar target/fairbilling-1.0-SNAPSHOT.jar com.chieftainsys.sample.FairBilling test.log
    
A sample file test.log is in the project root.    	

Note: The assessment specification does not specify the use of a .jar, but
      this is what I would expect to use in a Java application, unless the
      code was all in a single file (which violates the SOLID principles),
      or the environment classpath was modified to include current directory.
      
Note: Java 8 bulk collections syntax avoided intentionally.

Note: Command-line tested on Git Bash on Windows, as I don't have an Ubuntu
      distro set up at the moment.      