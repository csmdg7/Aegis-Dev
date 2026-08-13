import docker
import redis
import json
import os

# Initialize Redis Connection (matches application.properties configuration)
redis_client = redis.Redis(
    host='10.193.173.124',
    port=6379,
    password='YOUR_REDIS_PASSWORD_HERE',
    decode_responses=True
)
docker_client = docker.from_env()

REDIS_CHANNEL = "aegis-tasks"
SCRATCH_DIR = os.path.abspath("./scratch")

def execute_tier2_loop():
    print(f"🛡️ Tier-2 Executor Online. Polling Redis channel: {REDIS_CHANNEL}...")

    # Subscribe to the Redis channel
    pubsub = redis_client.pubsub()
    pubsub.subscribe(REDIS_CHANNEL)

    for message in pubsub.listen():
        if message['type'] == 'message':
            payload = json.loads(message['data'])
            prompt = payload.get('prompt')
            code = payload.get('code')

            print(f"\n[+] Task Received! Executing isolated sandbox...")

            # Step 1: Write code to scratch directory
            script_path = os.path.join(SCRATCH_DIR, "payload.py")
            with open(script_path, "w") as f:
                f.write(code)

            # Step 2: Configure & Launch zero-trust container
            try:
                container = docker_client.containers.run(
                    image="python:3.10-alpine",
                    command="python /tmp/payload.py",
                    volumes={script_path: {'bind': '/tmp/payload.py', 'mode': 'ro'}},
                    network_mode="none",
                    mem_limit="128m",
                    detach=True
                )

                # Step 3: Wait with timeout (T_max = 5.0s)
                result = container.wait(timeout=5)
                out = container.logs(stdout=True, stderr=False).decode('utf-8')
                err = container.logs(stdout=False, stderr=True).decode('utf-8')
                print(f"[SUCCESS] Output:\n{out}")

            except Exception as e:
                # Step 4: Threat Mitigation (Timeout / Resource Flood)
                print(f"[INTERCEPT] Container Force-Killed: {str(e)}")
                try:
                    container.kill()
                except Exception:
                    pass
            finally:
                # Step 5: Unconditional Teardown
                try:
                    container.remove(force=True)
                except Exception:
                    pass
                if os.path.exists(script_path):
                    os.remove(script_path)
                print("[-] Sandbox Teardown Complete.\n")

if __name__ == "__main__":
    execute_tier2_loop()