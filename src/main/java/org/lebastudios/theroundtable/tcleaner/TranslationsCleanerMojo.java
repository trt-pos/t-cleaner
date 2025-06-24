package org.lebastudios.theroundtable.tcleaner;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;

@Mojo(name = "clean-translations", defaultPhase = LifecyclePhase.VALIDATE)
public class TranslationsCleanerMojo extends AbstractMojo
{
    @Parameter(property = "translation.resDir", defaultValue = "${project.basedir}/src/main/resources")
    private File resDir;

    @Parameter(property = "translations.javaDir", defaultValue = "${project.basedir}/src/main/java")
    private File javaDir;

    @Parameter(property = "translation.pluginId", required = true)
    private String pluginId;
    
    @Parameter(property = "translation.sort", defaultValue = "true")
    private boolean sort;

    @Parameter(property = "translation.removeUnused", defaultValue = "false")
    private boolean removeUnused;

    @Override
    public void execute() throws MojoExecutionException
    {
        getLog().info("--- Translation Cleaner Plugin ---");
        getLog().info("Resource directory: " + resDir);
        getLog().info("Java directory:     " + javaDir);
        getLog().info("Sort:               " + sort);
        getLog().info("Remove unsused:     " + removeUnused);

        try {
            TranslationProcessor processor = new TranslationProcessor(
                    resDir,
                    javaDir,
                    pluginId,
                    sort,
                    removeUnused,
                    getLog()
            );
            processor.run();
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to process translations", e);
        }
    }
}
