package de.koxi.duplicates;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

/**
 * Created by fk on 13.09.2016.
 */
public class DuplicateActions {

    public static Consumer<File> getNoAction() {
        return (__) -> { };
    }


        public static Consumer<File> getDeleteAction() {
        return File::delete;
    }

    public static Consumer<File> getRelativeMoveAction(Path from, Path to)
    {
        return p -> System.out.format("moving %s to %s%n"
                , p.getAbsolutePath()
                , to.resolve(p.getName())
        );

//        return p -> {
//            try {
//                Files.move(p, to.resolve(p.getName()).toFile());
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        };
    }

    public static Consumer<File> getMoveAction(Path to) {
//        return p -> System.out.format("moving %s to %s%n"
//                , p.getAbsolutePath()
//                , to.resolve(p.getName()));

        return p -> {
            try {
                Files.move(p.toPath(), to.resolve(p.getName()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }

    public static Consumer<File> getPrintAction()
    {
        return p -> {
            try {
                System.out.format("%s%n", p.getCanonicalPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        };
    }
}