package fiap.com.br.zelafix.usuario;

/**
 * Papéis de usuário no ZelaFix:
 * - CIDADAO: Munícipe que cria chamados, apoia ocorrências e acompanha resoluções.
 * - GESTOR: Funcionário da Prefeitura / Poder Público responsável por alterar o status dos chamados.
 * - ADMIN: Administrador do sistema com acesso irrestrito ao CRUD completo da plataforma.
 */
public enum CargoUsuario {
    CIDADAO,
    GESTOR,
    ADMIN
}
