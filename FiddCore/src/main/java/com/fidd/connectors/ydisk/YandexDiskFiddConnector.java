package com.fidd.connectors.ydisk;

import com.fidd.common.streamchain.BufferChainInputStream;
import com.fidd.common.streamchain.BufferChainOutputStream;
import com.fidd.common.streamchain.OutputStreamLimitReachedException;
import com.fidd.common.streamchain.chain.BufferChain;
import com.fidd.common.streamchain.chain.ConcurrentBufferChain;
import com.fidd.connectors.FiddConnector;
import com.fidd.connectors.base.BaseDirectoryConnector;
import com.yandex.disk.rest.Credentials;
import com.yandex.disk.rest.DownloadListener;
import com.yandex.disk.rest.ResourcesArgs;
import com.yandex.disk.rest.RestClient;
import com.yandex.disk.rest.exceptions.ServerException;
import com.yandex.disk.rest.exceptions.ServerIOException;
import com.yandex.disk.rest.exceptions.http.HttpCodeException;
import com.yandex.disk.rest.json.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class YandexDiskFiddConnector extends BaseDirectoryConnector implements FiddConnector {
    public final static Logger LOGGER = LoggerFactory.getLogger(YandexDiskFiddConnector.class);
    public static final long BUFFER_SIZE = 1024;

    final String user;
    final String token;
    final RestClient client;
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
            client = new RestClient(new Credentials(user, token));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // TODO: listing speed can be improved with sort, offset, limit parameters of the request
    @Override
    protected FileInfo getFileInfoInternal(String path) throws FileNotFoundException, IOException {
        ResourcesArgs rootDirSubdirArgs = new ResourcesArgs.Builder().setPath(StringUtils.defaultIfBlank(path, "/")).build();
        try {
            List<FileListInfo> result = new ArrayList<>();
            Resource mainResource = client.getResources(rootDirSubdirArgs);
            if (mainResource.getResourceList() != null) {
                for (Resource childResource : mainResource.getResourceList().getItems()) {
                    if ("dir".equals(childResource.getType())) {
                        // Add dir
                        result.add(new FileListInfo(childResource.getPath().getPath(), true));
                    } else if ("file".equals(childResource.getType())) {
                        //Add file
                        result.add(new FileListInfo(childResource.getPath().getPath(), false));
                    }
                }
            }
            return new FileInfo("dir".equals(mainResource.getType()), mainResource.getSize(), result);
        } catch (HttpCodeException he) {
            // Will throw 404 if not found
            if (he.getCode() == 404) {
                throw new FileNotFoundException("Path not found: " + path);
            }
            throw new IOException(he);
        } catch (ServerIOException e) {
            throw new IOException(e);
        }
    }

    @Override
    protected String fiddFolderPath() { return fiddFolderPath; }

    @Override
    protected byte[] readAllBytes(String path) throws IOException {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            client.downloadFile(path, new DownloadListener() {
                @Override
                public OutputStream getOutputStream(boolean append) throws IOException {
                    return bos;
                }
            });
            return bos.toByteArray();
        } catch (ServerIOException e) {
            throw new IOException(e);
        } catch (ServerException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * DownloadListener.getLocalLength looks like offset to me
     * Length (Data limit) can be controlled by throwing an exception from OutputStream
     */
    @Override
    protected InputStream getSubInpuStream(String path, long offset, long length) throws IOException {
        BufferChain chain = new ConcurrentBufferChain();
        BufferChainInputStream is = new BufferChainInputStream(chain);
        BufferChainOutputStream os = new BufferChainOutputStream(chain, (int)BUFFER_SIZE, length);

        // TODO YaDisk Client is blocking, mb use Scheduler here?
        new Thread(() -> {
            try {
                client.downloadFile(path, new DownloadListener() {
                    @Override
                    public long getLocalLength() { return offset; }

                    @Override
                    public Long getLocalSize() { return length; }

                    @Override
                    public OutputStream getOutputStream(boolean append) throws IOException {
                        return os;
                    }
                });
            } catch (OutputStreamLimitReachedException e) {
                // This is our exception, we already got the chunk, ignore
                //LOGGER.debug("getSubInputStream", e);
            } catch (IOException | ServerException e) {
                LOGGER.debug("getSubInputStream", e);
            }
        }).start();

        return is;
    }
}
