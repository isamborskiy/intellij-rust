package org.rust.ide.docs

import com.intellij.codeInsight.documentation.DocumentationManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.intellij.lang.annotations.Language
import org.rust.lang.RsTestBase
import java.io.File

abstract class RsDocumentationProviderTest : RsTestBase() {

    protected inline fun doTest(@Language("Rust") code: String,
                                @Language("Html") expected: String,
                                block: RsDocumentationProvider.(PsiElement, PsiElement?) -> String?) {
        InlineFile(code)

        val (originalElement, _, offset) = findElementWithDataAndOffsetInEditor<PsiElement>()
        val element = DocumentationManager.getInstance(project)
            .findTargetElement(myFixture.editor, offset, myFixture.file, originalElement)!!

        val actual = RsDocumentationProvider().block(element, originalElement)?.trim()
        assertSameLines(expected.trimIndent(), actual)
    }

    protected fun compareByHtml(block: (PsiElement, PsiElement?) -> String?) {
        val expectedFile = File("$testDataPath/${fileName.replace(".rs", ".html")}")
        val expected = FileUtil.loadFile(expectedFile).trim()

        myFixture.configureByText("main.rs", FileUtil.loadFile(File("$testDataPath/$fileName")))
        val originalElement = myFixture.elementAtCaret
        val element = DocumentationManager.getInstance(project).findTargetElement(myFixture.editor, myFixture.file)
        val actual = block(element, originalElement)?.trim()
        assertSameLines(expected, actual)
    }
}
