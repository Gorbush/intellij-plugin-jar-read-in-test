
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.openapi.vfs.newvfs.VfsImplUtil;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.Nullable;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;

public class JarContentTest extends LightJavaCodeInsightFixtureTestCase {

    /**
     * TODO: Fix for JAR parsing https://intellij-support.jetbrains.com/hc/en-us/community/posts/4407534839826-JarFileSystem-getInstance-getJarRootForLocalFile-returns-null-in-test
     */
    public static final String TEST_PROJECT_FOLDER = "test-root";
    public static final String JAR_PATH = "lib/commons-lang3-3.10.jar";
    public static final String CONTENT_FILE_PATH = "/META-INF/MANIFEST.MF";

    VirtualFile vfJarCopied;
    Project project;
    VirtualFile baseDir;
    VirtualFile vfJarFile;

    public JarContentTest() {
        super();
    }

    @Override
    protected String getTestDataPath() {
        return TEST_PROJECT_FOLDER;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        vfJarCopied = myFixture.copyFileToProject(JAR_PATH);
        assertNotNull(vfJarCopied);

        project = myFixture.getProject();
        assertNotNull(project);

        baseDir = ApplicationManager.getApplication().isUnitTestMode()
                ? TempFileSystem.getInstance().findFileByPath("///src")
                : project.getBaseDir();

        assertNotNull(baseDir);

        vfJarFile = baseDir.findFileByRelativePath(JAR_PATH);
        assertNotNull("Jar file not found", vfJarFile);
        assertTrue("Jar file not found", vfJarFile.exists());
    }

    /**
     * This one DOES NOT works, as the path we provide is the relative path in the TMP filesystem
     * It returns the NULL - as it looks like JarFileSystem tries to evaluate it wrong
     */
    public void testJarAsArchiveFolder_VF() throws IOException {
        if ("ARCHIVE".equals(vfJarFile.getFileType().getName())) {
            //// Get content from Jar using VirtualFile
            VirtualFile jarRootForLocalFile = JarFileSystem.getInstance().getJarRootForLocalFile(vfJarFile);

            if (jarRootForLocalFile == null) {
                // Attempt to use the VfsUtilCore
                jarRootForLocalFile = VfsUtilCore.findRelativeFile(CONTENT_FILE_PATH, vfJarFile);
            }
            if (jarRootForLocalFile == null) {
                // Attempt to use the VfsUtilCore
                jarRootForLocalFile = VfsUtilCore.findRelativeFile(vfJarFile.getCanonicalPath()+"!"+CONTENT_FILE_PATH, vfJarFile);
            }
            assertNotNull("jarRootForLocalFile is still not populated", jarRootForLocalFile);

            VirtualFile manifestContentFile = jarRootForLocalFile.findFileByRelativePath(CONTENT_FILE_PATH);
            assertNotNull("Content of manifest file is not found", manifestContentFile);
            assertNotNull("Content is not retrieved", manifestContentFile.contentsToByteArray());
        } else {
            fail("Jar file is not an Archive");
        }
    }

    /**
     * This one works, as the path we provide is the full real path in the filesystem.
     * It returns the valid Jar File with the content parsed
     */
    public void testJarAsArchiveFolder_VF_FullPath() throws IOException {
        if ("ARCHIVE".equals(vfJarFile.getFileType().getName())) {
            //// Get content from Jar using VirtualFile
            File realJarFile = new File("test-root/lib/commons-lang3-3.10.jar");
            VirtualFile jarRootForLocalFile = VfsImplUtil.findFileByPath(JarFileSystem.getInstance(), realJarFile.getAbsolutePath()+"!/");
            // At the same time if we try to evaluate it with !/ at the end using TempSystem - it will not be able to parse that reference and also return NULL
            VfsImplUtil.findFileByPath(TempFileSystem.getInstance(), realJarFile.getAbsolutePath()+"!/");

            if (jarRootForLocalFile == null) {
                // Attempt to use the VfsUtilCore
                jarRootForLocalFile = VfsUtilCore.findRelativeFile(CONTENT_FILE_PATH, vfJarFile);
            }
            if (jarRootForLocalFile == null) {
                // Attempt to use the VfsUtilCore
                jarRootForLocalFile = VfsUtilCore.findRelativeFile(vfJarFile.getCanonicalPath()+"!"+CONTENT_FILE_PATH, vfJarFile);
            }
            assertNotNull("jarRootForLocalFile is still not populated", jarRootForLocalFile);

            VirtualFile manifestContentFile = jarRootForLocalFile.findFileByRelativePath(CONTENT_FILE_PATH);
            assertNotNull("Content of manifest file is not found", manifestContentFile);
            assertNotNull("Content is not retrieved", manifestContentFile.contentsToByteArray());
        } else {
            fail("Jar file is not an Archive");
        }
    }

    @Ignore
    public void testJarAsArchiveFolder_PSI() {
        if ("ARCHIVE".equals(vfJarFile.getFileType().getName())) {
            //// Get content from Jar using PsiFile
            @Nullable PsiFile jarPsiFile = PsiManager.getInstance(project).findFile(vfJarFile);
            // Returns NULL for findDirectory
            assertEquals("", jarPsiFile.getFileType().getName());
            assertTrue("No children found in PsiFile", jarPsiFile.getChildren().length > 0);
        } else {
            fail("Jar file is not an Archive");
        }
    }

}