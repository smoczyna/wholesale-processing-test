pipeline {
    agent any

    stages {
        stage('Build') {
            steps {
                sh 'maven clean' 
                 archiveArtifacts artifacts: 'sftp://khanaas@170.127.114.148/app/deploy/', fingerprint: true 
	
            }
        }
    }
}