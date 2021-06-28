/*
 * Copyright (C) 2021 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.persistence

import akka.NotUsed
import akka.actor.ActorSystem
import akka.grpc.GrpcClientSettings
import akka.persistence.proto.EventsByPersistenceIdRequest
import akka.persistence.proto.ReplicatedEventSourcingServiceClient
import akka.persistence.query.EventEnvelope
import akka.persistence.query.Offset
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.serialization.SerializationExtension
import akka.stream.scaladsl.Source

class ReplicatedEventSourcingQuery(system: ActorSystem)
    extends EventsByPersistenceIdQuery {

  private val serialization = SerializationExtension(system)

  private val replicatedEventSourcingService = {
    val clientSettings =
      GrpcClientSettings
        .connectToServiceAt(
          system.settings.config.getString(
            "replicated-event-sourcing-service.host"),
          system.settings.config.getInt(
            "replicated-event-sourcing-service.port"))(system)
        .withTls(false)

    ReplicatedEventSourcingServiceClient(clientSettings)(system)
  }

  override def eventsByPersistenceId(
      persistenceId: String,
      fromSequenceNr: Long,
      toSequenceNr: Long): Source[EventEnvelope, NotUsed] = {
    replicatedEventSourcingService
      .eventsByPersistenceId(
        EventsByPersistenceIdRequest(persistenceId, fromSequenceNr))
      .map { env =>
        val event = serialization
          .deserialize(
            env.event.toByteArray,
            env.eventSerId,
            env.eventSerManifest)
          .get
        val metadata =
          if (env.metadata.isEmpty)
            None
          else
            Option(
              serialization
                .deserialize(
                  env.metadata.toByteArray,
                  env.metadataSerId,
                  env.metadataSerManifest)
                .get)

        EventEnvelope(
          Offset.sequence(env.seqNr),
          persistenceId,
          env.seqNr,
          event,
          env.timestamp,
          metadata)
      }
  }
}
