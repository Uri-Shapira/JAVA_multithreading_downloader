import java.io.*;
import java.util.concurrent.BlockingQueue;
import java.nio.file.*;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;

public class DownloadWriter implements Runnable {

    private Metadata m_metadata;
    private BlockingQueue<WriteChunk> m_writingQueue;
    private String m_writeFile;
    private String m_metadataFile;

    DownloadWriter(Metadata i_metadata, String i_downloadedFile, String i_metadataFile, BlockingQueue<WriteChunk> i_writeQueue){
        m_metadata = i_metadata;
        m_writingQueue = i_writeQueue;
        m_metadataFile = i_metadataFile;
        m_writeFile = i_downloadedFile;
    }

    public void run() {
        while (true) {
            if (!m_writingQueue.isEmpty()) {
                WriteChunk chunkToWrite = m_writingQueue.peek();
                m_writingQueue.remove();
                // m_downloadFinished will be true only when the dummy downloadFinisher chunk exits the queue
                if (chunkToWrite.m_downloadFinished) {
                    break;
                }
                try (ObjectOutputStream metadataWriter = new ObjectOutputStream(new FileOutputStream("temp_file"));
                     RandomAccessFile downloadedFile = new RandomAccessFile(m_writeFile, "rw")) {
                    downloadedFile.seek(chunkToWrite.m_writePosition);
                    downloadedFile.write(chunkToWrite.m_chunkToWrite, 0, chunkToWrite.m_chunkSize);
                    m_metadata.chunksDownloaded[chunkToWrite.m_metadataIndex] = true;
                    m_metadata.chunksDownloadedAlready += 1;
                    m_metadata.bytesDownloadedAlready += chunkToWrite.m_chunkSize;
                    metadataWriter.writeObject(m_metadata);
                    metadataWriter.close();
                    // create temporary file to hold metadata object before being moved to the actual metadata
                    File temp = new File("temp_file");
                    File metadataDestination = new File(m_metadataFile);
                    try {
                        Files.move(temp.toPath(), metadataDestination.toPath(), ATOMIC_MOVE);
                    }
                    catch (IOException inner) {
                        //TODO: do something with this catch error
                    }
                }
                catch (IOException e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }
}
