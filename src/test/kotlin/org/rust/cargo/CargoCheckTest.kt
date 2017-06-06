package org.rust.cargo

import com.intellij.util.PathUtil
import org.assertj.core.api.Assertions.assertThat
import org.rust.cargo.project.settings.toolchain

class CargoCheckTest : RustWithToolchainTestBase() {
    override val dataPath = "src/test/resources/org/rust/cargo/check/fixtures"

    fun `returns zero error code if project has no errors`() = withProject("hello") {
        val moduleDirectory = PathUtil.getParentPath(module.moduleFilePath)
        val result = module.project.toolchain!!.cargo(moduleDirectory).checkProject(testRootDisposable)
        assertThat(result.exitCode).isEqualTo(0)
    }
}
