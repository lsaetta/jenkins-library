def call() {
	stage('cloneProject') {
		steps {
			git branch: BRANCH_NAME, changelog: false, credentialsId: CRED_ID, poll: true, url: GIT_PROJECT_URL
			dir('kubernetes') {
				git branch: 'development', changelog: false, credentialsId: CRED_ID, poll: true, url: 'git@geogitlab1.intersistemi.it:geosystems/kubernates.git'
			}
		}
	}
}
