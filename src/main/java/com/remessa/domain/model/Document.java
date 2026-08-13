package com.remessa.domain.model;

import com.remessa.domain.exception.InvalidDocumentException;

import java.util.Objects;

/**
 * Value object que representa um CPF ou CNPJ já validado (formato e dígitos verificadores).
 * A distinção entre Pessoa Física e Pessoa Jurídica é modelada através do {@link DocumentType},
 * evitando a necessidade de duas hierarquias de usuário para uma única diferença de dado.
 */
public final class Document {

    private final DocumentType type;
    private final String value;

    private Document(DocumentType type, String value) {
        this.type = type;
        this.value = value;
    }

    public static Document cpf(String rawValue) {
        String digits = onlyDigits(rawValue);
        if (!isValidCpf(digits)) {
            throw new InvalidDocumentException("CPF inválido: " + rawValue);
        }
        return new Document(DocumentType.CPF, digits);
    }

    public static Document cnpj(String rawValue) {
        String digits = onlyDigits(rawValue);
        if (!isValidCnpj(digits)) {
            throw new InvalidDocumentException("CNPJ inválido: " + rawValue);
        }
        return new Document(DocumentType.CNPJ, digits);
    }

    /** Reconstrói um Document já validado anteriormente (uso restrito à camada de persistência). */
    public static Document reconstruct(DocumentType type, String value) {
        return new Document(type, value);
    }

    public DocumentType type() {
        return type;
    }

    public String value() {
        return value;
    }

    private static String onlyDigits(String raw) {
        return raw == null ? "" : raw.replaceAll("\\D", "");
    }

    private static boolean isValidCpf(String cpf) {
        if (cpf.length() != 11 || allDigitsEqual(cpf)) {
            return false;
        }
        int firstDigit = calculateCheckDigit(cpf.substring(0, 9), 10);
        int secondDigit = calculateCheckDigit(cpf.substring(0, 9) + firstDigit, 11);
        return cpf.equals(cpf.substring(0, 9) + firstDigit + secondDigit);
    }

    private static boolean isValidCnpj(String cnpj) {
        if (cnpj.length() != 14 || allDigitsEqual(cnpj)) {
            return false;
        }
        int[] firstWeights = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int[] secondWeights = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
        int firstDigit = calculateCheckDigit(cnpj.substring(0, 12), firstWeights);
        int secondDigit = calculateCheckDigit(cnpj.substring(0, 12) + firstDigit, secondWeights);
        return cnpj.equals(cnpj.substring(0, 12) + firstDigit + secondDigit);
    }

    private static int calculateCheckDigit(String base, int startWeight) {
        int sum = 0;
        int weight = startWeight;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weight--;
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static int calculateCheckDigit(String base, int[] weights) {
        int sum = 0;
        for (int i = 0; i < base.length(); i++) {
            sum += Character.getNumericValue(base.charAt(i)) * weights[i];
        }
        int remainder = sum % 11;
        return remainder < 2 ? 0 : 11 - remainder;
    }

    private static boolean allDigitsEqual(String digits) {
        return digits.chars().distinct().count() == 1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Document other)) {
            return false;
        }
        return type == other.type && value.equals(other.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, value);
    }

    @Override
    public String toString() {
        return type + ":" + value;
    }
}
