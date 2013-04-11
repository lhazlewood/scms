/*
 * Copyright 2013 Les Hazlewood
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
package com.leshazlewood.scms.cli;

import com.leshazlewood.scms.core.SiteExporter;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;

import java.io.File;
import java.io.IOException;

/**
 * @since 0.1
 */
public class Main {

    private static final String DEFAULT_CONFIG_FILE_NAME = SiteExporter.DEFAULT_CONFIG_FILE_NAME;

    private static final Option CONFIG = new Option("c", "config", true, "read the config file at the specified path. Default is <src_dir>/" + DEFAULT_CONFIG_FILE_NAME);
    private static final Option DEBUG = new Option("d", "debug", false, "show additional error (stack trace) information.");
    private static final Option HELP = new Option("help", "help", false, "show this help message.");

    public static void main(String[] args) throws Exception {

        CommandLineParser parser = new PosixParser();

        Options options = new Options();
        options.addOption(CONFIG).addOption(DEBUG).addOption(HELP);

        boolean debug = false;
        File sourceDir = toFile(System.getProperty("user.dir"));
        File configFile = null;
        File destDir = null;

        try {
            CommandLine line = parser.parse(options, args);

            if (line.hasOption(HELP.getOpt())) {
                printHelpAndExit(options, null, debug, 0);
            }
            if (line.hasOption(DEBUG.getOpt())) {
                debug = true;
            }
            if (line.hasOption(CONFIG.getOpt())) {
                String configFilePath = line.getOptionValue(CONFIG.getOpt());
                configFile = toFile(configFilePath);
            }

            String[] remainingArgs = line.getArgs();
            if (remainingArgs == null) {
                printHelpAndExit(options, null, debug, -1);
            }

            assert remainingArgs != null;

            if (remainingArgs.length == 1) {
                String workingDirPath = System.getProperty("user.dir");
                sourceDir = toFile(workingDirPath);
                destDir = toFile(remainingArgs[0]);
            } else if (remainingArgs.length == 2) {
                sourceDir = toFile(remainingArgs[0]);
                destDir = toFile((remainingArgs[1]));
            } else {
                printHelpAndExit(options, null, debug, -1);
            }

            assert sourceDir != null;
            assert destDir != null;

            if (configFile == null) {
                configFile = new File(sourceDir, DEFAULT_CONFIG_FILE_NAME);
            }

            if (configFile.exists()) {
                if (configFile.isDirectory()) {
                    throw new IllegalArgumentException("Expected configuration file " + configFile + " is a directory, not a file.");
                }
            } else {
                String msg = "Configuration file not found.  Create a default " + DEFAULT_CONFIG_FILE_NAME +
                        " file in your source directory or specify the " + CONFIG +
                        " option to provide the file location.";
                throw new IllegalStateException(msg);
            }

            SiteExporter siteExporter = new SiteExporter();
            siteExporter.setSourceDir(sourceDir);
            siteExporter.setDestDir(destDir);
            siteExporter.setConfigFile(configFile);
            siteExporter.init();
            siteExporter.execute();

        } catch (IllegalArgumentException iae) {
            exit(iae, debug);
        } catch (IllegalStateException ise) {
            exit(ise, debug);
        } catch (IOException e) {
            printHelpAndExit(options, e, debug, -1);
        }
    }

    private static void printHelpAndExit(Options options, Exception e, boolean debug, int exitCode) {
        printHelp(options, e, debug);
        System.exit(exitCode);
    }

    private static void exit(Exception e, boolean debug) {
        printException(e, debug);
        System.exit(-1);
    }

    private static void printHelp(Options options, Exception e, boolean debug) {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(80);
        String command = "java -jar scms-cli-<version>-cli.jar [options] [src dir] dest_dir";
        String header = "Injests content files in [src dir] and renders a static website into dest_dir.\n\n" +
                "  If unspecified, [src dir] defaults to the current working directory.  dest_dir is required and " +
                "cannot be the same as the source directory.";
        /*String footer = "\n" +
                "Injests source content files and page templates in [src dir] and renders a\n" +
                "renders a static website into destination_directory.\n\n" +
                "If unspecified, [source directory] defaults to the current working\n" +
                "directory.  destination_directory is required and cannot be the same\n" +
                "as the source directory.";*/

        printException(e, debug);

        System.out.println();

        System.out.println("Usage:");
        System.out.print("  ");
        System.out.println(command);
        System.out.println();
        System.out.println("Description:");
        System.out.print("  ");
        System.out.println(header);
        System.out.println();
        System.out.println("Options:");

        StringBuilder sb = new StringBuilder();

        int columnWidth = calculateColumnWidth(options);

        for (Object o : options.getOptions()) {
            Option option = (Option) o;
            StringBuilder csb = new StringBuilder("  ");
            csb.append("-").append(option.getOpt()).append(",--").append(option.getLongOpt());
            if (option.hasArg()) {
                csb.append(" <arg>");
            }
            int csbLength = csb.length();
            for (int i = 0; i < (columnWidth - csbLength); i++) {
                csb.append(" ");
            }
            sb.append(csb.toString()).append("   ").append(option.getDescription()).append("\n");
        }
        System.out.println(sb);

        //help.printHelp("", "", options, null);
        //System.out.println(footer);
    }

    private static int calculateColumnWidth(Options options) {
        int max = 0;
        for (Object o : options.getOptions()) {
            Option opt = (Option) o;
            int columnWidth = "-".length() + opt.getOpt().length();
            if (opt.hasLongOpt()) {
                columnWidth += ",--".length();
                columnWidth += opt.getLongOpt().length();
            }
            if (opt.hasArg()) {
                columnWidth += " <arg>".length();
            }
            columnWidth += 3; //buffer between description
            max = Math.max(max, columnWidth);
        }
        return max;
    }

    private static void printException(Exception e, boolean debug) {
        if (e != null) {
            System.out.println();
            if (debug) {
                System.out.println("Error: ");
                e.printStackTrace(System.out);
                System.out.println(e.getMessage());
            } else {
                System.out.println("Error: " + e.getMessage());
                System.out.println();
                System.out.println("Specify -d or --debug for more information.");
            }
        }
    }

    private static File toFile(String path) {
        String resolved = path;
        if (path.startsWith("~/") || path.startsWith(("~\\"))) {
            resolved = path.replaceFirst("\\~", System.getProperty("user.home"));
        }
        return new File(resolved);
    }
}
