package no.nav.dagpenger.dataprodukter.produkter.innlop

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.dataprodukt.innlop.Utland
import no.nav.dagpenger.dataprodukter.kafka.DataTopic
import no.nav.dagpenger.dataprodukter.søknad.data.SøknadData
import no.nav.dagpenger.dataprodukter.søknad.erEØS
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
                        erUtland = erUtland(søknad)
                        bostedsland = søknad.bostedsland
                        arbeidsforholdEos = søknad.arbeidsforholdLand.any { it.erEØS() }
                        arbeidsforholdLand = søknad.arbeidsforholdLand.joinToString()
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

    private fun erUtland(søknad: SøknadData): Boolean =
        try {
            søknad.bostedsland != "NOR" || søknad.arbeidsforholdLand.any { it != "NOR" }
        } catch (e: Exception) {
            sikkerlogg.error(e) { søknad.data }
            throw e
        }
}
