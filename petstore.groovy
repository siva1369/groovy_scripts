pipeline {
    agent any
    tools {
        jdk 'jdk17'
        maven 'maven'
    }
    
    environment {
        SCANNER_HOME= tool 'sonar-scanner'
    }

    stages {
        stage('Git Checkout') {
            steps {
                git changelog: false, poll: false, url: 'https://github.com/jaiswaladi246/jpetstore-6.git'
            }
        }
        stage('Code Compile') {
            steps {
                sh 'mvn clean compile'
            }
        }
        stage('SonarQube Analysis') {
            steps {
                sh '''
                $SCANNER_HOME/bin/sonar-scanner \
                    -Dsonar.host.url=http://65.2.146.101:9000/ \
                    -Dsonar.login=squ_7715943e09522066e851dd736cf67068d438654c \
                    -Dsonar.projectName=petstore \
                    -Dsonar.java.binaries=target/classes \
                    -Dsonar.projectKey=petstore \
                    -X
                '''
            }
        }
        stage('OWASP Scan') {
            steps {
                dependencyCheck additionalArguments: '--scan ./', odcInstallation: 'DP'
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        
        stage('code build') {
            steps {
                sh 'mvn clean install -DskipTests=true' 
            }
        }
        stage('docker image build & push') {
            steps {
                script {
                    withDockerRegistry(credentialsId: 'docker_hub', toolName: 'docker') {
                        sh "docker build -t petstore"
                        sh "docker tag petstore siva1369/petstore:latest"
                        sh "docker push siva1369/petstore:latest"
                    }
                }
            }
        }
        stage('trivy') {
            steps {
                sh 'trivy image siva1369/petstore:latest'
            }
        }
    }
}
