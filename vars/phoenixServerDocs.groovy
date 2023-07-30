def call (String BRANCH_NAME, String CRED_ID, String PROJECT, String GIT_PROJECT_URL, String PHXVER, String RJSVER, String PACKAGE_VERSION, String PACKAGE_MAJOR) {
		
	//'lou_dev_v1.0.5'


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
						PACKAGE_VERSION = sh(script: '''grep -m1 "<version>" pom.xml | grep -oP  "(?<=>).*(?=<)"''', returnStdout: true).trim()
						PACKAGE_MAJOR = PACKAGE_VERSION[0..0]
					}
					sh "echo ${PACKAGE_MAJOR}"
				}
			}
			stage('build') {
				steps {
					sh "cp ./kubernetes/template_phoenix/backend/maven_settings.xml ./"
					sh "ls -la"
					sh "docker login ${NEXUS_DOCKER_PUSH_URL} -u geosystems -p developer"
					sh "docker build --no-cache --build-arg MAJOR=${PACKAGE_MAJOR} -t ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-${PACKAGE_MAJOR}-docs ."
					sh "docker tag ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-${PACKAGE_MAJOR}-docs ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-${PACKAGE_MAJOR}-docs:${PACKAGE_VERSION}.${BUILD_NUMBER}"
					sh "docker push -a ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-${PACKAGE_MAJOR}-docs"
					sh "docker rmi ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-${PACKAGE_MAJOR}-docs:${PACKAGE_VERSION}.${BUILD_NUMBER}"
				}
			}
			stage('deploy') {
				steps {
					sh "ls ./kubernetes/helm_argocd_frontend/ -la"
					sh "docker build --no-cache --build-arg APPNAME=${PROJECT}-${PACKAGE_MAJOR}-docs --build-arg TAGIMG=${PACKAGE_VERSION}.${BUILD_NUMBER} -t k8sctl-${PROJECT}-${PACKAGE_MAJOR} -f ./kubernetes/helm_argocd_frontend/Dockerfile ./kubernetes/helm_argocd_frontend/"
					sh "docker rmi k8sctl-${PROJECT}-${PACKAGE_MAJOR}"
				}
			}
		}
	}


}
