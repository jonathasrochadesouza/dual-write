# lab-infrastructure Specification

# Spec Delta: lab-infrastructure

## Purpose

Fornece o ambiente Docker Compose completo e reproduzível do laboratório — Postgres, Kafka (KRaft), backend Java 21, frontend Angular, Prometheus e Grafana — com versões compatíveis entre si e healthchecks.

## Requirements

### Requirement: Ambiente completo por Docker Compose
O ambiente SHALL ser iniciado por um único comando de Docker Compose e SHALL conter os serviços: PostgreSQL, Apache Kafka, backend Spring Boot (Java 21 LTS), frontend Angular, Prometheus e Grafana.

#### Scenario: Subida do ambiente
- **WHEN** o comando de subida do Docker Compose é executado a partir do repositório
- **THEN** todos os seis serviços sobem e o laboratório fica utilizável de ponta a ponta

### Requirement: Versões compatíveis com Java 21 LTS
O backend SHALL usar Java 21 LTS e uma versão de Spring Boot compatível com Java 21; o Kafka SHALL usar uma versão estável/LTS em modo KRaft (sem ZooKeeper); o PostgreSQL SHALL usar a versão estável mais recente compatível com o ecossistema.

#### Scenario: Verificação de compatibilidade de versões
- **WHEN** o ambiente sobe
- **THEN** o backend executa sobre Java 21, o Kafka opera em modo KRaft sem ZooKeeper e o PostgreSQL atende conexões na versão estável escolhida

### Requirement: Healthchecks e dependência de inicialização
Cada serviço SHALL declarar healthcheck no Compose e os serviços dependentes SHALL aguardar a prontidão dos seus provedores (ex.: backend aguarda Postgres e Kafka; Prometheus e Grafana aguardam backend).

#### Scenario: Backend aguarda banco e broker
- **WHEN** o ambiente sobe com o Kafka ainda não pronto
- **THEN** o backend só inicia a aplicação após os healthchecks de Postgres e Kafka serem aprovados

### Requirement: Persistência e reprodutibilidade do ambiente
O ambiente SHALL provisionar automaticamente o schema do banco (tabelas de pedidos, outbox e experimentos), o tópico Kafka do laboratório e o provisioning do Grafana (datasource Prometheus e dashboard), de modo que uma subida limpa produza um laboratório completo sem passos manuais.

#### Scenario: Subida limpa provisiona tudo
- **WHEN** o ambiente sobe pela primeira vez com volumes limpos
- **THEN** o schema do banco existe, o tópico Kafka existe e o Grafana já contém datasource e dashboard do laboratório

### Requirement: Portas de acesso padronizadas
O ambiente SHALL expor os serviços nas portas padronizadas: Postgres 5432, Kafka 9092, backend 8080, frontend 4200, Prometheus 9090 e Grafana 3000.

#### Scenario: Acesso aos serviços pelas portas padrão
- **WHEN** o ambiente está de pé
- **THEN** cada serviço responde na sua porta padronizada no localhost
