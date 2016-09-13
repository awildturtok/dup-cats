package de.koxi.duplicates;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by fk on 13.09.2016.
 */
public class Duplicates
{
    public static final String PROGRAM_NAME = "kx_dup";

    public static void main(String[] args) throws IOException {
        Options options = new Options();

        // add options
        Option help_option = new Option("h", "help", false, "Print this help page.");

        Option dry_run_option = new Option("D", "dry-run", false, "Do a dry run.");
        dry_run_option.setRequired(false);

        Option hash_option = new Option("H", "hash", true, "Hashing Algorithm Name. Default is MD5.\\n See: http://docs.oracle.com/javase/6/docs/technotes/guides/security/StandardNames.html#alg");
        hash_option.setRequired(false);

        Option buffer_size_option = new Option("b", "buffer", true, "Amount of read data in kilobytes, for hashing. Bigger size produces more precise results, but is also slower.");
        hash_option.setRequired(false);

        Option dir_option = new Option("d", "dir", true, "start directory");
        dir_option.setRequired(false);

        Option move_option = new Option("t", "target-dir", true, "Move files to target directory");
        move_option.setRequired(false);

        Option move_relative_option = new Option("rel", "relative", false, "Move files relative to root & target");
        move_relative_option.setRequired(false);


        options
                .addOption(help_option)
                .addOption(dry_run_option)
                .addOption(buffer_size_option)
                .addOption(dir_option)
                .addOption(hash_option)
//                .addOptionGroup(new OptionGroup().addOption(move_option).addOption(move_relative_option))
        // TODO: 13.09.2016 Implement moving strategies
        ;


        CommandLineParser parser = new BasicParser();
        HelpFormatter formatter = new HelpFormatter();
        final CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp(PROGRAM_NAME, options);

            System.exit(1);
            return;
        }

        if(cmd.hasOption('h'))
        {
            formatter.printHelp(PROGRAM_NAME, options);

            System.exit(0);
            return;
        }

        final boolean dry_run = cmd.hasOption('D');

        final String hashname = cmd.getOptionValue('H', "MD5");
        final Path root_dir = Paths.get(cmd.getOptionValue('d', "."));
        final int buffer_size_kb = Integer.parseInt(cmd.getOptionValue('b', "2"));

        Consumer<File> duplicateAction = DuplicateActions.getDeleteAction();

        if(cmd.hasOption('t'))
        {
            final Path target_dir = Paths.get(cmd.getOptionValue('t'));

            if (cmd.hasOption("rel"))
                duplicateAction = DuplicateActions.getRelativeMoveAction(root_dir, target_dir);
            else
                duplicateAction = DuplicateActions.getMoveAction(target_dir);

            target_dir.toFile().mkdirs();
        }


        findDuplicates(
                root_dir
                , hashname
                , (dry_run ? DuplicateActions.getNoAction() : duplicateAction).andThen(DuplicateActions.getPrintAction())
                , buffer_size_kb
        );
    }


    public static List<Path> findDuplicates(Path root, String hashname, Consumer<File> duplicateAction, int buffer_size_kb) throws IOException
    {
        final File root_file = root.toFile();

        if (!root_file.isDirectory())
            return new LinkedList<>();

        final DuplicateFinder duplicateFinder = new DuplicateFinder(hashname, FileUtils.ONE_KB * buffer_size_kb, duplicateAction);
// TODO: 13.09.2016 add option for link following
        Files.walkFileTree(root, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE, duplicateFinder);

        System.out.println();
        System.out.println(duplicateFinder.summarise());

        return duplicateFinder.getFoundDuplicates();
    }
}
