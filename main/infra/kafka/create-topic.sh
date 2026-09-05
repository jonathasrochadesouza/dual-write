#!/bin/bash
set -euo pipefail

BOOTSTRAP="${KAFKA_BOOTSTRAP_SERVERS:-kafka:9092}"
TOPIC="${KAFKA_TOPIC:-orders.order-created}"

echo "Waiting for Kafka at ${BOOTSTRAP}..."
for i in $(seq 1 60); do
  if /opt/kafka/bin/kafka-topics.sh --bootstrap-server "${BOOTSTRAP}" --list >/dev/null 2>&1; then
    break
  fi
  sleep 2
done

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "${BOOTSTRAP}" \
  --create \
  --if-not-exists \
  --topic "${TOPIC}" \
  --partitions 1 \
  --replication-factor 1

/opt/kafka/bin/kafka-topics.sh \
  --bootstrap-server "${BOOTSTRAP}" \
  --describe \
  --topic "${TOPIC}"

echo "Topic ${TOPIC} ready."
