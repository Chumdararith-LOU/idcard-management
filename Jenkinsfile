pipeline {
    agent any

    triggers {
        pollSCM('*/5 * * * *')
    }

    stages {
        stage('Build and Test') {
            steps {
                echo 'Starting Automated Build and Test Phase...'
                sh 'mvn clean test -Dspring.profiles.active=test'
            }
        }

        stage('Deploy to Web Server') {
            steps {
                echo 'Build and Test passed successfully! Initiating deployment via Ansible...'
                sh 'ansible-playbook -i inventory.ini deploy-playbook.yml'
            }
        }
    }

    post {
        failure {
            echo 'Build, Test, or Deployment failed! Sending out error notification emails...'
            
            emailext (
                subject: "BUILD FAILED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'",
                body: """The build pipeline has encountered an error.
                         Please review the live console logs at: ${env.BUILD_URL}""",
                to: 'srengty@gmail.com',
                recipientProviders: [
                    [$class: 'DevelopersRecipientProvider'], 
                    [$class: 'CulpritsRecipientProvider']    
                ]
            )
        }
    }
}