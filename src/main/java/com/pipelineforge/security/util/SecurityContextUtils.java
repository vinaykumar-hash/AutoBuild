package com.pipelineforge.security.util;

import com.pipelineforge.security.service.SecurityUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

public final class SecurityContextUtils {
	private SecurityContextUtils() {
	}

	public static String currentUsername() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null || !authentication.isAuthenticated()) {
			return "system";
		}
		Object principal = authentication.getPrincipal();
		if (principal instanceof SecurityUser securityUser) {
			return securityUser.getUser().getEmail();
		}
		if (principal instanceof UserDetails userDetails) {
			return userDetails.getUsername();
		}
		if (principal instanceof String value) {
			return value;
		}
		return "system";
	}
}
