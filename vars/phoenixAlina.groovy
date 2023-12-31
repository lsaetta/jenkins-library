def call (String BRANCH_NAME, String CRED_ID, String PROJECT, String GIT_PROJECT_URL, String PHXVER, String RJSVER, String PACKAGE_VERSION) {

	pipeline {
		agent any
		stages {
			stage('clone project') {
				steps {
					git branch: BRANCH_NAME, changelog: false, credentialsId: CRED_ID, poll: true, url: GIT_PROJECT_URL
					dir('kubernetes') {
						git branch: 'development', changelog: false, credentialsId: CRED_ID, poll: false, url: 'git@geogitlab1.intersistemi.it:geosystems/kubernates.git'
					}
				}
			}
			stage('build') {
				steps {
					sh "docker login ${NEXUS_DOCKER_PUSH_URL} -u geosystems -p developer"
					sh "docker build ${DOCKER_CACHE} -t ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} -f ./Dockerfile ."
					sh "docker tag ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}:${PACKAGE_VERSION}.${BUILD_NUMBER}"
					sh "docker push ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}:${PACKAGE_VERSION}.${BUILD_NUMBER}"
					sh "docker rmi ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} -f"
					//sh "docker image prune -a --force --filter 'until=10m'"
				}
			}
			stage('deploy') {
				steps {
					sh "ls ./kubernetes/helm_argocd_alina/ -la"
					sh "docker build ${DOCKER_CACHE} --build-arg APPNAME=${PROJECT} --build-arg TAGIMG=${PACKAGE_VERSION}.${BUILD_NUMBER} -t k8sctl-${PROJECT} -f ./kubernetes/helm_argocd_alina/Dockerfile ./kubernetes/helm_argocd_alina/"
					sh "docker rmi k8sctl-${PROJECT}"
				}
			}
		}
	}




}