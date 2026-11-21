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

Le chart Helm `arc-agent-ad` (dépôt [arc-kubeorchestrator](https://github.com/arckit-iam/arc-kubeorchestrator)) supporte une liste `targets`.
Chaque entree genere un Deployment dedie avec son `TARGET_SYSTEM` et son `consumerGroup`.

## Documentation

La documentation d'architecture de la plateforme est dans le dépôt [arc-docs](https://github.com/arckit-iam/arc-docs) :

- [Architecture globale](https://github.com/arckit-iam/arc-docs/blob/main/docs/02-architecture-globale.md).
- [Collecte Active Directory](https://github.com/arckit-iam/arc-docs/blob/main/docs/03-collecte-ad.md).

Le déploiement (Helm, Flux, CI/CD) est documenté dans [arc-kubeorchestrator](https://github.com/arckit-iam/arc-kubeorchestrator/tree/main/docs).

## Licence

Copyright © 2026 Dédale Solutions

Licensed under the EUPL

Ce projet est distribué sous la licence [EUPL-1.2](LICENSE) (European Union Public Licence v. 1.2).
