# Kubernetes Leader Election (Spring Boot)

This project demonstrates a simple and production-friendly **leader election mechanism** using Kubernetes `Lease` objects with Spring Boot and Fabric8.

---

# What is this?

In a distributed system with multiple instances, some tasks must run **only once** (e.g., batch jobs).

Leader election ensures that **only one instance (leader)** executes these tasks.

---

# How it works

* Kubernetes stores leadership state in a `Lease`
* Each instance:

  * Reads the lease
  * If expired → tries to become leader
  * If leader → renews the lease
  * Otherwise → stays follower

---

# Important

Leadership is **not rotated periodically**

Leader changes only when:

* Instance stops
* Lease is not renewed
* Network issues occur

---

# Components

* **Fabric8LeaderElector** → handles leader election logic
* **LeaderOnlyJob** → runs only on leader
* **Lease (Kubernetes)** → shared coordination object

---

# Configuration

### application.yml

```yaml
leader-election:
  namespace: default
  lease-name: order-processor-leader
  renew-interval-seconds: 10
```

### lease.yml

```yaml
apiVersion: coordination.k8s.io/v1
kind: Lease
metadata:
  name: order-processor-leader
  namespace: default
spec:
  leaseDurationSeconds: 30
```

---

# Run

```bash
kubectl apply -f lease.yml
./mvnw spring-boot:run
```

Run multiple instances to observe behavior.

---

# Expected Behavior

* First instance becomes leader
* Others stay followers
* If leader stops → another instance takes over

---

# Key Guarantees

✔ Single active leader
✔ Safe failover
✔ No duplicate job execution
