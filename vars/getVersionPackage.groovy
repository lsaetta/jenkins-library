def call(){
	stage('getVersionPackage') {
		steps {
			script {
				PACKAGE_VERSION = sh(script: '''awk -F'"' '/"version": ".+"/{ print $4; exit; }' ${PACKAGE_PATH}''', returnStdout: true).trim()
				PACKAGE_MAJOR = PACKAGE_VERSION[0..0]
			}
			sh "echo PACKAGE_VERSION ${PACKAGE_VERSION}"
			sh "echo PACKAGE_MAJOR ${PACKAGE_MAJOR}"
		}
	}
}

