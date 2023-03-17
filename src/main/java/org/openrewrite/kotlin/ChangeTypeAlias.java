package org.openrewrite.kotlin;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.openrewrite.ExecutionContext;
import org.openrewrite.Option;
import org.openrewrite.Recipe;
import org.openrewrite.TreeVisitor;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.NameTree;
import org.openrewrite.java.tree.TypeTree;
import org.openrewrite.java.tree.TypeUtils;
import org.openrewrite.kotlin.marker.Modifier;

import java.util.List;

@Value
@EqualsAndHashCode(callSuper = true)
public class ChangeTypeAlias extends Recipe {

    @Option(displayName = "Old fully-qualified alias name",
            description = "Fully-qualified class name of the alias type.",
            example = "org.junit.Assume")
    String aliasName;

    @Option(displayName = "New fully-qualified alias name",
            description = "Fully-qualified class name of the alias type.",
            example = "org.junit.Assume")
    String newName;

    @Option(displayName = "Aliased type name",
            description = "Fully-qualified class name of the aliased type.",
            example = "org.junit.Assume")
    String fullyQualifiedAliasedType;

    @Override
    public String getDisplayName() {
        return "Change type alias";
    }

    @Override
    public String getDescription() {
        return "Change a given type alias to another.";
    }

    @Override
    protected TreeVisitor<?, ExecutionContext> getVisitor() {
        return new KotlinIsoVisitor<ExecutionContext>() {
            @Override
            public J visitVariableDeclarations(J.VariableDeclarations multiVariable, ExecutionContext executionContext) {
                if (isTypeAlias(multiVariable.getLeadingAnnotations()) && TypeUtils.isOfClassType(multiVariable.getType(), fullyQualifiedAliasedType)) {
                    return super.visitVariableDeclarations(multiVariable, executionContext);
                } else if (isAliasedTypeExpression(multiVariable.getTypeExpression())) {
                    TypeTree typeExpression = multiVariable.getTypeExpression();
                    if (typeExpression instanceof J.Identifier) {
                        return multiVariable.withTypeExpression(((J.Identifier) typeExpression).withSimpleName(newName));
                    } else if (typeExpression instanceof J.ParameterizedType) {
                        NameTree clazz = ((J.ParameterizedType) typeExpression).getClazz();
                        if (clazz instanceof J.Identifier) {
                            return multiVariable.withTypeExpression(((J.ParameterizedType) typeExpression).withClazz(((J.Identifier) clazz).withSimpleName(newName)));
                        } else if (clazz instanceof J.FieldAccess) {
                            return multiVariable.withTypeExpression(((J.ParameterizedType) typeExpression).withClazz(((J.FieldAccess) clazz).withName(((J.FieldAccess) clazz).getName().withSimpleName(newName))));
                        }
                    }
                }
                return multiVariable;
            }

            @Override
            public J visitVariable(J.VariableDeclarations.NamedVariable variable, ExecutionContext executionContext) {
                return variable.withName(variable.getName().withSimpleName(newName));
            }

            private boolean isTypeAlias(List<J.Annotation> annotationList) {
                return annotationList.stream()
                        .anyMatch(a -> "typealias".equals(a.getSimpleName()) && a.getMarkers().findFirst(Modifier.class).isPresent());
            }

            private boolean isAliasedTypeExpression(@Nullable J typeExpression) {
                if (typeExpression instanceof J.Identifier) {
                    return ((J.Identifier) typeExpression).getSimpleName().equals(aliasName) && TypeUtils.isOfClassType(((J.Identifier) typeExpression).getType(), fullyQualifiedAliasedType);
                } else if (typeExpression instanceof J.ParameterizedType) {
                    return isAliasedTypeExpression(((J.ParameterizedType) typeExpression).getClazz());
                }
                return false;
            }
        };
    }
}
