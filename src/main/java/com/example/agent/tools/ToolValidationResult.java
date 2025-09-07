package com.example.agent.tools;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ToolValidationResult represents the result of validating tool parameters Contains information
 * about validation errors and warnings
 */
public final class ToolValidationResult {

  private final boolean valid;
  private final List<String> errors;
  private final List<String> warnings;

  private ToolValidationResult(boolean valid, List<String> errors, List<String> warnings) {
    this.valid = valid;
    this.errors = new ArrayList<>(Objects.requireNonNull(errors, "Errors cannot be null"));
    this.warnings = new ArrayList<>(Objects.requireNonNull(warnings, "Warnings cannot be null"));
  }

  public static class Builder {
    private boolean valid = true;
    private List<String> errors = new ArrayList<>();
    private List<String> warnings = new ArrayList<>();

    public Builder valid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public Builder addError(String error) {
      this.errors.add(error);
      this.valid = false;
      return this;
    }

    public Builder addWarning(String warning) {
      this.warnings.add(warning);
      return this;
    }

    public Builder errors(List<String> errors) {
      this.errors = new ArrayList<>(errors);
      if (!errors.isEmpty()) {
        this.valid = false;
      }
      return this;
    }

    public Builder warnings(List<String> warnings) {
      this.warnings = new ArrayList<>(warnings);
      return this;
    }

    public ToolValidationResult build() {
      return new ToolValidationResult(valid, errors, warnings);
    }
  }

  public static Builder builder() {
    return new Builder();
  }

  // Static factory methods
  public static ToolValidationResult valid() {
    return new Builder().valid(true).build();
  }

  public static ToolValidationResult invalid(String error) {
    return new Builder().addError(error).build();
  }

  public static ToolValidationResult invalid(List<String> errors) {
    return new Builder().errors(errors).build();
  }

  public static ToolValidationResult withWarning(String warning) {
    return new Builder().addWarning(warning).build();
  }

  // Getters
  public boolean isValid() {
    return valid;
  }

  public List<String> getErrors() {
    return new ArrayList<>(errors);
  }

  public List<String> getWarnings() {
    return new ArrayList<>(warnings);
  }

  // Utility methods
  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }

  public String getErrorSummary() {
    return String.join("; ", errors);
  }

  public String getWarningSummary() {
    return String.join("; ", warnings);
  }

  // Combine validation results
  public ToolValidationResult combine(ToolValidationResult other) {
    List<String> combinedErrors = new ArrayList<>(this.errors);
    combinedErrors.addAll(other.errors);

    List<String> combinedWarnings = new ArrayList<>(this.warnings);
    combinedWarnings.addAll(other.warnings);

    return new Builder().valid(this.valid && other.valid).errors(combinedErrors)
        .warnings(combinedWarnings).build();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ToolValidationResult)) {
      return false;
    }
    ToolValidationResult result = (ToolValidationResult) obj;
    return valid == result.valid && Objects.equals(errors, result.errors)
        && Objects.equals(warnings, result.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(valid, errors, warnings);
  }

  @Override
  public String toString() {
    if (valid && !hasWarnings()) {
      return "ToolValidationResult(valid)";
    }

    // Pre-size: base label (20) + flags (~20) + numbers (~10)
    StringBuilder sb = new StringBuilder(54);
    sb.append("ToolValidationResult(");
    if (!valid) {
      sb.append("invalid, errors=").append(errors.size());
    } else {
      sb.append("valid");
    }

    if (hasWarnings()) {
      sb.append(", warnings=").append(warnings.size());
    }

    sb.append(')');
    return sb.toString();
  }
}
