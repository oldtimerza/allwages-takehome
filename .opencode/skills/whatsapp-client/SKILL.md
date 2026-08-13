---
name: whatsapp-client
description: Implement or change AllWage client-layer messaging using the supplied WhatsApp client stub. Use when adding external messaging calls, client ports, or client adapters.
---

# WhatsApp Client

For this assessment, the only external client required is WhatsApp messaging.

- Use `client.InstantMessagingClient` for application messaging dependencies.
- Use the supplied `client.WhatsAppClientStub` implementation, which logs messages instead of sending them.
- Do not integrate a real WhatsApp provider or add other external messaging clients or infrastructure.
- Keep external service abstractions and adapters in the `client` package.
- Services may depend on the client abstraction; models, repositories, and store code must not depend on client code.
- Treat `sendMessage` as an external side effect: callers must handle its boolean result appropriately for the use case.
- Document any production delivery, retry, timeout, or provider-failure behavior that the stub cannot represent when it affects a feature's design.
