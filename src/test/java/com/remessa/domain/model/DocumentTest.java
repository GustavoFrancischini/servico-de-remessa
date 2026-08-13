package com.remessa.domain.model;

import com.remessa.domain.exception.InvalidDocumentException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DocumentTest {

    @Test
    void shouldAcceptValidCpfIgnoringFormatting() {
        Document document = Document.cpf("529.982.247-25");

        assertThat(document.type()).isEqualTo(DocumentType.CPF);
        assertThat(document.value()).isEqualTo("52998224725");
    }

    @Test
    void shouldRejectInvalidCpf() {
        assertThatThrownBy(() -> Document.cpf("111.111.111-11"))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void shouldRejectCpfWithWrongCheckDigit() {
        assertThatThrownBy(() -> Document.cpf("529.982.247-26"))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void shouldAcceptValidCnpjIgnoringFormatting() {
        Document document = Document.cnpj("11.222.333/0001-81");

        assertThat(document.type()).isEqualTo(DocumentType.CNPJ);
        assertThat(document.value()).isEqualTo("11222333000181");
    }

    @Test
    void shouldRejectInvalidCnpj() {
        assertThatThrownBy(() -> Document.cnpj("11.111.111/1111-11"))
                .isInstanceOf(InvalidDocumentException.class);
    }

    @Test
    void documentsWithSameTypeAndValueShouldBeEqual() {
        Document a = Document.cpf("529.982.247-25");
        Document b = Document.cpf("52998224725");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
