package com.atendeja.service;

import com.atendeja.enums.StatusNotificacao;
import com.atendeja.enums.TipoNotificacao;
import com.atendeja.model.Notificacao;
import com.atendeja.model.Senha;
import com.atendeja.repository.NotificacaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.MessageAttributeValue;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

import java.util.List;
import java.util.Map;

@Service
public class NotificacaoService {

    @Autowired
    private NotificacaoRepository notificacaoRepository;

    @Value("${aws.access-key-id}")
    private String accessKeyId;

    @Value("${aws.secret-access-key}")
    private String secretAccessKey;

    @Value("${aws.region}")
    private String region;

    public void enviarSms(Senha senha, String mensagem) {
        if (senha.getCelular() == null || senha.getCelular().isBlank()) {
            return;
        }

        String numero;
        try {
            numero = normalizarNumeroBrasil(senha.getCelular());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return;
        }

        // Verifica se já enviou essa mesma mensagem para essa senha
        List<Notificacao> jaEnviadas = notificacaoRepository.findBySenhaId(senha.getId());
        boolean jaEnviou = jaEnviadas.stream()
                .anyMatch(n -> n.getMensagem().equals(mensagem)
                        && n.getStatus() != StatusNotificacao.FALHOU);
        if (jaEnviou) return;

        // Cria registro no banco
        Notificacao notificacao = new Notificacao();
        notificacao.setSenha(senha);
        notificacao.setTipo(TipoNotificacao.SMS);
        notificacao.setDestino(numero);
        notificacao.setMensagem(mensagem);
        notificacao.setStatus(StatusNotificacao.PENDENTE);
        notificacaoRepository.save(notificacao);

        try {
            validarConfiguracaoAws();
            PublishResponse response = publicarSms(numero, mensagem);

            notificacao.setStatus(StatusNotificacao.ENVIADO);
            notificacaoRepository.save(notificacao);

            System.out.println("SMS enviado para " + numero + " | MessageId: " + response.messageId());

        } catch (Exception e) {
            notificacao.setStatus(StatusNotificacao.FALHOU);
            notificacaoRepository.save(notificacao);
            System.err.println("Erro ao enviar SMS para " + numero + ": " + e.getMessage());
        }
    }

    String normalizarNumeroBrasil(String celular) {
        String digitos = celular.replaceAll("[^0-9]", "");

        if (digitos.startsWith("55") && (digitos.length() == 12 || digitos.length() == 13)) {
            return "+" + digitos;
        }

        if (digitos.length() == 10 || digitos.length() == 11) {
            return "+55" + digitos;
        }

        throw new IllegalArgumentException("Celular invalido para envio de SMS: " + celular);
    }

    private void validarConfiguracaoAws() {
        if (accessKeyId == null || accessKeyId.isBlank()
                || secretAccessKey == null || secretAccessKey.isBlank()
                || region == null || region.isBlank()) {
            throw new IllegalStateException("Credenciais/regiao AWS SNS nao configuradas.");
        }
    }

    PublishResponse publicarSms(String numero, String mensagem) {
        try (SnsClient snsClient = SnsClient.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKeyId, secretAccessKey)))
                .build()) {

            PublishRequest request = PublishRequest.builder()
                    .phoneNumber(numero)
                    .message(mensagem)
                    .messageAttributes(Map.of(
                            "AWS.SNS.SMS.SMSType",
                            MessageAttributeValue.builder()
                                    .dataType("String")
                                    .stringValue("Transactional")
                                    .build()))
                    .build();

            return snsClient.publish(request);
        }
    }

    // Notifica quando faltam 3 senhas
    public void notificarProximidade(Senha senha, int posicaoNaFila) {
        if (posicaoNaFila <= 3 && posicaoNaFila > 0) {
            String mensagem = "AtendeJa: Sua senha " + senha.getNumero()
                    + " esta quase sendo chamada na " + senha.getUnidade().getNome()
                    + ". Faltam " + posicaoNaFila + " senhas. Dirija-se ao local.";
            enviarSms(senha, mensagem);
        }
    }

    // Notifica quando a senha é chamada
    public void notificarChamada(Senha senha, String guicheNumero) {
        String mensagem = "AtendeJa: Sua senha " + senha.getNumero()
                + " FOI CHAMADA! Dirija-se ao Guiche " + guicheNumero
                + " na " + senha.getUnidade().getNome() + ".";
        enviarSms(senha, mensagem);
    }
}
