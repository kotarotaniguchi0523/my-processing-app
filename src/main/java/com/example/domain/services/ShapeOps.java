package com.example.domain.services;

import com.example.domain.models.DrawingState;
import com.example.domain.models.Shape;

import java.util.List;
import java.util.Optional;

/**
 * ShapeOps: Phase A3 abstraction isolating all shape-level read/write operations.
 *
 * Responsibilities: - Provide immutable state snapshots (state()) - Offer O(1) id lookup (optional
 * initial simple linear scan; can optimize later) - Centralize mutation primitives (add / replace /
 * batchReplace / delete) - Return new immutable DrawingState on each mutating operation
 *
 * Out of scope (for now): persistence, history ring, grouping semantics
 */
public interface ShapeOps {
  /** Latest immutable snapshot. */
  DrawingState state();

  /** Find shape by id. */
  Optional<Shape> findById(String id);

  /** All shapes in drawing order. */
  List<Shape> listAll();

  /** Append a shape. */
  DrawingState add(Shape shape);

  /** Replace shape with same id; no-op if not found. */
  DrawingState replace(String id, Shape replacement);

  /** Batch replace by id; skip ids not found. */
  DrawingState batchReplace(List<Shape> replacements);

  /** Delete shape by id; no-op if not found. */
  DrawingState delete(String id);

  /**
   * Clear all shapes in one atomic operation returning the new empty state.
   *
   * Implementations should avoid per-shape delete loops for performance and consistency (single
   * state mutation + history entry). Non-shape data (settings/arrays/lists) must be preserved.
   */
  DrawingState clearAll();
}
