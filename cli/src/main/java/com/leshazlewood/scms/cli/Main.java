/*
 * Copyright 2013-2021 Les Hazlewood, scms contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.leshazlewood.scms.cli;

import com.leshazlewood.scms.core.DefaultProcessor;
import com.leshazlewood.scms.core.Processor;
import com.leshazlewood.scms.core.Version;
import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** @since 0.1 */
public class Main {

  private static final String DEFAULT_CONFIG_FILE_NAME = DefaultProcessor.DEFAULT_CONFIG_FILE_NAME;

  private static final Option CONFIG =
      new Option(
          "c",
          "config",
          true,
          "read the config file at the specified path. Default is <src_dir>/"
              + DEFAULT_CONFIG_FILE_NAME);
  private static final Option DEBUG =
      new Option("d", "debug", false, "show additional error (stack trace) information.");
  private static final Option ENVIRONMENT =
      new Option("e", "env", true, "the configuration environment to enable.");
  private static final Option HELP = new Option("help", "help", false, "show this help message.");
  private static final Option VERSION =
      new Option("version", "version", false, "display the SCMS and Java versions");

  public static void main(String[] args) throws Exception {

    CommandLineParser parser = new DefaultParser();

    Options options = new Options();
    options
        .addOption(CONFIG)
        .addOption(ENVIRONMENT)
        .addOption(DEBUG)
        .addOption(HELP)
        .addOption(VERSION);

    boolean debug = false;
    File sourceDir = toFile(System.getProperty("user.dir"));
    File configFile = null;
    File destDir = null;
    String envName = null;

    try {
      CommandLine line = parser.parse(options, args);

      if (line.hasOption(VERSION.getOpt())) {
        printVersionAndExit();
      }
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
      if (line.hasOption(ENVIRONMENT.getOpt())) {
        envName = line.getOptionValue(ENVIRONMENT.getOpt());
        envName = envName != null ? envName.trim() : envName;
        envName = "".equals(envName) ? null : envName;
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

      assertConfigNotDirectory(configFile);

      /*

      else {
          String msg = "Configuration file not found.  Create a default " + DEFAULT_CONFIG_FILE_NAME +
              " file in your source directory or specify the " + CONFIG +
              " option to provide the file location.";
          throw new IllegalStateException(msg);
      }
      */

      Processor processor = new DefaultProcessor();
      processor.setSourceDir(sourceDir);
      processor.setDestDir(destDir);
      if (configFile != null) {
        processor.setConfigFile(configFile);
      }
      if (envName != null) {
        processor.setEnvironment(envName);
      }

      processor.init();
      processor.run();

      /*
      SiteExporter siteExporter = new SiteExporter();
      siteExporter.setSourceDir(sourceDir);
      siteExporter.setDestDir(destDir);
      siteExporter.setConfigFile(configFile);
      siteExporter.init();
      siteExporter.execute();
      */

    } catch (IllegalArgumentException iae) {
      exit(iae, debug);
    } catch (IllegalStateException ise) {
      exit(ise, debug);
    } catch (Exception e) {
      printHelpAndExit(options, e, debug, -1);
    }
  }

  private static void assertConfigNotDirectory(File f) {
    if (f.exists()) {
      if (f.isDirectory()) {
        throw new IllegalArgumentException(
            "Expected configuration file " + f + " is a directory, not a file.");
      }
    }
  }

  private static void printVersionAndExit() {
    System.out.println(
        "SCMS Version: "
            + Version.getVersion()
            + "\n"
            + "JVM Version : "
            + System.getProperty("java.version"));
    System.exit(0);
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
    String command = "scms [options] [src_dir] dest_dir";
    String header =
        "Injests content files in src_dir and renders them into dest_dir.\n\n"
            + "  src_dir is optional and defaults to the current working directory.\n"
            + "  dest_dir is required and cannot be the same as src_dir.";
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

    // help.printHelp("", "", options, null);
    // System.out.println(footer);
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
      columnWidth += 3; // buffer between description
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
