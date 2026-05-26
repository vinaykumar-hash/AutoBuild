package com.pipelineforge.security.crypto;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;

@Converter(autoApply = false)
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {
	private final EncryptionService encryptionService;

	public EncryptedStringConverter(EncryptionService encryptionService) {
		this.encryptionService = encryptionService;
	}

	@Override
	public String convertToDatabaseColumn(String attribute) {
		return encryptionService.encrypt(attribute);
	}

	@Override
	public String convertToEntityAttribute(String dbData) {
		return encryptionService.decrypt(dbData);
	}
}
