pipeline {
    agent any

    environment {
        ECR_REPO = "331220047086.dkr.ecr.us-east-1.amazonaws.com/adservice"
        S3_BUCKET = "satishpolaka-artifacts-botique"
        IMAGE_TAG = "v${BUILD_NUMBER}"
    }

    stages {
        stage('Build & Tag Docker Image') {
            steps {
                sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
            }
        }

        stage('Push to ECR') {
            steps {
                sh "aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin ${ECR_REPO}"
                sh "docker push ${ECR_REPO}:${IMAGE_TAG}"
            }
        }

        stage('Upload JAR to S3') {
            steps {
                sh "./gradlew build -x test"
                sh "aws s3 cp build/libs/hipstershop-*.jar s3://${S3_BUCKET}/adservice/${IMAGE_TAG}/hipstershop.jar"
            }
        }
    }
}
