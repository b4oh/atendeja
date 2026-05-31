package com.atendeja.service;

import com.atendeja.enums.StatusNotificacao;
import com.atendeja.model.Notificacao;
import com.atendeja.model.Senha;
import com.atendeja.repository.NotificacaoRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificacaoServiceTest {

    @Test
    void normalizaNumeroBrasileiroSemDuplicarCodigoDoPais() {
        NotificacaoService service = new NotificacaoService();

        assertThat(service.normalizarNumeroBrasil("(82) 99999-0000"))
                .isEqualTo("+5582999990000");
        assertThat(service.normalizarNumeroBrasil("+55 82 99999-0000"))
                .isEqualTo("+5582999990000");
    }

    @Test
    void enviarSmsRegistraNotificacaoEnviadaQuandoSnsPublicaComSucesso() {
        NotificacaoRepository repository = mock(NotificacaoRepository.class);
        TestavelNotificacaoService service = new TestavelNotificacaoService();
        ReflectionTestUtils.setField(service, "notificacaoRepository", repository);
        ReflectionTestUtils.setField(service, "accessKeyId", "access-key");
        ReflectionTestUtils.setField(service, "secretAccessKey", "secret-key");
        ReflectionTestUtils.setField(service, "region", "us-east-1");

        Senha senha = new Senha();
        senha.setId(10L);
        senha.setCelular("+55 82 99999-0000");
        when(repository.findBySenhaId(10L)).thenReturn(List.of());

        service.enviarSms(senha, "Mensagem de teste");

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository, times(2)).save(captor.capture());

        Notificacao finalizada = captor.getAllValues().get(1);
        assertThat(finalizada.getDestino()).isEqualTo("+5582999990000");
        assertThat(finalizada.getMensagem()).isEqualTo("Mensagem de teste");
        assertThat(finalizada.getStatus()).isEqualTo(StatusNotificacao.ENVIADO);
        assertThat(service.numeroPublicado).isEqualTo("+5582999990000");
        assertThat(service.mensagemPublicada).isEqualTo("Mensagem de teste");
    }

    private static class TestavelNotificacaoService extends NotificacaoService {
        private String numeroPublicado;
        private String mensagemPublicada;

        @Override
        PublishResponse publicarSms(String numero, String mensagem) {
            this.numeroPublicado = numero;
            this.mensagemPublicada = mensagem;
            return PublishResponse.builder().messageId("msg-1").build();
        }
    }
}
