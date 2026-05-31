package com.fidd.connectors.ydisk;

import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.base.BaseDirectoryConnector;
import com.fidd.ydisk.rest.Client;
import com.fidd.ydisk.rest.models.Resource;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class YandexDiskFiddConnector extends BaseDirectoryConnector implements FiddConnector {
    public final static Logger LOGGER = LoggerFactory.getLogger(YandexDiskFiddConnector.class);
    public static final long BUFFER_SIZE = 1024;

    final String user;
    final String token;
    public final Client client;
    final String fiddFolderPath;

    public YandexDiskFiddConnector(URL fiddFolderUrl) {
        try {
            String userInfo = fiddFolderUrl.getUserInfo();
            String[] userInfoParts = userInfo.split(":");
            if (userInfoParts.length == 1) {
                user = "ydisk";
                token = userInfoParts[0];
            } else {
                user = userInfoParts[0];
                token = userInfoParts[1];
            }

            fiddFolderPath = fiddFolderUrl.getPath();
            client = new Client(token, new ObjectMapper());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: relative paths?
    @Override
    protected boolean isRootFolder(String path) {
        Path folder = new File(path).toPath();
        Path fiddFolder = new File(fiddFolderPath).toPath();
        return folder.equals(fiddFolder);
    }

    // TODO: relative paths?
    // TODO: listing speed can be improved with sort, offset, limit parameters of the request
    @Override
    protected FileInfo getFileInfoInternal(String path) throws FileNotFoundException, IOException {
//        System.out.println(FMT.format(Instant.now()) + " getFileInfoInternal: " + path);
        try {
            List<FileListInfo> result = new ArrayList<>();
            Resource mainResource = client.getResources(StringUtils.defaultIfBlank(path, "/"));
            if (mainResource.embedded() != null && mainResource.embedded().items() != null) {
                for (Resource childResource : mainResource.embedded().items()) {
                    if ("dir".equals(childResource.type())) {
                        // Add dir
                        result.add(new FileListInfo(childResource.path(), true));
                    } else if ("file".equals(childResource.type())) {
                        //Add file
                        result.add(new FileListInfo(childResource.path(), false));
                    }
                }
            }
            return new FileInfo("dir".equals(mainResource.type()), mainResource.size() == null ? 0 : mainResource.size(), result);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("404")) {
                throw new FileNotFoundException("Path not found: " + path);
            }
            throw new IOException(e);
        }
    }

    @Override
    protected String fiddFolderPath() { return fiddFolderPath; }

    @Override
    protected byte[] readAllBytes(String path) throws IOException {
        try {
//            System.out.println(FMT.format(Instant.now()) + " readAllBytes: " + path);
            InputStream in = client.downloadFile(path);
            try (in) {
                return in.readAllBytes();
            }
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /**
     * DownloadListener.getLocalLength looks like offset to me
     * Length (Data limit) can be controlled by throwing an exception from OutputStream
     */
    @Override
    protected InputStream getSubInpuStream(String path, long offset, long length) throws IOException {
//        System.out.println(FMT.format(Instant.now()) + " getSubInputStream: " + path + ", offset: " + offset + ", length: " + length);
        try {
            return client.downloadFileWithRange(path, offset, length);
        } catch (IOException ie) {
            throw ie;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
