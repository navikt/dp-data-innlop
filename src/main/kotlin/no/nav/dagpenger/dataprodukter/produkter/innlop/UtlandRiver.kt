package no.nav.dagpenger.dataprodukter.produkter.innlop

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.innlop.Utland
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.søknad.data.SøknadData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class UtlandRiver(
    rapidsConnection: RapidsConnection,
    private val dataTopic: DataTopic<Utland>,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "innsending_ferdigstilt") }
                validate {
                    it.requireAny(
                        "type",
                        listOf(
                            "NySøknad",
                            "Gjenopptak",
                        ),
                    )
                }
                validate {
                    it.interestedIn(
                        "journalpostId",
                        "søknadsData",
                    )
                }
            }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.UtlandRiver")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val søknadsData = packet["søknadsData"]
        val journalpostId = packet["journalpostId"].asText()
        withLoggingContext(
            "journalpostId" to journalpostId,
        ) {
            if (søknadsData.isEmpty) {
                logger.debug { " Journalpost mangler søknadsdata, hopper over" }
                return
            }
            val søknad = SøknadData.lagMapper(søknadsData)
            try {
                Utland
                    .newBuilder()
                    .apply {
                        this.journalpostId = journalpostId
                        erUtland = søknad.utenlandstilsnitt.erUtland
                        bostedsland = søknad.utenlandstilsnitt.bostedsland
                        arbeidsforholdEos = søknad.utenlandstilsnitt.harArbeidsforholdEØS
                        arbeidsforholdLand = søknad.utenlandstilsnitt.arbeidsland.joinToString(",")
                    }.build()
                    .also { data ->
                        logger.info { "Publiserer rad for ${data::class.java.simpleName}" }
                        sikkerlogg.info { "Publiserer rad for ${data::class.java.simpleName}: $data" }
                        dataTopic.publiser(data)
                    }
            } catch (e: NoSuchElementException) {
                logger.error(e) { "Fant ikke riktig data i søknaden" }
                sikkerlogg.error(e) { "Fant ikke riktig data i søknad=$søknadsData" }
                throw e
            }
        }
    }
}