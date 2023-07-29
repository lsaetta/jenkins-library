def call(){
	pipeline {
    	agent any
      	stages {
    		stage('clone project') {
    			steps {
            		echo "OK"
          		}
  		  	}
      	}
   	} 
}
