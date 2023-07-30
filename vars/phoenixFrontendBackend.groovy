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
						if (PACKAGE_VERSION == ""){
							PACKAGE_VERSION = sh(script: '''awk -F'"' '/"version": ".+"/{ print $4; exit; }' ./frontend/package.json''', returnStdout: true).trim()
						}
					}
				}
			}
			stage('builds') {
				parallel {
					stage('build frontend') {
						steps {
							sh "cp -n ./kubernetes/template_phoenix/frontend/Dockerfile ./frontend/Dockerfile"
							sh "ls ./frontend -la"
							sh "docker login ${NEXUS_DOCKER_PUSH_URL} -u geosystems -p developer"
							sh "docker build ${DOCKER_CACHE} --build-arg APPNAME=${PROJECT} --build-arg RJSVER=${RJSVER} --build-arg PHXVER=${PHXVER} -t ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} -f ./frontend/Dockerfile ./frontend"
							sh "docker tag ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}:${PACKAGE_VERSION}.${BUILD_NUMBER}"
							sh "docker push -a ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}"
							sh "docker rmi ${NEXUS_DOCKER_PUSH_URL}/${PROJECT} -f"
							sh "docker image prune -a --force --filter 'until=10m'"
						}
					}
					stage('build backend') {
						steps {
							sh "cp -n ./kubernetes/template_phoenix/backend/Dockerfile ./backend/Dockerfile"
							sh "cp ./kubernetes/template_phoenix/backend/maven_settings.xml ./backend/"
							sh "ls ./backend -la"
							sh "docker login ${NEXUS_DOCKER_PUSH_URL} -u geosystems -p developer"
							sh "docker build ${DOCKER_CACHE} --build-arg APPNAME=${PROJECT} -t ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-server -f ./backend/Dockerfile ./backend"
							sh "docker tag ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-server ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-server:${PACKAGE_VERSION}.${BUILD_NUMBER}"
							sh "docker push -a ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-server"
							sh "docker rmi ${NEXUS_DOCKER_PUSH_URL}/${PROJECT}-server -f"
							sh "docker image prune -a --force --filter 'until=10m'"
						}
					}
				}
			}
			stage('deploy') {
				steps {
					sh "ls ./kubernetes/helm_argocd/ -la"
					sh "docker build ${DOCKER_CACHE} --build-arg APPNAME=${PROJECT} --build-arg TAGIMG=${PACKAGE_VERSION}.${BUILD_NUMBER} -t k8sctl-${PROJECT} -f ./kubernetes/helm_argocd/Dockerfile ./kubernetes/helm_argocd/"
					sh "docker rmi k8sctl-${PROJECT}"
				}
			}
		}
	}
}