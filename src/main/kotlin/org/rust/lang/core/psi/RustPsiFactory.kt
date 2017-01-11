package org.rust.lang.core.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFileFactory
import org.rust.lang.RustLanguage
import org.rust.lang.core.psi.impl.mixin.selfParameter
import org.rust.lang.core.psi.impl.mixin.valueParameters
import org.rust.lang.core.psi.util.childOfType

class RustPsiFactory(private val project: Project) {
    fun createIdentifier(text: String): PsiElement =
        createFromText<RustModDeclItemElement>("mod $text;")?.identifier
            ?: error("Failed to create identifier: `$text`")

    fun createExpression(text: String): RustExprElement =
        createFromText("fn main() { $text; }")
            ?: error("Failed to create expression from text: `$text`")

    fun createBlockExpr(body: String): RustBlockExprElement =
        createExpression("{ $body }") as RustBlockExprElement

    fun createStructExprBody(fieldNames: List<String>): RustStructExprBodyElement {
        val fields = fieldNames.map { "$it: ()," }.joinToString("\n")
        return (createExpression("S { $fields }") as RustStructExprElement).structExprBody
    }

    fun createStatement(text: String): RustStmtElement =
        createFromText("fn main() { $text 92; }")
            ?: error("Failed to create statement from text: `$text`")

    fun createLetDeclaration(name: String, expr: RustExprElement, mutable: Boolean = false): RustLetDeclElement =
        createStatement("let ${if (mutable) "mut " else ""}$name = ${expr.text};") as RustLetDeclElement

    fun createType(text: String): RustTypeElement =
        createFromText("fn main() { let a : $text; }")
            ?: error("Failed to create type from text: `$text`")

    fun createMethodParam(text: String): PsiElement {
        val fnItem: RustFunctionElement = createTraitMethodMember("fn foo($text);")
        return fnItem.selfParameter ?: fnItem.valueParameters.firstOrNull()
            ?: error("Failed to create type from text: `$text`")
    }

    fun createReferenceType(innerTypeText: String, mutable: Boolean): RustRefLikeTypeElement =
        createType("&${if (mutable) "mut " else ""}$innerTypeText") as RustRefLikeTypeElement

    fun createModDeclItem(modName: String): RustModDeclItemElement =
        createFromText("mod $modName;")
            ?: error("Failed to crate mod decl with name: `$modName`")

    fun createUseItem(text: String): RustUseItemElement =
        createFromText("use $text;")
            ?: error("Failed to create use item from text: `$text`")

    fun createTraitImplItem(traitMethods: List<RustFunctionElement>): RustImplItemElement {
        val methods = traitMethods
            .mapNotNull { " ${it.signatureText} {\nunimplemented!()\n}" }
            .joinToString("\n\n")
        val text = "impl T for S { $methods }"
        return createFromText(text)
            ?: error("Failed to create an impl from text: `$text`")
    }

    fun createTraitMethodMember(text: String): RustFunctionElement {
        val traitImpl: RustTraitItemElement = createFromText("trait Foo { $text }") ?:
            error("Failed to create an method member from text: `$text`")
        return traitImpl.functionList.first()
    }

    fun createInherentImplItem(name: String): RustImplItemElement =
        createFromText("impl $name {  }")
            ?: error("Failed to create an inherent impl with name: `$name`")

    fun createWhereClause(
        lifetimeBounds: List<RustLifetimeParameterElement>,
        typeBounds: List<RustTypeParameterElement>
    ): RustWhereClauseElement {

        val lifetimes = lifetimeBounds
            .filter { it.lifetimeParamBounds != null }
            .mapNotNull { it.text }

        val typeConstraints = typeBounds
            .filter { it.typeParamBounds != null }
            .mapNotNull { it.text }

        val whereClauseConstraints = (lifetimes + typeConstraints).joinToString(", ")

        val text = "where $whereClauseConstraints"
        return createFromText("fn main() $text {}")
            ?: error("Failed to create a where clause from text: `$text`")
    }

    fun createGenericParams(
        params: Iterable<String>
    ): RustTypeParameterListElement {
        val text = params.joinToString(prefix = "<", separator = ", ", postfix = ">")

        return createFromText<RustFunctionElement>("fn foo$text() {}")?.typeParameterList
            ?: error("Failed to create type from text: `$text`")
    }

    fun createOuterAttr(text: String): RustOuterAttrElement =
        createFromText("#[$text] struct Dummy;")
            ?: error("Failed to create an outer attribute from text: `$text`")

    private inline fun <reified T : RustCompositeElement> createFromText(code: String): T? =
        PsiFileFactory.getInstance(project)
            .createFileFromText("DUMMY.rs", RustLanguage, code)
            ?.childOfType<T>()
}

private val RustFunctionElement.signatureText: String? get() {
    // We can't simply take a substring of original method declaration
    // because of anonymous parameters.
    val name = name ?: return null
    val generics = typeParameterList?.text ?: ""

    val allArguments = listOfNotNull(selfParameter?.text) + valueParameters.map {
        // fix possible anon parameter
        "${it.pat?.text ?: "_"}: ${it.type?.text ?: "()"}"
    }

    val ret = retType?.text ?: ""
    val where = whereClause?.text ?: ""
    return "fn $name $generics (${allArguments.joinToString(",")}) $ret $where"
}
