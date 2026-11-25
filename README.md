# Polyglot E-Commerce Microservices Project

A cloud-native, polyglot e-commerce application built with 12 microservices using multiple programming languages and frameworks. Each service is independently containerized, built via Jenkins CI/CD pipelines, and deployed to an AWS EKS Kubernetes cluster.

---

## Architecture Overview

The application is a fully functional online boutique store. Services communicate over gRPC and are deployed in the `webapps` namespace on EKS cluster `EKS-1` (region: `us-east-1`).

| Service | Language | Description | Port |
|---|---|---|---|
| `frontend` | Go | Serves the web UI, routes requests to all backend services | 8080 |
| `adservice` | Java (Gradle) | Serves targeted advertisements | 9555 |
| `cartservice` | C# (.NET) | Manages user shopping cart, backed by Redis | 7070 |
| `checkoutservice` | Go | Orchestrates the checkout flow across multiple services | 5050 |
| `currencyservice` | Node.js | Converts currency values | 7000 |
| `emailservice` | Python | Sends order confirmation emails | 8080 |
| `paymentservice` | Node.js | Processes payment transactions | 50051 |
| `productcatalogservice` | Go | Serves product listings from a JSON catalog | 3550 |
| `recommendationservice` | Python | Recommends products based on cart contents | 8080 |
| `shippingservice` | Go | Calculates shipping costs and estimates | 50051 |
| `loadgenerator` | Python (Locust) | Simulates user traffic for load testing | - |
| `redis-cart` | Redis | In-memory store for cart data | 6379 |

---

## Repository Branch Structure

Each service has its own dedicated branch in this repository:

| Branch | Contents |
|---|---|
| `main` | `deployment-service.yml` + `Jenkinsfile` for EKS deployment |
| `adservice` | Java/Gradle source, Dockerfile, Jenkinsfile |
| `cartservice` | C# source, Dockerfile, Jenkinsfile |
| `checkoutservice` | Go source, Dockerfile, Jenkinsfile |
| `currencyservice` | Node.js source, Dockerfile, Jenkinsfile |
| `emailservice` | Python source, Dockerfile, Jenkinsfile |
| `frontend` | Go source, static assets, templates, Dockerfile, Jenkinsfile |
| `loadgenerator` | Python Locust scripts, Dockerfile, Jenkinsfile |
| `paymentservice` | Node.js source, Dockerfile, Jenkinsfile |
| `productcatalogservice` | Go source, products catalog JSON, Dockerfile, Jenkinsfile |
| `recommendationservice` | Python source, Dockerfile, Jenkinsfile |
| `shippingservice` | Go source, Dockerfile, Jenkinsfile |
| `Infra-Steps` | AWS infrastructure setup guide |

---

## CI/CD Pipeline Flow

Each service branch has a `Jenkinsfile` that:
1. Builds a Docker image
2. Pushes it to Docker Hub (`satishpolaka/` or `satishpolaka/`)

The `adservice` additionally:
- Builds the JAR using Gradle
- Uploads the artifact to S3 bucket `satishpolaka-artifacts-botique`

The `main` branch `Jenkinsfile`:
1. Deploys all services to EKS using `kubectl apply -f deployment-service.yml`
2. Verifies deployment by listing services in the `webapps` namespace

---

## Prerequisites

### 1. AWS IAM User Setup

Create an IAM user and attach the following policies:
- `AmazonEC2FullAccess`
- `AmazonEKS_CNI_Policy`
- `AmazonEKSClusterPolicy`
- `AmazonEKSWorkerNodePolicy`
- `AWSCloudFormationFullAccess`
- `IAMFullAccess`
- Custom EKS policy:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "VisualEditor0",
            "Effect": "Allow",
            "Action": "eks:*",
            "Resource": "*"
        }
    ]
}
```

### 2. Install AWS CLI

```bash
curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
sudo apt install unzip
unzip awscliv2.zip
sudo ./aws/install
aws configure
```

### 3. Install kubectl

```bash
curl -o kubectl https://amazon-eks.s3.us-west-2.amazonaws.com/1.19.6/2021-01-05/bin/linux/amd64/kubectl
chmod +x ./kubectl
sudo mv ./kubectl /usr/local/bin
kubectl version --short --client
```

### 4. Install eksctl

```bash
curl --silent --location "https://github.com/weaveworks/eksctl/releases/latest/download/eksctl_$(uname -s)_amd64.tar.gz" | tar xz -C /tmp
sudo mv /tmp/eksctl /usr/local/bin
eksctl version
```

---

## EKS Cluster Setup

```bash
eksctl create cluster --name=EKS-1 \
                      --region=ap-south-1 \
                      --zones=ap-south-1a,ap-south-1b \
                      --without-nodegroup

eksctl utils associate-iam-oidc-provider \
    --region ap-south-1 \
    --cluster EKS-1 \
    --approve

eksctl create nodegroup --cluster=EKS-1 \
                       --region=ap-south-1 \
                       --name=node2 \
                       --node-type=t3.medium \
                       --nodes=3 \
                       --nodes-min=2 \
                       --nodes-max=4 \
                       --node-volume-size=20 \
                       --ssh-access \
                       --ssh-public-key=DevOps \
                       --managed \
                       --asg-access \
                       --external-dns-access \
                       --full-ecr-access \
                       --appmesh-access \
                       --alb-ingress-access
```

---

## Kubernetes RBAC Setup for Jenkins

### Create Namespace

```bash
kubectl create namespace webapps
```

### Create Service Account

```yaml
apiVersion: v1
kind: ServiceAccount
metadata:
  name: jenkins
  namespace: webapps
```

### Create Role

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: Role
metadata:
  name: app-role
  namespace: webapps
rules:
  - apiGroups:
        - ""
        - apps
        - autoscaling
        - batch
        - extensions
        - policy
        - rbac.authorization.k8s.io
    resources:
      - pods
      - componentstatuses
      - configmaps
      - daemonsets
      - deployments
      - events
      - endpoints
      - horizontalpodautoscalers
      - ingress
      - jobs
      - limitranges
      - namespaces
      - nodes
      - pods
      - persistentvolumes
      - persistentvolumeclaims
      - resourcequotas
      - replicasets
      - replicationcontrollers
      - serviceaccounts
      - services
    verbs: ["get", "list", "watch", "create", "update", "patch", "delete"]
```

### Bind Role to Service Account

```yaml
apiVersion: rbac.authorization.k8s.io/v1
kind: RoleBinding
metadata:
  name: app-rolebinding
  namespace: webapps
roleRef:
  apiGroup: rbac.authorization.k8s.io
  kind: Role
  name: app-role
subjects:
- namespace: webapps
  kind: ServiceAccount
  name: jenkins
```

### Generate Token for Jenkins

Generate a non-expiring token for the Jenkins service account and add it to Jenkins credentials as `k8-token`.

---

## Jenkins Configuration

### Required Credentials
| Credential ID | Type | Usage |
|---|---|---|
| `docker-cred` | Username/Password | Docker Hub login |
| `k8-token` | Secret text | Kubernetes service account token |

### Required Jenkins Plugins
- Docker Pipeline
- Kubernetes CLI Plugin
- AWS Steps Plugin (for adservice S3 upload)

---

## Deployment

Once all service images are built and pushed via their individual Jenkins pipelines, trigger the `main` branch pipeline to deploy everything to EKS:

```bash
kubectl apply -f deployment-service.yml
kubectl get svc -n webapps
```

The `frontend-external` service is exposed as a `LoadBalancer`. Access the application using the external IP:

```
http://<EXTERNAL-IP>:80
```

---

## Service Communication Map

```
frontend
  ├── adservice
  ├── cartservice ──── redis-cart
  ├── checkoutservice
  │     ├── cartservice
  │     ├── currencyservice
  │     ├── emailservice
  │     ├── paymentservice
  │     ├── productcatalogservice
  │     └── shippingservice
  ├── currencyservice
  ├── productcatalogservice
  ├── recommendationservice ── productcatalogservice
  └── shippingservice
```
