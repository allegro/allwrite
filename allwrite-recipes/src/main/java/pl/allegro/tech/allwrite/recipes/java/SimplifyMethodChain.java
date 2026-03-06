/*
 * Copyright 2021 the original author or authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.allegro.tech.allwrite.recipes.java;

import org.openrewrite.*;
import org.openrewrite.internal.lang.Nullable;
import org.openrewrite.java.JavaIsoVisitor;
import org.openrewrite.java.MethodMatcher;
import org.openrewrite.java.tree.Expression;
import org.openrewrite.java.tree.J;
import org.openrewrite.java.tree.JavaType;
import pl.allegro.tech.allwrite.recipes.kotlin.KotlinPropertyMatcher;

import java.util.List;

public class SimplifyMethodChain extends Recipe {

    List<String> methodPatternChain;
    String newMethodName;
    Boolean matchOverrides;

    public SimplifyMethodChain() {
    }

    public SimplifyMethodChain(List<String> methodPatternChain, String newMethodName, Boolean matchOverrides) {
        this.methodPatternChain = methodPatternChain;
        this.newMethodName = newMethodName;
        this.matchOverrides = matchOverrides;
    }

    @Option(displayName = "Method pattern chain",
        description = "A list of method patterns that are called in sequence",
        example = "['java.util.Map keySet()', 'java.util.Set contains(..)']")
    public void setMethodPatternChain(List<String> methodPatternChain) {
        this.methodPatternChain = methodPatternChain;
    }

    @Option(displayName = "New method name",
        description = "The method name that will replace the existing name. The new method name target is assumed to have the same arguments as the last method in the chain.",
        example = "containsKey")
    public void setNewMethodName(String newMethodName) {
        this.newMethodName = newMethodName;
    }

    @Option(displayName = "Match on overrides",
        description = "When enabled, find methods that are overrides of the method pattern.",
        required = false,
        example = "false")
    @Nullable
    public void setMatchOverrides(@Nullable Boolean matchOverrides) {
        this.matchOverrides = matchOverrides;
    }

    @Override
    public String getDisplayName() {
        return "Simplify a call chain";
    }

    @Override
    public String getDescription() {
        return "Simplify `a.b().c()` to `a.d()`.";
    }

    @Override
    public Validated<Object> validate() {
        return super.validate().and(Validated.test("methodPatternChain",
            "Requires more than one pattern",
            methodPatternChain, c -> c != null && c.size() > 1));
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        List<MethodMatcher> methodMatchers = methodPatternChain.stream()
            .map(it -> new MethodMatcher(it, matchOverrides))
            .toList()
            .reversed();

        List<KotlinPropertyMatcher> kotlinPropertyMatchers = methodPatternChain.stream()
            .map(KotlinPropertyMatcher::new)
            .toList()
            .reversed();

        return new JavaIsoVisitor<>() {
            @Override
            public J.MethodInvocation visitMethodInvocation(J.MethodInvocation method, ExecutionContext ctx) {

                J.MethodInvocation m = super.visitMethodInvocation(method, ctx);

                Expression select = m;

                for (MethodMatcher matcher : methodMatchers) {
                    if (select instanceof J.MethodInvocation sm && matcher.matches(sm)) {
                        select = sm.getSelect();
                    } else {
                        return m;
                    }
                }

                assert m.getMethodType() != null;
                JavaType.Method mt = m.getMethodType().withName(newMethodName);
                return m.withSelect(select)
                    .withName(m.getName().withSimpleName(newMethodName).withType(mt))
                    .withMethodType(mt);
            }

            @Override
            public J.FieldAccess visitFieldAccess(J.FieldAccess fieldAccess, ExecutionContext executionContext) {
                J.FieldAccess f = super.visitFieldAccess(fieldAccess, executionContext);

                Expression target = f;

                for (KotlinPropertyMatcher matcher : kotlinPropertyMatchers) {
                    if (target instanceof J.FieldAccess sf && matcher.matches(sf)) {
                        target = sf.getTarget();
                    } else {
                        return f;
                    }
                }

                assert f.getType() != null;
                return f.withTarget(target)
                    .withName(f.getName().withSimpleName(newMethodName));
            }
        };
    }
}
