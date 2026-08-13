---
name: document-store-repository
description: Implement or change AllWage repository-layer persistence using the provided in-memory DocumentStore. Use when adding repositories, document-store adapters, or persistence access.
---

# Document Store Repository

For this assessment, all application persistence uses the provided in-memory, document-oriented `repository.store.DocumentStore`.

- Do not introduce a relational database, ORM, external database client, or alternative persistence implementation.
- Model aggregate data as documents around the access patterns required by the feature.
- Keep repository ports in `repository.<aggregate>` and name them `*Repository`.
- Implement ports with adapters named `*DocumentStoreRepository` that use `DocumentStore`.
- Keep repository code dependent only on `repository.store` and `model` packages.
- The store is process-local and loses all data on restart; do not assume durable storage.
- Tests using additional collections must clear the collections they create so shared in-memory state does not leak between tests.
