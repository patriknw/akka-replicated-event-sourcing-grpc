/*
 * Copyright (C) 2021 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.persistence

import akka.NotUsed
import akka.actor.typed.ActorSystem
import akka.persistence.proto.EventEnvelope
import akka.persistence.proto.EventsByPersistenceIdRequest
import akka.persistence.proto.ReplicatedEventSourcingService
import akka.persistence.query.PersistenceQuery
import akka.persistence.query.scaladsl.EventsByPersistenceIdQuery
import akka.serialization.SerializationExtension
import akka.serialization.Serializers
import akka.stream.scaladsl.Source
import com.google.protobuf.ByteString

class ReplicatedEventSourcingServiceImpl(system: ActorSystem[_])
    extends ReplicatedEventSourcingService {

  private val serialization = SerializationExtension(system)

  // FIXME readJournalPluginId config
  private val readJournal: EventsByPersistenceIdQuery =
    PersistenceQuery(system).readJournalFor("jdbc-read-journal")

  override def eventsByPersistenceId(
      in: EventsByPersistenceIdRequest): Source[EventEnvelope, NotUsed] = {
    readJournal
      .eventsByPersistenceId(in.persistenceId, in.fromSeqNr, Long.MaxValue)
      .map { env =>
        val event = env.event.asInstanceOf[AnyRef]
        val meta = env.eventMetadata.map(_.asInstanceOf[AnyRef])
        val eventSerializer = serialization.findSerializerFor(event)
        val metaSerializer = meta.map(serialization.findSerializerFor)

        EventEnvelope(
          seqNr = env.sequenceNr,
          event = ByteString.copyFrom(eventSerializer.toBinary(event)),
          eventSerId = eventSerializer.identifier,
          eventSerManifest = Serializers.manifestFor(eventSerializer, event),
          metadata = metaSerializer
            .map(ser => ByteString.copyFrom(ser.toBinary(meta.get)))
            .getOrElse(ByteString.EMPTY),
          metadataSerId = metaSerializer.map(_.identifier).getOrElse(0),
          metadataSerManifest = metaSerializer
            .map(ser => Serializers.manifestFor(ser, meta.get))
            .getOrElse(""),
          timestamp = env.timestamp)
      }
  }
}
