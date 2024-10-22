package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.data.innlop.SoknadIdent
import no.nav.dagpenger.data.innlop.asUUID
import no.nav.dagpenger.data.innlop.kafka.DataTopic
import no.nav.dagpenger.data.innlop.person.PersonRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SøknadIdentRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<SoknadIdent>,
    private val personRepository: PersonRepository,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
                validate { it.demandValue("gjeldendeTilstand", "opprettet") }
                validate { it.requireKey("søknad_uuid", "ident") }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadIdentRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadId = packet["søknad_uuid"].asUUID()
        val ident = packet["ident"].asText()

        withLoggingContext(
            "søknadId" to søknadId.toString(),
        ) {
            logger.info { "Sjekker om vi skal publisere SøknadIdent" }
            val person = personRepository.hentPerson(ident)

            if (person.harAdressebeskyttelse) return

            SoknadIdent
                .newBuilder()
                .apply {
                    this.soknadId = søknadId
                    this.ident = ident
                }.build()
                .also { data ->
                    logger.info { "Sender ut SøknadIdent" }
                    sikkerlogg.info { "Sender ut SøknadIdent: $data" }
                    dataTopic.publiser(data)
                }
        }
    }
}
