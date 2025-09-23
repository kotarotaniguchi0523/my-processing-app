package com.example.domain.values;


/**
 * Position value object representing x, y coordinates Immutable value object for position
 * information
 */
public final class Position {
  private final float x;
  private final float y;

  public Position(float x, float y) {
    this.x = x;
    this.y = y;
  }

  public float getX() {
    return x;
  }

  public float getY() {
    return y;
  }

  public Position add(Position other) {
    return new Position(x + other.x, y + other.y);
  }

  public Position subtract(Position other) {
    return new Position(x - other.x, y - other.y);
  }


  @Override
  public String toString() {
    return String.format("Position(x=%.2f, y=%.2f)", x, y);
  }

}
