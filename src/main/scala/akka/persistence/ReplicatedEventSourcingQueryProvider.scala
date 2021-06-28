/*
 * Copyright (C) 2021 Lightbend Inc. <https://www.lightbend.com>
 */
package akka.persistence

import akka.actor.ExtendedActorSystem
import akka.persistence.query.ReadJournalProvider
import akka.persistence.query.javadsl
import akka.persistence.query.scaladsl

class ReplicatedEventSourcingQueryProvider(system: ExtendedActorSystem)
    extends ReadJournalProvider {
  override def scaladslReadJournal(): scaladsl.ReadJournal =
    new ReplicatedEventSourcingQuery(system)

  override def javadslReadJournal(): javadsl.ReadJournal = null // FIXME
}
