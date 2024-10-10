pipeline {
    agent any
    tools {
        jdk 'jdk'         // Make sure 'jdk' is the name used in your Global Tool Configuration
        maven 'maven3'    // Make sure 'maven3' is the name used in your Global Tool Configuration
    }
    environment{
        SCANNER_HOME= tool 'sonar-scanner'
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
                dependencyCheck additionalArguments: '--scan ./', odcInstallation: 'dp'  // Make sure 'dp' is the name used in your Global Tool Configuration for OWASP Dependency-Check
                dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
        stage('sonarqube') {
            steps {
                withSonarQubeEnv('sonar-server'){       //make sure sonar-server is the name in system configuration
                    sh ''' $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=Shopping-Cart \
                    -Dsonar.java.binaries=. \
                    -Dsonar.projectkey=Shopping-Cart '''   // shopping-cart is the name of the application
                    }
             }
        }
        stage('build') {
            steps {
                sh "mvn clean package"    // is their any bugs to skip use -DskipTests=true 
            }
        }
        stage('docker build & push') {
            steps {
                script{
                    withDockerRegistry(credentialsId: 'ac77104d-6fa8-4ca8-bfda-b65269a9f965', toolName: 'docker')
                    sh "docker build -t shopping-cart -f docekr/Dockerfile ,"
                    sh "docker tag shopping-cart siva1369/shoping-cart:latest"
                    sh "docekr push siva1369/shopping-cart:latest"
                }
       
            }
        }
        stage('deploy') {
            steps {
                script{
                    withDockerRegistry(credentialsId: 'ac77104d-6fa8-4ca8-bfda-b65269a9f965', toolName: 'docker')
                    sh "docker run -d --name shop-shop -p 8070:8070 siva1369/shopping-cart:latest"           // port number should be in docker file
                    
                }
       
            }
        }
    }
}    

