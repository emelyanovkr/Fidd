package com.fidd.connectors.folder;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.base.BaseDirectoryConnector;
import com.fidd.core.common.SubFileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class FolderFiddConnector extends BaseDirectoryConnector implements FiddConnector {
    public final static Logger LOGGER = LoggerFactory.getLogger(FolderFiddConnector.class);

    protected final String fiddFolderPath;
    protected final Path fiddFolder;

    public FolderFiddConnector(URL fiddFolderUrl) {
        try {
            Path fiddFolder = new File(fiddFolderUrl.toURI()).toPath();
            this.fiddFolder = fiddFolder;
            this.fiddFolderPath = fiddFolder.toAbsolutePath().toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public FolderFiddConnector(String fiddFolderPath) {
        this.fiddFolderPath = fiddFolderPath;
        this.fiddFolder = Paths.get(fiddFolderPath);
    }

    public FolderFiddConnector(Path fiddFolder) {
        this.fiddFolder = fiddFolder;
        this.fiddFolderPath = fiddFolder.toAbsolutePath().toString();
    }

    @Override
    protected String fiddFolderPath() { return fiddFolder.toString(); }

    @Override
    protected byte[] readAllBytes(String path) throws IOException {
        return Files.readAllBytes(Path.of(path));
    }

    @Override
    protected InputStream getSubInpuStream(String path, long offset, long length) throws IOException {
        return SubFileInputStream.of(Path.of(path).toFile(), offset, length);
    }

    @Override
    protected FileInfo getFileInfoInternal(String path) throws FileNotFoundException, IOException {
        if (!Files.exists(Path.of(path))) {
            throw new FileNotFoundException("Path not found: " + path);
        }
        boolean isDirectory = Files.isDirectory(Path.of(path));
        long size = Files.size(Path.of(path));

        List<FileListInfo> fileListInfos = new ArrayList<>();
        if (isDirectory) {
            try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Path.of(path))) {
                for (Path childPath : directoryStream) {
                    try {
                        fileListInfos.add(new FileListInfo(childPath.toString(), Files.isDirectory(childPath)));
                    } catch(Exception e) {
                        LOGGER.debug("Fidd subfolder is not a message / message number parse error", e);
                    }
                }
            } catch (IOException e) {
                LOGGER.error("Error reading directory: {}", path, e);
                throw e;
            }
        }

        return new FileInfo(isDirectory, size, fileListInfos);
    }
}
