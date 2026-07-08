package com.dragenda.infrastructure.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;

import java.util.Map;

public class SecurityUtils {

    private SecurityUtils() {}

    @SuppressWarnings("unchecked")
    public static Long getEmpresaId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getDetails() == null) return null;
        Map<String, Object> details = (Map<String, Object>) auth.getDetails();
        Object val = details.get("empresaId");
        if (val == null) return null;
        Long id = ((Number) val).longValue();
        return id == 0L ? null : id;
    }

    public static Long getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? (Long) auth.getPrincipal() : null;
    }
}
