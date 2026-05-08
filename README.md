# arc-agent-ad

Agent de provisioning Active Directory pour InstantAccessProvider.

## But

- Consommer les commandes publiées par `arc-publisher` sur `arc.provisioning.*`
- Filtrer strictement sur `targetSystem`
- Exécuter l'opération AD (stub par défaut, extensible)

Ce design permet de lancer **1 pod par cible AD** (`ad://AD01`, `ad://AD02`, etc.).

## Flux

1. `Assignment Service` appelle `arc-publisher` via API `POST /jobs`
2. `arc-publisher` stocke le job puis le publie au bon moment sur Kafka
3. `arc-agent-ad` consomme les topics de provisioning
4. L'agent ignore les messages qui ne correspondent pas a son `targetSystem`

## Configuration

Variables principales:

- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (default `localhost:9092`)
- `ARC_PROVISIONING_TOPICS` (default `arc.provisioning`)
- `TARGET_SYSTEM` (ex: `ad://AD01`)
- `ARC_AGENT_CONSUMER_GROUP` (ex: `arc-agent-ad-ad01`)
- `ARC_AGENT_DRY_RUN` (`true`/`false`)

## Lancement local

```bash
mvn spring-boot:run
```

## Build

```bash
mvn clean package
```

## Deploiement Kubernetes (1 pod par target)

Le chart Helm `k8s/` supporte une liste `targets`.
Chaque entree genere un Deployment dedie avec son `TARGET_SYSTEM` et son `consumerGroup`.
