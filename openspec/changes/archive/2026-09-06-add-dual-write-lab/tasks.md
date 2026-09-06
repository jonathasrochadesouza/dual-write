# Tasks: add-dual-write-lab

## 1. Infraestrutura base (compose + serviços)

- [x] 1.1 Criar estrutura `main/` (`docker-compose.yml`, `main/infra/{postgres,kafka,prometheus,grafana}/`, `main/src/{back,front}/`) e verificar diretórios vazios presentes
- [x] 1.2 Escrever `main/infra/postgres/init.sql` com tabelas `orders`, `outbox_events` (payload JSONB) e `experiments`, e verificar que uma subida limpa do Postgres cria o schema (`psql \dt`)
- [x] 1.3 Configurar serviço Kafka no compose (imagem `apache/kafka` estável, modo KRaft single-node, sem ZooKeeper, tópico `orders.order-created` criado de forma idempotente) e verificar com `kafka-topics --describe`
- [x] 1.4 Adicionar serviços backend (multi-stage Maven/Java 21), Prometheus e Grafana ao compose com healthchecks e `depends_on` com `condition: service_healthy`, e verificar `docker compose up` sobe tudo sem erro

## 2. Backend — domínio, ports e fault injection

- [x] 2.1 Scaffold Spring Boot 3.x compatível com Java 21 em `main/src/back/` (Maven, actuator, web, validation) e verificar `mvn verify` passa e `/actuator/health` responde
- [x] 2.2 Implementar domínio `Order` + `OrderRepository` e `OutboxEvent` + `OutboxRepository` como ports com adapters de persistência, e verificar testes de repositório gravam/lêem ambos
- [x] 2.3 Implementar `EventPublisher` (port) + `KafkaEventPublisher` (adapter) publicando evento com `experimentId` no header/payload, e verificar teste de integração publica e relê a mensagem do tópico
- [x] 2.4 Implementar `FaultInjector` com pontos nomeados de outage simulada (`FAIL_BEFORE_COMMIT`, `FAIL_PUBLISH_AFTER_COMMIT`, `FAIL_DB_AFTER_PUBLISH`) e verificar testes unitários cobrem cada ponto lançando a exceção correta

## 3. Backend — cenários dual-write (C1–C4)

- [x] 3.1 Implementar `ScenarioPort` + `ScenarioId` (enum dos 6 cenários) + controller com endpoint único parametrizado (validação rejeita cenário inválido sem escritas), e verificar teste de API: cenário inválido retorna erro de validação
- [x] 3.2 Implementar C1 `DUAL_WRITE_BOTH_OK` (save → send, recursos independentes) e verificar teste de integração: pedido no banco E mensagem no tópico
- [x] 3.3 Implementar C2 `DUAL_WRITE_NONE` (fault antes do commit → rollback total) e verificar teste de integração: nenhuma linha em `orders` e nenhuma mensagem no tópico
- [x] 3.4 Implementar C3 `DUAL_WRITE_DB_ONLY` (commit do banco → fault no publish) e verificar teste de integração: pedido no banco e tópico vazio para o experimento
- [x] 3.5 Implementar C4 `DUAL_WRITE_KAFKA_ONLY` (send → fault na escrita do banco) e verificar teste de integração: mensagem no tópico e nenhuma linha em `orders`

## 4. Backend — cenários outbox (C5–C6)

- [x] 4.1 Implementar C5 `OUTBOX_COMMIT` (`@Transactional`: INSERT order + INSERT outbox_event, mesmo commit) e verificar teste de integração: linha em `orders` E linha PENDING em `outbox_events`, tópico vazio (sem relay)
- [x] 4.2 Implementar C6 `OUTBOX_ROLLBACK` (fault após ambos os INSERTs, antes do commit) e verificar teste de integração: zero linhas nas duas tabelas
- [x] 4.3 Gravar registro em `experiments` (id, cenário, timestamp) fora da transação do cenário, e verificar que o registro existe mesmo nos cenários com rollback

## 5. Backend — relatório e veredito

- [x] 5.1 Implementar inspeção de estado (orders/outbox por experimento + leitura do tópico desde o início filtrando por `experimentId`) e verificar teste: retorna contagens corretas após C1 e C4
- [x] 5.2 Implementar `ExpectedOutcome` declarativo por cenário + derivação de veredito (`ATOMICO` / `INCONSISTENTE` / `OPERACAO_PERDIDA`) e verificar testes unitários: C3→INCONSISTENTE, C2→OPERACAO_PERDIDA, C5→ATOMICO (Kafka vazio marcado como esperado)
- [x] 5.3 Implementar endpoints de relatório por experimento e histórico de experimentos, e verificar teste de API: relatório completo (previsto/observado/veredito) e 404 para experimento inexistente

## 6. Observabilidade

- [x] 6.1 Adicionar `micrometer-registry-prometheus`, expor `/actuator/prometheus` e instrumentar contadores `dualwrite.*` com tag `scenario` (escritas, rollbacks, publishes, falhas de publish, outbox commitado, experimentos por veredito), e verificar com `curl /actuator/prometheus` após rodar cada cenário
- [x] 6.2 Configurar Prometheus (`main/infra/prometheus/prometheus.yml`, scrape do backend) e verificar targets UP no Prometheus
- [x] 6.3 Provisionar Grafana como código (datasource Prometheus + dashboard "Dual-Write Lab" com painel de lacuna de consistência banco×Kafka por cenário, contadores por veredito e latência de transações), e verificar: após `docker compose up -v`, o dashboard existe sem importação manual
- [x] 6.4 Verificação de lacuna: rodar C3 pela API e confirmar no painel a divergência entre séries `db.writes` e `kafka.publishes` daquele cenário

## 7. Frontend Angular

- [x] 7.1 Scaffold Angular (standalone components, signals) em `main/src/front/` com `HttpClient` e modelo de tipos (cenários, experimento, relatório/veredito), e verificar `ng build` passa
- [x] 7.2 Implementar tela de seleção/execução: grid de 6 cards agrupados por padrão (dual-write vs outbox), execução via endpoint único com botão desabilitado sem seleção, e verificar manualmente: seleção + execução dispara a chamada correta
- [x] 7.3 Implementar painel de resultado previsto vs observado com destaque visual por veredito (`ATOMICO` sucesso / `INCONSISTENTE` erro apontando o recurso divergente / `OPERACAO_PERDIDA` aviso) e visão de histórico, e verificar manualmente com C3, C2 e C5
- [x] 7.4 Dockerfile multi-stage (node build → nginx) com `proxy_pass /api → backend:8080` (sem CORS) + links para Grafana/Prometheus, e verificar: ambiente completo de pé, execução de experimento de ponta a ponta pelo navegador

## 8. Verificação ponta a ponta

- [x] 8.1 Rodar os 6 cenários contra o ambiente completo (compose de pé) e verificar que cada relatório devolve o veredito previsto na spec `experiment-scenarios`
- [x] 8.2 Rodar `mvn verify` no backend e `ng build` (ou `ng test`) no frontend e verificar tudo verde; finalizar com `docker compose down -v && docker compose up` provando reprodutibilidade total sem passos manuais
