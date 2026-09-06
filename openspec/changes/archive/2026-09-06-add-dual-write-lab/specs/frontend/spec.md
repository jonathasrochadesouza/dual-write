# Spec Delta: frontend

## Purpose

Fornece uma interface Angular com boa UX/UI para operar o laboratório: selecionar um dos seis cenários, executá-lo, visualizar previsto vs observado com veredito e consultar o histórico de experimentos.

## ADDED Requirements

### Requirement: Seleção e execução de cenário
A interface SHALL apresentar os seis cenários como opções selecionáveis, agrupados por padrão (dual-write vs outbox), e permitir executar o cenário selecionado acionando o endpoint único do backend.

#### Scenario: Execução de cenário pela interface
- **WHEN** o usuário seleciona um cenário e aciona a execução
- **THEN** a interface envia a requisição ao backend e apresenta o resultado do experimento

#### Scenario: Cenário inválido impedido na interface
- **WHEN** nenhum cenário está selecionado e o usuário aciona a execução
- **THEN** a interface impede o envio e sinaliza visualmente a necessidade de seleção

### Requirement: Apresentação de hipótese vs observação
A interface SHALL exibir, para cada experimento, o resultado previsto do cenário e o estado observado (banco, outbox, Kafka), lado a lado ou de forma comparativa, junto ao veredito com destaque visual por classificação (`ATOMICO`, `INCONSISTENTE`, `OPERACAO_PERDIDA`).

#### Scenario: Exibição de experimento inconsistente
- **WHEN** um experimento com veredito `INCONSISTENTE` retorna da execução
- **THEN** a interface exibe previsto vs observado com destaque visual de erro apontando o recurso divergente

#### Scenario: Exibição de experimento atômico
- **WHEN** um experimento com veredito `ATOMICO` retorna da execução
- **THEN** a interface exibe previsto vs observado com destaque visual de sucesso

### Requirement: Histórico de experimentos
A interface SHALL exibir o histórico de experimentos executados com identificador, cenário, marcação temporal e veredito.

#### Scenario: Consulta do histórico pela interface
- **WHEN** o usuário acessa a visão de histórico
- **THEN** a interface lista os experimentos previamente executados com suas informações de veredito

### Requirement: Acesso às ferramentas de observabilidade
A interface SHALL oferecer acesso direto (links) ao Grafana e ao Prometheus do ambiente.

#### Scenario: Acesso ao dashboard
- **WHEN** o usuário aciona o link do Grafana
- **THEN** o dashboard do laboratório abre no Grafana

### Requirement: Interface servida pelo ambiente Docker
A interface SHALL ser servida dentro do ambiente Docker Compose, acessível por navegador, e SHALL alcançar o backend sem exigir configuração de CORS pelo usuário.

#### Scenario: Acesso à interface no ambiente subido
- **WHEN** o ambiente Docker Compose sobe completamente e o usuário abre a URL do frontend no navegador
- **THEN** a interface carrega e consegue executar experimentos contra o backend
