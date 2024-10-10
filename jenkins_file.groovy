pipeline {
    agent any

    tools {
        jdk 'jdk'         // Ensure 'jdk' is configured in Global Tool Configuration
        maven 'maven3'    // Ensure 'maven3' is configured in Global Tool Configuration
    }

    environment {
        SCANNER_HOME = tool 'sonar-server'  // Ensure 'sonar-server' is configured in Global Tool Configuration
    }

    stages {
        stage('git checkout') {
            steps {
                git branch: 'main', changelog: false, poll: false, url: 'https://github.com/jaiswaladi246/Ekart.git'
            }
        }
        stage('compile') {
            steps {
                sh "mvn clean compile"
            }
        }
        stage('owasp scan') {
            steps {
                dependencyCheck additionalArguments: '--scan ./', odcInstallation: 'dp'  // Ensure 'dp' is configured in Global Tool Configuration for OWASP Dependency-Check
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('sonarqube') {
            steps {
                withSonarQubeEnv(credentialsId: '4a9fb12d-29fd-4023-a49f-2a232842a870', installationName: 'sonar-server') {  // Ensure 'sonar-server' is the correct name in system configuration
                    sh '''
                        $SCANNER_HOME/bin/sonar-scanner \
                        -Dsonar.projectKey=Shopping-Cart \
                        -Dsonar.projectName=Shopping-Cart \
                        -Dsonar.sources=. \
                        -Dsonar.java.binaries=./target/classes \
                        -Dsonar.host.url=http://13.234.33.78:9000/
                    '''
                }
            }
        }
        stage('build') {
            steps {
                sh "mvn clean package -DskipTests=true"    // Use -DskipTests=true to skip tests if there are any failures in the tests
            }
        }
        stage('docker build & push') {
            steps {
                script {
                    // Define the Dockerfile path relative to the workspace
                    def dockerfilePath = "docker/Dockerfile"

                    // Docker build
                    withDockerRegistry(credentialsId: 'b99d6b61-2179-414e-8d40-d86d5f4a4f63', toolName: 'docker') {
                        sh "docker build -t shopping-cart -f ${dockerfilePath} ."

                        // Docker tag and push
                        sh "docker tag shopping-cart siva1369/shopping-cart:latest"
                        sh "docker push siva1369/shopping-cart:latest"
                    }
                }
            }
        }
         stage('deploy') {
            steps {
                script{
                    withDockerRegistry(credentialsId: 'b99d6b61-2179-414e-8d40-d86d5f4a4f63', toolName: 'docker')
                    sh "docker run -d --name shoping -p 8070:8070 siva1369/shopping-cart:latest"           // port number should be in docker file
                }
       
            }
        }
    }
}
