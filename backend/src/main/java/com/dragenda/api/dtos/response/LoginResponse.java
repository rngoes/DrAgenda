package com.dragenda.api.dtos.response;

public record LoginResponse(
    String token,
    String perfil,
    String nome,
    Long empresaId,
    boolean senhaTemporaria
) {}
