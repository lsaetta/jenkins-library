def getVersion(String filePath){
	steps {
		script {
			if (filePath.contains("package.json")) { 
				PACKAGE_VERSION = sh(script: '''awk -F'"' '/"version": ".+"/{ print $4; exit; }' ${filePath}''', returnStdout: true).trim()
			} else{ 
				PACKAGE_VERSION = sh(script: '''grep -m1 "<version>" ${filePath} | grep -oP  "(?<=>).*(?=<)"''', returnStdout: true).trim()
			}			
		}
	}
}

