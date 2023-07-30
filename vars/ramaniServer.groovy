def call (String BRANCH_NAME, String CRED_ID, String PROJECT, String GIT_PROJECT_URL, String PHXVER, String RJSVER, String PACKAGE_VERSION) {
		
	pipeline {
		agent any
		stages {
			stage('clone project') {
				steps {
					git branch: BRANCH_NAME, changelog: false, credentialsId: CRED_ID, poll: true, url: GIT_PROJECT_URL
					dir('kubernetes') {
						git branch: 'development', changelog: false, credentialsId: CRED_ID, poll: true, url: 'git@geogitlab1.intersistemi.it:geosystems/kubernates.git'
					}
				}
			}
			stage('get version') {
				steps {
					script {
						PACKAGE_VERSION = sh(script: '''grep -m1 "<version>" ./ramani/pom.xml | grep -oP  "(?<=>).*(?=<)"''', returnStdout: true).trim()
					}
				}
			}
			stage('build') {
				steps {
					sh "cp ./kubernetes/template_phoenix/backend/maven_settings.xml ./ramani/"
					sh "ls ./ramani/ -la"
					sh "docker build ${DOCKER_CACHE} -t ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} -f ./ramani/Dockerfile ./ramani"
					sh "docker rmi ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} -f"
					sh "docker image prune -a --force --filter 'until=10m'"
				}
			}
		}
	}



}