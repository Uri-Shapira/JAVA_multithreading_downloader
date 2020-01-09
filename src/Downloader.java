import java.io.*;
import java.net.*;

public class Downloader implements Runnable{

    private final int m_chunk = 4096;
    private String m_fileToDownload;
    private String m_metadataFile;
    private Metadata m_metadata;
    private String m_downloadedFile;
    private long m_startPosition;
    private long m_currentPosition;
    private long m_endPosition;

    Downloader(String fileToDownload, String savedFileName, long startIndex, long endIndex, Metadata metadata, String metadataFile){
        m_fileToDownload = fileToDownload;
        m_downloadedFile = savedFileName;
        m_startPosition = startIndex;
        m_currentPosition = startIndex;
        m_endPosition = endIndex;
        m_metadataFile = metadataFile;
        m_metadata = metadata;
    }

    public void run() {
        try{
            System.out.println("[" + Thread.currentThread().getId() + "] Started Running");
            URL url = new URL(m_fileToDownload);
            HttpURLConnection HTTPUrlConnection = (HttpURLConnection) url.openConnection();
            HTTPUrlConnection.setRequestProperty("Range", "bytes=" + m_startPosition + "-" + m_endPosition);
            BufferedInputStream bufferedInputStream = new BufferedInputStream(HTTPUrlConnection.getInputStream());
            RandomAccessFile writeFile = new RandomAccessFile(m_downloadedFile, "rw");
            writeFile.seek(m_startPosition);
            byte[] bufferedData = new byte[m_chunk];
            ObjectOutputStream metadataWriter = new ObjectOutputStream(new FileOutputStream(new File(m_metadataFile)));
            int read;
            while(m_currentPosition < m_endPosition){
                int fileChunk = (int)m_currentPosition / m_chunk;
                System.out.println("[" + Thread.currentThread().getId()+ "] File chunk " + fileChunk);
                System.out.println("[" + Thread.currentThread().getId()+ "] Current Position Before " + m_currentPosition);
                read = bufferedInputStream.read(bufferedData,0, m_chunk);
                if (read == -1){
                    break;
                }
                writeFile.write(bufferedData, 0, read);
                m_metadata.downloadStatus[fileChunk] = 1;
                m_currentPosition += read;
                System.out.println("[" + Thread.currentThread().getId()+ "] Current Position After " + m_currentPosition);
            }
            metadataWriter.writeObject(m_metadata);
            System.out.println("[" + Thread.currentThread().getId() + "] has finished successfully.");
            bufferedInputStream.close();
            writeFile.close();
            metadataWriter.close();
        }
        catch (IOException e){
            System.err.println(e.getMessage());
        }
    }

}
