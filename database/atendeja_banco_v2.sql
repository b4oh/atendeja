-- ============================================================
--  AtendeJá — Script de Criação do Banco de Dados
--  Banco: PostgreSQL 17
--  Versão: 2.0 | Maio de 2026
-- ============================================================

-- ------------------------------------------------------------
-- 0. Criação do banco (executar separadamente se necessário)
-- ------------------------------------------------------------
-- CREATE DATABASE atendeja
--     WITH ENCODING 'UTF8'
--     LC_COLLATE = 'pt_BR.UTF-8'
--     LC_CTYPE   = 'pt_BR.UTF-8';

-- \c atendeja

-- ============================================================
-- 1. UNIDADE_SAUDE
--    Representa cada unidade do SUS (UBS, UPA, Hospital)
-- ============================================================
CREATE TABLE unidade_saude (
    id            BIGSERIAL     PRIMARY KEY,
    nome          VARCHAR(150)  NOT NULL,
    cnes          VARCHAR(7)    NOT NULL UNIQUE,
    tipo          VARCHAR(30)   NOT NULL
                      CHECK (tipo IN ('UBS','UPA','HOSPITAL','CLINICA')),
    endereco      VARCHAR(255)  NOT NULL,
    telefone      VARCHAR(20),
    horario       VARCHAR(100),
    ativa         BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  unidade_saude      IS 'Unidades de saúde do SUS cadastradas no sistema';
COMMENT ON COLUMN unidade_saude.cnes IS 'Código único nacional CNES de 7 dígitos';
COMMENT ON COLUMN unidade_saude.tipo IS 'Categoria da unidade: UBS, UPA, HOSPITAL ou CLINICA';

-- ============================================================
-- 2. USUARIO
--    Perfis: ADMIN, GESTOR, ATENDENTE, CIDADAO
-- ============================================================
CREATE TABLE usuario (
    id            BIGSERIAL    PRIMARY KEY,
    cpf           VARCHAR(11)  NOT NULL UNIQUE,
    nome          VARCHAR(150) NOT NULL,
    email         VARCHAR(150) NOT NULL UNIQUE,
    senha_hash    VARCHAR(255) NOT NULL,
    perfil        VARCHAR(20)  NOT NULL
                      CHECK (perfil IN ('ADMIN','GESTOR','ATENDENTE','CIDADAO')),
    unidade_id    BIGINT       REFERENCES unidade_saude(id) ON DELETE SET NULL,
    ativo         BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em     TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  usuario            IS 'Usuários do sistema com controle de acesso por perfil';
COMMENT ON COLUMN usuario.senha_hash IS 'Hash BCrypt da senha — nunca armazenar texto puro';
COMMENT ON COLUMN usuario.perfil     IS 'ADMIN: acesso total | GESTOR: relatórios | ATENDENTE: guichê | CIDADAO: portal';
COMMENT ON COLUMN usuario.unidade_id IS 'Unidade vinculada ao usuário (null para ADMIN e CIDADAO)';

-- ============================================================
-- 3. LOG_ACESSO
--    Auditoria de logins (RF004)
-- ============================================================
CREATE TABLE log_acesso (
    id          BIGSERIAL    PRIMARY KEY,
    usuario_id  BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    ip          VARCHAR(45),
    sucesso     BOOLEAN      NOT NULL,
    acessado_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE log_acesso IS 'Registro de tentativas de login para auditoria de segurança';

-- ============================================================
-- 4. SERVICO
--    Serviços oferecidos por cada unidade
-- ============================================================
CREATE TABLE servico (
    id                BIGSERIAL    PRIMARY KEY,
    unidade_id        BIGINT       NOT NULL REFERENCES unidade_saude(id) ON DELETE CASCADE,
    nome              VARCHAR(100) NOT NULL,
    prefixo           CHAR(1)      NOT NULL,
    disponivel        BOOLEAN      NOT NULL DEFAULT TRUE,
    capacidade_diaria INT          NOT NULL DEFAULT 50,
    criado_em         TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE (unidade_id, prefixo)
);

COMMENT ON TABLE  servico                   IS 'Serviços disponíveis em cada unidade de saúde';
COMMENT ON COLUMN servico.prefixo           IS 'Letra prefixo da senha — ex: A001 (Clínico), B010 (Vacinação)';
COMMENT ON COLUMN servico.capacidade_diaria IS 'Limite máximo de senhas emitidas por dia para este serviço';

-- ============================================================
-- 5. GUICHE
--    Guichês de atendimento de cada unidade
-- ============================================================
CREATE TABLE guiche (
    id         BIGSERIAL    PRIMARY KEY,
    unidade_id BIGINT       NOT NULL REFERENCES unidade_saude(id) ON DELETE CASCADE,
    usuario_id BIGINT       REFERENCES usuario(id) ON DELETE SET NULL,
    numero     VARCHAR(10)  NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'FECHADO'
                   CHECK (status IN ('DISPONIVEL','EM_ATENDIMENTO','PAUSADO','FECHADO')),
    UNIQUE (unidade_id, numero)
);

COMMENT ON TABLE  guiche            IS 'Guichês de atendimento físico de cada unidade';
COMMENT ON COLUMN guiche.usuario_id IS 'Atendente atualmente operando o guichê';
COMMENT ON COLUMN guiche.status     IS 'DISPONIVEL | EM_ATENDIMENTO | PAUSADO | FECHADO';

-- ============================================================
-- 6. SENHA
--    Senha de atendimento emitida pelo cidadão
-- ============================================================
CREATE TABLE senha (
    id            BIGSERIAL    PRIMARY KEY,
    unidade_id    BIGINT       NOT NULL REFERENCES unidade_saude(id) ON DELETE CASCADE,
    servico_id    BIGINT       NOT NULL REFERENCES servico(id) ON DELETE CASCADE,
    numero        VARCHAR(10)  NOT NULL,
    canal_emissao VARCHAR(20)  NOT NULL DEFAULT 'TOTEM'
                      CHECK (canal_emissao IN ('TOTEM','APP','PORTAL_WEB')),
    prioritaria   BOOLEAN      NOT NULL DEFAULT FALSE,
    status        VARCHAR(20)  NOT NULL DEFAULT 'AGUARDANDO'
                      CHECK (status IN ('AGUARDANDO','CHAMADA','EM_ATENDIMENTO','ATENDIDA','NAO_COMPARECEU','CANCELADA')),
    celular       VARCHAR(15),
    emitida_em    TIMESTAMP    NOT NULL DEFAULT NOW(),
    chamada_em    TIMESTAMP
);

CREATE UNIQUE INDEX idx_senha_numero_unico ON senha (unidade_id, numero, CAST(emitida_em AS DATE));

COMMENT ON TABLE  senha             IS 'Senhas de atendimento emitidas pelo cidadão via totem, app ou portal';
COMMENT ON COLUMN senha.numero      IS 'Número formatado: prefixo + sequencial (ex: A042). Reinicia a cada dia';
COMMENT ON COLUMN senha.prioritaria IS 'TRUE para idosos, gestantes, PCD — atendidos antes das senhas normais';
COMMENT ON COLUMN senha.celular     IS 'Número para notificação quando a senha estiver próxima de ser chamada';

-- ============================================================
-- 7. ATENDIMENTO
--    Registro do desfecho de cada senha chamada (RF005)
-- ============================================================
CREATE TABLE atendimento (
    id               BIGSERIAL    PRIMARY KEY,
    senha_id         BIGINT       NOT NULL UNIQUE REFERENCES senha(id) ON DELETE RESTRICT,
    guiche_id        BIGINT       NOT NULL REFERENCES guiche(id) ON DELETE RESTRICT,
    usuario_id       BIGINT       NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    cpf_paciente     VARCHAR(11),
    desfecho         VARCHAR(20)  NOT NULL
                         CHECK (desfecho IN ('ATENDIDO','REDIRECIONADO','NAO_COMPARECEU')),
    observacoes      TEXT,
    tempo_espera_min INT          NOT NULL DEFAULT 0,
    realizado_em     TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  atendimento                  IS 'Registro de cada atendimento realizado no guichê';
COMMENT ON COLUMN atendimento.tempo_espera_min IS 'Calculado automaticamente: diferença entre emitida_em e chamada_em';
COMMENT ON COLUMN atendimento.cpf_paciente     IS 'CPF do paciente — opcional, mascarado na exibição';

-- ============================================================
-- 8. NOTIFICACAO
--    Log de notificações enviadas ao cidadão (RF008)
-- ============================================================
CREATE TABLE notificacao (
    id         BIGSERIAL    PRIMARY KEY,
    senha_id   BIGINT       NOT NULL REFERENCES senha(id) ON DELETE CASCADE,
    tipo       VARCHAR(10)  NOT NULL CHECK (tipo IN ('SMS','PUSH')),
    destino    VARCHAR(20)  NOT NULL,
    mensagem   TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'PENDENTE'
                   CHECK (status IN ('PENDENTE','ENVIADO','FALHOU')),
    enviada_em TIMESTAMP    NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE  notificacao         IS 'Log de todas as notificações disparadas ao cidadão';
COMMENT ON COLUMN notificacao.tipo    IS 'SMS ou PUSH (notificação via aplicativo)';
COMMENT ON COLUMN notificacao.destino IS 'Número de celular (SMS) ou device token (PUSH)';

-- ============================================================
-- 9. TIPO_EXAME
--    Tipos de exames disponíveis por unidade
-- ============================================================
CREATE TABLE tipo_exame (
    id              BIGSERIAL     PRIMARY KEY,
    unidade_id      BIGINT        NOT NULL REFERENCES unidade_saude(id) ON DELETE CASCADE,
    nome            VARCHAR(150)  NOT NULL,
    descricao       VARCHAR(255),
    duracao_minutos INTEGER       NOT NULL DEFAULT 30,
    vagas_por_dia   INTEGER       NOT NULL DEFAULT 20,
    disponivel      BOOLEAN       NOT NULL DEFAULT TRUE,
    criado_em       TIMESTAMP     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE tipo_exame IS 'Tipos de exames disponíveis em cada unidade de saúde';

-- ============================================================
-- 10. AGENDAMENTO
--     Agendamentos de exames pelos cidadãos
-- ============================================================
CREATE TABLE agendamento (
    id            BIGSERIAL     PRIMARY KEY,
    usuario_id    BIGINT        NOT NULL REFERENCES usuario(id) ON DELETE CASCADE,
    tipo_exame_id BIGINT        NOT NULL REFERENCES tipo_exame(id) ON DELETE CASCADE,
    unidade_id    BIGINT        NOT NULL REFERENCES unidade_saude(id) ON DELETE CASCADE,
    data_exame    DATE          NOT NULL,
    hora_exame    TIME          NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'AGENDADO'
                      CHECK (status IN ('AGENDADO','CONFIRMADO','CANCELADO','REALIZADO','NAO_COMPARECEU')),
    observacoes   TEXT,
    criado_em     TIMESTAMP     NOT NULL DEFAULT NOW(),
    atualizado_em TIMESTAMP     NOT NULL DEFAULT NOW()
);

COMMENT ON TABLE agendamento IS 'Agendamentos de exames realizados pelos cidadãos';

-- ============================================================
-- 11. ÍNDICES — Performance para consultas frequentes
-- ============================================================

-- Senhas e fila
CREATE INDEX idx_senha_unidade_status     ON senha (unidade_id, status);
CREATE INDEX idx_senha_servico_status     ON senha (servico_id, status);
CREATE INDEX idx_senha_emitida_em         ON senha (CAST(emitida_em AS DATE));
CREATE INDEX idx_senha_chamada_em         ON senha (chamada_em DESC NULLS LAST);

-- Atendimentos
CREATE INDEX idx_atendimento_guiche       ON atendimento (guiche_id, realizado_em DESC);
CREATE INDEX idx_atendimento_realizado_em ON atendimento (realizado_em DESC);
CREATE INDEX idx_atendimento_usuario      ON atendimento (usuario_id);

-- Notificações
CREATE INDEX idx_notificacao_senha        ON notificacao (senha_id);

-- Auditoria
CREATE INDEX idx_log_acesso_usuario       ON log_acesso (usuario_id, acessado_em DESC);

-- Agendamentos
CREATE INDEX idx_agendamento_data         ON agendamento (data_exame);
CREATE INDEX idx_agendamento_unidade      ON agendamento (unidade_id, data_exame);
CREATE INDEX idx_agendamento_usuario      ON agendamento (usuario_id);
CREATE INDEX idx_agendamento_status       ON agendamento (status);

-- ============================================================
-- 12. GATILHOS (TRIGGERS)
-- ============================================================

-- Função reutilizável: atualiza campo atualizado_em
CREATE OR REPLACE FUNCTION fn_atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Trigger: atualizar unidade_saude
CREATE TRIGGER trg_atualizar_unidade_saude
    BEFORE UPDATE ON unidade_saude
    FOR EACH ROW
    EXECUTE FUNCTION fn_atualizar_timestamp();

-- Trigger: atualizar usuario
CREATE TRIGGER trg_atualizar_usuario
    BEFORE UPDATE ON usuario
    FOR EACH ROW
    EXECUTE FUNCTION fn_atualizar_timestamp();

-- Trigger: atualizar agendamento
CREATE TRIGGER trg_atualizar_agendamento
    BEFORE UPDATE ON agendamento
    FOR EACH ROW
    EXECUTE FUNCTION fn_atualizar_timestamp();

-- Trigger: validar emissão de senha
CREATE OR REPLACE FUNCTION fn_validar_emissao_senha()
RETURNS TRIGGER AS $$
DECLARE
    v_disponivel    BOOLEAN;
    v_capacidade    INTEGER;
    v_emitidas_hoje INTEGER;
BEGIN
    SELECT disponivel, capacidade_diaria
    INTO v_disponivel, v_capacidade
    FROM servico
    WHERE id = NEW.servico_id;

    IF NOT v_disponivel THEN
        RAISE EXCEPTION 'Serviço indisponível para emissão de senhas.';
    END IF;

    SELECT COUNT(*)
    INTO v_emitidas_hoje
    FROM senha
    WHERE servico_id = NEW.servico_id
      AND CAST(emitida_em AS DATE) = CAST(NOW() AS DATE)
      AND status != 'CANCELADA';

    IF v_emitidas_hoje >= v_capacidade THEN
        RAISE EXCEPTION 'Capacidade diária de atendimentos atingida para este serviço.';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validar_emissao_senha
    BEFORE INSERT ON senha
    FOR EACH ROW
    EXECUTE FUNCTION fn_validar_emissao_senha();

-- Trigger: calcular tempo de espera automaticamente
CREATE OR REPLACE FUNCTION fn_calcular_tempo_espera()
RETURNS TRIGGER AS $$
DECLARE
    v_emitida_em TIMESTAMP;
BEGIN
    SELECT emitida_em INTO v_emitida_em
    FROM senha
    WHERE id = NEW.senha_id;

    NEW.tempo_espera_min = EXTRACT(EPOCH FROM (NOW() - v_emitida_em)) / 60;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_calcular_tempo_espera
    BEFORE INSERT ON atendimento
    FOR EACH ROW
    EXECUTE FUNCTION fn_calcular_tempo_espera();

-- ============================================================
-- 13. VIEWS
-- ============================================================

-- Fila atual de senhas aguardando por unidade
CREATE VIEW vw_fila_atual AS
SELECT
    s.id,
    s.numero,
    s.prioritaria,
    s.status,
    s.emitida_em,
    s.celular,
    sv.nome        AS servico,
    u.nome         AS unidade,
    ROW_NUMBER() OVER (
        PARTITION BY s.unidade_id, s.servico_id
        ORDER BY s.prioritaria DESC, s.emitida_em ASC
    )              AS posicao_fila
FROM senha s
JOIN servico sv ON sv.id = s.servico_id
JOIN unidade_saude u ON u.id = s.unidade_id
WHERE s.status = 'AGUARDANDO'
  AND CAST(s.emitida_em AS DATE) = CAST(NOW() AS DATE);

COMMENT ON VIEW vw_fila_atual IS 'Fila atual de senhas aguardando, com posição por unidade e serviço. Prioritárias primeiro.';

-- Resumo de atendimentos do dia por unidade
CREATE VIEW vw_resumo_dia AS
SELECT
    u.id                                        AS unidade_id,
    u.nome                                      AS unidade,
    COUNT(s.id)                                 AS senhas_emitidas,
    COUNT(a.id)                                 AS atendimentos,
    COUNT(CASE WHEN a.desfecho = 'ATENDIDO'        THEN 1 END) AS atendidos,
    COUNT(CASE WHEN a.desfecho = 'NAO_COMPARECEU'  THEN 1 END) AS nao_compareceu,
    COUNT(CASE WHEN a.desfecho = 'REDIRECIONADO'   THEN 1 END) AS redirecionados,
    ROUND(AVG(a.tempo_espera_min)::NUMERIC, 1)  AS tempo_medio_espera_min
FROM unidade_saude u
LEFT JOIN senha s     ON s.unidade_id = u.id
    AND CAST(s.emitida_em AS DATE) = CAST(NOW() AS DATE)
LEFT JOIN atendimento a ON a.senha_id = s.id
GROUP BY u.id, u.nome
ORDER BY u.nome;

COMMENT ON VIEW vw_resumo_dia IS 'Resumo diário de atendimentos por unidade — dashboard do gestor.';

-- Desempenho por guichê no dia
CREATE VIEW vw_desempenho_guiche AS
SELECT
    g.id                                        AS guiche_id,
    g.numero                                    AS guiche,
    u.nome                                      AS unidade,
    us.nome                                     AS atendente,
    COUNT(a.id)                                 AS total_atendimentos,
    ROUND(AVG(a.tempo_espera_min)::NUMERIC, 1)  AS tempo_medio_min
FROM guiche g
JOIN unidade_saude u  ON u.id = g.unidade_id
LEFT JOIN usuario us  ON us.id = g.usuario_id
LEFT JOIN atendimento a ON a.guiche_id = g.id
    AND CAST(a.realizado_em AS DATE) = CAST(NOW() AS DATE)
GROUP BY g.id, g.numero, u.nome, us.nome
ORDER BY total_atendimentos DESC;

COMMENT ON VIEW vw_desempenho_guiche IS 'Ranking de desempenho dos guichês no dia — relatório gerencial.';

-- ============================================================
-- 14. DADOS INICIAIS — Seed para desenvolvimento
-- ============================================================

-- Unidades de saúde de Maceió
INSERT INTO unidade_saude (nome, cnes, tipo, endereco, telefone, horario) VALUES
  ('UBS Tabuleiro do Martins',  '2008901', 'UBS',      'Av. Fernandes Lima, 3274 – Tabuleiro',     '(82) 3315-5001', 'Seg-Sex 07h-17h'),
  ('UBS Benedito Bentes',       '2008902', 'UBS',      'Rua Comendador Leão, 12 – Benedito Bentes','(82) 3315-5002', 'Seg-Sex 07h-17h'),
  ('UPA do Trapiche',           '2008903', 'UPA',      'Av. Álvaro Otacílio, 4650 – Trapiche',     '(82) 3315-5010', 'Seg-Dom 00h-24h'),
  ('Hospital Geral do Estado',  '2008917', 'HOSPITAL', 'Av. Comendador Gustavo Paiva, 2862 – Cruz','(82) 3315-5100', 'Seg-Dom 00h-24h');

-- Serviços da UBS Tabuleiro do Martins (id=1)
INSERT INTO servico (unidade_id, nome, prefixo, capacidade_diaria) VALUES
  (1, 'Clínico Geral', 'A', 40),
  (1, 'Vacinação',     'B', 60),
  (1, 'Curativo',      'C', 30),
  (1, 'Prioritário',   'P', 20);

-- Serviços da UPA do Trapiche (id=3)
INSERT INTO servico (unidade_id, nome, prefixo, capacidade_diaria) VALUES
  (3, 'Emergência',       'E', 999),
  (3, 'Consulta Clínica', 'A', 80),
  (3, 'Coleta de Exames', 'C', 50),
  (3, 'Vacinação',        'B', 40),
  (3, 'Farmácia',         'F', 100),
  (3, 'Prioritário',      'P', 30);

-- Usuários de teste (senha padrão: 123456)
INSERT INTO usuario (cpf, nome, email, senha_hash, perfil, unidade_id) VALUES
  ('00000000000', 'Administrador AtendeJá', 'admin@atendeja.al.gov.br',
   '$2a$10$66EgScBcNVJi9msnKXeLKOrZqW4pU5.XNyNllEdsrwRxCJMVI0A8W', 'ADMIN', NULL),
  ('11111111111', 'Gestor UBS Tabuleiro', 'gestor@atendeja.al.gov.br',
   '$2a$10$66EgScBcNVJi9msnKXeLKOrZqW4pU5.XNyNllEdsrwRxCJMVI0A8W', 'GESTOR', 1),
  ('22222222222', 'Atendente UBS Tabuleiro', 'atendente@atendeja.al.gov.br',
   '$2a$10$66EgScBcNVJi9msnKXeLKOrZqW4pU5.XNyNllEdsrwRxCJMVI0A8W', 'ATENDENTE', 1),
  ('33333333333', 'Cidadão Teste', 'cidadao@atendeja.al.gov.br',
   '$2a$10$66EgScBcNVJi9msnKXeLKOrZqW4pU5.XNyNllEdsrwRxCJMVI0A8W', 'CIDADAO', NULL);

-- Guichês da UBS Tabuleiro do Martins
INSERT INTO guiche (unidade_id, numero) VALUES
  (1, '01'), (1, '02'), (1, '03'), (1, '04');

-- Guichês da UPA do Trapiche
INSERT INTO guiche (unidade_id, numero) VALUES
  (3, '01'), (3, '02'), (3, '03'), (3, '04'),
  (3, '05'), (3, '06'), (3, '07'), (3, '08');

-- Tipos de exames da UBS Tabuleiro do Martins (id=1)
INSERT INTO tipo_exame (unidade_id, nome, descricao, duracao_minutos, vagas_por_dia) VALUES
  (1, 'Hemograma Completo',  'Exame de sangue completo',          15, 30),
  (1, 'Glicemia em Jejum',   'Dosagem de glicose no sangue',      10, 30),
  (1, 'Raio-X Tórax',        'Radiografia do tórax',              20, 15),
  (1, 'Eletrocardiograma',   'ECG de repouso',                    30, 10),
  (1, 'Ultrassom Abdominal', 'Ultrassonografia do abdômen',       40, 8);

-- Tipos de exames da UPA do Trapiche (id=3)
INSERT INTO tipo_exame (unidade_id, nome, descricao, duracao_minutos, vagas_por_dia) VALUES
  (3, 'Hemograma Completo',  'Exame de sangue completo',          15, 40),
  (3, 'Tomografia',          'Tomografia computadorizada',        45, 6),
  (3, 'Raio-X',              'Radiografia geral',                 20, 20),
  (3, 'Teste Rápido COVID',  'Teste antígeno',                    10, 50);

-- ============================================================
-- FIM DO SCRIPT
-- ============================================================
