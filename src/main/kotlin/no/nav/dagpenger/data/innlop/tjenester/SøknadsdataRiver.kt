package no.nav.dagpenger.data.innlop.tjenester

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.data.innlop.DataTopic
import no.nav.dagpenger.data.innlop.SøknadFaktum
import no.nav.dagpenger.data.innlop.asUUID
import no.nav.dagpenger.data.innlop.søknad.QuizSøknadData
import no.nav.dagpenger.data.innlop.søknad.SøknadRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SøknadsdataRiver(
    rapidsConnection: RapidsConnection,
    private val ferdigeSøknader: SøknadRepository
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søker_oppgave") }
            validate { it.demandValue("ferdig", true) }
            validate { it.demandValue("versjon_navn", "Dagpenger") }
            validate { it.requireKey("søknad_uuid", "seksjoner") }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet["søknad_uuid"].asUUID()

        ferdigeSøknader.lagre(søknadId, QuizSøknadData(packet["seksjoner"])).also {
            logger.info { "Mellomlagrer data for søknadId=$søknadId" }
        }
    }
}

internal class SøknadInnsendtRiver(
    rapidsConnection: RapidsConnection,
    private val ferdigeSøknader: SøknadRepository,
    private val dataTopic: DataTopic<SøknadFaktum>
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "søknad_endret_tilstand") }
            validate { it.demandValue("gjeldendeTilstand", "Innsendt") }
            validate { it.requireKey("søknad_uuid") }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknadId = packet["søknad_uuid"].asUUID()

        withLoggingContext("søknadId" to søknadId.toString()) {
            ferdigeSøknader.hent(søknadId)?.let { data ->
                logger.info { "Fant data for innsendt søknad" }
                data.fakta.onEach { faktum ->
                    SøknadFaktum.newBuilder().apply {
                        this.søknadId = søknadId
                        beskrivelse = faktum.beskrivendeId
                        type = faktum.type
                        svar = faktum.svar
                        gruppe = faktum.gruppe
                        gruppeId = faktum.gruppeId
                    }.build().also { data ->
                        logger.info { "Sender ut $data" }
                        dataTopic.publiser(data)
                    }
                }.also {
                    logger.info { "Produserte ${it.size} faktumrader" }
                }

                ferdigeSøknader.slett(søknadId)
            } ?: logger.warn { "Manglet søknadsdata for innsendt søknad" }
        }
    }
}
