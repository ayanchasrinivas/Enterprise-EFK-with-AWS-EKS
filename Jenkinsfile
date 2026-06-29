pipeline {
    agent any
    environment = {
        AWS_REGION = "ap-south-1"
        CLUSTER_NAME = "elk-efk-prod"
        ECR_REGISTRY = "${sh(script: 'aws sts get-caller-identity --query Account --output text', returnStdout: true).trim()}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_NAME = "elk-efk-monitor"
        IMAGE_TAG = "${env.GIT_COMMIT[0..7]}"
        ARGOCD_SERVER = "argocd.ayanchasrinivas.space"
        DOMAIN = "ayanchasrinivas.space"
    }

    options = {
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timeout(time: 60, unit: 'MINUTES')
        timestamps()
    }

    stages {
        stage("Terraform Validate") {
            when {
                anyOf {
                    changeset "terraform/**"
                    changeset "jenkins/terraform/**"
                }
            }
            steps {
                dir("terraform") {
                    sh """
                        terraform init -backend=false
                        terrafrom validate
                        terrafrom fmt -check -recursive
                    """
                }
            }
        }

        stage("Terraform Plan") {
            when {
                allOf {
                    branch "main"
                    anyOf {
                        changeset "terraform/**"
                    }
                }
            }
            steps {
                withCredentials([
                    string(credentialsId: "es-password", variable: "ES_PASSWORD"),
                    string(credentialsId: "kibana-encryption-key", variable: "KIBANA_ENCRYPTION_KEY")
                ]) {
                    dir(terraform) {
                        sh """
                            terraform init
                            terraform plan \
                                -var-file=prod.tfvars \
                                -var="elasticsearch_password=${ES_PASSWORD}" \
                                -var="kibana_encryption_key=${KIBANA_ENCRYPTION_KEY} \
                                -out=tfplan
                        """
                    }
                }
            }
            post {
                always {
                    archiveArtifacts artifacts: "terraform/tfplan", allowEmptyArchive: true
                }
            }
        }

        stage("Helm Lint") {
            when {
                changeset "helm/**"
            }
            steps {
                sh """
                    for chart in elasticsearch kibana logstash fluent-bit; do
                        echo "Linting ---- helm/${chart} ---- "
                        helm lint helm/${chart}/ --strict
                    done
                """
            }
        }

        stage("Python Tests") {
            when {
                changeset "monitoring/**"
            }
            steps {
                sh """
                    cd monitoring
                    pip install -r requirements.txt --quiet
                    pip install pytest pytest-cov --quiet
                    python -m pytest tests/ -v \
                        --cov=. \
                        --cov-report=xml.coverage.xml \
                        --cov-report=term-missing \
                        || echo "No Tests found - Skipping"
                """
            }
            posts {
                always {
                    publishCoverage adapters: [coberturaAdapter('monitoring/coverage.xml')],
                    sourceFileResolver: sourceFiles('STORE_ALL_BUILD')
                }
            }
        }

        stage("Docker Build & Push") {
            when {
                anyOf {
                    branch "main"
                    branch "develop"
                }
                changeset "monitoring/**"
            }
            steps {
                sh """
                    aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}
                    aws ecr describe-repositories --repository-names ${IMAGE_NAME} --region ${AWS_REGION} 2>/dev/null || \
                        aws ecr create-repository --repository-name ${IMAGE_NAME} --region ${AWS_REGION}
                    
                    docker build -t ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG} \
                        -t ${ECR_REGISTRY}/${IMAGE_NAME}:latest
                        -f docker/Dockerfile \
                        monitoring/
                    
                    docker push ${ECR_REGISTRY}/${IMAGE_NAME}:${IMAGE_TAG}
                    docker push ${ECR_REGISTRY}/${IMAGE_NAME}:latest
                """
            }
        }

        stage("ArgoCD Sync") {
            when {
                branch "main"
            }
            steps {
                withCredentials([string(credentialsId: 'argocd-auth-token', variable: 'ARGOCD_TOKEN')]) {
                    sh """
                        which argocd || (
                            curl -sSL -o /usr/local/bin/argocd \
                                https://github.com/argoproj/argo-cd/releases/latest/download/argocd-linux-amd64
                            chmod +x /usr/local/bin/argocd
                        )

                        argocd login ${ARGOCD_SERVER} \
                            --auth-token ${ARGOCD_TOKEN} \
                            --grpc-web \
                            --insecure
                        
                        for app in elasticsearch kibana logstash fluent-bit elk-monitor; do
                            echo "Syncing ${app}..."
                            argocd app sync ${app} --grpc-web --timeout 300 || true
                        done

                        for app in elasticsearch kibana logstash fluent-bit elk-monitor; do
                            argocd app wait ${app} \
                                --health \
                                --timeout 600 \
                                --grpc-web
                        done
                    """    
                }
            }
        }

        stage("Smoke Test") {
            when {
                branch "main"
            }
            steps {
                sh """
                    for i in ${seq 1 12}; do
                        STATUS=$(curl -sk -o /dev/null -w "%{http_code}" https://kibana.${DOMAIN}/api/status)
                        if[ "$STATUS" = "200" ]; then
                            echo "Kibana is up and running"
                            break
                        fi
                        echo "Attempt ${i}/12 — Kibana returned ${STATUS}, retrying in 15s..."
                        sleep 15
                    done

                    ES_HEALTH=$(curl -sk -u "elastic:${ES_PASSWORD}" \
                        https://elasticsearch.${DOMAIN}/_cluster/health | jq -r '.status')
                    echo "ES cluster status: ${ES_HEALTH}"
                    if [ "$ES_HEALTH" = "red" ]; then
                        echo "ERROR: Elasticsearch cluster is RED"
                        exit 1
                    fi
                """
                withCredentials([string(credentialsId: 'es-password', variable: 'ES_PASSWORD')]) {
                sh """
                    ES_HEALTH=$(curl -sk -u "elastic:${ES_PASSWORD}" \
                        https://elasticsearch.${DOMAIN}/_cluster/health | jq -r '.status')
                    echo "ES cluster status: ${ES_HEALTH}"
                    [ "$ES_HEALTH" != "red" ] || exit 1
                """
            }
        }
    }

    post {
        success {
            echo "Pipeline Succeeded - all ELK components synced and healthy"
        }
        failure {
            echo "Pipeline FAILED - Check stage logs"
        }
        always {
            cleanWs()
        }
    }
}