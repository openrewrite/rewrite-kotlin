package org.openrewrite.kotlin.psi;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;

@AllArgsConstructor
@EqualsAndHashCode
@Data
public class SourceRange {
    /**
     * Source code start position
     */
    int start;

    /**
     * Source code end position, not included.
     */
    int end;

    public boolean isInRange(SourceRange range) {
        return start > range.start && end < range.end;
    }

    public boolean includeRange(SourceRange range) {
        return (!range.equals(this)) && range.start >= start && range.end <= end;
    }

    @Override
    public String toString() {
        return "[" + start + "," + end + ")";
    }
}
