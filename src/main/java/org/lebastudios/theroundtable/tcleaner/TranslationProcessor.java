package org.lebastudios.theroundtable.tcleaner;

import org.apache.maven.plugin.logging.Log;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TranslationProcessor
{
    private final File resDir;
    private final File javaDir;
    private final String pluginId;
    private final boolean sort;
    private final boolean removeUnused;
    private final Log log;

    private final Pattern javaKeyUsagePattern;
    private final Pattern fxmlKeyUsagePattern;

    public TranslationProcessor(File resDir, File javaDir,
            String pluginId, boolean sort, boolean removeUnused, Log log)
    {
        this.resDir = resDir;
        this.javaDir = javaDir;
        this.pluginId = pluginId;
        this.sort = sort;
        this.removeUnused = removeUnused;
        this.log = log;


        var regex = "\"(" + Pattern.quote(pluginId) + ":" + "[a-zA-Z0-9_\\-.]+)\"";
        this.javaKeyUsagePattern = Pattern.compile(regex);

        regex = "\"%(" + Pattern.quote(pluginId) + ":" + "[a-zA-Z0-9_\\-.]+)\"";
        this.fxmlKeyUsagePattern = Pattern.compile(regex);
    }

    public void run() throws IOException
    {
        Set<String> usedKeys = collectUsedKeys();
        log.info("Detected " + usedKeys.size() + " used keys in source code.");

        Files.walk(resDir.toPath())
                .filter(p -> p.toString().endsWith(".properties") && p.getFileName().toString().startsWith("lang"))
                .forEach(p -> processPropertiesFile(p.toFile(), usedKeys));
    }

    private Set<String> collectUsedKeys() throws IOException
    {
        Set<String> keys = new HashSet<>();
        Files.walk(javaDir.toPath())
                .filter(p -> p.toString().endsWith(".java"))
                .forEach(p ->
                {
                    try
                    {
                        String content = Files.readString(p, StandardCharsets.UTF_8);
                        Matcher matcher = javaKeyUsagePattern.matcher(content);
                        while (matcher.find())
                        {
                            keys.add(matcher.group(1));
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("Could not read " + p + ": " + e.getMessage());
                    }
                });
        Files.walk(resDir.toPath())
                .filter(p -> p.toString().endsWith(".fxml"))
                .forEach(p ->
                {
                    try
                    {
                        String content = Files.readString(p, StandardCharsets.UTF_8);
                        Matcher matcher = fxmlKeyUsagePattern.matcher(content);
                        while (matcher.find())
                        {
                            keys.add(matcher.group(1));
                        }
                    }
                    catch (Exception e)
                    {
                        log.warn("Could not read " + p + ": " + e.getMessage());
                    }
                });
        return keys;
    }

    private void processPropertiesFile(File file, Set<String> usedKeys)
    {
        log.info("Processing: " + file.getName());
        try
        {
            Properties props = new Properties();
            try (InputStream is = new FileInputStream(file))
            {
                props.load(new InputStreamReader(is));
            }

            Map<String, String> entries = new LinkedHashMap<>();
            for (String key : props.stringPropertyNames())
            {
                String property = props.getProperty(key);
                if (property.contains("�"))
                {
                    log.warn("Found invalid character in property '" + key + "' in file " + file.getName());
                }
                entries.put(key, property);
            }

            if (sort)
            {
                entries = new TreeMap<>(entries);
            }

            Map<String, String> unusedEntries = new LinkedHashMap<>();
            Iterator<Map.Entry<String, String>> iterator = entries.entrySet().iterator();
            while (iterator.hasNext())
            {
                Map.Entry<String, String> entry = iterator.next();
                
                if (!usedKeys.contains(pluginId + ":" + entry.getKey()))
                {
                    unusedEntries.put(entry.getKey(), entry.getValue());
                    iterator.remove();
                }
            }

            try (Writer writer = new OutputStreamWriter(new FileOutputStream(file)))
            {
                for (Map.Entry<String, String> entry : entries.entrySet())
                {
                    writer.write(entry.getKey() + "=" + entry.getValue().replace("\n", "\\n") + "\n");
                }

                if (!removeUnused)
                {
                    writer.write("\n### Unused keys: ###\n");

                    for (Map.Entry<String, String> entry : unusedEntries.entrySet())
                    {
                        writer.write(entry.getKey() + "=" + entry.getValue().replace("\n", "\\n") + "\n");
                    }
                }
            }

            log.info("Updated: " + file.getName());
        }
        catch (IOException e)
        {
            log.error("Failed to process " + file.getName(), e);
        }
    }
}
