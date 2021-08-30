# Support Transport Layer Security (TLS)
Transport Layer Security (TLS) is a very common security way when transport data through Internet.
In some use cases, end users report the background:

> Target(under monitoring) applications are in a region, which also named VPC,
at the same time, the SkyWalking backend is in another region (VPC).
> 
> Because of that, security requirement is very obvious.

## Authentication Mode
- Use this [script](../../../../../tools/TLS/tls_key_generate.sh) if you are not familiar with how to generate key files.
- Find `ca.crt`, and use it at client side. In `mTLS` mode, `client.crt` and `client.pem` are required at client side.
- Find `server.crt` ,`server.pem` and `ca.crt`. Use them at server side. Please refer to `gRPC Security` of the OAP server doc.
  for more details.

## Open and config TLS

### Agent config
- Agent enables TLS automatically after the `ca.crt`(by default `/ca` folder in agent package) file is detected.
- TLS with no CA mode could be activated by this setting.
```
agent.force_tls=${SW_AGENT_FORCE_TLS:true}
```

## Enable mutual TLS
- Sharing gRPC server must be started with mTLS enabled. More details can be found in `receiver-sharing-server` section in `application.yaml`. Please refer to `gRPC Security` and `gRPC/HTTP server for receiver`.
- Copy CA certificate, certificate and private key of client into `agent/ca`.
- Configure client-side SSL/TLS in `agent.conf`.
- Change `SW_AGENT_COLLECTOR_BACKEND_SERVICES` targeting to host and port of `receiver-sharing-server`.

For example:
```
agent.force_tls=${SW_AGENT_FORCE_TLS:true}
agent.ssl_trusted_ca_path=${SW_AGENT_SSL_TRUSTED_CA_PATH:/ca/ca.crt}
agent.ssl_key_path=${SW_AGENT_SSL_KEY_PATH:/ca/client.pem}
agent.ssl_cert_chain_path=${SW_AGENT_SSL_CERT_CHAIN_PATH:/ca/client.crt}

collector.backend_service=${SW_AGENT_COLLECTOR_BACKEND_SERVICES:skywalking-oap:11801}
```
