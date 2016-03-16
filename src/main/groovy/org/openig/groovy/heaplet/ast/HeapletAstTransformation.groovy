/*
 * Copyright 2016 ForgeRock AS.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openig.groovy.heaplet.ast

import static java.lang.reflect.Modifier.FINAL
import static java.lang.reflect.Modifier.PUBLIC
import static java.lang.reflect.Modifier.STATIC

import org.codehaus.groovy.ast.ASTNode
import org.codehaus.groovy.ast.ClassHelper
import org.codehaus.groovy.ast.ClassNode
import org.codehaus.groovy.ast.ConstructorNode
import org.codehaus.groovy.ast.InnerClassNode
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.control.CompilePhase
import org.codehaus.groovy.control.SourceUnit
import org.codehaus.groovy.transform.AbstractASTTransformation
import org.codehaus.groovy.transform.GroovyASTTransformation
import org.openig.groovy.heaplet.GHeaplet

import groovy.transform.CompileStatic

/**
 * Created by guillaume on 05/03/16.
 */
@CompileStatic
@GroovyASTTransformation(phase = CompilePhase.SEMANTIC_ANALYSIS)
class HeapletAstTransformation extends AbstractASTTransformation {

    static final ClassNode GHEAPLET_NODE = ClassHelper.make(GHeaplet)

    @Override
    void visit(final ASTNode[] nodes, final SourceUnit source) {
        ClassNode outerNode = nodes[ 1 ] as ClassNode

        def innerClassNode = new InnerClassNode(outerNode,
                                                outerNode.name + '$Heaplet',
                                                PUBLIC | STATIC | FINAL,
                                                GHEAPLET_NODE)
        def statement = new BlockStatement()
        statement.addStatement(
                new ExpressionStatement(
                        new ConstructorCallExpression(
                                ClassNode.SUPER,
                                new ArgumentListExpression(new ClassExpression(outerNode)))))
        innerClassNode.addConstructor(new ConstructorNode(PUBLIC, statement))

        source.getAST().addClass(innerClassNode)
    }
}
