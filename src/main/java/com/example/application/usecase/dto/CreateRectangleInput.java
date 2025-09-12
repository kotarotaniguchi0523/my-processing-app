package com.example.application.usecase.dto;

import com.example.domain.values.Color;

/**
 * Input DTO for creating a rectangle shape. Keeps UI / tool layer decoupled from domain model
 * constructors.
 */
public record CreateRectangleInput(float x, float y, float width, float height, Color fillColor,
    Color strokeColor, float strokeWeight, boolean hasFill, boolean hasStroke) {
}
