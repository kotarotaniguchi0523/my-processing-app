package com.example.application.usecase.dto;

/** Input DTO for moving a shape */
public record MoveShapeInput(String shapeId, float newX, float newY) {
}
