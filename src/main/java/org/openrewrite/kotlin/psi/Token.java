package org.openrewrite.kotlin.psi;

import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
@Data
public class Token {
    SourceRange range;
    String type;
    String text;

    @Override
    public String toString() {
        return range + " | Type: " + type + " | Text: \"" + text.replace("\n", "\\n" + "\"");
    }
}
