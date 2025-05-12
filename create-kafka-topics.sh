#!/bin/bash

# Default values
BOOTSTRAP_SERVER="${1:-localhost:9092}"

# Topics definition: name:partitions:replication
TOPICS=(
  "location-events:3:1"
)

echo "Creating Kafka topics on broker: $BOOTSTRAP_SERVER"
for topic_def in "${TOPICS[@]}"; do
  IFS=":" read -r topic partitions replication <<< "$topic_def"

  echo "Creating topic: $topic (partitions=$partitions, replication=$replication)"
  podman exec kafka kafka-topics \
    --create \
    --if-not-exists \
    --topic "$topic" \
    --partitions "$partitions" \
    --replication-factor "$replication" \
    --bootstrap-server "$BOOTSTRAP_SERVER"
done

echo "Topic creation complete."