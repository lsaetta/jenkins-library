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
						PACKAGE_VERSION = sh(script: '''awk -F'"' '/"version": ".+"/{ print $4; exit; }' ./package.json''', returnStdout: true).trim()
					}
				}
			}
			
			stage('clean artifact') {
				steps {
					sh "echo 'registry=http://${NEXUS_NPM_URL}/repository/geosystems-npm-group/' > .npmrc"
					//sh "curl -X DELETE -u geosystems:developer http://10.199.22.70:30081/repository/geosystems-npm-release/ramanijs"
				}
			}
			stage('build') {
				steps {

					sh "docker login ${NEXUS_DOCKER_PUSH_URL} -u geosystems -p developer"
					sh "docker build ${DOCKER_CACHE} --build-arg APPNAME=${PROJECT} -t ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} ."
					sh "docker tag ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}:${PACKAGE_VERSION}.${BUILD_NUMBER}"
					sh "docker push -a ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}"
					sh "docker rmi ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}"
				}
			}
			stage('deploy') {
				steps {
					sh "ls ./kubernetes/helm_argocd_frontend/ -la"
					sh "docker build ${DOCKER_CACHE} --build-arg APPNAME=${PROJECT} --build-arg TAGIMG=${PACKAGE_VERSION}.${BUILD_NUMBER} -t k8sctl-${PROJECT} -f ./kubernetes/helm_argocd_frontend/Dockerfile ./kubernetes/helm_argocd_frontend/"
					sh "docker rmi k8sctl-${PROJECT}"
				}
			}
			
		}
	}
}