def call(String branchName, String credId, String gitUrl){
	steps {
		git branch: branchName, changelog: false, credentialsId: credId, poll: true, url: gitUrl
		dir('kubernetes') {
			git branch: 'development', changelog: false, credentialsId: credId, poll: true, url: 'git@geogitlab1.intersistemi.it:geosystems/kubernates.git'
		}
	}
}
