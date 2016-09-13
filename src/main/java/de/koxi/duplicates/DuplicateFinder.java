package de.koxi.duplicates;

import org.apache.commons.codec.digest.DigestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.MessageDigest;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Created by fk on 13.09.2016.
 */
public class DuplicateFinder extends SimpleFileVisitor<Path>
{
    private final List<Path> _duplicates = new LinkedList<>();

    private final Set<BigInteger> _visited;
    private final MessageDigest _digest;

    private final byte[] _buffer;

    private final Consumer<File> _duplicateAction;

    public DuplicateFinder(String hashAlgorithm, long buffer_size, Consumer<File> duplicateAction)
    {
        _digest = DigestUtils.getDigest(hashAlgorithm);
        _duplicateAction = duplicateAction;
        _visited = new HashSet<>();
        _buffer = new byte[(int)buffer_size];
    }

    public String summarise()
    {
        return String.format("Found %d duplicates in %d unique files%n", _duplicates.size(), _visited.size());
    }

    @Override
    public FileVisitResult visitFile(Path path, BasicFileAttributes attrs) throws IOException
    {
        final File file = path.toFile();

        if(!file.canRead())
            return FileVisitResult.CONTINUE;

        final InputStream inputStream = java.nio.file.Files.newInputStream(path, StandardOpenOption.READ);

        final int read = inputStream.read(_buffer);

        _digest.update(_buffer, 0, read);

        final byte[] hashed = _digest.digest();

        if(!_visited.add(new BigInteger(1, hashed)))
        {
            _duplicateAction.accept(file);
            _duplicates.add(path);
        }

        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException
    {
        final File file = dir.toFile();

        if(file == null)
            return FileVisitResult.CONTINUE;

        final String[] files = file.list();
        if(files == null || files.length == 0)
            file.delete();

        return FileVisitResult.CONTINUE;
    }

    public List<Path> getFoundDuplicates()
    {
        return _duplicates;
    }
}
