package com.example.application.usecase.dto;

import com.example.domain.values.Color;

/** Input DTO for changing style(color) of a shape */
public record ChangeColorInput(String shapeId, Color fillColor, Color strokeColor) {
}
