# AEGIS-DEV: A Proactive Security Enclave for Agentic AI Workflows 🛡️

A distributed, zero-trust security architecture designed to intercept prompt injections, prevent hallucinated payloads, and safely execute Agentic AI workflows inside air-gapped container sandboxes.

## Overview
As Large Language Models (LLMs) transition into autonomous "Agentic" systems capable of executing code, host infrastructure becomes highly vulnerable to Remote Code Execution (RCE) and data exfiltration. AEGIS-DEV solves this by decoupling the AI intelligence layer from the code execution layer via an asynchronous message broker, ensuring total host system isolation.

## System Architecture

The architecture operates on a strict "Assume Breach" philosophy across two distinct nodes:

### Tier 1: The AI Security Gateway (Ingress & Analysis)
* **Cryptographic Auth:** Validates incoming payloads via HMAC-SHA256 signatures.
* **Semantic Threat Filter:** Utilizes a locally hosted ONNX DistilBERT model to calculate mathematical intent probabilities, rejecting payloads with a malicious confidence score $\ge 0.7f$.
* **LLM Synthesis:** Securely interfaces with the external Groq LPU API (`llama-3.3-70b-versatile`) to generate deterministic Python code.
* **Generative Output Scan:** Performs a secondary ONNX scan on the LLM's raw output to catch hallucinated exploit payloads before execution.

### Tier 2: The Execution Sandbox (Detonation & Teardown)
* **Asynchronous Spooling:** Retrieves verified tasks from a Redis Message Queue (FIFO).
* **Zero-Trust Docker Enclave:** Wraps the untrusted code in an ephemeral `python:3.10-alpine` container.
* **Structural Containment:** Enforces strict execution parameters including a 128MB memory ceiling and absolute network isolation (`network_mode="none"`).
* **Unconditional Teardown:** Forcibly kills and purges the container and local scratch files after a hard 5.0-second timeout, regardless of execution success or failure.

---

## Technology Stack

| Component | Framework / Technology | Role |
| :--- | :--- | :--- |
| **Gateway Orchestrator** | Java 25 LTS, Spring Boot | REST API ingress and pipeline management |
| **Local AI Inference** | ONNX Runtime (Java), DistilBERT | Dual-pass semantic intent classification |
| **External LLM** | Groq LPU API | High-speed code synthesis |
| **Message Broker** | Redis 7.x | Asynchronous task decoupling |
| **Execution Agent** | Python 3.10, Docker SDK | Container lifecycle orchestration |
| **Sandbox Environment** | Docker Desktop | Ephemeral Alpine execution boundaries |

---

## Project Structure

```text
aegis-dev/
├── src/                      # Tier-1: Java Spring Boot Gateway
├── executor-sandbox/         # Tier-2: Python Docker Orchestration Agent
├── pom.xml                   # Maven build configuration for Tier-1
└── README.md                 # System documentation

---

## Local Deployment Instructions

### Prerequisites
Ensure the following are installed and running on your system:
* **JDK 25 LTS & Maven**
* **Python 3.10+**
* **Docker Desktop** (Must be actively running)
* **Redis Server** (Must be actively running on port 6379)

---

### Node 1: The AI Security Gateway (Tier 1)
This node handles cryptographic ingress, semantic intent scanning, and LLM code synthesis. Open your terminal, navigate to the root directory, and set your environment variables:

```bash
# Export your keys (Linux/Mac) or use 'set' (Windows CMD)
export GROQ_API_KEY="your_api_key_here"
export REDIS_PASSWORD="your_redis_password_here"

# Boot the Java API Gateway
mvn spring-boot:run

### Node 2: The Execution Agent (Tier 2 Sandbox)
This node must run independently (ideally on a separate machine or VM) to ensure host isolation. Open a separate terminal window and navigate into the Tier-2 sandbox directory:

cd executor-sandbox

# Create and activate a Python virtual environment
python -m venv venv

# Activate it (Windows)
venv\Scripts\activate
# OR Activate it (Mac/Linux)
source venv/bin/activate

# Install dependencies and ignite the orchestration agent
pip install -r requirements.txt
python agent.py

### Node 3: The Client Interface (Agentic UI)
With the Gateway, Executor, and Redis broker online, initialize your client node to transmit payloads.

Open src/main/resources/static/index.html in your web browser.

Input your Agentic AI prompts to watch the dual-layer pipeline intercept or execute the payload in real-time

---

Author
Chetana Srinivasa Murthy
B.E. Computer Science and Engineering, Sai Vidya Institute of Technology
