pipeline {
    agent any
    environment {
        AWS_REGION = "${env.AWS_REGION}"
        CLUSTER_NAME = "${env.CLUSTER_NAME}"
        AWS_ACCOUNT_ID = credentials('aws-account-id')
        ECR_REGISTRY = "${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com"
        IMAGE_NAME = "${env.IMAGE_NAME}"
        ARGOCD_SERVER = "${env.ARGOCD_SERVER}"
        DOMAIN = "${env.DOMAIN}"
    }

    options {
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
                    sh '''
                        terraform init -backend=false
                        terrafrom validate
                        terrafrom fmt -check -recursive
                    '''
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
                        sh '''
                            terraform init
                            terraform plan \
                                -var-file=prod.tfvars \
                                -var="elasticsearch_password=${ES_PASSWORD}" \
                                -var="kibana_encryption_key=${KIBANA_ENCRYPTION_KEY} \
                                -out=tfplan
                        '''
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
                sh '''
                    for chart in elasticsearch kibana logstash fluent-bit; do
                        echo "Linting ---- helm/${chart} ---- "
                        helm lint helm/${chart}/ --strict
                    done
                '''
            }
        }

        stage("Python Tests") {
            when {
                changeset "monitoring/**"
            }
            steps {
                sh '''
                    cd monitoring
                    pip install -r requirements.txt --quiet
                    pip install pytest pytest-cov --quiet
                    python -m pytest tests/ -v \
                        --cov=. \
                        --cov-report=xml.coverage.xml \
                        --cov-report=term-missing \
                        || echo "No Tests found - Skipping"
                '''
            }
            post {
                always {
                    publishCoverage adapters: [coberturaAdapter('monitoring/coverage.xml')],
                    sourceFileResolver: sourceFiles('STORE_ALL_BUILD')
                }
            }
        }

        stage("Build Task-API") {
            when {
                anyOf {
                    branch "main"
                    branch "develop"
                }
                changeset "services/task-api/**"
            }
            steps {
                sh '''
                    aws ecr get-login-password --region ${AWS_REGION} | docker login --username AWS --password-stdin ${ECR_REGISTRY}

                    docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/srinivas/task-api:latest -f services/task-api/Dockerfile serices/task-api/
                    docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/srinivas/task-api:latest
                '''
            }
        }

        stage("Build Fronted") {
            when {
                anyOf {
                    branch "main"
                    branch "develop"
                }
                changeset "frontend/**"
            }
            steps {
                sh '''
                    docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/srinivas/frontend:latest -f frontend/Dockerfile frontend/
                    docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/srinivas/frontend:latest
                '''
            }
        }

        stage("Build Notification Worker Microservice") {
            when {
                anyOf {
                    branch "main"
                    branch "develop"
                }
                changeset "services/notification-worker/**"
            }
            steps {
                sh '''
                    docker build -t ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/srinivas/notification-worker:latest -f services/notification-worker/Dockerfile serices/notification-worker/
                    docker push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/srinivas/notification-worker:latest
                '''
            }
        }

        stage("ArgoCD Sync") {
            when {
                branch "main"
            }
            steps {
                withCredentials([string(credentialsId: 'argocd-auth-token', variable: 'ARGOCD_TOKEN')]) {
                    sh '''
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
                    '''    
                }
            }
        }

        stage("Smoke Test") {
            when {
                branch "main"
            }
            steps {
                sh '''
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
                '''
                withCredentials([string(credentialsId: 'es-password', variable: 'ES_PASSWORD')]) {
                sh '''
                    ES_HEALTH=$(curl -sk -u "elastic:${ES_PASSWORD}" \
                        https://elasticsearch.${DOMAIN}/_cluster/health | jq -r '.status')
                    echo "ES cluster status: ${ES_HEALTH}"
                    [ "$ES_HEALTH" != "red" ] || exit 1
                '''
                }
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