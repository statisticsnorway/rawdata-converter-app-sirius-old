package no.ssb.rawdata.converter.app.sirius;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SiriusHendelseTest {

    @Test
    public void parseXml() {
        String xml = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><hendelse><sekvensnummer>1</sekvensnummer><identifikator>12345678901</identifikator><gjelderPeriode>2018</gjelderPeriode></hendelse>";
        SiriusHendelse hendelse = SiriusHendelse.parseXml(xml);
        assertThat(hendelse.getSekvensnummer()).isEqualTo("1");
        assertThat(hendelse.getIdentifikator()).isEqualTo("12345678901");
        assertThat(hendelse.getGjelderPeriode()).isEqualTo("2018");
    }
}