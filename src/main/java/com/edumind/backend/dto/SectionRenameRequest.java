package com.edumind.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SectionRenameRequest(
        @NotBlank @Size(max = 40) String name) {
}
